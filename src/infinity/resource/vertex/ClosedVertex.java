// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import infinity.resource.AbstractStruct;

public final class ClosedVertex extends Vertex
{
  public ClosedVertex() throws Exception
  {
    super(null, "Closed vertex", new byte[4], 0);
  }

  public ClosedVertex(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Closed vertex " + nr, buffer, offset);
  }
}

