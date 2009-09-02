// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import infinity.resource.AbstractStruct;

public final class OpenVertex extends Vertex
{
  public OpenVertex() throws Exception
  {
    super(null, "Open vertex", new byte[4], 0);
  }

  public OpenVertex(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Open vertex " + nr, buffer, offset);
  }
}

