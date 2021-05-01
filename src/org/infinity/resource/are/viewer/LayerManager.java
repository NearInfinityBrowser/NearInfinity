// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.EnumMap;
import java.util.List;

import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.viewer.ViewerConstants.LayerType;
import org.infinity.resource.wed.WedResource;

/**
 * Manages all layer objects of a single ARE map.
 */
public final class LayerManager
{
  // Defines order of drawing
  public static final LayerType[] LayerOrdered = {
    LayerType.ACTOR,
    LayerType.ENTRANCE,
    LayerType.AMBIENT,
    LayerType.PRO_TRAP,
    LayerType.ANIMATION,
    LayerType.SPAWN_POINT,
    LayerType.AUTOMAP,
    LayerType.CONTAINER,
    LayerType.DOOR,
    LayerType.REGION,
    LayerType.TRANSITION,
    LayerType.DOOR_POLY,
    LayerType.WALL_POLY
  };

  private static final EnumMap<LayerType, String> LayerLabels = new EnumMap<>(LayerType.class);
  static {
    LayerLabels.put(LayerType.ACTOR, "Actors");
    LayerLabels.put(LayerType.REGION, "Regions");
    LayerLabels.put(LayerType.ENTRANCE, "Entrances");
    LayerLabels.put(LayerType.CONTAINER, "Containers");
    LayerLabels.put(LayerType.AMBIENT, "Ambient Sounds");
    LayerLabels.put(LayerType.DOOR, "Doors");
    LayerLabels.put(LayerType.ANIMATION, "Background Animations");
    LayerLabels.put(LayerType.AUTOMAP, "Automap Notes");
    LayerLabels.put(LayerType.SPAWN_POINT, "Spawn Points");
    LayerLabels.put(LayerType.TRANSITION, "Map Transitions");
    LayerLabels.put(LayerType.PRO_TRAP, "Projectile Traps");
    LayerLabels.put(LayerType.DOOR_POLY, "Door Polygons");
    LayerLabels.put(LayerType.WALL_POLY, "Wall Polygons");
  }

  private final EnumMap<LayerType, BasicLayer<?, ?>> layers = new EnumMap<>(LayerType.class);
  private final AreaViewer viewer;

  private AreResource are;
  private WedResource wed;
  private boolean scheduleEnabled, animForcedInterpolation;
  private int schedule;
  private Object animInterpolationType;
  private double animFrameRate;

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
    LayerType[] lt = LayerType.values();
    for (int i = 0; i < lt.length; i++) {
      if (lt[i] == layer) {
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
    if (layer == LayerType.AMBIENT) {
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
      for (int i = 0, ltCount = getLayerTypeCount(); i < ltCount; i++) {
          BasicLayer<?, ?> bl = getLayer(getLayerType(i));
        if (bl != null) {
          bl.setScheduleEnabled(scheduleEnabled);
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
      for (int i = 0, ltCount = getLayerTypeCount(); i < ltCount; i++) {
        BasicLayer<?, ?> bl = getLayer(getLayerType(i));
        if (bl != null) {
          bl.setSchedule(this.schedule);
        }
      }
    }
  }

  /**
   * Returns whether the specified layer object is active at the current scheduled time.
   * @param obj The layer object to query
   * @return {@code true} if the layer object is scheduled at the currently set hour or
   *         schedule is disabled, {@code false} otherwise.
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
    close(layer);
    return loadLayer(layer, true);
  }

  /**
   * Removes all layer objects from memory.
   */
  public void close()
  {
    for (LayerType layer: LayerType.values()) {
      BasicLayer<?, ?> bl = layers.get(layer);
      if (bl != null) {
        bl.close();
      }
    }
    layers.clear();
    are = null;
    wed = null;
  }

  /**
   * Removes the specified layer from memory.
   */
  public void close(LayerType layer)
  {
    if (layer != null) {
      BasicLayer<?, ?> bl = layers.get(layer);
      if (bl != null) {
        bl.close();
      }
    }
  }

  /**
   * Returns a layer-specific manager.
   * @param layer
   * @return
   */
  public BasicLayer<?, ?> getLayer(LayerType layer)
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
    BasicLayer<?, ?> bl = layers.get(layer);
    if (bl != null) {
      return bl.getLayerObjectCount();
    }
    return 0;
  }

  /**
   * Returns a list of objects associated with the specified layer.
   * @param layer The layer of the objects
   * @return A list of objects or {@code null} if not found.
   */
  public List<? extends LayerObject> getLayerObjects(LayerType layer)
  {
    BasicLayer<?, ?> bl = layers.get(layer);
    if (bl != null) {
      return bl.getLayerObjects();
    }
    return null;
  }

  /**
   * Returns the current state of the door.
   * @return Either {@code ViewerConstants.DOOR_OPEN} or {@code ViewerConstants.DOOR_CLOSED}.
   */
  public int getDoorState()
  {
    return ((LayerDoor)getLayer(LayerType.DOOR)).getDoorState();
  }

  /**
   * Sets the door state used by certain layers. Automatically updates the visibility state of
   * related layers.
   * @param state Either {@code ViewerConstants.DOOR_OPEN} or {@code ViewerConstants.DOOR_CLOSED}.
   */
  public void setDoorState(int state)
  {
    ((LayerDoor)getLayer(LayerType.DOOR)).setDoorState(state);
    ((LayerDoorPoly)getLayer(LayerType.DOOR_POLY)).setDoorState(state);
  }

  /**
   * Returns whether to show iconic representations of actors or the real thing.
   */
  public boolean isRealActorEnabled()
  {
    LayerActor layer = (LayerActor)getLayer(ViewerConstants.LayerType.ACTOR);
    if (layer != null) {
      return layer.isRealActorEnabled();
    }
    return false;
  }

  /**
   * Specify whether to show iconic representations of actors or the real thing.
   */
  public void setRealActorEnabled(boolean enable)
  {
    LayerActor layer = (LayerActor)getLayer(ViewerConstants.LayerType.ACTOR);
    if (layer != null) {
      layer.setRealActorEnabled(enable);
    }
  }

  /**
   * Returns whether to show iconic representations of background animations or the real thing.
   */
  public boolean isRealAnimationEnabled()
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.ANIMATION);
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
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.ANIMATION);
    if (layer != null) {
      layer.setRealAnimationEnabled(enable);
    }
  }

  /**
   * Returns whether real actor sprites are enabled and animated.
   */
  public boolean isRealActorPlaying()
  {
    LayerActor layer = (LayerActor)getLayer(ViewerConstants.LayerType.ACTOR);
    if (layer != null) {
      return layer.isRealActorPlaying();
    }
    return false;
  }

  /**
   * Specify whether to animate real actor sprites. Setting to {@code true} implicitly
   * enables real actors.
   */
  public void setRealActorPlaying(boolean play)
  {
    LayerActor layer = (LayerActor)getLayer(ViewerConstants.LayerType.ACTOR);
    if (layer != null) {
      layer.setRealActorPlaying(play);
    }
  }

  /**
   * Returns whether real animations are enabled and animated.
   */
  public boolean isRealAnimationPlaying()
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.ANIMATION);
    if (layer != null) {
      return layer.isRealAnimationPlaying();
    }
    return false;
  }

  /**
   * Specify whether to animate real background animations. Setting to {@code true} implicitly
   * enables real animations.
   */
  public void setRealAnimationPlaying(boolean play)
  {
    LayerAnimation layer = (LayerAnimation)getLayer(ViewerConstants.LayerType.ANIMATION);
    if (layer != null) {
      layer.setRealAnimationPlaying(play);
    }
  }

  /**
   * Returns the currently active interpolation type for real animations.
   * @return Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR, ViewerConstants.TYPE_NEAREST_BILINEAR
   *         or ViewerConstants.TYPE_BICUBIC.
   */
  public Object getRealAnimationInterpolation()
  {
    return animInterpolationType;
  }

  /**
   * Sets the interpolation type for real animations.
   * @param interpolationType Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR,
   *                          ViewerConstants.TYPE_NEAREST_BILINEAR or ViewerConstants.TYPE_BICUBIC.
   */
  public void setRealAnimationInterpolation(Object interpolationType)
  {
    if (interpolationType == ViewerConstants.TYPE_NEAREST_NEIGHBOR ||
        interpolationType == ViewerConstants.TYPE_BILINEAR ||
        interpolationType == ViewerConstants.TYPE_BICUBIC) {
      animInterpolationType = interpolationType;
      LayerAnimation layerAnim = (LayerAnimation)getLayer(LayerType.ANIMATION);
      if (layerAnim != null) {
        layerAnim.setRealAnimationInterpolation(animInterpolationType);
      }
      LayerActor layerActor = (LayerActor)getLayer(LayerType.ACTOR);
      if (layerActor != null) {
        layerActor.setRealActorInterpolation(animInterpolationType);
      }
    }
  }

  /**
   * Returns whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public boolean isRealAnimationForcedInterpolation()
  {
    return animForcedInterpolation;
  }

  /**
   * Specify whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public void setRealAnimationForcedInterpolation(boolean forced)
  {
    animForcedInterpolation = forced;
    LayerAnimation layerAnim = (LayerAnimation)getLayer(LayerType.ANIMATION);
    if (layerAnim != null) {
      layerAnim.setRealAnimationForcedInterpolation(animForcedInterpolation);
    }
    LayerActor layerActor = (LayerActor)getLayer(LayerType.ACTOR);
    if (layerActor != null) {
      layerActor.setRealActorForcedInterpolation(animForcedInterpolation);
    }
  }

  /**
   * Returns the frame rate used for playing back background animations.
   * @return Frame rate in frames/second.
   */
  public double getRealAnimationFrameRate()
  {
    return animFrameRate;
  }

  /**
   * Specify a new frame rate for background animations.
   * @param frameRate Frame rate in frames/second.
   */
  public void setRealAnimationFrameRate(double frameRate)
  {
    frameRate = Math.min(Math.max(frameRate, 1.0), 30.0);
    if (frameRate != this.animFrameRate) {
      animFrameRate = frameRate;
      LayerAnimation layerAnim = (LayerAnimation)getLayer(LayerType.ANIMATION);
      if (layerAnim != null) {
        layerAnim.setRealAnimationFrameRate(animFrameRate);
      }
      LayerActor layerActor = (LayerActor)getLayer(LayerType.ACTOR);
      if (layerActor != null) {
        layerActor.setRealActorFrameRate(animFrameRate);
      }
    }
  }

  /**
   * Returns whether the items of the specified layer are visible.
   * @param layer The layer to check for visibility.
   * @return {@code true} if one or more items of the specified layer are visible, {@code false} otherwise.
   */
  public boolean isLayerVisible(LayerType layer)
  {
    if (layer != null) {
      BasicLayer<?, ?> bl = layers.get(layer);
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
      BasicLayer<?, ?> bl = layers.get(layer);
      if (bl != null) {
        bl.setLayerVisible(visible);
      }
    }
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
          case ACTOR:
          case REGION:
          case ENTRANCE:
          case CONTAINER:
          case AMBIENT:
          case DOOR:
          case ANIMATION:
          case AUTOMAP:
          case SPAWN_POINT:
          case TRANSITION:
          case PRO_TRAP:
            loadLayer(layer, forced || areChanged);
            break;
          case DOOR_POLY:
          case WALL_POLY:
            loadLayer(layer, forced || wedChanged);
            break;
          default:
            System.err.println(String.format("Unsupported layer type: %s", layer.toString()));
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
        case ACTOR:
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
        case REGION:
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
        case ENTRANCE:
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
        case CONTAINER:
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
        case AMBIENT:
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
        case DOOR:
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
        case ANIMATION:
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
        case AUTOMAP:
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
        case SPAWN_POINT:
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
        case TRANSITION:
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
        case PRO_TRAP:
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
        case DOOR_POLY:
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
        case WALL_POLY:
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
          System.err.println(String.format("Unsupported layer type: %s", layer.toString()));
      }
    }
    return retVal;
  }
}
