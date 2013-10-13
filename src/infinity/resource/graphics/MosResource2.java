// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

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
import javax.swing.SwingConstants;

public class MosResource2 implements Resource, ActionListener, Closeable
{
  private final ResourceEntry entry;
  private MosDecoder.MosInfo.MosType mosType;
  private ButtonPopupMenu mnuExport;
  private JMenuItem miExport, miExport2, miExportBMP;
  private JButton bFind;
  private JLabel lImage;
  private JPanel panel;
  private boolean compressed;

  public MosResource2(ResourceEntry entry)
  {
    this.entry = entry;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String fileName = entry.toString().replace(".MOS", ".BMP");
        BufferedImage image = getImage();
        if (ImageIO.write(image, "bmp", os)) {
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
    lImage.setIcon(null);
    lImage = null;
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  @Override
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
      miExport2.setEnabled(mosType == MosDecoder.MosInfo.MosType.PALETTE);
    } else {
      miExport2.setEnabled(false);
    }

    lImage = new JLabel();
    lImage.setHorizontalAlignment(SwingConstants.CENTER);
    lImage.setVerticalAlignment(SwingConstants.CENTER);
    WindowBlocker.blockWindow(true);
    try {
      lImage.setIcon(loadImage());
      WindowBlocker.blockWindow(false);
    } catch (Exception e) {
      WindowBlocker.blockWindow(false);
    }
    JScrollPane scroll = new JScrollPane(lImage);
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
    if (lImage != null) {
      ImageIcon icon = (ImageIcon)lImage.getIcon();
      if (icon != null) {
        return ColorConvert.toBufferedImage(icon.getImage(), false);
      }
    } else if (entry != null) {
      return (BufferedImage)loadImage().getImage();
    }
    return null;
  }

  private ImageIcon loadImage()
  {
    ImageIcon icon = null;
    MosDecoder decoder = null;
    if (entry != null) {
      try {
        decoder = new MosDecoder(entry);
        compressed = decoder.info().isCompressed();
        mosType = decoder.info().type();
        BufferedImage image = ColorConvert.createCompatibleImage(decoder.info().width(),
                                                                 decoder.info().height(), false);
        if (decoder.decode(image)) {
          icon = new ImageIcon(image);
        }
        image = null;
        decoder.close();
      } catch (Exception e) {
        if (decoder != null)
          decoder.close();
        icon = null;
        e.printStackTrace();
      }
    }
    return icon;
  }

}
