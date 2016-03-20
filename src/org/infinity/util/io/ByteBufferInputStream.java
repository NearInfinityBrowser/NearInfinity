// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A simple InputStream implementation which uses a {@link ByteBuffer} as backend.
 */
public class ByteBufferInputStream extends InputStream
{
  private ByteBuffer buf;

  public ByteBufferInputStream(ByteBuffer buf)
  {
    if (buf == null) {
      throw new NullPointerException();
    }
    this.buf = buf;
  }

  @Override
  public int read() throws IOException
  {
    if (buf == null) {
      throw new IOException("Stream not open");
    }
    if (!buf.hasRemaining()) {
      return -1;
    }
    return buf.get() & 0xff;
  }

  public int read(byte[] bytes, int off, int len) throws IOException
  {
    if (buf == null) {
      throw new IOException("Stream not open");
    }
    if (!buf.hasRemaining()) {
      return -1;
    }
    len = Math.min(len, buf.remaining());
    buf.get(bytes, off, len);
    return len;
  }

  public void close() throws IOException
  {
    if (buf != null) {
      buf = null;
    }
  }
}
