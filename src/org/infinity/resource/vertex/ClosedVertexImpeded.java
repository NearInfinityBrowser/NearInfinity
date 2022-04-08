// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vertex;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class ClosedVertexImpeded extends Vertex {
  // ClosedVertexImpeded-specific field labels
  public static final String VERTEX_CLOSED_IMPEDED = "Impeded cell (closed)";

  public ClosedVertexImpeded() throws Exception {
    super(null, VERTEX_CLOSED_IMPEDED, StreamUtils.getByteBuffer(4), 0);
  }

  public ClosedVertexImpeded(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, VERTEX_CLOSED_IMPEDED + " " + nr, buffer, offset);
  }
}
