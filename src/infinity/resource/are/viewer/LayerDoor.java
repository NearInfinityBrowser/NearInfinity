// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.StructEntry;
import infinity.resource.are.AreResource;
import infinity.resource.are.Door;

/**
 * Manages door layer objects.
 * @author argent77
 */
public class LayerDoor extends BasicLayer<LayerObjectDoor>
{
  private static final String AvailableFmt = "%1$d door%2$s available";

  private boolean doorClosed;

  public LayerDoor(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.Door, viewer);
    doorClosed = false;
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectDoor> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute("Doors offset");
        SectionCount sc = (SectionCount)are.getAttribute("# doors");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, Door.class);
          for (int i = 0; i < listStruct.size(); i++) {
            LayerObjectDoor obj = new LayerObjectDoor(are, (Door)listStruct.get(i));
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
    return String.format(AvailableFmt, cnt, (cnt == 1) ? "" : "s");
  }

  @Override
  public void setLayerVisible(boolean visible)
  {
    setVisibilityState(visible);
    List<LayerObjectDoor> list = getLayerObjects();
    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        LayerObjectDoor obj = list.get(i);
        obj.getLayerItem(ViewerConstants.DOOR_OPEN).setVisible(isLayerVisible() && !doorClosed);
        obj.getLayerItem(ViewerConstants.DOOR_CLOSED).setVisible(isLayerVisible() && doorClosed);
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
