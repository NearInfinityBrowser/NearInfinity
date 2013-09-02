// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.util.EnumMap;

/**
 * Contains static methods to convert pixels from one color format into another (little endian order only).
 * @author argent77
 */
public class ColorConvert
{
  /**
   * Specifies the color layout of a single pixel. (A=alpha, R=red, G=green, B=blue - followed by
   * the individual bit count for the channel)<br>
   * Note: The order (left to right) is defined from highest bit to lowest bit.
   */
  public enum ColorFormat {
    A8R8G8B8, A8B8G8R8, R8G8B8A8, B8G8R8A8,   // 32 bit
    R8G8B8, B8G8R8,                           // 24 bit
    A1R5G5B5, A1B5G5R5, R5G5B5A1, B5G5R5A1,   // 16 bit
    A4R4G4B4, A4B4G4R4, R4G4B4A4, B4G4R4A4,   // 16 bit
    R5G6B5, B5G6R5,                           // 16 bit
  }

  private final static EnumMap<ColorFormat, int[]>  ColorMap = new EnumMap<ColorFormat, int[]>(ColorFormat.class);
  static {
    // Format: AlphaBits, AlphaPos, RedBits, RedPos, GreenBits, GreenPos, BlueBits, BluePos
    ColorMap.put(ColorFormat.A8R8G8B8, new int[]{8, 24, 8, 16, 8,  8, 8,  0});
    ColorMap.put(ColorFormat.A8B8G8R8, new int[]{8, 24, 8,  0, 8,  8, 8, 16});
    ColorMap.put(ColorFormat.R8G8B8A8, new int[]{8,  0, 8, 24, 8, 16, 8,  8});
    ColorMap.put(ColorFormat.B8G8R8A8, new int[]{8,  0, 8,  8, 8, 16, 8, 24});
    ColorMap.put(ColorFormat.R8G8B8,   new int[]{0,  0, 8, 16, 8,  8, 8,  0});
    ColorMap.put(ColorFormat.B8G8R8,   new int[]{0,  0, 8,  0, 8,  8, 8, 16});
    ColorMap.put(ColorFormat.A1R5G5B5, new int[]{1, 15, 5, 10, 5,  5, 5,  0});
    ColorMap.put(ColorFormat.A1B5G5R5, new int[]{1, 15, 5,  0, 5,  5, 5, 10});
    ColorMap.put(ColorFormat.R5G5B5A1, new int[]{1,  0, 5, 11, 5,  6, 5,  1});
    ColorMap.put(ColorFormat.B5G5R5A1, new int[]{1,  0, 5,  1, 5,  6, 5, 11});
    ColorMap.put(ColorFormat.A4R4G4B4, new int[]{4, 12, 4,  8, 4,  4, 4,  0});
    ColorMap.put(ColorFormat.A4B4G4R4, new int[]{4, 12, 4,  0, 4,  4, 4, 8});
    ColorMap.put(ColorFormat.R4G4B4A4, new int[]{4,  0, 4, 12, 4,  8, 4,  4});
    ColorMap.put(ColorFormat.B4G4R4A4, new int[]{4,  0, 4,  4, 4,  8, 4, 12});
    ColorMap.put(ColorFormat.R5G6B5,   new int[]{0,  0, 5, 11, 6,  5, 5,  0});
    ColorMap.put(ColorFormat.B5G6R5,   new int[]{0,  0, 5,  0, 6,  5, 5, 11});
  }

  /**
   * Returns the number of bits required for the specified color format.
   * @parem format The requested color format.
   * @return Number of bits required by the specified color format.
   */
  public static int ColorBits(ColorFormat format)
  {
    int bits = 0;
    int[] layout = ColorMap.get(format);
    if (layout != null)
      for (int i = 0; i < layout.length; i+=2)
        bits += layout[i];

    return bits;
  }

  /**
   * Returns the color format definition structure for the specified color format.
   * @param format The color format to get structure information about.
   * @return An array containing bit count and positions of the individual color channels.
   */
  public static int[] ColorDefinition(ColorFormat format)
  {
    return ColorMap.get(format);
  }

  /**
   * Convert a specified number of pixel from one color format into another.
   * @param inFormat The input color format.
   * @param inBuffer Input buffer containing pixel data to convert.
   * @param inOfs Start offset into input buffer.
   * @param outFormat The desired output color format.
   * @param outBuffer Output buffer to store the converted pixel data.
   * @param outOfs Start offset into the output buffer.
   * @param count Desired number of pixels to convert.
   * @return Actual number of pixels converted.
   */
  public static int Convert(ColorFormat inFormat, byte[] inBuffer, int inOfs,
                            ColorFormat outFormat, byte[] outBuffer, int outOfs,
                            int count)
  {
    if (inBuffer == null || inOfs < 0 || outBuffer == null || outOfs < 0 || count <= 0)
      return 0;

    int inPixelSize = ColorBits(inFormat) >> 3;
    int outPixelSize = ColorBits(outFormat) >> 3;
    if ((inBuffer.length - inOfs) > (count * inPixelSize))
      count = (inBuffer.length - inOfs) / inPixelSize;
    if ((outBuffer.length - outOfs) > (count * outPixelSize))
      count = (outBuffer.length - outOfs) / outPixelSize;
    if (count < 0)
      count = 0;

    if (inFormat == outFormat) {
      System.arraycopy(inBuffer, inOfs, outBuffer, outOfs, count * inPixelSize);
    } else if (count > 0) {
      int[] inLayout = ColorMap.get(inFormat);
      int[] outLayout = ColorMap.get(outFormat);
      int[] transform = new int[outLayout.length];

      // calculating transformation array (position > 0: shift left, position < 0: shift right)
      for (int i = 0; i < transform.length; i+=2) {
        transform[i+1] = (outLayout[i+1] - inLayout[i+1]) + (outLayout[i] - inLayout[i]);
        transform[i] = outLayout[i];    // bit count from output color layout
      }

      for (int i = 0; i < count; i++) {
        // reading input pixel
        int inPixel = 0;
        for (int j = 0; j < inPixelSize; j++)
          inPixel |= (inBuffer[inOfs+j] & 0xff) << (j << 3);

        // transforming input pixel to output pixel
        int outPixel = 0;
        for (int j = 0; j < transform.length; j+=2) {
          if (outLayout[j] > 0) {
            if (transform[j+1] > 0)
              outPixel |= (inPixel << transform[j+1]) & (((1 << outLayout[j]) - 1) << outLayout[j+1]);    // left shift and mask
            else if (transform[j+1] < 0)
              outPixel |= (inPixel >> -transform[j+1]) & (((1 << outLayout[j]) - 1) << outLayout[j+1]);   // right shift and mask
            else
              outPixel |= inPixel & (((1 << outLayout[j]) - 1) << outLayout[j+1]);                        // mask only
          }
        }

        // writing output pixel
        for (int j = 0; j < outPixelSize; j++)
          outBuffer[outOfs+j] = (byte)((outPixel >> (j << 3)) & 0xff);

        inOfs += inPixelSize;
        outOfs += outPixelSize;
      }
    }
    return count;
  }
}
