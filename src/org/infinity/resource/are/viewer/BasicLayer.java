// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.viewer.ViewerConstants.LayerType;
import org.infinity.resource.wed.WedResource;

/**
 * Common base class for layer-specific managers.
 *
 * @param <E> Type of the layer item in the manager
 * @param <R> Type of the resource that contains layer items
 */
public abstract class BasicLayer<E extends LayerObject, R extends AbstractStruct>
{
  private final LayerType layerType;
  private final int layerTypeIndex;
  private final List<E> listObjects = new ArrayList<>();
  private final AreaViewer viewer;

  protected final R parent;
  private int schedule;
  private boolean visible, scheduleEnabled;
  /** Determines whether this layer has been loaded at least once. */
  private boolean initialized;

  /**
   * Initializes the current layer.
   * @param parent The parent resource of the layer (either {@link AreResource} or {@link WedResource}).
   * @param type The type/identifier of the layer.
   */
  public BasicLayer(R parent, LayerType type, AreaViewer viewer)
  {
    // setting parent resource
    this.parent = parent;

    // setting layer type
    this.layerType = type;
    // getting associated layer type index
    int idx = -1;
    LayerType[] lt = LayerType.values();
    for (int i = 0; i < lt.length; i++) {
      if (lt[i] == this.layerType) {
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
   * Returns the list of layer objects for direct manipulation.
   * @return List of layer objects. Never {@code null}
   */
  public List<E> getLayerObjects()
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
    for (int i = 0, size = listObjects.size(); i < size; i++) {
      boolean state = visible && (!isScheduleEnabled() || isScheduled(i));
      E obj = listObjects.get(i);
      for (final AbstractLayerItem item : obj.getLayerItems()) {
        item.setVisible(state);
      }
    }
    this.visible = visible;
  }

  /**
   * Loads all available objects of this layer if it hasn't been loaded yet.
   * @param forced If {@code true}, always (re-)loads the current layer, even if it has been loaded already.
   * @return The number of initialized layer objects.
   */
  public final int loadLayer(boolean forced)
  {
    if (forced || !initialized) {
      close();
      loadLayer();
      return listObjects.size();
    }
    return 0;
  }

  /**
   * Loads all items with specified class from {@link #parent} structure.
   *
   * @param offsetAttribute Attribute in the {@link #parent} structure that contains
   *        offset of the first item to load
   * @param countAttribute Attribute in the {@link #parent} structure that contains
   *        count of the items to load
   * @param itemClass Class ot the item to load
   * @param newLayerObject Function that creates new layer object from item, extracted
   *        from resource
   *
   * @param <T> Type of the items on the layer
   */
  protected <T extends StructEntry> void loadLayerItems(String offsetAttribute, String countAttribute,
                                                        Class<T> itemClass, Function<T, E> newLayerObject)
  {
//    long timeStart = System.nanoTime();
    final SectionOffset so = (SectionOffset)parent.getAttribute(offsetAttribute);
    final SectionCount  sc = (SectionCount )parent.getAttribute(countAttribute);
    if (so != null && sc != null) {
      final int ofs = so.getValue();
      final int cnt = sc.getValue();
      for (final T entry : getStructures(ofs, cnt, itemClass)) {
        final E obj = newLayerObject.apply(entry);
        setListeners(obj);
        listObjects.add(obj);
      }
      setInitialized(true);
    }
//    long timeEnd = System.nanoTime();
//    System.out.printf("Area viewer > load layer items (%s): %,d µs\n", itemClass.getSimpleName(), (timeEnd - timeStart) / 1000);
  }

  /** Loads all available objects of this layer. */
  protected abstract void loadLayer();

  /**
   * Removes all objects of the layer from memory. Additionally all associated layer items will be
   * removed from their associated container(s).
   */
  public void close()
  {
    if (getViewer() != null) {
      for (final E obj : listObjects) {
        obj.close();
      }
    }
    listObjects.clear();
    setInitialized(false);
  }

  /**
   * Returns whether schedules will be considered when querying {@link #isScheduled(int)}.
   * @return {@code true} if schedules will be considered, {@code false} otherwise.
   */
  public boolean isScheduleEnabled()
  {
    return scheduleEnabled;
  }

  /**
   * Set whether schedules will be considered when querying {@link #isScheduled(int)}.
   * When setting {@code false}, {@link #isScheduled(int)} will always return true.
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
   * @return {@code true} if the layer object is scheduled, {@code false} otherwise.
   *         (Note: Layer objects without schedule always return {@code true}.)
   */
  public boolean isScheduled(int index)
  {
    if (index >= 0 && index < listObjects.size()) {
      return !isScheduleEnabled() || listObjects.get(index).isScheduled(getSchedule());
    }
    return false;
  }

  /**
   * Returns the number of objects in this layer as a formatted string.
   * @return A formatted string telling about the number of objects in this layer.
   */
  public abstract String getAvailability();


  /**
   * Returns all direct children from parent of this object (i.e. siblings of this
   * object) with the specified type, which are located after the specified offset.
   *
   * @param baseOfs Offset from which take fields
   * @param maxCount Maximum count of returned objects. Returned list will have
   *        size not more than this value
   * @param type Class of fields to get
   *
   * @param <T> Expected return type
   *
   * @return List of specified fields. Never {@code null}
   */
  protected <T extends StructEntry> List<T> getStructures(int baseOfs, int maxCount, Class<? extends T> type)
  {
    final List<T> fields = new ArrayList<>();
    if (parent != null && baseOfs >= 0 && maxCount >= 0 && type != null) {
      for (final StructEntry field : parent.getFields()) {
        if (field.getOffset() >= baseOfs && type.isAssignableFrom(field.getClass())) {
          if (maxCount-- < 0) { break; }

          fields.add(type.cast(field));
        }
      }
    }

    return fields;
  }

  /**
   * Subscribe viewer to events from all items in this object.
   *
   * @param obj Layer object, must not be {@code null}
   */
  protected void setListeners(LayerObject obj)
  {
    for (final AbstractLayerItem item : obj.getLayerItems()) {
      item.addActionListener(viewer.getListeners());
      item.addLayerItemListener(viewer.getListeners());
      item.addMouseListener(viewer.getListeners());
      item.addMouseMotionListener(viewer.getListeners());
    }
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
}
