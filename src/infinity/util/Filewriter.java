// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

public final class Filewriter
{

  public static void writeByte(OutputStream os, byte b) throws IOException
  {
    writeBytes(os, Byteconvert.convertBack(b));
  }

  public static void writeBytes(OutputStream os, byte buffer[]) throws IOException
  {
    os.write(buffer);
  }

// --Recycle Bin START (21.10.03 21:45):
//  public static void writeString(RandomAccessFile ranfile, String s, int length) throws IOException
//  {
//    ranfile.writeChars(s);
//    length -= s.length();
//    if (length > 0) {
//      byte buffer[] = new byte[length];
//      ranfile.writeField(buffer);
//    }
//  }
// --Recycle Bin STOP (21.10.03 21:45)

  public static void writeInt(RandomAccessFile ranfile, int b) throws IOException
  {
    ranfile.write(Byteconvert.convertBack(b));
  }

  public static void writeInt(OutputStream os, int b) throws IOException
  {
    writeBytes(os, Byteconvert.convertBack(b));
  }

  public static void writeShort(OutputStream os, short b) throws IOException
  {
    writeBytes(os, Byteconvert.convertBack(b));
  }

  public static void writeString(OutputStream os, String s, int length) throws IOException
  {
    writeString(os, s, length, Charset.forName("ISO-8859-1")); // For NWN, no other conflicts?
  }

  public static void writeString(OutputStream os, String s, int length, Charset charset) throws IOException
  {
    writeBytes(os, s.getBytes(charset));
    byte buffer[] = new byte[length - s.length()];
    if (buffer.length != 0)
      writeBytes(os, buffer);
  }

// --Recycle Bin START (21.10.03 21:45):
//  public static void writeLong(OutputStream os, long b) throws IOException
//  {
//    writeBytes(os, Byteconvert.convertBack(b));
//  }
// --Recycle Bin STOP (21.10.03 21:45)

  public static void writeUnsignedByte(OutputStream os, int b) throws IOException
  {
    if (b > 128) // Or 127???
      b -= 256;
    writeBytes(os, Byteconvert.convertBack((byte)b));
  }

  public static void writeUnsignedInt(OutputStream os, long b) throws IOException
  {
    if (b > 2147483648L) // Or ???
      b -= 4294967296L;
    writeBytes(os, Byteconvert.convertBack((int)b));
  }

  public static void writeUnsignedShort(OutputStream os, int b) throws IOException
  {
    if (b > 32768) // Or 32767???
      b -= 65536;
    writeBytes(os, Byteconvert.convertBack((short)b));
  }

  public static void writeUnsignedThrees(OutputStream os, long b) throws IOException
  {
    if (b > 8388608) // Or ???
      b -= (long)16777216;
    writeBytes(os, Byteconvert.convertThreeBack((int)b));
  }

  private Filewriter(){}

// --Recycle Bin START (21.10.03 21:45):
//  public static void writeShort(RandomAccessFile ranfile, short b) throws IOException
//  {
//    ranfile.writeField(Byteconvert.convertBack(b));
//  }
// --Recycle Bin STOP (21.10.03 21:45)
}

