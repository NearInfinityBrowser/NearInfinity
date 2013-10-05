// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.EnumMap;

/**
 * Contains a set of color-related static methods (little endian order only).
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

  private static final EnumMap<ColorFormat, int[]>  ColorMap = new EnumMap<ColorFormat, int[]>(ColorFormat.class);
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
   * Creates a BufferedImage object in the native color format for best possible performance.
   * @param width Image width in pixels
   * @param height Image height in pixels
   * @param hasTransparency Transparency support
   * @return A new BufferedImage object with the specified properties.
   */
  public static BufferedImage createCompatibleImage(int width, int height, boolean hasTransparency)
  {
    // obtain the current system's graphical settings
    final GraphicsConfiguration gfxConfig =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

    return gfxConfig.createCompatibleImage(width, height,
                                           hasTransparency ? BufferedImage.TRANSLUCENT : BufferedImage.OPAQUE);
  }

  /**
   * Converts a generic image object into a BufferedImage object if possible.
   * @param img The image to convert into a BufferedImage object.
   * @param hasTransparency Indicates whether the converted image should support transparency.
   *        (Does nothing if the specified image object is already a BufferedImage object.)
   * @return A BufferedImage object of the specified image.
   */
  public static BufferedImage toBufferedImage(Image img, boolean hasTransparency)
  {
    if (img != null) {
      if (img instanceof BufferedImage) {
        return (BufferedImage)img;
      } else {
        final BufferedImage image = createCompatibleImage(img.getWidth(null), img.getHeight(null),
                                                          hasTransparency);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return image;
      }
    }
    return null;
  }

//  /**
//   * Returns the color, located in buffer, starting at offset ofs, as an integer
//   * @param format The color format of the data in the buffer
//   * @param buffer Input buffer containing the color data
//   * @param ofs Start offset into the input buffer
//   * @return The color as integer value
//   */
//  public static int BufferToColor(ColorFormat format, byte[] buffer, int ofs)
//  {
//    int c = 0;
//    int bpp = ColorBits(format) >> 3;
//
//    if (buffer != null && ofs >= 0 && ofs + bpp <= buffer.length) {
//      for (int i = 0; i < bpp; i++) {
//        c |= (int)((buffer[ofs+i] & 0xff) << (i << 3));
//      }
//    }
//
//    return c;
//  }

  /**
   * Converts 'count' colors from a byte array into an array of integers, consisting of the whole color values.
   * @param format The color format of the data in the buffer.
   * @param inBuffer Input buffer containing the color data as byte array.
   * @param inOfs Start offset into the input buffer.
   * @param outBuffer Output buffer to write the whole color value to.
   * @param outOfs Start offset into the output buffer.
   * @param count Number of colors to convert.
   * @return The actual number of converted colors.
   */
  public static int BufferToColor(ColorFormat format, byte[] inBuffer, int inOfs, int[] outBuffer, int outOfs, int count)
  {
    if (inBuffer == null || outBuffer == null || inOfs < 0 || outOfs < 0)
      return 0;

    int bpp = ColorBits(format) >> 3;
    if (inOfs + count*bpp > inBuffer.length)
      count = (inBuffer.length - inOfs) / bpp;
    if (outOfs + count > outBuffer.length)
      count = outBuffer.length - outOfs;

    for (int idx = 0; idx < count; idx++, inOfs+=bpp, outOfs++) {
      int c = 0;
      for (int i = 0; i < bpp; i++)
        c |= (int)((inBuffer[inOfs+i] & 0xff) << (i << 3));
      outBuffer[outOfs] = c;
    }

    return count;
  }

//  /**
//   * Convert the specified color into an array of bytes.
//   * @param format The color format.
//   * @param color The color.
//   * @param buffer The output buffer to write the stream of bytes into.
//   * @param ofs Start offset into the output buffer.
//   * @return true if the conversion succeeded, false otherwise.
//   */
//  public static boolean ColorToBuffer(ColorFormat format, int color, byte[] buffer, int ofs)
//  {
//    int bpp = ColorBits(format) >> 3;
//    if (buffer != null && buffer.length - ofs >= bpp) {
//      for (int i = 0; i < bpp; i++)
//        buffer[ofs+i] = (byte)((color >>> (i << 3)) & 0xff);
//      return true;
//    } else
//      return false;
//  }

//  /**
//   * Convert the array of colors into an array of bytes.
//   * @param format The format of the colors.
//   * @param inBuffer Input buffer containing the colors
//   * @param inOfs Start offset into the input buffer.
//   * @param outBuffer Output buffer to write the stream of bytes into.
//   * @param outOfs Start offset into the output buffer.
//   * @param count Number of colors to convert.
//   * @return The actual number of converted colors.
//   */
//  public static int ColorToBuffer(ColorFormat format, int[] inBuffer, int inOfs, byte[] outBuffer, int outOfs, int count)
//  {
//    if (inBuffer == null || outBuffer == null || inOfs < 0 || outOfs < 0)
//      return 0;
//
//    int bpp = ColorBits(format) >> 3;
//    if (inOfs + count > inBuffer.length)
//      count = inBuffer.length - inOfs;
//    if (outOfs + count*bpp > outBuffer.length)
//      count = (outBuffer.length - outOfs) / bpp;
//
//    for (int idx = 0; idx < count; idx++, inOfs++) {
//      int c = inBuffer[inOfs];
//      for (int i = 0; i < bpp; i++, outOfs++)
//        outBuffer[outOfs] = (byte)(c >>> (i << 3) & 0xff);
//    }
//
//    return count;
//  }

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
    if ((inBuffer.length - inOfs) < (count * inPixelSize))
      count = (inBuffer.length - inOfs) / inPixelSize;
    if ((outBuffer.length - outOfs) < (count * outPixelSize))
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

//  /**
//   * Returns a Windows BMP header structure as byte array.
//   * @param width The image width in pixels.
//   * @param height The image height in pixels.
//   * @param format The color format of the image.
//   * @return A byte array containing a complete Windows BMP header.
//   * @throws Exception Thrown if color format is not supported by the graphics format.
//   */
//  public static byte[] CreateBMPHeader(int width, int height, ColorFormat format) throws Exception
//  {
//    if (width < 0 || height < 0)
//      throw new Exception("Invalid image dimensions specified");
//
//    final int bmpFileHeaderSize = 14;
//
//    int bmpInfoHeaderSize;
//    int compression;
//    boolean useV5Header;
//    // checking for supported BMP color formats
//    switch (format) {
//      case A8R8G8B8:
//      case A8B8G8R8:
//      case R8G8B8A8:
//      case B8G8R8A8:
//      case R5G6B5:
//      case B5G5R5A1:
//      case R5G5B5A1:
//      case A4R4G4B4:
//      case A4B4G4R4:
//        // using BITMAPV5HEADER structure
//        useV5Header = true;
//        bmpInfoHeaderSize = 124;
//        compression = 3;    // BITFIELD compression
//        break;
//      case A1R5G5B5:
//      case R8G8B8:
//        // using old BITMAPINFOHEADER structure
//        useV5Header = false;
//        bmpInfoHeaderSize = 40;
//        compression = 0;    // RGB compression
//        break;
//      default:
//        throw new Exception("Pixel format not supported.");
//    }
//
//    int depth = ColorBits(format);
//    int bpp = depth >> 3;
//    int bmpHeaderSize = bmpFileHeaderSize + bmpInfoHeaderSize;
//    int bytesPerLine = width*bpp;
//    if ((bytesPerLine & 3) != 0)    // don't forget the padding
//      bytesPerLine += 4 - (bytesPerLine & 3);
//    long bmpSize = bmpHeaderSize + height*bytesPerLine;   // complete header size + pixel data size
//    byte[] bmpHeader = new byte[bmpHeaderSize];
//
//    ByteBuffer bb = ByteBuffer.wrap(bmpHeader).order(ByteOrder.LITTLE_ENDIAN);
//    // BITMAP header
//    bb.putShort((short)0x4D42);         // BM
//    bb.putLong(bmpSize);                // total file size
//    bb.putInt(bmpHeaderSize);           // offset to pixel data block
//    // BITMAPINFOHEADER structure
//    bb.putInt(bmpInfoHeaderSize);       // BITMAPxxxHEADER size
//    bb.putInt(width);                   // width
//    bb.putInt(height);                  // height
//    bb.putShort((short)1);              // # planes
//    bb.putShort((short)depth);          // bpp
//    bb.putInt(compression);             // Compression method
//    bb.putInt(width*height*bpp);        // image size in bytes
//    bb.putInt(2834);                    // X resolution in pixels/meter
//    bb.putInt(2834);                    // Y resolution in pixels/meter
//    bb.putInt(0);                       // # of palette entries
//    bb.putInt(0);                       // # of important colors
//    if (useV5Header) {
//      // BITMAPV5HEADER additions
//      final int csRGB  = 0x206e6957;    // linear RGB color space ID
//      int[] colorFormat = ColorDefinition(format);
//      int[] mask = new int[4];
//      mask[0] = ((1 << colorFormat[2]) - 1) << colorFormat[3];
//      mask[1] = ((1 << colorFormat[4]) - 1) << colorFormat[5];
//      mask[2] = ((1 << colorFormat[6]) - 1) << colorFormat[7];
//      mask[3] = ((1 << colorFormat[0]) - 1) << colorFormat[1];
//      for (final int m: mask)           // color masks
//        bb.putInt(m);
//      bb.putInt(csRGB);                 // type of color space
//      bb.put(new byte[0x24]);           // unused
//      bb.putInt(0);                     // red gamma
//      bb.putInt(0);                     // green gamma
//      bb.putInt(0);                     // blue gamma
//      bb.putInt(8);                     // intent (LCS_GM_ABS_COLORIMETRIC)
//      bb.putInt(0);                     // profile data
//      bb.putInt(0);                     // profile size
//      bb.putInt(0);                     // reserved
//    }
//    return bmpHeader;
//  }
}
