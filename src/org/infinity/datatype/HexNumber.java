// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import org.infinity.resource.StructEntry;

public class HexNumber extends DecNumber
{
  public HexNumber(byte buffer[], int offset, int length, String desc)
  {
    this(null, buffer, offset, length, desc);
  }

  public HexNumber(StructEntry parent, byte buffer[], int offset, int length, String desc)
  {
    super(parent, buffer, offset, length, desc);
  }

// --------------------- Begin Interface InlineEditable ---------------------

  @Override
  public boolean update(Object value)
  {
    try {
      setValue((int)DecNumber.parseNumber(value, getSize(), true, true));
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface InlineEditable ---------------------

  @Override
  public String toString()
  {
    return Integer.toHexString(getValue()) + " h";
  }
}

