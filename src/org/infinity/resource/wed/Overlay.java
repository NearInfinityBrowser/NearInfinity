// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wed;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;

public final class Overlay extends AbstractStruct // implements AddRemovable, HasChildStructs
{
  // WED/Overlay-specific field labels
  public static final String WED_OVERLAY                        = "Overlay";
  public static final String WED_OVERLAY_WIDTH                  = "Width";
  public static final String WED_OVERLAY_HEIGHT                 = "Height";
  public static final String WED_OVERLAY_TILESET                = "Tileset";
  public static final String WED_OVERLAY_NUM_UNIQUE_TILES       = "# unique tiles";
  public static final String WED_OVERLAY_MOVEMENT_TYPE          = "Movement type";
  public static final String WED_OVERLAY_OFFSET_TILEMAP         = "Tilemap offset";
  public static final String WED_OVERLAY_OFFSET_TILEMAP_LOOKUP  = "Tilemap lookup offset";
  public static final String WED_OVERLAY_TILEMAP_INDEX          = "Tilemap index";

  public static final String[] s_movement = {"Default", "Disable rendering", "Alternate rendering"};

  public Overlay(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, WED_OVERLAY + " " + number, buffer, offset);
  }

  public void updateOffsets(int offset, int size)
  {
    HexNumber offset_tilemap = (HexNumber)getAttribute(WED_OVERLAY_OFFSET_TILEMAP);
    if (offset_tilemap.getValue() >= offset)
      offset_tilemap.incValue(size);

    HexNumber offset_tilelookup = (HexNumber)getAttribute(WED_OVERLAY_OFFSET_TILEMAP_LOOKUP);
    if (offset_tilelookup.getValue() >= offset)
      offset_tilelookup.incValue(size);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    DecNumber width = new DecNumber(buffer, offset, 2, WED_OVERLAY_WIDTH);
    addField(width);
    DecNumber height = new DecNumber(buffer, offset + 2, 2, WED_OVERLAY_HEIGHT);
    addField(height);
    ResourceRef tileset = new ResourceRef(buffer, offset + 4, WED_OVERLAY_TILESET, "TIS");
    addField(tileset);
    if (Profile.isEnhancedEdition()) {
      addField(new DecNumber(buffer, offset + 12, 2, WED_OVERLAY_NUM_UNIQUE_TILES));
      addField(new Bitmap(buffer, offset + 14, 2, WED_OVERLAY_MOVEMENT_TYPE, s_movement));
    } else {
      addField(new Unknown(buffer, offset + 12, 4));
    }
    final SectionOffset offset_tilemap = new SectionOffset(buffer, offset + 16, WED_OVERLAY_OFFSET_TILEMAP, Tilemap.class);
    addField(offset_tilemap);
    final SectionOffset offset_tilelookup = new SectionOffset(buffer, offset + 20, WED_OVERLAY_OFFSET_TILEMAP_LOOKUP, IndexNumber.class);
    addField(offset_tilelookup);
    int retoff = offset + 24;

    // readTilemap
    int lookuptablesize = 0;
    if (!tileset.toString().equalsIgnoreCase(".TIS")) {
      offset = offset_tilemap.getValue();
      int map_count = width.getValue() * height.getValue();
      for (int i = 0; i < map_count; i++) {
        Tilemap map = new Tilemap(this, buffer, offset, i);
        offset = map.getEndOffset();
        lookuptablesize += map.getTileCount();
        addField(map);
      }
    }
    // readLookuptable
    offset = offset_tilelookup.getValue();
    for (int i = 0; i < lookuptablesize; i++) {
      addField(new IndexNumber(buffer, offset + i * 2, 2, WED_OVERLAY_TILEMAP_INDEX + " " + i));
    }
    return retoff;
  }
}
