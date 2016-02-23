// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.infinity.resource.StructEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileWriterNI;

public final class TextString extends Datatype implements InlineEditable, IsTextual
{
  private final byte bytes[];
  private String text;

  public TextString(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public TextString(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, offset, length, name);
    bytes = new byte[length];
    read(buffer, offset);
  }

// --------------------- Begin Interface InlineEditable ---------------------

  @Override
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

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (text != null) {
      byte[] buf = text.getBytes(Charset.forName("windows-1252"));
      int len = Math.min(buf.length, bytes.length);
      System.arraycopy(buf, 0, bytes, 0, len);
      if (len < bytes.length) {
        bytes[len] = 0;
      }
    }
    FileWriterNI.writeBytes(os, bytes);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    System.arraycopy(buffer, offset, bytes, 0, getSize());
    text = null;

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

//--------------------- Begin Interface IsTextual ---------------------

  @Override
  public String getText()
  {
    if (text == null) {
      text = DynamicArray.getString(bytes, 0, bytes.length);
    }
    return text;
  }

//--------------------- End Interface IsTextual ---------------------

  @Override
  public String toString()
  {
    return getText();
  }
}

