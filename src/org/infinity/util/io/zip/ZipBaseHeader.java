// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.zip.ZipError;

/**
 * Common base class for internal zip structures.
 */
public abstract class ZipBaseHeader implements Comparable<ZipBaseHeader>
{
  public static final int SIG_LOCAL       = 0x04034b50;
  public static final int SIG_CENTRAL     = 0x02014b50;
  public static final int SIG_CENTRAL_END = 0x06054b50;

  /** Absolute start offset of the header structure. */
  public long offset;

  /** Total size of the header structure in bytes. */
  public long size;

  /** Signature of the header structure. */
  public long signature;


  protected ZipBaseHeader(long offset, long signature)
  {
    this.offset = offset;
    this.signature = signature;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + Long.hashCode(offset);
    hash = 31 * hash + Long.hashCode(size);
    hash = 31 * hash + Long.hashCode(signature);
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    } else if (o instanceof ZipBaseHeader) {
      return (((ZipBaseHeader)o).offset == this.offset);
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(ZipBaseHeader o)
  {
    if (this == o) {
      return 0;
    } else if (o != null) {
      if (this.offset < o.offset) {
        return -1;
      } else if (this.offset > o.offset) {
        return 1;
      } else {
        return 0;
      }
    } else {
      throw new NullPointerException();
    }
  }



  static final long readFullyAt(SeekableByteChannel ch, byte[] buf, int ofs, long len, long pos)
      throws IOException
  {
    ByteBuffer bb = ByteBuffer.wrap(buf);
    bb.position(ofs);
    bb.limit((int)(ofs + len));
    return readFullyAt(ch, bb, pos);
  }

  static final long readFullyAt(SeekableByteChannel ch, ByteBuffer bb, long pos) throws IOException
  {
    synchronized (ch) {
      return ch.position(pos).read(bb);
    }
  }

  static final void zerror(String msg)
  {
    throw new ZipError(msg);
  }

}
