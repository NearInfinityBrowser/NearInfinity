// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vertex;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class OpenVertex extends Vertex {
  // OpenVertex-specific field labels
  public static final String VERTEX_OPEN = "Open vertex";

  public OpenVertex() throws Exception {
    super(null, VERTEX_OPEN, StreamUtils.getByteBuffer(4), 0);
  }

  public OpenVertex(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, VERTEX_OPEN + " " + nr, buffer, offset);
  }
}
