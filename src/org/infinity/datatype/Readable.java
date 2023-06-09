// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;

public interface Readable {
  /**
   * Read data from buffer, starting at offset. Returns first offset after processed data.
   *
   * @param buffer The {@link ByteBuffer} to read from.
   * @param offset The start offset within the buffer.
   * @return The first index after the processed data.
   * @throws Exception
   */
  int read(ByteBuffer buffer, int offset) throws Exception;
}
