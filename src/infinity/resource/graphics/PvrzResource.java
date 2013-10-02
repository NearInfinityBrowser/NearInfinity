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

public class PvrzResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private BufferedImage image;
  private PvrDecoder decoder;
  private ButtonPopupMenu mnuExport;
  private JMenuItem miExport, miBMP;
  private JPanel panel;

  public PvrzResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte[] data = entry.getResourceData();
    int size = DynamicArray.getInt(data, 0);
    int marker = DynamicArray.getShort(data, 4) & 0xffff;
    if ((size & 0xff) != 0x34 && marker != 0x9c78)
      throw new Exception("Invalid PVRZ resource");

    data = Compressor.decompress(data, 0);
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);

    try {
      setImage(data);
    } catch (Exception e) {
      blocker.setBlocked(false);
      throw e;
    }

    blocker.setBlocked(false);
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
        if (ImageIO.write(image, "bmp", os)) {
          ResourceFactory.getInstance().exportResource(entry,
              os.toByteArray(), fileName, panel.getTopLevelAncestor());
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
    JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(image)));
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
    return image;
  }

  public PvrDecoder getDecoder()
  {
    return decoder;
  }

  private void setImage(byte[] buffer) throws Exception
  {
    decoder = new PvrDecoder(buffer);
    ColorConvert.ColorFormat outputFormat = ColorConvert.ColorFormat.A8R8G8B8;
    int imgWidth = decoder.info().width();
    int imgHeight = decoder.info().height();
    int imgSize = imgWidth*imgHeight;
    int[] block = new int[imgSize];
    ColorConvert.BufferToColor(outputFormat, decoder.decode(outputFormat), 0, block, 0, imgSize);
    image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, imgWidth, imgHeight, block, 0, imgWidth);
  }

}
