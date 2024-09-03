// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.video.MveDecoder.MveInfo;
import org.infinity.resource.video.MveDecoder.MveSegment;
import org.infinity.util.Logger;
import org.infinity.util.Misc;

/**
 * Decodes a single 8x8 pixel block of video data. (Internally used by MveDecoder)
 */
public class MveVideoDecoder {
  private final MveInfo info;       // the currently used MVE info structure
  private final int[] tmpBlock;     // pre-allocated 8x8 pixel block usable in block copy routines
  private final int[] tmpData;      // pre-allocated working buffer of 32 elements for general use
  private final Palette palette;    // palette used in indexed color mode
  private final BasicVideoBuffer workingBuffer;  // internally used video buffer

  private BufferedImage curBuffer;  // points to the current working buffer
  private BufferedImage prevBuffer; // points to the previous working buffer
  private BufferedImage blackImage; // used when no video buffer has been updated in the current video chunk
  private MveSegment codeSegment;   // contains the code map of the last MVE_OC_CODE_MAP segment
  private boolean isVideoDrawn;     // set when the current video buffer has been updated
  private boolean isVideoInit;      // set when video initialization occured in the current chunk

  /**
   * Returns a new MveVideoDecoder object, asociated with the specified MveDecoder.
   *
   * @param info The parent MveDecoder object
   * @return A new MveVideoDecoder object associated with the specified MveDecoder or null on error.
   */
  public static MveVideoDecoder createDecoder(MveInfo info) {
    if (info != null) {
      return new MveVideoDecoder(info);
    } else {
      return null;
    }
  }

  /**
   * Processes video specific segments.
   *
   * @param segment The current segment to process.
   * @return {@code true} if the segment has been processed successfully, {@code false} if the segment did not fit into
   *         the video category.
   * @throws Exception On error.
   */
  public boolean processVideo(MveSegment segment) throws Exception {
    if (info == null || segment == null) {
      throw new NullPointerException();
    }

    switch (segment.getOpcode()) {
      case MveDecoder.MVE_OC_CREATE_TIMER: // sets a stable timer that can be used throughout the whole video
        processVideoTimer(segment);
        break;
      case MveDecoder.MVE_OC_VIDEO_BUFFERS: // initializes video properties
        processInitVideo(segment);
        break;
      case MveDecoder.MVE_OC_PLAY_VIDEO: // presents the completed frame
        processOutputFrame(segment);
        break;
      case MveDecoder.MVE_OC_VIDEO_MODE: // unused
        break;
      case MveDecoder.MVE_OC_CREATE_GRADIENT: // creates a gradient palette (unused)
        processGradient(segment);
        break;
      case MveDecoder.MVE_OC_PALETTE: // sets a palette
        processPalette(segment);
        break;
      case MveDecoder.MVE_OC_PALETTE_PACKED: // sets palette entries
        processPalettePacked(segment);
        break;
      case MveDecoder.MVE_OC_CODE_MAP: // prepares video code stream for this frame
        processCodeMap(segment);
        break;
      case MveDecoder.MVE_OC_VIDEO_DATA: // decodes a frame (used in conjunction with MVE_OC_CODE_MAP)
        processVideoFrame(segment);
        break;
      case MveDecoder.MVE_OC_END_OF_CHUNK: // do some temporary clean up
        cleanUp();
        break;
      case MveDecoder.MVE_OC_END_OF_STREAM: // do final clean up
        shutDown();
        break;
      default:
        return false;
    }
    return true;
  }

  /**
   * Properly releases temporary resources.
   */
  public void close() {
    shutDown();
  }

  // Cannot be constructed directly.
  private MveVideoDecoder(MveInfo info) {
    if (info == null) {
      throw new NullPointerException();
    }

    this.info = info;
    workingBuffer = new BasicVideoBuffer();
    curBuffer = (BufferedImage) workingBuffer.frontBuffer();
    prevBuffer = (BufferedImage) workingBuffer.backBuffer();
    palette = new MveVideoDecoder.Palette();
    tmpBlock = new int[8 * 8];
    tmpData = new int[32];
    codeSegment = null;
    isVideoDrawn = false;
    blackImage = null;
  }

  // cleans up temporary data
  private void cleanUp() {
    info.videoInitialized = isVideoInit;
    isVideoInit = false;
    isVideoDrawn = false;
    codeSegment = null;
  }

  // cleans up all MVE specific
  private void shutDown() {
    workingBuffer.release();
  }

  // sets the stable timer
  private void processVideoTimer(MveSegment segment) throws Exception {
    int rate = segment.getBits(32);
    int factor = segment.getBits(16);
    info.frameDelay = rate * factor;
    info.isFrameDelayStable = true;
  }

  // initializes video properties
  private void processInitVideo(MveSegment segment) throws Exception {
    isVideoInit = true;
    int width, height;
    boolean isPalette;
    switch (segment.getVersion()) {
      case 0:
        width = segment.getBits(16);
        height = segment.getBits(16);
        isPalette = true;
        break;
      case 1:
        width = segment.getBits(16);
        height = segment.getBits(16);
        segment.getBits(16); // count: not needed
        isPalette = true;
        break;
      case 2:
        width = segment.getBits(16);
        height = segment.getBits(16);
        segment.getBits(16); // count: not needed
        isPalette = (segment.getBits(16) == 0);
        break;
      default:
        throw new Exception("Unsupported version of video initialization segment: " + segment.getVersion());
    }
    info.width = width << 3;
    info.height = height << 3;
    info.isPalette = isPalette;
    palette.clearPalette();
    workingBuffer.create(2, info.width, info.height, false);
    curBuffer = (BufferedImage) workingBuffer.frontBuffer();
    prevBuffer = (BufferedImage) workingBuffer.backBuffer();
    blackImage = ColorConvert.createCompatibleImage(info.width, info.height, false);
  }

  // Presents the completed frame
  private void processOutputFrame(MveSegment segment) throws Exception {
    if (info.videoOutput != null) {
      // image is drawn centered
      Image dstImage = info.videoOutput.backBuffer();
      Image srcImage = null;
      if (isVideoDrawn) {
        srcImage = workingBuffer.frontBuffer();
      } else if (blackImage != null) {
        srcImage = blackImage;
      } else {
        srcImage = ColorConvert.createCompatibleImage(info.width, info.height, false);
      }
      Graphics2D g = (Graphics2D) dstImage.getGraphics();
      int x = (dstImage.getWidth(null) - srcImage.getWidth(null)) / 2;
      int y = (dstImage.getHeight(null) - srcImage.getHeight(null)) / 2;
      g.drawImage(srcImage, x, y, null);
      g.dispose();
      info.videoOutput.flipBuffers();
      srcImage = null;
      dstImage = null;
    }
    workingBuffer.flipBuffers();
    curBuffer = (BufferedImage) workingBuffer.frontBuffer();
    prevBuffer = (BufferedImage) workingBuffer.backBuffer();
  }

  // reads decoding map from stream
  private void processCodeMap(MveSegment segment) throws Exception {
    codeSegment = segment;
  }

  // creates a gradient palette
  private void processGradient(MveSegment segment) throws Exception {
    // Color ranges: [0..63] for red and [0..39] for green/blue
    int baseRB = segment.getBits(8);
    int numR_RB = segment.getBits(8);
    int numB_RB = segment.getBits(8);
    int baseRG = segment.getBits(8);
    int numR_RG = segment.getBits(8);
    int numG_RG = segment.getBits(8);
    if (baseRB > 0 && numR_RB > 0 && numB_RB > 0) {
      int idx = baseRB;
      for (int y = 0; y < numR_RB; y++) {
        for (int x = 0; x < numB_RB; x++) {
          palette.setColor(idx++, (byte) ((63 * y) / (numR_RB - 1)), (byte) 0, (byte) ((36 * x) / (numB_RB - 1)));
        }
      }
    }
    if (baseRG > 0 && numR_RG > 0 && numG_RG > 0) {
      int idx = baseRG;
      for (int y = 0; y < numR_RG; y++) {
        for (int x = 0; x < numG_RG; x++) {
          palette.setColor(idx++, (byte) ((63 * y) / (numR_RG - 1)), (byte) ((36 * x) / (numG_RG - 1)), (byte) 0);
        }
      }
    }
  }

  // sets a new palette
  private void processPalette(MveSegment segment) throws Exception {
    int palStart = segment.getBits(16);
    int palCount = segment.getBits(16);
    palette.setPalette(palStart, palCount, segment.getData(), 4);
  }

  // sets specific palette entries as defined in the segment data
  private void processPalettePacked(MveSegment segment) throws Exception {
    for (int i = 0; i < (256 >>> 3); i++) {
      int mask = segment.getBits(8);
      if (mask != 0) {
        for (int j = 0; j < 8; j++) {
          if ((mask & (1 << j)) != 0) {
            int idx = (i << 3) + j; // palette index
            byte r = (byte) segment.getBits(8); // red
            byte g = (byte) segment.getBits(8); // green
            byte b = (byte) segment.getBits(8); // blue
            palette.setColor(idx, r, g, b);
          }
        }
      }
    }
  }

  // entry point for the main video decoding routine
  private void processVideoFrame(MveSegment segment) throws Exception {
    if (codeSegment == null) {
      throw new Exception("No code map available");
    }

    isVideoDrawn = true;
    info.currentFrame = segment.getBits(16);
    segment.getBits(16);
    segment.getBits(16); // x offset: always 0 (maybe used for panning?)
    segment.getBits(16); // y offset: always 0 (maybe used for panning?)
    int sizeX = segment.getBits(16); // width in 8x8 blocks
    int sizeY = segment.getBits(16); // height in 8x8 blocks
    segment.getBits(16); // flags: bit 0 = delta frame (usually all frames but the first)

    // using separate decoding functions for direct color and indexed color modes
    int idx = 0, count = sizeX * sizeY;
    if (info.isPalette) {
      while (idx < count) {
        int w = (idx % sizeX) << 3; // start x of the current 8x8 pixel block
        int h = (idx / sizeX) << 3; // start y of the current 8x8 pixel block
        byte code = (byte) codeSegment.getBits(4);
        switch (code) {
          case 0x00:
            decode8_00(w, h, segment);
            break;
          case 0x01:
            decode8_01(w, h, segment);
            break;
          case 0x02:
            decode8_02(w, h, segment);
            break;
          case 0x03:
            decode8_03(w, h, segment);
            break;
          case 0x04:
            decode8_04(w, h, segment);
            break;
          case 0x05:
            decode8_05(w, h, segment);
            break;
          case 0x06:
            decode8_06(w, h, segment);
            idx++;
            codeSegment.getBits(4);
            break;
          case 0x07:
            decode8_07(w, h, segment);
            break;
          case 0x08:
            decode8_08(w, h, segment);
            break;
          case 0x09:
            decode8_09(w, h, segment);
            break;
          case 0x0a:
            decode8_0a(w, h, segment);
            break;
          case 0x0b:
            decode8_0b(w, h, segment);
            break;
          case 0x0c:
            decode8_0c(w, h, segment);
            break;
          case 0x0d:
            decode8_0d(w, h, segment);
            break;
          case 0x0e:
            decode8_0e(w, h, segment);
            break;
          case 0x0f:
            decode8_0f(w, h, segment);
            break;
        }
        idx++;
      }
    } else {
      // separate data block used in codes 0x02, 0x03 and 0x04
      int ofs = segment.getOffset();
      int ofsExtra = segment.getBits(16);
      segment.setOffsetExtra(ofs + ofsExtra);

      while (idx < count) {
        int w = (idx % sizeX) << 3; // start x of the current 8x8 pixel block
        int h = (idx / sizeX) << 3; // start y of the current 8x8 pixel block
        byte code = (byte) codeSegment.getBits(4);
        switch (code) {
          case 0x00:
            decode16_00(w, h, segment);
            break;
          case 0x01:
            decode16_01(w, h, segment);
            break;
          case 0x02:
            decode16_02(w, h, segment);
            break;
          case 0x03:
            decode16_03(w, h, segment);
            break;
          case 0x04:
            decode16_04(w, h, segment);
            break;
          case 0x05:
            decode16_05(w, h, segment);
            break;
          case 0x06:
            decode16_06(w, h, segment);
            idx++;
            codeSegment.getBits(4);
            break;
          case 0x07:
            decode16_07(w, h, segment);
            break;
          case 0x08:
            decode16_08(w, h, segment);
            break;
          case 0x09:
            decode16_09(w, h, segment);
            break;
          case 0x0a:
            decode16_0a(w, h, segment);
            break;
          case 0x0b:
            decode16_0b(w, h, segment);
            break;
          case 0x0c:
            decode16_0c(w, h, segment);
            break;
          case 0x0d:
            decode16_0d(w, h, segment);
            break;
          case 0x0e:
            decode16_0e(w, h, segment);
            break;
          case 0x0f:
            decode16_0f(w, h, segment);
            break;
        }
        idx++;
      }
    }
  }

  // --------------- indexed color decoding routines ---------------

  private void decode8_00(int startX, int startY, MveSegment segment) {
    // copy block from previous buffer
    copyBlock8x8(prevBuffer, startX, startY, startX, startY);
  }

  private void decode8_01(int startX, int startY, MveSegment segment) {
    // block has same content as two buffers ago -> no change in double buffered chain
  }

  private void decode8_02(int startX, int startY, MveSegment segment) {
    // block is copied from below/right of current buffer
    int v = segment.getBits(8);
    int x, y;
    if (v < 56) {
      x = 8 + (v % 7);
      y = v / 7;
    } else {
      x = -14 + ((v - 56) % 29);
      y = 8 + ((v - 56) / 29);
    }
    copyBlock8x8(curBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode8_03(int startX, int startY, MveSegment segment) {
    // block is copied from above/left of current buffer
    int v = segment.getBits(8);
    int x, y;
    if (v < 56) {
      x = -(8 + (v % 7));
      y = -(v / 7);
    } else {
      x = -(-14 + (v - 56) % 29);
      y = -(8 + ((v - 56) / 29));
    }
    copyBlock8x8(curBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode8_04(int startX, int startY, MveSegment segment) {
    // block is copied from nearby (all directions) of previous buffer - short ranged
    int x = -8 + segment.getBits(4);
    int y = -8 + segment.getBits(4);
    copyBlock8x8(prevBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode8_05(int startX, int startY, MveSegment segment) {
    // block is copied from nearby (all directions) of previous buffer - long ranged
    int x = Misc.signExtend(segment.getBits(8), 8);
    int y = Misc.signExtend(segment.getBits(8), 8);
    copyBlock8x8(prevBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode8_06(int startX, int startY, MveSegment segment) {
    // indicates to skip this and the next pixel block (??? to be confirmed ???)
  }

  private void decode8_07(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 1)
    int p0 = segment.getBits(8);
    tmpData[0] = palette.data[p0];
    int p1 = segment.getBits(8);
    tmpData[1] = palette.data[p1];
    int ofs = 0;

    if (p0 <= p1) {
      // mask bits (from left=7 to right=0, for each pixel): clear=c0, set=c1
      for (int y = 0; y < 8; y++) {
        int m = segment.getBits(8); // mask
        for (int bit = 0; bit < 8; bit++, ofs++) {
          tmpBlock[ofs] = tmpData[(m >>> bit) & 1];
        }
      }
    } else {
      // mask bits (from top-left=15, to bottom-right=0, for each 2x2 pixel block): clear=c1, set=c0
      int m = segment.getBits(16);
      for (int y = 0, bit = 0; y < 4; y++, ofs += 8) {
        for (int x = 0; x < 4; x++, bit++, ofs += 2) {
          int v = tmpData[(m >>> bit) & 1];
          tmpBlock[ofs] = v;
          tmpBlock[ofs + 1] = v;
          tmpBlock[ofs + 8] = v;
          tmpBlock[ofs + 9] = v;
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_08(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 2)
    int p0 = segment.getBits(8);
    tmpData[0] = palette.data[p0];
    int p1 = segment.getBits(8);
    tmpData[1] = palette.data[p1];
    tmpData[2] = segment.getBits(16);
    int ofs = 0;

    if (p0 <= p1) {
      // pattern: c0, c1, mask(16) for each quadrant
      for (int i = 3; i < 12; i += 3) {
        tmpData[i] = palette.data[segment.getBits(8)];
        tmpData[i + 1] = palette.data[segment.getBits(8)];
        tmpData[i + 2] = segment.getBits(16);
      }

      for (int y = 0; y < 4; y++, ofs += 4) {
        for (int x = 0; x < 4; x++, ofs++) {
          int bit = (y << 2) + x;
          // quadrant top-left [c0=0, c1=1, m=2]
          tmpBlock[ofs] = tmpData[(tmpData[2] >>> bit) & 1];
          // quadrant bottom-left [c0=3, c1=4, m=5]
          tmpBlock[32 + ofs] = tmpData[3 + ((tmpData[5] >>> bit) & 1)];
          // quadrant top-right [c0=6, c1=7, m=8]
          tmpBlock[4 + ofs] = tmpData[6 + ((tmpData[8] >>> bit) & 1)];
          // quadrant bottom-right [c0=9, c1=10, m=11]
          tmpBlock[36 + ofs] = tmpData[9 + ((tmpData[11] >>> bit) & 1)];
        }
      }
    } else {
      // pattern: c0, c1, mask(32) for either left/right or top/bottom halves
      tmpData[2] |= segment.getBits(8) << 16;
      tmpData[2] |= segment.getBits(8) << 24;
      int p2 = segment.getBits(8);
      tmpData[3] = palette.data[p2];
      int p3 = segment.getBits(8);
      tmpData[4] = palette.data[p3];
      tmpData[5] = segment.getBits(32);

      if (p2 <= p3) {
        // left/right halves
        for (int y = 0; y < 8; y++, ofs += 4) {
          for (int x = 0; x < 4; x++, ofs++) {
            int bit = (y << 2) + x;
            // left half: [c0=0, c1=1, m=2]
            tmpBlock[ofs] = tmpData[(tmpData[2] >>> bit) & 1];
            // right half: [c0=3, c1=4, m=5]
            tmpBlock[4 + ofs] = tmpData[3 + ((tmpData[5] >>> bit) & 1)];
          }
        }
      } else {
        // top/bottom halves
        for (int y = 0; y < 4; y++) {
          for (int x = 0; x < 8; x++, ofs++) {
            int bit = (y << 3) + x;
            // top half: [c0=0, c1=1, m=2]
            tmpBlock[ofs] = tmpData[(tmpData[2] >>> bit) & 1];
            // bottom half: [c0=3, c1=4, m=5]
            tmpBlock[32 + ofs] = tmpData[3 + ((tmpData[5] >>> bit) & 1)];
          }
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_09(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 3)
    int p0 = segment.getBits(8);
    tmpData[0] = palette.data[p0];
    int p1 = segment.getBits(8);
    tmpData[1] = palette.data[p1];
    int p2 = segment.getBits(8);
    tmpData[2] = palette.data[p2];
    int p3 = segment.getBits(8);
    tmpData[3] = palette.data[p3];
    int ofs = 0, m = 0;

    if (p0 <= p1) {
      if (p2 <= p3) {
        // two bits per pixel define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 8; y++) {
          if ((y & 1) == 0) {
            m = segment.getBits(32);
          }
          for (int x = 0; x < 8; x++, ofs++, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> (bit & 31)) & 3];
          }
        }
      } else {
        m = segment.getBits(32);
        // two bits per 2x2 pixel block define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 4; y++, ofs += 8) {
          for (int x = 0; x < 4; x++, ofs += 2, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> bit) & 3];
            tmpBlock[ofs + 1] = tmpData[(m >>> bit) & 3];
            tmpBlock[ofs + 8] = tmpData[(m >>> bit) & 3];
            tmpBlock[ofs + 9] = tmpData[(m >>> bit) & 3];
          }
        }
      }
    } else {
      if (p2 <= p3) {
        // two bits per 2x1 pixel block define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 8; y++) {
          if ((y & 3) == 0) {
            m = segment.getBits(32);
          }
          for (int x = 0; x < 4; x++, ofs += 2, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> (bit & 31)) & 3];
            tmpBlock[ofs + 1] = tmpData[(m >>> (bit & 31)) & 3];
          }
        }
      } else {
        // two bits per 1x2 pixel block define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 4; y++, ofs += 8) {
          if ((y & 1) == 0) {
            m = segment.getBits(32);
          }
          for (int x = 0; x < 8; x++, ofs++, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> (bit & 31)) & 3];
            tmpBlock[ofs + 8] = tmpData[(m >>> (bit & 31)) & 3];
          }
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_0a(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 4)
    int p0 = segment.getBits(8);
    tmpData[0] = palette.data[p0];
    int p1 = segment.getBits(8);
    tmpData[1] = palette.data[p1];
    int p2 = segment.getBits(8);
    tmpData[2] = palette.data[p2];
    int p3 = segment.getBits(8);
    tmpData[3] = palette.data[p3];
    tmpData[4] = segment.getBits(32);
    int ofs = 0;

    if (p0 <= p1) {
      // 4 quadrants, two bits per pixel to address one of 4 colors
      for (int i = 5; i < 20; i += 5) {
        tmpData[i] = palette.data[segment.getBits(8)];
        tmpData[i + 1] = palette.data[segment.getBits(8)];
        tmpData[i + 2] = palette.data[segment.getBits(8)];
        tmpData[i + 3] = palette.data[segment.getBits(8)];
        tmpData[i + 4] = segment.getBits(32);
      }

      for (int y = 0, bit = 0; y < 4; y++, ofs += 4) {
        for (int x = 0; x < 4; x++, ofs++, bit += 2) {
          // Quadrant top-left [0..4]
          tmpBlock[ofs] = tmpData[(tmpData[4] >>> bit) & 3];
          // Quadrant bottom-left [5..9]
          tmpBlock[ofs + 32] = tmpData[5 + ((tmpData[9] >>> bit) & 3)];
          // Quadrant top-right [10..14]
          tmpBlock[ofs + 4] = tmpData[10 + ((tmpData[14] >>> bit) & 3)];
          // Quadrant bottom-right [15..19]
          tmpBlock[ofs + 36] = tmpData[15 + ((tmpData[19] >>> bit) & 3)];
        }
      }
    } else {
      tmpData[5] = segment.getBits(32);
      int p4 = segment.getBits(8);
      tmpData[6] = palette.data[p4];
      int p5 = segment.getBits(8);
      tmpData[7] = palette.data[p5];
      int p6 = segment.getBits(8);
      tmpData[8] = palette.data[p6];
      int p7 = segment.getBits(8);
      tmpData[9] = palette.data[p7];
      tmpData[10] = segment.getBits(32);
      tmpData[11] = segment.getBits(32);

      if (p4 <= p5) {
        // left/right halves
        for (int y = 0, bit = 0; y < 8; y++, ofs += 4) {
          for (int x = 0; x < 4; x++, ofs++, bit += 2) {
            // left half [0..5]
            tmpBlock[ofs] = tmpData[(tmpData[4 + (bit >>> 5)] >>> (bit & 31)) & 3];
            // right half [6..11]
            tmpBlock[4 + ofs] = tmpData[6 + ((tmpData[10 + (bit >>> 5)] >>> (bit & 31)) & 3)];
          }
        }
      } else {
        // top/bottom halves
        for (int y = 0, bit = 0; y < 4; y++) {
          for (int x = 0; x < 8; x++, ofs++, bit += 2) {
            // top half [0..5]
            tmpBlock[ofs] = tmpData[(tmpData[4 + (bit >>> 5)] >>> (bit & 30)) & 3];
            // bottom half [6..11]
            tmpBlock[32 + ofs] = tmpData[6 + ((tmpData[10 + (bit >>> 5)] >>> (bit & 30)) & 3)];
          }
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_0b(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte per pixel)
    for (int i = 0; i < 64; i++) {
      tmpBlock[i] = palette.data[segment.getBits(8)];
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_0c(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte per 2x2 pixel block)
    int ofs = 0;
    for (int y = 0; y < 4; y++, ofs += 8) {
      for (int x = 0; x < 4; x++, ofs += 2) {
        int c = palette.data[segment.getBits(8)];
        tmpBlock[ofs] = c;
        tmpBlock[ofs + 1] = c;
        tmpBlock[ofs + 8] = c;
        tmpBlock[ofs + 9] = c;
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_0d(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte per 4x4 pixel block)
    tmpData[0] = palette.data[segment.getBits(8)];
    tmpData[1] = palette.data[segment.getBits(8)];
    tmpData[2] = palette.data[segment.getBits(8)];
    tmpData[3] = palette.data[segment.getBits(8)];
    int ofs = 0;
    for (int y = 0; y < 4; y++, ofs += 4) {
      for (int x = 0; x < 4; x++, ofs++) {
        tmpBlock[ofs] = tmpData[0];
        tmpBlock[4 + ofs] = tmpData[1];
        tmpBlock[32 + ofs] = tmpData[2];
        tmpBlock[36 + ofs] = tmpData[3];
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_0e(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte for the whole 8x8 pixel block)
    int c = palette.data[segment.getBits(8)];
    Arrays.fill(tmpBlock, c);
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode8_0f(int startX, int startY, MveSegment segment) {
    // create dithered pixel block by alternating two pixels from segment data block
    tmpData[0] = palette.data[segment.getBits(8)];
    tmpData[1] = palette.data[segment.getBits(8)];
    int ofs = 0;
    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++, ofs++) {
        tmpBlock[ofs] = tmpData[(y + x) & 1];
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  // --------------- direct color decoding routines ---------------

  private void decode16_00(int startX, int startY, MveSegment segment) {
    // copy block from previous buffer
    copyBlock8x8(prevBuffer, startX, startY, startX, startY);
  }

  private void decode16_01(int startX, int startY, MveSegment segment) {
    // block has same content as two buffers ago -> no change in double buffered chain
  }

  private void decode16_02(int startX, int startY, MveSegment segment) {
    // block is copied from below/right of current buffer
    int v = segment.getBitsExtra(8);
    int x, y;
    if (v < 56) {
      x = 8 + (v % 7);
      y = v / 7;
    } else {
      x = -14 + ((v - 56) % 29);
      y = 8 + ((v - 56) / 29);
    }
    copyBlock8x8(curBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode16_03(int startX, int startY, MveSegment segment) {
    // block is copied from above/left of current buffer
    int v = segment.getBitsExtra(8);
    int x, y;
    if (v < 56) {
      x = -(8 + (v % 7));
      y = -(v / 7);
    } else {
      x = -(-14 + (v - 56) % 29);
      y = -(8 + ((v - 56) / 29));
    }
    copyBlock8x8(curBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode16_04(int startX, int startY, MveSegment segment) {
    // block is copied from nearby (all directions) of previous buffer - short ranged
    int x = -8 + segment.getBitsExtra(4);
    int y = -8 + segment.getBitsExtra(4);
    copyBlock8x8(prevBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode16_05(int startX, int startY, MveSegment segment) {
    // block is copied from nearby (all directions) of previous buffer - long ranged
    int x = Misc.signExtend(segment.getBits(8), 8);
    int y = Misc.signExtend(segment.getBits(8), 8);
    copyBlock8x8(prevBuffer, startX + x, startY + y, startX, startY);
  }

  private void decode16_06(int startX, int startY, MveSegment segment) {
    // indicates to skip this and the next pixel block (??? to be confirmed ???)
  }

  private void decode16_07(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 1)
    int p0 = segment.getBits(16);
    tmpData[0] = pixelToColor(p0);
    int p1 = segment.getBits(16);
    tmpData[1] = pixelToColor(p1);
    int ofs = 0;

    if ((p0 & 0x8000) == 0) {
      // mask bits (from left=7 to right=0, for each pixel): clear=c0, set=c1
      for (int y = 0; y < 8; y++) {
        int m = segment.getBits(8); // mask
        for (int bit = 0; bit < 8; bit++, ofs++) {
          tmpBlock[ofs] = tmpData[(m >>> bit) & 1];
        }
      }
    } else {
      // mask bits (from top-left=15, to bottom-right=0, for each 2x2 pixel block): clear=c1, set=c0
      int m = segment.getBits(16);
      for (int y = 0, bit = 0; y < 4; y++, ofs += 8) {
        for (int x = 0; x < 4; x++, bit++, ofs += 2) {
          int v = tmpData[(m >>> bit) & 1];
          tmpBlock[ofs] = v;
          tmpBlock[ofs + 1] = v;
          tmpBlock[ofs + 8] = v;
          tmpBlock[ofs + 9] = v;
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_08(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 2)
    int p0 = segment.getBits(16);
    tmpData[0] = pixelToColor(p0);
    int p1 = segment.getBits(16);
    tmpData[1] = pixelToColor(p1);
    tmpData[2] = segment.getBits(16);
    int ofs = 0;

    if ((p0 & 0x8000) == 0) {
      // pattern: c0, c1, mask(16) for each quadrant
      for (int i = 3; i < 12; i += 3) {
        tmpData[i] = pixelToColor(segment.getBits(16));
        tmpData[i + 1] = pixelToColor(segment.getBits(16));
        tmpData[i + 2] = segment.getBits(16);
      }

      for (int y = 0; y < 4; y++, ofs += 4) {
        for (int x = 0; x < 4; x++, ofs++) {
          int bit = (y << 2) + x;
          // quadrant top-left [c0=0, c1=1, m=2]
          tmpBlock[ofs] = tmpData[(tmpData[2] >>> bit) & 1];
          // quadrant bottom-left [c0=3, c1=4, m=5]
          tmpBlock[32 + ofs] = tmpData[3 + ((tmpData[5] >>> bit) & 1)];
          // quadrant top-right [c0=6, c1=7, m=8]
          tmpBlock[4 + ofs] = tmpData[6 + ((tmpData[8] >>> bit) & 1)];
          // quadrant bottom-right [c0=9, c1=10, m=11]
          tmpBlock[36 + ofs] = tmpData[9 + ((tmpData[11] >>> bit) & 1)];
        }
      }
    } else {
      // pattern: c0, c1, mask(32) for either left/right or top/bottom halves
      tmpData[2] |= segment.getBits(8) << 16;
      tmpData[2] |= segment.getBits(8) << 24;
      int p2 = segment.getBits(16);
      tmpData[3] = pixelToColor(p2);
      int p3 = segment.getBits(16);
      tmpData[4] = pixelToColor(p3);
      tmpData[5] = segment.getBits(32);

      if ((p2 & 0x8000) == 0) {
        // left/right halves
        for (int y = 0; y < 8; y++, ofs += 4) {
          for (int x = 0; x < 4; x++, ofs++) {
            int bit = (y << 2) + x;
            // left half: [c0=0, c1=1, m=2]
            tmpBlock[ofs] = tmpData[(tmpData[2] >>> bit) & 1];
            // right half: [c0=3, c1=4, m=5]
            tmpBlock[4 + ofs] = tmpData[3 + ((tmpData[5] >>> bit) & 1)];
          }
        }
      } else {
        // top/bottom halves
        for (int y = 0; y < 4; y++) {
          for (int x = 0; x < 8; x++, ofs++) {
            int bit = (y << 3) + x;
            // top half: [c0=0, c1=1, m=2]
            tmpBlock[ofs] = tmpData[(tmpData[2] >>> bit) & 1];
            // bottom half: [c0=3, c1=4, m=5]
            tmpBlock[32 + ofs] = tmpData[3 + ((tmpData[5] >>> bit) & 1)];
          }
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_09(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 3)
    int p0 = segment.getBits(16);
    tmpData[0] = pixelToColor(p0);
    int p1 = segment.getBits(16);
    tmpData[1] = pixelToColor(p1);
    int p2 = segment.getBits(16);
    tmpData[2] = pixelToColor(p2);
    int p3 = segment.getBits(16);
    tmpData[3] = pixelToColor(p3);
    int ofs = 0, m = 0;

    if ((p0 & 0x8000) == 0) {
      if ((p2 & 0x8000) == 0) {
        // two bits per pixel define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 8; y++) {
          if ((y & 1) == 0) {
            m = segment.getBits(32);
          }
          for (int x = 0; x < 8; x++, ofs++, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> (bit & 31)) & 3];
          }
        }
      } else {
        m = segment.getBits(32);
        // two bits per 2x2 pixel block define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 4; y++, ofs += 8) {
          for (int x = 0; x < 4; x++, ofs += 2, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> bit) & 3];
            tmpBlock[ofs + 1] = tmpData[(m >>> bit) & 3];
            tmpBlock[ofs + 8] = tmpData[(m >>> bit) & 3];
            tmpBlock[ofs + 9] = tmpData[(m >>> bit) & 3];
          }
        }
      }
    } else {
      if ((p2 & 0x8000) == 0) {
        // two bits per 2x1 pixel block define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 8; y++) {
          if ((y & 3) == 0) {
            m = segment.getBits(32);
          }
          for (int x = 0; x < 4; x++, ofs += 2, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> (bit & 31)) & 3];
            tmpBlock[ofs + 1] = tmpData[(m >>> (bit & 31)) & 3];
          }
        }
      } else {
        // two bits per 1x2 pixel block define which color [c0..c3] to take
        for (int y = 0, bit = 0; y < 4; y++, ofs += 8) {
          if ((y & 1) == 0) {
            m = segment.getBits(32);
          }
          for (int x = 0; x < 8; x++, ofs++, bit += 2) {
            tmpBlock[ofs] = tmpData[(m >>> (bit & 31)) & 3];
            tmpBlock[ofs + 8] = tmpData[(m >>> (bit & 31)) & 3];
          }
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_0a(int startX, int startY, MveSegment segment) {
    // create a patterned 8x8 block (version 4)
    int p0 = segment.getBits(16);
    tmpData[0] = pixelToColor(p0);
    int p1 = segment.getBits(16);
    tmpData[1] = pixelToColor(p1);
    int p2 = segment.getBits(16);
    tmpData[2] = pixelToColor(p2);
    int p3 = segment.getBits(16);
    tmpData[3] = pixelToColor(p3);
    tmpData[4] = segment.getBits(32);
    int ofs = 0;

    if ((p0 & 0x8000) == 0) {
      // 4 quadrants, two bits per pixel to address one of 4 colors
      for (int i = 5; i < 20; i += 5) {
        tmpData[i] = pixelToColor(segment.getBits(16));
        tmpData[i + 1] = pixelToColor(segment.getBits(16));
        tmpData[i + 2] = pixelToColor(segment.getBits(16));
        tmpData[i + 3] = pixelToColor(segment.getBits(16));
        tmpData[i + 4] = segment.getBits(32);
      }

      for (int y = 0, bit = 0; y < 4; y++, ofs += 4) {
        for (int x = 0; x < 4; x++, ofs++, bit += 2) {
          // Quadrant top-left [0..4]
          tmpBlock[ofs] = tmpData[(tmpData[4] >>> bit) & 3];
          // Quadrant bottom-left [5..9]
          tmpBlock[ofs + 32] = tmpData[5 + ((tmpData[9] >>> bit) & 3)];
          // Quadrant top-right [10..14]
          tmpBlock[ofs + 4] = tmpData[10 + ((tmpData[14] >>> bit) & 3)];
          // Quadrant bottom-right [15..19]
          tmpBlock[ofs + 36] = tmpData[15 + ((tmpData[19] >>> bit) & 3)];
        }
      }
    } else {
      tmpData[5] = segment.getBits(32);
      int p4 = segment.getBits(16);
      tmpData[6] = pixelToColor(p4);
      int p5 = segment.getBits(16);
      tmpData[7] = pixelToColor(p5);
      int p6 = segment.getBits(16);
      tmpData[8] = pixelToColor(p6);
      int p7 = segment.getBits(16);
      tmpData[9] = pixelToColor(p7);
      tmpData[10] = segment.getBits(32);
      tmpData[11] = segment.getBits(32);

      if ((p4 & 0x8000) == 0) {
        // left/right halves
        for (int y = 0, bit = 0; y < 8; y++, ofs += 4) {
          for (int x = 0; x < 4; x++, ofs++, bit += 2) {
            // left half [0..5]
            tmpBlock[ofs] = tmpData[(tmpData[4 + (bit >>> 5)] >>> (bit & 31)) & 3];
            // right half [6..11]
            tmpBlock[4 + ofs] = tmpData[6 + ((tmpData[10 + (bit >>> 5)] >>> (bit & 31)) & 3)];
          }
        }
      } else {
        // top/bottom halves
        for (int y = 0, bit = 0; y < 4; y++) {
          for (int x = 0; x < 8; x++, ofs++, bit += 2) {
            // top half [0..5]
            tmpBlock[ofs] = tmpData[(tmpData[4 + (bit >>> 5)] >>> (bit & 30)) & 3];
            // bottom half [6..11]
            tmpBlock[32 + ofs] = tmpData[6 + ((tmpData[10 + (bit >>> 5)] >>> (bit & 30)) & 3)];
          }
        }
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_0b(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte per pixel)
    for (int i = 0; i < 64; i++) {
      tmpBlock[i] = pixelToColor(segment.getBits(16));
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_0c(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte per 2x2 pixel block)
    int ofs = 0;
    for (int y = 0; y < 4; y++, ofs += 8) {
      for (int x = 0; x < 4; x++, ofs += 2) {
        int c = pixelToColor(segment.getBits(16));
        tmpBlock[ofs] = c;
        tmpBlock[ofs + 1] = c;
        tmpBlock[ofs + 8] = c;
        tmpBlock[ofs + 9] = c;
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_0d(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte per 4x4 pixel block)
    tmpData[0] = pixelToColor(segment.getBits(16));
    tmpData[1] = pixelToColor(segment.getBits(16));
    tmpData[2] = pixelToColor(segment.getBits(16));
    tmpData[3] = pixelToColor(segment.getBits(16));
    int ofs = 0;
    for (int y = 0; y < 4; y++, ofs += 4) {
      for (int x = 0; x < 4; x++, ofs++) {
        tmpBlock[ofs] = tmpData[0];
        tmpBlock[4 + ofs] = tmpData[1];
        tmpBlock[32 + ofs] = tmpData[2];
        tmpBlock[36 + ofs] = tmpData[3];
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_0e(int startX, int startY, MveSegment segment) {
    // copy raw pixel data from segment data block (1 byte for the whole 8x8 pixel block)
    int c = pixelToColor(segment.getBits(16));
    Arrays.fill(tmpBlock, c);
    writeBlock8x8(tmpBlock, startX, startY);
  }

  private void decode16_0f(int startX, int startY, MveSegment segment) {
    // create dithered pixel block by alternating two pixels from segment data block
    tmpData[0] = pixelToColor(segment.getBits(16));
    tmpData[1] = pixelToColor(segment.getBits(16));
    int ofs = 0;
    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++, ofs++) {
        tmpBlock[ofs] = tmpData[(y + x) & 1];
      }
    }
    writeBlock8x8(tmpBlock, startX, startY);
  }

  // converts a 16-bit R5G5B5 color into a 32-bit A8R8G8B8 color
  private int pixelToColor(int pixel) {
    return 0xff000000 | ((pixel & 0x7c00) << 9) | ((pixel & 0x03e0) << 6) | ((pixel & 0x1f) << 3);
  }

  // copy 8x8 pixel block from imgSrc to current buffer, using specified coordinates
  private void copyBlock8x8(BufferedImage src, int srcX, int srcY, int dstX, int dstY) {
    if (srcX >= 0 && srcX + 8 <= src.getWidth() && srcY >= 0 && srcY + 8 <= src.getHeight()) {
      int[] srcBuf = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
      int[] dstBuf = ((DataBufferInt) curBuffer.getRaster().getDataBuffer()).getData();
      int srcOfs = srcY * src.getWidth() + srcX;
      int dstOfs = dstY * curBuffer.getWidth() + dstX;
      if (srcOfs < 0) {
        Logger.debug("copyBlock8x8(src, {}, {}, {}, {})", srcX, srcY, dstX, dstY);
        return;
      }

      for (int y = 0; y < 8; y++, srcOfs += src.getWidth(), dstOfs += curBuffer.getWidth()) {
        System.arraycopy(srcBuf, srcOfs, dstBuf, dstOfs, 8);
      }
    }
  }

  // write a 8x8 pixel block to the current buffer at the specified coordinates
  private void writeBlock8x8(int[] block, int dstX, int dstY) {
    int[] dstBuf = ((DataBufferInt) curBuffer.getRaster().getDataBuffer()).getData();
    int dstOfs = dstY * curBuffer.getWidth() + dstX;

    for (int y = 0; y < 8; y++, dstOfs += curBuffer.getWidth()) {
      System.arraycopy(block, y << 3, dstBuf, dstOfs, 8);
    }
  }

  // ----------------------------- INNER CLASSES -----------------------------

  /**
   * Stores a color palette of variable size. The color component's range is always assumed [0..63].
   */
  public static class Palette {
    /**
     * Palette of 256 entries in format ARGB
     */
    private final int[] data;

    public Palette() {
      data = new int[256];
      clearPalette();
    }

    /**
     * Returns the specified palette entry.
     *
     * @param index The palette index (range: 0..255).
     * @return The palette entry in A8R8G8B8 format.
     */
    public int getColor(int index) {
      index &= 0xff;
      return data[index];
    }

    /**
     * Sets a specific palette entry to the specified RGB color.
     *
     * @param index The palette entry.
     * @param r     The red component
     * @param g     The green component
     * @param b     The blue component
     */
    public void setColor(int index, byte r, byte g, byte b) {
      index &= 0xff;
      data[index] = 0xff000000 | ((r & 0x3f) << 18) | ((g & 0x3f) << 10) | ((b & 0x3f) << 2);
    }

    /**
     * Provides access to the palette.
     *
     * @return The palette as integer array.
     */
    public int[] getPalette() {
      return data;
    }

    /**
     * Sets a range of palette entries
     *
     * @param startIndex First palette entry to set.
     * @param count      Number of palette entries to set.
     * @param rgbData    RGB triplet data buffer to get the new palette entries from.
     * @param rgbOfs     Start offset in rgbData
     */
    public void setPalette(int startIndex, int count, byte[] rgbData, int rgbOfs) {
      if (rgbData != null) {
        startIndex &= 0xff;
        if (startIndex + count > 256) {
          count = 256 - startIndex;
        }
        if (rgbData.length - rgbOfs < count * 3) {
          count = (rgbData.length - rgbOfs) / 3;
        }

        for (int i = 0; i < count; i++) {
          data[startIndex + i] = 0xff000000 | ((rgbData[rgbOfs + i * 3] & 0x3f) << 18)
              | ((rgbData[rgbOfs + i * 3 + 1] & 0x3f) << 10) | ((rgbData[rgbOfs + i * 3 + 2] & 0x3f) << 2);
        }
      }
    }

    /**
     * Sets all color entries to black.
     */
    public void clearPalette() {
      Arrays.fill(data, 0xff000000);
    }
  }

}
