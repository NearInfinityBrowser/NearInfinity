// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Entrance;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Entrance
 */
public class LayerObjectEntrance extends LayerObject
{
  private static final Image[] ICONS = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ENTRANCE_1),
                                        Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ENTRANCE_2)};
  private static final Point CENTER = new Point(11, 18);

  private final Entrance entrance;
  private final Point location = new Point();

  private final IconLayerItem item;

  public LayerObjectEntrance(AreResource parent, Entrance entrance)
  {
    super("Entrance", Entrance.class, parent);
    this.entrance = entrance;
    String msg = null;
    try {
      location.x = ((IsNumeric)entrance.getAttribute(Entrance.ARE_ENTRANCE_LOCATION_X)).getValue();
      location.y = ((IsNumeric)entrance.getAttribute(Entrance.ARE_ENTRANCE_LOCATION_Y)).getValue();
      int o = ((IsNumeric)entrance.getAttribute(Entrance.ARE_ENTRANCE_ORIENTATION)).getValue();
      if (o < 0) o = 0; else if (o >= AbstractStruct.OPTION_ORIENTATION.length) o = AbstractStruct.OPTION_ORIENTATION.length - 1;
      final String name = entrance.getAttribute(Entrance.ARE_ENTRANCE_NAME).toString();
      msg = String.format("%s (%s)", name, AbstractStruct.OPTION_ORIENTATION[o]);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(entrance, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelEntrances);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable()
  {
    return entrance;
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
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
