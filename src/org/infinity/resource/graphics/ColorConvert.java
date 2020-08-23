// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.infinity.util.DynamicArray;
import org.infinity.util.io.StreamUtils;

/**
 * Contains a set of color-related static methods (little endian order only).
 */
public class ColorConvert
{
  // max. number of colors for color reduction algorithms
  private static final int MAX_COLORS = 256;

  public enum SortType {
    None,       // Don't sort. Useful if you simply want to reverse color order.
    Lightness,  // Sort by perceived lightness aspect of color.
    Saturation, // Sort by saturation aspect of color.
    Hue,        // Sort by hue aspect of color.
    Red,        // Sort by red color component.
    Green,      // Sort by green color component.
    Blue,       // Sort by blue color component.
    Alpha       // Sort by alpha component.
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
    return createCompatibleImage(width, height,
                                 hasTransparency ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
  }

  /**
   * Creates a BufferedImage object in the native color format for best possible performance.
   * @param width Image width in pixels
   * @param height Image height in pixels
   * @param transparency The transparency type (either one of {@code Transparency.OPAQUE},
   *                     {@code Transparency.BITMASK} or {@code Transparency.TRANSLUCENT}).
   * @return A new BufferedImage object with the specified properties.
   */
  public static BufferedImage createCompatibleImage(int width, int height, int transparency)
  {
    if (transparency == Transparency.TRANSLUCENT) {
      return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    } else {
      // obtain the current system's graphical settings
      final GraphicsConfiguration gfxConfig =
          GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

      return gfxConfig.createCompatibleImage(width, height, transparency);
    }
  }

  /**
   * Creates a VolatileImage object in the native color format for best possible performance.
   * @param width Image width in pixels
   * @param height Image height in pixels
   * @param hasTransparency Transparency support
   * @return A new VolatileImage object with the specified properties.
   */
  public static VolatileImage createVolatileImage(int width, int height, boolean hasTransparency)
  {
    return createVolatileImage(width, height,
                               hasTransparency ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
  }

  /**
   * Creates a VolatileImage object in the native color format for best possible performance.
   * @param width Image width in pixels
   * @param height Image height in pixels
   * @param transparency The transparency type (either one of {@code Transparency.OPAQUE},
   *                     {@code Transparency.BITMASK} or {@code Transparency.TRANSLUCENT}).
   * @return A new VolatileImage object with the specified properties.
   */
  public static VolatileImage createVolatileImage(int width, int height, int transparency)
  {
    GraphicsConfiguration gc =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

    return gc.createCompatibleVolatileImage(width, height, transparency);
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
    return toBufferedImage(img, hasTransparency, true);
  }

  /**
   * Converts a generic image object into a BufferedImage object if possible.
   * Returns a BufferedImage object that is guaranteed to be either in true color or indexed color format.
   * @param img The image to convert into a BufferedImage object.
   * @param hasTransparency Indicates whether the converted image should support transparency.
   *        (Does nothing if the specified image object is already a BufferedImage object.)
   * @param forceTrueColor Indicates whether the returned BufferedImage object will alway be in
   *        true color color format (i.e. pixels in ARGB format, with or without transparency support).
   * @return A BufferedImage object of the specified image in either BufferedImage.TYPE_BYTE_INDEXED format
   *         or one of the true color formats, Returns {@code null} on error.
   */
  public static BufferedImage toBufferedImage(Image img, boolean hasTransparency, boolean forceTrueColor)
  {
    if (img != null) {
      if (img instanceof BufferedImage) {
        BufferedImage srcImage = (BufferedImage)img;
        int type = srcImage.getRaster().getDataBuffer().getDataType();
        if (type == DataBuffer.TYPE_INT) {
          return srcImage;
        } else if (!forceTrueColor) {
          // attempting to convert special formats into 8-bit paletted image format
          BufferedImage dstImage = convertToIndexedImage(srcImage);
          if (dstImage != null) {
            return dstImage;
          }
        }
      }
      final BufferedImage image = createCompatibleImage(img.getWidth(null), img.getHeight(null),
                                                        hasTransparency);
      Graphics2D g = image.createGraphics();
      try {
        g.drawImage(img, 0, 0, null);
      } finally {
        g.dispose();
        g = null;
      }
      return image;
    }
    return null;
  }

  /**
   * Attempts to create a deep copy of the specified BufferedImage object without losing any of its
   * original properties. Creates a truecolored BufferedImage object if source image format is not
   * fully supported.
   * Note: Only indexed and truecolored BufferedImage objects are fully supported!
   * @param image The image object to clone.
   * @return A new BufferedImage object possessing the content and properties of the source image.
   *         Returns {@code null} on error.
   */
  public static BufferedImage cloneImage(BufferedImage image)
  {
    BufferedImage dstImage = null;
    if (image != null) {
      ColorModel cm = image.getColorModel();
      boolean isAlphaPreMultiplied = cm.isAlphaPremultiplied();
      WritableRaster raster = image.copyData(null);
      Hashtable<String, Object> table = null;
      String[] propertyNames = image.getPropertyNames();
      if (propertyNames != null) {
        table = new Hashtable<>(propertyNames.length);
        for (String name: propertyNames) {
          table.put(name, image.getProperty(name));
        }
      }
      dstImage = new BufferedImage(cm, raster, isAlphaPreMultiplied, table);
    }
    return dstImage;
  }

  /**
   * Attempts to retrieve the width and height of the specified image file without loading it completely.
   * @param fileName The image filename.
   * @return The image dimensions.
   */
  public static Dimension getImageDimension(Path fileName)
  {
    Dimension d = new Dimension();
    try (ImageInputStream iis = ImageIO.createImageInputStream(StreamUtils.getInputStream(fileName))) {
      final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
      if (readers.hasNext()) {
        ImageReader reader = readers.next();
        try {
          reader.setInput(iis);
          d.width = reader.getWidth(0);
          d.height = reader.getHeight(0);
        } finally {
          reader.dispose();
        }
      }
    } catch (Exception e) {
      d.width = d.height = 0;
    }

    return d;
  }

  /**
   * Calculates the nearest color available in the given RGBA palette for the specified color.
   * @param rgbColor The source color in ARGB format.
   * @param rgbPalette A palette containing ARGB color entries.
   * @param ignoreAlpha Whether to exclude alpha component from the calculation.
   * @return The palette index pointing to the nearest color, or -1 on error.
   */
  public static int nearestColorRGB(int rgbColor, int[] rgbPalette, boolean ignoreAlpha)
  {
    // TODO: Improve match quality for grayscaled colors
    int index = -1;
    if (rgbPalette != null && rgbPalette.length > 0) {
      int mask = ignoreAlpha ? 0 : 0xff000000;
      int minDist = Integer.MAX_VALUE;
      int a = ((rgbColor & mask) >> 24) & 0xff;
      int r = (rgbColor >> 16) & 0xff;
      int g = (rgbColor >> 8) & 0xff;
      int b = rgbColor & 0xff;

      int da, dr, dg, db;
      for (int i = 0; i < rgbPalette.length; i++) {
        int col = rgbPalette[i];
        // Extra check for full transparency
        if (a == 0) {
          if ((col & 0xff000000) == 0) { return i; }
          if (col == 0xff00ff00) { return i; }
        }
        // Computing weighted distance to compensate for perceived color differences
        int a2 = ((rgbPalette[i] & mask) >> 24) & 0xff;
        int r2 = (rgbPalette[i] >> 16) & 0xff;
        int g2 = (rgbPalette[i] >> 8) & 0xff;
        int b2 = rgbPalette[i] & 0xff;
        da = (a - a2) * 48;
        dr = (r - r2) * 14;
        dg = (g - g2) * 28;
        db = (b - b2) * 6;
        int dist = da*da + dr*dr + dg*dg + db*db;
        if (dist < minDist) {
          minDist = dist;
          index = i;
        }
      }
    }
    return index;
  }

  /**
   * Sorts the given palette in-place by the specified sort type.
   * @param palette Palette with ARGB color entries.
   * @param startIndex First color entry to consider for sorting.
   * @param type The sort type.
   * @param reversed Whether to sort in reversed order.
   */
  public static void sortPalette(int[] palette, int startIndex, SortType type, boolean reversed)
  {
    if (palette != null && palette.length > startIndex+1) {
      Integer[] tmpColors = new Integer[palette.length];
      for (int i = 0; i < palette.length; i++) {
        tmpColors[i] = Integer.valueOf(palette[i]);
      }

      switch (type) {
        case Lightness:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareByLightness());
          break;
        case Saturation:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareBySaturation());
          break;
        case Hue:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareByHue());
          break;
        case Red:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareByRed());
          break;
        case Green:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareByGreen());
          break;
        case Blue:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareByBlue());
          break;
        case Alpha:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, new CompareByAlpha());
          break;
        default:
          break;
      }

      for (int i = 0; i < tmpColors.length; i++) {
        palette[i] = tmpColors[i].intValue();
      }

      if (reversed) {
        for (int i = startIndex, j = palette.length - 1; i < j; i++, j--) {
          int tmp = palette[i];
          palette[i] = palette[j];
          palette[j] = tmp;
        }
      }
    }
  }

  // Returns each color component as float array {b, g, r, a} in range [0.0, 1.0].
  private static double[] getNormalizedColor(int color)
  {
    return new double[] {
        (double)(color & 0xff) / 255.0,
        (double)((color >> 8) & 0xff) / 255.0,
        (double)((color >> 16) & 0xff) / 255.0,
        (double)((color >> 24) & 0xff) / 255.0
    };
  }


  /**
   * Reduces the number of colors of the specified pixel data block.
   * @param pixels The pixel block of the image in ARGB format (alpha is ignored).
   * @param desiredColors The resulting number of colors after reduction (range 1..256).
   * @param ignoreAlpha If {@code false}, only visible color values (alpha > 0) will be counted.
   * @return An array containing the resulting colors, or {@code null} on error.
   */
  public static int[] medianCut(int[] pixels, int desiredColors, boolean ignoreAlpha)
  {
    if (desiredColors > 0 && desiredColors <= MAX_COLORS) {
      int[] pal = new int[desiredColors];
      if (medianCut(pixels, desiredColors, pal, ignoreAlpha)) {
        return pal;
      } else {
        pal = null;
      }
    }
    return null;
  }

  /**
   * TODO: Consider alpha values in color reduction.
   * Reduces the number of colors of the specified pixel data block.
   * @param pixels The pixel block of the image in ARGB format (alpha is ignored).
   * @param desiredColors The resulting number of colors after reduction (range 1..256).
   * @param palette The array to write the resulting colors into.
   * @param ignoreAlpha If {@code false}, only visible color values (alpha > 0) will be counted.
   * @return {@code true} if color reduction succeeded, {@code false} otherwise.
   */
  public static boolean medianCut(int[] pixels, int desiredColors, int[] palette, boolean ignoreAlpha)
  {
    if (pixels == null || palette == null)
      throw new NullPointerException();

    if (desiredColors > 0 && desiredColors <= MAX_COLORS && palette.length >= desiredColors) {
      PriorityQueue<PixelBlock> blockQueue =
          new PriorityQueue<PixelBlock>(desiredColors, PixelBlock.PixelBlockComparator);

      Pixel[] p = null;
      p = new Pixel[pixels.length];
      int mask = ignoreAlpha ? 0xff000000: 0;
      p = new Pixel[pixels.length];
      for (int i = 0; i < p.length; i++) {
        p[i] = new Pixel(pixels[i] | mask);
      }

      PixelBlock initialBlock = new PixelBlock(p);
      initialBlock.shrink();
      blockQueue.add(initialBlock);
      while (blockQueue.size() < desiredColors) {
        PixelBlock longestBlock = blockQueue.poll();
        int ofsBegin = longestBlock.offset();
        int ofsMedian = longestBlock.offset() + (longestBlock.size() + 1) / 2;
        int ofsEnd = longestBlock.offset() + longestBlock.size();
        Arrays.sort(longestBlock.getPixels(), longestBlock.offset(),
                    longestBlock.offset() + longestBlock.size(),
                    Pixel.PixelComparator.get(longestBlock.getLongestSideIndex()));
        PixelBlock block1 = new PixelBlock(longestBlock.getPixels(), ofsBegin, ofsMedian - ofsBegin);
        PixelBlock block2 = new PixelBlock(longestBlock.getPixels(), ofsMedian, ofsEnd - ofsMedian);
        block1.shrink();
        block2.shrink();
        blockQueue.add(block1);
        blockQueue.add(block2);
      }

      int palIndex = 0;
      while (!blockQueue.isEmpty() && palIndex < desiredColors) {
        PixelBlock block = blockQueue.poll();
        int[] sum = {0, 0, 0, 0};
        for (int i = 0; i < block.size(); i++) {
          for (int j = 0; j < Pixel.MAX_SIZE; j++) {
            sum[j] += block.getPixel(i).getElement(j);
          }
        }
        Pixel avgPixel = new Pixel();
        if (block.size() > 0) {
          for (int i = 0; i < Pixel.MAX_SIZE; i++) {
            avgPixel.color[i] = (byte)(sum[i] / block.size());
          }
        }
        palette[palIndex++] = avgPixel.toColor();
      }
      blockQueue.clear();

      return true;
    }
    return false;
  }

  /**
   * Attempts to load a palette from the specified Windows BMP file.
   * @param file The Windows BMP file to extract the palette from.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPaletteBMP(Path file) throws Exception
  {
    if (file != null && Files.isRegularFile(file)) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        byte[] signature = new byte[8];
        is.read(signature);
        if ("BM".equals(new String(signature, 0, 2))) {
          // extracting palette from BMP file
          byte[] header = new byte[54];
          System.arraycopy(signature, 0, header, 0, signature.length);
          is.read(header, signature.length, header.length - signature.length);
          if (DynamicArray.getInt(header, 0x0e) == 0x28 &&      // correct BMP header size
              DynamicArray.getInt(header, 0x12) > 0 &&          // valid width
              DynamicArray.getInt(header, 0x16) > 0 &&          // valid height
              (DynamicArray.getShort(header, 0x1c) == 4 ||      // either 4bpp
               DynamicArray.getInt(header, 0x1c) == 8) &&       // or 8bpp
              DynamicArray.getInt(header, 0x1e) == 0) {         // no special encoding
            int bpp = DynamicArray.getUnsignedShort(header, 0x1c);
            int colorCount = 1 << bpp;
            byte[] palette = new byte[colorCount*4];
            is.read(palette);
            int[] retVal = new int[colorCount];
            for (int i =0; i < colorCount; i++) {
              retVal[i] = 0xff000000 | (DynamicArray.getInt(palette, i << 2) & 0x00ffffff);
            }
            return retVal;
          } else {
            throw new Exception("Error loading palette from BMP file " + file.getFileName());
          }
        } else {
          throw new Exception("Invalid BMP file " + file.getFileName());
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read BMP file " + file.getFileName());
      }
    } else {
      throw new Exception("File does not exist.");
    }
  }

  /**
   * Attempts to load a palette from the specified PNG file.
   * @param file The PNG file to extract the palette from.
   * @param preserveAlpha Whether to preserve original alpha transparency of the palette.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPalettePNG(Path file, boolean preserveAlpha) throws Exception
  {
    if (file != null && Files.isRegularFile(file)) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        BufferedImage img = ImageIO.read(is);
        if (img.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
          IndexColorModel cm = (IndexColorModel)img.getColorModel();
          int[] retVal = new int[cm.getMapSize()];
          cm.getRGBs(retVal);
          if (!preserveAlpha) {
            for (int i = 0; i < retVal.length; i++)
              retVal[i] |= 0xff000000;
          }
          return retVal;
        } else {
          throw new Exception("Error loading palette from PNG file " + file.getFileName());
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read PNG file " + file.getFileName());
      }
    } else {
      throw new Exception("File does not exist.");
    }
  }

  /**
   * Attempts to load a palette from the specified Windows PAL file. Does not support alpha transparency.
   * @param file The Windows PAL file to load.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPalettePAL(Path file) throws Exception
  {
    if (file != null && Files.isRegularFile(file)) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        byte[] signature = new byte[12];
        boolean eof = is.read(signature) != signature.length;
        if ("RIFF".equals(new String(signature, 0, 4)) && "PAL ".equals(new String(signature, 8, 4))) {
          byte[] signature2 = new byte[8];

          // find palette data block
          eof = is.read(signature2) != signature2.length;
          while (!eof && !"data".equals(new String(signature2, 0, 4))) {
            is.skip(DynamicArray.getInt(signature2, 4) - 4);
            eof = is.read(signature2) != signature2.length;
          }
          if (eof)
            throw new Exception();

          // extracting palette from Windows palette file
          byte[] header = new byte[4];
          is.read(header);
          int numColors = DynamicArray.getUnsignedShort(header, 2);
          if (numColors >= 2 && numColors <= 256) {
            byte[] palData = new byte[numColors << 2];
            is.read(palData);
            int[] retVal = new int[numColors];
            for (int i = 0; i < numColors; i++) {
              int col = DynamicArray.getInt(palData, i << 2);
              retVal[i] = 0xff000000 | ((col << 16) & 0xff0000) | (col & 0x00ff00) | ((col >> 16) & 0x0000ff);
            }
            return retVal;
          } else {
            throw new Exception("Invalid number of color entries in Windows palette file " + file.getFileName());
          }
        } else {
          throw new Exception("Invalid Windows palette file " + file.getFileName());
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read Windows palette file " + file.getFileName());
      }
    } else {
      throw new Exception("File does not exist.");
    }
  }

  /**
   * Attempts to load a palette from the specified Adobe Color Table file. Does not support alpha transparency.
   * @param file The Adobe Color Table file to load.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPaletteACT(Path file) throws Exception
  {
    if (file != null && Files.isRegularFile(file)) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        int size = (int)Files.size(file);
        if (size >= 768) {
          byte[] palData = new byte[768];
          is.read(palData);
          int count = 256;
          int[] retVal = new int[count];
          int transColor = -1;
          if (size == 772) {
            is.skip(3);
            transColor = is.read();
          }
          for (int ofs = 0, i = 0; i < count; i++, ofs += 3) {
            if (i == transColor) {
              retVal[i] = 0x00ff00;
            } else {
              retVal[i] = 0xff000000 | ((palData[ofs] & 0xff) << 16) | ((palData[ofs+1] & 0xff) << 8) | (palData[ofs+2] & 0xff);
            }
          }
          return retVal;
        } else {
          throw new Exception("Invalid Adobe Photoshop palette file " + file.getFileName());
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read Adobe Photoshop palette file " + file.getFileName());
      }
    } else {
      throw new Exception("File does not exist.");
    }
  }

  /**
   * Attempts to load a palette from the specified BAM file.
   * @param file The BAM file to extract the palette from.
   * @param preserveAlpha Whether to preserve original alpha transparency of the BAM palette.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPaletteBAM(Path file, boolean preserveAlpha) throws Exception
  {
    if (file != null && Files.isRegularFile(file)) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        byte[] signature = new byte[8];
        is.read(signature);
        String s = new String(signature);
        if ("BAM V1  ".equals(s) || "BAMCV1  ".equals(s)) {
          byte[] bamData = new byte[(int)Files.size(file)];
          System.arraycopy(signature, 0, bamData, 0, signature.length);
          is.read(bamData, signature.length, bamData.length - signature.length);
          if ("BAMCV1  ".equals(s)) {
            bamData = Compressor.decompress(bamData);
          }
          // extracting palette from BAM v1 file
          int ofs = DynamicArray.getInt(bamData, 0x10);
          if (ofs >= 0x18 && ofs < bamData.length - 1024) {
            int[] retVal = new int[256];
            for (int i = 0; i < 256; i++) {
              retVal[i] = DynamicArray.getInt(bamData, ofs+(i << 2));
              // backwards compatibility with non-EE BAM files
              if (!preserveAlpha || (retVal[i] & 0xff000000) == 0)
                retVal[i] |= 0xff000000;
            }
            return retVal;
          } else {
            throw new Exception("Error loading palette from BAM file " + file.getFileName());
          }
        } else {
          throw new Exception("Unsupport file type.");
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read BAM file " + file.getFileName());
      }
    } else {
      throw new Exception("File does not exist.");
    }
  }


  /**
   * Attempts to convert a 1-, 2- or 4-bit paletted image or a grayscale image into an 8-bit paletted
   * image.
   * @param image The image to convert.
   * @return The converted image if the source format is compatible with a 256 color table,
   *         {@code null} otherwise.
   */
  private static BufferedImage convertToIndexedImage(BufferedImage image)
  {
    if (image != null) {
      if (image.getType() == BufferedImage.TYPE_BYTE_BINARY) {
        // converting 1-, 2-, 4-bit image data into paletted image
        int[] cmap = new int[256];
        IndexColorModel srcPal = (IndexColorModel)image.getColorModel();
        int bits = srcPal.getPixelSize();
        int bitMask = (1 << bits) - 1;
        int numColors = 1 << bits;
        for (int i = 0; i < 256; i++) {
          if (i < numColors) {
            cmap[i] = srcPal.getRGB(i) | (srcPal.hasAlpha() ? (srcPal.getAlpha(i) << 24) : 0xff000000);
          } else {
            cmap[i] = 0xff000000;
          }
        }
        IndexColorModel dstPal;
        dstPal = new IndexColorModel(8, 256, cmap, 0, srcPal.hasAlpha(), -1, DataBuffer.TYPE_BYTE);
        BufferedImage dstImage = new BufferedImage(image.getWidth(), image.getHeight(),
            BufferedImage.TYPE_BYTE_INDEXED, dstPal);
        byte[] src = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        byte[] dst = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
        int srcOfs = 0, srcBitPos = 8 - bits, dstOfs = 0;
        while (dstOfs < dst.length) {
          dst[dstOfs] = (byte)((src[srcOfs] >>> srcBitPos) & bitMask);
          srcBitPos -= bits;
          if (srcBitPos < 0) {
            srcBitPos += 8;
            srcOfs++;
          }
          dstOfs++;
        }
        cmap = null;
        src = null; dst = null;
        return dstImage;
      } else if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
        // converting grayscaled image with implicit palette into indexed image with explicit palette
        int[] cmap = new int[256];
        for (int i = 0; i < cmap.length; i++) {
          cmap[i] = (i << 16) | (i << 8) | i;
        }
        IndexColorModel cm = new IndexColorModel(8, 256, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
        BufferedImage dstImage = new BufferedImage(image.getWidth(), image.getHeight(),
                                                   BufferedImage.TYPE_BYTE_INDEXED, cm);
        byte[] src = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        byte[] dst = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(src, 0, dst, 0, src.length);
        cmap = null;
        src = null; dst = null;
        return dstImage;
      } else if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        return image;
      }
    }
    return null;
  }

//-------------------------- INNER CLASSES --------------------------

  private static class PixelBlock
  {
    private final Pixel minCorner, maxCorner;
    private final Pixel[] pixels;
    private final int ofs, len;

    public PixelBlock(Pixel[] pixels)
    {
      this(pixels, 0, pixels != null ? pixels.length : 0);
    }

    public PixelBlock(Pixel[] pixels, int ofs, int len)
    {
      if (pixels == null)
        throw new NullPointerException();

      this.pixels = pixels;
      this.ofs = ofs;
      this.len = len;
      minCorner = new Pixel(Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE);
      maxCorner = new Pixel(Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE);
    }

    public Pixel[] getPixels()
    {
      return pixels;
    }

    public int size()
    {
      return len;
    }

    public int offset()
    {
      return ofs;
    }

    public Pixel getPixel(int index)
    {
      if (index >= 0 && index < len) {
        return pixels[ofs + index];
      } else {
        return new Pixel(0);
      }
    }

    public int getLongestSideIndex()
    {
      int m = Integer.MIN_VALUE;
      int maxIndex = -1;
      for (int i = 0; i < Pixel.MAX_SIZE; i++) {
        int diff = maxCorner.getElement(i) - minCorner.getElement(i);
        if (diff > m) {
          m = diff;
          maxIndex = i;
        }
      }
      return maxIndex;
    }

    public int getLongestSideLength()
    {
      int i = getLongestSideIndex();
      return maxCorner.getElement(i) - minCorner.getElement(i);
    }

    public void shrink()
    {
      if (len > 0) {
        for (int i = 0; i < Pixel.MAX_SIZE; i++) {
          minCorner.color[i] = maxCorner.color[i] = pixels[ofs].color[i];
        }
      } else {
        for (int i = 0; i < Pixel.MAX_SIZE; i++) {
          minCorner.color[i] = maxCorner.color[i] = 0;
        }
      }

      for (int i = ofs; i < ofs + len; i++) {
        for (int j = 0; j < Pixel.MAX_SIZE; j++) {
          if (pixels[i].getElement(j) < minCorner.getElement(j))
            minCorner.color[j] = pixels[i].color[j];
          if (pixels[i].getElement(j) > maxCorner.getElement(j))
            maxCorner.color[j] = pixels[i].color[j];
        }
      }
    }

    public static Comparator<PixelBlock> PixelBlockComparator = new Comparator<PixelBlock>() {
      @Override
      public int compare(PixelBlock pb1, PixelBlock pb2)
      {
        // inverting natural order by switching sides
        return pb2.getLongestSideLength() - pb1.getLongestSideLength();
      }
    };
  }

  private static class Pixel
  {
    public static final int MAX_SIZE = 4;
    public final byte[] color;

    public Pixel()
    {
      this.color = new byte[]{0, 0, 0, 0};
    }

    public Pixel(int color)
    {
      this.color = new byte[]{(byte)((color >>> 24) & 0xff),
                              (byte)((color >>> 16) & 0xff),
                              (byte)((color >>> 8) & 0xff),
                              (byte)(color & 0xff)};
    }

    public Pixel(byte r, byte g, byte b, byte a)
    {
      this.color = new byte[]{a, r, g, b};
    }

    public int toColor()
    {
      return ((color[0] & 0xff) << 24) | ((color[1] & 0xff) << 16) | ((color[2] & 0xff) << 8) | (color[3] & 0xff);
    }

    public int getElement(int index)
    {
      if (index >= 0 && index < MAX_SIZE) {
        return (color[index] & 0xff);
      }  else {
        return 0;
      }
    }

    public static List<Comparator<Pixel>> PixelComparator = new ArrayList<Comparator<Pixel>>(MAX_SIZE);
    static {
      PixelComparator.add(new Comparator<Pixel>() {
        @Override
        public int compare(Pixel p1, Pixel p2)
        {
          return p1.getElement(0) - p2.getElement(0);
        }
      });
      PixelComparator.add(new Comparator<Pixel>() {
        @Override
        public int compare(Pixel p1, Pixel p2)
        {
          return p1.getElement(1) - p2.getElement(1);
        }
      });
      PixelComparator.add(new Comparator<Pixel>() {
        @Override
        public int compare(Pixel p1, Pixel p2)
        {
          return p1.getElement(2) - p2.getElement(2);
        }
      });
      PixelComparator.add(new Comparator<Pixel>() {
        @Override
        public int compare(Pixel p1, Pixel p2)
        {
          return p1.getElement(3) - p2.getElement(3);
        }
      });
    }
  }


  // Compare colors by perceived lightness.
  private static class CompareByLightness implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      Integer[] colors = new Integer[] {c1, c2};
      double[] dist = new double[colors.length];
      for (int i = 0; i < colors.length; i++) {
        double r, g, b, a;
        double[] rgba = getNormalizedColor(colors[i]);
        b = rgba[0] * rgba[0] * 0.057;
        g = rgba[1] * rgba[1] * 0.2935;
        r = rgba[2] * rgba[2] * 0.1495;
        a = rgba[3] * rgba[3] * 0.5;
        dist[i] = Math.sqrt(b + g + r + a);
      }
      return (dist[0] < dist[1]) ? -1 : ((dist[0] > dist[1]) ? 1 : 0);
    }
  }

  // Compare colors by saturation.
  private static class CompareBySaturation implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      Integer[] colors = new Integer[] {c1, c2};
      double[] dist = new double[colors.length];
      for (int i = 0; i < colors.length; i++) {
        double[] rgba = getNormalizedColor(colors[i]);
        double cmin = rgba[0] < rgba[1] ? rgba[0] : rgba[1];
        if (rgba[2] < cmin) cmin = rgba[2];
        double cmax = rgba[0] > rgba[1] ? rgba[0] : rgba[1];
        if (rgba[2] > cmax) cmax = rgba[2];
        double csum = cmax + cmin;
        double cdelta = cmax - cmin;
        double s;
        if (cdelta != 0.0) {
          s = (csum / 2.0 < 0.5) ? cdelta / csum : cdelta / (2.0 - csum);
        } else {
          s = 0.0;
        }
        dist[i] = s;
      }
      return (dist[0] < dist[1]) ? -1 : ((dist[0] > dist[1]) ? 1 : 0);
    }
  }

  // Compare colors by hue.
  private static class CompareByHue implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      Integer[] colors = new Integer[] {c1, c2};
      double[] dist = new double[colors.length];
      for (int i = 0; i < colors.length; i++) {
        double[] rgba = getNormalizedColor(colors[i]);
        double cmin = rgba[0] < rgba[1] ? rgba[0] : rgba[1];
        if (rgba[2] < cmin) cmin = rgba[2];
        double cmax = rgba[0] > rgba[1] ? rgba[0] : rgba[1];
        if (rgba[2] > cmax) cmax = rgba[2];
        double cdelta = cmax - cmin;
        double cdelta2 = cdelta / 2.0;
        double h;
        if (cdelta != 0.0) {
          double dr = ((cmax - rgba[2]) / 6.0 + cdelta2) / cdelta;
          double dg = ((cmax - rgba[1]) / 6.0 + cdelta2) / cdelta;
          double db = ((cmax - rgba[0]) / 6.0 + cdelta2) / cdelta;
          if (cmax == rgba[2]) {
            h = db - dg;
          } else if (cmax == rgba[1]) {
            h = 1.0/3.0 + dr - db;
          } else {
            h = 2.0/3.0 + dg - dr;
          }
          if (h < 0.0) { h += 1.0; }
          if (h > 1.0) { h -= 1.0; }
        } else {
          h = 0.0;
        }
        dist[i] = h;
      }
      return (dist[0] < dist[1]) ? -1 : ((dist[0] > dist[1]) ? 1 : 0);
    }
  }

  // Compare colors by red amount.
  private static class CompareByRed implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      int dist1 = (c1 >>> 16) & 0xff;
      int dist2 = (c2 >>> 16) & 0xff;
      return dist1 - dist2;
    }
  }

  // Compare colors by green amount.
  private static class CompareByGreen implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      int dist1 = (c1 >>> 8) & 0xff;
      int dist2 = (c2 >>> 8) & 0xff;
      return dist1 - dist2;
    }
  }

  // Compare colors by blue amount.
  private static class CompareByBlue implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      int dist1 = c1 & 0xff;
      int dist2 = c2 & 0xff;
      return dist1 - dist2;
    }
  }

  // Compare colors by alpha.
  private static class CompareByAlpha implements Comparator<Integer> {
    @Override
    public int compare(Integer c1, Integer c2)
    {
      int dist1 = (c1 >>> 24) & 0xff;
      int dist2 = (c2 >>> 24) & 0xff;
      return dist1 - dist2;
    }
  }
}
