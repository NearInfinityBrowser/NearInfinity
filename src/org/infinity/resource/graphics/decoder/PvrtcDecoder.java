// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics.decoder;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.infinity.util.DynamicArray;

/**
 * Texture decoder for PVRTC pixel formats.
 */
public class PvrtcDecoder implements Decodable {
  // The local cache list for decoded PVR textures. The "key" has to be a unique PvrInfo structure.
  private static final Map<PvrInfo, BufferedImage> TEXTURE_CACHE = Collections
      .synchronizedMap(new LinkedHashMap<>());

  // The max. number of cache entries to hold
  private static final int MAX_CACHE_ENTRIES = 8;

  // Datatypes as used in the reference implementation:
  // Pixel32/128S: int[]{red, green, blue, alpha}
  // PVRTCWord: int[]{modulation, color}
  // PVRTCWordIndices: int[]{p0, p1, q0, q1, r0, r1, s0, s1}

  // color channel indices into an array of pixel values
  private static final int CH_R = 0;
  private static final int CH_G = 1;
  private static final int CH_B = 2;
  private static final int CH_A = 3;
  // start indices into an array of PVRTC blocks
  private static final int IDX_P = 0;
  private static final int IDX_Q = 2;
  private static final int IDX_R = 4;
  private static final int IDX_S = 6;
  // indices into a PVRTC data block
  private static final int BLK_MOD = 0;
  private static final int BLK_COL = 1;

  private final PvrInfo info;

  /** Initializes a new {@code PVRTC} decoder from with the specified {@link PvrInfo}. */
  public PvrtcDecoder(PvrInfo pvr) {
    this.info = Objects.requireNonNull(pvr);
  }

  // --------------------- Begin Interface Decodable ---------------------

  @Override
  public boolean decode(BufferedImage image, Rectangle region) throws Exception {
    switch (info.pixelFormat) {
      case PVRTC_2BPP_RGB:
      case PVRTC_2BPP_RGBA:
        return decodePVRT(image, region, true);
      case PVRTC_4BPP_RGB:
      case PVRTC_4BPP_RGBA:
        return decodePVRT(image, region, false);
      default:
        return false;
    }
  }

  @Override
  public PvrInfo getPvrInfo() {
    return info;
  }

  // --------------------- End Interface Decodable ---------------------

  // Decodes both 2bpp and 4bpp versions of the PVRT format
  private boolean decodePVRT(BufferedImage image, Rectangle region, boolean is2bpp)
      throws Exception {
    if (image == null || region == null) {
      return false;
    }

    int imgWidth = image.getWidth();
    int imgHeight = image.getHeight();
    int[] imgData;

    // bounds checking
    if (region.x < 0) {
      region.width -= region.x;
      region.x = 0;
    }
    if (region.y < 0) {
      region.height -= region.y;
      region.y = 0;
    }
    if (region.x + region.width > info.width)
      region.width = info.width - region.x;
    if (region.y + region.height > info.height)
      region.height = info.height - region.y;

    // preparing image buffer for faster rendering
    BufferedImage alignedImage = getCachedImage(info);
    if (alignedImage == null) {
      if (!region.equals(new Rectangle(0, 0, info.width, info.height))) {
        alignedImage = new BufferedImage(info.width, info.height, BufferedImage.TYPE_INT_ARGB);
        imgData = ((DataBufferInt) alignedImage.getRaster().getDataBuffer()).getData();
        if (imgWidth < region.width) {
          region.width = imgWidth;
        }
        if (imgHeight < region.height) {
          region.height = imgHeight;
        }
      } else {
        imgData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
      }

      int wordWidth = is2bpp ? 8 : 4;
      int wordHeight = 4;
      int numXWords = info.width / wordWidth;
      int numYWords = info.height / wordHeight;
      int[] indices = new int[8];
      int[] p = new int[2], q = new int[2], r = new int[2], s = new int[2];
      int[][] pixels = new int[wordWidth * wordHeight][4];

      for (int wordY = -1; wordY < numYWords - 1; wordY++) {
        for (int wordX = -1; wordX < numXWords - 1; wordX++) {
          indices[IDX_P] = wrapWordIndex(numXWords, wordX);
          indices[IDX_P + 1] = wrapWordIndex(numYWords, wordY);
          indices[IDX_Q] = wrapWordIndex(numXWords, wordX + 1);
          indices[IDX_Q + 1] = wrapWordIndex(numYWords, wordY);
          indices[IDX_R] = wrapWordIndex(numXWords, wordX);
          indices[IDX_R + 1] = wrapWordIndex(numYWords, wordY + 1);
          indices[IDX_S] = wrapWordIndex(numXWords, wordX + 1);
          indices[IDX_S + 1] = wrapWordIndex(numYWords, wordY + 1);

          // work out the offsets into the twiddle structs, multiply by two as there are two members per word
          int[] wordOffsets = new int[] { twiddleUV(numXWords, numYWords, indices[IDX_P], indices[IDX_P + 1]) << 1,
              twiddleUV(numXWords, numYWords, indices[IDX_Q], indices[IDX_Q + 1]) << 1,
              twiddleUV(numXWords, numYWords, indices[IDX_R], indices[IDX_R + 1]) << 1,
              twiddleUV(numXWords, numYWords, indices[IDX_S], indices[IDX_S + 1]) << 1 };

          // access individual elements to fill out input words
          p[BLK_MOD] = DynamicArray.getInt(info.data, wordOffsets[0] << 2);
          p[BLK_COL] = DynamicArray.getInt(info.data, (wordOffsets[0] + 1) << 2);
          q[BLK_MOD] = DynamicArray.getInt(info.data, wordOffsets[1] << 2);
          q[BLK_COL] = DynamicArray.getInt(info.data, (wordOffsets[1] + 1) << 2);
          r[BLK_MOD] = DynamicArray.getInt(info.data, wordOffsets[2] << 2);
          r[BLK_COL] = DynamicArray.getInt(info.data, (wordOffsets[2] + 1) << 2);
          s[BLK_MOD] = DynamicArray.getInt(info.data, wordOffsets[3] << 2);
          s[BLK_COL] = DynamicArray.getInt(info.data, (wordOffsets[3] + 1) << 2);

          // assemble four words into struct to get decompressed pixels from
          getDecompressedPixels(p, q, r, s, pixels, is2bpp);
          mapDecompressedData(imgData, info.width, pixels, indices, is2bpp);
        }
      }
      imgData = null;
      registerCachedImage(info, alignedImage);
    } else {
      if (imgWidth < region.width) {
        region.width = imgWidth;
      }
      if (imgHeight < region.height) {
        region.height = imgHeight;
      }
    }

    // rendering aligned image to target image
    if (alignedImage != null) {
      Graphics2D g = image.createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
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

  // Decodes the first color in a PVRT data word
  private static int[] getColorA(int colorData) {
    int[] retVal = new int[4];
    if ((colorData & 0x8000) != 0) {
      // opaque color mode: RGB554
      retVal[CH_R] = (colorData & 0x7c00) >>> 10; // red: 5->5 bits
      retVal[CH_G] = (colorData & 0x3e0) >>> 5; // green: 5->5 bits
      retVal[CH_B] = (colorData & 0x1e) | ((colorData & 0x1e) >>> 4); // blue: 4->5 bits
      retVal[CH_A] = 0x0f; // alpha: 0->4 bits
    } else {
      // transparent color mode: ARGB3443
      retVal[CH_R] = ((colorData & 0xf00) >>> 7) | ((colorData & 0xf00) >>> 11); // red: 4->5 bits
      retVal[CH_G] = ((colorData & 0xf0) >>> 3) | ((colorData & 0xf0) >>> 7); // green: 4->5 bits
      retVal[CH_B] = ((colorData & 0xe) << 1) | ((colorData & 0xe) >>> 2); // blue: 3->5 bits
      retVal[CH_A] = ((colorData & 0x7000) >>> 11); // alpha: 3->4 bits
    }
    return retVal;
  }

  // Decodes the second color in a PVRT data word
  private static int[] getColorB(int colorData) {
    int[] retVal = new int[4];
    if ((colorData & 0x80000000) != 0) {
      // opaque color mode: RGB555
      retVal[CH_R] = (colorData & 0x7c000000) >>> 26; // red: 5->5 bits
      retVal[CH_G] = (colorData & 0x3e00000) >>> 21; // green: 5->5 bits
      retVal[CH_B] = (colorData & 0x1f0000) >>> 16; // blue: 5->5 bits
      retVal[CH_A] = 0x0f; // alpha: 0->4 bits
    } else {
      // transparent color mode: ARGB3444
      retVal[CH_R] = ((colorData & 0xf000000) >>> 23) | ((colorData & 0xf000000) >>> 27); // red: 4->5 bits
      retVal[CH_G] = ((colorData & 0xf00000) >>> 19) | ((colorData & 0xf00000) >>> 23); // green: 4->5 bits
      retVal[CH_B] = ((colorData & 0xf0000) >>> 15) | ((colorData & 0xf0000) >>> 19); // blue: 4->5 bits
      retVal[CH_A] = ((colorData & 0x70000000) >>> 27); // alpha: 3->4 bits
    }
    return retVal;
  }

  // Bilinear upscale from 2x2 pixels to 4x4/8x4 pixels (depending on is2bpp argument)
  // p, q, r, s = [channels]
  // outBlock = [pixels][channels]
  // is2bpp: true=2bpp mode, false=4bpp mode
  private static void interpolateColors(int[] p, int[] q, int[] r, int[] s, int[][] outBlock, boolean is2bpp) {
    int wordWidth = is2bpp ? 8 : 4;
    int wordHeight = 4;

    // making working copy
    int[] hp = Arrays.copyOf(p, p.length);
    int[] hq = Arrays.copyOf(q, q.length);
    int[] hr = Arrays.copyOf(r, r.length);
    int[] hs = Arrays.copyOf(s, s.length);

    // get vectors
    int[] qmp = new int[] { hq[CH_R] - hp[CH_R], hq[CH_G] - hp[CH_G], hq[CH_B] - hp[CH_B], hq[CH_A] - hp[CH_A] };
    int[] smr = new int[] { hs[CH_R] - hr[CH_R], hs[CH_G] - hr[CH_G], hs[CH_B] - hr[CH_B], hs[CH_A] - hr[CH_A] };

    // multiply colors
    for (int i = 0; i < 4; i++) {
      hp[i] *= wordWidth;
      hr[i] *= wordWidth;
    }

    int[] result = new int[4], dy = new int[4];
    if (is2bpp) {
      // loop through pixels to achieve results
      for (int x = 0; x < wordWidth; x++) {
        for (int i = 0; i < 4; i++) {
          result[i] = hp[i] << 2;
          dy[i] = hr[i] - hp[i];
        }

        for (int y = 0; y < wordHeight; y++) {
          outBlock[y * wordWidth + x][CH_R] = (result[CH_R] >> 7) + (result[CH_R] >> 2);
          outBlock[y * wordWidth + x][CH_G] = (result[CH_G] >> 7) + (result[CH_G] >> 2);
          outBlock[y * wordWidth + x][CH_B] = (result[CH_B] >> 7) + (result[CH_B] >> 2);
          outBlock[y * wordWidth + x][CH_A] = (result[CH_A] >> 5) + (result[CH_A] >> 1);

          result[CH_R] += dy[CH_R];
          result[CH_G] += dy[CH_G];
          result[CH_B] += dy[CH_B];
          result[CH_A] += dy[CH_A];
        }

        hp[CH_R] += qmp[CH_R];
        hp[CH_G] += qmp[CH_G];
        hp[CH_B] += qmp[CH_B];
        hp[CH_A] += qmp[CH_A];
        hr[CH_R] += smr[CH_R];
        hr[CH_G] += smr[CH_G];
        hr[CH_B] += smr[CH_B];
        hr[CH_A] += smr[CH_A];
      }
    } else {
      // loop through pixels to achieve results
      for (int y = 0; y < wordHeight; y++) {
        for (int i = 0; i < 4; i++) {
          result[i] = hp[i] << 2;
          dy[i] = hr[i] - hp[i];
        }

        for (int x = 0; x < wordWidth; x++) {
          outBlock[y * wordWidth + x][CH_R] = (result[CH_R] >> 6) + (result[CH_R] >> 1);
          outBlock[y * wordWidth + x][CH_G] = (result[CH_G] >> 6) + (result[CH_G] >> 1);
          outBlock[y * wordWidth + x][CH_B] = (result[CH_B] >> 6) + (result[CH_B] >> 1);
          outBlock[y * wordWidth + x][CH_A] = (result[CH_A] >> 4) + result[CH_A];

          result[CH_R] += dy[CH_R];
          result[CH_G] += dy[CH_G];
          result[CH_B] += dy[CH_B];
          result[CH_A] += dy[CH_A];
        }

        hp[CH_R] += qmp[CH_R];
        hp[CH_G] += qmp[CH_G];
        hp[CH_B] += qmp[CH_B];
        hp[CH_A] += qmp[CH_A];
        hr[CH_R] += smr[CH_R];
        hr[CH_G] += smr[CH_G];
        hr[CH_B] += smr[CH_B];
        hr[CH_A] += smr[CH_A];
      }
    }
  }

  // Reads out and decodes the modulation values within the specified data word
  // modValues, modModes = [x][y]
  private static void unpackModulations(int[] word, int ofsX, int ofsY, int[][] modValues, int[][] modModes,
      boolean is2bpp) {
    int modMode = word[BLK_COL] & 1;
    int modBits = word[BLK_MOD];

    // unpack differently depending on 2bpp or 4bpp modes
    if (is2bpp) {
      if (modMode != 0) {
        // determine which of the three modes are in use:
        if ((modBits & 1) != 0) {
          // look at the LSB for the center (V=2, H=4) texel. Its LSB is now actually used to
          // indicate whether it's the H-only mode or the V-only

          // the center texel data is at (y=2, x=4) and so its LSB is at bit 20
          if ((modBits & (1 << 20)) != 0) {
            // this is V-only mode
            modMode = 3;
          } else {
            // this is H-only mode
            modMode = 2;
          }

          // create an extra bit for the center pixel so that it looks like we have 2 actual bits
          // for this texel. It makes later coding much easier.
          if ((modBits & (1 << 21)) != 0) {
            modBits |= (1 << 20);
          } else {
            modBits &= ~(1 << 20);
          }
        }

        if ((modBits & 2) != 0) {
          modBits |= 1; // set it
        } else {
          modBits &= ~1; // clear it
        }

        // run through all the pixels in the block. Note we can now treat all the stored values as
        // if they have 2 bits (even when they didn't!)
        for (int y = 0; y < 4; y++) {
          for (int x = 0; x < 8; x++) {
            modModes[x + ofsX][y + ofsY] = modMode;

            // if this is a stored value...
            if (((x ^ y) & 1) == 0) {
              modValues[x + ofsX][y + ofsY] = modBits & 3;
              modBits >>>= 2;
            }
          }
        }
      } else {
        // if direct encoded 2bpp mode - i.e. mode bit per pixel
        for (int y = 0; y < 4; y++) {
          for (int x = 0; x < 8; x++) {
            modModes[x + ofsX][y + ofsY] = modMode;

            // double the bits, so 0 -> 00, and 1 -> 11
            modValues[x + ofsX][y + ofsY] = ((modBits & 1) != 0) ? 3 : 0;
            modBits >>>= 1;
          }
        }
      }
    } else {
      // much simpler than 2bpp decompression, only two modes, so the n/8 values are set directly
      // run through all the pixels in the word
      if (modMode != 0) {
        for (int y = 0; y < 4; y++) {
          for (int x = 0; x < 4; x++) {
            modValues[y + ofsY][x + ofsX] = modBits & 3;
            if (modValues[y + ofsY][x + ofsX] == 1) {
              modValues[y + ofsY][x + ofsX] = 4;
            } else if (modValues[y + ofsY][x + ofsX] == 2) {
              modValues[y + ofsY][x + ofsX] = 14; // +10 tells the decompressor to punch through alpha
            } else if (modValues[y + ofsY][x + ofsX] == 3) {
              modValues[y + ofsY][x + ofsX] = 8;
            }
            modBits >>>= 2;
          }
        }
      } else {
        for (int y = 0; y < 4; y++) {
          for (int x = 0; x < 4; x++) {
            modValues[y + ofsY][x + ofsX] = modBits & 3;
            modValues[y + ofsY][x + ofsX] *= 3;
            if (modValues[y + ofsY][x + ofsX] > 3) {
              modValues[y + ofsY][x + ofsX] -= 1;
            }
            modBits >>>= 2;
          }
        }
      }
    }
  }

  // Gets the effective modulation values for the given pixel
  // modValues, modModes = [x][y]
  // xPos, yPos = x, y positions within the current data word
  private static int getModulationValues(int[][] modValues, int[][] modModes, int xPos, int yPos, boolean is2bpp) {
    if (is2bpp) {
      final int[] repVals0 = new int[] { 0, 3, 5, 8 };

      // extract the modulation value...
      if (modModes[xPos][yPos] == 0) {
        // ...if a simple encoding
        return repVals0[modValues[xPos][yPos]];
      } else {
        // ...if this is a stored value
        if (((xPos ^ yPos) & 1) == 0) {
          return repVals0[modValues[xPos][yPos]];

          // else average from the neighbors
        } else if (modModes[xPos][yPos] == 1) {
          // if H&V interpolation
          return (repVals0[modValues[xPos][yPos - 1]] + repVals0[modValues[xPos][yPos + 1]]
              + repVals0[modValues[xPos - 1][yPos]] + repVals0[modValues[xPos + 1][yPos]] + 2) >> 2;
        } else if (modModes[xPos][yPos] == 2) {
          // if H-only
          return (repVals0[modValues[xPos - 1][yPos]] + repVals0[modValues[xPos + 1][yPos]] + 1) >> 1;
        } else {
          // if V-only
          return (repVals0[modValues[xPos][yPos - 1]] + repVals0[modValues[xPos][yPos + 1]] + 1) >> 1;
        }
      }
    } else {
      return modValues[xPos][yPos];
    }
  }

  // Gets decompressed pixels for a given decompression area
  // p, q, r, s = [block word]
  // outBlock = [pixels][channels]
  // is2bpp: true=2bpp mode, false=4bpp mode
  private static void getDecompressedPixels(int[] p, int[] q, int[] r, int[] s, int[][] outData, boolean is2bpp) {
    // 4bpp only needs 8*8 values, but 2bpp needs 16*8, so rather than wasting processor time we just statically
    // allocate 16*8
    int[][] modValues = new int[16][8];
    // Only 2bpp needs this
    int[][] modModes = new int[16][8];
    // 4bpp only needs 16 values, but 2bpp needs 32, so rather than wasting processor time we just statically allocate
    // 32.
    int[][] upscaledColorA = new int[32][4];
    int[][] upscaledColorB = new int[32][4];

    int wordWidth = is2bpp ? 8 : 4;
    int wordHeight = 4;

    // get modulation from each word
    unpackModulations(p, 0, 0, modValues, modModes, is2bpp);
    unpackModulations(q, wordWidth, 0, modValues, modModes, is2bpp);
    unpackModulations(r, 0, wordHeight, modValues, modModes, is2bpp);
    unpackModulations(s, wordWidth, wordHeight, modValues, modModes, is2bpp);

    // bilinear upscale image data from 2x2 -> 4x4
    interpolateColors(getColorA(p[BLK_COL]), getColorA(q[BLK_COL]), getColorA(r[BLK_COL]), getColorA(s[BLK_COL]),
        upscaledColorA, is2bpp);
    interpolateColors(getColorB(p[BLK_COL]), getColorB(q[BLK_COL]), getColorB(r[BLK_COL]), getColorB(s[BLK_COL]),
        upscaledColorB, is2bpp);

    int[] result = new int[4];
    for (int y = 0; y < wordHeight; y++) {
      for (int x = 0; x < wordWidth; x++) {
        int mod = getModulationValues(modValues, modModes, x + (wordWidth >>> 1), y + (wordHeight >>> 1), is2bpp);
        boolean punchThroughAlpha = false;
        if (mod > 10) {
          punchThroughAlpha = true;
          mod -= 10;
        }

        result[CH_R] = (upscaledColorA[y * wordWidth + x][CH_R] * (8 - mod)
            + upscaledColorB[y * wordWidth + x][CH_R] * mod) >> 3;
        result[CH_G] = (upscaledColorA[y * wordWidth + x][CH_G] * (8 - mod)
            + upscaledColorB[y * wordWidth + x][CH_G] * mod) >> 3;
        result[CH_B] = (upscaledColorA[y * wordWidth + x][CH_B] * (8 - mod)
            + upscaledColorB[y * wordWidth + x][CH_B] * mod) >> 3;
        if (punchThroughAlpha) {
          result[CH_A] = 0;
        } else {
          result[CH_A] = (upscaledColorA[y * wordWidth + x][CH_A] * (8 - mod)
              + upscaledColorB[y * wordWidth + x][CH_A] * mod) >> 3;
        }

        // convert the 32bit precision result to 8 bit per channel color
        if (is2bpp) {
          outData[y * wordWidth + x][CH_R] = result[CH_R];
          outData[y * wordWidth + x][CH_G] = result[CH_G];
          outData[y * wordWidth + x][CH_B] = result[CH_B];
          outData[y * wordWidth + x][CH_A] = result[CH_A];
        } else {
          outData[y + x * wordHeight][CH_R] = result[CH_R];
          outData[y + x * wordHeight][CH_G] = result[CH_G];
          outData[y + x * wordHeight][CH_B] = result[CH_B];
          outData[y + x * wordHeight][CH_A] = result[CH_A];
        }
      }
    }
  }

  // Maps decompressed data to the correct location in the output buffer
  private static int wrapWordIndex(int numWords, int word) {
    return ((word + numWords) % numWords);
  }

  // Given the word coordinates and the dimension of the texture in words, this returns the twiddled
  // offset of the word from the start of the map
  private static int twiddleUV(int xSize, int ySize, int xPos, int yPos) {
    // initially assume x is the larger size
    int minDimension = xSize;
    int maxValue = yPos;
    int twiddled = 0;
    int srcBitPos = 1;
    int dstBitPos = 1;
    int shiftCount = 0;

    // if y is the larger dimension - switch the min/max values
    if (ySize < xSize) {
      minDimension = ySize;
      maxValue = xPos;
    }

    // step through all the bits in the "minimum" dimension
    while (srcBitPos < minDimension) {
      if ((yPos & srcBitPos) != 0) {
        twiddled |= dstBitPos;
      }

      if ((xPos & srcBitPos) != 0) {
        twiddled |= (dstBitPos << 1);
      }

      srcBitPos <<= 1;
      dstBitPos <<= 2;
      shiftCount++;
    }

    // prepend any unused bits
    maxValue >>>= shiftCount;
    twiddled |= (maxValue << (shiftCount << 1));

    return twiddled;
  }

  // Maps decompressed data to the correct location in the output buffer
  // outBuffer = [pixel]
  // inData = [pixel][channel]
  // indices = [two per p, q, r, s]
  private static void mapDecompressedData(int[] outBuffer, int width, int[][] inData, int[] indices, boolean is2bpp) {
    int wordWidth = is2bpp ? 8 : 4;
    int wordHeight = 4;

    for (int y = 0; y < (wordHeight >>> 1); y++) {
      for (int x = 0; x < (wordWidth >>> 1); x++) {
        // map p
        int outOfs = (((indices[IDX_P + 1] * wordHeight) + y + (wordHeight >>> 1)) * width
            + indices[IDX_P] * wordWidth + x + (wordWidth >>> 1));
        int inOfs = y * wordWidth + x;
        outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) | (inData[inOfs][CH_G] << 8)
            | inData[inOfs][CH_B];

        // map q
        outOfs = (((indices[IDX_Q + 1] * wordHeight) + y + (wordHeight >>> 1)) * width
            + indices[IDX_Q] * wordWidth + x);
        inOfs = y * wordWidth + x + (wordWidth >>> 1);
        outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) | (inData[inOfs][CH_G] << 8)
            | inData[inOfs][CH_B];

        // map r
        outOfs = (((indices[IDX_R + 1] * wordHeight) + y) * width + indices[IDX_R] * wordWidth + x
            + (wordWidth >>> 1));
        inOfs = (y + (wordHeight >>> 1)) * wordWidth + x;
        outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) | (inData[inOfs][CH_G] << 8)
            | inData[inOfs][CH_B];

        // map s
        outOfs = (((indices[IDX_S + 1] * wordHeight) + y) * width + indices[IDX_S] * wordWidth + x);
        inOfs = (y + (wordHeight >>> 1)) * wordWidth + x + (wordWidth >>> 1);
        outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) | (inData[inOfs][CH_G] << 8)
            | inData[inOfs][CH_B];
      }
    }
  }

  /** Removes all PvrDecoder objects from the cache. */
  public static void flushCache() {
    TEXTURE_CACHE.clear();
  }

  // Returns a PvrDecoder object only if it already exists in the cache.
  private static BufferedImage getCachedImage(PvrInfo pvr) {
    BufferedImage retVal = null;
    if (pvr != null) {
      if (TEXTURE_CACHE.containsKey(pvr)) {
        retVal = TEXTURE_CACHE.get(pvr);
        // re-inserting entry to prevent premature removal from cache
        TEXTURE_CACHE.remove(pvr);
        TEXTURE_CACHE.put(pvr, retVal);
      }
    }
    return retVal;
  }

  // Returns a PvrDecoder object of the specified key if available, or creates and returns a new one otherwise.
  private static void registerCachedImage(PvrInfo pvr, BufferedImage image) {
    if (pvr != null && getCachedImage(pvr) == null && image != null) {
      TEXTURE_CACHE.put(pvr, image);
      // removing excess cache entries
      while (TEXTURE_CACHE.size() > MAX_CACHE_ENTRIES) {
        TEXTURE_CACHE.remove(TEXTURE_CACHE.keySet().iterator().next());
      }
    }
  }
}
