// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.resource.wed.Door;
import org.infinity.resource.wed.WedResource;
import static org.infinity.resource.wed.WedResource.WED_NUM_DOORS;
import static org.infinity.resource.wed.WedResource.WED_OFFSET_DOORS;

/**
 * Manages door polygon layer objects.
 */
public class LayerDoorPoly extends BasicLayer<LayerObjectDoorPoly, WedResource>
{
  private static final String AvailableFmt = "Door polygons: %d";

  private boolean doorClosed;

  public LayerDoorPoly(WedResource wed, AreaViewer viewer)
  {
    super(wed, ViewerConstants.LayerType.DOOR_POLY, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(WED_OFFSET_DOORS, WED_NUM_DOORS,
                   Door.class, d -> new LayerObjectDoorPoly(parent, d));
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }

  @Override
  public void setLayerVisible(boolean visible)
  {
    setVisibilityState(visible);
    for (final LayerObjectDoorPoly obj : getLayerObjects()) {
      // processing open door items
      AbstractLayerItem[] items = obj.getLayerItems(ViewerConstants.DOOR_OPEN);
      if (items != null) {
        for (final AbstractLayerItem item : items) {
          item.setVisible(isLayerVisible() && !doorClosed);
        }
      }
      // processing open door items
      items = obj.getLayerItems(ViewerConstants.DOOR_CLOSED);
      if (items != null) {
        for (final AbstractLayerItem item : items) {
          item.setVisible(isLayerVisible() && doorClosed);
        }
      }
    }
  }

  /**
   * Returns the current state of doors.
   * @return Either {@code ViewerConstants.DOOR_OPEN} or {@code ViewerConstants.DOOR_CLOSED}.
   */
  public int getDoorState()
  {
    return doorClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
  }

  /**
   * Sets the state of doors for all door layer objects.
   * @param state The door state (either {@code ViewerConstants.DOOR_OPEN} or
   *              {@code ViewerConstants.DOOR_CLOSED}).
   */
  public void setDoorState(int state)
  {
    boolean isClosed = (state == ViewerConstants.DOOR_CLOSED);
    if (isClosed != doorClosed) {
      doorClosed = isClosed;
      if (isLayerVisible()) {
        setLayerVisible(isLayerVisible());
      }
    }
  }
}
