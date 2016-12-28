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
      dstImage = new BufferedImage(cm, raster, isAlphaPreMultiplied, null);
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
   * Calculates the nearest color available in the palette for the specified color value.
   * Note: For better color matching results the palette is expected in HCL color model.
   * @param rgbColor The source color value in ARGB format (A is ignored).
   * @param hclPalette A HCL palette with the available color entries. Use the method
   *                   {@link #toHclPalette(int[], int[])} to convert a RGB palette into the HCL format.
   * @return The palette index pointing to the nearest color, or -1 on error.
   */
  public static int nearestColor(int rgbColor, int[] hclPalette)
  {
    int index = -1;
    if (hclPalette != null && hclPalette.length > 0) {
      int distance = Integer.MAX_VALUE;
      int v = rgbToHcl(rgbColor);
      int h = (byte)((v >>> 16) & 0xff), s = (byte)((v >>> 8) & 0xff), l = (byte)(v & 0xff);
      for (int i = 0; i < hclPalette.length; i++) {
        int h2 = (byte)((hclPalette[i] >>> 16) & 0xff);
        int s2 = (byte)((hclPalette[i] >>> 8) & 0xff);
        int l2 = (byte)(hclPalette[i] & 0xff);
        int dh = (h2 - h);
        int ds = (s2 - s);
        int dl = (l2 - l);
        int curDistance = dh*dh + ds*ds + dl*dl;
        if (curDistance < distance) {
          distance = curDistance;
          index = i;
          if (distance == 0)
            break;
        }
      }
    }
    return index;
  }

  /**
   * Converts an array of colors from the RGB color model into the HCL color model. This is needed
   * if you want to use the method {@link #nearestColor(int, int[])}.
   * @param rgbPalette The source RGB palette.
   * @param hclPalette An array to store the resulting HCL colors into.
   * @return {@code true} if the conversion finished successfully, {@code false} otherwise.
   */
  public static boolean toHclPalette(int[] rgbPalette, int[] hclPalette)
  {
    if (rgbPalette != null && hclPalette != null && hclPalette.length >= rgbPalette.length) {
      for (int i = 0; i < rgbPalette.length; i++) {
        hclPalette[i] = rgbToHcl(rgbPalette[i]);
      }
      return true;
    }
    return false;
  }

  /**
   * Converts an RGB color value into a normalized HCL color (hue, chroma, luminance).
   * @param color The color value in ARGB format (A is ignored).
   * @return The normalized HCL representation of the color. Range of each component: [-128..127]
   */
  public static int rgbToHcl(int color)
  {
    // using HCL (hue, chrome, luminance) approach
    float r = (float)((color >>> 16) & 0xff) / 255.0f;
    float g = (float)((color >>> 8) & 0xff) / 255.0f;
    float b = (float)(color & 0xff) / 255.0f;
    float cmax = r; if (g > cmax) cmax = g; if (b > cmax) cmax = b;
    float cmin = r; if (g < cmin) cmin = g; if (b < cmin) cmin = b;
    float cdelta = cmax - cmin;
    float h, c, l;

    l = (cmax + cmin) / 2.0f;
    if (l < 0.0f) l = 0.0f; else if (l > 1.0f) l = 1.0f;

    if (cdelta == 0.0f) {
      h = 0.0f;
      c = 0.0f;
    } else {
      c = cdelta;

      final float cdelta2 = cdelta / 2.0f;
      float dr = (((cmax - r) / 6.0f) + cdelta2) / cdelta;
      float dg = (((cmax - g) / 6.0f) + cdelta2) / cdelta;
      float db = (((cmax - b) / 6.0f) + cdelta2) / cdelta;

      final float c13 = 1.0f/3.0f;
      final float c23 = 2.0f/3.0f;
      if (r == cmax) {
        h = db - dg;
      } else if (g == cmax) {
        h = c13 + dr - db;
      } else {
        h = c23 + dg - dr;
      }

      if (h < 0.0f) h += 1.0f;
      if (h > 1.0f) h -= 1.0f;
    }

    // normalizing: h = [0..2], c = [0..1], l = [-1..1]
    h *= 2.0f;
    l = (l - 0.5f) * 2.0f;

    double x= c * Math.cos(h*Math.PI);
    double y = c * Math.sin(h*Math.PI);
    double z = l;

    // re-normalizing values for conversion into integer range [-128..127]
    x = Math.floor(x * 127.5);
    y = Math.floor(y * 127.5);
    z = Math.floor(z * 127.5);

    return (((int)x & 0xff) << 16) | (((int)y & 0xff) << 8) | ((int)z & 0xff);
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
      if (ignoreAlpha) {
        p = new Pixel[pixels.length];
        for (int i = 0; i < p.length; i++) {
          p[i] = new Pixel(pixels[i]);
        }
      } else {
        int len = 0;
        for (int i = 0; i < pixels.length; i++) {
          if ((pixels[i] & 0xff000000) != 0)
            len++;
        }
        p = new Pixel[len];
        for (int i = 0, idx = 0; i < pixels.length && idx < len; i++) {
          if ((pixels[i] & 0xff000000) != 0)
            p[idx++] = new Pixel(pixels[i]);
        }
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
        int[] sum = {0, 0, 0};
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
              retVal[i] = DynamicArray.getInt(palette, i << 2) & 0x00ffffff;
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
   * Attempts to load a palette from the specified Windows PAL file.
   * @param file The Windows PAL file to load.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPalettePAL(Path file) throws Exception
  {
    if (file != null && Files.isRegularFile(file)) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        byte[] signature = new byte[8];
        is.read(signature);
        if ("RIFF".equals(new String(signature, 0, 4))) {
          // extracting palette from Windows palette file
          byte[] signature2 = new byte[8];
          is.read(signature2);
          if ("PAL data".equals(new String(signature2))) {
            byte[] header = new byte[8];
            is.read(header);
            int numColors = DynamicArray.getUnsignedShort(header, 6);
            if (numColors >= 2 && numColors <= 256) {
              byte[] palData = new byte[numColors << 2];
              is.read(palData);
              int[] retVal = new int[numColors];
              for (int i = 0; i < numColors; i++) {
                int col = DynamicArray.getInt(palData, i << 2);
                retVal[i] = ((col << 16) & 0xff0000) | (col & 0x00ff00) | ((col >> 16) & 0x0000ff);
              }
              return retVal;
            } else {
              throw new Exception("Invalid number of color entries in Windows palette file " + file.getFileName());
            }
          } else {
            throw new Exception("Error loading palette from Windows palette file " + file.getFileName());
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
   * Attempts to load a palette from the specified Adobe Color Table file.
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
            is.skip(2);
            transColor = is.read();
          }
          for (int ofs = 0, i = 0; i < count; i++, ofs += 3) {
            if (i == transColor) {
              retVal[i] = 0x00ff00;
            } else {
              retVal[i] = ((palData[ofs] & 0xff) << 16) | ((palData[ofs+1] & 0xff) << 8) | (palData[ofs+2] & 0xff);
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
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPaletteBAM(Path file) throws Exception
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
              retVal[i] = DynamicArray.getInt(bamData, ofs+(i << 2)) & 0x00ffffff;
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
      minCorner = new Pixel(Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE);
      maxCorner = new Pixel(Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE);
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
      int m = Integer.MIN_VALUE, maxIndex = -1;
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
    public static final int MAX_SIZE = 3;
    public final byte[] color;

    public Pixel()
    {
      this.color = new byte[]{0, 0, 0};
    }

    public Pixel(int color)
    {
      this.color = new byte[]{(byte)((color >>> 16) & 0xff),
                              (byte)((color >>> 8) & 0xff),
                              (byte)(color & 0xff)};
    }

    public Pixel(byte r, byte g, byte b)
    {
      this.color = new byte[]{r, g, b};
    }

    public int toColor()
    {
      return ((color[0] & 0xff) << 16) | ((color[1] & 0xff) << 8) | (color[2] & 0xff);
    }

    public int getElement(int index)
    {
      if (index >= 0 && index < MAX_SIZE) {
        return (color[index] & 0xff);
      }  else {
        return 0;
      }
    }

    public static List<Comparator<Pixel>> PixelComparator = new ArrayList<Comparator<Pixel>>(3);
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
    }
  }
}
