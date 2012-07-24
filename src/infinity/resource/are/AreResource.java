// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.vertex.Vertex;
import infinity.util.Byteconvert;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public final class AreResource extends AbstractStruct implements Resource, HasAddRemovable, HasDetailViewer
{
  private static final String s_flag[] = {"No flags set", "Outdoor", "Day/Night",
                                          "Weather", "City", "Forest", "Dungeon",
                                          "Extended night", "Can rest"};
  private static final String s_flag_torment[] = {"Indoors", "Hive", "", "Clerk's ward", "Lower ward",
                                                  "Ravel's maze", "Baator", "Rubikon",
                                                  "Negative material plane", "Curst", "Carceri",
                                                  "Allow day/night"};
  private static final String s_atype[] = {"Normal", "Can't save game", "Tutorial area", "Dead magic zone",
                                           "Dream area"};
  private static final String s_atype_torment[] = {"Can rest", "Cannot save",
                                                   "Cannot rest", "Cannot save", "Too dangerous to rest",
                                                   "Cannot save", "Can rest with permission"};
  private static final String s_atype_iwd2[] = {"Normal", "Can't save game", "Cannot rest", "Lock battle music"};
  private static final String s_edge[] = {"No flags set", "Party required", "Party enabled"};

  public static void addScriptNames(Set<String> scriptNames, byte buffer[])
  {
    int offset = 0;
    if (new String(buffer, 4, 4).equalsIgnoreCase("V9.1"))
      offset = 16;

    // Actors
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
      addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 84),
                     (int)Byteconvert.convertShort(buffer, offset + 88), 272);

    // ITEPoints
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 92),
                   (int)Byteconvert.convertShort(buffer, offset + 90), 196);

    // Spawnpoints
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 96),
                   Byteconvert.convertInt(buffer, offset + 100), 200);

    // Entrances
//    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 104),
//                   Byteconvert.convertInt(buffer, offset + 108), 104);

    // Containers
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 112),
                   (int)Byteconvert.convertShort(buffer, offset + 116), 192);

    // Ambients
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 132),
                   (int)Byteconvert.convertShort(buffer, offset + 130), 212);

    // Variables
//    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 136),
//                   Byteconvert.convertInt(buffer, offset + 140), 84);

    // Doors
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 168),
                   Byteconvert.convertInt(buffer, offset + 164), 200);

    // Animations
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 176),
                   Byteconvert.convertInt(buffer, offset + 172), 76);

    // Tiled objects
    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 184),
                   Byteconvert.convertInt(buffer, offset + 180), 108);

    // Rest spawn
//    addScriptNames(scriptNames, buffer, Byteconvert.convertInt(buffer, offset + 192), 1, 228);
  }

  private static void addScriptNames(Set<String> scriptNames, byte buffer[], int offset, int count, int size)
  {
    for (int i = 0; i < count; i++) {
      StringBuilder sb = new StringBuilder(32);
      for (int j = 0; j < 32; j++) {
        byte b = buffer[offset + i * size + j];
        if (b == 0x00)
          break;
        else if (b != 0x20) // Space
          sb.append(Character.toLowerCase((char)b));
      }
      scriptNames.add(sb.toString());
    }
  }

  public AreResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      return new AddRemovable[]{new Actor(), new ITEPoint(), new SpawnPoint(),
                                new Entrance(), new Container(), new Ambient(),
                                new Variable(), new Door(), new Animation(),
                                new TiledObject(), new AutomapNotePST()};
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
             ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
             ResourceFactory.getGameID() == ResourceFactory.ID_TUTU)
      return new AddRemovable[]{new Actor(), new ITEPoint(), new SpawnPoint(),
                                new Entrance(), new Container(), new Ambient(),
                                new Variable(), new Door(), new Animation(),
                                new TiledObject(), new AutomapNote(),
                                new ProTrap()};
    else
      return new AddRemovable[]{new Actor(), new ITEPoint(), new SpawnPoint(),
                                new Entrance(), new Container(), new Ambient(),
                                new Variable(), new Door(), new Animation(),
                                new TiledObject()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    JScrollPane scroll = new JScrollPane(new Viewer(this));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

// --------------------- End Interface HasDetailViewer ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  protected void datatypeAdded(AddRemovable datatype)
  {
    HexNumber offset_vertices = (HexNumber)getAttribute("Vertices offset");
    if (datatype.getOffset() <= offset_vertices.getValue())
      offset_vertices.incValue(datatype.getSize());
    HexNumber offset_items = (HexNumber)getAttribute("Items offset");
    if (datatype.getOffset() <= offset_items.getValue())
      offset_items.incValue(datatype.getSize());

    if (datatype instanceof HasVertices)
      updateVertices();
    if (datatype instanceof Container)
      updateItems();
    updateActorCREOffsets();
  }

  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    if (datatype instanceof Vertex)
      updateVertices();
    else {
      HexNumber offset_vertices = (HexNumber)getAttribute("Vertices offset");
      if (datatype.getOffset() <= offset_vertices.getValue()) {
        offset_vertices.incValue(datatype.getSize());
        updateVertices();
      }
    }
    if (datatype instanceof Item)
      updateItems();
    else {
      HexNumber offset_items = (HexNumber)getAttribute("Items offset");
      if (datatype.getOffset() <= offset_items.getValue()) {
        offset_items.incValue(datatype.getSize());
        updateItems();
      }
    }
    updateActorCREOffsets();
  }

  protected void datatypeRemoved(AddRemovable datatype)
  {
    HexNumber offset_vertices = (HexNumber)getAttribute("Vertices offset");
    if (datatype.getOffset() < offset_vertices.getValue())
      offset_vertices.incValue(-datatype.getSize());
    HexNumber offset_items = (HexNumber)getAttribute("Items offset");
    if (datatype.getOffset() < offset_items.getValue())
      offset_items.incValue(-datatype.getSize());

    if (datatype instanceof HasVertices)
      updateVertices();
    if (datatype instanceof Container)
      updateItems();
    updateActorCREOffsets();
  }

  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    if (datatype instanceof Vertex)
      updateVertices();
    else {
      HexNumber offset_vertices = (HexNumber)getAttribute("Vertices offset");
      if (datatype.getOffset() < offset_vertices.getValue()) {
        offset_vertices.incValue(-datatype.getSize());
        updateVertices();
      }
    }
    if (datatype instanceof Item)
      updateItems();
    else {
      HexNumber offset_items = (HexNumber)getAttribute("Items offset");
      if (datatype.getOffset() < offset_items.getValue()) {
        offset_items.incValue(-datatype.getSize());
        updateItems();
      }
    }
    updateActorCREOffsets();
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    list.add(version);
    list.add(new ResourceRef(buffer, offset + 8, "WED resource", "WED"));
    list.add(new DecNumber(buffer, offset + 16, 4, "Last saved"));
    if (version.toString().equalsIgnoreCase("V9.1"))
      list.add(new Flag(buffer, offset + 20, 4, "Area type", s_atype_iwd2));
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      list.add(new Bitmap(buffer, offset + 20, 4, "Area type", s_atype_torment));
    else
      list.add(new Flag(buffer, offset + 20, 4, "Area type", s_atype));
    list.add(new ResourceRef(buffer, offset + 24, "Area north", "ARE"));
    list.add(new Flag(buffer, offset + 32, 4, "Edge flags north", s_edge));
    list.add(new ResourceRef(buffer, offset + 36, "Area east", "ARE"));
    list.add(new Flag(buffer, offset + 44, 4, "Edge flags east", s_edge));
    list.add(new ResourceRef(buffer, offset + 48, "Area south", "ARE"));
    list.add(new Flag(buffer, offset + 56, 4, "Edge flags south", s_edge));
    list.add(new ResourceRef(buffer, offset + 60, "Area west", "ARE"));
    list.add(new Flag(buffer, offset + 68, 4, "Edge flags west", s_edge));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      list.add(new Flag(buffer, offset + 72, 2, "Location", s_flag_torment));
    else
      list.add(new Flag(buffer, offset + 72, 2, "Location", s_flag));
    list.add(new DecNumber(buffer, offset + 74, 2, "Rain probability"));
    list.add(new DecNumber(buffer, offset + 76, 2, "Snow probability"));
    list.add(new DecNumber(buffer, offset + 78, 2, "Fog probability"));
    list.add(new DecNumber(buffer, offset + 80, 2, "Lightning probability"));
    list.add(new DecNumber(buffer, offset + 82, 2, "Wind speed"));
    if (version.toString().equalsIgnoreCase("V9.1")) {
      list.add(new DecNumber(buffer, offset + 84, 1, "Area difficulty 2"));
      list.add(new DecNumber(buffer, offset + 85, 1, "Area difficulty 3"));
      list.add(new Unknown(buffer, offset + 86, 14));
      offset += 16;
    }
    SectionOffset offset_actors = new SectionOffset(buffer, offset + 84, "Actors offset",
                                                    Actor.class);
    list.add(offset_actors);
    SectionCount count_actors = new SectionCount(buffer, offset + 88, 2, "# actors",
                                                 Actor.class);
    list.add(count_actors);
    SectionCount count_itepoints = new SectionCount(buffer, offset + 90, 2, "# triggers",
                                                    ITEPoint.class);
    list.add(count_itepoints);
    SectionOffset offset_itepoints = new SectionOffset(buffer, offset + 92,
                                                       "Triggers offset",
                                                       ITEPoint.class);
    list.add(offset_itepoints);
    SectionOffset offset_spoints = new SectionOffset(buffer, offset + 96, "Spawn points offset",
                                                     SpawnPoint.class);
    list.add(offset_spoints);
    SectionCount count_spoints = new SectionCount(buffer, offset + 100, 4, "# spawn points",
                                                  SpawnPoint.class);
    list.add(count_spoints);
    SectionOffset offset_entrances = new SectionOffset(buffer, offset + 104, "Entrances offset",
                                                       Entrance.class);
    list.add(offset_entrances);
    SectionCount count_entrances = new SectionCount(buffer, offset + 108, 4, "# entrances",
                                                    Entrance.class);
    list.add(count_entrances);
    SectionOffset offset_containers = new SectionOffset(buffer, offset + 112, "Containers offset",
                                                        Container.class);
    list.add(offset_containers);
    SectionCount count_containers = new SectionCount(buffer, offset + 116, 2, "# containers",
                                                     Container.class);
    list.add(count_containers);
    DecNumber count_items = new DecNumber(buffer, offset + 118, 2, "# items");
    list.add(count_items);
    HexNumber offset_items = new HexNumber(buffer, offset + 120, 4, "Items offset");
    list.add(offset_items);
    HexNumber offset_vertices = new HexNumber(buffer, offset + 124, 4, "Vertices offset");
    list.add(offset_vertices);
    DecNumber count_vertices = new DecNumber(buffer, offset + 128, 2, "# vertices");
    list.add(count_vertices);
    SectionCount count_ambients = new SectionCount(buffer, offset + 130, 2, "# ambients",
                                                   Ambient.class);
    list.add(count_ambients);
    SectionOffset offset_ambients = new SectionOffset(buffer, offset + 132, "Ambients offset",
                                                      Ambient.class);
    list.add(offset_ambients);
    SectionOffset offset_variables = new SectionOffset(buffer, offset + 136, "Variables offset",
                                                       Variable.class);
    list.add(offset_variables);
    SectionCount count_variables = new SectionCount(buffer, offset + 140, 2, "# variables",
                                                    Variable.class);
    list.add(count_variables);
    list.add(new HexNumber(buffer, offset + 142, 2, "# object flags"));
    list.add(new HexNumber(buffer, offset + 144, 4, "Object flags offset"));
    list.add(new ResourceRef(buffer, offset + 148, "Area script", "BCS"));
    SectionCount size_exploredbitmap = new SectionCount(buffer, offset + 156, 4, "Explored bitmap size",
                                                        Unknown.class);
    list.add(size_exploredbitmap);
    SectionOffset offset_exploredbitmap = new SectionOffset(buffer, offset + 160, "Explored bitmap offset",
                                                            Unknown.class);
    list.add(offset_exploredbitmap);
    SectionCount count_doors = new SectionCount(buffer, offset + 164, 4, "# doors",
                                                Door.class);
    list.add(count_doors);
    SectionOffset offset_doors = new SectionOffset(buffer, offset + 168, "Doors offset",
                                                   Door.class);
    list.add(offset_doors);
    SectionCount count_animations = new SectionCount(buffer, offset + 172, 4, "# animations",
                                                     Animation.class);
    list.add(count_animations);
    SectionOffset offset_animations = new SectionOffset(buffer, offset + 176, "Animations offset",
                                                        Animation.class);
    list.add(offset_animations);
    SectionCount count_tiledobjects = new SectionCount(buffer, offset + 180, 4, "# tiled objects",
                                                       TiledObject.class);
    list.add(count_tiledobjects);
    SectionOffset offset_tiledobjects = new SectionOffset(buffer, offset + 184, "Tiled objects offset",
                                                          TiledObject.class);
    list.add(offset_tiledobjects);
    SectionOffset offset_songs = new SectionOffset(buffer, offset + 188, "Songs offset",
                                                   Song.class);
    list.add(offset_songs);
    SectionOffset offset_rest = new SectionOffset(buffer, offset + 192, "Rest encounters offset",
                                                  RestSpawn.class);
    list.add(offset_rest);

    SectionOffset offset_automapnote = null, offset_protrap = null;
    SectionCount count_automapnote = null, count_protrap = null;
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
      list.add(new Unknown(buffer, offset + 196, 4));
      offset_automapnote = new SectionOffset(buffer, offset + 200, "Automap notes offset",
                                             AutomapNotePST.class);
      list.add(offset_automapnote);
      count_automapnote = new SectionCount(buffer, offset + 204, 4, "# automap notes",
                                           AutomapNotePST.class);
      list.add(count_automapnote);
      list.add(new Unknown(buffer, offset + 208, 76));
    }
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
             ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
             ResourceFactory.getGameID() == ResourceFactory.ID_TUTU) {
      offset_automapnote = new SectionOffset(buffer, offset + 196, "Automap notes offset",
                                             AutomapNote.class);
      list.add(offset_automapnote);
      count_automapnote = new SectionCount(buffer, offset + 200, 4, "# automap notes",
                                           AutomapNote.class);
      list.add(count_automapnote);
      offset_protrap = new SectionOffset(buffer, offset + 204, "Projectile traps offset",
                                         ProTrap.class);
      list.add(offset_protrap);
      count_protrap = new SectionCount(buffer, offset + 208, 4, "# projectile traps",
                                       ProTrap.class);
      list.add(count_protrap);
      list.add(new ResourceRef(buffer, offset + 212, "Rest movie (day)", "MVE"));
      list.add(new ResourceRef(buffer, offset + 220, "Rest movie (night)", "MVE"));
      list.add(new Unknown(buffer, offset + 228, 56));
    }
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      offset_automapnote = new SectionOffset(buffer, offset + 196, "Automap notes offset",
                                             AutomapNote.class);
      list.add(offset_automapnote);
      count_automapnote = new SectionCount(buffer, offset + 200, 4, "# automap notes",
                                           AutomapNote.class);
      list.add(count_automapnote);
      list.add(new Unknown(buffer, offset + 204, 80));
    }
    else
      list.add(new Unknown(buffer, offset + 196, 88));

    offset = offset_actors.getValue();
    for (int i = 0; i < count_actors.getValue(); i++) {
      Actor actor = new Actor(this, buffer, offset, i + 1);
      offset = actor.getEndOffset();
      list.add(actor);
    }

    offset = offset_itepoints.getValue();
    for (int i = 0; i < count_itepoints.getValue(); i++) {
      ITEPoint ite = new ITEPoint(this, buffer, offset);
      offset = ite.getEndOffset();
      list.add(ite);
    }

    offset = offset_spoints.getValue();
    for (int i = 0; i < count_spoints.getValue(); i++) {
      SpawnPoint sp = new SpawnPoint(this, buffer, offset);
      offset = sp.getEndOffset();
      list.add(sp);
    }

    offset = offset_entrances.getValue();
    for (int i = 0; i < count_entrances.getValue(); i++) {
      Entrance ent = new Entrance(this, buffer, offset);
      offset = ent.getEndOffset();
      list.add(ent);
    }

    offset = offset_containers.getValue();
    for (int i = 0; i < count_containers.getValue(); i++) {
      Container con = new Container(this, buffer, offset, i + 1);
      offset = con.getEndOffset();
      list.add(con);
    }

    offset = offset_ambients.getValue();
    for (int i = 0; i < count_ambients.getValue(); i++) {
      Ambient ambi = new Ambient(this, buffer, offset, i + 1);
      offset = ambi.getEndOffset();
      list.add(ambi);
    }

    offset = offset_variables.getValue();
    for (int i = 0; i < count_variables.getValue(); i++) {
      Variable var = new Variable(this, buffer, offset);
      offset = var.getEndOffset();
      list.add(var);
    }

    offset = offset_exploredbitmap.getValue();
    if (size_exploredbitmap.getValue() > 0)
      list.add(new Unknown(buffer, offset, size_exploredbitmap.getValue(), "Explored bitmap"));

    offset = offset_doors.getValue();
    for (int i = 0; i < count_doors.getValue(); i++) {
      Door door = new Door(this, buffer, offset, i + 1);
      offset = door.getEndOffset();
      list.add(door);
    }

    offset = offset_animations.getValue();
    for (int i = 0; i < count_animations.getValue(); i++) {
      Animation anim = new Animation(this, buffer, offset);
      offset = anim.getEndOffset();
      list.add(anim);
    }

    offset = offset_tiledobjects.getValue();
    for (int i = 0; i < count_tiledobjects.getValue(); i++) {
      TiledObject tile = new TiledObject(this, buffer, offset);
      offset = tile.getEndOffset();
      list.add(tile);
    }

    if (offset_automapnote != null) { // Torment, BG2
      offset = offset_automapnote.getValue();
      if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
        for (int i = 0; i < count_automapnote.getValue(); i++) {
          AutomapNotePST note = new AutomapNotePST(this, buffer, offset);
          offset = note.getEndOffset();
          list.add(note);
        }
      }
      else {
        for (int i = 0; i < count_automapnote.getValue(); i++) {
          AutomapNote note = new AutomapNote(this, buffer, offset);
          offset = note.getEndOffset();
          list.add(note);
        }
      }
    }

    if (offset_protrap != null) { // BG2
      offset = offset_protrap.getValue();
      for (int i = 0; i < count_protrap.getValue(); i++) {
        ProTrap trap = new ProTrap(this, buffer, offset);
        offset = trap.getEndOffset();
        list.add(trap);
      }
    }

    offset = offset_items.getValue();
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Container)
        ((Container)o).readItems(buffer, offset);
    }

    offset = offset_vertices.getValue();
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof HasVertices)
        ((HasVertices)o).readVertices(buffer, offset);
    }

    if (offset_songs.getValue() > 0)
      list.add(new Song(this, buffer, offset_songs.getValue()));
    if (offset_rest.getValue() > 0)
      list.add(new RestSpawn(this, buffer, offset_rest.getValue()));

    int endoffset = offset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  private void updateActorCREOffsets()
  {
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Actor)
        ((Actor)o).updateCREOffset();
    }
  }

  private void updateItems()
  {
    // Assumes items offset is correct
    int offset = ((HexNumber)getAttribute("Items offset")).getValue();
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Container) {
        Container container = (Container)o;
        int itemNum = container.updateItems(offset, count);
        offset += 20 * itemNum;
        count += itemNum;
      }
    }
    ((DecNumber)getAttribute("# items")).setValue(count);
  }

  private void updateVertices()
  {
    // Assumes vertices offset is correct
    int offset = ((HexNumber)getAttribute("Vertices offset")).getValue();
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof HasVertices) {
        HasVertices vert = (HasVertices)o;
        int vertNum = vert.updateVertices(offset, count);
        offset += 4 * vertNum;
        count += vertNum;
      }
    }
    ((DecNumber)getAttribute("# vertices")).setValue(count);
  }
}

