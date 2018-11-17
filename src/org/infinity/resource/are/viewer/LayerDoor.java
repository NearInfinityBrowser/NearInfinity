// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_DOORS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_DOORS;
import org.infinity.resource.are.Door;

/**
 * Manages door layer objects.
 */
public class LayerDoor extends BasicLayer<LayerObjectDoor, AreResource>
{
  private static final String AvailableFmt = "Doors: %d";

  private boolean doorClosed;

  public LayerDoor(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.DOOR, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(ARE_OFFSET_DOORS, ARE_NUM_DOORS,
                   Door.class, d -> new LayerObjectDoor(parent, d));
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
    for (final LayerObjectDoor obj : getLayerObjects()) {
      obj.getLayerItem(ViewerConstants.DOOR_OPEN).setVisible(isLayerVisible() && !doorClosed);
      obj.getLayerItem(ViewerConstants.DOOR_CLOSED).setVisible(isLayerVisible() && doorClosed);
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
