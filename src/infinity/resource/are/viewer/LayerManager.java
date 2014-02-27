// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.Viewable;
import infinity.resource.are.Actor;
import infinity.resource.are.Ambient;
import infinity.resource.are.Animation;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNote;
import infinity.resource.are.AutomapNotePST;
import infinity.resource.are.Container;
import infinity.resource.are.Door;
import infinity.resource.are.Entrance;
import infinity.resource.are.ITEPoint;
import infinity.resource.are.ProTrap;
import infinity.resource.are.SpawnPoint;
import infinity.resource.wed.WedResource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Manages all layer objects of a single ARE map.
 * @author argent77
 */
public final class LayerManager
{
  // Supported layer types
  public static enum Layer { Actor, Region, Entrance, Container, Ambient, AmbientRange, Door, Animation,
                             Automap, SpawnPoint, Transition, ProTrap, DoorPoly, WallPoly }
  private static final int LayerSize = Layer.values().length;


  // Defines order of drawing
  public static final Layer[] LayerOrdered = new Layer[]{
    Layer.Actor, Layer.Entrance, Layer.Ambient, Layer.ProTrap, Layer.Animation, Layer.SpawnPoint,
    Layer.Automap, Layer.Container, Layer.Door, Layer.Region, Layer.AmbientRange,
    Layer.Transition, Layer.DoorPoly, Layer.WallPoly
  };

  private static final EnumMap<Layer, String> LayerLabels = new EnumMap<Layer, String>(Layer.class);
  private static final EnumMap<Layer, String> LayerAvailabilityFmt = new EnumMap<Layer, String>(Layer.class);
  static {
    LayerLabels.put(Layer.Actor, "Actors");
    LayerLabels.put(Layer.Region, "Regions");
    LayerLabels.put(Layer.Entrance, "Entrances");
    LayerLabels.put(Layer.Container, "Containers");
    LayerLabels.put(Layer.Ambient, "Ambient Sounds");
    LayerLabels.put(Layer.AmbientRange, "Show Sound Ranges");
    LayerLabels.put(Layer.Door, "Doors");
    LayerLabels.put(Layer.Animation, "Background Animations");
    LayerLabels.put(Layer.Automap, "Automap Notes");
    LayerLabels.put(Layer.SpawnPoint, "Spawn Points");
    LayerLabels.put(Layer.Transition, "Map Transitions");
    LayerLabels.put(Layer.ProTrap, "Projectile Traps");
    LayerLabels.put(Layer.DoorPoly, "Door Polygons");
    LayerLabels.put(Layer.WallPoly, "Wall Polygons");
    LayerAvailabilityFmt.put(Layer.Actor, "%1$d actors available");
    LayerAvailabilityFmt.put(Layer.Region, "%1$d regions available");
    LayerAvailabilityFmt.put(Layer.Entrance, "%1$d entrances available");
    LayerAvailabilityFmt.put(Layer.Container, "%1$d containers available");
    LayerAvailabilityFmt.put(Layer.Ambient, "%1$d ambient sounds available");
    LayerAvailabilityFmt.put(Layer.AmbientRange, "%1$d ambient sounds with local radius available");
    LayerAvailabilityFmt.put(Layer.Door, "%1$d doors available");
    LayerAvailabilityFmt.put(Layer.Animation, "%1$d background animations available");
    LayerAvailabilityFmt.put(Layer.Automap, "%1$d automap notes available");
    LayerAvailabilityFmt.put(Layer.SpawnPoint, "%1$d spawn points available");
    LayerAvailabilityFmt.put(Layer.Transition, "%1$d map transitions available");
    LayerAvailabilityFmt.put(Layer.ProTrap, "%1$d projectile traps available");
    LayerAvailabilityFmt.put(Layer.DoorPoly, "%1$d door polygons available");
    LayerAvailabilityFmt.put(Layer.WallPoly, "%1$d wall polygons available");
  }

  private final EnumMap<Layer, List<LayerObject>> layers = new EnumMap<Layer, List<LayerObject>>(Layer.class);
  private final EnumMap<Layer, Boolean> layersVisible = new EnumMap<Layer, Boolean>(Layer.class);
  private final AreaViewer viewer;

  private AreResource are;
  private WedResource wed;
  private int doorState;

  /**
   * Returns the number of supported layer types.
   */
  public static int getLayerTypeCount()
  {
    return LayerSize;
  }

  /**
   * Returns the layer type located at the specified index.
   */
  public static Layer getLayerType(int index)
  {
    if (index >= 0 && index < LayerSize) {
      return Layer.values()[index];
    } else {
      return null;
    }
  }

  /**
   * Returns the index of the specified layer.
   */
  public static int getLayerIndex(Layer layer)
  {
    Layer[] l = Layer.values();
    for (int i = 0; i < LayerSize; i++) {
      if (l[i] == layer) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the label associated with the specified layer.
   */
  public static String getLayerLabel(Layer layer)
  {
    String s = LayerLabels.get(layer);
    if (s != null) {
      return s;
    } else {
      return "";
    }
  }

  public LayerManager(AreResource are, WedResource wed, AreaViewer viewer)
  {
    this.viewer = viewer;
    doorState = ViewerConstants.DOOR_OPEN;
    init(are, wed, true);
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

  public String getLayerAvailability(Layer layer)
  {
    String fmt = LayerAvailabilityFmt.get(layer);
    if (fmt != null && !fmt.isEmpty()) {
      if (layer == Layer.AmbientRange) {
        // special handling for ambient sounds with local radius
        int cnt = 0;
        List<LayerObject> list = getLayerObjects(layer);
        for (int j = 0; j < list.size(); j++) {
          if (((LayerObjectAmbient)list.get(j)).isLocal()) {
            cnt++;
          }
        }
        return String.format(fmt, cnt);
      } else {
        return String.format(fmt, getLayerObjects(layer).size());
      }
    }
    return "";
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
  public int reload(Layer layer)
  {
    switch (layer) {
      case Actor:
        return loadActors(true);
      case Region:
        return loadRegions(true);
      case Entrance:
        return loadEntrances(true);
      case Container:
        return loadContainers(true);
      case Ambient:
        return loadAmbientSounds(true);
      case AmbientRange:
        return loadAmbientRanges(true);
      case Door:
        return loadDoors(true);
      case Animation:
        return loadAnimations(true);
      case Automap:
        return loadAutomapNotes(true);
      case SpawnPoint:
        return loadSpawnPoints(true);
      case Transition:
        return loadTransitions(true);
      case ProTrap:
        return loadProTraps(true);
      case DoorPoly:
        return loadDoorPolys(true);
      case WallPoly:
        return loadWallPolys(true);
      default:
        return 0;
    }
  }

  /**
   * Removes all layer objects from memory.
   */
  public void clear()
  {
    layersVisible.clear();
    for (Layer layer: Layer.values()) {
      List<LayerObject> list = layers.get(layer);
      if (list != null) {
        list.clear();
        list = null;
      }
    }
    layers.clear();
    are = null;
    wed = null;
  }

  /**
   * Returns the number of objects in the specified layer.
   * @param layer The layer to check.
   * @return Number of objects in the specified layer.
   */
  public int getLayerObjectCount(Layer layer)
  {
    List<LayerObject> list = layers.get(layer);
    if (list != null) {
      return list.size();
    } else {
      return 0;
    }
  }

  /**
   * Returns a specific layer object.
   * @param layer The layer of the object.
   * @param index The index of the object.
   * @return The layer object if found, <code>null</code> otherwise.
   */
  public LayerObject getLayerObject(Layer layer, int index)
  {
    List<LayerObject> list = layers.get(layer);
    if (list != null && index >= 0 && index < list.size()) {
      return list.get(index);
    } else {
      return null;
    }
  }

  /**
   * Returns a list of objects associated with the specified layer.
   * @param layer The layer of the objects
   * @return A list of objects or <code>null</code> if not found.
   */
  public List<LayerObject> getLayerObjects(Layer layer)
  {
    return layers.get(layer);
  }

  /**
   * Sets the door state used by certain layers. Automatically updates the visibility state of
   * related layers.
   * @param state One of <code>Open</code> or <code>Closed</code>.
   */
  public void setDoorState(int state)
  {
    state = (state == ViewerConstants.DOOR_CLOSED) ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
    if (state != doorState) {
      doorState = state;
      setLayerVisible(Layer.Door, isLayerVisible(Layer.Door));
      setLayerVisible(Layer.DoorPoly, isLayerVisible(Layer.DoorPoly));
    }
  }

  /**
   * Returns whether the items of the specified layer are visible.
   * @param layer The layer to check for visibility.
   * @return <code>true</code> if one or more items of the specified layer are visible, <code>false</code> otherwise.
   */
  public boolean isLayerVisible(Layer layer)
  {
    if (layer != null && layersVisible.containsKey(layer)) {
      return layersVisible.get(layer);
    } else {
      return false;
    }
  }

  /**
   * Sets the items of the specified layer to the specified visibility state.
   * (Note: For items that support opened/closed state, only the current state will be considered.)
   * @param layer The layer of the items to change.
   * @param visible The visibility state to set.
   */
  public void setLayerVisible(Layer layer, boolean visible)
  {
    if (layer != null && layersVisible.containsKey(layer)) {
      layersVisible.put(layer, visible);
      List<LayerObject> list = getLayerObjects(layer);
      int oppositeDoorState = (doorState == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
      if (list != null && !list.isEmpty()) {
        for (int i = 0; i < list.size(); i++) {
          switch (layer) {
            case Door:
            {
              // affect only door items of specific state
              AbstractLayerItem item = ((LayerObjectDoor)list.get(i)).getLayerItem(doorState);
              if (item != null) {
                item.setVisible(visible);
              }
              // inactive door states are always invisible
              item = ((LayerObjectDoor)list.get(i)).getLayerItem(oppositeDoorState);
              if (item != null) {
                item.setVisible(false);
              }
              break;
            }
            case DoorPoly:
            {
              // affect only door poly items of specific state
              AbstractLayerItem[] items = ((LayerObjectDoorPoly)list.get(i)).getLayerItems(doorState);
              if (items != null) {
                for (int j = 0; j < items.length; j++) {
                  if (items[j] != null) {
                    items[j].setVisible(visible);
                  }
                }
              }
              // inactive door states are always invisible
              items = ((LayerObjectDoorPoly)list.get(i)).getLayerItems(oppositeDoorState);
              if (items != null) {
                for (int j = 0; j < items.length; j++) {
                  if (items[j] != null) {
                    items[j].setVisible(false);
                  }
                }
              }
              break;
            }
            case Ambient:
            {
              // process only sound icon items
              AbstractLayerItem item = ((LayerObjectAmbient)list.get(i)).getLayerItem(ViewerConstants.AMBIENT_ICON);
              if (item != null) {
                item.setVisible(visible);
              }
              break;
            }
            case AmbientRange:
            {
              // process only sound range items
              AbstractLayerItem item = ((LayerObjectAmbient)list.get(i)).getLayerItem(ViewerConstants.AMBIENT_RANGE);
              if (item != null) {
                item.setVisible(visible);
              }
              break;
            }
            default:
            {
              AbstractLayerItem[] items = list.get(i).getLayerItems();
              if (items != null) {
                for (int j = 0; j < items.length; j++) {
                  if (items[j] != null) {
                    items[j].setVisible(visible);
                  }
                }
              }
            }
          }
        }
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
      Viewable v = item.getViewable();
      for (int i = 0; i < Layer.values().length; i++) {
        List<LayerObject> list = getLayerObjects(Layer.values()[i]);
        if (list != null) {
          for (int j = 0; j < list.size(); j++) {
            if (!list.get(j).getClassType().isAssignableFrom(v.getClass())) {
              break;
            }
            AbstractLayerItem[] items = list.get(j).getLayerItems();
            if (items != null) {
              for (int k = 0; k < items.length; k++) {
                if (items[k] == item) {
                  return list.get(j);
                }
              }
            }
          }
        }
      }
    }
    return null;
  }


  // Loads objects for each layer if the parent resource (are, wed) has changed.
  // If forceReload is true, layer objects are always reloaded.
  private void init(AreResource are, WedResource wed, boolean forceReload)
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

      Layer[] ids = Layer.values();
      for (int i = 0; i < ids.length; i++) {
        switch (ids[i]) {
          case Actor:
            loadActors(forceReload || areChanged);
            break;
          case Region:
            loadRegions(forceReload || areChanged);
            break;
          case Entrance:
            loadEntrances(forceReload || areChanged);
            break;
          case Container:
            loadContainers(forceReload || areChanged);
            break;
          case Ambient:
            loadAmbientSounds(forceReload || areChanged);
            break;
          case AmbientRange:
            loadAmbientRanges(forceReload || areChanged);
            break;
          case Door:
              loadDoors(forceReload || areChanged);
            break;
          case Animation:
            loadAnimations(forceReload || areChanged);
            break;
          case Automap:
            loadAutomapNotes(forceReload || areChanged);
            break;
          case SpawnPoint:
            loadSpawnPoints(forceReload || areChanged);
            break;
          case Transition:
              loadTransitions(forceReload || areChanged);
            break;
          case ProTrap:
            loadProTraps(forceReload || areChanged);
            break;
          case DoorPoly:
            loadDoorPolys(forceReload || wedChanged);
            break;
          case WallPoly:
            loadWallPolys(forceReload || wedChanged);
            break;
          default:
            System.err.println("Error: Unknown layer entry");
        }
      }
    }
  }

  private int loadActors(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Actor)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Actors offset");
        SectionCount sc = (SectionCount)are.getAttribute("# actors");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, Actor.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectActor obj = new LayerObjectActor(are, (Actor)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Actor, list);
      layersVisible.put(Layer.Actor, false);
      return list.size();
    } else {
      return layers.get(Layer.Actor).size();
    }
  }

  private int loadRegions(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Region)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Triggers offset");
        SectionCount sc = (SectionCount)are.getAttribute("# triggers");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, ITEPoint.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectRegion obj = new LayerObjectRegion(are, (ITEPoint)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Region, list);
      layersVisible.put(Layer.Region, false);
      return list.size();
    } else {
      return layers.get(Layer.Region).size();
    }
  }

  private int loadEntrances(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Entrance)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Entrances offset");
        SectionCount sc = (SectionCount)are.getAttribute("# entrances");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, Entrance.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectEntrance obj = new LayerObjectEntrance(are, (Entrance)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Entrance, list);
      layersVisible.put(Layer.Entrance, false);
      return list.size();
    } else {
      return layers.get(Layer.Entrance).size();
    }
  }

  private int loadContainers(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Container)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Containers offset");
        SectionCount sc = (SectionCount)are.getAttribute("# containers");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, Container.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectContainer obj = new LayerObjectContainer(are, (Container)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Container, list);
      layersVisible.put(Layer.Container, false);
      return list.size();
    } else {
      return layers.get(Layer.Container).size();
    }
  }

  private int loadAmbientSounds(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Ambient)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Ambients offset");
        SectionCount sc = (SectionCount)are.getAttribute("# ambients");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, Ambient.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectAmbient obj = new LayerObjectAmbient(are, (Ambient)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Ambient, list);
      layersVisible.put(Layer.Ambient, false);
      return list.size();
    } else {
      return layers.get(Layer.Ambient).size();
    }
  }

  // Special: References layer objects from the ambient list that have local sound radius.
  private int loadAmbientRanges(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.AmbientRange)) {
      List<LayerObject> list = null;
      if (!layers.containsKey(Layer.Ambient)) {
        loadAmbientSounds(true);
      }
      List<LayerObject> listBase = layers.get(Layer.Ambient);
      if (listBase != null) {
        list = new ArrayList<LayerObject>(listBase.size());
        for (int i = 0; i < listBase.size(); i++) {
          LayerObjectAmbient obj = (LayerObjectAmbient)listBase.get(i);
          if (obj.isLocal()) {
            list.add(obj);
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.AmbientRange, list);
      layersVisible.put(Layer.AmbientRange, false);
      return list.size();
    } else {
      return layers.get(Layer.AmbientRange).size();
    }
  }

  private int loadDoors(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Door)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Doors offset");
        SectionCount sc = (SectionCount)are.getAttribute("# doors");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, Door.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectDoor obj = new LayerObjectDoor(are, (Door)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Door, list);
      layersVisible.put(Layer.Door, false);
      return list.size();
    } else {
      return layers.get(Layer.Door).size();
    }
  }

  private int loadAnimations(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Animation)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Animations offset");
        SectionCount sc = (SectionCount)are.getAttribute("# animations");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, Animation.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectAnimation obj = new LayerObjectAnimation(are, (Animation)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Animation, list);
      layersVisible.put(Layer.Animation, false);
      return list.size();
    } else {
      return layers.get(Layer.Animation).size();
    }
  }

  private int loadAutomapNotes(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Automap)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Automap notes offset");
        SectionCount sc = (SectionCount)are.getAttribute("# automap notes");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          if ((ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)) {
            List<StructEntry> structList = getStruct(are, ofs, count, AutomapNotePST.class);
            for (int i = 0; i < count; i++) {
              if (i < structList.size()) {
                LayerObjectAutomapPST obj = new LayerObjectAutomapPST(are, (AutomapNotePST)structList.get(i));
                setListeners(obj);
                list.add(obj);
              }
            }
          } else {
            List<StructEntry> structList = getStruct(are, ofs, count, AutomapNote.class);
            for (int i = 0; i < count; i++) {
              if (i < structList.size()) {
                LayerObjectAutomap obj = new LayerObjectAutomap(are, (AutomapNote)structList.get(i));
                setListeners(obj);
                list.add(obj);
              }
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Automap, list);
      layersVisible.put(Layer.Automap, false);
      return list.size();
    } else {
      return layers.get(Layer.Automap).size();
    }
  }

  private int loadSpawnPoints(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.SpawnPoint)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Spawn points offset");
        SectionCount sc = (SectionCount)are.getAttribute("# spawn points");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, SpawnPoint.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectSpawnPoint obj = new LayerObjectSpawnPoint(are, (SpawnPoint)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.SpawnPoint, list);
      layersVisible.put(Layer.SpawnPoint, false);
      return list.size();
    } else {
      return layers.get(Layer.SpawnPoint).size();
    }
  }

  private int loadTransitions(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.Transition)) {
      List<LayerObject> list = null;
      if (are != null) {
        list = new ArrayList<LayerObject>(4);
        for (int i = 0; i < LayerObjectTransition.FieldName.length; i++) {
          ResourceRef ref = (ResourceRef)are.getAttribute(LayerObjectTransition.FieldName[i]);
          if (ref != null && !ref.getResourceName().isEmpty() && !"None".equalsIgnoreCase(ref.getResourceName())) {
            try {
              AreResource destAre = new AreResource(ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
              LayerObjectTransition obj = new LayerObjectTransition(are, destAre, i, viewer.getRenderer());
              setListeners(obj);
              list.add(obj);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.Transition, list);
      layersVisible.put(Layer.Transition, false);
      return list.size();
    } else {
      return layers.get(Layer.Transition).size();
    }
  }

  private int loadProTraps(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.ProTrap)) {
      List<LayerObject> list = null;
      if (are != null) {
        SectionOffset so = (SectionOffset)are.getAttribute("Projectile traps offset");
        SectionCount sc = (SectionCount)are.getAttribute("# projectile traps");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(are, ofs, count, ProTrap.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectProTrap obj = new LayerObjectProTrap(are, (ProTrap)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.ProTrap, list);
      layersVisible.put(Layer.ProTrap, false);
      return list.size();
    } else {
      return layers.get(Layer.ProTrap).size();
    }
  }

  private int loadDoorPolys(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.DoorPoly)) {
      List<LayerObject> list = null;
      if (wed != null) {
        SectionOffset so = (SectionOffset)wed.getAttribute("Doors offset");
        SectionCount sc = (SectionCount)wed.getAttribute("# doors");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(wed, ofs, count, infinity.resource.wed.Door.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectDoorPoly obj = new LayerObjectDoorPoly(wed, (infinity.resource.wed.Door)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.DoorPoly, list);
      layersVisible.put(Layer.DoorPoly, false);
      return list.size();
    } else {
      return layers.get(Layer.DoorPoly).size();
    }
  }

  private int loadWallPolys(boolean forceLoad)
  {
    if (forceLoad || !layers.containsKey(Layer.WallPoly)) {
      List<LayerObject> list = null;
      if (wed != null) {
        SectionOffset so = (SectionOffset)wed.getAttribute("Wall polygons offset");
        SectionCount sc = (SectionCount)wed.getAttribute("# wall polygons");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          list = new ArrayList<LayerObject>(count);
          List<StructEntry> structList = getStruct(wed, ofs, count, infinity.resource.wed.WallPolygon.class);
          for (int i = 0; i < count; i++) {
            if (i < structList.size()) {
              LayerObjectWallPoly obj = new LayerObjectWallPoly(wed, (infinity.resource.wed.WallPolygon)structList.get(i));
              setListeners(obj);
              list.add(obj);
            }
          }
        }
      }
      if (list == null) {
        list = new ArrayList<LayerObject>();
      }
      layers.put(Layer.WallPoly, list);
      layersVisible.put(Layer.WallPoly, false);
      return list.size();
    } else {
      return layers.get(Layer.WallPoly).size();
    }
  }


  // Returns a list of structures matching the specified arguments
  private List<StructEntry> getStruct(AbstractStruct parent, int baseOfs, int count, Class<? extends StructEntry> classType)
  {
    List<StructEntry> outList = null;
    if (parent != null && baseOfs >= 0 && count > 0 && classType != null) {
      List<StructEntry> list = parent.getList();
      outList = new ArrayList<StructEntry>(count);
      int cnt = 0;
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).getOffset() >= baseOfs && list.get(i).getClass().isAssignableFrom(classType)) {
          outList.add(list.get(i));
          cnt++;
          if(cnt >= count) {
            break;
          }
        }
      }
    } else {
      outList = new ArrayList<StructEntry>();
    }
    return outList;
  }

  // Adds listeners to the layer items provided by the specified LayerObject instance.
  private void setListeners(LayerObject obj)
  {
    if (obj != null && viewer != null) {
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
}
