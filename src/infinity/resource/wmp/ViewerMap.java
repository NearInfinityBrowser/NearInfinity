// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.graphics.BamResource;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;

final class ViewerMap extends JPanel implements ListSelectionListener
{
  private static final ImageIcon areaIcon = Icons.getIcon("Stop16.gif");
  private final BufferedImage map;
  private int xCoord = -1, yCoord, pixels[] = new int[16 * 16];

  ViewerMap(MapEntry wmpMap)
  {
    BamResource icons = null;
    ResourceRef iconRef = (ResourceRef)wmpMap.getAttribute("Map icons");
    if (iconRef != null) {
      ResourceEntry iconEntry = ResourceFactory.getInstance().getResourceEntry(iconRef.getResourceName());
      if (iconEntry != null)
        icons = (BamResource)ResourceFactory.getResource(iconEntry);
    }
    JLabel mapLabel = ViewerUtil.makeImagePanel((ResourceRef)wmpMap.getAttribute("Map"));
    map = (BufferedImage)((ImageIcon)mapLabel.getIcon()).getImage();
    JPanel areas = ViewerUtil.makeListPanel("Areas", wmpMap, AreaEntry.class, "Name",
                                            new WmpAreaListRenderer(icons), this);
    JScrollPane mapScroll = new JScrollPane(mapLabel);
    mapScroll.setBorder(BorderFactory.createEmptyBorder());

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapScroll, areas);
    split.setDividerLocation(NearInfinity.getInstance().getWidth() - 475);
    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (!event.getValueIsAdjusting()) {
      JList list = (JList)event.getSource();
      AreaEntry areaEntry = (AreaEntry)list.getSelectedValue();
      if (xCoord != -1)
        map.setRGB(xCoord, yCoord, 16, 16, pixels, 0, 16);
      xCoord = ((DecNumber)areaEntry.getAttribute("Coordinate: X")).getValue();
      yCoord = ((DecNumber)areaEntry.getAttribute("Coordinate: Y")).getValue();
      pixels = map.getRGB(xCoord, yCoord, 16, 16, pixels, 0, 16);
      map.getGraphics().drawImage(areaIcon.getImage(), xCoord, yCoord, null);
      repaint();
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class WmpAreaListRenderer extends DefaultListCellRenderer
  {
    private final BamResource icons;

    private WmpAreaListRenderer(BamResource icons)
    {
      this.icons = icons;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct struct = (AbstractStruct)value;
      label.setText(struct.getAttribute("Name").toString());
      DecNumber animNr = (DecNumber)struct.getAttribute("Icon number");
      setIcon(null);
      if (icons != null)
        setIcon(new ImageIcon(icons.getFrame(icons.getFrameNr(animNr.getValue(), 0))));
      return label;
    }
  }
}

