// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io.zip;

import static org.infinity.util.io.zip.ZipConstants.CENCOM;
import static org.infinity.util.io.zip.ZipConstants.CENEXT;
import static org.infinity.util.io.zip.ZipConstants.CENHDR;
import static org.infinity.util.io.zip.ZipConstants.CENNAM;
import static org.infinity.util.io.zip.ZipConstants.CENSIG;
import static org.infinity.util.io.zip.ZipConstants.ENDCOM;
import static org.infinity.util.io.zip.ZipConstants.ENDHDR;
import static org.infinity.util.io.zip.ZipConstants.ENDSIG;
import static org.infinity.util.io.zip.ZipConstants.END_MAXLEN;
import static org.infinity.util.io.zip.ZipConstants.LL;
import static org.infinity.util.io.zip.ZipConstants.LOCHDR;
import static org.infinity.util.io.zip.ZipConstants.LOCLEN;
import static org.infinity.util.io.zip.ZipConstants.LOCSIG;
import static org.infinity.util.io.zip.ZipConstants.LOCSIZ;
import static org.infinity.util.io.zip.ZipConstants.READBLOCKSZ;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_ENDHDR;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_ENDOFF;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_ENDSIG;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_ENDSIZ;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_ENDTOT;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_LOCHDR;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_LOCOFF;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_LOCSIG;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_MINVAL;
import static org.infinity.util.io.zip.ZipConstants.ZIP64_MINVAL32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import org.infinity.util.io.StreamUtils;

/**
 * Storage class for the end of central directory entry.
 */
public class ZipCentralEndHeader extends ZipBaseHeader
{
  /** Number of disk where this structure is located. */
  public int idxDisk;

  /** Number of disk where the central directory starts. */
  public int idxDiskCentral;

  /** Total number of central directory entries on this disk. */
  public int numEntriesDisk;

  /** Total number of central directory entries. */
  public int numEntries;

  /** Size of central directory in bytes. */
  public long sizeCentral;

  /** Start offset of central directory. */
  public long ofsCentral;

  /** Zip file comment. (Is never {@code null}) */
  public byte[] comment;


  public ZipCentralEndHeader(ByteBuffer buffer, long absOffset)
  {
    super(absOffset, buffer.getInt());
    long headerStart = buffer.position() - 4L;
    this.idxDisk = buffer.getShort();
    this.idxDiskCentral = buffer.getShort();
    this.numEntriesDisk = buffer.getShort();
    this.numEntries = buffer.getShort();
    this.sizeCentral = buffer.getInt();
    this.ofsCentral = buffer.getInt();
    short commentLength = buffer.getShort();
    this.comment = new byte[commentLength];
    if (commentLength > 0) {
      buffer.get(this.comment);
    }
    this.size = (int)(buffer.position() - headerStart);
  }


  // Attempts to find the CEN end header in the specified file
  static ZipCentralEndHeader findZipEndHeader(SeekableByteChannel ch) throws IOException
  {
    if (ch == null) {
      throw new NullPointerException();
    }
    if (!ch.isOpen()) {
      throw new IOException("Channel not open");
    }

    // try to find CEN from back to front first
    byte[] buf = new byte[READBLOCKSZ];
    long zipLen = ch.size();
    long minHeader = (zipLen - END_MAXLEN) > 0 ? zipLen - END_MAXLEN : 0;
    long minPos = minHeader - (buf.length - ENDHDR);
    ZipCentralEndHeader end = null;

    for (long pos = zipLen - buf.length; (pos >= minPos) && (end == null); pos -= (buf.length - ENDHDR)) {
      int off = 0;
      if (pos < 0) {
        // Pretend there are some NUL bytes before start of file
        off = (int)-pos;
        Arrays.fill(buf, 0, off, (byte)0);
      }
      int len = buf.length - off;
      if (readFullyAt(ch, buf, off, len, pos + off) != len) {
        zerror("zip END header not found");
      }

      // Now scan the block backwards for END header signature
      for (int i = buf.length - ENDHDR; (i >= 0) && (end == null); i--) {
        long sig = CENSIG(buf, i);
        if ((sig == ENDSIG) && (pos + i + ENDHDR + ENDCOM(buf, i) <= zipLen)) {
          // Found END header
          buf = Arrays.copyOfRange(buf, i, i + ENDHDR);
          end = new ZipCentralEndHeader(StreamUtils.getByteBuffer(buf), pos + i);
        }
      }
    }

    // Double check by parsing through CEN and updating END structure if necessary,
    // or search for END structure from front to back as fall back solution.
    if (end != null && end.ofsCentral >= end.offset) {
      zerror("invalid END header (bad central directory size)");
    }
    long curPos = (end != null) ? end.ofsCentral : 0;
    long endPos = (end != null) ? end.offset : zipLen;
    int bufSize = Math.max(Math.max(Math.max(12, LOCHDR), CENHDR), ENDHDR);
    buf = new byte[bufSize];

    // do a sequential search to find the first instance of a supported header signature
    while (curPos < endPos) {
      readFullyAt(ch, buf, 0, 4, curPos);
      long sig = CENSIG(buf, 0);
      if (sig == LOCSIG || sig == CENSIG || sig == ZIP64_ENDSIG || sig == ZIP64_LOCHDR || sig == ENDSIG) {
        break;
      }
      curPos++;
    }

    while (curPos < endPos) {
      long sig = LOCSIG(buf);
      if (sig == LOCSIG) {
        if (readFullyAt(ch, buf, 0, LOCHDR, curPos) != LOCHDR) {
          zerror("read LOC structure failed");
        }
        long csize = LOCSIZ(buf);
        long size = LOCLEN(buf);
        if (csize == ZIP64_MINVAL || size == ZIP64_MINVAL) {
          zerror("ZIP64 LOC structure not supported");
        }
      } else if (sig == CENSIG) { // central directory record
        if (readFullyAt(ch, buf, 0, CENHDR, curPos) != CENHDR) {
          zerror("read CEN tables failed");
        }
        curPos += CENHDR + CENNAM(buf, 0) + CENEXT(buf, 0) + CENCOM(buf, 0);
      } else if (sig == ZIP64_ENDSIG) { // zip64 end of central directory record
        if (readFullyAt(ch, buf, 0, 12, curPos) != 12) {
          zerror("read ZIP64 end header failed");
        }
        curPos += 12 + LL(buf, 4);
      } else if (sig == ZIP64_LOCSIG) { // zip64 end of central directory locator
        curPos += ZIP64_LOCHDR;
      } else if (sig == ENDSIG) { // END header
        if (readFullyAt(ch, buf, 0, ENDHDR, curPos) != ENDHDR) {
          zerror("zip END header not found");
        }
        if (end == null) {
          end = new ZipCentralEndHeader(StreamUtils.getByteBuffer(buf), curPos);
        }
        break;
      } else {
        zerror("invalid header data found");
      }
      readFullyAt(ch, buf, 0, 4, curPos);
    }

    if (end != null) {
      if (end.sizeCentral == ZIP64_MINVAL || end.ofsCentral == ZIP64_MINVAL || end.numEntries == ZIP64_MINVAL32) {
        // need to find the zip64 end
        byte[] loc64 = new byte[ZIP64_LOCHDR];
        if (readFullyAt(ch, loc64, 0, loc64.length, end.offset - ZIP64_LOCHDR) != loc64.length) {
          return end;
        }
        long end64pos = ZIP64_LOCOFF(loc64);
        byte[] end64buf = new byte[ZIP64_ENDHDR];
        if (readFullyAt(ch, end64buf, 0, end64buf.length, end64pos) != end64buf.length) {
          return end;
        }
        // end64 found, re-calculate everything.
        end.sizeCentral = ZIP64_ENDSIZ(end64buf);
        end.ofsCentral = ZIP64_ENDOFF(end64buf);
        end.numEntries = (int)ZIP64_ENDTOT(end64buf); // assume total < 2g
        end.offset = end64pos;
      }
      return end;
    }
    zerror("zip END header not found");
    return null; // make compiler happy
  }
}
