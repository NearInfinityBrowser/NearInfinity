// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;
import infinity.util.Filewriter;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Datatype implements StructEntry
{
  protected static final Dimension DIM_BROAD = new Dimension(650, 100);
  protected static final Dimension DIM_MEDIUM = new Dimension(400, 100);
  private final int length;
  private String name;
  private int offset;

  protected Datatype(int offset, int length, String name)
  {
    this.offset = offset;
    this.length = length;
    this.name = name;
  }

// --------------------- Begin Interface Comparable ---------------------

  public int compareTo(StructEntry o)
  {
    return offset - o.getOffset();
  }

// --------------------- End Interface Comparable ---------------------


// --------------------- Begin Interface StructEntry ---------------------

  public Object clone() throws CloneNotSupportedException
  {
    return super.clone();
  }

  public void copyNameAndOffset(StructEntry entry)
  {
    name = entry.getName();
    offset = entry.getOffset();
  }

  public String getName()
  {
    return name;
  }

  public int getOffset()
  {
    return offset;
  }

  public int getSize()
  {
    return length;
  }

  public void setOffset(int newoffset)
  {
    offset = newoffset;
  }

// --------------------- End Interface StructEntry ---------------------

  void writeInt(OutputStream os, int value) throws IOException
  {
    if (getSize() == 4)
      Filewriter.writeInt(os, value);
    else if (getSize() == 3)
      Filewriter.writeUnsignedThrees(os, (long)value);
    else if (getSize() == 2)
      Filewriter.writeShort(os, (short)value);
    else if (getSize() == 1)
      Filewriter.writeByte(os, (byte)value);
    else
      throw new IllegalArgumentException();
  }

  void writeLong(OutputStream os, long value) throws IOException
  {
    if (getSize() == 4)
      Filewriter.writeUnsignedInt(os, value);
    else if (getSize() == 3)
      Filewriter.writeUnsignedThrees(os, value);
    else if (getSize() == 2)
      Filewriter.writeUnsignedShort(os, (int)value);
    else if (getSize() == 1)
      Filewriter.writeUnsignedByte(os, (int)value);
    else
      throw new IllegalArgumentException();
  }
}

