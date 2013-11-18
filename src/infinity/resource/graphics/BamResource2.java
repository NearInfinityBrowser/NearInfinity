// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

public class BamResource2 implements Resource, ActionListener
{
  private static final int ANIM_DELAY = 1000 / 15;    // 15 fps in milliseconds

  private final ResourceEntry entry;

  private BamDecoder decoder;
  private ButtonPopupMenu bpmExport;
  private JMenuItem miExport, miExportBAM, miExportBAMC, miExportFramesPNG;
  private JButton bFind, bPrevCycle, bNextCycle, bPrevFrame, bNextFrame;
  private JLabel lDisplay, lCycle, lFrame;
  private JToggleButton bPlay;
  private JPanel panel;
  private int curCycle, curFrame;
  private Timer timer;

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
      decoder.data().cycleSet(curCycle);
      if (timer != null && timer.isRunning() && decoder.data().cycleFrameCount() == 0) {
        timer.stop();
        bPlay.setSelected(false);
      }
      curFrame = 0;
      showFrame();
    } else if (event.getSource() == bNextCycle) {
      curCycle++;
      decoder.data().cycleSet(curCycle);
      if (timer != null && timer.isRunning() && decoder.data().cycleFrameCount() == 0) {
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
        curFrame = (curFrame + 1) % decoder.data().cycleFrameCount();
      } else {
        curFrame = (curFrame + 1) % decoder.data().frameCount();
      }
      showFrame();
    } else if (event.getSource() == miExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExportBAM) {
      try {
        byte data[] = Compressor.decompress(entry.getResourceData());
        ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                     panel.getTopLevelAncestor());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (event.getSource() == miExportBAMC) {
      try {
        byte data[] = Compressor.compress(entry.getResourceData(), "BAMC", "V1  ");
        ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                     panel.getTopLevelAncestor());
      } catch (Exception e) {
        e.printStackTrace();
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
    lDisplay = new JLabel("", JLabel.CENTER);

    bFind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bFind.setMnemonic('f');
    bFind.addActionListener(this);

    initDecoder();

    miExport = new JMenuItem("original");
    miExport.addActionListener(this);
    if (decoder.data() != null) {
      if (decoder.data().type() == BamDecoder.BamType.BAMC) {
        miExportBAM = new JMenuItem("decompressed");
        miExportBAM.addActionListener(this);
      } else if (decoder.data().type() == BamDecoder.BamType.BAMV1 &&
                 ResourceFactory.getGameID() != ResourceFactory.ID_TORMENT) {
        miExportBAMC = new JMenuItem("compressed");
        miExportBAMC.addActionListener(this);
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

    panel = new JPanel(new BorderLayout());
    panel.add(lDisplay, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    lDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
    showFrame();
    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  public Image getFrame(int frameIdx)
  {
    initDecoder();
    Image image = null;
    if (decoder != null && decoder.data() != null) {
      image = decoder.data().frameGet(frameIdx);
    }
    // must always return a valid image!
    if (image == null) {
      image = ColorConvert.createCompatibleImage(1, 1, true);
    }
    return image;
  }

  public int getFrameIndex(int cycleIdx, int frameIdx)
  {
    initDecoder();
    if (decoder != null && decoder.data() != null) {
      decoder.data().cycleSet(cycleIdx);
      int ret = decoder.data().cycleGetFrameIndexAbs(frameIdx);
      // restoring previous cycle setting
      decoder.data().cycleSet(curCycle);
      decoder.data().cycleSetFrameIndex(curFrame);
      return ret;
    } else {
      return 0;
    }
  }

  private void initDecoder()
  {
    if (decoder == null && entry != null) {
      decoder = new BamDecoder(entry);
    }
  }

  private void showFrame()
  {
    if (decoder != null && decoder.data() != null) {

      if (curCycle >= 0) {
        if (!decoder.data().cycleSetFrameIndex(curFrame)) {
          decoder.data().cycleReset();
          curFrame = 0;
        }
      }

      lDisplay.setText("");
      ImageIcon lastImage = (ImageIcon)lDisplay.getIcon();
      if (lastImage != null) {
        lastImage.getImage().flush();
      }
      lDisplay.setIcon(null);

      if (curCycle >= 0) {
        if (decoder.data().cycleGetFrame() == null) {
          lDisplay.setText("No image");
        } else {
          lDisplay.setIcon(new ImageIcon(decoder.data().cycleGetFrame()));
        }
      } else {
        if (decoder.data().frameGet(curFrame) == null) {
          lDisplay.setText("No image");
        } else {
          lDisplay.setIcon(new ImageIcon(decoder.data().frameGet(curFrame)));
        }
      }

      if (curCycle >= 0) {
        lCycle.setText(String.format("Cycle: %1$d/%2$d", curCycle+1, decoder.data().cycleCount()));
        lFrame.setText(String.format("Frame: %1$d/%2$d", curFrame+1, decoder.data().cycleFrameCount()));
      } else {
        lCycle.setText("All frames");
        lFrame.setText(String.format("Frame: %1$d/%2$d", curFrame+1, decoder.data().frameCount()));
      }

      bPrevCycle.setEnabled(curCycle > -1);
      bNextCycle.setEnabled(curCycle + 1 < decoder.data().cycleCount());
      bPrevFrame.setEnabled(curFrame > 0);
      if (curCycle >= 0) {
        bNextFrame.setEnabled(curFrame + 1 < decoder.data().cycleFrameCount());
        bPlay.setEnabled(decoder.data().cycleFrameCount() > 1);
      } else {
        bNextFrame.setEnabled(curFrame + 1 < decoder.data().frameCount());
        bPlay.setEnabled(decoder.data().frameCount() > 1);
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
    if (filePath.charAt(filePath.length() - 1) != File.separatorChar)
      filePath = filePath + File.separator;

    int max = 0, counter = 0, failCounter = 0;
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      blocker.setBlocked(true);
      if (decoder != null && decoder.data() != null) {
        max = decoder.data().frameCount();
        for (int i = 0; i < decoder.data().frameCount(); i++) {
          String fileIndex = String.format("%1$05d", i);
          Image image = decoder.data().frameGet(i);
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
        }
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
}
