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

  public static void writeBytes(OutputStream os, byte buffer[]) throws IOException
  {
    os.write(buffer);
  }

  /**
   * Writes a byte (8 bit) to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeByte(OutputStream out, byte value) throws IOException
  {
    int res = 0;
    if (out != null) {
      out.write(value);
      res++;
    }
    return res;
  }

  /**
   * Writes a short (16 bit) to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeShort(OutputStream out, short value) throws IOException
  {
    int res = 0;
    if (out != null) {
      for (int i = 0, shift = 0; i < 2; i++, shift+=8) {
        out.write((value >>> shift) & 0xff);
        res++;
      }
    }
    return res;
  }

  public static void writeInt(RandomAccessFile ranfile, int b) throws IOException
  {
    ranfile.write(DynamicArray.convertInt(b));
  }

  /**
   * Writes an integer (32 bit) to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeInt(OutputStream out, int value) throws IOException
  {
    int res = 0;
    if (out != null) {
      for (int i = 0, shift = 0; i < 4; i++, shift+=8) {
        out.write((value >>> shift) & 0xff);
        res++;
      }
    }
    return res;
  }

  /**
   * Writes a 24 bit integer to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeInt24(OutputStream out, int value) throws IOException
  {
    int res = 0;
    if (out != null) {
      for (int i = 0, shift = 0; i < 3; i++, shift+=8) {
        out.write((value >>> shift) & 0xff);
        res++;
      }
    }
    return res;
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

  private Filewriter(){}
}

