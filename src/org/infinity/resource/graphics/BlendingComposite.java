// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.util.Objects;

/**
 * This composite class implements blending modes that simulates the blending modes
 * supported by the IE games.
 * Note: Blending is done in software and results in a noticeably performance penalty when used
 *       in combination with hardware-accelerated image data.
 */
public class BlendingComposite implements Composite
{
  public enum BlendingMode {
    /**
     * This mode applied the OpenGL blending operation:
     * src={@code GL_ONE_MINUS_DST_COLOR}, dst={@code GL_ONE}.
     */
    BRIGHTEST {
      @Override
      void blend(int[] src, int[] dst, int[] result) {
        result[0] = Math.min(255, (src[0] * (256 - dst[0]) + (dst[0] << 8)) >>> 8);
        result[1] = Math.min(255, (src[1] * (256 - dst[1]) + (dst[1] << 8)) >>> 8);
        result[2] = Math.min(255, (src[2] * (256 - dst[2]) + (dst[2] << 8)) >>> 8);
        result[3] = Math.min(255, (src[3] * (256 - dst[3]) + (dst[3] << 8)) >>> 8);
//        result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
      }
    },

    /**
     * This mode applied the OpenGL blending operation:
     * src={@code GL_DST_COLOR}, dst={@code GL_ONE}.
     */
    MULTIPLY {
      @Override
      void blend(int[] src, int[] dst, int[] result) {
        result[0] = Math.min(255, ((src[0] * dst[0]) + (dst[0] << 8)) >>> 8);
        result[1] = Math.min(255, ((src[1] * dst[1]) + (dst[1] << 8)) >>> 8);
        result[2] = Math.min(255, ((src[2] * dst[2]) + (dst[2] << 8)) >>> 8);
        result[3] = Math.min(255, ((src[3] * dst[3]) + (dst[3] << 8)) >>> 8);
//        result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
      }
    },

    /**
     * This mode applied the OpenGL blending operation:
     * src={@code GL_SRC_COLOR}, dst={@code GL_ONE}.
     */
    BRIGHTEST_MULTIPLY {
      @Override
      void blend(int[] src, int[] dst, int[] result) {
        result[0] = Math.min(255, ((src[0] * src[0]) + (dst[0] << 8)) >>> 8);
        result[1] = Math.min(255, ((src[1] * src[1]) + (dst[1] << 8)) >>> 8);
        result[2] = Math.min(255, ((src[2] * src[2]) + (dst[2] << 8)) >>> 8);
        result[3] = Math.min(255, ((src[3] * src[3]) + (dst[3] << 8)) >>> 8);
//        result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
      }
    };

    /**
     * Blends input colors {@code src} and {@code dst}, and writes the result to {@code result}.
     * @param src the source pixel (format: RGBA where R=0, ... A=3; all values in range [0, 255])
     * @param dst the destination pixel
     * @param result the blended pixel
     */
    abstract void blend(int[] src, int[] dst, int[] result);
  }

  /** Implements the following blending operations: src={@code GL_ONE_MINUS_DST_COLOR}, dst={@code GL_ONE}. */
  public static final BlendingComposite Brightest = new BlendingComposite(BlendingMode.BRIGHTEST);
  /** Implements the following blending operations: src={@code GL_DST_COLOR}, dst={@code GL_ONE}. */
  public static final BlendingComposite Multiply = new BlendingComposite(BlendingMode.MULTIPLY);
  /** Implements the following blending operations: src={@code GL_SRC_COLOR}, dst={@code GL_ONE}. */
  public static final BlendingComposite BrightestMultiply = new BlendingComposite(BlendingMode.BRIGHTEST_MULTIPLY);

  private final float alpha;
  private final BlendingMode mode;

  /**
   *
   * @param modes
   */
  public BlendingComposite(BlendingMode mode)
  {
    this(1.0f, mode);
  }

  /**
   *
   * @param alpha
   * @param modes
   */
  public BlendingComposite(float alpha, BlendingMode mode)
  {
    // filtering out null items
    this.mode = Objects.requireNonNull(mode, "Blending mode cannot be null");

    if (alpha < 0.0f || alpha > 1.0f) {
      throw new IllegalArgumentException("alpha value must be between 0.0 and 1.0");
    }

    this.alpha = alpha;
  }

  /**
   *
   * @param modes
   * @return
   */
  public static BlendingComposite getInstance(BlendingMode mode)
  {
    return new BlendingComposite(1.0f, mode);
  }

  /**
   *
   * @param alpha
   * @param modes
   * @return
   */
  public static BlendingComposite getInstance(float alpha, BlendingMode mode)
  {
    return new BlendingComposite(alpha, mode);
  }

  /**
   *
   * @param modes
   * @return
   */
  public BlendingComposite derive(BlendingMode mode)
  {
    return (mode == this.mode) ? this : new BlendingComposite(this.alpha, mode);
  }

  /**
   *
   * @param alpha
   * @return
   */
  public BlendingComposite derive(float alpha)
  {
    return (this.alpha == alpha) ? this : new BlendingComposite(this.alpha, this.mode);
  }

  /**
   *
   * @return
   */
  public float getAlpha()
  {
    return alpha;
  }

  /**
   *
   * @return
   */
  public BlendingMode getMode()
  {
    return this.mode;
  }

  @Override
  public int hashCode()
  {
    return Float.floatToIntBits(alpha) * 31 + mode.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof BlendingComposite)) {
      return false;
    }
    BlendingComposite bc = (BlendingComposite)obj;
    return alpha == bc.alpha && this.mode.equals(bc.mode);
  }

  private static boolean isColorModelRGB(ColorModel cm)
  {
    if (cm instanceof DirectColorModel && cm.getTransferType() == DataBuffer.TYPE_INT) {
      DirectColorModel dcm = (DirectColorModel)cm;
      return dcm.getRedMask() == 0x00ff0000 &&
             dcm.getGreenMask() == 0x0000ff00 &&
             dcm.getBlueMask() == 0x000000ff &&
             (dcm.getNumColorComponents() == 3 || dcm.getAlphaMask() == 0xff000000);
    }
    return false;
  }

  private static boolean isColorModelBGR(ColorModel cm)
  {
    if (cm instanceof DirectColorModel && cm.getTransferType() == DataBuffer.TYPE_INT) {
      DirectColorModel dcm = (DirectColorModel)cm;
      return dcm.getRedMask() == 0x000000ff &&
             dcm.getGreenMask() == 0x0000ff00 &&
             dcm.getBlueMask() == 0x00ff0000 &&
             (dcm.getNumColorComponents() == 3 || dcm.getAlphaMask() == 0xff000000);
    }
    return false;
  }

  @Override
  public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints)
  {
    Objects.requireNonNull(srcColorModel);
    Objects.requireNonNull(dstColorModel);

    if (isColorModelRGB(srcColorModel) && isColorModelRGB(dstColorModel)) {
      return new BlendingRGBContext(this);
    } else if (isColorModelBGR(srcColorModel) && isColorModelBGR(dstColorModel)) {
      return new BlendingBGRContext(this);
    }

    throw new RasterFormatException("Incompatible color models:\n  " + srcColorModel + "\n  " + dstColorModel);
  }

//-------------------------- INNER CLASSES --------------------------

  private static abstract class BlendingContext implements CompositeContext
  {
    protected final BlendingComposite composite;

    protected BlendingContext(BlendingComposite c)
    {
      this.composite = Objects.requireNonNull(c);
    }

    @Override
    public void dispose() { }
  }

  private static class BlendingRGBContext extends BlendingContext
  {
    public BlendingRGBContext(BlendingComposite c)
    {
      super(c);
    }

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
    {
      int width = Math.min(src.getWidth(), dstIn.getWidth());
      int height = Math.min(src.getHeight(), dstIn.getHeight());

      int alpha = (int)(composite.getAlpha() * 256.0f);

      int[] result = new int[4];
      int[] srcPixel = new int[4];
      int[] dstPixel = new int[4];
      int[] srcPixels = new int[width];
      int[] dstPixels = new int[width];

      for (int y = 0; y < height; y++) {
        src.getDataElements(0, y, width, 1, srcPixels);
        dstIn.getDataElements(0, y, width, 1, dstPixels);
        for (int x = 0; x < width; x++) {
          // transforming pixels: INT_ARGB -> array [R, G, B, A]
          int pixel = srcPixels[x];
          srcPixel[0] = (pixel >> 16) & 0xFF;
          srcPixel[1] = (pixel >>  8) & 0xFF;
          srcPixel[2] = (pixel      ) & 0xFF;
          srcPixel[3] = (pixel >> 24) & 0xFF;

          // transforming pixels: INT_ARGB -> array [R, G, B, A]
          pixel = dstPixels[x];
          dstPixel[0] = (pixel >> 16) & 0xFF;
          dstPixel[1] = (pixel >>  8) & 0xFF;
          dstPixel[2] = (pixel      ) & 0xFF;
          dstPixel[3] = (pixel >> 24) & 0xFF;

          // reusing results from previous blending operation as source
          composite.mode.blend(srcPixel, dstPixel, result);

          // mixing results and applying global alpha
          dstPixels[x] = ((dstPixel[3] + (result[3] - ((dstPixel[3] * alpha) >> 8))) & 0xFF) << 24 |
                         ((dstPixel[0] + (result[0] - ((dstPixel[0] * alpha) >> 8))) & 0xFF) << 16 |
                         ((dstPixel[1] + (result[1] - ((dstPixel[1] * alpha) >> 8))) & 0xFF) <<  8 |
                          (dstPixel[2] + (result[2] - ((dstPixel[2] * alpha) >> 8))) & 0xFF;
        }
        dstOut.setDataElements(0, y, width, 1, dstPixels);
      }
    }
  }

  private static class BlendingBGRContext extends BlendingContext
  {
    public BlendingBGRContext(BlendingComposite c)
    {
      super(c);
    }

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
    {
      int width = Math.min(src.getWidth(), dstIn.getWidth());
      int height = Math.min(src.getHeight(), dstIn.getHeight());

      int alpha = (int)(composite.getAlpha() * 256.0f);

      int[] result = new int[4];
      int[] srcPixel = new int[4];
      int[] dstPixel = new int[4];
      int[] srcPixels = new int[width];
      int[] dstPixels = new int[width];

      for (int y = 0; y < height; y++) {
        src.getDataElements(0, y, width, 1, srcPixels);
        dstIn.getDataElements(0, y, width, 1, dstPixels);
        for (int x = 0; x < width; x++) {
          // transforming pixels: INT_ABGR -> array [R, G, B, A]
          int pixel = srcPixels[x];
          srcPixel[0] = (pixel      ) & 0xFF;
          srcPixel[1] = (pixel >>  8) & 0xFF;
          srcPixel[2] = (pixel >> 16) & 0xFF;
          srcPixel[3] = (pixel >> 24) & 0xFF;

          // transforming pixels: INT_ABGR -> array [R, G, B, A]
          pixel = dstPixels[x];
          dstPixel[0] = (pixel      ) & 0xFF;
          dstPixel[1] = (pixel >>  8) & 0xFF;
          dstPixel[2] = (pixel >> 16) & 0xFF;
          dstPixel[3] = (pixel >> 24) & 0xFF;

          // reusing results from previous blending operation as source
          composite.mode.blend(srcPixel, dstPixel, result);

          // mixing results and applying global alpha
          dstPixels[x] = ((dstPixel[3] + (result[3] - ((dstPixel[3] * alpha) >> 8))) & 0xFF) << 24 |
                         ((dstPixel[0] + (result[0] - ((dstPixel[0] * alpha) >> 8))) & 0xFF) << 16 |
                         ((dstPixel[1] + (result[1] - ((dstPixel[1] * alpha) >> 8))) & 0xFF) <<  8 |
                          (dstPixel[2] + (result[2] - ((dstPixel[2] * alpha) >> 8))) & 0xFF;
        }
        dstOut.setDataElements(0, y, width, 1, dstPixels);
      }
    }
  }

}
