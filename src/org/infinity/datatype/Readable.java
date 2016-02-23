// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

public interface Readable
{
  /**
   * Read data from buffer, starting at offset. Returns first offset after processed data.
   * @param buffer The buffer to read from.
   * @param offset The start offset within the buffer.
   * @return The first index after the processed data.
   * @throws Exception
   */
  int read(byte[] buffer, int offset) throws Exception;
}
