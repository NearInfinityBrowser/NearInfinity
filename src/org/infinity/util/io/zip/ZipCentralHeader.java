// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io.zip;

import static org.infinity.util.io.zip.ZipConstants.CENSIG;
import static org.infinity.util.io.zip.ZipConstants.LOCEXT;
import static org.infinity.util.io.zip.ZipConstants.LOCHDR;
import static org.infinity.util.io.zip.ZipConstants.LOCNAM;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import org.infinity.util.io.StreamUtils;

/**
 * Storage class for a single central directory entry.
 */
public class ZipCentralHeader extends ZipLocalHeader
{
  /** Zip version of compression tool that encoded the file. */
  public int versionCreated;

  /** Number of disk on which this file begins. (Must be 0.) */
  public int idxDisk;

  /** Internal zip attributes. */
  public int attribInternal;

  /** Filesystem-dependent file attributes. */
  public long attribExternal;

  /** Start offset of associated local header structure. */
  public long ofsLocalHeader;

  /** Optional file comment as ascii string. (Is never {@code null}) */
  public byte[] comment;

  // Cached local header
  private ZipLocalHeader localHeader;

  public ZipCentralHeader(ByteBuffer buffer, long absOffset)
  {
    super(absOffset, buffer.getInt() & 0xffffffffL);
    long headerStart = buffer.position() - 4L;
    if (this.signature != CENSIG) {
      zerror("invalid CEN header (bad signature)");
    }
    this.versionCreated = buffer.getShort() & 0xffff;
    this.version = buffer.getShort() & 0xffff;
    if (this.version > 20) {
      zerror("Unsupported zip version: " + this.version);
    }
    this.flags = buffer.getShort() & 0xffff;
    this.compression = buffer.getShort() & 0xffff;
    if (this.compression != 0) {
      zerror("Unsupported compression method: " + this.compression);
    }
    this.mtime = ZipUtils.dosToJavaTime(buffer.getInt() & 0xffffffffL);
    this.atime = this.ctime = this.mtime;
    this.crc32 = buffer.getInt() & 0xffffffffL;
    this.sizeCompressed = buffer.getInt() & 0xffffffffL;
    this.sizeUncompressed = buffer.getInt() & 0xffffffffL;
    if ((this.sizeCompressed == 0xffffffffL) || (this.sizeUncompressed == 0xffffffffL)) {
      zerror("ZIP64 header not supported");
    }
    short nameLength = buffer.getShort();
    short extraLength = buffer.getShort();
    short commentLength = buffer.getShort();
    this.idxDisk = buffer.getShort() & 0xffff;
    this.attribInternal = buffer.getShort() & 0xffff;
    this.attribExternal = buffer.getInt() & 0xffffffffL;
    this.ofsLocalHeader = buffer.getInt() & 0xffffffffL;
    this.fileName = new byte[nameLength];
    buffer.get(this.fileName);
    this.extra = new byte[extraLength];
    if (extraLength > 0) {
      buffer.get(this.extra);
    }
    this.comment = new byte[commentLength];
    if (commentLength > 0) {
      buffer.get(this.comment);
    }
    this.size = (int)(buffer.position() - headerStart);

    this.localHeader = null;
  }

  /**
   * Returns the absolute offset to the start of file data.
   * @param ch ByteChannel of the zip archive.
   * @return Absolute offset to data start from beginning of zip archive.
   */
  public long getDataOffset(SeekableByteChannel ch) throws IOException
  {
    synchronized (ch) {
      return getLocalHeader(ch).getDataOffset();
    }
  }

  @Override
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = 31 * hash + versionCreated;
    hash = 31 * hash + idxDisk;
    hash = 31 * hash + attribInternal;
    hash = 31 * hash + Long.hashCode(attribExternal);
    hash = 31 * hash + Long.hashCode(ofsLocalHeader);
    hash = 31 * hash + ((comment == null) ? 0 : comment.hashCode());
    hash = 31 * hash + ((localHeader == null) ? 0 : localHeader.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    } else if (o instanceof ZipCentralHeader) {
      return (((ZipCentralHeader)o).ofsLocalHeader == this.ofsLocalHeader);
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(ZipBaseHeader o)
  {
    if (this == o) {
      return 0;
    } else if (o instanceof ZipCentralHeader) {
      if (this.ofsLocalHeader < ((ZipCentralHeader)o).ofsLocalHeader) {
        return -1;
      } else if (this.ofsLocalHeader > ((ZipCentralHeader)o).ofsLocalHeader) {
        return 1;
      } else {
        return 0;
      }
    } else if (o != null) {
      return super.compareTo(o);
    } else {
      throw new NullPointerException();
    }
  }


  private ZipLocalHeader getLocalHeader(SeekableByteChannel ch) throws IOException
  {
    if (localHeader == null) {
      // reading base LOC header
      int locSize = LOCHDR;
      ByteBuffer locBuf = StreamUtils.getByteBuffer(locSize);
      if (ch.position(ofsLocalHeader).read(locBuf) != locSize) {
        zerror("read LOC header failed");
      }
      locBuf.flip();
      int nameLen = locBuf.getShort(LOCNAM) & 0xffff;
      int extraLen = locBuf.getShort(LOCEXT) & 0xffff;
      locSize += nameLen + extraLen;

      // reading LOC header, including filename and extra data
      locBuf = StreamUtils.getByteBuffer(locSize);
      if (ch.position(ofsLocalHeader).read(locBuf) != locSize) {
        zerror("read LOC header failed");
      }
      locBuf.flip();
      ZipLocalHeader locHeader = new ZipLocalHeader(locBuf, ofsLocalHeader);
      if (!Arrays.equals(this.fileName, locHeader.fileName)) {  // just in case
        zerror("Filename mismatch between CEN and LOC");
      }
      if (sizeCompressed != locHeader.sizeCompressed) {    // just in case
        zerror("File size mismatch between CEN and LOC");
      }
      localHeader = locHeader;
    }
    return localHeader;
  }
}
