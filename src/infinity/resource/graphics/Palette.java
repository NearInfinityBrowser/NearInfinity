// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.util.Byteconvert;

final class Palette
{
  private final int[] colors;

  public static int getColor(byte buffer[], int offset, byte index)
  {
    if (index < 0)
      return Byteconvert.convertInt(buffer, offset + (256 + (int)index) * 4);
    return Byteconvert.convertInt(buffer, offset + (int)index * 4);
  }

  Palette(byte buffer[], int offset, int length)
  {
    colors = new int[length / 4];
    for (int i = 0; i < colors.length; i++)
      colors[i] = Byteconvert.convertInt(buffer, offset + i * 4);
  }

  public int getColor(int index)
  {
    if (index < 0)
      return colors[index + 256];
    return colors[index];
  }

  public short[] getColorBytes(int index)
  {
    byte bytes[] = Byteconvert.convertBack(getColor(index));
    short shorts[] = new short[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      shorts[i] = (short)bytes[i];
      if (shorts[i] < 0)
        shorts[i] += (short)256;
    }
    return shorts;
  }
}

