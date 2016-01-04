// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import infinity.resource.AbstractStruct;

public final class OpenVertexImpeded extends Vertex
{
  // OpenVertexImpeded-specific field labels
  public static final String VERTEX_OPEN_IMPEDED  = "Open vertex, impeded";

  public OpenVertexImpeded() throws Exception
  {
    super(null, VERTEX_OPEN_IMPEDED, new byte[4], 0);
  }

  public OpenVertexImpeded(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, VERTEX_OPEN_IMPEDED + " " + nr, buffer, offset);
  }
}

