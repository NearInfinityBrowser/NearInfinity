// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.infinity.resource.StructEntry;
import org.infinity.util.io.FileWriterNI;


public class FloatNumber extends Datatype implements InlineEditable
{
  private double value;

  public FloatNumber(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public FloatNumber(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, offset, length, name);
    value = 0.0;
    read(buffer, offset);
  }

//--------------------- Begin Interface InlineEditable ---------------------

 @Override
 public boolean update(Object value)
 {
   try {
     double newValue = Double.parseDouble(value.toString());
     if (getSize() == 4) {
       newValue = Double.valueOf(newValue).floatValue();
     }
     this.value = newValue;
     return true;
   } catch (NumberFormatException e) {
     e.printStackTrace();
   }
   return false;
 }

//--------------------- End Interface InlineEditable ---------------------

//--------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    FileWriterNI.writeBytes(os, toByteArray(value, getSize()));
  }

//--------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 4:
      case 8:
        value = toFloatNumber(buffer, offset, getSize());
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
    return Double.toString(value);
  }

  public double getValue()
  {
    return value;
  }


  // Converts byte array of specified length into a double value
  private static double toFloatNumber(byte[] buffer, int offset, int length)
  {
    if (length == 4 || length == 8) {
      byte[] tmp = new byte[length];
      System.arraycopy(buffer, offset, tmp, 0, length);
      reverseByteOrder(tmp, 0, length);
      switch (length) {
        case 4:
          return ByteBuffer.wrap(tmp).getFloat();
        case 8:
          return ByteBuffer.wrap(tmp).getDouble();
      }
    }
    return 0.0;
  }

  // Converts double value into byte array of specified length
  private static byte[] toByteArray(double value, int length)
  {
    if (length == 4 || length == 8) {
      byte[] buffer = new byte[length];
      switch (length) {
        case 4:
          ByteBuffer.wrap(buffer).putFloat((float)value);
          break;
        case 8:
          ByteBuffer.wrap(buffer).putDouble(value);
          break;
      }
      reverseByteOrder(buffer, 0, length);
      return buffer;
    }
    return null;
  }

  // Toggles between big endian <-> little endian order of bytes
  private static boolean reverseByteOrder(byte[] buffer, int offset, int length)
  {
    if (buffer != null && (length == 4 || length == 8)) {
      for (int i = offset, j = offset + length - 1; i < j; i++, j--) {
        byte tmp = buffer[i];
        buffer[i] = buffer[j];
        buffer[j] = tmp;
      }
      return true;
    }
    return false;
  }
}
