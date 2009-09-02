// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.util.ArrayUtil;
import infinity.util.Byteconvert;

import java.io.IOException;
import java.util.zip.*;

public final class Compressor
{
  private static final Inflater inflater = new Inflater();

  public static byte[] compress(byte data[], String signature, String version)
  {
    byte header[] = ArrayUtil.mergeArrays(signature.getBytes(), version.getBytes());
    header = ArrayUtil.mergeArrays(header, Byteconvert.convertBack(data.length));
    byte result[] = ArrayUtil.resizeArray(header, data.length * 2);
    Deflater deflater = new Deflater();
    deflater.setInput(data);
    deflater.finish();
    int clength = deflater.deflate(result, 12, result.length - 12);
    return ArrayUtil.getSubArray(result, 0, clength + 12);
  }

  public static byte[] decompress(byte buffer[]) throws IOException
  {
    byte result[] = new byte[Byteconvert.convertInt(buffer, 8)];
    inflater.setInput(buffer, 12, buffer.length - 12);
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

