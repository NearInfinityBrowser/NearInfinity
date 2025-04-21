// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.SpawnPoint;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.util.Logger;

/**
 * Handles specific layer type: ARE/Spawn Point
 */
public class LayerObjectSpawnPoint extends LayerObject {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_SPAWN_POINT_1.getIcon().getImage(),
                                         ViewerIcons.ICON_ITM_SPAWN_POINT_2.getIcon().getImage() };

  private static final Point CENTER = ViewerIcons.ICON_ITM_SPAWN_POINT_1.getCenter();

  private final SpawnPoint sp;
  private final Point location = new Point();

  private final IconLayerItem item;
  private Flag scheduleFlags;

  public LayerObjectSpawnPoint(AreResource parent, SpawnPoint sp) {
    super("Spawn Point", SpawnPoint.class, parent);
    this.sp = sp;
    String msg = null;
    try {
      msg = sp.getAttribute(SpawnPoint.ARE_SPAWN_NAME).toString();
      location.x = ((IsNumeric) sp.getAttribute(SpawnPoint.ARE_SPAWN_LOCATION_X)).getValue();
      location.y = ((IsNumeric) sp.getAttribute(SpawnPoint.ARE_SPAWN_LOCATION_Y)).getValue();

      scheduleFlags = ((Flag) sp.getAttribute(SpawnPoint.ARE_SPAWN_ACTIVE_AT));

    } catch (Exception e) {
      Logger.error(e);
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(sp, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelSpawnPoints);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable() {
    return sp;
  }

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    if (type == 0 && item != null) {
      return new AbstractLayerItem[] { item };
    }
    return new AbstractLayerItem[0];
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    return new AbstractLayerItem[] { item };
  }

  @Override
  public void update(double zoomFactor) {
    if (item != null) {
      item.setItemLocation((int) (location.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (location.y * zoomFactor + (zoomFactor / 2.0)));
    }
  }

  @Override
  public boolean isScheduled(int schedule) {
    if (schedule >= ViewerConstants.TIME_0 && schedule <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(schedule));
    } else {
      return false;
    }
  }
}
