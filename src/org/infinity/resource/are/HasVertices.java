// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

public interface HasVertices {
  void readVertices(ByteBuffer buffer, int offset) throws Exception;

  int updateVertices(int offset, int number);
}
