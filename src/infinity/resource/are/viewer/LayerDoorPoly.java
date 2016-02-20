// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.resource.StructEntry;
import infinity.resource.wed.Door;
import infinity.resource.wed.WedResource;

/**
 * Manages door polygon layer objects.
 * @author argent77
 */
public class LayerDoorPoly extends BasicLayer<LayerObjectDoorPoly>
{
  private static final String AvailableFmt = "Door polygons: %1$d";

  private boolean doorClosed;

  public LayerDoorPoly(WedResource wed, AreaViewer viewer)
  {
    super(wed, ViewerConstants.LayerType.DoorPoly, viewer);
    doorClosed = false;
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectDoorPoly> list = getLayerObjects();
      if (hasWed()) {
        WedResource wed = getWed();
        SectionOffset so = (SectionOffset)wed.getAttribute(WedResource.WED_OFFSET_DOORS);
        SectionCount sc = (SectionCount)wed.getAttribute(WedResource.WED_NUM_DOORS);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, Door.class);
          for (int i = 0, size = listStruct.size(); i < size; i++) {
            LayerObjectDoorPoly obj = new LayerObjectDoorPoly(wed, (Door)listStruct.get(i));
            setListeners(obj);
            list.add(obj);
          }
          setInitialized(true);
        }
      }
      return list.size();
    }
    return 0;
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
    List<LayerObjectDoorPoly> list = getLayerObjects();
    if (list != null) {
      for (int i = 0, size = list.size(); i < size; i++) {
        LayerObjectDoorPoly obj = list.get(i);
        // processing open door items
        AbstractLayerItem[] items = obj.getLayerItems(ViewerConstants.DOOR_OPEN);
        if (items != null) {
          for (int j = 0; j < items.length; j++) {
            items[j].setVisible(isLayerVisible() && !doorClosed);
          }
        }
        // processing open door items
        items = obj.getLayerItems(ViewerConstants.DOOR_CLOSED);
        if (items != null) {
          for (int j = 0; j < items.length; j++) {
            items[j].setVisible(isLayerVisible() && doorClosed);
          }
        }
      }
    }
  }

  /**
   * Returns the current state of doors.
   * @return Either <code>ViewerConstants.DOOR_OPEN</code> or <code>ViewerConstants.DOOR_CLOSED</code>.
   */
  public int getDoorState()
  {
    return doorClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
  }

  /**
   * Sets the state of doors for all door layer objects.
   * @param state The door state (either <code>ViewerConstants.DOOR_OPEN</code> or
   *              <code>ViewerConstants.DOOR_CLOSED</code>).
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
