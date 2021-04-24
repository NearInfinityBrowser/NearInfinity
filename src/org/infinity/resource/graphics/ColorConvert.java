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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Triple;

/**
 * Contains a set of color-related static methods (little endian order only).
 */
public class ColorConvert
{
  /**
   * A fast but somewhat inaccurate algorithm for calculating a distance between two ARGB values.
   * It uses predefined weight values for each color component to calculate the distance.
   */
  public static final ColorDistanceFunc COLOR_DISTANCE_ARGB = (argb1, argb2, weight) -> {
    int a1 = (argb1 >> 24) & 0xff;
    int r1 = (argb1 >> 16) & 0xff;
    int g1 = (argb1 >> 8) & 0xff;
    int b1 = argb1 & 0xff;
    if (a1 != 0xff) {
      r1 = r1 * a1 / 255;
      g1 = g1 * a1 / 255;
      b1 = b1 * a1 / 255;
    }

    int a2 = (argb2 >> 24) & 0xff;
    int r2 = (argb2 >> 16) & 0xff;
    int g2 = (argb2 >> 8) & 0xff;
    int b2 = argb2 & 0xff;
    if (a2 != 0xff) {
      r2 = r2 * a2 / 255;
      g2 = g2 * a2 / 255;
      b2 = b2 * a2 / 255;
    }

    weight = Math.max(0.0, Math.min(2.0, weight));
    double da = (double)(a1 - a2) * 48.0 * weight;
    double dr = (double)(r1 - r2) * 14.0;
    double dg = (double)(g1 - g2) * 28.0;
    double db = (double)(b1 - b2) * 6.0;
    return Math.sqrt(da*da + dr*dr + dg*dg + db*db);
  };

  /**
   * Returns the distance between the two ARGB values using CIELAB colorspace and CIE94 formula.
   * This algorithm is slower than the default ARGB distance calculation but more accurate.
   */
  public static final ColorDistanceFunc COLOR_DISTANCE_CIE94 = (argb1, argb2, weight) -> {
    Triple<Double, Double, Double> lab1 = convertRGBtoLab(argb1);
    Triple<Double, Double, Double> lab2 = convertRGBtoLab(argb2);
    weight = Math.max(0.0, Math.min(2.0, weight));
    double alpha1 = (double)((argb1 >> 24) & 0xff) * weight;
    double alpha2 = (double)((argb2 >> 24) & 0xff) * weight;
    return getColorDistanceLabCIE94(lab1.getValue0().doubleValue(), lab1.getValue1().doubleValue(), lab1.getValue2().doubleValue(), alpha1,
                                    lab2.getValue0().doubleValue(), lab2.getValue1().doubleValue(), lab2.getValue2().doubleValue(), alpha2);
  };

  // Cache for ARGB key -> CIELAB color space values
  private static final HashMap<Integer, Triple<Double, Double, Double>> ARGB_LAB_CACHE = new HashMap<>();

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
    Alpha,      // Sort by alpha component.
    Lab,        // Sort by CIELAB L component.
  }

  public static void clearCache()
  {
    ARGB_LAB_CACHE.clear();
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

//  /**
//   * Calculates the nearest color available in the given RGBA palette for the specified color.
//   * @param rgbColor The source color in ARGB format.
//   * @param rgbPalette A palette containing ARGB color entries.
//   * @param ignoreAlpha Whether to exclude alpha component from the calculation.
//   * @return The palette index pointing to the nearest color, or -1 on error.
//   */
//  public static int nearestColorRGB(int rgbColor, int[] rgbPalette, boolean ignoreAlpha)
//  {
//    // TODO: Improve match quality for grayscaled colors
//    int index = -1;
//    if (rgbPalette != null && rgbPalette.length > 0) {
//      int mask = ignoreAlpha ? 0 : 0xff000000;
//      int minDist = Integer.MAX_VALUE;
//      int a = ((rgbColor & mask) >> 24) & 0xff;
//      int r = (rgbColor >> 16) & 0xff;
//      int g = (rgbColor >> 8) & 0xff;
//      int b = rgbColor & 0xff;
//
//      int da, dr, dg, db;
//      for (int i = 0; i < rgbPalette.length; i++) {
//        int col = rgbPalette[i];
//        // Extra check for full transparency
//        if (a == 0) {
//          if ((col & 0xff000000) == 0) { return i; }
//          if (col == 0xff00ff00) { return i; }
//        }
//        // Computing weighted distance to compensate for perceived color differences
//        int a2 = ((rgbPalette[i] & mask) >> 24) & 0xff;
//        int r2 = (rgbPalette[i] >> 16) & 0xff;
//        int g2 = (rgbPalette[i] >> 8) & 0xff;
//        int b2 = rgbPalette[i] & 0xff;
//        da = (a - a2) * 48;
//        dr = (r - r2) * 14;
//        dg = (g - g2) * 28;
//        db = (b - b2) * 6;
//        int dist = da*da + dr*dr + dg*dg + db*db;
//        if (dist < minDist) {
//          minDist = dist;
//          index = i;
//        }
//      }
//    }
//    return index;
//  }

  /**
   * Calculates the nearest color available in the given palette using the specified color distance function.
   * @param argb the reference ARGB color.
   * @param palette palette with ARGB colors to search.
   * @param alphaWeight Weight factor of the alpha component. Supported range: [0.0, 2.0].
   *                    A value < 1.0 makes alpha less important for the distance calculation.
   *                    A value > 1.0 makes alpha more important for the distance calculation.
   *                    Specify 1.0 to use the unmodified alpha compomponent for the calculation.
   *                    Specify 0.0 to ignore the alpha part in the calculation.
   * @param calculator the function for distance calculation. Choose one of the predefined functions or specify a custom
   *                   instance. Specify {@code null} to use the fastest (but slightly inaccurate) distance calculation.
   * @return Palette index pointing to the nearest color value. Returns -1 if color entry could not be determined.
   */
  public static int getNearestColor(int argb, int[] palette, double alphaWeight, ColorDistanceFunc calculator)
  {
    int retVal = -1;
    if (palette == null) {
      return retVal;
    }

    if (calculator == null) {
      calculator = COLOR_DISTANCE_ARGB;
    }
    alphaWeight = Math.max(0.0, Math.min(2.0, alphaWeight));
    double minDist = Double.MAX_VALUE;
    for (int i = 0; i < palette.length; i++) {
      double dist = calculator.calculate(argb, palette[i], alphaWeight);
      if (dist < minDist) {
        minDist = dist;
        retVal = i;
      }
    }

    return retVal;
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
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByLightness);
          break;
        case Saturation:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareBySaturation);
          break;
        case Hue:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByHue);
          break;
        case Red:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByRed);
          break;
        case Green:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByGreen);
          break;
        case Blue:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByBlue);
          break;
        case Alpha:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByAlpha);
          break;
        case Lab:
          Arrays.sort(tmpColors, startIndex, tmpColors.length, CompareByLabL);
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

  /**
   * Converts a single RGB value into the CIELAB colorspace.
   * @param argb The ARGB value to convert.
   * @return the converted color value in CIELAB colorspace. Order: L, a, b, alpha
   *         where L range is [0.0, 100.0], a and b are open-ended (usually between -150 and 150).
   */
  public static Triple<Double, Double, Double> convertRGBtoLab(int argb)
  {
    Integer key = Integer.valueOf(argb & 0xffffff);
    Triple<Double, Double, Double> retVal = ARGB_LAB_CACHE.get(key);

    if (retVal == null) {
      int alpha = (argb >> 24) & 0xff;
      int red = (argb >> 16) & 0xff;
      int green = (argb >> 8) & 0xff;
      int blue = argb & 0xff;
      if (alpha != 255) {
        red = red * alpha / 255;
        green = green * alpha / 255;
        blue = blue * alpha / 255;
      }

      // 1. Linearize RGB
      double r = (double)red / 255.0;
      double g = (double)green / 255.0;
      double b = (double)blue / 255.0;

      // 2. Convert to CIEXYZ
      r = (r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92;
      g = (g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92;
      b = (b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92;
      r *= 100.0;
      g *= 100.0;
      b *= 100.0;
      double x = (r * 0.4124) + (g * 0.3576) + (b * 0.1805);
      double y = (r * 0.2126) + (g * 0.7152) + (b * 0.0722);
      double z = (r * 0.0193) + (g * 0.1192) + (b * 0.9505);

      // 3. Convert to Lab
      x /= 95.047;
      y /= 100.0;
      z /= 108.883;
      x = (x > 0.008856) ? Math.pow(x, 1.0 / 3.0) : (7.787 * x) + (16.0 / 116.0);
      y = (y > 0.008856) ? Math.pow(y, 1.0 / 3.0) : (7.787 * y) + (16.0 / 116.0);
      z = (z > 0.008856) ? Math.pow(z, 1.0 / 3.0) : (7.787 * z) + (16.0 / 116.0);
      Double dstL = Double.valueOf((116.0 * y) - 16.0);
      Double dstA = Double.valueOf(500.0 * (x - y));
      Double dstB = Double.valueOf(200.0 * (y - z));
      retVal = Triple.with(dstL, dstA, dstB);

      ARGB_LAB_CACHE.put(key, retVal);
    }

    return retVal;
  }

  /**
   * Converts a color entry in CIELAB colorspace into an RGB value. Alpha part is set to 0.
   * @param L the L (lightness) value in range [0.0, 100.0].
   * @param a the a axis (green-red) value.
   * @param b the b axis (blue-yellow) value.
   * @return RGB value where blue is located in the lowest byte, followed by green and red. Highest byte is set to 0.
   */
  public static int convertLabToRGB(double L, double a, double b)
  {
    // 1. Convert to CIEXYZ
    double y = (L + 16.0) / 116.0;
    double x = (a / 500.0) + y;
    double z = y - (b / 200.0);

    double d = Math.pow(y, 3.0);
    y = (d > 0.008856) ? d : (y - (16.0 / 116.0)) / 7.787;
    d = Math.pow(x, 3.0);
    x = (d > 0.008856) ? d : (x - (16.0 / 116.0)) / 7.787;
    d = Math.pow(z, 3.0);
    z = (d > 0.008856) ? d : (z - (16.0 / 116.0)) / 7.787;

    x *= 95.047;
    y *= 100.0;
    z *= 108.883;

    // 2. Convert to linear RGB
    x /= 100.0;
    y /= 100.0;
    z /= 100.0;
    double red = (x * 3.2406) + (y * -1.5372) + (z * -0.4986);
    double green = (x * -0.9689) + (y * 1.8758) + (z *  0.0415);
    double blue = (x * 0.0557) + (y * -0.2040) + (z * 1.0570);

    red = (red > 0.0031308) ? 1.055 * Math.pow(red, 1.0 / 2.4) - 0.055 : 12.92 * red;
    green = (green > 0.0031308) ? 1.055 * Math.pow(green, 1.0 / 2.4) - 0.055 : 12.92 * green;
    blue = (blue > 0.0031308) ? 1.055 * Math.pow(blue, 1.0 / 2.4) - 0.055 : 12.92 * blue;

    // convert to non-linear RGB
    int retVal = 0;
    retVal |= Math.max(0, Math.min(255, (int)(red * 255.0)));
    retVal <<= 8;
    retVal |= Math.max(0, Math.min(255, (int)(green * 255.0)));
    retVal <<= 8;
    retVal |= Math.max(0, Math.min(255, (int)(blue * 255.0)));

    return retVal;
  }

  /**
   * Converts a ARGB palette to a palette in CIELAB colorspace. The returned array contains four components per
   * color entry: L, a, b and alpha. L range is [0.0, 100.0], a and b are open-ended (usually between -150 and 150),
   * alpha range is [0.0, 255.0].
   * @param palette Palette with ARGB color entries
   * @return array with CIELAB color entries plus alpha component.
   */
  public static double[] convertRGBtoLabPalette(int[] palette)
  {
    double[] retVal = null;
    if (palette == null) {
      return retVal;
    }

    retVal = new double[palette.length * 4];
    for (int i = 0; i < palette.length; i++) {
      int a = (palette[i] >> 24) & 0xff;
      Triple<Double, Double, Double> entry = convertRGBtoLab(palette[i]);
      retVal[i * 4] = entry.getValue0().doubleValue();
      retVal[i * 4 + 1] = entry.getValue1().doubleValue();
      retVal[i * 4 + 2] = entry.getValue2().doubleValue();
      retVal[i * 4 + 3] = (double)a;
    }

    return retVal;
  }

  /**
   * Converts a palette in CIELAB colorspace plus alpha into a ARGB palette.
   * @param palette Palette with L, a, b and alpha component per color entry.
   * @return array with ARGB color entries packed into single integer per entry.
   */
  public static int[] convertLabToRGBPalette(double[] palette)
  {
    int[] retVal = null;
    if (palette == null) {
      return retVal;
    }

    retVal = new int[palette.length / 4];
    for (int i = 0; i < retVal.length; i++) {
      double L = palette[i * 4];
      double a = palette[i * 4 + 1];
      double b = palette[i * 4 + 2];

      int rgba = convertLabToRGB(L, a, b);
      int alpha = Math.max(0, Math.min(255, (int)palette[i * 4 + 3]));
      rgba |= (alpha << 24);
      retVal[i] = rgba;
    }

    return retVal;
  }

  /** Calculates the distance between two CIELAB colors based on CIE94 formula. */
  public static double getColorDistanceLabCIE94(double L1, double a1, double b1, double alpha1,
                                                double L2, double a2, double b2, double alpha2)
  {
    final double kl = 1.0;
    final double k1 = 0.045;
    final double k2 = 0.015;

    double deltaL = L1 - L2;
    double deltaA = a1 - a2;
    double deltaB = b1 - b2;

    double c1 = Math.sqrt(a1*a1 + b1*b1);
    double c2 = Math.sqrt(a2*a2 + b2*b2);
    double deltaC = c1 - c2;

    double deltaH = deltaA*deltaA + deltaB*deltaB - deltaC*deltaC;
    deltaH = (deltaH < 0.0) ? 0.0 : Math.sqrt(deltaH);

    double sc = 1.0 + k1*c1;
    double sh = 1.0 + k2*c1;

    double i = Math.pow(deltaL / kl, 2.0) +
               Math.pow(deltaC / sc, 2.0) +
               Math.pow(deltaH / sh, 2.0) +
               (alpha1 - alpha2)*(alpha1 - alpha2);

    return (i < 0.0) ? 0.0 : Math.sqrt(i);
  }

  /**
   * Returns the distance between the two ARGB values using CIELAB colorspace and CIE94 formula.
   * @param argb1 the first ARGB color entry.
   * @param argb2 the second ARGB color entry.
   * @param alphaWeight Weight factor of the alpha component. Supported range: [0.0, 2.0].
   *                    A value < 1.0 makes alpha less important for the distance calculation.
   *                    A value > 1.0 makes alpha more important for the distance calculation.
   *                    Specify 0.0 to ignore the alpha part in the calculation.
   */
  public static double getRGBColorDistanceLabCIE94(int argb1, int argb2, double alphaWeight)
  {
    Triple<Double, Double, Double> lab1 = convertRGBtoLab(argb1);
    Triple<Double, Double, Double> lab2 = convertRGBtoLab(argb2);
    alphaWeight = Math.max(0.0, Math.min(2.0, alphaWeight));
    double alpha1 = (double)((argb1 >> 24) & 0xff) * alphaWeight;
    double alpha2 = (double)((argb2 >> 24) & 0xff) * alphaWeight;
    return getColorDistanceLabCIE94(lab1.getValue0().doubleValue(), lab1.getValue1().doubleValue(), lab1.getValue2().doubleValue(), alpha1,
                                    lab2.getValue0().doubleValue(), lab2.getValue1().doubleValue(), lab2.getValue2().doubleValue(), alpha2);
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
      final PriorityQueue<PixelBlock> blockQueue =
          new PriorityQueue<>(desiredColors, PixelBlock.PixelBlockComparator);

      final Pixel[] p = new Pixel[pixels.length];
      int mask = ignoreAlpha ? 0xff000000: 0;
      for (int i = 0; i < p.length; i++) {
        p[i] = new Pixel(pixels[i] | mask);
      }

      final PixelBlock initialBlock = new PixelBlock(p);
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
        final PixelBlock block1 = new PixelBlock(longestBlock.getPixels(), ofsBegin, ofsMedian - ofsBegin);
        final PixelBlock block2 = new PixelBlock(longestBlock.getPixels(), ofsMedian, ofsEnd - ofsMedian);
        block1.shrink();
        block2.shrink();
        blockQueue.add(block1);
        blockQueue.add(block2);
      }

      int palIndex = 0;
      while (!blockQueue.isEmpty() && palIndex < desiredColors) {
        final PixelBlock block = blockQueue.poll();
        final int[] sum = {0, 0, 0, 0};
        for (int i = 0; i < block.size(); i++) {
          for (int j = 0; j < Pixel.MAX_SIZE; j++) {
            sum[j] += block.getPixel(i).getElement(j);
          }
        }
        final Pixel avgPixel = new Pixel();
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
    return loadPaletteBMP(new FileResourceEntry(file));
  }

  /**
   * Attempts to load a palette from the specified Windows BMP file.
   * @param entry The BMP resource entry to extract the palette from.
   * @return The palette as ARGB integers.
   * @throws Exception on error.
   */
  public static int[] loadPaletteBMP(ResourceEntry entry) throws Exception
  {
    if (entry != null) {
      try (InputStream is = entry.getResourceDataAsStream()) {
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
            for (int i = 0; i < colorCount; i++) {
              retVal[i] = DynamicArray.getInt(palette, i << 2);
              if ((retVal[i] & 0xff000000) == 0) {
                retVal[i] |= 0xff000000;
              }
            }
            return retVal;
          } else {
            throw new Exception("Error loading palette: " + entry.getResourceName());
          }
        } else {
          throw new Exception("Invalid BMP resource: " + entry.getResourceName());
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read BMP resource: " + entry.getResourceName());
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
    if (file != null && FileEx.create(file).isFile()) {
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
    if (file != null && FileEx.create(file).isFile()) {
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
    if (file != null && FileEx.create(file).isFile()) {
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
    return loadPaletteBAM(new FileResourceEntry(file), preserveAlpha);
  }

  public static int[] loadPaletteBAM(ResourceEntry entry, boolean preserveAlpha) throws Exception
  {
    if (entry != null) {
      try (InputStream is = entry.getResourceDataAsStream()) {
        byte[] signature = new byte[8];
        is.read(signature);
        String s = new String(signature);
        if ("BAM V1  ".equals(s) || "BAMCV1  ".equals(s)) {
          byte[] bamData = new byte[(int)entry.getResourceSize()];
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
            throw new Exception("Error loading palette: " + entry.getResourceName());
          }
        } else {
          throw new Exception("Unsupport file type.");
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new Exception("Unable to read BAM resource: " + entry.getResourceName());
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

  /**
   * Represents a function to calculate the distance between two ARGB color values.
   */
  public interface ColorDistanceFunc
  {
    /**
     * Performs a calculation to determine the distance between the specified ARGB color values. The third argument
     * indicates how much influence the alpha component should have.
     * @param argb1 the first ARGB color value.
     * @param argb2 the second ARGB color value
     * @param alphaWeight Weight factor of the alpha component. Supported range: [0.0, 2.0].
     *                    A value < 1.0 makes alpha less important for the distance calculation.
     *                    A value > 1.0 makes alpha more important for the distance calculation.
     *                    Specify 0.0 to completely ignore the alpha part in the calculation.
     * @return the relative distance between the two color values.
     */
    double calculate(int argb1, int argb2, double alphaWeight);
  }

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
      minCorner = new Pixel(0);
      maxCorner = new Pixel(0xffffffff);
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

    public static final Comparator<PixelBlock> PixelBlockComparator = (pb1, pb2) -> {
      // inverting natural order by switching sides
      return pb2.getLongestSideLength() - pb1.getLongestSideLength();
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

    public static final List<Comparator<Pixel>> PixelComparator = new ArrayList<Comparator<Pixel>>(MAX_SIZE) {{
      add((p1, p2) -> p1.getElement(0) - p2.getElement(0));
      add((p1, p2) -> p1.getElement(1) - p2.getElement(1));
      add((p1, p2) -> p1.getElement(2) - p2.getElement(2));
      add((p1, p2) -> p1.getElement(3) - p2.getElement(3));
    }};
  }


  // Compare colors by perceived lightness.
  private static final Comparator<Integer> CompareByLightness = (c1, c2) -> {
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
  };

  // Compare colors by saturation.
  private static final Comparator<Integer> CompareBySaturation = (c1, c2) -> {
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
  };

  // Compare colors by hue.
  private static final Comparator<Integer> CompareByHue = (c1, c2) -> {
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
  };

  // Compare colors by red amount.
  private static final Comparator<Integer> CompareByRed = (c1, c2) -> {
      int dist1 = (c1 >>> 16) & 0xff;
      int dist2 = (c2 >>> 16) & 0xff;
      return dist1 - dist2;
  };

  // Compare colors by green amount.
  private static final Comparator<Integer> CompareByGreen = (c1, c2) -> {
      int dist1 = (c1 >>> 8) & 0xff;
      int dist2 = (c2 >>> 8) & 0xff;
      return dist1 - dist2;
  };

  // Compare colors by blue amount.
  private static final Comparator<Integer> CompareByBlue = (c1, c2) -> {
      int dist1 = c1 & 0xff;
      int dist2 = c2 & 0xff;
      return dist1 - dist2;
  };

  // Compare colors by alpha.
  private static final Comparator<Integer> CompareByAlpha = (c1, c2) -> {
    int dist1 = (c1 >>> 24) & 0xff;
    int dist2 = (c2 >>> 24) & 0xff;
    return dist1 - dist2;
  };

  // Compare colors by CIELAB L component.
  private static final Comparator<Integer> CompareByLabL = (c1, c2) -> {
    Triple<Double, Double, Double> dist1 = convertRGBtoLab(c1);
    Triple<Double, Double, Double> dist2 = convertRGBtoLab(c2);
    if (dist1.getValue0() < dist2.getValue0()) {
      return -1;
    } else if (dist1.getValue0() > dist2.getValue0()) {
      return 1;
    } else {
      return 0;
    }
//    int dist1 = (c1 >>> 24) & 0xff;
//    int dist2 = (c2 >>> 24) & 0xff;
//    return dist1 - dist2;
  };
}
