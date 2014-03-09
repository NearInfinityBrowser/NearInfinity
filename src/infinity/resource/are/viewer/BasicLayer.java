// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.gui.layeritem.AbstractLayerItem;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.resource.are.AreResource;
import infinity.resource.are.viewer.ViewerConstants.LayerType;
import infinity.resource.wed.WedResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Common base class for layer-specific managers.
 * @author argent77
 *
 */
public abstract class BasicLayer<O extends LayerObject>
{
  private final LayerType layerType;
  private final int layerTypeIndex;
  private final List<O> listObjects = new ArrayList<O>();
  private final AreaViewer viewer;

  private AbstractStruct parent;
  private int schedule;
  private boolean visible, initialized, scheduleEnabled;

  /**
   * Initializes the current layer.
   * @param parent The parent resource of the layer (either AreResource or WedResource).
   * @param type The type/identifier of the layer.
   */
  public BasicLayer(AbstractStruct parent, LayerType type, AreaViewer viewer)
  {
    // setting parent resource
    if (parent instanceof AreResource || parent instanceof WedResource) {
      this.parent = parent;
    } else {
      this.parent = null;
    }

    // setting layer type
    this.layerType = type;
    // getting associated layer type index
    int idx = -1;
    for (int i = 0; i < LayerType.values().length; i++) {
      if (LayerType.values()[i] == this.layerType) {
        idx = i;
      }
    }
    layerTypeIndex = idx;

    // setting associated area viewer instance
    this.viewer = viewer;
  }

  /**
   * Returns the registered AreaViewer instance.
   */
  public AreaViewer getViewer()
  {
    return viewer;
  }

  /**
   * Returns whether the parent structure is of type AreResource.
   */
  public boolean hasAre()
  {
    return (parent instanceof AreResource);
  }

  /**
   * Returns the parent as AreResource structure if available.
   */
  public AreResource getAre()
  {
    return (parent instanceof AreResource) ? (AreResource)parent : null;
  }

  /**
   * Returns whether the parent structure is of type WedResource.
   */
  public boolean hasWed()
  {
    return (parent instanceof WedResource);
  }

  /**
   * Returns the parent as WedResource structure if available.
   */
  public WedResource getWed()
  {
    return (parent instanceof WedResource) ? (WedResource)parent : null;
  }

  /**
   * Returns the layer type of this specific layer.
   * @return The layer type
   */
  public LayerType getLayerType()
  {
    return layerType;
  }

  /**
   * Returns the index of the layer type.
   * @return Index of layer type.
   */
  public int getLayerTypeIndex()
  {
    return layerTypeIndex;
  }

  /**
   * Returns the number of objects in this layer.
   * @return Number of layer objects.
   */
  public int getLayerObjectCount()
  {
    return listObjects.size();
  }

  /**
   * Returns the layer object at the specified index.
   * @param index The index of the layer object.
   * @return The layer object, of <code>null</code> if not available.
   */
  public O getLayerObject(int index)
  {
    if (index >= 0 && index < listObjects.size()) {
      return listObjects.get(index);
    } else {
      return null;
    }
  }

  /**
   * Returns the list of layer objects for direct manipulation.
   * @return List of layer objects.
   */
  public List<O> getLayerObjects()
  {
    return listObjects;
  }

  /**
   * Returns whether the whole layer of items is marked as visible. (Note: Does not work correctly
   * if the visibility state of individual items are overridden.)
   */
  public boolean isLayerVisible()
  {
    return visible;
  }

  /**
   * Sets the visibility state of all items in the layer.
   */
  public void setLayerVisible(boolean visible)
  {
    for (int i = 0; i < listObjects.size(); i++) {
      boolean state = visible && (!isScheduleEnabled() || (isScheduleEnabled() && isScheduled(i)));
      O obj = listObjects.get(i);
      AbstractLayerItem[] items = obj.getLayerItems();
      for (int j = 0; j < items.length; j++) {
        items[j].setVisible(state);
      }
    }
    this.visible = visible;
  }

  /**
   * Returns the layer object containing the specified layer item.
   * @return The object, if it has been found, <code>null</code> otherwise.
   */
  public O getLayerObjectOf(AbstractLayerItem item)
  {
    if (item != null) {
      for (int i = 0; i < listObjects.size(); i++) {
        O obj = listObjects.get(i);
        AbstractLayerItem[] items = obj.getLayerItems();
        for (int j = 0; j < items.length; j++) {
          if (items[j] == item) {
            return obj;
          }
        }
      }
    }
    return null;
  }

  /**
   * Loads all available objects of this layer if it hasn't been loaded yet.
   * @param forced If <code>true</code>, always (re-)loads the current layer, even if it has been loaded already.
   * @return The number of initialized layer objects.
   */
  public abstract int loadLayer(boolean forced);

  /**
   * Removes all objects of the layer from memory. Additionally all associated layer items will be
   * removed from their associated container(s).
   */
  public void close()
  {
    if (getViewer() != null) {
      for (int i = 0; i < listObjects.size(); i++) {
        listObjects.get(i).close();
      }
    }
    listObjects.clear();
    setInitialized(false);
  }

  /**
   * Returns whether schedules will be considered when querying {@link #isScheduled(int)}.
   * @return <code>true</code> if schedules will be considered, <code>false</code> otherwise.
   */
  public boolean isScheduleEnabled()
  {
    return scheduleEnabled;
  }

  /**
   * Set whether schedules will be considered when querying {@link #isScheduled(int)}.
   * When setting <code>false</code>, {@link #isScheduled(int)} will always return true.
   */
  public void setScheduleEnabled(boolean enable)
  {
    if (enable != scheduleEnabled) {
      scheduleEnabled = enable;
      setLayerVisible(isLayerVisible());
    }
  }

  /**
   * Set the current schedule. It can be used on layer objects that support schedules to check against.
   * @param schedule The schedule value (schedule = hour - 1)
   */
  public void setSchedule(int schedule)
  {
    if (schedule < 0) schedule = 0; else if (schedule > 23) schedule = 23;
    if (this.schedule != schedule)  {
      this.schedule = schedule;
      setLayerVisible(isLayerVisible());
    }
  }

  /**
   * Returns the currently set schedule.
   * @return The current schedule value.
   */
  public int getSchedule()
  {
    return schedule;
  }

  /**
   * Returns whether the layer object at the specified index is active at the currently set schedule.
   * @param index The index of the layer object to check.
   * @return <code>true</code> if the layer object is scheduled, <code>false</code> otherwise.
   *         (Note: Layer objects without schedule always return <code>true</code>.)
   */
  public boolean isScheduled(int index)
  {
    O obj = getLayerObject(index);
    if (obj != null) {
      return !isScheduleEnabled() || obj.isScheduled(getSchedule());
    } else {
      return false;
    }
  }

  /**
   * Returns the number of objects in this layer as a formatted string.
   * @return A formatted string telling about the number of objects in this layer.
   */
  public abstract String getAvailability();


  // Creates a list of structures of the specified type from the parent structure
  protected List<StructEntry> getStructures(int baseOfs, int count, Class<? extends StructEntry> type)
  {
    List<StructEntry> listStruct = new ArrayList<StructEntry>();
    if (getParent() != null && baseOfs >= 0 && count >= 0 && type != null) {
      List<StructEntry> list = getParent().getList();
      int cnt = 0;
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).getOffset() >= baseOfs && list.get(i).getClass().isAssignableFrom(type)) {
          listStruct.add(list.get(i));
          cnt++;
          if (cnt >= count) {
            break;
          }
        }
      }
    }

    return listStruct;
  }

  // Convenience method: sets required listeners
  protected void setListeners(LayerObject obj)
  {
    if (obj != null) {
      AbstractLayerItem[] items = obj.getLayerItems();
      for (int i = 0; i < items.length; i++) {
        if (items[i] != null) {
          items[i].addActionListener(viewer);
          items[i].addLayerItemListener(viewer);
          items[i].addMouseListener(viewer);
          items[i].addMouseMotionListener(viewer);
        }
      }
    }
  }

  /**
   * [For internal use only] Returns whether this layer has been loaded at least once.
   */
  protected boolean isInitialized()
  {
    return initialized;
  }

  /**
   * [For internal use only] Marks the current layer as loaded.
   */
  protected void setInitialized(boolean set)
  {
    initialized = set;
  }

  /**
   * [For internal use only] Sets the global visibility state of layer items without actually
   * touching the layer items. Use this method if you need to override {@link #setLayerVisible(boolean)}.
   */
  protected void setVisibilityState(boolean state)
  {
    visible = state;
  }

  // Returns the parent structure of the layer objects regardless of type.
  private AbstractStruct getParent()
  {
    return parent;
  }
}
