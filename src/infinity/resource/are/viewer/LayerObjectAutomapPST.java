// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNotePST;

/**
 * Handles specific layer type: ARE/Automap Note (PST-specific)
 * @author argent77
 */
public class LayerObjectAutomapPST extends LayerObject
{
  private static final Image[] Icon = new Image[]{Icons.getImage("Automap.png"),
                                                  Icons.getImage("Automap_s.png")};
  private static Point Center = new Point(26, 26);
  private static final double MapScale = 32.0 / 3.0;    // scaling factor for MOS to TIS coordinates

  private final AutomapNotePST note;
  private final Point location = new Point();

  private IconLayerItem item;


  public LayerObjectAutomapPST(AreResource parent, AutomapNotePST note)
  {
    super("Automap", AutomapNotePST.class, parent);
    this.note = note;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return note;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{note};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(Point mapOrigin, double zoomFactor)
  {
    if (note != null && mapOrigin != null) {
      item.setItemLocation(mapOrigin.x + (int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           mapOrigin.y + (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location};
  }

  private void init()
  {
    if (note != null) {
      String msg = "";
      try {
        int v = ((DecNumber)note.getAttribute("Coordinate: X")).getValue();
        location.x = (int)(v * MapScale);
        v = ((DecNumber)note.getAttribute("Coordinate: Y")).getValue();
        location.y = (int)(v * MapScale);
        msg = ((TextString)note.getAttribute("Text")).toString();
      } catch (Exception e) {
        e.printStackTrace();
      }

      item = new IconLayerItem(location, note, msg, Icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, Icon[1]);
      item.setVisible(isVisible());
    }
  }
}
