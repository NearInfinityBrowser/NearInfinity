// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
 * A simple OutputStream implementation which uses a {@link ByteBuffer} as backend.
 */
public class ByteBufferOutputStream extends OutputStream
{
  private ByteBuffer buf;

  public ByteBufferOutputStream(ByteBuffer buf)
  {
    if (buf == null) {
      throw new NullPointerException();
    }
    this.buf = buf;
  }

  @Override
  public void write(int b) throws IOException
  {
    if (buf == null) {
      throw new IOException("Stream not open");
    }
    buf.put((byte)b);
  }

  public void write(byte[] bytes, int off, int len) throws IOException
  {
    if (buf == null) {
      throw new IOException("Stream not open");
    }
    buf.put(bytes, off, len);
  }

  public void close() throws IOException
  {
    if (buf != null) {
      if (buf instanceof MappedByteBuffer) {
        ((MappedByteBuffer)buf).force();
      }
      buf = null;
    }
  }
}
