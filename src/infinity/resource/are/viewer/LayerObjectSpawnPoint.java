// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.are.AreResource;
import infinity.resource.are.SpawnPoint;

/**
 * Handles specific layer type: ARE/Spawn Point
 * @author argent77
 */
public class LayerObjectSpawnPoint extends LayerObject
{
  private static final Image[] Icon = new Image[]{Icons.getImage("itm_SpawnPoint1.png"),
                                                  Icons.getImage("itm_SpawnPoint2.png")};
  private static Point Center = new Point(22, 22);

  private final SpawnPoint sp;
  private final Point location = new Point();

  private IconLayerItem item;
  private Flag scheduleFlags;


  public LayerObjectSpawnPoint(AreResource parent, SpawnPoint sp)
  {
    super("Spawn Point", SpawnPoint.class, parent);
    this.sp = sp;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return sp;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{sp};
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

  @Override
  public boolean isActiveAt(int dayTime)
  {
    return isActiveAt(scheduleFlags, dayTime);
  }

  @Override
  public boolean isActiveAtHour(int time)
  {
    if (time >= ViewerConstants.TIME_0 && time <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(time));
    } else {
      return false;
    }
  }


  private void init()
  {
    if (sp != null) {
      String msg = "";
      try {
        location.x = ((DecNumber)sp.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)sp.getAttribute("Location: Y")).getValue();

        scheduleFlags = ((Flag)sp.getAttribute("Active at"));

        msg = ((TextString)sp.getAttribute("Name")).toString();
      } catch (Exception e) {
        e.printStackTrace();
      }

      item = new IconLayerItem(location, sp, msg, Icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, Icon[1]);
      item.setVisible(isVisible());
    }
  }
}
