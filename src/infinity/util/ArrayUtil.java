// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

public final class ArrayUtil
{
  public static byte[] getSubArray(byte[] buffer, int offset, int length)
  {
    byte r[] = new byte[length];
    System.arraycopy(buffer, offset, r, 0, length);
    return r;
  }

  public static byte[] mergeArrays(byte[] a1, byte[] a2)
  {
    byte r[] = new byte[a1.length + a2.length];
    System.arraycopy(a1, 0, r, 0, a1.length);
    System.arraycopy(a2, 0, r, a1.length, a2.length);
    return r;
  }

  public static byte[] resizeArray(byte[] src, int new_size)
  {
    byte tmp[] = new byte[new_size];
    System.arraycopy(src, 0, tmp, 0, Math.min(src.length, new_size));
    return tmp;
  }

  private ArrayUtil(){}
}

