// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wed;

import org.infinity.resource.AbstractStruct;

public final class WallPolygon extends Polygon
{
  // WED/WallPolygon-specific field labels
  public static final String WED_POLY_WALL  = "Wall polygon";

  public WallPolygon() throws Exception
  {
    super(null, WED_POLY_WALL, new byte[18], 0);
  }

  public WallPolygon(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, WED_POLY_WALL + " " + nr, buffer, offset);
  }
}

