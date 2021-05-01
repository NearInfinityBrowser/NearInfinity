// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

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
  private final ZipNode folder;

  DlcFileAttributes(ZipNode folder)
  {
    this.folder = folder;
  }

  @Override
  public FileTime lastModifiedTime()
  {
    return FileTime.fromMillis(folder.getCentral().mtime);
  }

  @Override
  public FileTime lastAccessTime()
  {
    return FileTime.fromMillis(folder.getCentral().atime);
  }

  @Override
  public FileTime creationTime()
  {
    return FileTime.fromMillis(folder.getCentral().ctime);
  }

  @Override
  public boolean isRegularFile()
  {
    return !folder.isDirectory();
  }

  @Override
  public boolean isDirectory()
  {
    return folder.isDirectory();
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
    return folder.getCentral().sizeUncompressed;
  }

  @Override
  public Object fileKey()
  {
    return folder;
  }

  // --------------- zip specific attributes ---------------

  public long compressedSize()
  {
    return folder.getCentral().sizeCompressed;
  }

  public long crc()
  {
    return folder.getCentral().crc32;
  }

  public int method()
  {
    return folder.getCentral().compression;
  }

  public byte[] extra()
  {
    byte[] data = folder.getCentral().extra;
    if (data.length > 0) {
      return Arrays.copyOf(data, data.length);
    }
    return null;
  }

  public byte[] comment()
  {
    byte[] data = folder.getCentral().comment;
    if (data.length > 0) {
      return Arrays.copyOf(data, data.length);
    }
    return null;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(1024);
    try (Formatter fm = new Formatter(sb)) {
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
    }
    return sb.toString();
  }
}
