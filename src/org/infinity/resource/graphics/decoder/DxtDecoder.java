// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics.decoder;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Objects;

import org.infinity.util.DynamicArray;

/**
 * Texture decoder for DXT1, DXT3 and DXT5 pixel formats.
 */
public class DxtDecoder implements Decodable {
  private final PvrInfo info;

  /** Initializes a new {@code DXT} decoder from with the specified {@link PvrInfo}. */
  public DxtDecoder(PvrInfo pvr) {
    this.info = Objects.requireNonNull(pvr);
  }

  // --------------------- Begin Interface Decodable ---------------------

  @Override
  public boolean decode(BufferedImage image, Rectangle region) throws Exception {
    return decodeDXT(image, region);
  }

  @Override
  public PvrInfo getPvrInfo() {
    return info;
  }

  // --------------------- End Interface Decodable ---------------------

  private boolean decodeDXT(BufferedImage image, Rectangle region) throws Exception {
    if (image == null || region == null) {
      return false;
    }

    int imgWidth = image.getWidth();
    int imgHeight = image.getHeight();
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
    Rectangle rect = alignRectangle(region, 4, 4);

    // preparing aligned image buffer for faster rendering
    BufferedImage alignedImage = null;
    int imgWidthAligned;
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
      case DXT1:
        decodeDXT1(imgData, rect, imgWidthAligned);
        break;
      case DXT3:
        decodeDXT3(imgData, rect, imgWidthAligned);
        break;
      case DXT5:
        decodeDXT5(imgData, rect, imgWidthAligned);
        break;
      default:
        return false;
    }
    imgData = null;

    // copying aligned image back to target image
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

  // Performs DXT1-specific decoding on {@code imgData}, within the aligned bounds
  // specified by {@code rect} and {@code imgWidth}.
  private void decodeDXT1(int[] imgData, Rectangle rect, int imgWidth) {
    final int wordSize = 8; // data size of an encoded 4x4 pixel block
    int wordImageWidth = info.width >>> 2; // the image width in data blocks
    int wordRectWidth = rect.width >>> 2; // the aligned region's width in data blocks
    int wordRectHeight = rect.height >>> 2; // the aligned region's height in data blocks
    int wordPosX = rect.x >>> 2;
    int wordPosY = rect.y >>> 2;

    int[] colors = new int[8];
    int pvrOfs = (wordPosY * wordImageWidth + wordPosX) * wordSize;
    int imgOfs = 0;
    for (int y = 0; y < wordRectHeight; y++) {
      for (int x = 0; x < wordRectWidth; x++) {
        // decoding single DXT1 block
        int c = DynamicArray.getInt(info.data, pvrOfs);
        unpackColors565(c, colors);
        int code = DynamicArray.getInt(info.data, pvrOfs + 4);
        for (int idx = 0; idx < 16; idx++, code >>>= 2) {
          int ofs = imgOfs + (idx >>> 2) * imgWidth + (idx & 3);
          if ((code & 3) == 0) {
            // 100% c0, 0% c1
            imgData[ofs] = 0xff000000 | (colors[2] << 16) | (colors[1] << 8) | colors[0];
          } else if ((code & 3) == 1) {
            // 0% c0, 100% c1
            imgData[ofs] = 0xff000000 | (colors[6] << 16) | (colors[5] << 8) | colors[4];
          } else if ((code & 3) == 2) {
            if ((c & 0xffff) > ((c >>> 16) & 0xffff)) {
              // 66% c0, 33% c1
              int v = 0xff000000;
              v |= (((colors[2] << 1) + colors[6]) / 3) << 16;
              v |= (((colors[1] << 1) + colors[5]) / 3) << 8;
              v |= ((colors[0] << 1) + colors[4]) / 3;
              imgData[ofs] = v;
            } else {
              // 50% c0, 50% c1
              int v = 0xff000000;
              v |= ((colors[2] + colors[6]) >>> 1) << 16;
              v |= ((colors[1] + colors[5]) >>> 1) << 8;
              v |= (colors[0] + colors[4]) >>> 1;
              imgData[ofs] = v;
            }
          } else {
            if ((c & 0xffff) > ((c >>> 16) & 0xffff)) {
              // 33% c0, 66% c1
              int v = 0xff000000;
              v |= ((colors[2] + (colors[6] << 1)) / 3) << 16;
              v |= ((colors[1] + (colors[5] << 1)) / 3) << 8;
              v |= (colors[0] + (colors[4] << 1)) / 3;
              imgData[ofs] = v;
            } else {
              // transparent
              imgData[ofs] = 0;
            }
          }
        }

        pvrOfs += wordSize;
        imgOfs += 4;
      }
      pvrOfs += (wordImageWidth - wordRectWidth) * wordSize;
      imgOfs += imgWidth * 4 - rect.width;
    }
  }

  // Performs DXT3-specific decoding on {@code imgData}, within the aligned bounds
  // specified by {@code rect} and {@code imgWidth}.
  private void decodeDXT3(int[] imgData, Rectangle rect, int imgWidth) {
    final int wordSize = 16; // data size of an encoded 4x4 pixel block
    int wordImageWidth = info.width >>> 2; // the image width in data blocks
    int wordRectWidth = rect.width >>> 2; // the aligned region's width in data blocks
    int wordRectHeight = rect.height >>> 2; // the aligned region's height in data blocks
    int wordPosX = rect.x >>> 2;
    int wordPosY = rect.y >>> 2;

    int[] colors = new int[8];
    int pvrOfs = (wordPosY * wordImageWidth + wordPosX) * wordSize;
    int imgOfs = 0;
    for (int y = 0; y < wordRectHeight; y++) {
      for (int x = 0; x < wordRectWidth; x++) {
        // decoding single DXT3 block
        long alpha = DynamicArray.getByte(info.data, pvrOfs);
        int c = DynamicArray.getInt(info.data, pvrOfs + 8);
        unpackColors565(c, colors);
        int code = DynamicArray.getInt(info.data, pvrOfs + 12);
        for (int idx = 0; idx < 16; idx++, code >>>= 2, alpha >>>= 4) {
          // calculating alpha (4 bit -> 8 bit)
          int ofs = imgOfs + (idx >>> 2) * imgWidth + (idx & 3);
          int color = (int) (alpha & 0xf) << 24;
          color |= color << 4;
          // decoding pixels
          if ((code & 3) == 0) {
            // 100% c0, 0% c1
            color |= (colors[2] << 16) | (colors[1] << 8) | colors[0];
          } else if ((code & 3) == 1) {
            // 0% c0, 100% c1
            color |= (colors[6] << 16) | (colors[5] << 8) | colors[4];
          } else if ((code & 3) == 2) {
            // 66% c0, 33% c1
            int v = (((colors[2] << 1) + colors[6]) / 3) << 16;
            color |= v;
            v = (((colors[1] << 1) + colors[5]) / 3) << 8;
            color |= v;
            v = ((colors[0] << 1) + colors[4]) / 3;
            color |= v;
          } else {
            // 33% c0, 66% c1
            int v = ((colors[2] + (colors[6] << 1)) / 3) << 16;
            color |= v;
            v = ((colors[1] + (colors[5] << 1)) / 3) << 8;
            color |= v;
            v = (colors[0] + (colors[4] << 1)) / 3;
            if (v > 255)
              v = 255;
            color |= v;
          }
          imgData[ofs] = color;
        }

        pvrOfs += wordSize;
        imgOfs += 4;
      }
      pvrOfs += (wordImageWidth - wordRectWidth) * wordSize;
      imgOfs += (imgWidth << 2) - rect.width;
    }
  }

  // Performs DXT5-specific decoding on {@code imgData}, within the aligned bounds
  // specified by {@code rect} and {@code imgWidth}.
  private void decodeDXT5(int[] imgData, Rectangle rect, int imgWidth) {
    final int wordSize = 16; // data size of an encoded 4x4 pixel block
    int wordImageWidth = info.width >>> 2; // the image width in data blocks
    int wordRectWidth = rect.width >>> 2; // the aligned region's width in data blocks
    int wordRectHeight = rect.height >>> 2; // the aligned region's height in data blocks
    int wordPosX = rect.x >>> 2;
    int wordPosY = rect.y >>> 2;

    int[] alpha = new int[8];
    int[] colors = new int[8];
    int pvrOfs = (wordPosY * wordImageWidth + wordPosX) * wordSize;
    int imgOfs = 0;
    for (int y = 0; y < wordRectHeight; y++) {
      for (int x = 0; x < wordRectWidth; x++) {
        // creating alpha table
        alpha[0] = DynamicArray.getByte(info.data, pvrOfs) & 0xff;
        alpha[1] = DynamicArray.getByte(info.data, pvrOfs + 1) & 0xff;
        if (alpha[0] > alpha[1]) {
          alpha[2] = (6 * alpha[0] + alpha[1]) / 7;
          alpha[3] = (5 * alpha[0] + 2 * alpha[1]) / 7;
          alpha[4] = (4 * alpha[0] + 3 * alpha[1]) / 7;
          alpha[5] = (3 * alpha[0] + 4 * alpha[1]) / 7;
          alpha[6] = (2 * alpha[0] + 5 * alpha[1]) / 7;
          alpha[7] = (alpha[0] + 6 * alpha[1]) / 7;
        } else {
          alpha[2] = (4 * alpha[0] + alpha[1]) / 5;
          alpha[3] = (3 * alpha[0] + 2 * alpha[1]) / 5;
          alpha[4] = (2 * alpha[0] + 3 * alpha[1]) / 5;
          alpha[5] = (alpha[0] + 4 * alpha[1]) / 5;
          alpha[6] = 0;
          alpha[7] = 255;
        }

        // decoding single DXT5 block
        long ctrl = DynamicArray.getLong(info.data, pvrOfs + 2) & 0xffffffffffffL;
        int c = DynamicArray.getInt(info.data, pvrOfs + 8);
        unpackColors565(c, colors);
        int code = DynamicArray.getInt(info.data, pvrOfs + 12);
        for (int idx = 0; idx < 16; idx++, code >>>= 2, ctrl >>>= 3) {
          int ofs = imgOfs + (idx >>> 2) * imgWidth + (idx & 3);
          int color = alpha[(int) (ctrl & 7L)] << 24;
          if ((code & 3) == 0) {
            // 100% c0, 0% c1
            color |= (colors[2] << 16) | (colors[1] << 8) | colors[0];
          } else if ((code & 3) == 1) {
            // 0% c0, 100% c1
            color |= (colors[6] << 16) | (colors[5] << 8) | colors[4];
          } else if ((code & 3) == 2) {
            // 66% c0, 33% c1
            int v = (((colors[2] << 1) + colors[6]) / 3) << 16;
            color |= v;
            v = (((colors[1] << 1) + colors[5]) / 3) << 8;
            color |= v;
            v = ((colors[0] << 1) + colors[4]) / 3;
            color |= v;
          } else {
            // 33% c0, 66% c1
            int v = ((colors[2] + (colors[6] << 1)) / 3) << 16;
            color |= v;
            v = ((colors[1] + (colors[5] << 1)) / 3) << 8;
            color |= v;
            v = (colors[0] + (colors[4] << 1)) / 3;
            if (v > 255)
              v = 255;
            color |= v;
          }
          imgData[ofs] = color;
        }

        pvrOfs += wordSize;
        imgOfs += 4;
      }
      pvrOfs += (wordImageWidth - wordRectWidth) * wordSize;
      imgOfs += (imgWidth << 2) - rect.width;
    }
  }

  // Returns a rectangle that is aligned to the values specified as arguments 2 and 3.
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

  // Converts two RGB565 words into separate components, ordered { B, G, R, A, B, G, R, A }
  private static void unpackColors565(int inData, int[] outData) {
    outData[0] = ((inData << 3) & 0xf8) | (inData >>> 2) & 0x07; // b1
    outData[1] = ((inData >>> 3) & 0xfc) | (inData >>> 9) & 0x03; // g1
    outData[2] = ((inData >>> 8) & 0xf8) | (inData >>> 13) & 0x07; // r1
    outData[3] = 255; // a1
    outData[4] = ((inData >>> 13) & 0xf8) | (inData >>> 18) & 0x07; // b2
    outData[5] = ((inData >>> 19) & 0xfc) | (inData >>> 25) & 0x03; // g2
    outData[6] = ((inData >>> 24) & 0xf8) | (inData >>> 29) & 0x07; // r2
    outData[7] = 255; // a2
  }
}
