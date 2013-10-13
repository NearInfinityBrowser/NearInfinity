// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;
import infinity.util.DynamicArray;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class TgaResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private BufferedImage image;
  private JButton bfind, bexport;
  private JPanel panel;

  public TgaResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte[] data = entry.getResourceData();

    // Header
    int idlength = (int)DynamicArray.getUnsignedByte(data, 0);
//    int colormaptype = DynamicArray.getUnsignedByte(data, 1);
    int datatypecode = (int)DynamicArray.getUnsignedByte(data, 2);
//    int colormaporigin = DynamicArray.getUnsignedShort(data, 3);
//    int colormaplength = DynamicArray.getUnsignedShort(data, 5);
//    int colormapdepth = DynamicArray.getUnsignedByte(data, 7);
//    int x_origin = DynamicArray.getUnsignedShort(data, 8);
//    int y_origin = DynamicArray.getUnsignedShort(data, 10);
    int width = DynamicArray.getUnsignedShort(data, 12);
    int height = DynamicArray.getUnsignedShort(data, 14);
    int bitsperpixel = (int)DynamicArray.getUnsignedByte(data, 16);
//    int imagedescriptor = DynamicArray.getUnsignedByte(data, 17);

    if (datatypecode == 2) {
      int offset = 18 + idlength;
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      if (bitsperpixel == 24 || bitsperpixel == 32) {
        int bytesperpixel = bitsperpixel / 8;
        for (int y = height - 1; y >= 0; y--) {
          for (int x = 0; x < width; x++) {
            byte[] color = {data[offset], data[offset + 1], data[offset + 2], 0};
            image.setRGB(x, y, DynamicArray.getInt(color, 0));
            offset += bytesperpixel;
          }
        }
      }
      else
        throw new Exception(bitsperpixel + " bits per pixel not supported");
    }
    else if (datatypecode == 3) {
      int offset = 18 + idlength;
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          byte[] color = {data[offset], data[offset], data[offset], 0};
          image.setRGB(x, y, DynamicArray.getInt(color, 0));
          offset++;
        }
      }
    }
    else if (datatypecode == 10) {
      int offset = 18 + idlength;
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      if (bitsperpixel == 24 || bitsperpixel == 32) {
        int bytesperpixel = bitsperpixel / 8;
        int rlecount = 0, rawcount = 0, rlecolor = 0;
        for (int y = height - 1; y >= 0; y--) {
          for (int x = 0; x < width; x++) {
            if (rlecount == 0 && rawcount == 0) {
              byte packet = data[offset++];
              if (packet < 0) { // rle
                rlecount = (int)packet + 129;
                byte[] color = {data[offset], data[offset + 1], data[offset + 2], 0};
                rlecolor = DynamicArray.getInt(color, 0);
                offset += bytesperpixel;
              }
              else  // raw
                rawcount = (int)packet + 1;
            }
            if (rlecount > 0) {
              rlecount--;
              image.setRGB(x, y, rlecolor);
            }
            else if (rawcount > 0) {
              rawcount--;
              byte color[] = {data[offset], data[offset + 1], data[offset + 2], 0};
              image.setRGB(x, y, DynamicArray.getInt(color, 0));
              offset += bytesperpixel;
            }
          }
        }
      }
    }
    else
      throw new Exception("Datatype " + datatypecode + " not supported");
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bfind)
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bfind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bexport.setMnemonic('e');
    bexport.addActionListener(this);
    bfind.setMnemonic('r');
    bfind.addActionListener(this);
    JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(image)));

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bfind);
    bpanel.add(bexport);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

// --------------------- End Interface Viewable ---------------------
}

