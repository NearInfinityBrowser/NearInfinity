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
import infinity.util.Byteconvert;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PvrzResource implements Resource, ItemListener
{
  private final ResourceEntry entry;
  private BufferedImage image;
  private PvrDecoder decoder;
  private ButtonPopupMenu bExport;
  private JMenuItem iExport, iBMP;
  private JPanel panel;

  public PvrzResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte[] data = entry.getResourceData();
    int size = Byteconvert.convertInt(data, 0);
    int marker = Byteconvert.convertShort(data, 4) & 0xffff;
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

//--------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bExport) {
      if (bExport.getSelectedItem() == iExport) {
        // export as original PVRZ
        ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
      } else if (bExport.getSelectedItem() == iBMP) {
        try {
          String fileName = entry.toString().replace(".PVRZ", ".BMP");
          ResourceFactory.getInstance().exportResource(entry,
                                                       decoder.decode(ColorConvert.ColorFormat.R8G8B8, true),
                                                       fileName,
                                                       panel.getTopLevelAncestor());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

//--------------------- End Interface ItemListener ---------------------

//--------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------


//--------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    iExport = new JMenuItem("original");
    iBMP = new JMenuItem("as BMP");
    bExport = new ButtonPopupMenu("Export...", new JMenuItem[]{iExport, iBMP});
    bExport.addItemListener(this);
    bExport.setIcon(Icons.getIcon("Export16.gif"));
    bExport.setMnemonic('e');
    JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(image)));

    JPanel bPanel = new JPanel();
    bPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    bPanel.add(bExport);

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
    ColorConvert.ColorFormat outputFormat = ColorConvert.ColorFormat.R8G8B8;
    byte[] imageData = decoder.decode(outputFormat, false);
    image = new BufferedImage(decoder.info().width(), decoder.info().height(), BufferedImage.TYPE_INT_RGB);
    int imgOfs = 0;
    int pixelSize = ColorConvert.ColorBits(outputFormat) >> 3;
    for (int y = 0; y < decoder.info().height(); y++) {
      for (int x = 0; x < decoder.info().width(); x++) {
        int color = ((imageData[imgOfs+2] & 0xff) << 16) |
                     ((imageData[imgOfs+1] & 0xff) << 8) |
                     (imageData[imgOfs] & 0xff);
        image.setRGB(x, y, color);
        imgOfs += pixelSize;
      }
    }
  }

}
