// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

public final class UnknownDecimal extends Unknown
{
  public UnknownDecimal(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public UnknownDecimal(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    String value = textArea.getText().trim().replace('\n', ' ').replace('\r', ' ') + ' ';
    byte newdata[] = new byte[data.length];
    int counter = 0;
    try {
      int index = value.indexOf((int)' ');
      while (counter < newdata.length && index != -1) {
        int i = Integer.parseInt(value.substring(0, index));
        if (i > 255)
          return false;
        newdata[counter] = (byte)i;
        counter++;
        value = value.substring(index + 1).trim() + ' ';
        index = value.indexOf((int)' ');
      }
      if (counter == newdata.length) {
        data = newdata;
        return true;
      }
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface Editable ---------------------

  @Override
  public String toString()
  {
    if (data != null && data.length > 0) {
      StringBuffer sb = new StringBuffer(4 * data.length + 1);
      for (final byte d : data) {
        String text = Integer.toString((int)d & 0xff);
        for (int j = 0, count = 3 - text.length(); j < count; j++) {
          sb.append('0');
        }
        sb.append(text).append(' ');
      }
      sb.append('d');
      return sb.toString();
    } else
      return new String();
  }
}

