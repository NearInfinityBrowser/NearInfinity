// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.resource.AbstractStruct;

public final class WallPolygon extends Polygon
{
  public WallPolygon() throws Exception
  {
    super(null, "Wall polygon", new byte[18], 0);
  }

  public WallPolygon(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Wall polygon " + nr, buffer, offset);
  }
}

