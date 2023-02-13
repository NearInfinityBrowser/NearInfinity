// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2023 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics.decoder;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Objects;

/**
 * Texture decoder for ETC1 and ETC2 pixel formats, implemented according to the
 * <em>Khronos Data Format Specification</em>.
 *
 * @see <a href="https://registry.khronos.org/DataFormat/">Khronos Data Format Specification Registry</a>
 */
public class Etc2Decoder implements Decodable {
  // Combined modifier table for base colors in
  // - "individual" and "differential" modes, used by format ETC2_RGB
  // - "differential" mode, used by format ETC2_RGB_A1
  // Values have been rearranged (compared to reference tables) to allow
  // indexing values without additional mapping.
  // Important:
  // For ETC2_RGB, bit 3 is always 1.
  // For ETC2_RGB_A1, bit 3 of the table index is borrowed from the "opaque" bit of the source data.
  private static final int[][] MODIFIERS = {
      {  0,  8,    0,   -8 },
      {  0,  17,   0,  -17 },
      {  0,  29,   0,  -29 },
      {  0,  42,   0,  -42 },
      {  0,  60,   0,  -60 },
      {  0,  80,   0,  -80 },
      {  0, 106,   0, -106 },
      {  0, 183,   0, -183 },
      {  2,  8,   -2,   -8 },
      {  5,  17,  -5,  -17 },
      {  9,  29,  -9,  -29 },
      { 13,  42, -13,  -42 },
      { 18,  60, -18,  -60 },
      { 24,  80, -24,  -80 },
      { 33, 106, -33, -106 },
      { 47, 183, -47, -183 },
  };

  // Distance table for "T" and "H" modes.
  private static final int[] DISTANCES = {
      3, 6, 11, 16, 23, 32, 41, 64,
  };

  // Intensity modifier table for alpha values.
  private static final int[][] MODIFIERS_ALPHA = {
      { -3, -6,  -9, -15, 2, 5, 8, 14 },
      { -3, -7, -10, -13, 2, 6, 9, 12 },
      { -2, -5,  -8, -13, 1, 4, 7, 12 },
      { -2, -4,  -6, -13, 1, 3, 5, 12 },
      { -3, -6,  -8, -12, 2, 5, 7, 11 },
      { -3, -7,  -9, -11, 2, 6, 8, 10 },
      { -4, -7,  -8, -11, 3, 6, 7, 10 },
      { -3, -5,  -8, -11, 2, 4, 7, 10 },
      { -2, -6,  -8, -10, 1, 5, 7,  9 },
      { -2, -5,  -8, -10, 1, 4, 7,  9 },
      { -2, -4,  -8, -10, 1, 3, 7,  9 },
      { -2, -5,  -7, -10, 1, 4, 6,  9 },
      { -3, -4,  -7, -10, 2, 3, 6,  9 },
      { -1, -2,  -3, -10, 0, 1, 2,  9 },
      { -4, -6,  -8,  -9, 3, 5, 7,  8 },
      { -3, -5,  -7,  -9, 2, 4, 6,  8 },
  };

  // 2-Bit offset mappings for pixel lookup indices, as used by "individual", "differential", "T" and "H" modes.
  private static final int[][] BITS_PIXEL_INDICES = {
      {0, 16}, {1, 17}, {2, 18}, {3, 19},
      {4, 20}, {5, 21}, {6, 22}, {7, 23},
      {8, 24}, {9, 25}, {10, 26}, {11, 27},
      {12, 28}, {13, 29}, {14, 30}, {15, 31},
  };

  // (offset, length)-pairs for 3-bit pixel lookup indices, as used for alpha computation.
  // The outer array refers to ofs/len pairs for pixels Pa .. Pp
  // (as described in the "Khronos Data Format Specification").
  private static final int[][] BITS_ALPHA_RANGE = {
      {45, 3}, {42, 3}, {39, 3}, {36, 3},
      {33, 3}, {30, 3}, {27, 3}, {24, 3},
      {21, 3}, {18, 3}, {15, 3}, {12, 3},
      {9, 3}, {6, 3}, {3, 3}, {0, 3},
  };

  // 3-Bit offset mappings for "distance" in "T" mode.
  private static final int[] BITS_T_D = { 32, 34, 35 };
  // 4-Bit offset mappings for red color in "T" mode.
  private static final int[] BITS_T_R = { 56, 57, 59, 60 };

  // 2-Bit offset mappings for "distance" in "H" mode.
  private static final int[] BITS_H_D = { 32, 34 };
  // 4-Bit offset mappings for red color in "H" mode.
  private static final int[] BITS_H_B = { 47, 48, 49, 51 };
  // 4-Bit offset mappings for green color in "H" mode.
  private static final int[] BITS_H_G = { 52, 56, 57, 58 };

  // 6-Bit offset mappings for horizontal red color in "planar" mode.
  private static final int[] BITS_PLANAR_RH = { 32, 34, 35, 36, 37, 38 };
  // 6-Bit offset mappings for blue color in "planar" mode.
  private static final int[] BITS_PLANAR_B  = { 39, 40, 41, 43, 44, 48 };
  // 7-Bit offset mappings for green color in "planar" mode.
  private static final int[] BITS_PLANAR_G  = { 49, 50, 51, 52, 53, 54, 56 };


  private final PvrInfo info;

  /** Initializes a new {@code ETC2} decoder from with the specified {@link PvrInfo}. */
  public Etc2Decoder(PvrInfo pvr) {
    this.info = Objects.requireNonNull(pvr);
  }

  // --------------------- Begin Interface Decodable ---------------------

  @Override
  public boolean decode(BufferedImage image, Rectangle region) throws Exception {
    return decodeETC(image, region);
  }

  @Override
  public PvrInfo getPvrInfo() {
    return info;
  }

  // --------------------- End Interface Decodable ---------------------

  /**
   * Decodes PVR data in ETC1 and ETC2 formats as specified by the associated {@link PvrInfo},
   * and draws the specified "region" into "image".
   *
   * @param image  The output image
   * @param region The PVR texture region to draw onto "image"
   * @return Success state of the operation.
   * @throws Exception on error.
   */
  private boolean decodeETC(BufferedImage image, Rectangle region) throws Exception {
    if (image == null || region == null) {
      return false;
    }

    final int imgWidth = image.getWidth();
    final int imgHeight = image.getHeight();
    int[] imgData = null;

    // checking region bounds and alignment
    if (region.x < 0) {
      region.width += -region.x;
      region.x = 0;
    }
    if (region.y < 0) {
      region.height += -region.y;
      region.y = 0;
    }
    if (region.x + region.width > info.width)
      region.width = info.width - region.x;
    if (region.y + region.height > info.height)
      region.height = info.height - region.y;
    final Rectangle rect = alignRectangle(region, 4, 4);

    // preparing aligned image buffer for faster rendering
    BufferedImage alignedImage = null;
    final int imgWidthAligned;
    if (!region.equals(rect)) {
      alignedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
      imgWidthAligned = alignedImage.getWidth();
      imgData = ((DataBufferInt) alignedImage.getRaster().getDataBuffer()).getData();
      // translating "region" to be relative to "rect"
      region.x -= rect.x;
      region.y -= rect.y;
      if (imgWidth < region.width) {
        region.width = imgWidth;
      }
      if (imgHeight < region.height) {
        region.height = imgHeight;
      }
    } else {
      imgWidthAligned = imgWidth;
      imgData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    switch (info.pixelFormat) {
      case ETC1:
      case ETC2_RGB:
        decodeData(imgData, rect, imgWidthAligned, false, false);
        break;
      case ETC2_RGB_A1:
        decodeData(imgData, rect, imgWidthAligned, false, true);
        break;
      case ETC2_RGBA:
        decodeData(imgData, rect, imgWidthAligned, true, false);
        break;
      default:
        return false;
    }
    imgData = null;

    // copying aligned image back to target image
    if (alignedImage != null) {
      Graphics2D g = image.createGraphics();
      try {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        g.drawImage(alignedImage, 0, 0, region.width, region.height, region.x, region.y, region.x + region.width,
            region.y + region.height, null);
      } finally {
        g.dispose();
        g = null;
      }
      alignedImage = null;
    }
    return true;
  }

  /**
   * Performs decoding of PVR data in ETC1, ETC2 RGB, ETC2 RGB A1 or ETC2 RGBA pixel format on {@code imgData},
   * within the aligned bounds specified by {@code rect} and {@code imgWidth}.
   *
   * @param imgData The output image data array
   * @param rect Defines the rectangular region of pixels to decode from the texture.
   * @param imgWidth Width of the output image, in pixels.
   * @param hasAlpha Indicates whether the texture data provides an additional alpha channel.
   * @param hasTransparency Indicates whether the texture data provides 1 bit punch-through alpha.
   */
  private void decodeData(int[] imgData, Rectangle rect, int imgWidth, boolean hasAlpha, boolean hasTransparency) {
    final int wordSize = hasAlpha ? 2 : 1;        // data size of an encoded 4x4 pixel block, in number of long ints
    final int wordImageWidth = info.width >> 2;   // the image width, in data blocks
    final int wordRectWidth = rect.width >> 2;    // the aligned region's width, in data blocks
    final int wordRectHeight = rect.height >> 2;  // the aligned region's height, in data blocks
    final int wordPosX = rect.x >> 2;             // X coordinate of region's origin, in data blocks
    final int wordPosY = rect.y >> 2;             // Y coordinate of region's origin, in data blocks

    // Storage for resulting pixels as 4x4xRGBA-quadruplet matrix.
    // As defined by the "Khronos Data Format Specification", the pixels run top to bottom, left to right.
    // X and Y coordinates are transposed later when copying pixels to target image.
    int[] pixels = new int[4 * 4 * 4];

    long colorWord = 0, alphaWord = 0;
    int pvrOfs = (wordPosY * wordImageWidth + wordPosX) * wordSize;
    final LongBuffer longBuf = ByteBuffer.wrap(info.data).order(ByteOrder.BIG_ENDIAN).asLongBuffer();
    for (int y = 0; y < wordRectHeight; y++) {
      longBuf.position(pvrOfs);
      for (int x = 0; x < wordRectWidth; x++) {
        // decoding single ETC data block
        if (hasAlpha) {
          // fetching alpha data
          alphaWord = longBuf.get();
        }

        // decoding color part
        colorWord = longBuf.get();
        decodeColorBlock(colorWord, pixels, hasTransparency);

        if (hasAlpha) {
          // decoding alpha part
          decodeAlphaBlock(alphaWord, pixels);
        }

        // writing pixel block to image data
        int imgOfs = (y << 2) * imgWidth + (x << 2);
        for (int i = 0; i < 16; i++) {
          final int py = i >> 2;
          final int px = i & 3;
          // using transposed coordinates to compensate for pixel order
          final int ci = ((px << 2) | py) << 2;
          final int c = colorToInt(pixels, ci, 4);
          final int ofs = imgOfs + py * imgWidth + px;
          imgData[ofs] = c;
        }
      }

      pvrOfs += wordImageWidth * wordSize;
    }
  }

  /**
   * Decodes the input 64-bit word into color values with optional punch-through alpha, and writes them into
   * the specified 4x4 matrix of RGBA pixels.
   *
   * @param code 64-bit code word with color information.
   * @param outPixels A 4x4 BGRA pixel array for decoded pixel data. Color and alpha channels are stored as separate values.
   * @param hasTransparency Indicates whether the code word provides 1 bit "punch-through" alpha.
   */
  private void decodeColorBlock(long code, int[] outPixels, boolean hasTransparency) {
    final boolean diff = isBitSet(code, 33);
    final int rd = getBits(code, 56, 3, true), r = getBits(code, 59, 5, false);
    final int gd = getBits(code, 48, 3, true), g = getBits(code, 51, 5, false);
    final int bd = getBits(code, 40, 3, true), b = getBits(code, 43, 5, false);

    if (!hasTransparency && !diff) {
      // Individual mode
      final int[] baseColor1 = {
          getBits(code, 44, 4, false),  // b
          getBits(code, 52, 4, false),  // g
          getBits(code, 60, 4, false),  // r
      };
      extend4To8Bits(baseColor1, 0, 3);
      final int[] baseColor2 = {
          getBits(code, 40, 4, false),  // b
          getBits(code, 48, 4, false),  // g
          getBits(code, 56, 4, false),  // r
      };
      extend4To8Bits(baseColor2, 0, 3);
      decodeColorSubBlocks(code, baseColor1, baseColor2, outPixels, false);
    } else if (((r + rd) & ~31) != 0) { // outside [0..31]?
      // Mode T
      decodeColorModeT(code, outPixels, hasTransparency);
    } else if (((g + gd) & ~31) != 0) { // outside [0..31]?
      // Mode H
      decodeColorModeH(code, outPixels, hasTransparency);
    } else if (((b + bd) & ~31) != 0) { // outside [0..31]?
      // Planar mode
      decodeColorPlanar(code, outPixels);
    } else {
      // Differential mode
      final int[] baseColor1 = { b, g, r };
      extend5To8Bits(baseColor1, 0, 3);
      final int[] baseColor2 = { b + bd, g + gd, r + rd };
      extend5To8Bits(baseColor2, 0, 3);
      decodeColorSubBlocks(code, baseColor1, baseColor2, outPixels, hasTransparency);
    }
  }

  /**
   * Decodes the input 64-bit word in either "individual" or "differential" mode.
   *
   * @param code 64-bit code word with color information.
   * @param baseColor1 Base color for first sub-block, as BGR array.
   * @param baseColor2 Base color for second sub-block, as BGR array.
   * @param outPixels A 4x4 BGRA pixel array for decoded pixel data. Color and alpha channels are stored as separate values.
   * @param hasTransparency Indicates whether the code word provides 1 bit "punch-through" alpha.
   */
  private void decodeColorSubBlocks(long code, int[] baseColor1, int[] baseColor2, int[] outPixels, boolean hasTransparency) {
    final boolean flipped = isBitSet(code, 32);
    final boolean opaque = hasTransparency ? isBitSet(code, 33) : true;
    final int[] table = { getBits(code, 37, 3, false), getBits(code, 34, 3, false) };

    final int table1Idx= (hasTransparency ? 0 : 1 << 3) | table[0];
    final int table2Idx= (hasTransparency ? 0 : 1 << 3) | table[1];
    final int[][] blockTable = { MODIFIERS[table1Idx], MODIFIERS[table2Idx] };

    final int[][] baseColor = { baseColor1, baseColor2 };

    for (int i = 0; i < 16; i++) {
      final int pixelIdx = getBitsEx(code, false, BITS_PIXEL_INDICES[i]);
      final int pofs = i << 2;  // pixel offset in outPixels array
      if (!opaque && pixelIdx == 2) { // punch-through alpha
        outPixels[pofs]     = 0;
        outPixels[pofs + 1] = 0;
        outPixels[pofs + 2] = 0;
        outPixels[pofs + 3] = 0;
      } else {  // opaque color
        // using transposed coordinates to compensate for pixel order
        final int py = i & 3;     // pixel x coordinate
        final int px = i >> 2;   // pixel y coordinate
        // flipped:     4x2 sub-blocks, on top of each other
        // not flipped: 2x4 sub-blocks, side-by-side
        final int blockIdx = flipped ? (py >> 1) : (px >> 1);

        final int modifier  = blockTable[blockIdx][pixelIdx];
        outPixels[pofs]     = clamp255(baseColor[blockIdx][0] + modifier);  // b
        outPixels[pofs + 1] = clamp255(baseColor[blockIdx][1] + modifier);  // g
        outPixels[pofs + 2] = clamp255(baseColor[blockIdx][2] + modifier);  // r
        outPixels[pofs + 3] = 255;  // alpha
      }
    }
  }

  /**
   * Decodes the input 64-bit word in "T" mode.
   *
   * @param code 64-bit code word with color information.
   * @param outPixels A 4x4 BGRA pixel array for decoded pixel data. Color and alpha channels are stored as separate values.
   * @param hasTransparency Indicates whether the code word provides 1 bit "punch-through" alpha.
   */
  private void decodeColorModeT(long code, int[] outPixels, boolean hasTransparency) {
    final int distIdx = getBitsEx(code, false, BITS_T_D);
    final boolean opaque = hasTransparency ? isBitSet(code, 33) : true;
    final int[] baseColor1 = {
        getBits(code, 48, 4, false),      // b
        getBits(code, 52, 4, false),      // g
        getBitsEx(code, false, BITS_T_R), // r
    };
    extend4To8Bits(baseColor1, 0, 3);

    final int[] baseColor2 = {
        getBits(code, 36, 4, false),  // b
        getBits(code, 40, 4, false),  // g
        getBits(code, 44, 4, false),  // r
    };
    extend4To8Bits(baseColor2, 0, 3);

    final int dist= DISTANCES[distIdx];

    final int[] baseColor2a = {
        clamp255(baseColor2[0] + dist),
        clamp255(baseColor2[1] + dist),
        clamp255(baseColor2[2] + dist)
    };
    final int[] baseColor2b = {
        clamp255(baseColor2[0] - dist),
        clamp255(baseColor2[1] - dist),
        clamp255(baseColor2[2] - dist)
    };

    final int[][] paintColor = { baseColor1, baseColor2a, baseColor2, baseColor2b };

    for (int i = 0; i < 16; i++) {
      final int pofs = i << 2;  // pixel offset in outPixels array
      final int pixelIdx = getBitsEx(code, false, BITS_PIXEL_INDICES[i]);
      if (!opaque && pixelIdx == 2) {
        // punch-through alpha
        outPixels[pofs]     = 0;
        outPixels[pofs + 1] = 0;
        outPixels[pofs + 2] = 0;
        outPixels[pofs + 3] = 0;
      } else {
        // opaque color
        outPixels[pofs]     = paintColor[pixelIdx][0];  // b
        outPixels[pofs + 1] = paintColor[pixelIdx][1];  // g
        outPixels[pofs + 2] = paintColor[pixelIdx][2];  // r
        outPixels[pofs + 3] = 255;  // alpha
      }
    }
  }

  /**
   * Decodes the input 64-bit word in "H" mode.
   *
   * @param code 64-bit code word with color information.
   * @param outPixels A 4x4 BGRA pixel array for decoded pixel data. Color and alpha channels are stored as separate values.
   * @param hasTransparency Indicates whether the code word provides 1 bit "punch-through" alpha.
   */
  private void decodeColorModeH(long code, int[] outPixels, boolean hasTransparency) {
    final int dIdx = getBitsEx(code, false, BITS_H_D);
    final boolean opaque = hasTransparency ? isBitSet(code, 33) : true;
    final int[] baseColor1 = {
        getBitsEx(code, false, BITS_H_B), // b
        getBitsEx(code, false, BITS_H_G), // g
        getBits(code, 59, 4, false),      // r
    };
    extend4To8Bits(baseColor1, 0, 3);

    final int[] baseColor2 = {
        getBits(code, 35, 4, false),  // b
        getBits(code, 39, 4, false),  // g
        getBits(code, 43, 4, false),  // r
    };
    extend4To8Bits(baseColor2, 0, 3);

    final int color1 = colorToInt(baseColor1, 0, 3);
    final int color2 = colorToInt(baseColor2, 0, 3);
    final int distIdx = (dIdx << 1) | (color1 >= color2 ? 1 : 0);
    final int dist = DISTANCES[distIdx];

    final int[] baseColor1a = {
        clamp255(baseColor1[0] + dist),
        clamp255(baseColor1[1] + dist),
        clamp255(baseColor1[2] + dist),
    };
    final int[] baseColor1b = {
        clamp255(baseColor1[0] - dist),
        clamp255(baseColor1[1] - dist),
        clamp255(baseColor1[2] - dist),
    };
    final int[] baseColor2a = {
        clamp255(baseColor2[0] + dist),
        clamp255(baseColor2[1] + dist),
        clamp255(baseColor2[2] + dist),
    };
    final int[] baseColor2b = {
        clamp255(baseColor2[0] - dist),
        clamp255(baseColor2[1] - dist),
        clamp255(baseColor2[2] - dist),
    };

    final int[][] paintColor = new int[][] { baseColor1a, baseColor1b, baseColor2a, baseColor2b };

    for (int i = 0; i < 16; i++) {
      final int pofs = i << 2;  // pixel offset in outPixels array
      final int pixelIdx = getBitsEx(code, false, BITS_PIXEL_INDICES[i]);
      if (!opaque && pixelIdx == 2) {
        // punch-through alpha
        outPixels[pofs]     = 0;
        outPixels[pofs + 1] = 0;
        outPixels[pofs + 2] = 0;
        outPixels[pofs + 3] = 0;
      } else {
        // opaque color
        outPixels[pofs]     = paintColor[pixelIdx][0];  // b
        outPixels[pofs + 1] = paintColor[pixelIdx][1];  // g
        outPixels[pofs + 2] = paintColor[pixelIdx][2];  // r
        outPixels[pofs + 3] = 255;  // alpha
      }
    }
  }

  /**
   * Decodes the input 64-bit word in "planar" mode. This mode does not support 1 bit "punch-through" alpha.
   *
   * @param code 64-bit code word with color information.
   * @param outPixels A 4x4 BGRA pixel array for decoded pixel data. Color and alpha channels are stored as separate values.
   */
  private void decodeColorPlanar(long code, int[] outPixels) {
    final int[] color = {
        getBitsEx(code, false, BITS_PLANAR_B),  // b
        getBitsEx(code, false, BITS_PLANAR_G),  // g
        getBits(code, 57, 6, false),            // r
    };
    extend676To8Bits(color, 0, 3);

    final int[] colorH = {
        getBits(code, 19, 6, false),            // b
        getBits(code, 25, 7, false),            // g
        getBitsEx(code, false, BITS_PLANAR_RH), // r
    };
    extend676To8Bits(colorH, 0, 3);

    final int[] colorV = {
        getBits(code, 0, 6, false),   // b
        getBits(code, 6, 7, false),   // g
        getBits(code, 13, 6, false),  // r
    };
    extend676To8Bits(colorV, 0, 3);

    final int[] outColor = new int[3];
    for (int i = 0; i < 16; i++) {
      // using transposed coordinates to compensate for pixel order
      final int x = i >> 2;
      final int y = i & 3;
      final int pofs = i << 2;  // pixel offset in outPixels array
      interpolate(x, y, color, colorH, colorV, outColor);
      outPixels[pofs]     = outColor[0];  // b
      outPixels[pofs + 1] = outColor[1];  // g
      outPixels[pofs + 2] = outColor[2];  // r
      outPixels[pofs + 3] = 255;  // alpha
    }
  }

  /**
   * Decodes the input 64-bit word into alpha values and writes them into the specified 4x4 matrix of RGBA pixels.
   *
   * @param code 64-bit code word with alpha information.
   * @param outPixels A 4x4 BGRA pixel array for decoded pixel data. Color and alpha channels are stored as separate values.
   */
  private void decodeAlphaBlock(long code, int[] outPixels) {
    final int tableIdx = getBits(code, 48, 4, false);
    final int multiplier = getBits(code, 52, 4, false);
    final int base = getBits(code, 56, 8, false);

    for (int i = 0; i < 16; i++) {
      final int pixelIdx = getBits(code, BITS_ALPHA_RANGE[i][0], BITS_ALPHA_RANGE[i][1], false);
      final int modifier = MODIFIERS_ALPHA[tableIdx][pixelIdx];
      final int alpha = clamp255(base + modifier * multiplier);
      outPixels[(i << 2) + 3] = alpha;
    }
  }

  /** Returns the specified value, clamped between 0 and 255. */
  private static int clamp255(int value) {
    return Math.min(Math.max(value, 0), 255);
  }

  /**
   * Attempts to extract the bit at specified {@code ofs} and returns it as boolean.
   *
   * @param value A 64-bit source value containing the bit to evaluate.
   * @param ofs The bit offset, in range 0..63.
   * @return A {@code boolean} which indicates whether the bit at the specified position is set. Returns {@code false}
   *         if the offset is out of bounds.
   */
  private static boolean isBitSet(long value, int ofs) {
    if (ofs >= 0 && ofs < 64) {
      long mask = 1L << ofs;
      return (value & mask) != 0;
    } else {
      return false;
    }
  }

  /**
   * Attempts to extract {@code len} number of bits, starting at {@code ofs} and expand it to full {@code int} size.
   *
   * @param value A 64-bit source value containing the bits to extract.
   * @param ofs Starting bit offset.
   * @param len Number of bits to extract. Allowed range: [0, 32].
   * @param signed Specifies whether the extracted bits should be sign-extended.
   * @return The extracted bits as {@code int} value.
   */
  private static int getBits(long value, int ofs, int len, boolean signed) {
    len = Math.min(64, ofs + len) - ofs;
    if (ofs < 0 || len <= 0 || len > 32 || ofs + len > 64) {
      return 0;
    }

    int mask = (1 << len) - 1;
    int maskMsb = 1 << (len - 1);
    int retVal = (int)(value >>> ofs) & mask;
    if (signed && (retVal & maskMsb) != 0) {
      retVal |= ~mask;
    }

    return retVal;
  }

  /**
   * Attempts to extracts all the bits at positions specified by the {@code offsets} array and expand it
   * to full {@code int} size.
   *
   * @param value A 64-bit source value containing the bits to extract.
   * @param signed Specifies whether the extracted bits should be sign-extended.
   * @param offsets Array of bit positions which are sequentially used to assemble the resulting value.
   * @return The extracted bits as {@code int} value.
   */
  private static int getBitsEx(long value, boolean signed, int[] offsets) {
    if (offsets.length > 32 || offsets.length == 0) {
      return 0;
    }

    int retVal = 0;
    for (int i = offsets.length - 1; i >= 0; i--) {
      retVal <<= 1;
      retVal |= (value >>> offsets[i]) & 1;
    }

    int maskMsb = 1 << (offsets.length - 1);
    if (signed && (retVal & maskMsb) != 0) {
      int mask = (1 << offsets.length) - 1;
      retVal |= ~mask;
    }

    return retVal;
  }

  /**
   * Extends the values in the {@code values} array from RGB:444 to RGB:888 by mirroring the most significant bits
   * to the lower bit positions. The result is written back to the {@code values} array.
   *
   * @param values Array with color values to extend. Each entry specifies a separate color channel.
   * @param offset Start offset in the {@code value} array.
   * @param count Number of values to extend.
   */
  private static void extend4To8Bits(int[] values, int offset, int count) {
    for (int i = offset, len = offset + Math.min(values.length - offset, count); i < len; i++) {
      values[i] = ((values[i] << 4) | (values[i] & 0x0f)) & 0xff;
    }
  }

  /**
   * Extends the values in the {@code values} array from RGB:555 to RGB:888 by mirroring the most significant bits
   * to the lower bit positions. The result is written back to the {@code values} array.
   *
   * @param values Array with color values to extend. Each entry specifies a separate color channel.
   * @param offset Start offset in the {@code value} array.
   * @param count Number of values to extend.
   */
  private static void extend5To8Bits(int[] values, int offset, int count) {
    for (int i = offset, len = offset + Math.min(values.length - offset, count); i < len; i++) {
      values[i] = ((values[i] << 3) | ((values[i] & 0x1f) >> 2)) & 0xff;
    }
  }

  /**
   * Extends the values in the {@code values} array from RGB:676 to RGB:888 by mirroring the most significant bits
   * to the lower bit positions. The result is written back to the {@code values} array.
   *
   * @param values Array with color values to extend. Each entry specifies a separate color channel.
   *               Index of color value, relative to {@code offset} is used to determine if value is a 6-bit value
   *               (red, blue) or 7-bit value (green).
   * @param offset Start offset in the {@code value} array.
   * @param count Number of values to extend.
   */
  private static void extend676To8Bits(int[] values, int offset, int count) {
    for (int i = offset, len = offset + Math.min(values.length - offset, count); i < len; i++) {
      if ((i - offset) % 3 == 1) {
        // 7 to 8
        values[i] = ((values[i] << 1) | ((values[i] & 0x7f) >> 6)) & 0xff;
      } else {
        // 6 to 8
        values[i] = ((values[i] << 2) | ((values[i] & 0x3f) >> 4)) & 0xff;
      }
    }
  }

  /**
   * Converts the given color array into a combined integer of BGR(A) bytes, in the order from highest to lowest byte:
   * (alpha), red, green, blue.
   *
   * @param color The color triplet as array in sequence { B, G, R, A }.
   * @param offset Start offset in the {@code color} array.
   * @param count Number of values to combine.
   * @return The combined color as {@code int}.
   */
  private static int colorToInt(int[] color, int offset, int count) {
    int retVal = 0;
    for (int i = offset + count - 1; i >= offset; i--) {
      retVal <<= 8;
      retVal |= color[i]  & 0xff;
    }
    return retVal;
  }

  /**
   * Computes the interpolated color in a 4x4 matrix, based on the specified coordinates and input colors,
   * and stores the result in the {@code outColor} array.
   *
   * @param x Column of the pixel, in range [0..3].
   * @param y Row of the pixel, in range [0..3].
   * @param c Color value as array of three ints (blue, green, red).
   * @param ch Horizontal color modifier as array of three ints (blue, green, red).
   * @param cv Vertical color modifier as array of three ints (blue, green, red).
   * @param oc Storage for the interpolated color with enough space for three values (blue, green, red).
   */
  private static void interpolate(int x, int y, int[] c, int[] ch, int[] cv, int[] oc) {
    for (int i = 0; i < 3; i++) {
      oc[i] = clamp255((x * (ch[i] - c[i]) + y * (cv[i] - c[i]) + 4 * c[i] + 2) >> 2);
    }
  }

  /** Returns a rectangle that is aligned to the values specified as arguments 2 and 3. */
  private static Rectangle alignRectangle(Rectangle rect, int alignX, int alignY) {
    if (rect == null)
      return null;

    Rectangle retVal = new Rectangle(rect);
    if (alignX < 1)
      alignX = 1;
    if (alignY < 1)
      alignY = 1;
    if (rect.x < 0) {
      rect.width -= -rect.x;
      rect.x = 0;
    }
    if (rect.y < 0) {
      rect.height -= -rect.y;
      rect.y = 0;
    }

    int diffX = retVal.x % alignX;
    if (diffX != 0) {
      retVal.x -= diffX;
      retVal.width += diffX;
    }
    int diffY = retVal.y % alignY;
    if (diffY != 0) {
      retVal.y -= diffY;
      retVal.height += diffY;
    }

    diffX = (alignX - (retVal.width % alignX)) % alignX;
    retVal.width += diffX;

    diffY = (alignY - (retVal.height % alignY)) % alignY;
    retVal.height += diffY;

    return retVal;
  }
}
