// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.RenderCanvas;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;
import infinity.util.DynamicArray;
import infinity.util.IntegerHashMap;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class BamResource2 implements Resource, ActionListener, ItemListener, PropertyChangeListener
{
  private static final int ANIM_DELAY = 1000 / 10;    // 10 fps in milliseconds

  private static boolean transparencyEnabled = true;

  private final ResourceEntry entry;

  private BamDecoder decoder;
  private ButtonPopupMenu bpmExport;
  private JMenuItem miExport, miExportBAM, miExportBAMC, miExportFramesPNG;
  private JButton bFind, bPrevCycle, bNextCycle, bPrevFrame, bNextFrame;
  private RenderCanvas rcDisplay;
  private JLabel lCycle, lFrame;
  private JToggleButton bPlay;
  private JCheckBox cbTransparency;
  private JPanel panel;
  private int curCycle, curFrame;
  private Timer timer;
  private RootPaneContainer rpc;
  private SwingWorker<List<byte[]>, Void> workerConvert;
  private boolean exportCompressed;
  private WindowBlocker blocker;

  public BamResource2(ResourceEntry entry)
  {
    this.entry = entry;
  }


//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bPrevCycle) {
      curCycle--;
      decoder.cycleSet(curCycle);
      if (timer != null && timer.isRunning() && decoder.cycleFrameCount() == 0) {
        timer.stop();
        bPlay.setSelected(false);
      }
      curFrame = 0;
      showFrame();
    } else if (event.getSource() == bNextCycle) {
      curCycle++;
      decoder.cycleSet(curCycle);
      if (timer != null && timer.isRunning() && decoder.cycleFrameCount() == 0) {
        timer.stop();
        bPlay.setSelected(false);
      }
      curFrame = 0;
      showFrame();
    } else if (event.getSource() == bPrevFrame) {
      curFrame--;
      showFrame();
    } else if (event.getSource() == bNextFrame) {
      curFrame++;
      showFrame();
    } else if (event.getSource() == bPlay) {
      if (bPlay.isSelected()) {
        if (timer == null) {
          timer = new Timer(ANIM_DELAY, this);
        }
        timer.restart();
      } else {
        if (timer != null) {
          timer.stop();
        }
      }
    } else if (event.getSource() == bFind) {
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == timer) {
      if (curCycle >= 0) {
        curFrame = (curFrame + 1) % decoder.cycleFrameCount();
      } else {
        curFrame = (curFrame + 1) % decoder.frameCount();
      }
      showFrame();
    } else if (event.getSource() == miExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExportBAM) {
      if (decoder != null) {
        if (decoder.getType() == BamDecoder.Type.BAMV2) {
          // create new BAM V1 from scratch
          if (checkCompatibility(panel.getTopLevelAncestor())) {
            blocker = new WindowBlocker(rpc);
            blocker.setBlocked(true);
            startConversion(false);
          }
        } else {
          // decompress existing BAMC V1 and save as BAM V1
          try {
            byte data[] = Compressor.decompress(entry.getResourceData());
            ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } else if (event.getSource() == miExportBAMC) {
      if (decoder != null) {
        if (decoder.getType() == BamDecoder.Type.BAMV2) {
          // create new BAMC V1 from scratch
          if (checkCompatibility(panel.getTopLevelAncestor())) {
            blocker = new WindowBlocker(rpc);
            blocker.setBlocked(true);
            startConversion(true);
          }
        } else {
          // compress existing BAM V1 and save as BAMC V1
          try {
            byte data[] = Compressor.compress(entry.getResourceData(), "BAMC", "V1  ");
            ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } else if (event.getSource() == miExportFramesPNG) {
      JFileChooser fc = new JFileChooser(ResourceFactory.getRootDir());
      fc.setDialogTitle("Export BAM frames");
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fc.setSelectedFile(new File(fc.getCurrentDirectory(), entry.toString().replace(".BAM", ".PNG")));
      if (fc.showSaveDialog(panel.getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
        String filePath = fc.getSelectedFile().getParent();
        String fileName = fc.getSelectedFile().getName();
        String fileExt = null;
        int extIdx = fileName.lastIndexOf('.');
        if (extIdx > 0) {
          fileExt = fileName.substring(extIdx);
          fileName = fileName.substring(0, extIdx);
        } else {
          fileExt = "";
        }
        exportFrames(filePath, fileName, fileExt);
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == cbTransparency) {
      transparencyEnabled = cbTransparency.isSelected();
      if (decoder != null && decoder instanceof BamV1Decoder) {
        ((BamV1Decoder)decoder).setTransparencyEnabled(transparencyEnabled);
        showFrame();
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
        byte[] bamData = null;
        try {
          List<byte[]> l = workerConvert.get();
          if (l != null && !l.isEmpty()) {
            bamData = l.get(0);
            l.clear();
            l = null;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (bamData != null) {
          if (bamData.length > 0) {
            ResourceFactory.getInstance().exportResource(entry, bamData, entry.toString(),
                                                         panel.getTopLevelAncestor());
          } else {
            JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                          "Export has been cancelled." + entry, "Information",
                                          JOptionPane.INFORMATION_MESSAGE);
          }
          bamData = null;
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

//--------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    if (container instanceof RootPaneContainer) {
      rpc = (RootPaneContainer)container;
    } else {
      rpc = NearInfinity.getInstance();
    }

    WindowBlocker.blockWindow(true);
    try {
      decoder = BamDecoder.loadBam(entry);
      decoder.setMode(BamDecoder.Mode.Shared);
      if (decoder instanceof BamV1Decoder) {
        ((BamV1Decoder)decoder).setTransparencyEnabled(transparencyEnabled);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      decoder = null;
    }
    WindowBlocker.blockWindow(false);

    Dimension dim = (decoder != null) ? decoder.getSharedDimension() : new Dimension(1, 1);
    rcDisplay = new RenderCanvas(ColorConvert.createCompatibleImage(dim.width, dim.height, true));
    rcDisplay.setHorizontalAlignment(SwingConstants.CENTER);
    rcDisplay.setVerticalAlignment(SwingConstants.CENTER);

    bFind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bFind.setMnemonic('f');
    bFind.addActionListener(this);


    miExport = new JMenuItem("original");
    miExport.addActionListener(this);
    if (decoder != null) {
      if (decoder.getType() == BamDecoder.Type.BAMC) {
        miExportBAM = new JMenuItem("decompressed");
        miExportBAM.addActionListener(this);
      } else if (decoder.getType() == BamDecoder.Type.BAMV1 &&
                 ResourceFactory.getGameID() != ResourceFactory.ID_TORMENT) {
        miExportBAMC = new JMenuItem("compressed");
        miExportBAMC.addActionListener(this);
      } else if (decoder.getType() == BamDecoder.Type.BAMV2) {
        miExportBAM = new JMenuItem("as BAM V1 (uncompressed)");
        miExportBAM.addActionListener(this);
        miExportBAM.setEnabled(decoder.frameCount() < 65536 && decoder.cycleCount() < 256);
        miExportBAMC = new JMenuItem("as BAM V1 (compressed)");
        miExportBAMC.addActionListener(this);
        miExportBAMC.setEnabled(decoder.frameCount() < 65536 && decoder.cycleCount() < 256);
      }
      miExportFramesPNG = new JMenuItem("all frames as PNG");
      miExportFramesPNG.addActionListener(this);
    }

    List<JMenuItem> list = new ArrayList<JMenuItem>();
    if (miExport != null) {
      list.add(miExport);
    }
    if (miExportBAM != null) {
      list.add(miExportBAM);
    }
    if (miExportBAMC != null) {
      list.add(miExportBAMC);
    }
    if (miExportFramesPNG != null) {
      list.add(miExportFramesPNG);
    }
    JMenuItem[] mi = new JMenuItem[list.size()];
    for (int i = 0; i < mi.length; i++) {
      mi[i] = list.get(i);
    }
    bpmExport = new ButtonPopupMenu("Export...", mi);
    bpmExport.setIcon(Icons.getIcon("Export16.gif"));
    bpmExport.setMnemonic('e');

    bPlay = new JToggleButton("Play", Icons.getIcon("Play16.gif"));
    bPlay.addActionListener(this);

    lCycle = new JLabel("", JLabel.CENTER);
    bPrevCycle = new JButton(Icons.getIcon("Back16.gif"));
    bPrevCycle.setMargin(new Insets(bPrevCycle.getMargin().top, 2, bPrevCycle.getMargin().bottom, 2));
    bPrevCycle.addActionListener(this);
    bNextCycle = new JButton(Icons.getIcon("Forward16.gif"));
    bNextCycle.setMargin(bPrevCycle.getMargin());
    bNextCycle.addActionListener(this);

    lFrame = new JLabel("", JLabel.CENTER);
    bPrevFrame = new JButton(Icons.getIcon("Back16.gif"));
    bPrevFrame.setMargin(bPrevCycle.getMargin());
    bPrevFrame.addActionListener(this);
    bNextFrame = new JButton(Icons.getIcon("Forward16.gif"));
    bNextFrame.setMargin(bPrevCycle.getMargin());
    bNextFrame.addActionListener(this);

    cbTransparency = new JCheckBox("Enable transparency", transparencyEnabled);
    if (decoder != null) {
      cbTransparency.setEnabled(decoder.getType() != BamDecoder.Type.BAMV2);
    }
    cbTransparency.setToolTipText("Affects only legacy BAM resources (BAM v1).");
    cbTransparency.addItemListener(this);
    JPanel optionsPanel = new JPanel();
    BoxLayout bl = new BoxLayout(optionsPanel, BoxLayout.Y_AXIS);
    optionsPanel.setLayout(bl);
    optionsPanel.add(cbTransparency);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(lCycle);
    buttonPanel.add(bPrevCycle);
    buttonPanel.add(bNextCycle);
    buttonPanel.add(lFrame);
    buttonPanel.add(bPrevFrame);
    buttonPanel.add(bNextFrame);
    buttonPanel.add(bPlay);
    buttonPanel.add(bFind);
    buttonPanel.add(bpmExport);
    buttonPanel.add(optionsPanel);

    panel = new JPanel(new BorderLayout());
    panel.add(rcDisplay, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    rcDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
    showFrame();
    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  public int getFrameCount()
  {
    int retVal = 0;
    if (decoder != null ) {
      retVal = decoder.frameCount();
    }
    return retVal;
  }

  public int getFrameCount(int cycleIdx)
  {
    int retVal = 0;
    if (decoder != null) {
      if (cycleIdx >= 0 && cycleIdx < decoder.cycleCount()) {
        int cycle = decoder.cycleGet();
        int frame = decoder.cycleGetFrameIndex();
        decoder.cycleSet(cycleIdx);
        retVal = decoder.cycleFrameCount();
        decoder.cycleSet(cycle);
        decoder.cycleSetFrameIndex(frame);
      }
    }
    return retVal;
  }

  public int getCycleCount()
  {
    int retVal = 0;
    if (decoder != null) {
      retVal = decoder.cycleCount();
    }
    return retVal;
  }

  public Image getFrame(int frameIdx)
  {
    Image image = null;
    if (decoder != null) {
      image = decoder.frameGet(frameIdx);
    }
    // must always return a valid image!
    if (image == null) {
      image = ColorConvert.createCompatibleImage(1, 1, true);
    }
    return image;
  }

  public int getFrameIndex(int cycleIdx, int frameIdx)
  {
    if (decoder != null) {
      decoder.cycleSet(cycleIdx);
      int ret = decoder.cycleGetFrameIndexAbsolute(frameIdx);
      // restoring previous cycle setting
      decoder.cycleSet(curCycle);
      decoder.cycleSetFrameIndex(curFrame);
      return ret;
    } else {
      return 0;
    }
  }

  public Point getFrameCenter(int frameIdx)
  {
    Point p = new Point();
    if (decoder != null) {
      p.x = decoder.frameCenterX(frameIdx);
      p.y = decoder.frameCenterY(frameIdx);
    }
    return p;
  }

  private void showFrame()
  {
    if (decoder != null) {

      if (curCycle >= 0) {
        if (!decoder.cycleSetFrameIndex(curFrame)) {
          decoder.cycleReset();
          curFrame = 0;
        }
      }

      Image image = rcDisplay.getImage();
      if (curCycle >= 0) {
        decoder.cycleGetFrame(image);
      } else {
        decoder.frameGet(curFrame, image);
      }
      rcDisplay.repaint();

      if (curCycle >= 0) {
        lCycle.setText(String.format("Cycle: %1$d/%2$d", curCycle+1, decoder.cycleCount()));
        lFrame.setText(String.format("Frame: %1$d/%2$d", curFrame+1, decoder.cycleFrameCount()));
      } else {
        lCycle.setText("All frames");
        lFrame.setText(String.format("Frame: %1$d/%2$d", curFrame+1, decoder.frameCount()));
      }

      bPrevCycle.setEnabled(curCycle > -1);
      bNextCycle.setEnabled(curCycle + 1 < decoder.cycleCount());
      bPrevFrame.setEnabled(curFrame > 0);
      if (curCycle >= 0) {
        bNextFrame.setEnabled(curFrame + 1 < decoder.cycleFrameCount());
        bPlay.setEnabled(decoder.cycleFrameCount() > 1);
      } else {
        bNextFrame.setEnabled(curFrame + 1 < decoder.frameCount());
        bPlay.setEnabled(decoder.frameCount() > 1);
      }

    } else {
      bPlay.setEnabled(false);
      bPrevCycle.setEnabled(false);
      bNextCycle.setEnabled(false);
      bPrevFrame.setEnabled(false);
      bNextFrame.setEnabled(false);
    }
  }

  // Exports frames as PNGs
  private void exportFrames(String filePath, String fileBase, String fileExt)
  {
    if (filePath == null)
      filePath = ".";

    int max = 0, counter = 0, failCounter = 0;
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      blocker.setBlocked(true);
      if (decoder != null) {
        BamDecoder.Mode oldMode = decoder.getMode();
        decoder.setMode(BamDecoder.Mode.Individual);
        max = decoder.frameCount();
        for (int i = 0; i < decoder.frameCount(); i++) {
          String fileIndex = String.format("%1$05d", i);
          Image image = decoder.frameGet(i);
          if (image != null) {
            try {
              ImageIO.write(ColorConvert.toBufferedImage(image, true), "png",
                            new File(filePath, fileBase + fileIndex + fileExt));
              counter++;
            } catch (IOException e) {
              failCounter++;
              System.err.println("Error writing frame #" + i);
            }
          } else {
            failCounter++;
          }
          image.flush();
          image = null;
        }
        decoder.setMode(oldMode);
      }
    } catch (Throwable t) {
    }
    blocker.setBlocked(false);

    // displaying results
    String msg = null;
    if (failCounter == 0 && counter == max) {
      msg = String.format("All %1$d frames exported successfully.", max);
    } else {
      msg = String.format("%2$d/%1$d frames exported.\n%3$d/%1$d frames not exported.",
                          max, counter, failCounter);
    }
    JOptionPane.showMessageDialog(panel.getTopLevelAncestor(), msg, "Information",
                                  JOptionPane.INFORMATION_MESSAGE);
  }

  // Checks current BAM (V2 only) for compatibility and shows an appropriate warning or error message
  private boolean checkCompatibility(Component parent)
  {
    if (parent == null)
      parent = NearInfinity.getInstance();

    if (decoder != null && decoder.getType() == BamDecoder.Type.BAMV2) {
      // Compatibility: 0=OK, 1=issues, but conversion is possible, 2=conversion is not possible
      int compatibility = 0;
      int numFrames = decoder.frameCount();
      int numCycles = decoder.cycleCount();
      boolean hasSemiTrans = false;
      int maxWidth = 0, maxHeight = 0;
      List<String> issues = new ArrayList<String>(10);

      // checking for issues
      for (int i = 0; i < numFrames; i++) {
        BufferedImage img = ColorConvert.toBufferedImage(decoder.frameGet(i), true);
        if (img != null) {
          maxWidth = Math.max(maxWidth, img.getWidth());
          maxHeight = Math.max(maxHeight, img.getHeight());
          if (hasSemiTrans == false) {
            int[] data = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
            for (int j = 0; j < data.length; j++) {
              int a = (data[j] >>> 24) & 0xff;
              if (a >= 0x20 && a <= 0xA0) {
                hasSemiTrans = true;
                break;
              }
            }
          }
        }
      }

      // number of frames > 65535
      if (numFrames > 65535) {
        issues.add("- [critical] Number of frames exceeds the supported maximum of 65535 frames.");
        compatibility = Math.max(compatibility, 2);
      }

      // number of cycles > 255
      if (numCycles > 255) {
        issues.add("- [severe] Number of cycles exceeds the supported maximum of 255 cycles. Excess cycles will be cut off.");
        compatibility = Math.max(compatibility, 1);
      }

      // frame dimensions > 256 pixels
      if (maxWidth > 256 || maxHeight > 256) {
        issues.add("- [severe] One or more frames are greater than 256x256 pixels. Those frames may not be visible in certain games.");
        compatibility = Math.max(compatibility, 1);
      }

      // semi-transparent pixels
      if (hasSemiTrans) {
        issues.add("- [medium] One or more frames contain semi-transparent pixels. The transparency information will be lost.");
        compatibility = Math.max(compatibility, 1);
      }

      if (compatibility == 0) {
        return true;
      } else if (compatibility == 1) {
        StringBuilder sb = new StringBuilder();
        sb.append("The BAM resource is not fully compatible with the legacy BAM V1 format:\n");
        for (final String s: issues) {
          sb.append(s);
          sb.append("\n");
        }
        sb.append("\n");
        sb.append("Do you still want to continue?");
        int retVal = JOptionPane.showConfirmDialog(parent, sb.toString(), "Warning",
                                                   JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return (retVal == JOptionPane.YES_OPTION);
      } else {
        StringBuilder sb = new StringBuilder();
        sb.append("The BAM resource is not compatible with the legacy BAM V1 format because of the following issues:\n");
        for (final String s: issues) {
          sb.append(s);
          sb.append("\n");
        }
        JOptionPane.showMessageDialog(parent, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }
    return false;
  }

  // Creates a new BAM V1 or BAMC V1 resource from scratch. DO NOT call directly!
  private byte[] convertToBamV1(boolean compressed) throws Exception
  {
    if (decoder != null) {
      BamDecoder.Mode oldMode = decoder.getMode();
      decoder.setMode(BamDecoder.Mode.Individual);
      // max. supported number of frames and cycles
      int frameCount = Math.min(decoder.frameCount(), 65535);
      int cycleCount = Math.min(decoder.cycleCount(), 255);

      // 1. calculating global palette for all frames
      final int transThreshold = 0x20;
      boolean[] frameTransparency = new boolean[frameCount];
      boolean hasTransparency = false;
      int totalWidth = 0, totalHeight = 0;
      for (int i = 0; i < frameCount; i++) {
        BufferedImage img = ColorConvert.toBufferedImage(decoder.frameGet(i), true);
        // getting max. dimensions
        if (img != null) {
          if (img.getHeight() > totalHeight)
            totalHeight = img.getHeight();
          totalWidth += img.getWidth();
          // frame uses transparent pixels?
          int[] data = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
          frameTransparency[i] = false;
          for (int j = 0; j < data.length; j++) {
            if (((data[j] >>> 24) & 0xff) < transThreshold) {
              frameTransparency[i] = true;
              hasTransparency = true;
              break;
            }
          }
        }
      }
      // creating global palette for all available frames
      BufferedImage composedImage = ColorConvert.createCompatibleImage(totalWidth, totalHeight, true);
      Graphics2D g = (Graphics2D)composedImage.getGraphics();
      for (int i = 0, w = 0; i < frameCount; i++) {
        BufferedImage img = ColorConvert.toBufferedImage(decoder.frameGet(i), true);
        if (img != null) {
          g.drawImage(img, w, 0, null);
          w += img.getWidth();
        }
      }
      g.dispose();
      int[] chainedImageData = ((DataBufferInt)composedImage.getRaster().getDataBuffer()).getData();
      int[] palette = ColorConvert.medianCut(chainedImageData, hasTransparency ? 255 : 256, false);
      int[] hclPalette = new int[palette.length];
      ColorConvert.toHclPalette(palette, hclPalette);
      // initializing color cache
      IntegerHashMap<Byte> colorCache = new IntegerHashMap<Byte>(1536);
      for (int i = 0; i < palette.length; i++) {
        colorCache.put(palette[i] & 0x00ffffff, (byte)i);
      }
      // adding transparent color index to the palette if available
      if (hasTransparency) {
        int[] tmp = palette;
        palette = new int[tmp.length + 1];
        palette[0] = 0x00ff00;    // it's usually defined as RGB(0, 255, 0)
        System.arraycopy(tmp, 0, palette, 1, tmp.length);
        tmp = null;
      }

      // 2. encoding frames
      List<byte[]> frameList = new ArrayList<byte[]>(frameCount);
      int colorShift = hasTransparency ? 1 : 0;   // considers transparent color index
      for (int i = 0; i < frameCount; i++) {
        if (decoder.frameGet(i) != null) {
          BufferedImage img = ColorConvert.toBufferedImage(decoder.frameGet(i), true);
          int[] srcData = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
          if (frameTransparency[i] == true) {
            // do RLE encoding (on transparent pixels only)
            byte[] dstData = new byte[img.getWidth()*img.getHeight() + (img.getWidth()*img.getHeight() + 1)/2];
            int srcIdx = 0, dstIdx = 0, srcMax = img.getWidth()*img.getHeight();
            while (srcIdx < srcMax) {
              if (((srcData[srcIdx] >>> 24) & 0xff) < transThreshold) {
                // transparent pixel
                int cnt = 0;
                srcIdx++;
                while (srcIdx < srcMax && cnt < 255 && ((srcData[srcIdx] >>> 24) & 0xff) < transThreshold) {
                  cnt++;
                  srcIdx++;
                }
                dstData[dstIdx++] = 0;
                dstData[dstIdx++] = (byte)cnt;
              } else {
                // visible pixel
                Byte colIdx = colorCache.get(srcData[srcIdx] & 0x00ffffff);
                if (colIdx != null) {
                  dstData[dstIdx++] = (byte)(colIdx + colorShift);
                } else {
                  int color = ColorConvert.nearestColor(srcData[srcIdx], hclPalette);
                  dstData[dstIdx++] = (byte)(color + colorShift);
                  colorCache.put(srcData[srcIdx] & 0x00ffffff, (byte)color);
                }
                srcIdx++;
              }
            }
            // truncating byte array
            byte[] tmp = dstData;
            dstData = new byte[dstIdx];
            System.arraycopy(tmp, 0, dstData, 0, dstData.length);
            tmp = null;
            frameList.add(dstData);
          } else {
            // storing uncompressed pixel data
            byte[] dstData = new byte[img.getWidth()*img.getHeight()];
            int idx = 0, max = dstData.length;
            while (idx < max) {
              Byte colIdx = colorCache.get(srcData[idx] & 0x00ffffff);
              if (colIdx != null) {
                dstData[idx] = (byte)(colIdx + colorShift);
              } else {
                int color = ColorConvert.nearestColor(srcData[idx], hclPalette);
                dstData[idx] = (byte)(color + colorShift);
                colorCache.put(srcData[idx] & 0x00ffffff, (byte)color);
              }
              idx++;
            }
            frameList.add(dstData);
          }
        } else {
          frameList.add(new byte[]{});
        }
      }

      // 3. creating header structures
      final int frameEntrySize = 0x0c;
      final int cycleEntrySize = 0x04;
      final int paletteSize = 0x400;
      final int lookupTableEntrySize = 0x02;
      // main header
      byte[] header = new byte[0x18];
      DynamicArray buf = DynamicArray.wrap(header, DynamicArray.ElementType.BYTE);
      buf.put(0x00, "BAM V1  ".getBytes(Charset.forName("US-ASCII")));
      buf.putShort(0x08, (short)frameCount);
      buf.putByte(0x0a, (byte)cycleCount);
      buf.putByte(0x0b, (byte)0);   // compressed color index
      int frameEntryOfs = 0x18;
      int paletteOfs = frameEntryOfs + frameCount*frameEntrySize + cycleCount*cycleEntrySize;
      int lookupTableOfs = paletteOfs + paletteSize;
      buf.putInt(0x0c, frameEntryOfs);
      buf.putInt(0x10, paletteOfs);
      buf.putInt(0x14, lookupTableOfs);
      // cycle entries
      byte[] cycleEntryHeader = new byte[cycleCount*cycleEntrySize];
      buf = DynamicArray.wrap(cycleEntryHeader, DynamicArray.ElementType.BYTE);
      int oldCycle = decoder.cycleGet();
      int oldCycleFrame = decoder.cycleGetFrameIndex();
      for (int i = 0; i < cycleCount; i++) {
        decoder.cycleSet(i);
        buf.putShort(0x00, (short)decoder.cycleFrameCount());
        buf.putShort(0x02, (short)decoder.cycleGetFrameIndexAbsolute(0));
        buf.addToBaseOffset(cycleEntrySize);
      }
      decoder.cycleSet(oldCycle);
      decoder.cycleSetFrameIndex(oldCycleFrame);
      // frame entries and frame lookup table
      byte[] frameEntryHeader = new byte[frameCount*frameEntrySize];
      buf = DynamicArray.wrap(frameEntryHeader, DynamicArray.ElementType.BYTE);
      byte[] lookupTableHeader = new byte[frameCount*lookupTableEntrySize];
      DynamicArray buf2 = DynamicArray.wrap(lookupTableHeader, DynamicArray.ElementType.BYTE);
      int dataOfs = lookupTableOfs + frameCount*lookupTableEntrySize;
      for (int i = 0; i < frameCount; i++) {
        buf.putShort(0x00, (short)decoder.frameWidth(i));
        buf.putShort(0x02, (short)decoder.frameHeight(i));
        buf.putShort(0x04, (short)decoder.frameCenterX(i));
        buf.putShort(0x06, (short)decoder.frameCenterY(i));
        int ofs = dataOfs & 0x7fffffff;
        if (frameTransparency[i] == false)
          ofs |= 0x80000000;
        buf.putInt(0x08, ofs);
        dataOfs += frameList.get(i).length;
        buf.addToBaseOffset(frameEntrySize);

        buf2.putShort(0x00, (short)i);
        buf2.addToBaseOffset(lookupTableEntrySize);
      }

      // 4. putting it all together
      int bamSize = dataOfs;    // dataOfs should now point to the end of the data
      int bamOfs = 0;
      byte[] bamArray = new byte[bamSize];
      System.arraycopy(header, 0, bamArray, bamOfs, header.length);
      bamOfs += header.length;
      System.arraycopy(frameEntryHeader, 0, bamArray, bamOfs, frameEntryHeader.length);
      bamOfs += frameEntryHeader.length;
      System.arraycopy(cycleEntryHeader, 0, bamArray, bamOfs, cycleEntryHeader.length);
      bamOfs += cycleEntryHeader.length;
      for (int i = 0; i < palette.length; i++) {
        System.arraycopy(DynamicArray.convertInt(palette[i]), 0, bamArray, bamOfs, 4);
        bamOfs += 4;
      }
      System.arraycopy(lookupTableHeader, 0, bamArray, bamOfs, lookupTableHeader.length);
      bamOfs += lookupTableHeader.length;
      for (int i = 0; i < frameList.size(); i++) {
        byte[] frame = frameList.get(i);
        if (frame != null) {
          System.arraycopy(frame, 0, bamArray, bamOfs, frame.length);
          bamOfs += frame.length;
        }
      }
      frameList.clear(); frameList = null;
      colorCache.clear(); colorCache = null;
      palette = null; hclPalette = null;

      // optionally compressing to MOSC V1
      if (compressed) {
        if (bamArray != null) {
          bamArray = Compressor.compress(bamArray, "BAMC", "V1  ");
        }
      }

      decoder.setMode(oldMode);
      return bamArray;
    }
    return null;
  }

  // Starts the worker thread for BAM conversion
  private void startConversion(boolean compressed)
  {
    exportCompressed = compressed;
    workerConvert = new SwingWorker<List<byte[]>, Void>() {
      @Override
      public List<byte[]> doInBackground()
      {
        List<byte[]> list = new Vector<byte[]>(1);
        try {
          byte[] buf = convertToBamV1(exportCompressed);
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
