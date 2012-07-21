// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.resource.AbstractStruct;

final class OpenPolygon extends Polygon
{
  OpenPolygon() throws Exception
  {
    super(null, "Open polygon", new byte[18], 0);
  }

  OpenPolygon(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Open polygon " + nr, buffer, offset);
  }
}

