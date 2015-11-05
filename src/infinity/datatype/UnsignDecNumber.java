// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;
import infinity.util.DynamicArray;

import java.io.IOException;
import java.io.OutputStream;

public final class UnsignDecNumber extends Datatype implements InlineEditable
{
  private long number;

  public UnsignDecNumber(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public UnsignDecNumber(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, offset, length, name);
    number = 0;
    read(buffer, offset);
  }

// --------------------- Begin Interface InlineEditable ---------------------

  @Override
  public boolean update(Object value)
  {
    try {
      number = DecNumber.parseNumber(value, getSize(), false, true);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface InlineEditable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, number);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 1:
        number = (long)DynamicArray.getUnsignedByte(buffer, offset);
        break;
      case 2:
        number = (long)DynamicArray.getUnsignedShort(buffer, offset);
        break;
      case 4:
        number = DynamicArray.getUnsignedInt(buffer, offset);
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    return Long.toString(number);
  }

  public long getValue()
  {
    return number;
  }
}

