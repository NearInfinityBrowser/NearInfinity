// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.infinity.gui.ButtonPanel;
import org.infinity.gui.RenderCanvas;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;

public final class PltResource implements Resource, ActionListener
{
  private static final ButtonPanel.Control CtrlColorList = ButtonPanel.Control.CUSTOM_1;

  private final ResourceEntry entry;
  private final ByteBuffer buffer;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private RenderCanvas rcCanvas;
  private JPanel panel;

  public PltResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    buffer = entry.getResourceBuffer();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(CtrlColorList) == event.getSource()) {
      rcCanvas.setImage(getImage());
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


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    JComboBox<Object> cbColorBMP = new JComboBox<>();
    cbColorBMP.addItem("None");
    List<ResourceEntry> bmps = ResourceFactory.getResources("BMP");
    for (final ResourceEntry re: bmps) {
      if (re.getResourceName().startsWith("PLT")) {
        cbColorBMP.addItem(re);
      }
    }
    cbColorBMP.setEditable(false);
    cbColorBMP.setSelectedIndex(0);
    cbColorBMP.addActionListener(this);

    buttonPanel.addControl(new JLabel("Colors: "));
    buttonPanel.addControl(cbColorBMP, CtrlColorList);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);

    rcCanvas = new RenderCanvas(getImage());
    JScrollPane scroll = new JScrollPane(rcCanvas);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------

  private BufferedImage getImage()
  {
    Palette palette = null;
    Object item = ((JComboBox<?>)buttonPanel.getControlByType(CtrlColorList)).getSelectedItem();
    if (!item.toString().equalsIgnoreCase("None")) {
      try {
        palette = new GraphicsResource((ResourceEntry)item).getPalette();
      } catch (Exception e) {
        e.printStackTrace();
        palette = null;
      }
    }
    int width = buffer.getInt(16);
    int height = buffer.getInt(20);
    int offset = 24;
    BufferedImage image = ColorConvert.createCompatibleImage(width, height, true);
    for (int y = height - 1; y >= 0; y--) {
      for (int x = 0; x < width; x++) {
        short colorIndex = (short)(buffer.get(offset++) & 0xff);
        short paletteIndex = (short)(buffer.get(offset++) & 0xff);
        if (palette == null) {
          short alpha = (short)((colorIndex == 255) ? 0 : 255);
          image.setRGB(x, y, DynamicArray.getInt(new byte[]{(byte)colorIndex, (byte)colorIndex,
                                                            (byte)colorIndex, (byte)alpha}, 0));
        } else {
          short colors[] = palette.getColorBytes((int)paletteIndex);
          double factor = (double)colorIndex / 256.0;
          for (int i = 0; i < 3; i++) {
            colors[i] = (short)((double)colors[i] * factor);
          }
          colors[3] = (short)((colors[0] == 0 && colors[1] >= 254 && colors[2] == 0) ? 0 : 255);
          image.setRGB(x, y, DynamicArray.getInt(new byte[]{(byte)colors[0],
                                                            (byte)colors[1],
                                                            (byte)colors[2],
                                                            (byte)colors[3]}, 0));
        }
      }
    }
    return image;
  }
}

