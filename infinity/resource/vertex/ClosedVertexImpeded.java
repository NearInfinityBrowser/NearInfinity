// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import infinity.resource.AbstractStruct;

public final class ClosedVertexImpeded extends Vertex
{
  public ClosedVertexImpeded() throws Exception
  {
    super(null, "Closed vertex, impeded", new byte[4], 0);
  }

  public ClosedVertexImpeded(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Closed vertex, impeded " + nr, buffer, offset);
  }
}

