// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.infinity.gui.ButtonPanel;
import org.infinity.gui.RenderCanvas;
import org.infinity.resource.Referenceable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceSearcher;
import org.infinity.util.io.StreamUtils;

public final class GraphicsResource implements Resource, Referenceable, ActionListener
{
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private BufferedImage image;
  private JPanel panel;
  private Palette palette;

  public GraphicsResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    init();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES) == event.getSource()) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable()
  {
    return true;
  }

  @Override
  public void searchReferences(Component parent)
  {
    new ReferenceSearcher(entry, parent);
  }

//--------------------- End Interface Referenceable ---------------------

// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    RenderCanvas rcCanvas = new RenderCanvas(image);
    JScrollPane scroll = new JScrollPane(rcCanvas);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    ((JButton)buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
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

  private void init() throws Exception
  {
    ByteBuffer buffer = entry.getResourceBuffer();
    // Checking signature
    boolean isBMP = false;
    if ("BM".equals(StreamUtils.readString(buffer, 0, 2))) {
      isBMP = true;
    }

    image = null;
    if (isBMP) {
      int rasteroff = buffer.getInt(10);

      int width = buffer.getInt(18);
      int height = buffer.getInt(22);
      int bitcount = buffer.getShort(28);
      int compression = buffer.getInt(30);
      if ((compression == 0 || compression == 3) && bitcount <= 32) {
        int colsUsed = buffer.getInt(46); // Colorsused

        if (bitcount <= 8) {
          if (colsUsed == 0) {
            colsUsed = 1 << bitcount;
          }
          int palSize = 4 * colsUsed;
          palette = new Palette(buffer, rasteroff - palSize, palSize);
        }

        int bytesprline = bitcount * width / 8;
        int padded = 4 - bytesprline % 4;
        if (padded == 4) {
          padded = 0;
        }

        image = ColorConvert.createCompatibleImage(width, height, bitcount >= 32);
        int offset = rasteroff;
        for (int y = height - 1; y >= 0; y--) {
          setPixels(buffer, offset, bitcount, bytesprline, y, palette);
          offset += bytesprline + padded;
        }
      }
    }
    if (image == null) {
      try (InputStream is = entry.getResourceDataAsStream()) {
        image = ImageIO.read(is);
      } catch (Exception e) {
        image = null;
        throw new Exception("Unsupported graphics format");
      }
    }
  }

  private void setPixels(ByteBuffer buffer, int offset, int bitcount, int width, int y, Palette palette)
  {
    if (bitcount == 4) {
      int pix = 0;
      for (int x = 0; x < width; x++) {
        int color = buffer.get(offset + x) & 0xff;
        int color1 = (color >> 4) & 0x0f;
        image.setRGB(pix++, y, palette.getColor(color1));
        int color2 = color & 0x0f;
        image.setRGB(pix++, y, palette.getColor(color2));
      }
    }
    else if (bitcount == 8) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, palette.getColor(buffer.get(offset + x) & 0xff));
      }
    }
    else if (bitcount == 24) {
      for (int x = 0; x < width / 3; x++) {
        int rgb = (buffer.get(offset + 3*x + 2) & 0xff) << 16;
        rgb |= (buffer.get(offset + 3*x + 1) & 0xff) << 8;
        rgb |= buffer.get(offset + 3*x) & 0xff;
        image.setRGB(x, y, rgb);
      }
    }
    else if (bitcount == 32) {
      for (int x = 0; x < width / 4; x++) {
        int rgb = buffer.getInt(offset + 4*x);
        image.setRGB(x, y, rgb);
      }
    }
  }
}

