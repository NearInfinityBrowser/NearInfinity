// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

public final class Byteconvert
{
  public static byte[] convertBack(byte value)
  {
    byte buffer[] = {value};
    return buffer;
  }

  public static byte[] convertBack(short value)
  {
    byte buffer[] = new byte[2];
    for (int i = 0; i <= 1; i++)
      buffer[i] = (byte)(value >> 8 * i & 0xFF);
    return buffer;
  }

  public static byte[] convertBack(int value)
  {
    byte buffer[] = new byte[4];
    for (int i = 0; i <= 3; i++)
      buffer[i] = (byte)(value >> 8 * i & 0xFF);
    return buffer;
  }

  public static byte[] convertBack(long value)
  {
    byte buffer[] = new byte[8];
    for (int i = 0; i <= 7; i++)
      buffer[i] = (byte)(value >> 8 * i & 0xFF);
    return buffer;
  }

  public static byte convertByte(byte buffer[], int offset)
  {
    int value = 0;
    for (int i = 0; i >= 0; i--)
      value = value << 8 | buffer[offset + i] & 0xFF;
    return (byte)value;
  }

  public static int convertInt(byte buffer[], int offset)
  {
    int value = 0;
    for (int i = 3; i >= 0; i--)
      value = value << 8 | buffer[offset + i] & 0xFF;
    return value;
  }

  public static long convertLong(byte buffer[], int offset)
  {
    long value = 0L;
    for (int i = 7; i >= 0; i--)
      value = value << 8 | buffer[offset + i] & 0xFF;
    return value;
  }

  public static short convertShort(byte buffer[], int offset)
  {
    int value = 0;
    for (int i = 1; i >= 0; i--)
      value = value << 8 | buffer[offset + i] & 0xFF;
    return (short)value;
  }

  public static String convertString(byte buffer[], int offset, int length)
  {
    for (int i = 0; i < length; i++) {
      if (buffer[offset + i] == 0x00)
        return new String(buffer, offset, i);
    }
    return new String(buffer, offset, length);
  }

  public static byte[] convertThreeBack(int value)
  {
    byte buffer[] = new byte[3];
    for (int i = 0; i <= 2; i++)
      buffer[i] = (byte)(value >> 8 * i & 0xFF);
    return buffer;
  }

  public static short convertUnsignedByte(byte buffer[], int offset)
  {
    short value = (short)convertByte(buffer, offset);
    if (value < 0)
      value += (short)256;
    return value;
  }

  public static long convertUnsignedInt(byte buffer[], int offset)
  {
    long value = (long)convertInt(buffer, offset);
    if (value < 0)
      value += 4294967296L;
    return value;
  }

  public static int convertUnsignedShort(byte buffer[], int offset)
  {
    int value = (int)convertShort(buffer, offset);
    if (value < 0)
      value += 65536;
    return value;
  }

  private Byteconvert(){}
}

