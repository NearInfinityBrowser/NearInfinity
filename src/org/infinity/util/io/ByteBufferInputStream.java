// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A simple InputStream implementation which uses one or more {@link ByteBuffer} objects as backend.
 */
public class ByteBufferInputStream extends InputStream
{
  private final ByteBuffer[] bufs;

  private int curBuf;
  private int markBufIndex, markBufPosition;

  public ByteBufferInputStream(ByteBuffer buf)
  {
    this(new ByteBuffer[]{buf});
  }

  public ByteBufferInputStream(ByteBuffer... bufs)
  {
    if (bufs == null) {
      throw new NullPointerException();
    }

    this.markBufIndex = -1;
    this.markBufPosition = -1;

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
  public int read() throws IOException
  {
    if (!isOpen()) {
      throw new IOException("Stream not open");
    }
    ByteBuffer buf = getBuffer(true);
    if (!buf.hasRemaining()) {
      return -1;
    }
    return buf.get() & 0xff;
  }

  @Override
  public int read(byte[] bytes, int off, int len) throws IOException
  {
    if (bytes == null) {
      throw new NullPointerException();
    }
    if (!isOpen()) {
      throw new IOException("Stream not open");
    }

    int read = 0;
    while (read < len) {
      ByteBuffer buf = getBuffer(true);
      if (buf == null) {
        break;
      }
      int remaining = Math.min(buf.remaining(), len - read);
      if (remaining <= 0) {
        break;
      }
      buf.get(bytes, off + read, remaining);
      read += remaining;
    }

    return (read > 0) ? read : -1;
  }

  @Override
  public long skip(long n) throws IOException
  {
    if (n <= 0) {
      return 0;
    }
    byte[] tmp = new byte[Math.min(1024, (int)n)];
    int read = 0;
    while (read < n) {
      int remaining = Math.min(tmp.length, (int)n - read);
      int n2 = read(tmp, 0, remaining);
      if (n2 < 0) {
        break;
      }
      read += n2;
    }
    return read;
  }

  @Override
  public int available() throws IOException
  {
    if (isOpen() && curBuf < bufs.length) {
      int idx = curBuf;
      int sum = bufs[idx++].remaining();
      for (int i = idx; i < bufs.length; i++) {
        sum += bufs[i].remaining();
      }
      return sum;
    }
    throw new IOException("Stream is closed");
  }

  @Override
  public void close() throws IOException
  {
    if (curBuf >= 0) {
      synchronized (this) {
        curBuf = -1;
      }
    }
  }

  @Override
  public boolean markSupported()
  {
    return true;
  }

  @Override
  public synchronized void mark(int readlimit)
  {
    if (isOpen()) {
      ByteBuffer buffer = getBuffer(true);
      if (buffer != null) {
        markBufIndex = curBuf;
        markBufPosition = buffer.position();
      } else {
        markBufIndex = -1;
        markBufPosition = -1;
      }
    }
  }

  @Override
  public synchronized void reset() throws IOException
  {
    if (isOpen() && markBufIndex >= 0 && markBufIndex < bufs.length && markBufPosition >= 0) {
      curBuf = markBufIndex;
      bufs[curBuf].position(markBufPosition);
      markBufIndex = -1;
      markBufPosition = -1;
    } else {
      throw new IOException();
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
      if (curBuf < bufs.length) {
        return bufs[curBuf];
      }
    }
    return null;
  }
}
