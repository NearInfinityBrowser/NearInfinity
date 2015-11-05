// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;
import infinity.util.DynamicArray;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class DecNumber extends Datatype implements InlineEditable
{
  private int number;

  public DecNumber(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public DecNumber(StructEntry parent, byte buffer[], int offset, int length, String name)
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
      number = (int)parseNumber(value, getSize(), true, true);
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
    super.writeInt(os, number);
  }

// --------------------- End Interface Writeable ---------------------

// --------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 1:
        number = (int)DynamicArray.getByte(buffer, offset);
        break;
      case 2:
        number = (int)DynamicArray.getShort(buffer, offset);
        break;
      case 4:
        number = DynamicArray.getInt(buffer, offset);
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }

// --------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    return Integer.toString(number);
  }

  public int getValue()
  {
    return number;
  }

  public void incValue(int value)
  {
    number += value;
  }

  public void setValue(int value)
  {
    number = value;
  }

  /** Attempts to parse the specified string into a decimal or, optionally, hexadecimal number. */
  static long parseNumber(Object value, int size, boolean negativeAllowed, boolean hexAllowed) throws Exception
  {
    if (value == null) {
      throw new NullPointerException();
    }
    String s = value.toString().trim().toLowerCase(Locale.ENGLISH);
    int radix = 10;
    if (hexAllowed && s.startsWith("0x")) {
      s = s.substring(2);
      radix = 16;
    } else if (hexAllowed && s.endsWith("h")) {
      s = s.substring(0, s.length() - 1).trim();
      radix = 16;
    }
    long newNumber = Long.parseLong(s, radix);
    long discard = negativeAllowed ? 1L : 0L;
    long maxNum = (1L << ((long)size*8L - discard)) - 1L;
    long minNum = negativeAllowed ? -(maxNum+1L) : 0;
    if (newNumber > maxNum || newNumber < minNum) {
      throw new NumberFormatException("Number out of range: " + newNumber);
    }
    return newNumber;
  }
}

