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
import infinity.resource.graphics.ColorConvert;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class PvrzResource implements Resource, ActionListener, Closeable
{
  private final ResourceEntry entry;
  private ButtonPopupMenu mnuExport;
  private JMenuItem miExport, miBMP;
  private JLabel lImage;
  private JPanel panel;

  public PvrzResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
  }

//--------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == miExport) {
      // export as original PVRZ
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miBMP) {
      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String fileName = entry.toString().replace(".PVRZ", ".BMP");
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

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    panel.removeAll();
    lImage.setIcon(null);
    lImage = null;
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    miExport = new JMenuItem("original");
    miExport.addActionListener(this);
    miBMP = new JMenuItem("as BMP");
    miBMP.addActionListener(this);
    mnuExport = new ButtonPopupMenu("Export...", new JMenuItem[]{miExport, miBMP});
    mnuExport.setIcon(Icons.getIcon("Export16.gif"));
    mnuExport.setMnemonic('e');
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

    JPanel bPanel = new JPanel();
    bPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    bPanel.add(mnuExport);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(bPanel, BorderLayout.SOUTH);
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
    if (entry != null) {
      try {
        byte[] data = entry.getResourceData();
        int size = DynamicArray.getInt(data, 0);
        int marker = DynamicArray.getUnsignedShort(data, 4);
        if ((size & 0xff) != 0x34 && marker != 0x9c78)
          throw new Exception("Invalid PVRZ resource");
        data = Compressor.decompress(data, 0);

        PvrDecoder decoder = new PvrDecoder(data);
        BufferedImage image = ColorConvert.createCompatibleImage(decoder.info().width(),
                                                                 decoder.info().height(), false);
        if (decoder.decode(image)) {
          decoder.close();
          return new ImageIcon(image);
        }
        decoder.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

}
