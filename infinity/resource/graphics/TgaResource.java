// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;
import infinity.util.Byteconvert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

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
    int idlength = (int)Byteconvert.convertUnsignedByte(data, 0);
//    int colormaptype = Byteconvert.convertUnsignedByte(data, 1);
    int datatypecode = (int)Byteconvert.convertUnsignedByte(data, 2);
//    int colormaporigin = Byteconvert.convertUnsignedShort(data, 3);
//    int colormaplength = Byteconvert.convertUnsignedShort(data, 5);
//    int colormapdepth = Byteconvert.convertUnsignedByte(data, 7);
//    int x_origin = Byteconvert.convertUnsignedShort(data, 8);
//    int y_origin = Byteconvert.convertUnsignedShort(data, 10);
    int width = Byteconvert.convertUnsignedShort(data, 12);
    int height = Byteconvert.convertUnsignedShort(data, 14);
    int bitsperpixel = (int)Byteconvert.convertUnsignedByte(data, 16);
//    int imagedescriptor = Byteconvert.convertUnsignedByte(data, 17);

    if (datatypecode == 2) {
      int offset = 18 + idlength;
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      if (bitsperpixel == 24 || bitsperpixel == 32) {
        int bytesperpixel = bitsperpixel / 8;
        for (int y = height - 1; y >= 0; y--) {
          for (int x = 0; x < width; x++) {
            byte[] color = {data[offset], data[offset + 1], data[offset + 2], 0};
            image.setRGB(x, y, Byteconvert.convertInt(color, 0));
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
          image.setRGB(x, y, Byteconvert.convertInt(color, 0));
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
                rlecolor = Byteconvert.convertInt(color, 0);
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
              image.setRGB(x, y, Byteconvert.convertInt(color, 0));
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

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bfind)
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

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

