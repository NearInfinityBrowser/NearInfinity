// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Contains a set of color-related static methods (little endian order only).
 * @author argent77
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
   * @param transparency The transparency type (either one of OPAQUE, BITMASK or TRANSLUCENT)
   * @return A new BufferedImage object with the specified properties.
   */
  public static BufferedImage createCompatibleImage(int width, int height, int transparency)
  {
    // obtain the current system's graphical settings
    final GraphicsConfiguration gfxConfig =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

    return gfxConfig.createCompatibleImage(width, height, transparency);
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
        try {
          // the main purpose of this method is direct access to the underlying data buffer
          BufferedImage image = (BufferedImage)img;
          int[] tmp = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
          if (tmp == null)
            throw new Exception();
          tmp = null;
          return image;
        } catch (Exception e) {
        }
      }
      final BufferedImage image = createCompatibleImage(img.getWidth(null), img.getHeight(null),
                                                        hasTransparency);
      Graphics2D g = (Graphics2D)image.getGraphics();
      g.drawImage(img, 0, 0, null);
      g.dispose();
      return image;
    }
    return null;
  }

  /**
   * Attempts to retrieve the width and height of the specified image file without loading it completely.
   * @param fileName The image filename.
   * @return The image dimensions.
   */
  public static Dimension getImageDimension(String fileName)
  {
    Dimension d = new Dimension();
    try {
      ImageInputStream iis = ImageIO.createImageInputStream(new File(fileName));
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
      iis.close();
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
   * @return <code>true</code> if the conversion finished successfully, <code>false</code> otherwise.
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
   * @param ignoreAlpha If <code>false</code>, only visible color values (alpha > 0) will be counted.
   * @return An array containing the resulting colors, or <code>null</code> on error.
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
   * @param ignoreAlpha If <code>false</code>, only visible color values (alpha > 0) will be counted.
   * @return <code>true</code> if color reduction succeeded, <code>false</code> otherwise.
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
