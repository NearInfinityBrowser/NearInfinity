// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.resource.AbstractStruct;

public final class OpenPolygon extends Polygon
{
  // WED/OpenPolygon-specific field labels
  public static final String WED_POLY_OPEN  = "Open polygon";

  public OpenPolygon() throws Exception
  {
    super(null, WED_POLY_OPEN, new byte[18], 0);
  }

  public OpenPolygon(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, WED_POLY_OPEN + " " + nr, buffer, offset);
  }
}

