// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.TileGrid;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.WedResource;
import infinity.util.DynamicArray;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.RootPaneContainer;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TisResource2 implements Resource, Closeable, ActionListener, ChangeListener,
                                     KeyListener, PropertyChangeListener
{
  private static final int DEFAULT_COLUMNS = 5;

  private static boolean showGrid = false;

  private final ResourceEntry entry;
  private TisDecoder decoder;
  private List<Image> tileImages;         // stores one tile per image
  private TileGrid tileGrid;              // the main component for displaying the tileset
  private JSlider slCols;                 // changes the tiles per row
  private JTextField tfCols;              // input/output tiles per row
  private JCheckBox cbGrid;               // show/hide frame around each tile
  private ButtonPopupMenu bpmExport;      // "Export..." button menu
  private JMenuItem miExport, miOldTis;
  private JButton bExport;
  private JPanel panel;                   // top-level panel of the viewer
  private RootPaneContainer rpc;
  private SwingWorker<List<byte[]>, Void> workerConvert;
  private WindowBlocker blocker;


  /**
   * Draws a list of map tiles into the specified image object.
   * @param image The image to draw the tiles into
   * @param decoder The TIS decoder needed to decode the tiles
   * @param tilesX Number of tiles per row
   * @param tilesY Number of tile rows
   * @param tileInfo A list of info objects needed to draw the right tiles
   * @return true if successful, false otherwise
   */
  public static boolean drawTiles(BufferedImage image, TisDecoder decoder,
                                  int tilesX, int tilesY, List<TileInfo> tileInfo)
  {
    if (image != null && decoder != null && tileInfo != null) {
      int tileWidth = decoder.info().tileWidth();
      int tileHeight = decoder.info().tileHeight();
      int width = tilesX * tileWidth;
      int height = tilesY * tileHeight;
      if (image.getWidth() >= width && image.getHeight() >= height) {
        final BufferedImage imgTile = ColorConvert.createCompatibleImage(tileWidth, tileHeight, false);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (final TileInfo tile: tileInfo) {
          try {
            if (decoder.decodeTile(imgTile, tile.tilenum)) {
              g.drawImage(imgTile, tile.xpos*tileWidth, tile.ypos*tileHeight, null);
            }
          } catch (Exception e) {
            System.err.println("Error drawing tile #" + tile.tilenum);
          }
        }
        g.dispose();
        return true;
      }
    }
    return false;
  }

  /**
   * Draws a specific list of primary or secondary tiles, depending on the specified opened/closed state.
   * @param image The image to draw the tiles into
   * @param decoder The TIS decoder needed to decode the tiles
   * @param tilesX Number of tiles per row
   * @param tilesY Number of tile rows
   * @param tileInfo List of info objects needed to draw the right tiles
   * @param doorIndices List of info objects of specific door tiles
   * @param drawClosed Indicates whether the primary or secondary tile has to be drawn
   * @return true if successful, false otherwise
   */
  public static boolean drawDoorTiles(BufferedImage image, TisDecoder decoder,
                                      int tilesX, int tilesY, List<TileInfo> tileInfo,
                                      List<Integer> doorIndices, boolean drawClosed)
  {
    if (image != null && decoder != null && tileInfo != null && doorIndices != null) {
      int tileWidth = decoder.info().tileWidth();
      int tileHeight = decoder.info().tileHeight();
      int width = tilesX * tileWidth;
      int height = tilesY * tileHeight;
      if (image.getWidth() >= width && image.getHeight() >= height) {
        final BufferedImage imgTile = ColorConvert.createCompatibleImage(tileWidth, tileHeight, false);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (final int index: doorIndices) {
          // searching for correct tileinfo object
          TileInfo tile = tileInfo.get(index);
          if (tile.tilenum != index) {
            for (TileInfo ti: tileInfo) {
              if (ti.tilenum == index) {
                tile = ti;
                break;
              }
            }
          }

          // decoding tile
          int tileIdx = (drawClosed && tile.tilenumAlt != -1) ? tile.tilenumAlt : tile.tilenum;
          try {
            if (decoder.decodeTile(imgTile, tileIdx)) {
              g.drawImage(imgTile, tile.xpos*tileWidth, tile.ypos*tileHeight, null);
            }
          } catch (Exception e) {
            System.err.println("Error drawing tile #" + tileIdx);
          }
        }
        g.dispose();
        return true;
      }
    }
    return false;
  }


  public TisResource2(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    initTileset();
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if ((miExport != null && event.getSource() == miExport) ||
        (bExport != null && event.getSource() == bExport)) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miOldTis) {
      blocker = new WindowBlocker(rpc);
      blocker.setBlocked(true);
      workerConvert = new SwingWorker<List<byte[]>, Void>() {
        @Override
        public List<byte[]> doInBackground()
        {
          List<byte[]> list = new Vector<byte[]>(1);
          try {
            byte[] buf = convertToOldTis();
            if (buf != null) {
              list.add(buf);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return list;
        }
      };
      workerConvert.addPropertyChangeListener(this);
      workerConvert.execute();
    }
  }

//--------------------- End Interface ActionListener ---------------------


//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == slCols) {
      int cols = slCols.getValue();
      tfCols.setText(Integer.toString(cols));
      tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
    } else if (event.getSource() == cbGrid) {
      showGrid = cbGrid.isSelected();
      tileGrid.setShowGrid(showGrid);
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface KeyListener ---------------------

  @Override
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


  @Override
  public void keyReleased(KeyEvent event)
  {
    // nothing to do
  }


  @Override
  public void keyTyped(KeyEvent event)
  {
    // nothing to do
  }

//--------------------- End Interface KeyListener ---------------------

//--------------------- Begin Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent event)
  {
    if (event.getSource() == workerConvert) {
      if ("state".equals(event.getPropertyName()) &&
          SwingWorker.StateValue.DONE == event.getNewValue()) {
        if (blocker != null) {
          blocker.setBlocked(false);
          blocker = null;
        }
        byte[] tisData = null;
        try {
          List<byte[]> l = workerConvert.get();
          if (l != null && !l.isEmpty()) {
            tisData = workerConvert.get().get(0);
            l.clear();
            l = null;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (tisData != null) {
          if (tisData.length > 0) {
            ResourceFactory.getInstance().exportResource(entry, tisData, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } else {
            JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                          "Export has been cancelled." + entry, "Information",
                                          JOptionPane.INFORMATION_MESSAGE);
          }
          tisData = null;
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Error while exporting " + entry, "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

//--------------------- End Interface PropertyChangeListener ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (workerConvert != null) {
      if (!workerConvert.isDone()) {
        workerConvert.cancel(true);
      }
      workerConvert = null;
    }
    tileImages.clear();
    tileImages = null;
    tileGrid.clearImages();
    tileGrid = null;
    if (decoder != null) {
      decoder.close();
      decoder = null;
    }
    System.gc();
  }

//--------------------- End Interface Closeable ---------------------


//--------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------


//--------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    if (container instanceof RootPaneContainer) {
      rpc = (RootPaneContainer)container;
    } else {
      rpc = NearInfinity.getInstance();
    }

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
    cbGrid = new JCheckBox("Show Grid", showGrid);
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
    if (tileGrid.getImageCount() > 6) {
      int colSize = calcTileWidth(entry, tileGrid.getImageCount());
      tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), colSize));
    } else {
      // displaying overlay tilesets in a single row
      tileGrid.setGridSize(new Dimension(tileGrid.getImageCount(), 1));
    }
    tileGrid.setShowGrid(showGrid);
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
    if (decoder.info().type() == TisDecoder.TisInfo.TisType.PVRZ) {
      miExport = new JMenuItem("original");
      miExport.addActionListener(this);
      miOldTis = new JMenuItem("as legacy TIS");
      miOldTis.addActionListener(this);
      bpmExport = new ButtonPopupMenu("Export...", new JMenuItem[]{miExport, miOldTis});
      bpmExport.setIcon(Icons.getIcon("Export16.gif"));
      bpmExport.setMnemonic('e');
      bExport = null;
    } else {
      bExport = new JButton("Export...", Icons.getIcon("Export16.gif"));
      bExport.setMnemonic('e');
      bExport.addActionListener(this);
      bpmExport = null;
    }

    // 3.2. putting bottom panel together
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    if (bpmExport != null) {
      bottomPanel.add(bpmExport);
    } else if (bExport != null) {
      bottomPanel.add(bExport);
    }

    // 4. packing all together
    panel = new JPanel(new BorderLayout());
    panel.add(topPanel, BorderLayout.PAGE_START);
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(bottomPanel, BorderLayout.PAGE_END);
    centerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  private void initTileset()
  {
    try {
      WindowBlocker.blockWindow(true);

      decoder = new TisDecoder(entry);
      int tileCount = decoder.info().tileCount();
      tileImages = new ArrayList<Image>(tileCount);
      for (int tileIdx = 0; tileIdx < tileCount; tileIdx++) {
        final BufferedImage image = decoder.decodeTile(tileIdx);
        if (image != null) {
          tileImages.add(image);
        } else {
          tileImages.add(ColorConvert.createCompatibleImage(decoder.info().tileWidth(),
                                                            decoder.info().tileHeight(), false));
        }
      }
      decoder.flush();
      WindowBlocker.blockWindow(false);
    } catch (Exception e) {
      e.printStackTrace();
      WindowBlocker.blockWindow(false);
      if (tileImages == null)
        tileImages = new ArrayList<Image>();
      if (tileImages.isEmpty())
        tileImages.add(ColorConvert.createCompatibleImage(1, 1, false));
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                    "Error while loading TIS resource: " + entry.getResourceName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  // Converts the current PVRZ-based tileset into the old tileset variant. DO NOT call directly!
  private byte[] convertToOldTis()
  {
    byte[] buf = null;
    if (tileImages != null && !tileImages.isEmpty()) {
        String note = "Converting tile %1$d / %2$d";
        int progressIndex = 0, progressMax = decoder.info().tileCount();
        ProgressMonitor progress =
            new ProgressMonitor(panel.getTopLevelAncestor(), "Converting TIS...",
                                String.format(note, progressIndex, progressMax), 0, progressMax);
        progress.setMillisToDecideToPopup(250);
        progress.setMillisToPopup(1000);

        buf = new byte[24 + decoder.info().tileCount()*5120];
        // writing header data
        System.arraycopy("TIS V1  ".getBytes(), 0, buf, 0, 8);
        DynamicArray.putInt(buf, 8, decoder.info().tileCount());
        DynamicArray.putInt(buf, 12, 0x1400);
        DynamicArray.putInt(buf, 16, 0x18);
        DynamicArray.putInt(buf, 20, 0x40);

        // writing tiles
        int bufOfs = 24;
        int[] palette = new int[256];
        int[] hslPalette = new int[256];
        byte[] tilePalette = new byte[1024];
        byte[] tileData = new byte[64*64];
        BufferedImage image = ColorConvert.createCompatibleImage(decoder.info().tileWidth(),
                                                                 decoder.info().tileHeight(), false);
        for (int tileIdx = 0; tileIdx < decoder.info().tileCount(); tileIdx++) {
          if (progress.isCanceled()) {
            buf = new byte[0];
            break;
          }
          progressIndex++;
          if ((progressIndex % 50) == 0) {
            progress.setProgress(progressIndex);
            progress.setNote(String.format(note, progressIndex, progressMax));
          }

          Graphics2D g = (Graphics2D)image.getGraphics();
          g.drawImage(tileImages.get(tileIdx), 0, 0, null);
          g.dispose();
          g = null;

          int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
          if (ColorConvert.medianCut(pixels, 256, palette)) {
            ColorConvert.toHslPalette(palette, hslPalette);
            // filling palette
            for (int i = 0; i < 256; i++) {
              tilePalette[(i << 2) + 0] = (byte)(palette[i] & 0xff);
              tilePalette[(i << 2) + 1] = (byte)((palette[i] >>> 8) & 0xff);
              tilePalette[(i << 2) + 2] = (byte)((palette[i] >>> 16) & 0xff);
              tilePalette[(i << 2) + 3] = 0;
            }
            // filling pixel data
            for (int i = 0; i < tileData.length; i++) {
              tileData[i] = (byte)(ColorConvert.nearestColor(pixels[i], hslPalette));
            }
          } else {
            buf = null;
            break;
          }
          System.arraycopy(tilePalette, 0, buf, bufOfs, 1024);
          bufOfs += 1024;
          System.arraycopy(tileData, 0, buf, bufOfs, 4096);
          bufOfs += 4096;
        }
        image.flush(); image = null;
        tileData = null; tilePalette = null; hslPalette = null; palette = null;
        progress.close();
    }
    return buf;
  }

  // calculates a Dimension structure with the correct number of columns and rows from the specified arguments
  private static Dimension calcGridSize(int imageCount, int colSize)
  {
    if (imageCount >= 0 && colSize > 0) {
      int rowSize = imageCount / colSize;
      if (imageCount % colSize > 0)
        rowSize++;
      return new Dimension(colSize, Math.max(1, rowSize));
    }
    return null;
  }

  // attempts to calculate the TIS width from an associated WED file
  private static int calcTileWidth(ResourceEntry entry, int tileCount)
  {
    // Try to fetch the correct width from an associated WED if available
    if (entry != null) {
      try {
        String tisNameBase = entry.getResourceName();
        if (tisNameBase.lastIndexOf('.') > 0)
          tisNameBase = tisNameBase.substring(0, tisNameBase.lastIndexOf('.'));
        ResourceEntry wedEntry = null;
        while (tisNameBase.length() >= 6) {
          String wedFileName = tisNameBase + ".WED";
          wedEntry = ResourceFactory.getInstance().getResourceEntry(wedFileName);
          if (wedEntry != null)
            break;
          else
            tisNameBase = tisNameBase.substring(0, tisNameBase.length() - 1);
        }
        if (wedEntry != null) {
          WedResource wedResource = new WedResource(wedEntry);
          Overlay overlay = (Overlay)wedResource.getAttribute("Overlay 0");
          ResourceRef tisRef = (ResourceRef)overlay.getAttribute("Tileset");
          ResourceEntry tisEntry = ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
          if (tisEntry != null) {
            String tisName = tisEntry.getResourceName();
            if (tisName.lastIndexOf('.') > 0)
              tisName = tisName.substring(0, tisName.lastIndexOf('.'));
            int maxLen = Math.min(tisNameBase.length(), tisName.length());
            if (tisNameBase.startsWith(tisName.substring(0, maxLen))) {
              return ((DecNumber)overlay.getAttribute("Width")).getValue();
            }
          }
        }
      } catch (Exception e) {
      }
    }
    // If WED is not available: approximate the most commonly used aspect ratio found in TIS files
    // Disadvantage: does not take extra tiles into account
    return (int)(Math.sqrt(tileCount)*1.18);
  }


//-------------------------- INNER CLASSES --------------------------

  // stores information about a single tile only
  public static final class TileInfo
  {
    private final int xpos, ypos;   // coordinate in tile grid
    private final int tilenum;      // primary tile index from WED
    private final int tilenumAlt;   // secondary tile index from WED
    private final int[] overlayIndices; // index of additional overlays to address

    public TileInfo(int xpos, int ypos, int tilenum)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = -1;
      this.overlayIndices = null;
    }

    public TileInfo(int xpos, int ypos, int tilenum, int tilenumAlt)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = tilenumAlt;
      this.overlayIndices = null;
    }

    public TileInfo(int xpos, int ypos, int tilenum, int tilenumAlt, int overlayMask)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = tilenumAlt;

      // calculating additional overlays
      int mcount = 0;
      int[] mindices = new int[8];
      for (int i = 0; i < 8; i++) {
        if ((overlayMask & (1 << i)) != 0) {
          mindices[mcount] = i;
          mcount++;
          break;
        }
      }
      if (mcount > 0) {
        this.overlayIndices = new int[mcount];
        System.arraycopy(mindices, 0, this.overlayIndices, 0, mcount);
      } else
        this.overlayIndices = null;
    }
  }
}
