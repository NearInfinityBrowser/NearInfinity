// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.AbstractStruct;
import infinity.util.Byteconvert;

public final class UnknownDecimal extends Unknown
{
  public UnknownDecimal(byte[] buffer, int offset, int length, String name)
  {
    super(buffer, offset, length, name);
  }

// --------------------- Begin Interface Editable ---------------------

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

  public String toString()
  {
    StringBuffer sb = new StringBuffer(4 * data.length);
    for (int i = 0; i < data.length; i++) {
      String text = String.valueOf((int)Byteconvert.convertUnsignedByte(data, i));
      for (int j = 0; j < 3 - text.length(); j++)
        sb.append('0');
      if (text.length() > 3)
        text = text.substring(text.length() - 3);
      sb.append(text).append(' ');
    }
    sb.append(' ');
    return sb.toString();
  }
}

