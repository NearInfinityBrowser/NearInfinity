// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.awt.Component;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasAddRemovable;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.viewer.AreaViewer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.vertex.Vertex;
import org.infinity.search.SearchOptions;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.StringTable;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.infinity.util.io.StreamUtils;

public final class AreResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
  // ARE-specific field labels
  public static final String ARE_WED_RESOURCE             = "WED resource";
  public static final String ARE_LAST_SAVED               = "Last saved";
  public static final String ARE_AREA_TYPE                = "Area type";
  public static final String ARE_AREA_NORTH               = "Area north";
  public static final String ARE_AREA_EAST                = "Area east";
  public static final String ARE_AREA_SOUTH               = "Area south";
  public static final String ARE_AREA_WEST                = "Area west";
  public static final String ARE_EDGE_FLAGS_NORTH         = "Edge flags north";
  public static final String ARE_EDGE_FLAGS_EAST          = "Edge flags east";
  public static final String ARE_EDGE_FLAGS_SOUTH         = "Edge flags south";
  public static final String ARE_EDGE_FLAGS_WEST          = "Edge flags west";
  public static final String ARE_LOCATION                 = "Location";
  public static final String ARE_PROBABILITY_RAIN         = "Rain probability";
  public static final String ARE_PROBABILITY_SNOW         = "Snow probability";
  public static final String ARE_PROBABILITY_FOG          = "Fog probability";
  public static final String ARE_PROBABILITY_LIGHTNING    = "Lightning probability";
  public static final String ARE_WIND_SPEED               = "Wind speed";
  public static final String ARE_OVERLAY_TRANSPARENCY     = "Overlay transparency";
  public static final String ARE_AREA_DIFFICULTY_2        = "Area difficulty 2";
  public static final String ARE_AREA_DIFFICULTY_3        = "Area difficulty 3";
  public static final String ARE_OFFSET_ACTORS            = "Actors offset";
  public static final String ARE_OFFSET_TRIGGERS          = "Triggers offset";
  public static final String ARE_OFFSET_SPAWN_POINTS      = "Spawn points offset";
  public static final String ARE_OFFSET_ENTRANCES         = "Entrances offset";
  public static final String ARE_OFFSET_CONTAINERS        = "Containers offset";
  public static final String ARE_OFFSET_ITEMS             = "Items offset";
  public static final String ARE_OFFSET_VERTICES          = "Vertices offset";
  public static final String ARE_OFFSET_AMBIENTS          = "Ambients offset";
  public static final String ARE_OFFSET_VARIABLES         = "Variables offset";
  public static final String ARE_OFFSET_OBJECT_FLAGS      = "Object flags offset";
  public static final String ARE_OFFSET_EXPLORED_BITMAP   = "Explored bitmap offset";
  public static final String ARE_OFFSET_DOORS             = "Doors offset";
  public static final String ARE_OFFSET_ANIMATIONS        = "Animations offset";
  public static final String ARE_OFFSET_TILED_OBJECTS     = "Tiled objects offset";
  public static final String ARE_OFFSET_SONGS             = "Songs offset";
  public static final String ARE_OFFSET_REST_ENCOUNTERS   = "Rest encounters offset";
  public static final String ARE_OFFSET_AUTOMAP_NOTES     = "Automap notes offset";
  public static final String ARE_OFFSET_PROJECTILE_TRAPS  = "Projectile traps offset";
  public static final String ARE_NUM_ACTORS               = "# actors";
  public static final String ARE_NUM_TRIGGERS             = "# triggers";
  public static final String ARE_NUM_SPAWN_POINTS         = "# spawn points";
  public static final String ARE_NUM_ENTRANCES            = "# entrances";
  public static final String ARE_NUM_CONTAINERS           = "# containers";
  public static final String ARE_NUM_ITEMS                = "# items";
  public static final String ARE_NUM_VERTICES             = "# vertices";
  public static final String ARE_NUM_AMBIENTS             = "# ambients";
  public static final String ARE_NUM_VARIABLES            = "# variables";
  public static final String ARE_NUM_OBJECT_FLAGS         = "# object flags";
  public static final String ARE_NUM_DOORS                = "# doors";
  public static final String ARE_NUM_ANIMATIONS           = "# animations";
  public static final String ARE_NUM_TILED_OBJECTS        = "# tiled objects";
  public static final String ARE_NUM_AUTOMAP_NOTES        = "# automap notes";
  public static final String ARE_NUM_PROJECTILE_TRAPS     = "# projectile traps";
  public static final String ARE_SIZE_EXPLORED_BITMAP     = "Explored bitmap size";
  public static final String ARE_AREA_SCRIPT              = "Area script";
  public static final String ARE_REST_MOVIE_DAY           = "Rest movie (day)";
  public static final String ARE_REST_MOVIE_NIGHT         = "Rest movie (night)";
  public static final String ARE_EXPLORED_BITMAP          = "Explored bitmap";

  public static final String[] s_flag = {"No flags set", "Outdoor", "Day/Night",
                                         "Weather", "City", "Forest", "Dungeon",
                                         "Extended night", "Can rest"};
  public static final String[] s_flag_torment = {"Indoors", "Hive", "Hive Night", "Clerk's ward",
                                                 "Lower ward", "Ravel's maze", "Baator", "Rubikon",
                                                 "Negative material plane", "Curst", "Carceri",
                                                 "Allow day/night"};
  public static final String[] s_atype = {"Normal", "Can't save game", "Tutorial area", "Dead magic zone",
                                          "Dream area"};
  public static final String[] s_atype_ee = {"Normal", "Can't save game", "Tutorial area", "Dead magic zone",
                                             "Dream area", "Player1 can die;Allows death of party leader without ending the game",
                                             "Can't rest", "Can't travel"};
  public static final String[] s_atype_torment = {"Normal", "Can't save game",
                                                  "Can't rest;Combined with bit 2: \"Can't rest without permission\"",
                                                  "Too dangerous to rest;Combined with bit 1: \"Can't rest without permission\""};
  public static final String[] s_atype_pstee = {"Normal", "Can't save game", "", "Dead magic zone",
                                                "Dream area", "Player1 can die;The Nameless One can die without ending the game",
                                                "Can't rest", "Can't travel",
                                                "Can't rest;Combined with bit 8: \"Can't rest without permission\"",
                                                "Too dangerous to rest;Combined with bit 7: \"Can't rest without permission\""};
  public static final String[] s_atype_iwd2 = {"Normal", "Can't save game", "Cannot rest", "Lock battle music"};
  public static final String[] s_edge = {"No flags set", "Party required", "Party enabled"};

  private StructHexViewer hexViewer;
  private AreaViewer areaViewer;

  public static String getSearchString(ResourceEntry entry)
  {
    String retVal = null;
    if (entry != null && ResourceFactory.resourceExists("MAPNAME.2DA")) {
      Table2da table = Table2daCache.get("MAPNAME.2DA");
      if (table != null) {
        String are = entry.getResourceName();
        are = are.substring(0, are.lastIndexOf('.'));
        for (int row = 0, cnt = table.getRowCount(); row < cnt; row++) {
          if (are.equalsIgnoreCase(table.get(row, 0))) {
            try {
              int strref = Integer.parseInt(table.get(row, 1));
              retVal = StringTable.getStringRef(strref);
            } catch (NumberFormatException e) {
            }
          }
        }
      }
    }
    return retVal;
  }

  public static void addScriptNames(Set<String> scriptNames, ByteBuffer buffer)
  {
    int offset = 0;
    if (StreamUtils.readString(buffer, 4, 4).equalsIgnoreCase("V9.1"))
      offset = 16;

    // Actors
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 84),
                   buffer.getShort(offset + 88), 272,
                   Profile.isEnhancedEdition() || Profile.getEngine() == Profile.Engine.BG2); // needed?

    // ITEPoints
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 92),
                   buffer.getShort(offset + 90), 196, false);

    // Spawnpoints
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 96),
                   buffer.getInt(offset + 100), 200, false);

    // Entrances
//    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 104),
//                   buffer.getInt(offset + 108), 104, false);

    // Containers
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 112),
                   buffer.getShort(offset + 116), 192, false);

    // Ambients
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 132),
                   buffer.getShort(offset + 130), 212, false);

    // Variables
//    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 136),
//                   buffer.getInt(offset + 140), 84, false);

    // Doors
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 168),
                   buffer.getInt(offset + 164), 200, false);

    // Animations
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 176),
                   buffer.getInt(offset + 172), 76, false);

    // Tiled objects
    addScriptNames(scriptNames, buffer, buffer.getInt(offset + 184),
                   buffer.getInt(offset + 180), 108, false);

    // Rest spawn
//    addScriptNames(scriptNames, buffer, DynamicArray.getInt(buffer, offset + 192), 1, 228, false);
  }

  private static void addScriptNames(Set<String> scriptNames, ByteBuffer buffer, int offset,
                                     int count, int size, boolean checkOverride)
  {
    for (int i = 0; i < count; i++) {
      int curOfs = offset + i*size;
      // Bit 3 of "Flags" field determines whether to override the actor's script name
      if (!checkOverride || ((buffer.get(curOfs + 40) & 8) == 8)) {
        StringBuilder sb = new StringBuilder(32);
        for (int j = 0; j < 32; j++) {
          byte b = buffer.get(curOfs + j);
          if (b == 0x00) {
            break;
          } else if (b != 0x20) { // Space
            sb.append(Character.toLowerCase((char)b));
          }
        }
        synchronized (scriptNames) {
          scriptNames.add(sb.toString());
        }
      }
    }
  }

  public AreResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

//--------------------- Begin Interface Closeable ---------------------

 @Override
 public void close() throws Exception
 {
   super.close();
   if (areaViewer != null) {
     areaViewer.close();
     areaViewer = null;
   }
 }

//--------------------- End Interface Closeable ---------------------

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    if (Profile.getEngine() == Profile.Engine.PST) {
      return new AddRemovable[]{new Actor(), new ITEPoint(), new SpawnPoint(),
                                new Entrance(), new Container(), new Ambient(),
                                new Variable(), new Door(), new Animation(),
                                new TiledObject(), new AutomapNotePST()};
    } else if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      return new AddRemovable[]{new Actor(), new ITEPoint(), new SpawnPoint(),
                                new Entrance(), new Container(), new Ambient(),
                                new Variable(), new Door(), new Animation(),
                                new TiledObject(), new AutomapNote(),
                                new ProTrap()};
    } else {
      return new AddRemovable[]{new Actor(), new ITEPoint(), new SpawnPoint(),
                                new Entrance(), new Container(), new Ambient(),
                                new Variable(), new Door(), new Animation(),
                                new TiledObject()};
    }
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public boolean confirmRemoveEntry(AddRemovable entry) throws Exception
  {
    return true;
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 2;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return StructViewer.TAB_RAW;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0: // view tab
      {
        JScrollPane scroll = new JScrollPane(new Viewer(this));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
      }
      case 1: // raw tab
      {
        if (hexViewer == null) {
          hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
      }
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

// --------------------- End Interface HasViewerTabs ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    HexNumber offset_vertices = (HexNumber)getAttribute(ARE_OFFSET_VERTICES);
    if (datatype.getOffset() <= offset_vertices.getValue())
      offset_vertices.incValue(datatype.getSize());
    HexNumber offset_items = (HexNumber)getAttribute(ARE_OFFSET_ITEMS);
    if (datatype.getOffset() <= offset_items.getValue())
      offset_items.incValue(datatype.getSize());

    if (datatype instanceof HasVertices)
      updateVertices();
    if (datatype instanceof Container)
      updateItems();
    updateActorCREOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    if (datatype instanceof Vertex)
      updateVertices();
    else {
      HexNumber offset_vertices = (HexNumber)getAttribute(ARE_OFFSET_VERTICES);
      if (datatype.getOffset() <= offset_vertices.getValue()) {
        offset_vertices.incValue(datatype.getSize());
        updateVertices();
      }
    }
    if (datatype instanceof Item)
      updateItems();
    else {
      HexNumber offset_items = (HexNumber)getAttribute(ARE_OFFSET_ITEMS);
      if (datatype.getOffset() <= offset_items.getValue()) {
        offset_items.incValue(datatype.getSize());
        updateItems();
      }
    }
    updateActorCREOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    HexNumber offset_vertices = (HexNumber)getAttribute(ARE_OFFSET_VERTICES);
    if (datatype.getOffset() < offset_vertices.getValue())
      offset_vertices.incValue(-datatype.getSize());
    HexNumber offset_items = (HexNumber)getAttribute(ARE_OFFSET_ITEMS);
    if (datatype.getOffset() < offset_items.getValue())
      offset_items.incValue(-datatype.getSize());

    if (datatype instanceof HasVertices)
      updateVertices();
    if (datatype instanceof Container)
      updateItems();
    updateActorCREOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    if (datatype instanceof Vertex)
      updateVertices();
    else {
      HexNumber offset_vertices = (HexNumber)getAttribute(ARE_OFFSET_VERTICES);
      if (datatype.getOffset() < offset_vertices.getValue()) {
        offset_vertices.incValue(-datatype.getSize());
        updateVertices();
      }
    }
    if (datatype instanceof Item)
      updateItems();
    else {
      HexNumber offset_items = (HexNumber)getAttribute(ARE_OFFSET_ITEMS);
      if (datatype.getOffset() < offset_items.getValue()) {
        offset_items.incValue(-datatype.getSize());
        updateItems();
      }
    }
    updateActorCREOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    TextString version = new TextString(buffer, offset + 4, 4, COMMON_VERSION);
    addField(version);
    addField(new ResourceRef(buffer, offset + 8, ARE_WED_RESOURCE, "WED"));
    addField(new DecNumber(buffer, offset + 16, 4, ARE_LAST_SAVED));
    if (version.toString().equalsIgnoreCase("V9.1")) {
      addField(new Flag(buffer, offset + 20, 4, ARE_AREA_TYPE, getUpdatedIdsFlags(s_atype_iwd2, "AREAFLAG.IDS", 4, false)));
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Flag(buffer, offset + 20, 4, ARE_AREA_TYPE, getUpdatedIdsFlags(s_atype_torment, null, 4, false)));
    } else if (Profile.getGame() == Profile.Game.PSTEE) {
      addField(new Flag(buffer, offset + 20, 4, ARE_AREA_TYPE, getUpdatedIdsFlags(s_atype_pstee, null, 4, false)));
    } else if (Profile.isEnhancedEdition()) {
      addField(new Flag(buffer, offset + 20, 4, ARE_AREA_TYPE, getUpdatedIdsFlags(s_atype_ee, "AREAFLAG.IDS", 4, false)));
    } else {
      addField(new Flag(buffer, offset + 20, 4, ARE_AREA_TYPE, getUpdatedIdsFlags(s_atype, "AREAFLAG.IDS", 4, false)));
    }
    addField(new ResourceRef(buffer, offset + 24, ARE_AREA_NORTH, "ARE"));
    addField(new Flag(buffer, offset + 32, 4, ARE_EDGE_FLAGS_NORTH, s_edge));
    addField(new ResourceRef(buffer, offset + 36, ARE_AREA_EAST, "ARE"));
    addField(new Flag(buffer, offset + 44, 4, ARE_EDGE_FLAGS_EAST, s_edge));
    addField(new ResourceRef(buffer, offset + 48, ARE_AREA_SOUTH, "ARE"));
    addField(new Flag(buffer, offset + 56, 4, ARE_EDGE_FLAGS_SOUTH, s_edge));
    addField(new ResourceRef(buffer, offset + 60, ARE_AREA_WEST, "ARE"));
    addField(new Flag(buffer, offset + 68, 4, ARE_EDGE_FLAGS_WEST, s_edge));
    if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
      addField(new Flag(buffer, offset + 72, 2, ARE_LOCATION, getUpdatedIdsFlags(s_flag_torment, null, 2, false)));
    } else {
      addField(new Flag(buffer, offset + 72, 2, ARE_LOCATION, getUpdatedIdsFlags(s_flag, "AREATYPE.IDS", 2, false)));
    }
    addField(new DecNumber(buffer, offset + 74, 2, ARE_PROBABILITY_RAIN));
    addField(new DecNumber(buffer, offset + 76, 2, ARE_PROBABILITY_SNOW));
    addField(new DecNumber(buffer, offset + 78, 2, ARE_PROBABILITY_FOG));
    addField(new DecNumber(buffer, offset + 80, 2, ARE_PROBABILITY_LIGHTNING));
    if (Profile.isEnhancedEdition()) {
      addField(new UnsignDecNumber(buffer, offset + 82, 1, ARE_OVERLAY_TRANSPARENCY));
      addField(new Unknown(buffer, offset + 83, 1));
    } else {
      addField(new DecNumber(buffer, offset + 82, 2, ARE_WIND_SPEED));
    }
    if (version.toString().equalsIgnoreCase("V9.1")) {
      addField(new DecNumber(buffer, offset + 84, 1, ARE_AREA_DIFFICULTY_2));
      addField(new DecNumber(buffer, offset + 85, 1, ARE_AREA_DIFFICULTY_3));
      addField(new Unknown(buffer, offset + 86, 14));
      offset += 16;
    }
    SectionOffset offset_actors = new SectionOffset(buffer, offset + 84, ARE_OFFSET_ACTORS,
                                                    Actor.class);
    addField(offset_actors);
    SectionCount count_actors = new SectionCount(buffer, offset + 88, 2, ARE_NUM_ACTORS,
                                                 Actor.class);
    addField(count_actors);
    SectionCount count_itepoints = new SectionCount(buffer, offset + 90, 2, ARE_NUM_TRIGGERS,
                                                    ITEPoint.class);
    addField(count_itepoints);
    SectionOffset offset_itepoints = new SectionOffset(buffer, offset + 92,
                                                       ARE_OFFSET_TRIGGERS,
                                                       ITEPoint.class);
    addField(offset_itepoints);
    SectionOffset offset_spoints = new SectionOffset(buffer, offset + 96, ARE_OFFSET_SPAWN_POINTS,
                                                     SpawnPoint.class);
    addField(offset_spoints);
    SectionCount count_spoints = new SectionCount(buffer, offset + 100, 4, ARE_NUM_SPAWN_POINTS,
                                                  SpawnPoint.class);
    addField(count_spoints);
    SectionOffset offset_entrances = new SectionOffset(buffer, offset + 104, ARE_OFFSET_ENTRANCES,
                                                       Entrance.class);
    addField(offset_entrances);
    SectionCount count_entrances = new SectionCount(buffer, offset + 108, 4, ARE_NUM_ENTRANCES,
                                                    Entrance.class);
    addField(count_entrances);
    SectionOffset offset_containers = new SectionOffset(buffer, offset + 112, ARE_OFFSET_CONTAINERS,
                                                        Container.class);
    addField(offset_containers);
    SectionCount count_containers = new SectionCount(buffer, offset + 116, 2, ARE_NUM_CONTAINERS,
                                                     Container.class);
    addField(count_containers);
    DecNumber count_items = new DecNumber(buffer, offset + 118, 2, ARE_NUM_ITEMS);
    addField(count_items);
    HexNumber offset_items = new HexNumber(buffer, offset + 120, 4, ARE_OFFSET_ITEMS);
    addField(offset_items);
    HexNumber offset_vertices = new HexNumber(buffer, offset + 124, 4, ARE_OFFSET_VERTICES);
    addField(offset_vertices);
    DecNumber count_vertices = new DecNumber(buffer, offset + 128, 2, ARE_NUM_VERTICES);
    addField(count_vertices);
    SectionCount count_ambients = new SectionCount(buffer, offset + 130, 2, ARE_NUM_AMBIENTS,
                                                   Ambient.class);
    addField(count_ambients);
    SectionOffset offset_ambients = new SectionOffset(buffer, offset + 132, ARE_OFFSET_AMBIENTS,
                                                      Ambient.class);
    addField(offset_ambients);
    SectionOffset offset_variables = new SectionOffset(buffer, offset + 136, ARE_OFFSET_VARIABLES,
                                                       Variable.class);
    addField(offset_variables);
    SectionCount count_variables = new SectionCount(buffer, offset + 140, 2, ARE_NUM_VARIABLES,
                                                    Variable.class);
    addField(count_variables);
    addField(new DecNumber(buffer, offset + 142, 2, ARE_NUM_OBJECT_FLAGS));
    addField(new HexNumber(buffer, offset + 144, 4, ARE_OFFSET_OBJECT_FLAGS));
    addField(new ResourceRef(buffer, offset + 148, ARE_AREA_SCRIPT, "BCS"));
    SectionCount size_exploredbitmap = new SectionCount(buffer, offset + 156, 4, ARE_SIZE_EXPLORED_BITMAP,
                                                        Unknown.class);
    addField(size_exploredbitmap);
    SectionOffset offset_exploredbitmap = new SectionOffset(buffer, offset + 160, ARE_OFFSET_EXPLORED_BITMAP,
                                                            Unknown.class);
    addField(offset_exploredbitmap);
    SectionCount count_doors = new SectionCount(buffer, offset + 164, 4, ARE_NUM_DOORS,
                                                Door.class);
    addField(count_doors);
    SectionOffset offset_doors = new SectionOffset(buffer, offset + 168, ARE_OFFSET_DOORS,
                                                   Door.class);
    addField(offset_doors);
    SectionCount count_animations = new SectionCount(buffer, offset + 172, 4, ARE_NUM_ANIMATIONS,
                                                     Animation.class);
    addField(count_animations);
    SectionOffset offset_animations = new SectionOffset(buffer, offset + 176, ARE_OFFSET_ANIMATIONS,
                                                        Animation.class);
    addField(offset_animations);
    SectionCount count_tiledobjects = new SectionCount(buffer, offset + 180, 4, ARE_NUM_TILED_OBJECTS,
                                                       TiledObject.class);
    addField(count_tiledobjects);
    SectionOffset offset_tiledobjects = new SectionOffset(buffer, offset + 184, ARE_OFFSET_TILED_OBJECTS,
                                                          TiledObject.class);
    addField(offset_tiledobjects);
    SectionOffset offset_songs = new SectionOffset(buffer, offset + 188, ARE_OFFSET_SONGS,
                                                   Song.class);
    addField(offset_songs);
    SectionOffset offset_rest = new SectionOffset(buffer, offset + 192, ARE_OFFSET_REST_ENCOUNTERS,
                                                  RestSpawn.class);
    addField(offset_rest);

    SectionOffset offset_automapnote = null, offset_protrap = null;
    SectionCount count_automapnote = null, count_protrap = null;
    if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Unknown(buffer, offset + 196, 4));
      offset_automapnote = new SectionOffset(buffer, offset + 200, ARE_OFFSET_AUTOMAP_NOTES,
                                             AutomapNotePST.class);
      addField(offset_automapnote);
      count_automapnote = new SectionCount(buffer, offset + 204, 4, ARE_NUM_AUTOMAP_NOTES,
                                           AutomapNotePST.class);
      addField(count_automapnote);
      addField(new Unknown(buffer, offset + 208, 76));
    }
    else if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      offset_automapnote = new SectionOffset(buffer, offset + 196, ARE_OFFSET_AUTOMAP_NOTES,
                                             AutomapNote.class);
      addField(offset_automapnote);
      count_automapnote = new SectionCount(buffer, offset + 200, 4, ARE_NUM_AUTOMAP_NOTES,
                                           AutomapNote.class);
      addField(count_automapnote);
      offset_protrap = new SectionOffset(buffer, offset + 204, ARE_OFFSET_PROJECTILE_TRAPS,
                                         ProTrap.class);
      addField(offset_protrap);
      count_protrap = new SectionCount(buffer, offset + 208, 4, ARE_NUM_PROJECTILE_TRAPS,
                                       ProTrap.class);
      addField(count_protrap);
      final String[] movieExt = (Profile.isEnhancedEdition()) ? new String[]{"MVE", "WBM"} : new String[]{"MVE"};
      addField(new ResourceRef(buffer, offset + 212, ARE_REST_MOVIE_DAY, movieExt));
      addField(new ResourceRef(buffer, offset + 220, ARE_REST_MOVIE_NIGHT, movieExt));
      addField(new Unknown(buffer, offset + 228, 56));
    }
    else if (Profile.getEngine() == Profile.Engine.IWD2) {
      offset_automapnote = new SectionOffset(buffer, offset + 196, ARE_OFFSET_AUTOMAP_NOTES,
                                             AutomapNote.class);
      addField(offset_automapnote);
      count_automapnote = new SectionCount(buffer, offset + 200, 4, ARE_NUM_AUTOMAP_NOTES,
                                           AutomapNote.class);
      addField(count_automapnote);
      addField(new Unknown(buffer, offset + 204, 80));
    }
    else {
      addField(new Unknown(buffer, offset + 196, 88));
    }

    offset = offset_actors.getValue();
    for (int i = 0; i < count_actors.getValue(); i++) {
      Actor actor = new Actor(this, buffer, offset, i);
      offset = actor.getEndOffset();
      addField(actor);
    }

    offset = offset_itepoints.getValue();
    for (int i = 0; i < count_itepoints.getValue(); i++) {
      ITEPoint ite = new ITEPoint(this, buffer, offset, i);
      offset = ite.getEndOffset();
      addField(ite);
    }

    offset = offset_spoints.getValue();
    for (int i = 0; i < count_spoints.getValue(); i++) {
      SpawnPoint sp = new SpawnPoint(this, buffer, offset, i);
      offset = sp.getEndOffset();
      addField(sp);
    }

    offset = offset_entrances.getValue();
    for (int i = 0; i < count_entrances.getValue(); i++) {
      Entrance ent = new Entrance(this, buffer, offset, i);
      offset = ent.getEndOffset();
      addField(ent);
    }

    offset = offset_containers.getValue();
    for (int i = 0; i < count_containers.getValue(); i++) {
      Container con = new Container(this, buffer, offset, i);
      offset = con.getEndOffset();
      addField(con);
    }

    offset = offset_ambients.getValue();
    for (int i = 0; i < count_ambients.getValue(); i++) {
      Ambient ambi = new Ambient(this, buffer, offset, i);
      offset = ambi.getEndOffset();
      addField(ambi);
    }

    offset = offset_variables.getValue();
    for (int i = 0; i < count_variables.getValue(); i++) {
      Variable var = new Variable(this, buffer, offset, i);
      offset = var.getEndOffset();
      addField(var);
    }

    offset = offset_exploredbitmap.getValue();
    if (size_exploredbitmap.getValue() > 0) {
      addField(new Unknown(buffer, offset, size_exploredbitmap.getValue(), ARE_EXPLORED_BITMAP));
    }

    offset = offset_doors.getValue();
    for (int i = 0; i < count_doors.getValue(); i++) {
      Door door = new Door(this, buffer, offset, i);
      offset = door.getEndOffset();
      addField(door);
    }

    offset = offset_animations.getValue();
    for (int i = 0; i < count_animations.getValue(); i++) {
      Animation anim = new Animation(this, buffer, offset, i);
      offset = anim.getEndOffset();
      addField(anim);
    }

    offset = offset_tiledobjects.getValue();
    for (int i = 0; i < count_tiledobjects.getValue(); i++) {
      TiledObject tile = new TiledObject(this, buffer, offset, i);
      offset = tile.getEndOffset();
      addField(tile);
    }

    if (offset_automapnote != null) { // Torment, BG2
      offset = offset_automapnote.getValue();
      if (Profile.getEngine() == Profile.Engine.PST) {
        for (int i = 0; i < count_automapnote.getValue(); i++) {
          AutomapNotePST note = new AutomapNotePST(this, buffer, offset, i);
          offset = note.getEndOffset();
          addField(note);
        }
      }
      else {
        for (int i = 0; i < count_automapnote.getValue(); i++) {
          AutomapNote note = new AutomapNote(this, buffer, offset, i);
          offset = note.getEndOffset();
          addField(note);
        }
      }
    }

    if (offset_protrap != null) { // BG2
      offset = offset_protrap.getValue();
      for (int i = 0; i < count_protrap.getValue(); i++) {
        ProTrap trap = new ProTrap(this, buffer, offset, i);
        offset = trap.getEndOffset();
        addField(trap);
      }
    }

    offset = offset_items.getValue();
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Container)
        ((Container)o).readItems(buffer, offset);
    }

    offset = offset_vertices.getValue();
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof HasVertices)
        ((HasVertices)o).readVertices(buffer, offset);
    }

    if (offset_songs.getValue() > 0) {
      addField(new Song(this, buffer, offset_songs.getValue()));
    }
    if (offset_rest.getValue() > 0) {
      addField(new RestSpawn(this, buffer, offset_rest.getValue()));
    }

    int endoffset = offset;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry instanceof HasVertices) {
        // may contain additional elements
        for (int j = 0, count = ((AbstractStruct)entry).getFieldCount(); j < count; j++) {
          StructEntry subEntry = ((AbstractStruct)entry).getField(j);
          endoffset = Math.max(endoffset, subEntry.getOffset() + subEntry.getSize());
        }
      } else {
        if (entry.getOffset() + entry.getSize() > endoffset) {
          endoffset = Math.max(endoffset, entry.getOffset() + entry.getSize());
        }
      }
    }
    return endoffset;
  }

  private void updateActorCREOffsets()
  {
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Actor) {
        ((Actor)o).updateCREOffset();
      }
    }
  }

  private void updateItems()
  {
    // Assumes items offset is correct
    int offset = ((HexNumber)getAttribute(ARE_OFFSET_ITEMS)).getValue();
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Container) {
        Container container = (Container)o;
        int itemNum = container.updateItems(offset, count);
        offset += 20 * itemNum;
        count += itemNum;
      }
    }
    ((DecNumber)getAttribute(ARE_NUM_ITEMS)).setValue(count);
  }

  private void updateVertices()
  {
    // Assumes vertices offset is correct
    int offset = ((HexNumber)getAttribute(ARE_OFFSET_VERTICES)).getValue();
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof HasVertices) {
        HasVertices vert = (HasVertices)o;
        int vertNum = vert.updateVertices(offset, count);
        offset += 4 * vertNum;
        count += vertNum;
      }
    }
    ((DecNumber)getAttribute(ARE_NUM_VERTICES)).setValue(count);
  }

  /**
   * Returns a String array for Flag datatypes, that has been updated or overwritten with entries
   * from the specified IDS resource.
   * @param flags A static String array used as basis for Flag labels.
   * @param idsFile IDS resource to take entries from.
   * @param size Size of flags field in bytes. (Range: 1..4)
   * @param overwrite If {@code true}, then static flag label will be overwritten with entries
   *                  from the IDS resource.
   *                  If {@code false}, then entries from IDS resource will be used only for
   *                  missing or empty entries in the {@code flags} array.
   * @return
   */
  public static String[] getUpdatedIdsFlags(String[] flags, String idsFile, int size, boolean overwrite)
  {
    ArrayList<String> list = new ArrayList<String>(32);
    size = Math.max(1, Math.min(4, size));

    // adding static labels
    if (flags != null && flags.length > 1) {
      Collections.addAll(list, flags);
    } else {
      list.add(null); // empty flags label
    }

    // updating list with labels from IDS entries
    if (ResourceFactory.resourceExists(idsFile)) {
      IdsMap map = IdsMapCache.get(idsFile);
      if (map != null) {
        int numBits = size * 8;
        for (int i = 0; i < numBits; i++) {
          IdsMapEntry entry = map.get((long)(1 << i));
          String s = (entry != null) ? entry.getSymbol() : null;
          if (i < list.size() - 1) {
            if (overwrite || list.get(i+1) == null) {
              list.set(i+1, s);
            }
          } else {
            list.add(s);
          }
        }
      }
    }

    // cleaning up trailing null labels
    while (list.size() > 1 && list.get(list.size() - 1) == null) {
      list.remove(list.size() - 1);
    }

    // converting list into array
    String[] retVal = new String[list.size()];
    for (int i = 0; i < retVal.length; i++) {
      retVal[i] = list.get(i);
    }

    return retVal;
  }

  /** Displays the area viewer for this ARE resource. */
  AreaViewer showAreaViewer(Component parent)
  {
    if (areaViewer == null) {
      areaViewer = new AreaViewer(parent, this);
    } else if (!areaViewer.isVisible()) {
      areaViewer.setVisible(true);
      areaViewer.toFront();
    } else {
      areaViewer.toFront();
    }
    return areaViewer;
  }

  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        AreResource are = new AreResource(entry);
        Actor[] actors;
        Animation[] animations;
        Item[][] items;
        boolean retVal = true;
        String key;
        Object o;

        // preparing substructures
        DecNumber ofs = (DecNumber)are.getAttribute(ARE_OFFSET_ACTORS, false);
        DecNumber cnt = (DecNumber)are.getAttribute(ARE_NUM_ACTORS, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          actors = new Actor[cnt.getValue()];
          for (int idx = 0; idx < actors.length; idx++) {
            actors[idx] = (Actor)are.getAttribute(String.format(SearchOptions.getResourceName(SearchOptions.ARE_Actor), idx), false);
          }
        } else {
          actors = new Actor[0];
        }

        ofs = (DecNumber)are.getAttribute(ARE_OFFSET_ANIMATIONS, false);
        cnt = (DecNumber)are.getAttribute(ARE_NUM_ANIMATIONS, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          animations = new Animation[cnt.getValue()];
          for (int idx = 0; idx < animations.length; idx++) {
            animations[idx] = (Animation)are.getAttribute(String.format(SearchOptions.getResourceName(SearchOptions.ARE_Animation), idx), false);
          }
        } else {
          animations = new Animation[0];
        }

        ofs = (DecNumber)are.getAttribute(ARE_OFFSET_CONTAINERS, false);
        cnt = (DecNumber)are.getAttribute(ARE_NUM_CONTAINERS, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          items = new Item[cnt.getValue()][];
          for (int i = 0; i < cnt.getValue(); i++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.ARE_Container), i);
            Container container = (Container)are.getAttribute(label, false);
            if (container != null) {
              DecNumber cnt2 = (DecNumber)container.getAttribute(ARE_NUM_ITEMS, false);
              if (cnt2 != null && cnt2.getValue() > 0) {
                items[i] = new Item[cnt2.getValue()];
                for (int j = 0; j < cnt2.getValue(); j++) {
                  label = String.format(SearchOptions.getResourceName(SearchOptions.ARE_Container_Item), j);
                  items[i][j] = (Item)container.getAttribute(label, false);
                }
              } else {
                items[i] = new Item[0];
              }
            } else {
              items[i] = new Item[0];
            }
          }
        } else {
          items = new Item[0][];
        }


        // checking options
        String[] keyList = new String[]{SearchOptions.ARE_AreaType,
                                        SearchOptions.ARE_Location};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = are.getAttribute(SearchOptions.getResourceName(key), false);
            retVal &= SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }

        if (retVal) {
          key = SearchOptions.ARE_AreaScript;
          o = searchOptions.getOption(key);
          StructEntry struct = are.getAttribute(SearchOptions.getResourceName(key), false);
          retVal &= SearchOptions.Utils.matchResourceRef(struct, o, false);
        }

        if (retVal) {
          key = SearchOptions.ARE_Actor_Character;
          o = searchOptions.getOption(key);
          boolean found = false;
          for (int idx = 0; idx < actors.length; idx++) {
            if (actors[idx] != null) {
              StructEntry struct = actors[idx].getAttribute(SearchOptions.getResourceName(key), false);
              found |= SearchOptions.Utils.matchResourceRef(struct, o, false);
            }
          }
          retVal &= found || (o == null);
        }

        if (retVal) {
          key = SearchOptions.ARE_Animation_Animation;
          o = searchOptions.getOption(key);
          boolean found = false;
          for (int idx = 0; idx < animations.length; idx++) {
            if (animations[idx] != null) {
              StructEntry struct = animations[idx].getAttribute(SearchOptions.getResourceName(key), false);
              found |= SearchOptions.Utils.matchResourceRef(struct, o, false);
            }
          }
          retVal &= found || (o == null);
        }

        if (retVal) {
          key = SearchOptions.ARE_Container_Item_Item;
          o = searchOptions.getOption(key);
          boolean found = false;
          for (int idx = 0; idx < items.length; idx++) {
            for (int idx2 = 0; idx2 < items[idx].length; idx2++) {
              if (items[idx][idx2] != null) {
                StructEntry struct = items[idx][idx2].getAttribute(SearchOptions.getResourceName(key), false);
                found |= SearchOptions.Utils.matchResourceRef(struct, o, false);
              }
              if (found) {
                break;
              }
            }
            if (found) {
              break;
            }
          }
          retVal &= found || (o == null);
        }

        keyList = new String[]{SearchOptions.ARE_Custom1, SearchOptions.ARE_Custom2,
                               SearchOptions.ARE_Custom3, SearchOptions.ARE_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(are, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}

