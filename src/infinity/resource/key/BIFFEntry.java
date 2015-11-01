// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.resource.ResourceFactory;
import infinity.resource.Writeable;
import infinity.util.DynamicArray;
import infinity.util.io.FileWriterNI;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public final class BIFFEntry implements Writeable, Comparable<BIFFEntry>
{
  private final short location;
  private String filename;
  private int index, filelength, stringoffset;
  private short stringlength;

  public BIFFEntry(String filename)
  {
    this.filename = filename;
    // Location: Indicates where file might be found
    // Bit 1: Data or movies        (LSB)
    // Bit 2: ???
    // Bit 3: ??? (CD1-directory?)
    // Bit 4: CD2-directory
    // Bit 5: CD3-directory
    // Bit 6: CD4-directory
    // Bit 7: CD5-directory
    // Bit 8: ??? (Doesn't exist?) (MSB)
    location = (short)1; // Data or movies
    index = -1; // Not put into keyfile yet
  }

  BIFFEntry(int index, byte buffer[], int offset, boolean usesShortFormat)
  {
    this.index = index;
    stringoffset = DynamicArray.getInt(buffer, offset);
    stringlength = DynamicArray.getShort(buffer, offset + 4);
    location = DynamicArray.getShort(buffer, offset + 6);
    filename = new String(buffer, stringoffset, (int)stringlength - 1);
    if (filename.startsWith("\\"))
      filename = filename.substring(1);
    filename = filename.replace('\\', '/').replace(':', '/');
  }

  BIFFEntry(int index, byte buffer[], int offset)
  {
    this.index = index;
    filelength = DynamicArray.getInt(buffer, offset);
    stringoffset = DynamicArray.getInt(buffer, offset + 4);
    stringlength = DynamicArray.getShort(buffer, offset + 8);
    location = DynamicArray.getShort(buffer, offset + 10);
    filename = new String(buffer, stringoffset, (int)stringlength - 1);
    if (filename.startsWith("\\"))
      filename = filename.substring(1);
    filename = filename.replace('\\', '/').replace(':', '/');
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(BIFFEntry o)
  {
    return filename.compareTo(o.filename);
  }

// --------------------- End Interface Comparable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    FileWriterNI.writeInt(os, filelength);
    FileWriterNI.writeInt(os, stringoffset);
    FileWriterNI.writeShort(os, stringlength);
    FileWriterNI.writeShort(os, location);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    } else if (!(o instanceof BIFFEntry)) {
      return false;
    } else {
      BIFFEntry other = (BIFFEntry)o;
      return filelength == other.filelength && stringoffset == other.stringoffset &&
             stringlength == other.stringlength &&
             location == other.location &&
             filename.equals(other.filename);
    }
  }

  @Override
  public String toString()
  {
    return filename;
  }

  public File getFile()
  {
    File file = ResourceFactory.getFile(filename);
    if (file == null) {
      String bifFilename = filename.substring(0, filename.lastIndexOf((int)'.')) + ".cbf";
      file = ResourceFactory.getFile(bifFilename); // Icewind Dale
    }
    return file;
  }

  public int getIndex()
  {
    return index;
  }

  void setIndex(int index)
  {
    this.index = index;
  }

  public void setFileLength(int filelength)
  {
    this.filelength = filelength;
  }

  public int updateOffset(int newoffset)
  {
    stringoffset = newoffset;
    stringlength = (short)(filename.length() + 1);
    return (int)stringlength;
  }

  public void writeString(OutputStream os) throws IOException
  {
    FileWriterNI.writeString(os, filename, (int)stringlength);
  }
}

