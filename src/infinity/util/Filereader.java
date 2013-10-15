// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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

  /**
   * Reads a byte (8 bit) from specified input stream.
   * @param in The input stream to read from.
   * @return The byte value from the stream.
   */
  public static byte readByte(InputStream in) throws IOException
  {
    byte res = 0;
    if (in != null) {
      int n = in.read();
      if (n < 0)
        return res;
      res = (byte)n;
    }
    return res;
  }

  /**
   * Reads an unsigned byte (8 bit) from specified input stream.
   * @param in The input stream to read from.
   * @return The unsigned byte value from the stream.
   */
  public static short readUnsignedByte(InputStream in) throws IOException
  {
    return (short)(readByte(in) & 0xff);
  }

  public static short readShort(RandomAccessFile ranfile) throws IOException
  {
    ranfile.readFully(buffer2);
    return DynamicArray.getShort(buffer2, 0);
  }

  /**
   * Reads a short (16 bit) from specified input stream.
   * @param in The input stream to read from.
   * @return The short value from the stream.
   */
  public static short readShort(InputStream in) throws IOException
  {
    short res = 0;
    if (in != null) {
      for (int i = 0, shift = 0; i < 2; i++, shift+=8) {
        int n = in.read();
        if (n < 0)
          throw new IOException("End of stream");
        res |= n << shift;
      }
    }
    return res;
  }

  /**
   * Reads an unsigned short (16 bit) from specified input stream.
   * @param in The input stream to read from.
   * @return The unsigned short value from the stream.
   */
  public static int readUnsignedShort(InputStream in) throws IOException
  {
    return readShort(in) & 0xffff;
  }

  public static int readInt(RandomAccessFile ranfile) throws IOException
  {
    ranfile.readFully(buffer4);
    return DynamicArray.getInt(buffer4, 0);
  }

  /**
   * Reads an integer (32 bit) from specified input stream.
   * @param in The input stream to read from.
   * @return The integer value from the stream.
   */
  public static int readInt(InputStream in) throws IOException
  {
    int res = 0;
    if (in != null) {
      for (int i = 0, shift = 0; i < 4; i++, shift+=8) {
        int n = in.read();
        if (n < 0)
          throw new IOException("End of stream");
        res |= n << shift;
      }
    }
    return res;
  }

  /**
   * Reads an unsigned integer (32 bit) from specified input stream.
   * @param in The input stream to read from.
   * @return The unsigned integer value from the stream.
   */
  public static long readUnsignedInt(InputStream in) throws IOException
  {
    return readInt(in) & 0xffffffffL;
  }

  /**
   * Reads an 24 bit integer from specified input stream.
   * @param in The input stream to read from.
   * @return The 24 bit integer value from the stream.
   */
  public static int readInt24(InputStream in) throws IOException
  {
    return signExtend(readUnsignedInt24(in), 24);
  }

  /**
   * Reads an unsigned 24 bit integer from specified input stream.
   * @param in The input stream to read from.
   * @return The unsigned 24 bit integer value from the stream.
   */
  public static int readUnsignedInt24(InputStream in) throws IOException
  {
    int res = 0;
    if (in != null) {
      for (int i = 0, shift = 0; i < 3; i++, shift+=8) {
        int n = in.read();
        if (n < 0)
          throw new IOException("End of stream");
        res |= n << shift;
      }
    }
    return res;
  }

  /**
   * Sign extends the specified value consisting of specified number of bits.
   * @param value The value to sign-extend
   * @param bits Size of <code>value</code> in bits.
   * @return A sign-extended version of <code>value</code>.
   */
  public static int signExtend(int value, int bits)
  {
    return (value << (32 - bits)) >> (32 - bits);
  }

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
    return DynamicArray.getString(buffer, 0, length);
  }

  private Filereader(){}
}

