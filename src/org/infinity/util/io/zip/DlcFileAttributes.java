// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Formatter;

/**
 * FileAttributes implementation for DLC archives in zip format.
 */
public class DlcFileAttributes implements BasicFileAttributes
{
  private final DlcFileSystem.Entry e;

  DlcFileAttributes(DlcFileSystem.Entry e)
  {
    this.e = e;
  }

  @Override
  public FileTime lastModifiedTime()
  {
    return FileTime.fromMillis(e.mtime);
  }

  @Override
  public FileTime lastAccessTime()
  {
    if (e.atime != -1) {
      return FileTime.fromMillis(e.atime);
    }
    return null;
  }

  @Override
  public FileTime creationTime()
  {
    if (e.ctime != -1) {
      return FileTime.fromMillis(e.ctime);
    }
    return null;
  }

  @Override
  public boolean isRegularFile()
  {
    return !e.isDir();
  }

  @Override
  public boolean isDirectory()
  {
    return e.isDir();
  }

  @Override
  public boolean isSymbolicLink()
  {
    return false;
  }

  @Override
  public boolean isOther()
  {
    return false;
  }

  @Override
  public long size()
  {
    return e.size;
  }

  @Override
  public Object fileKey()
  {
    return null;
  }


  ///////// zip entry attributes ///////////

  public long compressedSize()
  {
    return e.csize;
  }

  public long crc()
  {
    return e.crc;
  }

  public int method()
  {
    return e.method;
  }

  public byte[] extra()
  {
    if (e.extra != null) {
      return Arrays.copyOf(e.extra, e.extra.length);
    }
    return null;
  }

  public byte[] comment()
  {
    if (e.comment != null) {
      return Arrays.copyOf(e.comment, e.comment.length);
    }
    return null;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder(1024);
    Formatter fm = new Formatter(sb);
    if (creationTime() != null) {
      fm.format("    creationTime    : %tc%n", creationTime().toMillis());
    } else {
      fm.format("    creationTime    : null%n");
    }

    if (lastAccessTime() != null) {
      fm.format("    lastAccessTime  : %tc%n", lastAccessTime().toMillis());
    } else {
      fm.format("    lastAccessTime  : null%n");
    }
    fm.format("    lastModifiedTime: %tc%n", lastModifiedTime().toMillis());
    fm.format("    isRegularFile   : %b%n", isRegularFile());
    fm.format("    isDirectory     : %b%n", isDirectory());
    fm.format("    isSymbolicLink  : %b%n", isSymbolicLink());
    fm.format("    isOther         : %b%n", isOther());
    fm.format("    fileKey         : %s%n", fileKey());
    fm.format("    size            : %d%n", size());
    fm.format("    compressedSize  : %d%n", compressedSize());
    fm.format("    crc             : %x%n", crc());
    fm.format("    method          : %d%n", method());
    fm.close();
    return sb.toString();
  }
}
