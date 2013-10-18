// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.RemovableDecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.Actor;
import infinity.resource.are.Ambient;
import infinity.resource.are.Animation;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNote;
import infinity.resource.are.AutomapNotePST;
import infinity.resource.are.Door;
import infinity.resource.are.Entrance;
import infinity.resource.are.ITEPoint;
import infinity.resource.are.ProTrap;
import infinity.resource.are.RestSpawn;
import infinity.resource.are.Song;
import infinity.resource.are.SpawnPoint;
import infinity.resource.are.TiledObject;
import infinity.resource.are.Variable;
import infinity.resource.vertex.Vertex;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.WallPolygon;
import infinity.resource.wed.Wallgroup;
import infinity.resource.wed.WedResource;
import infinity.util.ArrayUtil;
import infinity.util.DynamicArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

/**
 * Manages map structures for the AreaViewer.
 * @author argent77
 */
public class AreaStructure
{
  // identifies preprocessed lists of StructEntry objects
  public static enum Structure {
    // super structures
    ARE, WED,
    // ARE related structures
    ACTOR, AMBIENT, ANIMATION, AUTOMAP, CONTAINER, ENTRANCE, EXPLORED, PROTRAP, REGION,
    REST, SONG, SPAWNPOINT, TILE, VARIABLE,
    // WED related structures
    DOORPOLY, DOORTILE, OVERLAY, POLYGONINDEX, TILEINDEX, TILEMAP, WALLGROUP, WALLPOLY,
    // used in both super structures
    DOOR, VERTEX
  }

  // preprocessed map structures
  private final EnumMap<Structure, EnumMap<Structure, List<StructEntry>>> structures =
      new EnumMap<Structure, EnumMap<Structure, List<StructEntry>>>(Structure.class);

  private final AreaViewer viewer;

  public AreaStructure(AreaViewer viewer)
  {
    if (viewer == null)
      throw new NullPointerException();

    this.viewer = viewer;
  }

  public void clear()
  {
    Collection<EnumMap<Structure, List<StructEntry>>> baseMapCol = structures.values();
    for (final EnumMap<Structure, List<StructEntry>> mapCol: baseMapCol) {
      Collection<List<StructEntry>> listCol = mapCol.values();
      for (final List<StructEntry> list: mapCol.values()) {
        list.clear();
      }
      listCol.clear();
    }
    baseMapCol.clear();
    structures.clear();
  }

  // initialize ARE and WED structure entries
  public void init(WedResource wed)
  {
    // preparing map entries
    initAre();
    initWed(wed);
  }

  public void initAre()
  {
    // removing old ARE entry
    if (structures.containsKey(Structure.ARE)) {
      structures.remove(Structure.ARE);
    }

    // adding new ARE super structure
    EnumMap<Structure, List<StructEntry>> structMap = new EnumMap<Structure, List<StructEntry>>(Structure.class);
    ArrayList<StructEntry> entryList = new ArrayList<StructEntry>();
    entryList.add(viewer.getAre());
    structMap.put(Structure.ARE, entryList);
    structures.put(Structure.ARE, structMap);

    // adding actors
    initAreActor(structMap);
    // adding regions
    initAreRegion(structMap);
    // adding spawn points
    initAreSpawnPoint(structMap);
    // adding entrances
    initAreEntrance(structMap);
    // adding containers
    initAreContainer(structMap);
    // adding ambients
    initAreAmbient(structMap);
    // adding variables
    initAreVariable(structMap);
    // adding "explored" bitmask
    initAreExplored(structMap);
    // adding doors
    initAreDoor(structMap);
    // adding animations
    initAreAnimation(structMap);
    // adding automap notes
    initAreAutomap(structMap);
    // adding tiled objects
    initAreTile(structMap);
    // adding projectile traps
    initAreProTrap(structMap);
    // adding song entries
    initAreSong(structMap);
    // adding rest interruptions
    initAreRest(structMap);
    // adding vertices
    initAreVertex(structMap);
  }

  public void initWed(WedResource wed)
  {
    // *** remove old WED entry ***
    if (structures.containsKey(Structure.WED)) {
      structures.remove(Structure.WED);
    }

    // *** create and add new WED entry ***
    if (wed != null) {
      EnumMap<Structure, List<StructEntry>> structMap = new EnumMap<Structure, List<StructEntry>>(Structure.class);
      ArrayList<StructEntry> entryList = new ArrayList<StructEntry>();
      entryList.add(wed);
      structMap.put(Structure.WED, entryList);
      structures.put(Structure.WED, structMap);

      // adding overlays
      initWedOverlay(structMap);
      // adding doors
      initWedDoor(structMap);
      // adding tile maps
      initWedTileIndex(structMap);
      // adding door tiles
      initWedDoorTile(structMap);
      // adding tile indices
      initWedTileIndex(structMap);
      // adding wall groups
      initWedWallGroup(structMap);
      // adding polygons (walls)
      initWedWallPoly(structMap);
      // adding polygons (doors)
      initWedDoorPoly(structMap);
      // adding polygon indices
      initWedPolyIndex(structMap);
      // adding vertices
      initWedVertex(structMap);
    }
  }

  public void initAreActor(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.ACTOR)) {
      areMap.remove(Structure.ACTOR);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Actors offset");
    SectionCount sc = (SectionCount)are.getAttribute("# actors");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            Actor entry = (Actor)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.ACTOR, list);
    }
  }

  public void initAreRegion(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.REGION)) {
      areMap.remove(Structure.REGION);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Triggers offset");
    SectionCount sc = (SectionCount)are.getAttribute("# triggers");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            ITEPoint entry = (ITEPoint)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.REGION, list);
    }
  }

  public void initAreSpawnPoint(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.SPAWNPOINT)) {
      areMap.remove(Structure.SPAWNPOINT);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Spawn points offset");
    SectionCount sc = (SectionCount)are.getAttribute("# spawn points");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            SpawnPoint entry = (SpawnPoint)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.SPAWNPOINT, list);
    }
  }

  public void initAreEntrance(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.ENTRANCE)) {
      areMap.remove(Structure.ENTRANCE);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Entrances offset");
    SectionCount sc = (SectionCount)are.getAttribute("# entrances");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            Entrance entry = (Entrance)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.ENTRANCE, list);
    }
  }

  public void initAreContainer(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.CONTAINER)) {
      areMap.remove(Structure.CONTAINER);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Containers offset");
    SectionCount sc = (SectionCount)are.getAttribute("# containers");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            infinity.resource.are.Container entry = (infinity.resource.are.Container)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.CONTAINER, list);
    }
  }

  public void initAreAmbient(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.AMBIENT)) {
      areMap.remove(Structure.AMBIENT);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Ambients offset");
    SectionCount sc = (SectionCount)are.getAttribute("# ambients");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            Ambient entry = (Ambient)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.AMBIENT, list);
    }
  }

  public void initAreVariable(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.VARIABLE)) {
      areMap.remove(Structure.VARIABLE);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Variables offset");
    SectionCount sc = (SectionCount)are.getAttribute("# variables");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            Variable entry = (Variable)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.VARIABLE, list);
    }
  }

  public void initAreExplored(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.EXPLORED)) {
      areMap.remove(Structure.EXPLORED);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Explored bitmap offset");
    SectionCount sc = (SectionCount)are.getAttribute("Explored bitmap size");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        Unknown entry = (Unknown)are.getAttribute("Explored bitmap");
        if (entry != null) {
          list.add(entry);
        }
      }
      areMap.put(Structure.EXPLORED, list);
    }
  }

  public void initAreDoor(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.DOOR)) {
      areMap.remove(Structure.DOOR);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Doors offset");
    SectionCount sc = (SectionCount)are.getAttribute("# doors");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            Door entry = (Door)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.DOOR, list);
    }
  }

  public void initAreAnimation(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.ANIMATION)) {
      areMap.remove(Structure.ANIMATION);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Animations offset");
    SectionCount sc = (SectionCount)are.getAttribute("# animations");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            Animation entry = (Animation)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.ANIMATION, list);
    }
  }

  public void initAreAutomap(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.AUTOMAP)) {
      areMap.remove(Structure.AUTOMAP);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Automap notes offset");
    SectionCount sc = (SectionCount)are.getAttribute("# automap notes");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        int gameID = ResourceFactory.getGameID();
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            StructEntry entry = null;
            if (gameID == ResourceFactory.ID_TORMENT) {
              entry = (AutomapNotePST)areEntries.get(i);
            } else {
              entry = (AutomapNote)areEntries.get(i);
            }
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.AUTOMAP, list);
    }
  }

  public void initAreTile(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.TILE)) {
      areMap.remove(Structure.TILE);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Tiled objects offset");
    SectionCount sc = (SectionCount)are.getAttribute("# tiled objects");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      if (so.getValue() > 0 && sc.getValue() > 0) {
        List<StructEntry> areEntries = are.getList();
        int firstEntry = -1;
        for (int i = 0; i < areEntries.size(); i++) {
          if (areEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < areEntries.size()) {
          for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
            TiledObject entry = (TiledObject)areEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
      }
      areMap.put(Structure.TILE, list);
    }
  }

  public void initAreProTrap(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.PROTRAP)) {
      areMap.remove(Structure.PROTRAP);
    }

    // adding new list
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BGEE) {
      AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
      SectionOffset so = (SectionOffset)are.getAttribute("Projectile traps offset");
      SectionCount sc = (SectionCount)are.getAttribute("# projectile traps");
      if (so != null && sc != null) {
        List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
        if (so.getValue() > 0 && sc.getValue() > 0) {
          List<StructEntry> areEntries = are.getList();
          int firstEntry = -1;
          for (int i = 0; i < areEntries.size(); i++) {
            if (areEntries.get(i).getOffset() == so.getValue()) {
              firstEntry = i;
              break;
            }
          }
          if (firstEntry >= 0 && firstEntry < areEntries.size()) {
            for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
              ProTrap entry = (ProTrap)areEntries.get(i);
              if (entry != null) {
                list.add(entry);
              }
            }
          }
        }
        areMap.put(Structure.PROTRAP, list);
      }
    }
  }

  public void initAreSong(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.SONG)) {
      areMap.remove(Structure.SONG);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Songs offset");
    if (so != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(1);
      if (so.getValue() > 0) {
        Song entry = (Song)are.getAttribute("Songs");
        if (entry != null) {
          list.add(entry);
        }
      }
      areMap.put(Structure.SONG, list);
    }
  }

  public void initAreRest(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.REST)) {
      areMap.remove(Structure.REST);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    SectionOffset so = (SectionOffset)are.getAttribute("Rest encounters offset");
    if (so != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(1);
      if (so.getValue() > 0) {
        RestSpawn entry = (RestSpawn)are.getAttribute("Rest encounters");
        if (entry != null) {
          list.add(entry);
        }
      }
      areMap.put(Structure.REST, list);
    }
  }

  // must be called AFTER initAreRegion(), initAreContainer() and initAreDoor()
  public void initAreVertex(EnumMap<Structure, List<StructEntry>> areMap)
  {
    // removing old list
    if (areMap.containsKey(Structure.VERTEX)) {
      areMap.remove(Structure.VERTEX);
    }

    // adding new list
    AreResource are = (AreResource)areMap.get(Structure.ARE).get(0);
    HexNumber so = (HexNumber)are.getAttribute("Vertices offset");
    DecNumber sc = (DecNumber)are.getAttribute("# vertices");
    if (so != null && sc != null) {
      List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
      List<List<StructEntry>> listOfLists = new ArrayList<List<StructEntry>>(2);
      listOfLists.add(getStructureList(Structure.ARE, Structure.REGION));
      listOfLists.add(getStructureList(Structure.ARE, Structure.CONTAINER));
      listOfLists.add(getStructureList(Structure.ARE, Structure.DOOR));
      // parsing each polygon-related structure, adding all polygon entries to the list
      for (List<StructEntry> entryList: listOfLists) {
        if (entryList != null) {
          for (int i = 0; i < entryList.size(); i++) {
            for (final StructEntry entry: ((AbstractStruct)entryList.get(i)).getList()) {
              if (entry instanceof Vertex) {
                list.add(entry);
              }
            }
          }
        }
      }
      // sorting polygon entries by offset
      Collections.sort(list, new Comparator<StructEntry>() {
        @Override
        public int compare(StructEntry e1, StructEntry e2) {
          return e1.getOffset() - e2.getOffset();
        }
      });
      // removing duplicate entries
      int i = 1;
      while (i < list.size()) {
        if (list.get(i).getOffset() == list.get(i-1).getOffset()) {
          list.remove(i);
          continue;
        }
        i++;
      }
      areMap.put(Structure.VERTEX, list);
    }
  }

  public void initWedOverlay(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.OVERLAY)) {
      wedMap.remove(Structure.OVERLAY);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      SectionOffset so = (SectionOffset)wed.getAttribute("Overlays offset");
      SectionCount sc = (SectionCount)wed.getAttribute("# overlays");
      if (so != null && sc != null) {
        List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
        if (so.getValue() > 0 && sc.getValue() > 0) {
          List<StructEntry> wedEntries = wed.getList();
          int firstEntry = -1;
          for (int i = 0; i < wedEntries.size(); i++) {
            if (wedEntries.get(i).getOffset() == so.getValue()) {
              firstEntry = i;
              break;
            }
          }
          if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
            for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
              Overlay entry = (Overlay)wedEntries.get(i);
              if (entry != null) {
                list.add(entry);
              }
            }
          }
        }
        wedMap.put(Structure.OVERLAY, list);
      }
    }
  }

  public void initWedDoor(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.DOOR)) {
      wedMap.remove(Structure.DOOR);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      SectionOffset so = (SectionOffset)wed.getAttribute("Doors offset");
      SectionCount sc = (SectionCount)wed.getAttribute("# doors");
      if (so != null && sc != null) {
        List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
        if (so.getValue() > 0 && sc.getValue() > 0) {
          List<StructEntry> wedEntries = wed.getList();
          int firstEntry = -1;
          for (int i = 0; i < wedEntries.size(); i++) {
            if (wedEntries.get(i).getOffset() == so.getValue()) {
              firstEntry = i;
              break;
            }
          }
          if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
            for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
              infinity.resource.wed.Door entry = (infinity.resource.wed.Door)wedEntries.get(i);
              if (entry != null) {
                list.add(entry);
              }
            }
          }
        }
        wedMap.put(Structure.DOOR, list);
      }
    }
  }

  // must be called AFTER initWedOverlay()
  public void initWedDoorTile(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.DOORTILE)) {
      wedMap.remove(Structure.DOORTILE);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      HexNumber so = (HexNumber)wed.getAttribute("Door tilemap lookup offset");

      // getting correct number of door tilemaps
      int tileCount = -1;
      if (wedMap.containsKey(Structure.OVERLAY) && !wedMap.get(Structure.OVERLAY).isEmpty()) {
        Overlay ovl = (Overlay)wedMap.get(Structure.OVERLAY).get(0);
        HexNumber scOvl = (HexNumber)ovl.getAttribute("Tilemap lookup offset");
        final int size = 2;
        tileCount = (scOvl.getValue() - so.getValue()) / size;
      }

      if (tileCount >= 0) {
        List<StructEntry> list = new ArrayList<StructEntry>(tileCount);
        List<StructEntry> wedEntries = wed.getFlatList();
        int firstEntry = -1;
        for (int i = 0; i < wedEntries.size(); i++) {
          if (wedEntries.get(i).getOffset() == so.getValue()) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
          for (int i = firstEntry; i < firstEntry + tileCount; i++) {
            RemovableDecNumber entry = (RemovableDecNumber)wedEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
        wedMap.put(Structure.DOORTILE, list);
      }
    }
  }

  // must be called AFTER initWedOverlay()
  public void initWedTileIndex(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.TILEINDEX)) {
      wedMap.remove(Structure.TILEINDEX);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      int tileOfs = -1;
      int tileCount = -1;
      if (wedMap.containsKey(Structure.OVERLAY) && !wedMap.get(Structure.OVERLAY).isEmpty()) {
        Overlay ovl = (Overlay)wedMap.get(Structure.OVERLAY).get(0);
        SectionOffset so = (SectionOffset)ovl.getAttribute("Tilemap lookup offset");
        tileOfs = so.getValue();
        SectionOffset so2 = (SectionOffset)wed.getAttribute("Wall groups offset");
        final int size = 2;
        tileCount = (so2.getValue() - so.getValue()) / size;
      }

      if (tileOfs > 0 && tileCount >= 0) {
        List<StructEntry> list = new ArrayList<StructEntry>(tileCount);
        List<StructEntry> wedEntries = wed.getFlatList();
        int firstEntry = -1;
        for (int i = 0; i < wedEntries.size(); i++) {
          if (wedEntries.get(i).getOffset() == tileOfs) {
            firstEntry = i;
            break;
          }
        }
        if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
          for (int i = firstEntry; i < firstEntry + tileCount; i++) {
            DecNumber entry = (DecNumber)wedEntries.get(i);
            if (entry != null) {
              list.add(entry);
            }
          }
        }
        wedMap.put(Structure.TILEINDEX, list);
      }
    }
  }

  public void initWedWallGroup(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.WALLGROUP)) {
      wedMap.remove(Structure.WALLGROUP);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      SectionOffset so = (SectionOffset)wed.getAttribute("Wall groups offset");
      if (so != null) {
        HexNumber[] offsets =
            new HexNumber[]{so, (HexNumber)wed.getAttribute("Overlays offset"),
                            (HexNumber)wed.getAttribute("Second header offset"),
                            (HexNumber)wed.getAttribute("Doors offset"),
                            (HexNumber)wed.getAttribute("Door tilemap lookup offset"),
                            (HexNumber)wed.getAttribute("Wall polygons offset"),
                            (HexNumber)wed.getAttribute("Wall polygon lookup offset"),
                            new HexNumber(DynamicArray.convertInt(wed.getSize()), 0, 4, "")};
        Arrays.sort(offsets, new Comparator<HexNumber>() {
          @Override
          public int compare(HexNumber s1, HexNumber s2) {
            return ((s1 != null) ? s1.getValue() : 0) - ((s2 != null) ? s2.getValue() : 0);
          }
        });
        final int sizeWallgroup = 4;   // XXX: get structure size dynamically
        int count = (offsets[ArrayUtil.indexOf(offsets, so) + 1].getValue() - so.getValue()) / sizeWallgroup;
        if (so.getValue() > 0 && count >= 0) {
          List<StructEntry> list = new ArrayList<StructEntry>(count);
          List<StructEntry> wedEntries = wed.getList();
          int firstEntry = -1;
          for (int i = 0; i < wedEntries.size(); i++) {
            if (wedEntries.get(i).getOffset() == so.getValue()) {
              firstEntry = i;
              break;
            }
          }
          if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
            for (int i = firstEntry; i < firstEntry + count; i++) {
              Wallgroup entry = (Wallgroup)wedEntries.get(i);
              if (entry != null) {
                list.add(entry);
              }
            }
          }
          wedMap.put(Structure.WALLGROUP, list);
        }
      }
    }
  }

  public void initWedWallPoly(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.WALLPOLY)) {
      wedMap.remove(Structure.WALLPOLY);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      SectionOffset so = (SectionOffset)wed.getAttribute("Wall polygons offset");
      SectionCount sc = (SectionCount)wed.getAttribute("# wall polygons");
      if (so != null && sc != null) {
        List<StructEntry> list = new ArrayList<StructEntry>(sc.getValue());
        if (so.getValue() > 0 && sc.getValue() > 0) {
          List<StructEntry> wedEntries = wed.getList();
          int firstEntry = -1;
          for (int i = 0; i < wedEntries.size(); i++) {
            if (wedEntries.get(i).getOffset() == so.getValue()) {
              firstEntry = i;
              break;
            }
          }
          if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
            for (int i = firstEntry; i < firstEntry + sc.getValue(); i++) {
              WallPolygon entry = (WallPolygon)wedEntries.get(i);
              if (entry != null) {
                list.add(entry);
              }
            }
          }
        }
        wedMap.put(Structure.WALLPOLY, list);
      }
    }
  }

  // must be called AFTER initWedDoor()
  public void initWedDoorPoly(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.DOORPOLY)) {
      wedMap.remove(Structure.DOORPOLY);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      List<StructEntry> list = new ArrayList<StructEntry>();
      // parsing each door entry, adding all polygon entries to the list
      List<StructEntry> doorEntries = getStructureList(Structure.WED, Structure.DOOR);
      if (doorEntries != null) {
        for (int i = 0; i < doorEntries.size(); i++) {
          infinity.resource.wed.Door door = (infinity.resource.wed.Door)doorEntries.get(i);
          for (final StructEntry entry: door.getList()) {
            if (entry instanceof infinity.resource.wed.Polygon) {
              list.add(entry);
            }
          }
        }
      }
      // sorting polygon entries by offset
      Collections.sort(list, new Comparator<StructEntry>() {
        @Override
        public int compare(StructEntry e1, StructEntry e2) {
          return e1.getOffset() - e2.getOffset();
        }
      });
      // removing duplicate entries
      int i = 1;
      while (i < list.size()) {
        if (list.get(i).getOffset() == list.get(i-1).getOffset()) {
          list.remove(i);
          continue;
        }
        i++;
      }
      wedMap.put(Structure.DOORPOLY, list);
    }
  }

  public void initWedPolyIndex(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.POLYGONINDEX)) {
      wedMap.remove(Structure.POLYGONINDEX);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      WedResource wed = (WedResource)wedMap.get(Structure.WED).get(0);
      SectionOffset so = (SectionOffset)wed.getAttribute("Wall polygon lookup offset");
      if (so != null) {
        HexNumber[] offsets =
            new HexNumber[]{so, (HexNumber)wed.getAttribute("Overlays offset"),
                            (HexNumber)wed.getAttribute("Second header offset"),
                            (HexNumber)wed.getAttribute("Doors offset"),
                            (HexNumber)wed.getAttribute("Door tilemap lookup offset"),
                            (HexNumber)wed.getAttribute("Wall polygons offset"),
                            (HexNumber)wed.getAttribute("Wall groups offset"),
                            (HexNumber)wed.getAttribute("Vertices offset"),
                            new HexNumber(DynamicArray.convertInt(wed.getSize()), 0, 4, "")};
        Arrays.sort(offsets, new Comparator<HexNumber>() {
          @Override
          public int compare(HexNumber s1, HexNumber s2) {
            return ((s1 != null) ? s1.getValue() : 0) - ((s2 != null) ? s2.getValue() : 0);
          }
        });
        final int size = 2;
        int count = (offsets[ArrayUtil.indexOf(offsets, so) + 1].getValue() - so.getValue()) / size;
        if (so.getValue() > 0 && count >= 0) {
          List<StructEntry> list = new ArrayList<StructEntry>(count);
          List<StructEntry> wedEntries = wed.getList();
          int firstEntry = -1;
          for (int i = 0; i < wedEntries.size(); i++) {
            if (wedEntries.get(i).getOffset() == so.getValue()) {
              firstEntry = i;
              break;
            }
          }
          if (firstEntry >= 0 && firstEntry < wedEntries.size()) {
            for (int i = firstEntry; i < firstEntry + count; i++) {
              DecNumber entry = (DecNumber)wedEntries.get(i);
              if (entry != null) {
                list.add(entry);
              }
            }
          }
          wedMap.put(Structure.POLYGONINDEX, list);
        }
      }
    }
  }

  //must be called AFTER initWedWallPoly() and initWedDoorPoly()
  public void initWedVertex(EnumMap<Structure, List<StructEntry>> wedMap)
  {
    // removing old list
    if (wedMap.containsKey(Structure.VERTEX)) {
      wedMap.remove(Structure.VERTEX);
    }

    // adding new list
    if (wedMap.containsKey(Structure.WED) && !wedMap.get(Structure.WED).isEmpty()) {
      List<StructEntry> list = new ArrayList<StructEntry>();
      List<List<StructEntry>> listOfLists = new ArrayList<List<StructEntry>>(2);
      listOfLists.add(getStructureList(Structure.WED, Structure.WALLPOLY));
      listOfLists.add(getStructureList(Structure.WED, Structure.DOORPOLY));
      // parsing each wallpoly and door entry, adding all polygon entries to the list
      for (List<StructEntry> entryList: listOfLists) {
        if (entryList != null) {
          for (int i = 0; i < entryList.size(); i++) {
            for (final StructEntry entry: ((AbstractStruct)entryList.get(i)).getList()) {
              if (entry instanceof Vertex) {
                list.add(entry);
              }
            }
          }
        }
      }
      // sorting polygon entries by offset
      Collections.sort(list, new Comparator<StructEntry>() {
        @Override
        public int compare(StructEntry e1, StructEntry e2) {
          return e1.getOffset() - e2.getOffset();
        }
      });
      // removing duplicate entries
      int i = 1;
      while (i < list.size()) {
        if (list.get(i).getOffset() == list.get(i-1).getOffset()) {
          list.remove(i);
          continue;
        }
        i++;
      }
      wedMap.put(Structure.VERTEX, list);
    }
  }

  public List<StructEntry> getStructureList(Structure superStruct, Structure struct)
  {
    if (structures.containsKey(superStruct)) {
      if (structures.get(superStruct).containsKey(struct)) {
        return structures.get(superStruct).get(struct);
      }
    }
    return null;
  }

  public StructEntry getStructureByOffset(Structure superStruct, Structure struct, int offset)
  {
    if (structures.containsKey(superStruct)) {
      if (structures.get(superStruct).containsKey(struct)) {
        List<StructEntry> list = structures.get(superStruct).get(struct);
        for (final StructEntry entry: list) {
          if (entry != null && entry.getOffset() == offset) {
            return entry;
          }
        }
      }
    }
    return null;
  }

  public StructEntry getStructureByIndex(Structure superStruct, Structure struct, int index)
  {
    if (structures.containsKey(superStruct)) {
      if (structures.get(superStruct).containsKey(struct)) {
        List<StructEntry> list = structures.get(superStruct).get(struct);
        if (index >= 0 && index < list.size()) {
          return list.get(index);
        }
      }
    }
    return null;
  }
}
