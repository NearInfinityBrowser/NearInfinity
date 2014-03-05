// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.gui.layeritem.AbstractLayerItem;
import infinity.resource.are.AreResource;
import infinity.resource.are.viewer.ViewerConstants.LayerType;
import infinity.resource.wed.WedResource;

import java.util.EnumMap;
import java.util.List;

/**
 * Manages all layer objects of a single ARE map.
 * @author argent77
 */
/*
 * TODO: add interpolation type support for animations
 * TODO: add update layer item methods for all layers
 */
public final class LayerManager
{
  // Defines order of drawing
  public static final LayerType[] LayerOrdered = new LayerType[]{
    LayerType.Actor, LayerType.Entrance, LayerType.Ambient, LayerType.ProTrap, LayerType.Animation,
    LayerType.SpawnPoint, LayerType.Automap, LayerType.Container, LayerType.Door, LayerType.Region,
    LayerType.Transition, LayerType.DoorPoly, LayerType.WallPoly
  };

  private static final EnumMap<LayerType, String> LayerLabels = new EnumMap<LayerType, String>(LayerType.class);
  private static final EnumMap<LayerType, String> LayerAvailabilityFmt = new EnumMap<LayerType, String>(LayerType.class);
  static {
    LayerLabels.put(LayerType.Actor, "Actors");
    LayerLabels.put(LayerType.Region, "Regions");
    LayerLabels.put(LayerType.Entrance, "Entrances");
    LayerLabels.put(LayerType.Container, "Containers");
    LayerLabels.put(LayerType.Ambient, "Ambient Sounds");
    LayerLabels.put(LayerType.Door, "Doors");
    LayerLabels.put(LayerType.Animation, "Background Animations");
    LayerLabels.put(LayerType.Automap, "Automap Notes");
    LayerLabels.put(LayerType.SpawnPoint, "Spawn Points");
    LayerLabels.put(LayerType.Transition, "Map Transitions");
    LayerLabels.put(LayerType.ProTrap, "Projectile Traps");
    LayerLabels.put(LayerType.DoorPoly, "Door Polygons");
    LayerLabels.put(LayerType.WallPoly, "Wall Polygons");
    LayerAvailabilityFmt.put(LayerType.Actor, "%1$d actor%2$s available");
    LayerAvailabilityFmt.put(LayerType.Region, "%1$d region%2$s available");
    LayerAvailabilityFmt.put(LayerType.Entrance, "%1$d entrance%2$s available");
    LayerAvailabilityFmt.put(LayerType.Container, "%1$d container%2$s available");
    LayerAvailabilityFmt.put(LayerType.Ambient, "%1$d ambient sound%2$s available");
    LayerAvailabilityFmt.put(LayerType.Door, "%1$d door%2$s available");
    LayerAvailabilityFmt.put(LayerType.Animation, "%1$d background animation%2$s available");
    LayerAvailabilityFmt.put(LayerType.Automap, "%1$d automap note%2$s available");
    LayerAvailabilityFmt.put(LayerType.SpawnPoint, "%1$d spawn point%2$s available");
    LayerAvailabilityFmt.put(LayerType.Transition, "%1$d map transition%2$s available");
    LayerAvailabilityFmt.put(LayerType.ProTrap, "%1$d projectile trap%2$s available");
    LayerAvailabilityFmt.put(LayerType.DoorPoly, "%1$d door polygon%2$s available");
    LayerAvailabilityFmt.put(LayerType.WallPoly, "%1$d wall polygon%2$s available");
  }

  @SuppressWarnings("rawtypes")
  private final EnumMap<LayerType, BasicLayer> layers = new EnumMap<LayerType, BasicLayer>(LayerType.class);
  private final AreaViewer viewer;

  private AreResource are;
  private WedResource wed;
  private boolean scheduleEnabled, forcedInterpolation;
  private int schedule, interpolationType;

  /**
   * Returns the number of supported layer types.
   */
  public static int getLayerTypeCount()
  {
    return LayerType.values().length;
  }

  /**
   * Returns the layer type located at the specified index.
   */
  public static LayerType getLayerType(int index)
  {
    if (index >= 0 && index < LayerType.values().length) {
      return LayerType.values()[index];
    } else {
      return null;
    }
  }

  /**
   * Returns the index of the specified layer.
   */
  public static int getLayerTypeIndex(LayerType layer)
  {
    LayerType[] l = LayerType.values();
    for (int i = 0; i < LayerType.values().length; i++) {
      if (l[i] == layer) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the label associated with the specified layer.
   */
  public static String getLayerTypeLabel(LayerType layer)
  {
    String s = LayerLabels.get(layer);
    if (s != null) {
      return s;
    } else {
      return "";
    }
  }

  /**
   * Converts hours into scheduled time indices.
   * @param hour The hour to convert (0..23).
   * @return The schedule index.
   */
  public static int toSchedule(int hour)
  {
    int schedule = hour - 1;
    while (schedule < 0) { schedule += 24; }
    schedule %= 24;
    return schedule;
  }

  /**
   * Converts scheduled time indices into hours.
   * @param schedule The schedule index to convert (0..23).
   * @return The hour.
   */
  public static int toHour(int schedule)
  {
    int hour = schedule + 1;
    while (hour < 0) { hour += 24; }
    hour %= 24;
    return hour;
  }


  public LayerManager(AreResource are, WedResource wed, AreaViewer viewer)
  {
    this.viewer = viewer;
    init(are, wed, true);
  }

  /**
   * Returns the associated area viewer instance.
   */
  public AreaViewer getViewer()
  {
    return viewer;
  }

  /**
   * Returns the currently used ARE resource structure.
   */
  public AreResource getAreResource()
  {
    return are;
  }

  /**
   * Specify a new ARE resource structure. Automatically reloads related layer objects.
   * @param are The new ARE resource structure.
   */
  public void setAreResource(AreResource are)
  {
    if (this.are != are) {
      init(are, wed, false);
    }
  }

  /**
   * Returns the currently used WED resource structure.
   */
  public WedResource getWedResource()
  {
    return wed;
  }

  /**
   * Specify a new WED resource structure. Automatically reloads related layer objects.
   * @param wed The new WED resource structure.
   */
  public void setWedResource(WedResource wed)
  {
    if (this.wed != wed) {
      init(are, wed, false);
    }
  }

  /**
   * Returns a formatted string that indicates how many objects of the specified layer type have been
   * loaded.
   * @param layer The layer to query.
   * @return A formatted string telling about number of layer objects.
   */
  public String getLayerAvailability(LayerType layer)
  {
    if (layers.containsKey(layer)) {
      return layers.get(layer).getAvailability();
    }
    return "";
  }

  /**
   * Same as {@link #getLayerAvailability(LayerType)}, but also considers special states for layer
   * managers that supports it.
   * @param layer The layer to query.
   * @param type A layer-specific type (currently only LayerAmbient is supported).
   * @return A formatted string telling about number of layer objects.
   */
  public String getLayerAvailability(LayerType layer, int type)
  {
    if (layer == LayerType.Ambient) {
      return getLayerAvailability(layer, type);
    } else {
      return getLayerAvailability(layer);
    }
  }

  /**
   * Returns whether time schedules for layer items will be considered in their visibility state.
   */
  public boolean isScheduleEnabled()
  {
    return scheduleEnabled;
  }

  /**
   * Specify whether time schedules for layer items will be considered.
   */
  public void setScheduleEnabled(boolean enable)
  {
    if (enable != scheduleEnabled) {
      scheduleEnabled = enable;
      for (int i = 0; i < getLayerTypeCount(); i++) {
        @SuppressWarnings("rawtypes")
        BasicLayer bl = getLayer(getLayerType(i));
        if (bl != null) {
          bl.setScheduleEnabled(enable);
        }
      }
    }
  }

  /**
   * Returns the currently defined schedule value (hour = schedule + 1).
   */
  public int getSchedule()
  {
    return schedule;
  }

  /**
   * Specifiy the current schedule for layer items.
   * @param schedule The schedule value (0 = 00:30-01:29, ..., 23 = 23:30-00:29)
   */
  public void setSchedule(int schedule)
  {
    while (schedule < 0) { schedule += 24; }
    schedule %= 24;
    if (schedule != this.schedule) {
      this.schedule = schedule;
      for (int i = 0; i < getLayerTypeCount(); i++) {
        @SuppressWarnings("rawtypes")
        BasicLayer bl = getLayer(getLayerType(i));
        if (bl != null) {
          bl.setSchedule(schedule);
        }
      }
    }
  }

  /**
   * Returns whether the specified layer object is active at the current scheduled time.
   * @param obj The layer object to query
   * @return <code>true</code> if the layer object is scheduled at the currently set hour or
   *         schedule is disabled, <code>false</code> otherwise.
   */
  public boolean isScheduled(LayerObject obj)
  {
    if (obj != null) {
      return !isScheduleEnabled() || (isScheduleEnabled() && obj.isScheduled(getSchedule()));
    }
    return false;
  }

  /**
   * Reloads all objects in every supported layer.
   */
  public void reload()
  {
    init(are, wed, true);
  }

  /**
   * Reloads objects of the specified layer.
   * @param layer The layer to reload.
   * @return Number of layer objects found.
   */
  public int reload(LayerType layer)
  {
    return loadLayer(layer, true);
  }

  /**
   * Removes all layer objects from memory.
   */
  public void clear()
  {
    for (LayerType layer: LayerType.values()) {
      @SuppressWarnings("rawtypes")
      BasicLayer bl = layers.get(layer);
      if (bl != null) {
        bl.clear();
      }
    }
    layers.clear();
    are = null;
    wed = null;
  }

  /**
   * Returns a layer-specific manager.
   * @param layer
   * @return
   */
  @SuppressWarnings("rawtypes")
  public BasicLayer getLayer(LayerType layer)
  {
    if (layer != null) {
      return layers.get(layer);
    } else {
      return null;
    }
  }

  /**
   * Returns the number of objects in the specified layer.
   * @param layer The layer to check.
   * @return Number of objects in the specified layer.
   */
  public int getLayerObjectCount(LayerType layer)
  {
    @SuppressWarnings("rawtypes")
    BasicLayer bl = layers.get(layer);
    if (bl != null) {
      return bl.getLayerObjectCount();
    }
    return 0;
  }

  /**
   * Returns a specific layer object.
   * @param layer The layer of the object.
   * @param index The index of the object.
   * @return The layer object if found, <code>null</code> otherwise.
   */
  public LayerObject getLayerObject(LayerType layer, int index)
  {
    @SuppressWarnings("rawtypes")
    BasicLayer bl = layers.get(layer);
    if (bl != null) {
      return bl.getLayerObject(index);
    }
    return null;
  }

  /**
   * Returns a list of objects associated with the specified layer.
   * @param layer The layer of the objects
   * @return A list of objects or <code>null</code> if not found.
   */
  @SuppressWarnings("unchecked")
  public List<LayerObject> getLayerObjects(LayerType layer)
  {
    @SuppressWarnings("rawtypes")
    BasicLayer bl = layers.get(layer);
    if (bl != null) {
      return bl.getLayerObjects();
    }
    return null;
  }

  /**
   * Returns the current state of the door.
   * @return Either <code>ViewerConstants.DOOR_OPEN</code> or <code>ViewerConstants.DOOR_CLOSED</code>.
   */
  public int getDoorState()
  {
    return ((LayerDoor)getLayer(LayerType.Door)).getDoorState();
  }

  /**
   * Sets the door state used by certain layers. Automatically updates the visibility state of
   * related layers.
   * @param state Either <code>ViewerConstants.DOOR_OPEN</code> or <code>ViewerConstants.DOOR_CLOSED</code>.
   */
  public void setDoorState(int state)
  {
    ((LayerDoor)getLayer(LayerType.Door)).setDoorState(state);
    ((LayerDoorPoly)getLayer(LayerType.DoorPoly)).setDoorState(state);
  }

  /**
   * Returns whether to show iconic representations of background animations or the real thing.
   */
  public boolean isRealAnimationEnabled()
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.Animation);
    if (layer != null) {
      return layer.isRealAnimationEnabled();
    }
    return false;
  }

  /**
   * Specify whether to show iconic representations of background animations or the real thing.
   */
  public void setRealAnimationEnabled(boolean enable)
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.Animation);
    if (layer != null) {
      layer.setRealAnimationEnabled(enable);
    }
  }

  /**
   * Returns whether real animations are enabled and animated.
   */
  public boolean isRealAnimationPlaying()
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.Animation);
    if (layer != null) {
      return layer.isRealAnimationPlaying();
    }
    return false;
  }

  /**
   * Specify whether to animate real background animations. Setting to <code>true</code> implicitly
   * enables real animations.
   */
  public void setRealAnimationPlaying(boolean play)
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.Animation);
    if (layer != null) {
      layer.setRealAnimationPlaying(play);
    }
  }

  /**
   * Returns the currently active interpolation type for real animations.
   * @return Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR, ViewerConstants.TYPE_NEAREST_BILINEAR
   *         or ViewerConstants.TYPE_BICUBIC.
   */
  public int getRealAnimationInterpolation()
  {
    return interpolationType;
  }

  /**
   * Sets the interpolation type for real animations
   * @param interpolationType Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR,
   *                          ViewerConstants.TYPE_NEAREST_BILINEAR or ViewerConstants.TYPE_BICUBIC.
   */
  public void setRealAnimationInterpolation(int interpolationType)
  {
    switch (interpolationType) {
      case ViewerConstants.TYPE_NEAREST_NEIGHBOR:
      case ViewerConstants.TYPE_BILINEAR:
      case ViewerConstants.TYPE_BICUBIC:
        if (interpolationType != this.interpolationType) {
          this.interpolationType = interpolationType;
          LayerAnimation layer = (LayerAnimation)getLayer(LayerType.Animation);
          if (layer != null) {
            layer.setRealAnimationInterpolation(this.interpolationType);
          }
        }
    }
  }

  /**
   * Returns whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public boolean isRealAnimationForcedInterpolation()
  {
    return forcedInterpolation;
  }

  /**
   * Specify whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public void setRealAnimationForcedInterpolation(boolean forced)
  {
    if (forced != forcedInterpolation) {
      forcedInterpolation = forced;
      LayerAnimation layer = (LayerAnimation)getLayer(LayerType.Animation);
      if (layer != null) {
        layer.setRealAnimationForcedInterpolation(forcedInterpolation);
      }
    }
  }

  /**
   * Returns whether the items of the specified layer are visible.
   * @param layer The layer to check for visibility.
   * @return <code>true</code> if one or more items of the specified layer are visible, <code>false</code> otherwise.
   */
  public boolean isLayerVisible(LayerType layer)
  {
    if (layer != null) {
      @SuppressWarnings("rawtypes")
      BasicLayer bl = layers.get(layer);
      if (bl != null) {
        return bl.isLayerVisible();
      }
    }
    return false;
  }

  /**
   * Sets the items of the specified layer to the specified visibility state.
   * (Note: For items that support opened/closed state, only the current state will be considered.)
   * @param layer The layer of the items to change.
   * @param visible The visibility state to set.
   */
  public void setLayerVisible(LayerType layer, boolean visible)
  {
    if (layer != null) {
      @SuppressWarnings("rawtypes")
      BasicLayer bl = layers.get(layer);
      if (bl != null) {
        bl.setLayerVisible(visible);
      }
    }
  }

  /**
   * Attempts to find the LayerObject instance the specified item belongs to.
   * @param item The AbstractLayerItem object.
   * @return A LayerObject instance if a match has been found, <code>null</code> otherwise.
   */
  public LayerObject getLayerObjectOf(AbstractLayerItem item)
  {
    if (item != null) {
      for (final LayerType type: LayerType.values())
      {
        @SuppressWarnings("rawtypes")
        BasicLayer bl = layers.get(type);
        if (bl != null) {
          LayerObject obj = bl.getLayerObjectOf(item);
          if (obj != null) {
            return obj;
          }
        }
      }
    }
    return null;
  }

  // Loads objects for each layer if the parent resource (are, wed) has changed.
  // If forceReload is true, layer objects are always reloaded.
  private void init(AreResource are, WedResource wed, boolean forced)
  {
    if (are != null && wed != null) {
      boolean areChanged = (are != this.are);
      boolean wedChanged = (wed != this.wed);

      // updating global structures
      if (this.are != are) {
        this.are = are;
      }
      if (this.wed != wed) {
        this.wed = wed;
      }

      for (final LayerType layer: LayerType.values()) {
        switch (layer) {
          case Actor:
          case Region:
          case Entrance:
          case Container:
          case Ambient:
          case Door:
          case Animation:
          case Automap:
          case SpawnPoint:
          case Transition:
          case ProTrap:
            loadLayer(layer, forced || areChanged);
            break;
          case DoorPoly:
          case WallPoly:
            loadLayer(layer, forced || wedChanged);
            break;
          default:
            System.err.println(String.format("Unsupported layer type: %1$s", layer.toString()));
        }
      }
    }
  }

  // (Re-)loads the specified layer
  private int loadLayer(LayerType layer, boolean forced)
  {
    int retVal = 0;
    if (layer != null) {
      switch (layer) {
        case Actor:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerActor obj = new LayerActor(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Region:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerRegion obj = new LayerRegion(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Entrance:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerEntrance obj = new LayerEntrance(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Container:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerContainer obj = new LayerContainer(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Ambient:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerAmbient obj = new LayerAmbient(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Door:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerDoor obj = new LayerDoor(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Animation:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerAnimation obj = new LayerAnimation(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Automap:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerAutomap obj = new LayerAutomap(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case SpawnPoint:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerSpawnPoint obj = new LayerSpawnPoint(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case Transition:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerTransition obj = new LayerTransition(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case ProTrap:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerProTrap obj = new LayerProTrap(are, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case DoorPoly:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerDoorPoly obj = new LayerDoorPoly(wed, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        case WallPoly:
        {
          if (layers.containsKey(layer)) {
            retVal = layers.get(layer).loadLayer(forced);
          } else {
            LayerWallPoly obj = new LayerWallPoly(wed, getViewer());
            layers.put(layer, obj);
            retVal = obj.getLayerObjectCount();
          }
          break;
        }
        default:
          System.err.println(String.format("Unsupported layer type: %1$s", layer.toString()));
      }
    }
    return retVal;
  }
}
