// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import infinity.resource.AbstractStruct;

public final class ClosedVertex extends Vertex
{
  // ClosedVertex-specific field labels
  public static final String VERTEX_CLOSED  = "Closed vertex";

  public ClosedVertex() throws Exception
  {
    super(null, VERTEX_CLOSED, new byte[4], 0);
  }

  public ClosedVertex(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, VERTEX_CLOSED + " " + nr, buffer, offset);
  }
}

