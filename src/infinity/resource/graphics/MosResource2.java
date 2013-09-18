// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MosResource2 implements Resource, ActionListener, Closeable
{
  private final ResourceEntry entry;
  private BufferedImage image;
  private MosDecoder decoder;
  private ButtonPopupMenu mnuExport;
  private JMenuItem miExport, miExport2, miExportBMP;
  private JButton bFind;
  private JPanel panel;
  private boolean compressed;

  public MosResource2(ResourceEntry entry)
  {
    this.entry = entry;

    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      blocker.setBlocked(true);
      decoder = new MosDecoder(entry);
      setImage();
      blocker.setBlocked(false);
    } catch (Exception e) {
      blocker.setBlocked(false);
      e.printStackTrace();
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bFind) {
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport2) {
      try {
        byte[] data = entry.getResourceData();
        if (compressed) {
          data = Compressor.decompress(data);
        } else {
          data = Compressor.compress(data, "MOSC", "V1  ");
        }
        ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                     panel.getTopLevelAncestor());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (event.getSource() == miExportBMP) {
      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream(image.getWidth()*image.getHeight()*3+256);
        String fileName = entry.toString().replace(".MOS", ".BMP");
        if (ImageIO.write(image, "bmp", os)) {
          ResourceFactory.getInstance().exportResource(entry,
              os.toByteArray(),
              fileName, panel.getTopLevelAncestor());
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Error while exporting " + entry, "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
        os.close();
        os = null;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    image = null;
    decoder = null;
    System.gc();
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    bFind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bFind.setMnemonic('f');
    bFind.addActionListener(this);

    miExport = new JMenuItem("original");
    miExport.setMnemonic('o');
    miExport.addActionListener(this);
    miExport2 = new JMenuItem("compress");
    if (compressed) {
      miExport2.setText("decompress");
      miExport2.setMnemonic('d');
    } else {
      miExport2.setText("compress");
      miExport2.setMnemonic('c');
    }
    miExport2.addActionListener(this);
    miExportBMP = new JMenuItem("as BMP");
    miExportBMP.setMnemonic('b');
    miExportBMP.addActionListener(this);

    mnuExport = new ButtonPopupMenu("Export...", new JMenuItem[]{miExport, miExport2, miExportBMP});
    mnuExport.setIcon(Icons.getIcon("Export16.gif"));
    mnuExport.setMnemonic('e');

    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BGEE) {
      miExport2.setEnabled(decoder.info().type() == MosDecoder.MosInfo.MosType.PALETTE);
    } else {
      miExport2.setEnabled(false);
    }

    JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(image)));
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(bFind);
    buttonPanel.add(mnuExport);

    panel = new JPanel(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.PAGE_END);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  public BufferedImage getImage()
  {
    return image;
  }

  public MosDecoder getDecoder()
  {
    return decoder;
  }

  private void setImage() throws Exception
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    if (decoder != null) {
      compressed = decoder.info().isCompressed();
      if (decoder.info().blockCount() > 0) {
        int blockCount = decoder.info().blockCount();
        ColorConvert.ColorFormat outputFormat = ColorConvert.ColorFormat.A8R8G8B8;
        image = new BufferedImage(decoder.info().width(), decoder.info().height(), BufferedImage.TYPE_INT_RGB);

        for (int blockIdx = 0; blockIdx < blockCount; blockIdx++) {
          MosDecoder.BlockInfo bi = decoder.info().blockInfo(blockIdx);
          int blockSize = bi.width()*bi.height();
          int[] block = new int[blockSize];
          // decoding block
          ColorConvert.BufferToColor(outputFormat, decoder.decodeBlock(blockIdx, outputFormat),
                                    0, block, 0, blockSize);

          // drawing block
          image.setRGB(bi.x(), bi.y(), bi.width(), bi.height(), block, 0, bi.width());
        }
        blocker.setBlocked(false);
      } else {
        blocker.setBlocked(false);
        throw new Exception("No image data available");
      }

    } else {
      blocker.setBlocked(false);
      throw new Exception("MOS decoder not initialized");
    }
  }

}
