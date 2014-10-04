// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.resource.AbstractStruct;

public final class ClosedPolygon extends Polygon
{
  public ClosedPolygon() throws Exception
  {
    super(null, "Closed polygon", new byte[18], 0);
  }

  public ClosedPolygon(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Closed polygon " + nr, buffer, offset);
  }
}

