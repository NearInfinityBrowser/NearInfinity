// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.RenderCanvas;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;
import infinity.util.DynamicArray;
import infinity.util.IntegerHashMap;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

public class MosResource2 implements Resource, Closeable, ActionListener, ItemListener, PropertyChangeListener
{
  private static boolean ignoreTransparency = false;

  private final ResourceEntry entry;
  private MosDecoder.MosInfo.MosType mosType;
  private ButtonPopupMenu bpmExport;
  private JMenuItem miExport, miExportMOSV1, miExportMOSC, miExportPNG;
  private JButton bFind;
  private JCheckBox cbTransparency;
  private RenderCanvas rcImage;
  private JPanel panel;
  private RootPaneContainer rpc;
  private boolean compressed;
  private SwingWorker<List<byte[]>, Void> workerConvert;
  private boolean exportCompressed;
  private WindowBlocker blocker;

  public MosResource2(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    if (this.entry != null) {
      MosDecoder decoder = new MosDecoder(entry);
      compressed = decoder.info().isCompressed();
      mosType = decoder.info().type();
      decoder.close();
      decoder = null;
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bFind) {
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExportMOSV1) {
      if (mosType == MosDecoder.MosInfo.MosType.PVRZ) {
        // create new MOS V1 from scratch
        blocker = new WindowBlocker(rpc);
        blocker.setBlocked(true);
        startConversion(false);
      } else {
        if (compressed) {
          // decompress existing MOSC V1 and save as MOS V1
          try {
            byte[] data = entry.getResourceData();
            data = Compressor.decompress(data);
            ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                          "Error while exporting " + entry, "Error",
                                          JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    } else if (event.getSource() == miExportMOSC) {
      if (mosType == MosDecoder.MosInfo.MosType.PVRZ) {
        // create new MOSC V1 from scratch
        blocker = new WindowBlocker(rpc);
        blocker.setBlocked(true);
        startConversion(true);
      } else {
        if (!compressed) {
          // compress existing MOS V1 and save as MOSC V1
          try {
            byte[] data = entry.getResourceData();
            data = Compressor.compress(data, "MOSC", "V1  ");
            ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                          "Error while exporting " + entry, "Error",
                                          JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    } else if (event.getSource() == miExportPNG) {
      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String fileName = entry.toString().replace(".MOS", ".PNG");
        BufferedImage image = getImage();
        if (ImageIO.write(image, "png", os)) {
          ResourceFactory.getInstance().exportResource(entry, os.toByteArray(),
                                                       fileName, panel.getTopLevelAncestor());
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Error while exporting " + entry, "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
        os.close();
        os = null;
        image = null;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == cbTransparency) {
      ignoreTransparency = cbTransparency.isSelected();
      if (mosType == MosDecoder.MosInfo.MosType.PALETTE) {
        WindowBlocker.blockWindow(true);
        try {
          rcImage.setImage(loadImage());
          WindowBlocker.blockWindow(false);
        } catch (Exception e) {
        }
        WindowBlocker.blockWindow(false);
      }
    }
  }

//--------------------- End Interface ItemListener ---------------------

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
        byte[] mosData = null;
        try {
          List<byte[]> l = workerConvert.get();
          if (l != null && !l.isEmpty()) {
            mosData = l.get(0);
            l.clear();
            l = null;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (mosData != null) {
          if (mosData.length > 0) {
            ResourceFactory.getInstance().exportResource(entry, mosData, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } else {
            JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                          "Export has been cancelled." + entry, "Information",
                                          JOptionPane.INFORMATION_MESSAGE);
          }
          mosData = null;
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Error while exporting " + entry, "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

//--------------------- End Interface PropertyChangeListener ---------------------

//--------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    panel.removeAll();
    rcImage.setImage(null);
    rcImage = null;
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    if (container instanceof RootPaneContainer) {
      rpc = (RootPaneContainer)container;
    } else {
      rpc = NearInfinity.getInstance();
    }

    bFind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bFind.setMnemonic('f');
    bFind.addActionListener(this);

    miExport = new JMenuItem("original");
    miExport.addActionListener(this);
    miExportPNG = new JMenuItem("as PNG");
    miExportPNG.addActionListener(this);
    if (mosType == MosDecoder.MosInfo.MosType.PVRZ) {
      miExportMOSV1 = new JMenuItem("as MOS V1 (uncompressed)");
      miExportMOSV1.addActionListener(this);
      miExportMOSC = new JMenuItem("as MOS V1 (compressed)");
      miExportMOSC.addActionListener(this);
    } else {
      if (compressed) {
        miExportMOSV1 = new JMenuItem("decompressed");
        miExportMOSV1.addActionListener(this);
      } else {
        if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
            ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
            ResourceFactory.getGameID() == ResourceFactory.ID_BGEE ||
            ResourceFactory.getGameID() == ResourceFactory.ID_BG2EE) {
          miExportMOSC = new JMenuItem("compressed");
          miExportMOSC.addActionListener(this);
        }
      }
    }
    List<JMenuItem> list = new ArrayList<JMenuItem>();
    if (miExport != null)
      list.add(miExport);
    if (miExportMOSV1 != null)
      list.add(miExportMOSV1);
    if (miExportMOSC != null)
      list.add(miExportMOSC);
    if (miExportPNG != null)
      list.add(miExportPNG);
    JMenuItem[] mi = new JMenuItem[list.size()];
    for (int i = 0; i < mi.length; i++) {
      mi[i] = list.get(i);
    }
    bpmExport = new ButtonPopupMenu("Export...", mi);
    bpmExport.setIcon(Icons.getIcon("Export16.gif"));
    bpmExport.setMnemonic('e');

    rcImage = new RenderCanvas();
    rcImage.setHorizontalAlignment(SwingConstants.CENTER);
    rcImage.setVerticalAlignment(SwingConstants.CENTER);
    WindowBlocker.blockWindow(true);
    try {
      rcImage.setImage(loadImage());
      WindowBlocker.blockWindow(false);
    } catch (Exception e) {
      WindowBlocker.blockWindow(false);
    }
    JScrollPane scroll = new JScrollPane(rcImage);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    cbTransparency = new JCheckBox("Ignore transparency", ignoreTransparency);
    cbTransparency.setToolTipText("Only legacy MOS resources (MOS V1) are affected.");
    cbTransparency.addItemListener(this);
    JPanel optionsPanel = new JPanel();
    BoxLayout bl = new BoxLayout(optionsPanel, BoxLayout.Y_AXIS);
    optionsPanel.setLayout(bl);
    optionsPanel.add(cbTransparency);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(bFind);
    buttonPanel.add(bpmExport);
    buttonPanel.add(optionsPanel);

    panel = new JPanel(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  public BufferedImage getImage()
  {
    if (rcImage != null) {
      return ColorConvert.toBufferedImage(rcImage.getImage(), false);
    } else if (entry != null) {
      return loadImage();
    }
    return null;
  }

  private BufferedImage loadImage()
  {
    BufferedImage image = null;
    MosDecoder decoder = null;
    if (entry != null) {
      try {
        decoder = new MosDecoder(entry);
        compressed = decoder.info().isCompressed();
        mosType = decoder.info().type();
        image = ColorConvert.createCompatibleImage(decoder.info().width(),
                                                   decoder.info().height(), true);
        if (!decoder.decode(image, ignoreTransparency)) {
          image = null;
        }
        decoder.close();
      } catch (Exception e) {
        if (decoder != null)
          decoder.close();
        image = null;
        e.printStackTrace();
      }
    }
    return image;
  }

  // Creates a new MOS V1 or MOSC V1 resource from scratch. DO NOT call directly!
  private byte[] convertToMosV1(boolean compressed) throws Exception
  {
    byte[] buf = null;
    if (rcImage != null && rcImage.getImage() != null) {
      // preparing source image
      Image img = rcImage.getImage();
      BufferedImage srcImage = ColorConvert.createCompatibleImage(img.getWidth(null),
                                                                  img.getHeight(null),
                                                                  Transparency.BITMASK);
      Graphics2D g = (Graphics2D)srcImage.getGraphics();
      g.drawImage(getImage(), 0, 0, null);
      g.dispose();
      g = null;

      // preparing MOS V1 header
      int width = srcImage.getWidth();
      int height = srcImage.getHeight();
      int cols = (width + 63) / 64;
      int rows = (height + 63) / 64;
      int tileCount = cols * rows;
      int palOfs = 24;
      int tableOfs = palOfs + tileCount*1024;
      int dataOfs = tableOfs + tileCount*4;
      buf = new byte[dataOfs + width*height];
      System.arraycopy("MOS V1  ".getBytes(Charset.forName("US-ASCII")), 0, buf, 0, 8);
      DynamicArray.putShort(buf, 8, (short)width);
      DynamicArray.putShort(buf, 10, (short)height);
      DynamicArray.putShort(buf, 12, (short)cols);
      DynamicArray.putShort(buf, 14, (short)rows);
      DynamicArray.putInt(buf, 16, 64);
      DynamicArray.putInt(buf, 20, palOfs);

      String note = "Converting tile %1$d / %2$d";
      int progressIndex = 0, progressMax = tileCount;
      ProgressMonitor progress =
          new ProgressMonitor(panel.getTopLevelAncestor(), "Converting MOS...",
                              String.format(note, progressIndex, progressMax), 0, progressMax);
      progress.setMillisToDecideToPopup(500);
      progress.setMillisToPopup(2000);

      // creating list of tiles as int[] arrays
      List<int[]> tileList = new ArrayList<int[]>(cols*rows);
      for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
          int tileX = x * 64;
          int tileY = y * 64;
          int tileW = (tileX + 64 < width) ? 64 : (width - tileX);
          int tileH = (tileY + 64 < height) ? 64 : (height - tileY);
          int[] rgbArray = new int[tileW*tileH];
          srcImage.getRGB(tileX, tileY, tileW, tileH, rgbArray, 0, tileW);
          tileList.add(rgbArray);
        }
      }
      srcImage.flush(); srcImage = null;

      // applying color reduction to each tile
      int[] palette = new int[255];
      int[] hslPalette = new int[255];
      byte[] tilePalette = new byte[1024];
      byte[] tileData = new byte[64*64];
      int curPalOfs = palOfs, curTableOfs = tableOfs, curDataOfs = dataOfs;
      IntegerHashMap<Byte> colorCache = new IntegerHashMap<Byte>(1536);   // caching RGBColor -> index
      for (int tileIdx = 0; tileIdx < tileList.size(); tileIdx++) {
        colorCache.clear();
        if (progress.isCanceled()) {
          buf = new byte[0];
          break;
        }
        progressIndex++;
        if ((progressIndex % 10) == 0) {
          progress.setProgress(progressIndex);
          progress.setNote(String.format(note, progressIndex, progressMax));
        }

        int[] pixels = tileList.get(tileIdx);
        if (ColorConvert.medianCut(pixels, 255, palette, false)) {
          ColorConvert.toHslPalette(palette, hslPalette);
          // filling palette
          // first palette entry denotes transparency
          tilePalette[0] = tilePalette[2] = tilePalette[3] = 0; tilePalette[1] = (byte)255;
          for (int i = 1; i < 256; i++) {
            tilePalette[(i << 2) + 0] = (byte)(palette[i - 1] & 0xff);
            tilePalette[(i << 2) + 1] = (byte)((palette[i - 1] >>> 8) & 0xff);
            tilePalette[(i << 2) + 2] = (byte)((palette[i - 1] >>> 16) & 0xff);
            tilePalette[(i << 2) + 3] = 0;
            colorCache.put(palette[i - 1], (byte)(i - 1));
          }
          // filling pixel data
          for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] & 0xff000000) == 0) {
              tileData[i] = 0;
            } else {
              Byte palIndex = colorCache.get(pixels[i]);
              if (palIndex != null) {
                tileData[i] = (byte)(palIndex + 1);
              } else {
                byte color = (byte)ColorConvert.nearestColor(pixels[i], hslPalette);
                tileData[i] = (byte)(color + 1);
                colorCache.put(pixels[i], color);
              }
            }
          }
        } else {
          buf = null;
          break;
        }

        System.arraycopy(tilePalette, 0, buf, curPalOfs, 1024);
        curPalOfs += 1024;
        DynamicArray.putInt(buf, curTableOfs, curDataOfs - dataOfs);
        curTableOfs += 4;
        System.arraycopy(tileData, 0, buf, curDataOfs, pixels.length);
        curDataOfs += pixels.length;
      }
      tileList.clear(); tileList = null;
      tileData = null; tilePalette = null; hslPalette = null; palette = null;

      // optionally compressing to MOSC V1
      if (compressed) {
        if (buf != null) {
          buf = Compressor.compress(buf, "MOSC", "V1  ");
        }
      }
      progress.close();
    }
    return buf;
  }

  // Starts the worker thread for MOS conversion
  private void startConversion(boolean compressed)
  {
    exportCompressed = compressed;
    workerConvert = new SwingWorker<List<byte[]>, Void>() {
      @Override
      public List<byte[]> doInBackground()
      {
        List<byte[]> list = new Vector<byte[]>(1);
        try {
          byte[] buf = convertToMosV1(exportCompressed);
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
