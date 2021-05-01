// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
 * A simple OutputStream implementation which uses one or  more {@link ByteBuffer} objects as backend.
 */
public class ByteBufferOutputStream extends OutputStream
{
  private final ByteBuffer[] bufs;

  private int curBuf;

  public ByteBufferOutputStream(ByteBuffer buf)
  {
    this(new ByteBuffer[]{buf});
  }

  public ByteBufferOutputStream(ByteBuffer... bufs)
  {
    if (bufs == null) {
      throw new NullPointerException();
    }
    if (bufs.length > 0) {
      this.bufs = new ByteBuffer[bufs.length];
      for (int i = 0; i < bufs.length; i++) {
        if (bufs[i] == null) {
          throw new NullPointerException();
        }
        this.bufs[i] = bufs[i];
      }
      this.curBuf = 0;
    } else {
      this.bufs = null;
      this.curBuf = -1;
    }
  }

  @Override
  public void write(int b) throws IOException
  {
    if (!isOpen()) {
      throw new IOException("Stream not open");
    }
    ByteBuffer buf = getBuffer(true);
    if (!buf.hasRemaining()) {
      throw new BufferOverflowException();
    }
    buf.put((byte)b);
  }

  @Override
  public void write(byte[] bytes, int off, int len) throws IOException
  {
    if (bytes == null) {
      throw new NullPointerException();
    }
    if(off < 0 || len < 0 || off + len > bytes.length) {
      throw new IndexOutOfBoundsException();
    }
    if (!isOpen()) {
      throw new IOException("Stream not open");
    }

    int written = 0;
    while (written < len) {
      ByteBuffer buf = getBuffer(true);
      int remaining = Math.min(buf.remaining(), len - written);
      if (remaining <= 0) {
        throw new IndexOutOfBoundsException();
      }
      buf.put(bytes, off + written, remaining);
      written += remaining;
    }
  }

  @Override
  public void close() throws IOException
  {
    if (curBuf >= 0) {
      for (final ByteBuffer buf: bufs) {
        if (buf instanceof MappedByteBuffer) {
          ((MappedByteBuffer)buf).force();
        }
      }
      synchronized (this) {
        curBuf = -1;
      }
    }
  }

  private boolean isOpen()
  {
    return (curBuf >= 0);
  }

  private void updateBuffer()
  {
    if (isOpen()) {
      if (curBuf < bufs.length) {
        if (bufs[curBuf].remaining() <= 0) {
          synchronized (this) {
            curBuf++;
          }
        }
      }
    }
  }

  private ByteBuffer getBuffer(boolean update)
  {
    if (isOpen()) {
      if (update) {
        updateBuffer();
      }
      return bufs[curBuf];
    }
    return null;
  }
}
