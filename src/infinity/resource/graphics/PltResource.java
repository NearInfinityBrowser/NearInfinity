// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.other.UnknownResource;
import infinity.util.DynamicArray;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.List;

public final class PltResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private final byte[] buffer;
  private JButton bexport;
  private JComboBox cbColorBMP;
  private JLabel imageLabel;
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

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == cbColorBMP)
      imageLabel.setIcon(new ImageIcon(getImage()));
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
    if (externalResource == null) {
      cbColorBMP = new JComboBox();
      cbColorBMP.addItem("None");
      List<ResourceEntry> bmps = ResourceFactory.getInstance().getResources("BMP");
      for (int i = 0; i < bmps.size(); i++) {
        Object o = bmps.get(i);
        if (o.toString().startsWith("PLT"))
          cbColorBMP.addItem(o);
      }
      cbColorBMP.setEditable(false);
      cbColorBMP.setSelectedIndex(0);
      cbColorBMP.addActionListener(this);

      bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
      bexport.setMnemonic('e');
      bexport.addActionListener(this);
      imageLabel = new JLabel(new ImageIcon(getImage()));
      JScrollPane scroll = new JScrollPane(imageLabel);

      JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      bpanel.add(new JLabel("Colors: "));
      bpanel.add(cbColorBMP);
      bpanel.add(bexport);

      panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(scroll, BorderLayout.CENTER);
      panel.add(bpanel, BorderLayout.SOUTH);
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
    Object item = cbColorBMP.getSelectedItem();
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
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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

