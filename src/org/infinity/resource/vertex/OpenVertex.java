// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vertex;

import org.infinity.resource.AbstractStruct;

public final class OpenVertex extends Vertex
{
  // OpenVertex-specific field labels
  public static final String VERTEX_OPEN  = "Open vertex";

  public OpenVertex() throws Exception
  {
    super(null, VERTEX_OPEN, new byte[4], 0);
  }

  public OpenVertex(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, VERTEX_OPEN + " " + nr, buffer, offset);
  }
}

