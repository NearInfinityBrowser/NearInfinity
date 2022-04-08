// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wed;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class WallPolygon extends Polygon {
  // WED/WallPolygon-specific field labels
  public static final String WED_POLY_WALL = "Wall polygon";

  public WallPolygon() throws Exception {
    super(null, WED_POLY_WALL, StreamUtils.getByteBuffer(18), 0);
  }

  public WallPolygon(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, WED_POLY_WALL + " " + nr, buffer, offset);
  }
}
