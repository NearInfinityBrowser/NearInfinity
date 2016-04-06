// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vertex;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class ClosedVertex extends Vertex
{
  // ClosedVertex-specific field labels
  public static final String VERTEX_CLOSED  = "Closed vertex";

  public ClosedVertex() throws Exception
  {
    super(null, VERTEX_CLOSED, StreamUtils.getByteBuffer(4), 0);
  }

  public ClosedVertex(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, VERTEX_CLOSED + " " + nr, buffer, offset);
  }
}

