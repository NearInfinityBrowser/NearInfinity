// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.nio.ByteBuffer;

import org.infinity.util.io.StreamUtils;

final class Palette
{
  private final ByteBuffer colors;

  public static int getColor(ByteBuffer buffer, int offset, int index)
  {
    index = Math.max(0, Math.min(255, index));
    return buffer.getInt(offset + index * 4);
  }

  Palette(ByteBuffer buffer, int offset, int length)
  {
    colors = StreamUtils.getByteBuffer(length);
    StreamUtils.copyBytes(buffer, offset, colors, 0, length);
  }

  public int getColor(int index)
  {
    index = Math.max(0, Math.min(255, index));
    return colors.getInt(index*4);
  }

  public short[] getColorBytes(int index)
  {
    index = Math.max(0, Math.min(255, index));
    int offset = index * 4;
    short[] shorts = { (short)(colors.get(offset) & 0xff),
                       (short)(colors.get(offset+1) & 0xff),
                       (short)(colors.get(offset+2) & 0xff),
                       (short)(colors.get(offset+3) & 0xff) };
    return shorts;
  }
}

