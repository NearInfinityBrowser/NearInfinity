// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wed;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JComponent;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.RemovableDecNumber;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.vertex.Vertex;
import org.infinity.util.ArrayUtil;
import org.infinity.util.Misc;

/**
 * This resource maps the layout of terrain to the tiles in the tileset, and adds
 * structure to an area by listing its {@link Door doors} and {@link WallPolygon walls}.
 * <p>
 * An area is a grid, with each 64*64 cell within the grid (called a tile cell)
 * being a location for a tile. Tile cells are numbered, starting at 0, and run
 * from top left to bottom right (i.e. a tile cell number can be calculated by
 * {@code y*width+x}). As well the tiles for the main area graphics, an area can
 * use {@link Overlay overlays}. Overlays are usually used for rivers and lakes.
 * Each overlay layer is placed in a separate grid, which are stacked on top of
 * the base grid. Areas also contain another grid, split into 16*16 squares, for
 * the exploration map.
 * <p>
 * The process of drawing an area is outlined below:
 * <ul>
 * <li>The cell number acts as an index into a tilemap structure</li>
 * <li>This give a "tile lookup index" which is an index into the tile indices
 *     lookup table</li>
 * <li>The tile indices lookup table gives the index into the actual tileset, at
 *     which point, the tile is drawn</li>
 * <li>The process is repeated for each required overlay (using the associated
 *     overlay tilemap / tile indices)</li>
 * </ul>
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/wed_v1.3.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/wed_v1.3.htm</a>
 */
public final class WedResource extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs
{
  // WED-specific field labels
  public static final String WED_NUM_OVERLAYS               = "# overlays";
  public static final String WED_NUM_DOORS                  = "# doors";
  public static final String WED_OFFSET_OVERLAYS            = "Overlays offset";
  public static final String WED_OFFSET_SECOND_HEADER       = "Second header offset";
  public static final String WED_OFFSET_DOORS               = "Doors offset";
  public static final String WED_OFFSET_DOOR_TILEMAP_LOOKUP = "Door tilemap lookup offset";
  public static final String WED_NUM_WALL_POLYGONS          = "# wall polygons";
  public static final String WED_OFFSET_WALL_POLYGONS       = "Wall polygons offset";
  public static final String WED_OFFSET_VERTICES            = "Vertices offset";
  public static final String WED_OFFSET_WALL_GROUPS         = "Wall groups offset";
  public static final String WED_OFFSET_WALL_POLYGON_LOOKUP = "Wall polygon lookup offset";
  public static final String WED_WALL_POLYGON_INDEX         = "Wall polygon index";

  private StructHexViewer hexViewer;

  public WedResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    return new AddRemovable[]{new Door(), new WallPolygon(), new Wallgroup()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeFlatFields(os);
  }

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    updateSectionOffsets(datatype, datatype.getSize());
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateSectionOffsets(datatype, datatype.getSize());
    if (datatype instanceof Vertex)
      updateVertices();
    else if (datatype instanceof RemovableDecNumber && child instanceof Door) {
      Door childDoor = (Door)child;
      int childIndex = childDoor.getTilemapIndex().getValue();
      for (final StructEntry o : getFields()) {
        if (o instanceof Door && o != childDoor) {
          DecNumber tilemapIndex = ((Door)o).getTilemapIndex();
          if (tilemapIndex.getValue() >= childIndex)
            tilemapIndex.incValue(1);
        }
      }
    }
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    updateSectionOffsets(datatype, -datatype.getSize());
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateSectionOffsets(datatype, -datatype.getSize());
    if (datatype instanceof Vertex)
      updateVertices();
    else if (datatype instanceof RemovableDecNumber && child instanceof Door) {
      Door childDoor = (Door)child;
      int childIndex = childDoor.getTilemapIndex().getValue();
      for (final StructEntry o : getFields()) {
        if (o instanceof Door && o != childDoor) {
          DecNumber tilemapIndex = ((Door)o).getTilemapIndex();
          if (tilemapIndex.getValue() > childIndex)
            tilemapIndex.incValue(-1);
        }
      }
    }
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    int startOffset = offset;

    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    SectionCount countOverlays = new SectionCount(buffer, offset + 8, 4, WED_NUM_OVERLAYS,
                                                  Overlay.class);
    addField(countOverlays);
    SectionCount countDoors = new SectionCount(buffer, offset + 12, 4, WED_NUM_DOORS,
                                               Door.class);
    addField(countDoors);
    SectionOffset offsetOverlays = new SectionOffset(buffer, offset + 16, WED_OFFSET_OVERLAYS,
                                                     Overlay.class);
    addField(offsetOverlays);
    SectionOffset offsetHeader2 = new SectionOffset(buffer, offset + 20, WED_OFFSET_SECOND_HEADER, HexNumber.class);
    addField(offsetHeader2);
    SectionOffset offsetDoors = new SectionOffset(buffer, offset + 24, WED_OFFSET_DOORS,
                                                  Door.class);
    addField(offsetDoors);
    HexNumber offsetDoortile = new HexNumber(buffer, offset + 28, 4, WED_OFFSET_DOOR_TILEMAP_LOOKUP);
    addField(offsetDoortile);

    offset = offsetOverlays.getValue();
    for (int i = 0; i < countOverlays.getValue(); i++) {
      Overlay overlay = new Overlay(this, buffer, offset, i);
      offset = overlay.getEndOffset();
      addField(overlay);
    }

    offset = offsetHeader2.getValue();
    SectionCount countWallpolygons = new SectionCount(buffer, offset, 4, WED_NUM_WALL_POLYGONS,
                                                      WallPolygon.class);
    addField(countWallpolygons);
    SectionOffset offsetPolygons = new SectionOffset(buffer, offset + 4, WED_OFFSET_WALL_POLYGONS,
                                                     WallPolygon.class);
    addField(offsetPolygons);
    HexNumber offsetVertices = new HexNumber(buffer, offset + 8, 4, WED_OFFSET_VERTICES);
    addField(offsetVertices);
    SectionOffset offsetWallgroups = new SectionOffset(buffer, offset + 12, WED_OFFSET_WALL_GROUPS,
                                                       Wallgroup.class);
    addField(offsetWallgroups);
    SectionOffset offsetPolytable = new SectionOffset(buffer, offset + 16, WED_OFFSET_WALL_POLYGON_LOOKUP,
                                                      IndexNumber.class);
    addField(offsetPolytable);

    HexNumber offsets[] = new HexNumber[]{offsetOverlays, offsetHeader2, offsetDoors, offsetDoortile,
                                          offsetPolygons, offsetWallgroups, offsetPolytable,
                                          new HexNumber(ByteBuffer.wrap(Misc
                                                          .intToArray(buffer.limit() - startOffset))
                                                              .order(ByteOrder.LITTLE_ENDIAN),
                                                        0, 4, "")};
    Arrays.sort(offsets, new Comparator<HexNumber>()
    {
      @Override
      public int compare(HexNumber s1, HexNumber s2)
      {
        return s1.getValue() - s2.getValue();
      }
    });

    offset = offsetDoors.getValue();
    for (int i = 0; i < countDoors.getValue(); i++) {
      Door door = new Door(this, buffer, offset, i);
      offset = door.getEndOffset();
      door.readVertices(buffer, offsetVertices.getValue());
      addField(door);
    }

    offset = offsetWallgroups.getValue();
    int countPolytable = 0;
    int countWallgroups = (offsets[ArrayUtil.indexOf(offsets, offsetWallgroups) + 1].getValue() -
                           offsetWallgroups.getValue()) / 4;
    for (int i = 0; i < countWallgroups; i++) {
      Wallgroup wall = new Wallgroup(this, buffer, offset, i);
      offset = wall.getEndOffset();
      countPolytable = Math.max(countPolytable, wall.getNextPolygonIndex());
      addField(wall);
    }

    offset = offsetPolygons.getValue();
    for (int i = 0; i < countWallpolygons.getValue(); i++) {
      Polygon poly = new WallPolygon(this, buffer, offset, i);
      offset = poly.getEndOffset();
      poly.readVertices(buffer, offsetVertices.getValue());
      addField(poly);
    }

    offset = offsetPolytable.getValue();
    for (int i = 0; i < countPolytable; i++) {
      addField(new IndexNumber(buffer, offset + i * 2, 2, WED_WALL_POLYGON_INDEX + " " + i));
    }

    int endoffset = offset;
    for (final StructEntry entry : getFlatFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset) {
        endoffset = entry.getOffset() + entry.getSize();
      }
    }
    return endoffset;
  }

  private void updateSectionOffsets(AddRemovable datatype, int size)
  {
    if (!(datatype instanceof Vertex)) {
      HexNumber offset_vertices = (HexNumber)getAttribute(WED_OFFSET_VERTICES);
      if (datatype.getOffset() <= offset_vertices.getValue()) {
        offset_vertices.incValue(size);
      }
    }
    if (!(datatype instanceof RemovableDecNumber)) {
      HexNumber offset_doortilemap = (HexNumber)getAttribute(WED_OFFSET_DOOR_TILEMAP_LOOKUP);
      if (datatype.getOffset() <= offset_doortilemap.getValue()) {
        offset_doortilemap.incValue(size);
      }
    }

    for (final StructEntry o : getFields()) {
      if (o instanceof Overlay) {
        ((Overlay)o).updateOffsets(datatype.getOffset(), size);
      }
    }

    // Assumes polygon offset is correct
    int offset = ((IsNumeric)getAttribute(WED_OFFSET_WALL_POLYGONS)).getValue();
    offset += ((IsNumeric)getAttribute(WED_NUM_WALL_POLYGONS)).getValue() * 18;
    for (final StructEntry o : getFields()) {
      if (o instanceof Door) {
        ((Door)o).updatePolygonsOffset(offset);
      }
    }
  }

  private void updateVertices()
  {
    // Assumes vertices offset is correct
    int offset = ((IsNumeric)getAttribute(WED_OFFSET_VERTICES)).getValue();
    int count = 0;
    for (final StructEntry o : getFields()) {
      if (o instanceof Polygon) {
        Polygon polygon = (Polygon)o;
        int vertNum = polygon.updateVertices(offset, count);
        offset += 4 * vertNum;
        count += vertNum;
      }
      else if (o instanceof Door) {
        Door door = (Door)o;
        for (final StructEntry q : door.getFields()) {
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
