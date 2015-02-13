// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.gui.ButtonPanel;
import infinity.gui.RenderCanvas;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.resource.other.UnknownResource;
import infinity.util.DynamicArray;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class PltResource implements Resource, ActionListener
{
  private static final ButtonPanel.Control CtrlColorList = ButtonPanel.Control.Custom1;

  private final ResourceEntry entry;
  private final byte[] buffer;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private RenderCanvas rcCanvas;
  private JPanel panel;
  private Resource externalResource;

  public PltResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    buffer = entry.getResourceData();

    // checking actual data type
    String s = new String(buffer, 0, 4);
    if (s.equals("PLT ")) {
      externalResource = null;
    } else if (s.equals("BAMC") || s.equals("BAM ")) {
      externalResource = new BamResource(entry);
    } else {
      externalResource = new UnknownResource(entry);
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(CtrlColorList) == event.getSource()) {
      rcCanvas.setImage(getImage());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.ExportButton) == event.getSource()) {
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
    if (externalResource == null) {
      JComboBox cbColorBMP = new JComboBox();
      cbColorBMP.addItem("None");
      List<ResourceEntry> bmps = ResourceFactory.getResources("BMP");
      for (int i = 0; i < bmps.size(); i++) {
        Object o = bmps.get(i);
        if (o.toString().startsWith("PLT"))
          cbColorBMP.addItem(o);
      }
      cbColorBMP.setEditable(false);
      cbColorBMP.setSelectedIndex(0);
      cbColorBMP.addActionListener(this);

      buttonPanel.addControl(new JLabel("Colors: "));
      buttonPanel.addControl(cbColorBMP, CtrlColorList);
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);

      rcCanvas = new RenderCanvas(getImage());
      JScrollPane scroll = new JScrollPane(rcCanvas);

      panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(scroll, BorderLayout.CENTER);
      panel.add(buttonPanel, BorderLayout.SOUTH);
      scroll.setBorder(BorderFactory.createLoweredBevelBorder());

      return panel;
    } else {
      return externalResource.makeViewer(container);
    }
  }

// --------------------- End Interface Viewable ---------------------

  private BufferedImage getImage()
  {
    Palette palette = null;
    Object item = ((JComboBox)buttonPanel.getControlByType(CtrlColorList)).getSelectedItem();
    if (!item.toString().equalsIgnoreCase("None")) {
      try {
        palette = new BmpResource((ResourceEntry)item).getPalette();
      } catch (Exception e) {
        e.printStackTrace();
        palette = null;
      }
    }
    new String(buffer, 0, 4); // Signature
    new String(buffer, 4, 4); // Version
    DynamicArray.getInt(buffer, 8); // Unknown 1
    DynamicArray.getInt(buffer, 12); // Unknown 2
    int width = DynamicArray.getInt(buffer, 16);
    int height = DynamicArray.getInt(buffer, 20);
    int offset = 24;
    BufferedImage image = ColorConvert.createCompatibleImage(width, height, false);
    for (int y = height - 1; y >= 0; y--) {
      for (int x = 0; x < width; x++) {
        short colorIndex = DynamicArray.getUnsignedByte(buffer, offset++);
        short paletteIndex = DynamicArray.getUnsignedByte(buffer, offset++);
        if (palette == null)
          image.setRGB(x, y, DynamicArray.getInt(new byte[]{(byte)colorIndex, (byte)colorIndex,
                                                            (byte)colorIndex, 0}, 0));
        else {
          short colors[] = palette.getColorBytes((int)paletteIndex);
          double factor = (double)colorIndex / 256.0;
          for (int i = 0; i < 3; i++)
            colors[i] = (short)((double)colors[i] * factor);
          image.setRGB(x, y, DynamicArray.getInt(new byte[]{(byte)colors[0],
                                                            (byte)colors[1],
                                                            (byte)colors[2], 0}, 0));
        }
      }
    }
    return image;
  }
}

