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

/**
 * @deprecated
 * Replaced by MosResource2.
 */
@Deprecated
public final class MosResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private BufferedImage image;
  private JButton bfind, bexport2, bexport;
  private JPanel panel;
  private boolean compressed;

  public MosResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte data[] = entry.getResourceData();
    String signature = new String(data, 0, 4);
    new String(data, 4, 4); // Version
    if (signature.equalsIgnoreCase("MOSC")) {
      compressed = true;
      data = Compressor.decompress(data);
      new String(data, 0, 4); // Signature
      new String(data, 4, 4); // Version
    }
    else if (!signature.equalsIgnoreCase("MOS "))
      throw new Exception("Unsupported MOS file: " + signature);

    int width = (int)DynamicArray.getShort(data, 8);
    int height = (int)DynamicArray.getShort(data, 10);
    int columns = (int)DynamicArray.getShort(data, 12);
    int rows = (int)DynamicArray.getShort(data, 14);
    DynamicArray.getInt(data, 16); // Blocksize
    int paloffset = DynamicArray.getInt(data, 20);

    int offset = paloffset;
    Palette palettes[][] = new Palette[columns][rows];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < columns; x++) {
        palettes[x][y] = new Palette(data, offset, 256 * 4);
        offset += 256 * 4;
      }

    int tileoffsets[][] = new int[columns][rows];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < columns; x++) {
        tileoffsets[x][y] = DynamicArray.getInt(data, offset);
        offset += 4;
      }

    image = ColorConvert.createCompatibleImage(width, height, false);
    int xoff = 0, yoff = 0;
    for (int y = 0; y < rows; y++) {
      int h = Math.min(64, height - yoff);
      for (int x = 0; x < columns; x++) {
        int w = Math.min(64, width - xoff);
        for (int ty = yoff; ty < h + yoff; ty++)
          for (int tx = xoff; tx < w + xoff; tx++)
            image.setRGB(tx, ty, palettes[x][y].getColor((int)data[offset++]));
        xoff += 64;
      }
      xoff = 0;
      yoff += 64;
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bfind)
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    else if (event.getSource() == bexport2) {
      try {
        byte data[] = entry.getResourceData();
        if (compressed)
          data = Compressor.decompress(data);
        else
          data = Compressor.compress(data, "MOSC", "V1  ");
        ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                     panel.getTopLevelAncestor());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
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
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        compressed) {
      if (compressed) {
        bexport2 = new JButton("Decompress...", Icons.getIcon("Export16.gif"));
        bexport2.setMnemonic('d');
      }
      else {
        bexport2 = new JButton("Compress...", Icons.getIcon("Import16.gif"));
        bexport2.setMnemonic('c');
      }
      bexport2.addActionListener(this);
    }

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bfind);
    bpanel.add(bexport);
    if (bexport2 != null)
      bpanel.add(bexport2);

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
}

