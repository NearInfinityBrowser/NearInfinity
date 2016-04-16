// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wed;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class OpenPolygon extends Polygon
{
  // WED/OpenPolygon-specific field labels
  public static final String WED_POLY_OPEN  = "Open polygon";

  public OpenPolygon() throws Exception
  {
    super(null, WED_POLY_OPEN, StreamUtils.getByteBuffer(18), 0);
  }

  public OpenPolygon(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, WED_POLY_OPEN + " " + nr, buffer, offset);
  }
}

