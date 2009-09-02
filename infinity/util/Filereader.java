// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.io.*;
import java.nio.charset.Charset;

public final class Filereader
{
  private static final byte[] buffer4 = new byte[4];
  private static final byte[] buffer2 = new byte[2];

  public static void readBytes(InputStream is, byte buffer[]) throws IOException
  {
    int bytesread = 0;
    while (bytesread < buffer.length) {
      int newread = is.read(buffer, bytesread, buffer.length - bytesread);
      if (newread == -1)
        throw new IOException("Unable to read " + buffer.length + " bytes");
      bytesread += newread;
    }
  }

  public static byte[] readBytes(InputStream is, int length) throws IOException
  {
    byte buffer[] = new byte[length];
    int bytesread = 0;
    while (bytesread < length) {
      int newread = is.read(buffer, bytesread, length - bytesread);
      if (newread == -1)
        throw new IOException("Unable to read " + length + " bytes");
      bytesread += newread;
    }
    return buffer;
  }

  public static void readBytes(InputStream is, byte buffer[], int offset, int length) throws IOException
  {
    int bytesread = 0;
    while (bytesread < length) {
      int newread = is.read(buffer, offset + bytesread, length - bytesread);
      if (newread == -1)
        throw new IOException("Unable to read " + buffer.length + " bytes");
      bytesread += newread;
    }
  }

  public static int readInt(RandomAccessFile ranfile) throws IOException
  {
    ranfile.readFully(buffer4);
    return Byteconvert.convertInt(buffer4, 0);
  }

  public static int readInt(InputStream is) throws IOException
  {
    return Byteconvert.convertInt(readBytes(is, 4), 0);
  }

// --Recycle Bin START (21.10.03 21:45):
//  public static long readUnsignedInt(RandomAccessFile ranfile) throws IOException
//  {
//    ranfile.readFully(buffer4);
//    long value = (long)Byteconvert.convertInt(buffer4, 0);
//    if (value < 0)
//      value += 4294967296L;
//    return value;
//  }
// --Recycle Bin STOP (21.10.03 21:45)

  public static short readShort(RandomAccessFile ranfile) throws IOException
  {
    ranfile.readFully(buffer2);
    return Byteconvert.convertShort(buffer2, 0);
  }

// --Recycle Bin START (21.10.03 21:45):
//  public static byte readByte(InputStream is) throws IOException
//  {
//    return Byteconvert.convertByte(readBytes(is, 1), 0);
//  }
// --Recycle Bin STOP (21.10.03 21:45)

  public static short readShort(InputStream is) throws IOException
  {
    return Byteconvert.convertShort(readBytes(is, 2), 0);
  }

// --Recycle Bin START (21.10.03 21:45):
//  public static long readLong(InputStream is) throws IOException
//  {
//    return Byteconvert.convertLong(readBytes(is, 8), 0);
//  }
// --Recycle Bin STOP (21.10.03 21:45)

// --Recycle Bin START (21.10.03 21:45):
//  public static int readUnsignedByte(InputStream is) throws IOException
//  {
//    int value = (int)Byteconvert.convertByte(readBytes(is, 1), 0);
//    if (value < 0)
//      value += 256;
//    return value;
//  }
// --Recycle Bin STOP (21.10.03 21:45)

// --Recycle Bin START (21.10.03 21:45):
//  public static int readUnsignedShort(InputStream is) throws IOException
//  {
//    int value = (int)Byteconvert.convertShort(readBytes(is, 2), 0);
//    if (value < 0)
//      value += 65536;
//    return value;
//  }
// --Recycle Bin STOP (21.10.03 21:45)

// --Recycle Bin START (21.10.03 21:45):
//  public static long readUnsignedInt(InputStream is) throws IOException
//  {
//    long value = (long)Byteconvert.convertInt(readBytes(is, 4), 0);
//    if (value < 0)
//      value += 4294967296L;
//    return value;
//  }
// --Recycle Bin STOP (21.10.03 21:45)

  public static String readString(RandomAccessFile ranfile, int length) throws IOException
  {
    return readString(ranfile, length, Charset.forName("ISO-8859-1"));
  }

  public static String readString(RandomAccessFile ranfile, int length, Charset charset) throws IOException
  {
    byte buffer[] = new byte[length];
    ranfile.readFully(buffer);
    return new String(buffer, charset);
  }

  public static String readString(InputStream is, int length) throws IOException
  {
    byte buffer[] = readBytes(is, length);
    return Byteconvert.convertString(buffer, 0, length);
  }

  private Filereader(){}
}

