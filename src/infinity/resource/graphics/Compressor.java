// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.util.ArrayUtil;
import infinity.util.DynamicArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.*;

public final class Compressor
{
  private static final Inflater inflater = new Inflater();

  public static byte[] compress(byte data[], String signature, String version)
  {
    byte header[] = ArrayUtil.mergeArrays(signature.getBytes(), version.getBytes());
    header = ArrayUtil.mergeArrays(header, DynamicArray.convertInt(data.length));
    byte result[] = Arrays.copyOf(header, data.length * 2);
    Deflater deflater = new Deflater();
    deflater.setInput(data);
    deflater.finish();
    int clength = deflater.deflate(result, 12, result.length - 12);
    return Arrays.copyOfRange(result, 0, clength + 12);
  }

  public static byte[] decompress(byte buffer[]) throws IOException
  {
    return decompress(buffer, 8);
  }

  public static byte[] decompress(byte buffer[], int ofs) throws IOException
  {
    byte result[] = new byte[DynamicArray.getInt(buffer, ofs)];
    ofs += 4;
    inflater.setInput(buffer, ofs, buffer.length - ofs);
    try {
      inflater.inflate(result);
    } catch (DataFormatException e) {
      throw new IOException();
    }
    inflater.reset();
    return result;
  }

  private Compressor(){}
}

