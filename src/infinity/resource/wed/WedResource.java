// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.vertex.Vertex;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public final class WedResource extends AbstractStruct implements Resource, HasAddRemovable
{
  private static HexNumber findNextOffset(HexNumber offsets[], HexNumber offset)
  {
    for (int i = 0; i < offsets.length; i++)
      if (offsets[i] == offset && i < offsets.length - 1)
        return offsets[i + 1];
    return null;
  }

  public WedResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Door(), new WallPolygon(), new Wallgroup()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  protected void datatypeAdded(AddRemovable datatype)
  {
    updateSectionOffsets(datatype, datatype.getSize());
  }

  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateSectionOffsets(datatype, datatype.getSize());
    if (datatype instanceof Vertex)
      updateVertices();
    else if (datatype instanceof RemovableDecNumber && child instanceof Door) {
      Door childDoor = (Door)child;
      int childIndex = childDoor.getTilemapIndex().getValue();
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Door && o != childDoor) {
          DecNumber tilemapIndex = ((Door)o).getTilemapIndex();
          if (tilemapIndex.getValue() >= childIndex)
            tilemapIndex.incValue(1);
        }
      }
    }
  }

  protected void datatypeRemoved(AddRemovable datatype)
  {
    updateSectionOffsets(datatype, -datatype.getSize());
  }

  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateSectionOffsets(datatype, -datatype.getSize());
    if (datatype instanceof Vertex)
      updateVertices();
    else if (datatype instanceof RemovableDecNumber && child instanceof Door) {
      Door childDoor = (Door)child;
      int childIndex = childDoor.getTilemapIndex().getValue();
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Door && o != childDoor) {
          DecNumber tilemapIndex = ((Door)o).getTilemapIndex();
          if (tilemapIndex.getValue() > childIndex)
            tilemapIndex.incValue(-1);
        }
      }
    }
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    SectionCount countOverlays = new SectionCount(buffer, offset + 8, 4, "# overlays",
                                                  Overlay.class);
    list.add(countOverlays);
    SectionCount countDoors = new SectionCount(buffer, offset + 12, 4, "# doors",
                                               Door.class);
    list.add(countDoors);
    SectionOffset offsetOverlays = new SectionOffset(buffer, offset + 16, "Overlays offset",
                                                     Overlay.class);
    list.add(offsetOverlays);
    SectionOffset offsetHeader2 = new SectionOffset(buffer, offset + 20, "Second header offset", null);
    list.add(offsetHeader2);
    SectionOffset offsetDoors = new SectionOffset(buffer, offset + 24, "Doors offset",
                                                  Door.class);
    list.add(offsetDoors);
    HexNumber offsetDoortile = new HexNumber(buffer, offset + 28, 4, "Door tilemap lookup offset");
    list.add(offsetDoortile);

    offset = offsetOverlays.getValue();
    for (int i = 0; i < countOverlays.getValue(); i++) {
      Overlay overlay = new Overlay(this, buffer, offset);
      offset = overlay.getEndOffset();
      list.add(overlay);
    }

    offset = offsetHeader2.getValue();
    SectionCount countWallpolygons = new SectionCount(buffer, offset, 4, "# wall polygons",
                                                      WallPolygon.class);
    list.add(countWallpolygons);
    SectionOffset offsetPolygons = new SectionOffset(buffer, offset + 4, "Wall polygons offset",
                                                     WallPolygon.class);
    list.add(offsetPolygons);
    HexNumber offsetVertices = new HexNumber(buffer, offset + 8, 4, "Vertices offset");
    list.add(offsetVertices);
    SectionOffset offsetWallgroups = new SectionOffset(buffer, offset + 12, "Wall groups offset",
                                                       Wallgroup.class);
    list.add(offsetWallgroups);
    SectionOffset offsetPolytable = new SectionOffset(buffer, offset + 16, "Wall polygon lookup offset",
                                                      RemovableDecNumber.class);
    list.add(offsetPolytable);

    HexNumber offsets[] = new HexNumber[]{offsetOverlays, offsetHeader2, offsetDoors, offsetDoortile,
                                          offsetPolygons, offsetWallgroups, offsetPolytable};
    Arrays.sort(offsets, new Comparator<HexNumber>()
    {
      public int compare(HexNumber s1, HexNumber s2)
      {
        return s1.getValue() - s2.getValue();
      }
    });

    offset = offsetDoors.getValue();
    for (int i = 0; i < countDoors.getValue(); i++) {
      Door door = new Door(this, buffer, offset);
      offset = door.getEndOffset();
      door.readVertices(buffer, offsetVertices.getValue());
      list.add(door);
    }

    offset = offsetWallgroups.getValue();
    int countPolytable = 0;
    int countWallgroups = (findNextOffset(offsets, offsetWallgroups).getValue() -
                           offsetWallgroups.getValue()) /
                          4;
    for (int i = 0; i < countWallgroups; i++) {
      Wallgroup wall = new Wallgroup(this, buffer, offset, i);
      offset = wall.getEndOffset();
      countPolytable = Math.max(countPolytable, wall.getNextPolygonIndex());
      list.add(wall);
    }

    offset = offsetPolygons.getValue();
    for (int i = 0; i < countWallpolygons.getValue(); i++) {
      Polygon poly = new WallPolygon(this, buffer, offset, i);
      offset = poly.getEndOffset();
      poly.readVertices(buffer, offsetVertices.getValue());
      list.add(poly);
    }

    offset = offsetPolytable.getValue();
    for (int i = 0; i < countPolytable; i++)
      list.add(new DecNumber(buffer, offset + i * 2, 2, "Wall polygon index " + i));

    int endoffset = offset;
    List flatList = getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      StructEntry entry = (StructEntry)flatList.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  private void updateSectionOffsets(AddRemovable datatype, int size)
  {
    if (!(datatype instanceof Vertex)) {
      HexNumber offset_vertices = (HexNumber)getAttribute("Vertices offset");
      if (datatype.getOffset() <= offset_vertices.getValue())
        offset_vertices.incValue(size);
    }
    if (!(datatype instanceof RemovableDecNumber)) {
      HexNumber offset_doortilemap = (HexNumber)getAttribute("Door tilemap lookup offset");
      if (datatype.getOffset() <= offset_doortilemap.getValue())
        offset_doortilemap.incValue(size);
    }

    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Overlay)
        ((Overlay)o).updateOffsets(datatype.getOffset(), size);
    }

    // Assumes polygon offset is correct
    int offset = ((SectionOffset)getAttribute("Wall polygons offset")).getValue();
    offset += ((SectionCount)getAttribute("# wall polygons")).getValue() * 18;
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Door)
        ((Door)o).updatePolygonsOffset(offset);
    }
  }

  private void updateVertices()
  {
    // Assumes vertices offset is correct
    int offset = ((HexNumber)getAttribute("Vertices offset")).getValue();
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Polygon) {
        Polygon polygon = (Polygon)o;
        int vertNum = polygon.updateVertices(offset, count);
        offset += 4 * vertNum;
        count += vertNum;
      }
      else if (o instanceof Door) {
        Door door = (Door)o;
        for (int j = 0; j < door.getRowCount(); j++) {
          StructEntry q = door.getStructEntryAt(j);
          if (q instanceof Polygon) {
            Polygon polygon = (Polygon)q;
            int vertNum = polygon.updateVertices(offset, count);
            offset += 4 * vertNum;
            count += vertNum;
          }
        }
      }
    }
  }
}

