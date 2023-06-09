// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information
//
// ----------------------------------------------------------------------------
//
// Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//   - Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//
//   - Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in the
//     documentation and/or other materials provided with the distribution.
//
//   - Neither the name of Oracle nor the names of its
//     contributors may be used to endorse or promote products derived
//     from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.infinity.util.io.zip;

/**
 * Zip constants.
 */
class ZipConstants
{
  /*
   * Compression methods
   */
  public static final int METHOD_STORED     = 0;
  public static final int METHOD_DEFLATED   = 8;
  public static final int METHOD_DEFLATED64 = 9;
  public static final int METHOD_BZIP2      = 12;
  public static final int METHOD_LZMA       = 14;
  public static final int METHOD_LZ77       = 19;
  public static final int METHOD_AES        = 99;

  /*
   * General purpose big flag
   */
  public static final int FLAG_ENCRYPTED  = 0x01;
  public static final int FLAG_DATADESCR  = 0x08;    // crc, size and csize in dd
  public static final int FLAG_EFS        = 0x800;   // If this bit is set the filename and
  // comment fields for this file must be
  // encoded using UTF-8.
  /*
   * Header signatures
   */
  public static final long LOCSIG = 0x04034b50L;   // "PK\003\004"
  public static final long EXTSIG = 0x08074b50L;   // "PK\007\008"
  public static final long CENSIG = 0x02014b50L;   // "PK\001\002"
  public static final long ENDSIG = 0x06054b50L;   // "PK\005\006"

  /*
   * Header sizes in bytes (including signatures)
   */
  public static final int LOCHDR = 30;       // LOC header size
  public static final int EXTHDR = 16;       // EXT header size
  public static final int CENHDR = 46;       // CEN header size
  public static final int ENDHDR = 22;       // END header size

  /*
   * Local file (LOC) header field offsets
   */
  public static final int LOCVER = 4;        // version needed to extract
  public static final int LOCFLG = 6;        // general purpose bit flag
  public static final int LOCHOW = 8;        // compression method
  public static final int LOCTIM = 10;       // modification time
  public static final int LOCCRC = 14;       // uncompressed file crc-32 value
  public static final int LOCSIZ = 18;       // compressed size
  public static final int LOCLEN = 22;       // uncompressed size
  public static final int LOCNAM = 26;       // filename length
  public static final int LOCEXT = 28;       // extra field length

  /*
   * Extra local (EXT) header field offsets
   */
  public static final int EXTCRC = 4;        // uncompressed file crc-32 value
  public static final int EXTSIZ = 8;        // compressed size
  public static final int EXTLEN = 12;       // uncompressed size

  /*
   * Central directory (CEN) header field offsets
   */
  public static final int CENVEM = 4;        // version made by
  public static final int CENVER = 6;        // version needed to extract
  public static final int CENFLG = 8;        // encrypt, decrypt flags
  public static final int CENHOW = 10;       // compression method
  public static final int CENTIM = 12;       // modification time
  public static final int CENCRC = 16;       // uncompressed file crc-32 value
  public static final int CENSIZ = 20;       // compressed size
  public static final int CENLEN = 24;       // uncompressed size
  public static final int CENNAM = 28;       // filename length
  public static final int CENEXT = 30;       // extra field length
  public static final int CENCOM = 32;       // comment length
  public static final int CENDSK = 34;       // disk number start
  public static final int CENATT = 36;       // internal file attributes
  public static final int CENATX = 38;       // external file attributes
  public static final int CENOFF = 42;       // LOC header offset

  /*
   * End of central directory (END) header field offsets
   */
  public static final int ENDSUB = 8;        // number of entries on this disk
  public static final int ENDTOT = 10;       // total number of entries
  public static final int ENDSIZ = 12;       // central directory size in bytes
  public static final int ENDOFF = 16;       // offset of first CEN header
  public static final int ENDCOM = 20;       // zip file comment length

  /*
   * ZIP64 constants
   */
  public static final long ZIP64_ENDSIG = 0x06064b50L;  // "PK\006\006"
  public static final long ZIP64_LOCSIG = 0x07064b50L;  // "PK\006\007"
  public static final int  ZIP64_ENDHDR = 56;           // ZIP64 end header size
  public static final int  ZIP64_LOCHDR = 20;           // ZIP64 end loc header size
  public static final int  ZIP64_EXTHDR = 24;           // EXT header size
  public static final int  ZIP64_EXTID  = 0x0001;       // Extra field Zip64 header ID

  public static final int  ZIP64_MINVAL32 = 0xFFFF;
  public static final long ZIP64_MINVAL = 0xFFFFFFFFL;

  /*
   * Zip64 End of central directory (END) header field offsets
   */
  public static final int  ZIP64_ENDLEN = 4;       // size of zip64 end of central dir
  public static final int  ZIP64_ENDVEM = 12;      // version made by
  public static final int  ZIP64_ENDVER = 14;      // version needed to extract
  public static final int  ZIP64_ENDNMD = 16;      // number of this disk
  public static final int  ZIP64_ENDDSK = 20;      // disk number of start
  public static final int  ZIP64_ENDTOD = 24;      // total number of entries on this disk
  public static final int  ZIP64_ENDTOT = 32;      // total number of entries
  public static final int  ZIP64_ENDSIZ = 40;      // central directory size in bytes
  public static final int  ZIP64_ENDOFF = 48;      // offset of first CEN header
  public static final int  ZIP64_ENDEXT = 56;      // zip64 extensible data sector

  /*
   * Zip64 End of central directory locator field offsets
   */
  public static final int  ZIP64_LOCDSK = 4;       // disk number start
  public static final int  ZIP64_LOCOFF = 8;       // offset of zip64 end
  public static final int  ZIP64_LOCTOT = 16;      // total number of disks

  /*
   * Zip64 Extra local (EXT) header field offsets
   */
  public static final int  ZIP64_EXTCRC = 4;       // uncompressed file crc-32 value
  public static final int  ZIP64_EXTSIZ = 8;       // compressed size, 8-byte
  public static final int  ZIP64_EXTLEN = 16;      // uncompressed size, 8-byte

  /*
   * Extra field header ID
   */
  public static final int  EXTID_ZIP64 = 0x0001;      // ZIP64
  public static final int  EXTID_NTFS  = 0x000a;      // NTFS
  public static final int  EXTID_UNIX  = 0x000d;      // UNIX
  public static final int  EXTID_EFS   = 0x0017;      // Strong Encryption
  public static final int  EXTID_EXTT  = 0x5455;      // Info-ZIP Extended Timestamp

  /*
   * fields access methods
   */
  ///////////////////////////////////////////////////////
  public static final int  CH(byte[] b, int n) { return Byte.toUnsignedInt(b[n]); }
  public static final int  SH(byte[] b, int n) { return Byte.toUnsignedInt(b[n]) | (Byte.toUnsignedInt(b[n + 1]) << 8); }
  public static final long LG(byte[] b, int n) { return ((SH(b, n)) | (SH(b, n + 2) << 16)) & 0xffffffffL; }
  public static final long LL(byte[] b, int n) { return (LG(b, n)) | (LG(b, n + 4) << 32); }

  public static final long GETSIG(byte[] b) { return LG(b, 0); }

  // local file (LOC) header fields
  public static final long LOCSIG(byte[] b) { return LG(b, 0); } // signature
  public static final int  LOCVER(byte[] b) { return SH(b, 4); } // version needed to extract
  public static final int  LOCFLG(byte[] b) { return SH(b, 6); } // general purpose bit flags
  public static final int  LOCHOW(byte[] b) { return SH(b, 8); } // compression method
  public static final long LOCTIM(byte[] b) { return LG(b, 10);} // modification time
  public static final long LOCCRC(byte[] b) { return LG(b, 14);} // crc of uncompressed data
  public static final long LOCSIZ(byte[] b) { return LG(b, 18);} // compressed data size
  public static final long LOCLEN(byte[] b) { return LG(b, 22);} // uncompressed data size
  public static final int  LOCNAM(byte[] b) { return SH(b, 26);} // filename length
  public static final int  LOCEXT(byte[] b) { return SH(b, 28);} // extra field length

  // extra local (EXT) header fields
  public static final long EXTCRC(byte[] b) { return LG(b, 4);}  // crc of uncompressed data
  public static final long EXTSIZ(byte[] b) { return LG(b, 8);}  // compressed size
  public static final long EXTLEN(byte[] b) { return LG(b, 12);} // uncompressed size

  // end of central directory header (END) fields
  public static final int  ENDSUB(byte[] b) { return SH(b, 8); }  // number of entries on this disk
  public static final int  ENDTOT(byte[] b) { return SH(b, 10);}  // total number of entries
  public static final long ENDSIZ(byte[] b) { return LG(b, 12);}  // central directory size
  public static final long ENDOFF(byte[] b) { return LG(b, 16);}  // central directory offset
  public static final int  ENDCOM(byte[] b) { return SH(b, 20);}  // size of zip file comment
  public static final int  ENDCOM(byte[] b, int off) { return SH(b, off + 20);}

  // zip64 end of central directory recoder fields
  public static final long ZIP64_ENDTOD(byte[] b) { return LL(b, 24);}  // total number of entries on disk
  public static final long ZIP64_ENDTOT(byte[] b) { return LL(b, 32);}  // total number of entries
  public static final long ZIP64_ENDSIZ(byte[] b) { return LL(b, 40);}  // central directory size
  public static final long ZIP64_ENDOFF(byte[] b) { return LL(b, 48);}  // central directory offset
  public static final long ZIP64_LOCOFF(byte[] b) { return LL(b, 8);}   // zip64 end offset

  // central directory header (CEN) fields
  public static final long CENSIG(byte[] b, int pos) { return LG(b, pos + 0); }
  public static final int  CENVEM(byte[] b, int pos) { return SH(b, pos + 4); }
  public static final int  CENVER(byte[] b, int pos) { return SH(b, pos + 6); }
  public static final int  CENFLG(byte[] b, int pos) { return SH(b, pos + 8); }
  public static final int  CENHOW(byte[] b, int pos) { return SH(b, pos + 10);}
  public static final long CENTIM(byte[] b, int pos) { return LG(b, pos + 12);}
  public static final long CENCRC(byte[] b, int pos) { return LG(b, pos + 16);}
  public static final long CENSIZ(byte[] b, int pos) { return LG(b, pos + 20);}
  public static final long CENLEN(byte[] b, int pos) { return LG(b, pos + 24);}
  public static final int  CENNAM(byte[] b, int pos) { return SH(b, pos + 28);}
  public static final int  CENEXT(byte[] b, int pos) { return SH(b, pos + 30);}
  public static final int  CENCOM(byte[] b, int pos) { return SH(b, pos + 32);}
  public static final int  CENDSK(byte[] b, int pos) { return SH(b, pos + 34);}
  public static final int  CENATT(byte[] b, int pos) { return SH(b, pos + 36);}
  public static final long CENATX(byte[] b, int pos) { return LG(b, pos + 38);}
  public static final long CENOFF(byte[] b, int pos) { return LG(b, pos + 42);}

  /* The END header is followed by a variable length comment of size < 64k. */
  public static final long END_MAXLEN = 0xFFFF + ENDHDR;
  public static final int READBLOCKSZ = 128;
}
