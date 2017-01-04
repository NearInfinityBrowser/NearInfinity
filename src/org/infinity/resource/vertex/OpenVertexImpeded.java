// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vertex;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class OpenVertexImpeded extends Vertex
{
  // OpenVertexImpeded-specific field labels
  public static final String VERTEX_OPEN_IMPEDED  = "Impeded cell (open)";

  public OpenVertexImpeded() throws Exception
  {
    super(null, VERTEX_OPEN_IMPEDED, StreamUtils.getByteBuffer(4), 0);
  }

  public OpenVertexImpeded(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, VERTEX_OPEN_IMPEDED + " " + nr, buffer, offset);
  }
}

