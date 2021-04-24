// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io.zip;

import static org.infinity.util.io.zip.ZipConstants.LOCSIG;

import java.nio.ByteBuffer;

/**
 * Storage class for a single local zip header.
 */
public class ZipLocalHeader extends ZipBaseHeader
{
  /** Required zip version to decode data. */
  public int version;

  /** Flags defining special properties of file data. */
  public int flags;

  /** The compression method used to encode the file. */
  public int compression;

  /** Modified time in standard MS-DOS format. */
  public long mtime;

  /** Access time in standard MS-DOS format. (Unused) */
  public long atime;

  /** Creation time in standard MS-DOS format. (Unused) */
  public long ctime;

  /** Checksum of file data in CRC-32 format. */
  public long crc32;

  /** Compressed file size in bytes. */
  public long sizeCompressed;

  /** Uncompressed file size in bytes. */
  public long sizeUncompressed;

  /** The name of the file with relative path as byte array. */
  public byte[] fileName;

  /** Optional extra data. (Is never {@code null}) */
  public byte[] extra;

  protected ZipLocalHeader(long offset, long signature)
  {
    super(offset, signature);
  }

  public ZipLocalHeader(ByteBuffer buffer, long absOffset)
  {
    super(absOffset, buffer.getInt());
    long headerStart = buffer.position() - 4L;
    if (this.signature != LOCSIG) {
      zerror("invalid LOC header (bad signature)");
    }
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
    this.fileName = new byte[nameLength];
    buffer.get(this.fileName);
    this.extra = new byte[extraLength];
    if (extraLength > 0) {
      buffer.get(this.extra);
    }
    this.size = (int)(buffer.position() - headerStart);
  }

  /** Returns the absolute start offset to file data. */
  public long getDataOffset()
  {
    return this.offset + this.size;
  }
}
