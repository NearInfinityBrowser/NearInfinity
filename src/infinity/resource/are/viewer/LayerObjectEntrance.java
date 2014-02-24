// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.are.Actor;
import infinity.resource.are.AreResource;
import infinity.resource.are.Entrance;

/**
 * Handles specific layer type: ARE/Entrance
 * @author argent77
 */
public class LayerObjectEntrance extends LayerObject
{
  private static final Image[] Icon = new Image[]{Icons.getImage("Entrance.png"),
                                                  Icons.getImage("Entrance_s.png")};
  private static Point Center = new Point(11, 18);

  private final Entrance entrance;
  private final Point location = new Point();

  private IconLayerItem item;

  public LayerObjectEntrance(AreResource parent, Entrance entrance)
  {
    super("Entrance", Entrance.class, parent);
    this.entrance = entrance;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return entrance;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{entrance};
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
    if (item != null && mapOrigin != null) {
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
    if (entrance != null) {
      String msg = "";
      try {
        location.x = ((DecNumber)entrance.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)entrance.getAttribute("Location: Y")).getValue();
        int o = ((Bitmap)entrance.getAttribute("Orientation")).getValue();
        if (o < 0) o = 0; else if (o >= Actor.s_orientation.length) o = Actor.s_orientation.length - 1;
        msg = String.format("%1$s (%2$s)", ((TextString)entrance.getAttribute("Name")).toString(),
                            Actor.s_orientation[o]);
      } catch (Exception e) {
        e.printStackTrace();
      }

      item = new IconLayerItem(location, entrance, msg, Icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, Icon[1]);
      item.setVisible(isVisible());
    }
  }
}
