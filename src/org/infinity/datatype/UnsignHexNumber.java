// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;

public class UnsignHexNumber extends UnsignDecNumber {
  public UnsignHexNumber(ByteBuffer buffer, int offset, int length, String desc) {
    super(buffer, offset, length, desc);
  }

  // --------------------- Begin Interface InlineEditable ---------------------

  @Override
  public boolean update(Object value) {
    try {
      setValue(UnsignDecNumber.parseNumber(value, getSize(), false, true));
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  // --------------------- End Interface InlineEditable ---------------------

  @Override
  public String toString() {
    return Long.toHexString(getLongValue() & 0xffffffffL) + " h";
  }
}
