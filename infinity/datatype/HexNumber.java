// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

public class HexNumber extends DecNumber
{
  public HexNumber(byte buffer[], int offset, int length, String desc)
  {
    super(buffer, offset, length, desc);
  }

// --------------------- Begin Interface InlineEditable ---------------------

  public boolean update(Object value)
  {
    String string = (String)value;
    int i = string.indexOf("h");
    if (i != -1)
      string = string.substring(0, i);
    string = string.trim();
    if (string.length() > 8)
      return false;
    try {
      int newnumber = (int)Long.parseLong(string, 16);
      setValue(newnumber);
      return true;
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface InlineEditable ---------------------

  public String toString()
  {
    return Integer.toHexString(getValue()) + " h";
  }
}

