// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.io.IOException;
import java.io.OutputStream;
import org.infinity.datatype.Readable;

/**
 * The interface implemented by objects which can be serialized in the self-natural
 * internal format. As a rule, the same objects implements the {@link Readable} interface
 * by means of which they can be read back from the input stream.
 *
 * @see org.infinity.datatype.Readable
 */
public interface Writeable
{
  /**
   * Writes content of this object to specified output stream.
   *
   * @param os Stream with binary representation of this object in its natural format
   *
   * @throws IOException if an I/O error occurs
   * @throws NullPointerException if os is {@code null}
   */
  void write(OutputStream os) throws IOException;
}

