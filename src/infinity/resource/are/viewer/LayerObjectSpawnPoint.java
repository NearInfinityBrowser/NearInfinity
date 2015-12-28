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
import infinity.resource.Viewable;
import infinity.resource.are.AreResource;
import infinity.resource.are.SpawnPoint;
import infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Spawn Point
 * @author argent77
 */
public class LayerObjectSpawnPoint extends LayerObject
{
  private static final Image[] Icon = new Image[]{Icons.getImage(ViewerIcons.class, "itm_SpawnPoint1.png"),
                                                  Icons.getImage(ViewerIcons.class, "itm_SpawnPoint2.png")};
  private static Point Center = new Point(22, 22);

  private final SpawnPoint sp;
  private final Point location = new Point();

  private IconLayerItem item;
  private Flag scheduleFlags;


  public LayerObjectSpawnPoint(AreResource parent, SpawnPoint sp)
  {
    super(ViewerConstants.RESOURCE_ARE, "Spawn Point", SpawnPoint.class, parent);
    this.sp = sp;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return sp;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{sp};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
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
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
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
  public boolean isScheduled(int schedule)
  {
    if (schedule >= ViewerConstants.TIME_0 && schedule <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(schedule));
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

      // Using cached icons
      Image[] icon;
      String keyIcon = String.format("%1$s%2$s", SharedResourceCache.createKey(Icon[0]),
                                                 SharedResourceCache.createKey(Icon[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.Icon, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.Icon, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon);
      } else {
        icon = Icon;
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      item = new IconLayerItem(location, sp, msg, icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}
