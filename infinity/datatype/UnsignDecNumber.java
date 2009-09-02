// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.util.Byteconvert;

import java.io.IOException;
import java.io.OutputStream;

public final class UnsignDecNumber extends Datatype implements InlineEditable
{
  private long number;

  public UnsignDecNumber(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    number = (long)0;
    if (length == 4)
      number = Byteconvert.convertUnsignedInt(buffer, offset);
    else if (length == 2)
      number = (long)Byteconvert.convertUnsignedShort(buffer, offset);
    else if (length == 1)
      number = (long)Byteconvert.convertUnsignedByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

// --------------------- Begin Interface InlineEditable ---------------------

  public boolean update(Object value)
  {
    try {
      long newnumber = Long.parseLong(value.toString());
      if (newnumber > Math.pow((double)2, (double)(8 * getSize())))
        return false;
      number = newnumber;
      return true;
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface InlineEditable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, number);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    return Long.toString(number);
  }

  public long getValue()
  {
    return number;
  }
}

