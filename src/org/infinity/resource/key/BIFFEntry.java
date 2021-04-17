// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.infinity.resource.Writeable;
import org.infinity.resource.Profile;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

/**
 * Provides information about the location of resource data within BIFF archives.
 */
public class BIFFEntry implements Writeable, Comparable<BIFFEntry>
{
  // Location: Indicates where file might be found
  // Bit 0: Root folder (where the KEY file is located)
  // Bit 1: Cache directory
  // Bit 2: ??? (CD1 directory?)
  // Bit 3: CD2 directory
  // Bit 4: CD3 directory
  // Bit 5: CD4 directory
  // Bit 6: CD5 directory
  // Bit 7: CD6 directory
  // Bit 8: ??? (CD7 directory?)
  private int location;     // supposed location of BIFF file
  private Path keyFile;     // Full path to KEY file containing BIFF entry
  private Path biffFile;    // Full path to BIFF file if available
  private String fileName;  // Raw path to BIFF file as defined in KEY file
  private int index;        // BIFF entry index in KEY file
  private int fileSize;     // Resource size in bytes
  private int stringOffset; // Offset to BIFF filename in KEY file
  private char separatorChar; // path separator used to assemble BIFF path

  /**
   * Constructs a new BIFF entry.
   * @param keyFile The associated key file.
   * @param fileName Name and relative path to the BIFF file
   */
  public BIFFEntry(Path keyFile, String fileName)
  {
    if (fileName == null) {
      throw new NullPointerException();
    }
    this.separatorChar = '/';
    this.fileName = fileName.replaceAll("[\\:]", "/"); // (normalized) relative path
    this.location = 1;        // put into root folder
    this.index = -1;          // not yet associated with KEY file
  }

  public BIFFEntry(Path keyFile, int index, ByteBuffer buffer, int offset)
  {
    updateBIFF(keyFile, index, buffer, offset);
  }

//--------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(BIFFEntry o)
  {
    return fileName.compareTo(o.fileName);
  }

//--------------------- End Interface Comparable ---------------------


//--------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    StreamUtils.writeInt(os, fileSize);
    StreamUtils.writeInt(os, stringOffset);
    StreamUtils.writeShort(os, getFileNameLength());
    StreamUtils.writeShort(os, (short)location);
  }

//--------------------- End Interface Writeable ---------------------

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + location;
    hash = 31 * hash + ((keyFile == null) ? 0 : keyFile.hashCode());
    hash = 31 * hash + ((biffFile == null) ? 0 : biffFile.hashCode());
    hash = 31 * hash + ((fileName == null) ? 0 : fileName.hashCode());
    hash = 31 * hash + index;
    hash = 31 * hash + fileSize;
    hash = 31 * hash + stringOffset;
    hash = 31 * hash + (int)separatorChar;
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }

    if (!(o instanceof BIFFEntry)) {
      return false;
    }

    BIFFEntry other = (BIFFEntry)o;
    boolean retVal = (location == other.location);
    retVal &= (keyFile == null && other.keyFile == null) ||
              (keyFile != null && keyFile.equals(other.keyFile));
    retVal &= (biffFile == null && other.biffFile == null) ||
              (biffFile != null && biffFile.equals(other.biffFile));
    retVal &= (fileName == null && other.fileName == null) ||
              (fileName != null && fileName.equals(other.fileName));
    retVal &= (index == other.index);
    retVal &= (fileSize == other.fileSize);
    retVal &= (stringOffset == other.stringOffset);
    retVal &= (separatorChar == other.separatorChar);
    return retVal;
  }

  @Override
  public String toString()
  {
    return fileName;
  }

  /** Returns the KEY file containing this BIFF archive. */
  public Path getKeyFile()
  {
    return keyFile;
  }

  /** Returns the relative file path to the BIFF file. */
  public String getFileName()
  {
    return fileName;
  }

  /** Returns whether the referenced BIFF file exists in the game. */
  public boolean exists()
  {
    return (biffFile != null && FileEx.create(biffFile).isFile());
  }

  /** Returns the absolute path to the BIFF file if it exists. */
  public Path getPath()
  {
    return biffFile;
  }

  public int getIndex()
  {
    return index;
  }

  void setIndex(int newIndex)
  {
    this.index = newIndex;
  }

  /**
   * Returns the relative BIFF file path as found in the KEY file.
   * @param normalized Specify {@code true} to return the filename with default path separator
   *                   {@code '/'} or {@code false} to return the filename with the original
   *                   path separators.
   */
  public String getFileName(boolean normalized)
  {
    if (normalized || separatorChar == '/') {
      return fileName;
    } else {
      return fileName.replace('/', separatorChar);
    }
  }

  public short getFileNameLength()
  {
    return (short)(fileName.length() + 1);
  }

  public int getFileSize()
  {
    return fileSize;
  }

  public void setFileSize(int fileSize)
  {
    this.fileSize = fileSize;
  }

  /**
   * Replaces the current data by the specified data.
   * @param keyFile KEY file containing this BIFF archive.
   * @param index BIFF entry index in KEY file.
   * @param buffer Buffered KEY file.
   * @param offset Start offset of BIFF entry data in KEY file.
   */
  public void updateBIFF(Path keyFile, int index, ByteBuffer buffer, int offset)
  {
    if (keyFile == null || buffer == null) {
      throw new NullPointerException();
    }
    this.keyFile = keyFile.toAbsolutePath();
    this.index = index;
    this.fileSize = buffer.getInt(offset);
    this.stringOffset = buffer.getInt(offset + 4);
    short stringLength = buffer.getShort(offset + 8);
    this.location = buffer.getShort(offset + 10) & 0xffff;
    this.fileName = StreamUtils.readString(buffer, this.stringOffset, stringLength - 1);
    if (this.fileName.charAt(0) == '\\') {
      this.fileName = this.fileName.substring(1);
    }
    if (this.fileName.indexOf('\\') > 0) {
      this.separatorChar = '\\';
    } else if (this.fileName.indexOf(':') > 0) {
      this.separatorChar = ':';
    } else {
      this.separatorChar = '/';
    }
    this.fileName = this.fileName.replace(this.separatorChar, '/');
    this.biffFile = findBiffFile(this.keyFile.getParent(), this.location, this.fileName);
  }


  public int updateOffset(int newOffset)
  {
    this.stringOffset = newOffset;
    return getFileNameLength();
  }

  public void writeString(OutputStream os) throws IOException
  {
    StreamUtils.writeString(os, getFileName(false), getFileNameLength());
  }

  // Searches for the specified BIFF file based on root
  private static Path findBiffFile(Path root, int location, String fileName)
  {
    Path retVal = null;
    if (root != null && fileName != null) {
      List<Path> biffFolders = new ArrayList<>(Profile.getProperty(Profile.Key.GET_GAME_BIFF_FOLDERS));
      if (Profile.isEnhancedEdition()) {
        // remove non-matching biff folder paths
        for (int idx = biffFolders.size() - 1; idx >= 0; idx--) {
          try {
            if (!biffFolders.get(idx).startsWith(root)) {
              biffFolders.remove(idx);
            }
          } catch (Throwable t) {
            biffFolders.remove(idx);
          }
        }
      }
      if (biffFolders.isEmpty()) {
        final String[] baseFolders = { "", "cache", "cd1", "cd2", "cd3", "cd4", "cd5", "cd6", "cd7", "cdall" };
        for (final String folderName: baseFolders) {
          Path path = FileManager.resolve(root.resolve(folderName));
          if (FileEx.create(path).isDirectory()) {
            biffFolders.add(path);
          }
        }
      }
      // Note: BIFF file may have extension ".cbf"
      String[] fileNames = { fileName, StreamUtils.replaceFileExtension(fileName, "cbf") };
      for (final Path path: biffFolders) {
        for (final String biffName: fileNames) {
          retVal = FileManager.queryExisting(path, biffName);
          if (retVal != null) {
            break;
          }
        }
        if (retVal != null) {
          break;
        }
      }
    }
    return retVal;
  }
}
