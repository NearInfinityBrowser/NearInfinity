// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.gui.TileGrid;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TisResource2 implements Resource, ActionListener, ChangeListener, KeyListener, Closeable
{
  private static final int DEFAULT_COLUMNS = 5;

  private final static JCheckBox cbGrid = new JCheckBox("Show Grid"); // show/hide frame around each tile

  private final ResourceEntry entry;
  private TisDecoder decoder;
  private BufferedImage[] tileImages;   // stores one tile per image
  private TileGrid tileGrid;            // the main component for displaying the tileset
  private JSlider slCols;               // changes the tiles per row
  private JTextField tfCols;            // input/output tiles per row
  private JButton bExport;              // "Export..." button
  private JPanel panel;                 // top-level panel of the viewer

  /*
  // single-threaded version
  public static BufferedImage drawImage(ResourceEntry entry, int width, int height, int mapIndex,
                                        int lookupIndex, Overlay overlay) throws Exception
  {
    TisDecoder decoder = new TisDecoder(entry);

    Vector<TileInfo> tiles = new Vector<TileInfo>(width * height);
    for (int ypos = 0; ypos < height; ypos++) {
      for (int xpos = 0; xpos < width; xpos++) {
        AbstractStruct wedtilemap = (AbstractStruct)overlay.getStructEntryAt(ypos*width + xpos + mapIndex);
        int lookup = ((DecNumber)wedtilemap.getAttribute("Primary tile index")).getValue();
        int tilenum = ((DecNumber)overlay.getStructEntryAt(lookup + lookupIndex)).getValue();
        tiles.add(new TileInfo(xpos, ypos, tilenum));
      }
    }

    BufferedImage image = new BufferedImage(width*decoder.info().tileWidth(),
        height*decoder.info().tileHeight(),
        BufferedImage.TYPE_INT_RGB);
    ColorConvert.ColorFormat colorFormat = ColorConvert.ColorFormat.R8G8B8;
    int tileWidth = decoder.info().tileWidth();
    int tileHeight = decoder.info().tileHeight();
    int[] tileBlock = new int[tileWidth*tileHeight];
    for (int i = 0; i < tiles.size(); i++) {
      TileInfo tile = tiles.get(i);

      // decoding tile
      ColorConvert.BufferToColor(colorFormat, decoder.decodeTile(tile.tilenum, colorFormat, false), 0,
          tileBlock, 0, tileBlock.length);

      // drawing tile
      for (int y = 0; y < tileHeight; y++)
        for (int x = 0; x < tileWidth; x++)
          image.setRGB(tile.xpos*tileWidth + x, tile.ypos*tileHeight + y, tileBlock[y*tileWidth + x]);
    }

    return image;
  }
  */

  // multi-threaded version
  public static BufferedImage drawImage(ResourceEntry entry, int width, int height, int mapIndex,
                                        int lookupIndex, Overlay overlay) throws Exception
  {
    // max. number of active threads
    final int THREADS_MAX = (int)(Math.ceil(Runtime.getRuntime().availableProcessors() * 1.25));

    TisDecoder decoder = new TisDecoder(entry);

    // creating tile index map
    List<TileInfo> tiles = new ArrayList<TileInfo>(width * height);
    for (int ypos = 0; ypos < height; ypos++) {
      for (int xpos = 0; xpos < width; xpos++) {
        AbstractStruct wedtilemap = (AbstractStruct)overlay.getStructEntryAt(ypos*width + xpos + mapIndex);
        int lookup = ((DecNumber)wedtilemap.getAttribute("Primary tile index")).getValue();
        int tilenum = ((DecNumber)overlay.getStructEntryAt(lookup + lookupIndex)).getValue();
        tiles.add(new TileInfo(xpos, ypos, tilenum));
      }
    }

    BufferedImage image = new BufferedImage(width*decoder.info().tileWidth(),
                                            height*decoder.info().tileHeight(),
                                            BufferedImage.TYPE_INT_RGB);
    ColorConvert.ColorFormat colorFormat = ColorConvert.ColorFormat.R8G8B8;
    int tileWidth = decoder.info().tileWidth();
    int tileHeight = decoder.info().tileHeight();
    int tileCount = tiles.size();

    LinkedBlockingQueue<TileInfo> queue = new LinkedBlockingQueue<TileInfo>();
    int threadsRunning = 0;   // indicates the number of currently active threads
    int tilesStarted = 0;     // indicates the total number of tiles already in the execution pipeline
    int tilesFinished = 0;    // indicates the total number of decoded and drawn tiles
    while (tilesFinished < tileCount) {
      // starting a couple of threads
      while (tilesStarted < tileCount && threadsRunning < THREADS_MAX) {
        (new Thread(new TileDecoder(queue, decoder, tiles.get(tilesStarted), colorFormat))).start();
        tilesStarted++;
        threadsRunning++;
      }

      // drawing tiles if available
      if (queue.peek() != null) {
        // drawing tile
        TileInfo info = queue.poll();
        for (int y = 0; y < tileHeight; y++)
          for (int x = 0; x < tileWidth; x++)
            image.setRGB(info.xpos*tileWidth + x, info.ypos*tileHeight + y, info.output[y*tileWidth + x]);
        info.output = null;
        threadsRunning--;
        tilesFinished++;
      }
    }
    decoder = null;
    tiles = null;

    return image;
  }


  public TisResource2(ResourceEntry entry) throws Exception
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());

    this.entry = entry;
    decoder = new TisDecoder(entry);

    try {
      blocker.setBlocked(true);
      tileImages = new BufferedImage[decoder.info().tileCount()];
      for (int idx = 0; idx < decoder.info().tileCount(); idx++)
        loadTile(idx);
      blocker.setBlocked(false);
    } catch (Exception e) {
      blocker.setBlocked(false);
      tileImages = new BufferedImage[0];
      throw e;
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    }
  }

//--------------------- End Interface ActionListener ---------------------


//--------------------- Begin Interface ChangeListener ---------------------

  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == slCols) {
      int cols = slCols.getValue();
      tfCols.setText(Integer.toString(cols));
      tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
    } else if (event.getSource() == cbGrid) {
      tileGrid.setShowGrid(cbGrid.isSelected());
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface KeyListener ---------------------

  public void keyPressed(KeyEvent event)
  {
    if (event.getSource() == tfCols) {
      if (event.getKeyCode() == KeyEvent.VK_ENTER) {
        int cols;
        try {
          cols = Integer.parseInt(tfCols.getText());
        } catch (NumberFormatException e) {
          cols = slCols.getValue();
          tfCols.setText(Integer.toString(slCols.getValue()));
        }
        if (cols != slCols.getValue()) {
          if (cols <= 0)
            cols = 1;
          if (cols >= decoder.info().tileCount())
            cols = decoder.info().tileCount();
          slCols.setValue(cols);
          tfCols.setText(Integer.toString(slCols.getValue()));
          tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
        }
        slCols.requestFocus();    // remove focus from textfield
      }
    }
  }


  public void keyReleased(KeyEvent event)
  {
    // nothing to do
  }


  public void keyTyped(KeyEvent event)
  {
    // nothing to do
  }

//--------------------- End Interface KeyListener ---------------------


//--------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    cbGrid.removeChangeListener(this);
    tileImages = null;
    tileGrid = null;
    decoder = null;
    System.gc();      // XXX: There have to be better ways to free resources from memory
  }

//--------------------- End Interface Closeable ---------------------


//--------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------


//--------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    int tileCount = decoder.info().tileCount();

    // 1. creating top panel
    // 1.1. creating label with text field
    JLabel lblTPR = new JLabel("Tiles per row:");
    tfCols = new JTextField(Integer.toString(DEFAULT_COLUMNS), 5);
    tfCols.addKeyListener(this);
    JPanel tPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
    tPanel1.add(lblTPR);
    tPanel1.add(tfCols);

    // 1.2. creating slider
    slCols = new JSlider(JSlider.HORIZONTAL, 1, tileCount, DEFAULT_COLUMNS);
    if (tileCount > 1000) {
      slCols.setMinorTickSpacing(100);
      slCols.setMajorTickSpacing(1000);
    } else if (tileCount > 100) {
      slCols.setMinorTickSpacing(10);
      slCols.setMajorTickSpacing(100);
    } else {
      slCols.setMinorTickSpacing(1);
      slCols.setMajorTickSpacing(10);
    }
    slCols.setPaintTicks(true);
    slCols.addChangeListener(this);

    // 1.3. adding left side of the top panel together
    JPanel tlPanel = new JPanel(new GridLayout(2, 1));
    tlPanel.add(tPanel1);
    tlPanel.add(slCols);

    // 1.4. configuring checkbox
    cbGrid.addChangeListener(this);
    JPanel trPanel = new JPanel(new GridLayout());
    trPanel.add(cbGrid);

    // 1.5. putting top panel together
    BorderLayout bl = new BorderLayout();
    JPanel topPanel = new JPanel(bl);
    topPanel.add(tlPanel, BorderLayout.CENTER);
    topPanel.add(trPanel, BorderLayout.LINE_END);

    // 2. creating main panel
    // 2.1. creating tiles table and scroll pane
    tileGrid = new TileGrid(1, DEFAULT_COLUMNS, decoder.info().tileWidth(), decoder.info().tileHeight());
    tileGrid.addImage(tileImages);
    tileGrid.setTileColor(Color.BLACK);
    // The formula for 'colSize' attempts to approximate the most commonly used aspect ratio found in TIS files
    tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), (int)(Math.sqrt(tileGrid.getImageCount())*1.18)));
    tileGrid.setShowGrid(cbGrid.isSelected());
    slCols.setValue(tileGrid.getTileColumns());
    tfCols.setText(Integer.toString(tileGrid.getTileColumns()));
    JScrollPane scroll = new JScrollPane(tileGrid);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    // 2.2. putting main panel together
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(scroll, BorderLayout.CENTER);

    // 3. creating bottom panel
    // 3.1. creating export button
    bExport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bExport.setMnemonic('e');
    bExport.addActionListener(this);
    // 3.2. putting bottom panel together
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottomPanel.add(bExport);

    // 4. packing all together
    panel = new JPanel(new BorderLayout());
    panel.add(topPanel, BorderLayout.PAGE_START);
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(bottomPanel, BorderLayout.PAGE_END);
    centerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  private void loadTile(int idx) throws Exception
  {
    if (decoder != null && tileImages != null) {
      try {
        if (idx >= 0 && idx < decoder.info().tileCount()) {
          // preparations
          ColorConvert.ColorFormat colorFormat = ColorConvert.ColorFormat.R8G8B8;
          int tileWidth = decoder.info().tileWidth();
          int tileHeight = decoder.info().tileHeight();
          int[] block = new int[tileWidth*tileHeight];

          // decoding tile
          ColorConvert.BufferToColor(colorFormat, decoder.decodeTile(idx, colorFormat, false), 0,
                                     block, 0, block.length);

          // drawing tile
          BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
          for (int y = 0; y < tileHeight; y++)
            for (int x = 0; x < tileWidth; x++)
              img.setRGB(x, y, block[y*tileWidth + x]);

          tileImages[idx] = img;
          block = null;
        }
      } catch (Exception e) {
        throw new Exception("Error loading tile #" + idx + ": " + e.getMessage());
      }
    }
  }

  private Dimension calcGridSize(int imageCount, int colSize)
  {
    if (imageCount >= 0 && colSize > 0) {
      int rowSize = imageCount / colSize;
      if (imageCount % colSize > 0)
        rowSize++;
      return new Dimension(colSize, Math.max(1, rowSize));
    }
    return null;
  }


//-------------------------- INNER CLASSES --------------------------

  // stores information about a single tile only
  private static final class TileInfo
  {
    private final int xpos, ypos; // coordinate in tile grid
    private final int tilenum;    // tile index from WED
    private int[] output;         // decoded pixel data of the tile

    private TileInfo(int xpos, int ypos, int tilenum)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.output = null;
    }
  }


  // decodes a single tile asynchronously
  private static class TileDecoder implements Runnable
  {
    private final TisDecoder decoder;
    private final LinkedBlockingQueue<TileInfo> queue;
    private final TileInfo tileInfo;
    private final ColorConvert.ColorFormat outFormat;

    public TileDecoder(LinkedBlockingQueue<TileInfo> queue,
                       TisDecoder decoder,
                       TileInfo info,
                       ColorConvert.ColorFormat fmt) throws Exception
    {
      if (queue == null || decoder == null || info == null ||
          info.tilenum < 0 || info.tilenum >= decoder.info().tileCount())
        throw new Exception("Invalid arguments specified");
      this.queue = queue;
      this.decoder = decoder;
      tileInfo = info;
      outFormat = fmt;
    }

    public void run()
    {
      tileInfo.output = new int[decoder.info().tileWidth()*decoder.info().tileHeight()];
      try {
        ColorConvert.BufferToColor(outFormat, decoder.decodeTile(tileInfo.tilenum, outFormat, false),
                                   0, tileInfo.output, 0, tileInfo.output.length);
      } catch (Exception e)
      {
        e.printStackTrace();
      }

      int counter = 50;
      while (counter > 0) {
        try {
          queue.offer(tileInfo, 10, TimeUnit.MILLISECONDS);
          counter = 0;
        } catch (InterruptedException e) {
          counter--;
          if (counter == 0) {
            System.err.println("Error putting tile data block into queue");
            e.printStackTrace();
          }
        }
      }
    }
  }

}
