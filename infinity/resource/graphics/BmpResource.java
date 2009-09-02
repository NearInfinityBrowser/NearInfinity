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

public final class BmpResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private BufferedImage image;
  private JButton bfind, bexport;
  private JPanel panel;
  private Palette palette;

  public BmpResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte[] data = entry.getResourceData();
    new String(data, 0, 2); // Signature
    Byteconvert.convertInt(data, 2); // Size
    Byteconvert.convertInt(data, 6); // Reserved
    int rasteroff = Byteconvert.convertInt(data, 10);

    Byteconvert.convertInt(data, 14); // Headersize
    int width = Byteconvert.convertInt(data, 18);
    int height = Byteconvert.convertInt(data, 22);
    Byteconvert.convertShort(data, 26); // Planes
    int bitcount = (int)Byteconvert.convertShort(data, 28);
    int compression = Byteconvert.convertInt(data, 30);
    if (compression != 0)
      throw new Exception("Compressed BMP files not supported");
    Byteconvert.convertInt(data, 34); // Comprsize
    Byteconvert.convertInt(data, 38); // Xpixprm
    Byteconvert.convertInt(data, 42); // Ypixprm
    Byteconvert.convertInt(data, 46); // Colorsused
    Byteconvert.convertInt(data, 50); // Colorsimp

    if (bitcount <= 8)
      palette = new Palette(data, 54, 4 * (int)Math.pow((double)2, (double)bitcount));

    int bytesprline = bitcount * width / 8;
    int padded = 4 - bytesprline % 4;
    if (padded == 4)
      padded = 0;

    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    int offset = rasteroff;
    for (int y = height - 1; y >= 0; y--) {
      setPixels(data, offset, bitcount, bytesprline, y, palette);
      offset += bytesprline + padded;
    }
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

  public BufferedImage getImage()
  {
    return image;
  }

  public Palette getPalette()
  {
    return palette;
  }

  private void setPixels(byte data[], int offset, int bitcount, int width, int y, Palette palette)
  {
    if (bitcount == 4) {
      int pix = 0;
      for (int x = 0; x < width; x++) {
        int color = (int)data[offset + x];
        if (color < 0)
          color += (int)Math.pow((double)2, (double)8);
        int color1 = color >> 4 & 0x0f;
        image.setRGB(pix++, y, palette.getColor(color1));
        int color2 = color & 0x0f;
        image.setRGB(pix++, y, palette.getColor(color2));
      }
    }
    else if (bitcount == 8) {
      for (int x = 0; x < width; x++)
        image.setRGB(x, y, palette.getColor((int)data[offset + x]));
    }
    else if (bitcount == 24) {
      for (int x = 0; x < width / 3; x++) {
        byte[] color = {data[offset + 3 * x], data[offset + 3 * x + 1], data[offset + 3 * x + 2], 0};
        image.setRGB(x, y, Byteconvert.convertInt(color, 0));
      }
    }
  }
}

