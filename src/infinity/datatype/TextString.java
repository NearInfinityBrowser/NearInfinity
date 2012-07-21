// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.util.*;

import java.io.IOException;
import java.io.OutputStream;

public final class TextString extends Datatype implements InlineEditable
{
  private final byte bytes[];
  private String text;

  public TextString(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    bytes = ArrayUtil.getSubArray(buffer, offset, length);
  }

// --------------------- Begin Interface InlineEditable ---------------------

  public boolean update(Object value)
  {
    String newstring = (String)value;
    if (newstring.length() > getSize())
      return false;
    text = newstring;
    return true;
  }

// --------------------- End Interface InlineEditable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    if (text == null)
      Filewriter.writeBytes(os, bytes);
    else
      Filewriter.writeString(os, text, getSize());
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    if (text == null)
      text = Byteconvert.convertString(bytes, 0, bytes.length);
    return text;
  }
}

