// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.datatype.DecNumber;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;
import infinity.util.Byteconvert;
import infinity.util.Filereader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public final class TisResource implements Resource, ActionListener, Closeable
{
  private static final int ROWS = 5, COLS = 7;
  private final ResourceEntry entry;
  private JButton bnext, bprev, bexport;
  private JLabel labels[];
  private JLabel imageLabels[];
  private JPanel panel;
  private byte imagedata[];
  private int currentnr, tilesize, tileoffset;

//  public BufferedImage drawImage(int width, int height, int mapIndex, int lookupIndex, Overlay overlay)
//  {
//    BufferedImage image = new BufferedImage(width * tilesize, height * tilesize, BufferedImage.TYPE_INT_RGB);
//    for (int xpos = 0; xpos < width; xpos++) {
//      for (int ypos = height - 1; ypos >= 0; ypos--) {
//        AbstractStruct wedtilemap = (AbstractStruct)overlay.getStructEntryAt(ypos * width + xpos + mapIndex);
//        int lookup = ((DecNumber)wedtilemap.getAttribute("Primary tile index")).getValue();
//        int tilenum = ((DecNumber)overlay.getStructEntryAt(lookup + lookupIndex)).getValue();
//        int paletteOffset = tileoffset + tilenum * (tilesize * tilesize + 4 * 256);
//        int dataOffset = paletteOffset + 4 * 256;
//        for (int y = 0; y < tilesize; y++)
//          for (int x = 0; x < tilesize; x++)
//            image.setRGB(xpos * tilesize + x, ypos * tilesize + y,
//                         Palette.getColor(imagedata, paletteOffset, imagedata[dataOffset++]));
//      }
//    }
//    return image;
//  }

  public static BufferedImage drawImage(ResourceEntry entry, int width, int height, int mapIndex,
                                        int lookupIndex, Overlay overlay) throws Exception
  {
    InputStream is = entry.getResourceDataAsStream();
    int tilesize;
    byte tileBuffer[];

    byte signature[] = Filereader.readBytes(is, 4);
    if (new String(signature).equalsIgnoreCase("TIS ")) {
      Filereader.readString(is, 4); // Version
      Filereader.readInt(is); // Tilecount
      Filereader.readInt(is); // Unknown
      Filereader.readInt(is); // TileOffset
      tilesize = Filereader.readInt(is);
      tileBuffer = new byte[tilesize * tilesize + 4 * 256];
      Filereader.readBytes(is, tileBuffer);
    }
    else {
      tilesize = 64;
      tileBuffer = new byte[tilesize * tilesize + 4 * 256];
      System.arraycopy(signature, 0, tileBuffer, 0, 4);
      Filereader.readBytes(is, tileBuffer, 4, tileBuffer.length - 4);
    }

    List<TileInfo> tiles = new ArrayList<TileInfo>(width * height);
    for (int xpos = 0; xpos < width; xpos++) {
      for (int ypos = 0; ypos < height; ypos++) {
        AbstractStruct wedtilemap = (AbstractStruct)overlay.getStructEntryAt(ypos * width + xpos + mapIndex);
        int lookup = ((DecNumber)wedtilemap.getAttribute("Primary tile index")).getValue();
        int tilenum = ((DecNumber)overlay.getStructEntryAt(lookup + lookupIndex)).getValue();
        tiles.add(new TileInfo(xpos, ypos, tilenum));
      }
    }

    Collections.sort(tiles);
    BufferedImage image = new BufferedImage(width * tilesize, height * tilesize, BufferedImage.TYPE_INT_RGB);
    TileInfo lastTile = null;
    for (int i = 0; i < tiles.size(); i++) {
      TileInfo tile = tiles.get(i);
      if (lastTile != null && tile.tilenum != lastTile.tilenum + 1) {
        for (int j = 0; j < tile.tilenum - lastTile.tilenum - 1; j++)
          Filereader.readBytes(is, tileBuffer);
      }
      if (lastTile != null && tile.tilenum == lastTile.tilenum) { // Copy data
        System.out.println("TisResource.drawImage: copy");
        for (int y = 0; y < tilesize; y++)
          for (int x = 0; x < tilesize; x++)
            image.setRGB(tile.xpos * tilesize + x, tile.ypos * tilesize + y,
                         image.getRGB(lastTile.xpos * tilesize + x, lastTile.ypos * tilesize + y));
      }
      else {
        for (int y = 0; y < tilesize; y++)
          for (int x = 0; x < tilesize; x++)
            image.setRGB(tile.xpos * tilesize + x, tile.ypos * tilesize + y,
                         Palette.getColor(tileBuffer, 0, tileBuffer[4 * 256 + y * tilesize + x]));
      }
      lastTile = tile;
      if (i + 1 < tiles.size())
        Filereader.readBytes(is, tileBuffer);
    }

    return image;
  }

  public TisResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    imagedata = entry.getResourceData();
    String signature = new String(imagedata, 0, 4);
//    new String(ArrayUtil.getSubArray(imagedata, 4, 4)); // Version
//    Byteconvert.convertInt(imagedata, 8); // Tilecount
//    Byteconvert.convertInt(imagedata, 12); // Unknown
    tileoffset = Byteconvert.convertInt(imagedata, 16);
    tilesize = Byteconvert.convertInt(imagedata, 20);
    if (!signature.equalsIgnoreCase("TIS ")) {
      // Due to bug in Keyfile?
      tileoffset = 0;
      tilesize = 64;
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bprev) {
      currentnr -= labels.length;
      showTiles(currentnr);
    }
    else if (event.getSource() == bnext) {
      currentnr += labels.length;
      showTiles(currentnr);
    }
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close()
  {
    imagedata = null;
    if (imageLabels != null) {
      for (final JLabel imageLabel : imageLabels) {
        if (imageLabel != null) {
          ImageIcon lastimage = (ImageIcon)imageLabel.getIcon();
          if (lastimage != null)
            lastimage.getImage().flush();
        }
      }
    }
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    labels = new JLabel[ROWS * COLS];
    imageLabels = new JLabel[ROWS * COLS];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new JLabel("", JLabel.CENTER);
      imageLabels[i] = new JLabel("", JLabel.CENTER);
    }
    bnext = new JButton(Icons.getIcon("Forward16.gif"));
    bprev = new JButton(Icons.getIcon("Back16.gif"));
    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bexport.setMnemonic('e');
    bexport.addActionListener(this);
    bnext.addActionListener(this);
    bprev.addActionListener(this);
    showTiles(currentnr);

    JPanel ipanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    ipanel.setLayout(gbl);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(6, 6, 0, 6);
    for (int row = 0; row < ROWS; row++) {
      gbc.gridwidth = 1;
      gbc.weighty = 1.0;
      gbc.insets.top = 6;
      gbc.insets.bottom = 0;
      for (int col = 0; col < COLS; col++) {
        if (col == COLS - 1)
          gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(imageLabels[COLS * row + col], gbc);
        ipanel.add(imageLabels[COLS * row + col]);
      }
      gbc.insets.top = 0;
      gbc.insets.bottom = 6;
      gbc.weighty = 0.0;
      gbc.gridwidth = 1;
      for (int col = 0; col < COLS; col++) {
        if (col == COLS - 1)
          gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(labels[COLS * row + col], gbc);
        ipanel.add(labels[COLS * row + col]);
      }
    }

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bprev);
    bpanel.add(bnext);
    bpanel.add(bexport);

    panel = new JPanel(new BorderLayout());
    panel.add(ipanel, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    ipanel.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------

  private BufferedImage getTile(int nr)
  {
    int offset = tileoffset + nr * (tilesize * tilesize + 4 * 256);
    if (offset + (tilesize * tilesize + 4 * 256) > imagedata.length)
      return null;
    int paletteOffset = offset;
    offset += 4 * 256;
    BufferedImage tile = new BufferedImage(tilesize, tilesize, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < tilesize; y++)
      for (int x = 0; x < tilesize; x++)
        tile.setRGB(x, y, Palette.getColor(imagedata, paletteOffset, imagedata[offset++]));
    return tile;
  }

  private void showTiles(int nr)
  {
    bprev.setEnabled(nr > 0);
    bnext.setEnabled((nr + labels.length) * (tilesize * tilesize + 4 * 256) < imagedata.length -
                                                                              tileoffset);
    for (int i = 0; i < labels.length; i++) {
      ImageIcon lastimage = (ImageIcon)imageLabels[i].getIcon();
      if (lastimage != null)
        lastimage.getImage().flush();
      BufferedImage image = getTile(nr + i);
      if (image != null) {
        imageLabels[i].setIcon(new ImageIcon(image));
        labels[i].setText("Tile " + (nr + i));
      }
      else {
        imageLabels[i].setIcon(null);
        labels[i].setText("");
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class TileInfo implements Comparable<TileInfo>
  {
    private final int xpos, ypos, tilenum;

    private TileInfo(int xpos, int ypos, int tilenum)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
    }

    public int compareTo(TileInfo o)
    {
      return tilenum - o.tilenum;
    }
  }
}

