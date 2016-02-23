// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vertex;

import org.infinity.resource.AbstractStruct;

public final class ClosedVertexImpeded extends Vertex
{
  // ClosedVertexImpeded-specific field labels
  public static final String VERTEX_CLOSED_IMPEDED  = "Closed vertex, impeded";

  public ClosedVertexImpeded() throws Exception
  {
    super(null, VERTEX_CLOSED_IMPEDED, new byte[4], 0);
  }

  public ClosedVertexImpeded(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, VERTEX_CLOSED_IMPEDED + " " + nr, buffer, offset);
  }
}

