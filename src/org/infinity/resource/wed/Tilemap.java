// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wed;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;

public final class Tilemap extends AbstractStruct // implements AddRemovable
{
  // WED/Tilemap-specific field labels
  public static final String WED_TILEMAP                  = "Tilemap";
  public static final String WED_TILEMAP_TILE_INDEX_PRI   = "Tilemap index (primary)";
  public static final String WED_TILEMAP_TILE_COUNT_PRI   = "Tilemap count (primary)";
  public static final String WED_TILEMAP_TILE_INDEX_SEC   = "Tile index (secondary)";
  public static final String WED_TILEMAP_DRAW_OVERLAYS    = "Draw Overlays";
  public static final String WED_TILEMAP_ANIMATION_SPEED  = "Animation speed";

  private static final String[] s_flags = {"Primary overlay only", "Unused", "Overlay 1",
                                           "Overlay 2", "Overlay 3", "Overlay 4", "Overlay 5",
                                           "Overlay 6", "Overlay 7"};

  public Tilemap(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, WED_TILEMAP + " " + number, buffer, offset, 5);
  }

  public int getTileCount()
  {
    return ((IsNumeric)getAttribute(WED_TILEMAP_TILE_COUNT_PRI)).getValue();
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 2, WED_TILEMAP_TILE_INDEX_PRI));
    addField(new DecNumber(buffer, offset + 2, 2, WED_TILEMAP_TILE_COUNT_PRI));
    addField(new DecNumber(buffer, offset + 4, 2, WED_TILEMAP_TILE_INDEX_SEC));
    addField(new Flag(buffer, offset + 6, 1, WED_TILEMAP_DRAW_OVERLAYS, s_flags));
    addField(new DecNumber(buffer, offset + 7, 1, WED_TILEMAP_ANIMATION_SPEED));
    addField(new Unknown(buffer, offset + 8, 2));
    return offset + 10;
  }
}

