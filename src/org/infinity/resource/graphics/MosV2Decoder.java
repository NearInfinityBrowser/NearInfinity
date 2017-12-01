// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.StreamUtils;

public class MosV2Decoder extends MosDecoder
{
  private final TreeSet<Integer> pvrIndices = new TreeSet<>();
  private static final int HeaderSize = 16;   // size of the MOS header
  private static final int BlockSize = 28;    // size of a single data block

  private ByteBuffer mosBuffer;
  private int width, height, blockCount, ofsData;

  public MosV2Decoder(ResourceEntry mosEntry)
  {
    super(mosEntry);
    init();
  }

  /**
   * Forces the internal cache to be filled with all PVRZ resources required by this MOS resource.
   * This will ensure that tiles are decoded at constant speed.
   */
  public void preloadPvrz()
  {
    for (int i = 0; i < getBlockCount(); i++) {
      int ofs = getBlockOffset(i);
      if (ofs > 0) {
        int page = mosBuffer.getInt(ofs);
        if (page >= 0) {
          getPVR(page);
        }
      }
    }
  }

  @Override
  public void close()
  {
    PvrDecoder.flushCache();
    pvrIndices.clear();
    mosBuffer = null;
    width = height = blockCount = 0;
    ofsData = 0;
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public ByteBuffer getResourceBuffer()
  {
    return mosBuffer;
  }

  @Override
  public int getWidth()
  {
    return width;
  }

  @Override
  public int getHeight()
  {
    return height;
  }

  @Override
  public Image getImage()
  {
    if (isInitialized()) {
      BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
      if (getImage(image)) {
        return image;
      } else {
        image = null;
      }
    }
    return null;
  }

  @Override
  public boolean getImage(Image canvas)
  {
    if (isInitialized() && canvas != null) {
      boolean bRet = false;
      for (int i = 0; i < getBlockCount(); i++) {
        int ofs = getBlockOffset(i);
        if (ofs > 0) {
          int dx = mosBuffer.getInt(ofs + 0x14);
          int dy = mosBuffer.getInt(ofs + 0x18);
          bRet |= renderBlock(i, canvas, dx, dy);
        }
      }
      return bRet;
    }
    return false;
  }

  @Override
  public int[] getImageData()
  {
    if (isInitialized()) {
      int[] buffer = new int[getWidth()*getHeight()];
      if (getImageData(buffer)) {
        return buffer;
      } else {
        buffer = null;
      }
    }
    return null;
  }

  @Override
  public boolean getImageData(int[] buffer)
  {
    if (isInitialized() && buffer != null) {
      boolean bRet = false;
      for (int i = 0; i < getBlockCount(); i++) {
        int ofs = getBlockOffset(i);
        if (ofs > 0) {
          int dx = mosBuffer.getInt(ofs + 0x14);
          int dy = mosBuffer.getInt(ofs + 0x18);
          bRet |= renderBlock(i, buffer, getWidth(), getHeight(), dx, dy);
        }
      }
      return bRet;
    }
    return false;
  }

  @Override
  public int getBlockCount()
  {
    return blockCount;
  }

  @Override
  public int getBlockWidth(int blockIdx)
  {
    int ofs = getBlockOffset(blockIdx);
    if (ofs > 0) {
      return mosBuffer.getInt(ofs + 0x0c);
    }
    return 0;
  }

  @Override
  public int getBlockHeight(int blockIdx)
  {
    int ofs = getBlockOffset(blockIdx);
    if (ofs > 0) {
      return mosBuffer.getInt(ofs + 0x10);
    }
    return 0;
  }

  @Override
  public Image getBlock(int blockIdx)
  {
    if (isValidBlock(blockIdx)) {
      Image image = ColorConvert.createCompatibleImage(getBlockWidth(blockIdx), getBlockHeight(blockIdx), true);
      if (renderBlock(blockIdx, image, 0, 0)) {
        return image;
      } else {
        image = null;
      }
    }
    return null;
  }

  @Override
  public boolean getBlock(int blockIdx, Image canvas)
  {
    if (isValidBlock(blockIdx) && canvas != null) {
      return renderBlock(blockIdx, canvas, 0, 0);
    }
    return false;
  }

  @Override
  public int[] getBlockData(int blockIdx)
  {
    if (isValidBlock(blockIdx)) {
      int w = getBlockWidth(blockIdx);
      int h = getBlockHeight(blockIdx);
      int[] buffer = new int[w*h];
      if (renderBlock(blockIdx, buffer, w, h, 0, 0)) {
        return buffer;
      } else {
        buffer = null;
      }
    }
    return null;
  }

  @Override
  public boolean getBlockData(int blockIdx, int[] buffer)
  {
    if (isValidBlock(blockIdx) && buffer != null) {
      int w = getBlockWidth(blockIdx);
      int h = getBlockHeight(blockIdx);
      if (buffer.length >= w*h) {
        return renderBlock(blockIdx, buffer, w, h, 0, 0);
      }
    }
    return false;
  }

  /** Returns the set of referenced PVRZ pages by this MOS. */
  public Set<Integer> getReferencedPVRZPages()
  {
    return Collections.unmodifiableSet(pvrIndices);
  }


  private void init()
  {
    close();

    if (getResourceEntry() != null) {
      try {
        mosBuffer = getResourceEntry().getResourceBuffer();
        String signature = StreamUtils.readString(mosBuffer, 0x00, 4);
        String version = StreamUtils.readString(mosBuffer, 0x04, 4);
        if ("MOS ".equals(signature) && "V2  ".equals(version)) {
          setType(Type.MOSV2);
        } else {
          throw new Exception("Invalid MOS type");
        }

        // evaluating header data
        width = mosBuffer.getInt(0x08);
        if (width <= 0) {
          throw new Exception("Invalid MOS width: " + width);
        }
        height = mosBuffer.getInt(0x0c);
        if (height <= 0) {
          throw new Exception("Invalid MOS height: " + height);
        }
        blockCount = mosBuffer.getInt(0x10);
        if (blockCount <= 0) {
          throw new Exception("Invalid number of data blocks: " + blockCount);
        }
        ofsData = mosBuffer.getInt(0x14);
        if (width < HeaderSize) {
          throw new Exception("Invalid data offset: " + ofsData);
        }
        // collecting referened pvrz pages
        for (int idx = 0; idx < blockCount; idx++) {
          int ofs = ofsData + (idx * 28);
          int page = mosBuffer.getInt(ofs);
          if (page >= 0) {
            pvrIndices.add(Integer.valueOf(page));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Returns and caches the PVRZ resource of the specified page
  private PvrDecoder getPVR(int page)
  {
    try {
      String name = String.format("MOS%04d.PVRZ", page);
      ResourceEntry entry = ResourceFactory.getResourceEntry(name);
      if (entry != null) {
        return PvrDecoder.loadPvr(entry);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // Returns if a valid MOS has been initialized
  private boolean isInitialized()
  {
    return (mosBuffer != null && blockCount > 0 && width > 0 && height > 0);
  }

  // Returns whether the specified block index is valid
  private boolean isValidBlock(int blockIdx)
  {
    return (blockIdx >= 0 && blockIdx < blockCount);
  }

  // Returns the start offset of the specified data block
  private int getBlockOffset(int blockIdx)
  {
    if (blockIdx >= 0 && blockIdx < blockCount) {
      return ofsData + blockIdx*BlockSize;
    }
    return -1;
  }

  // Renders the specified block onto the canvas at position (left, top)
  private boolean renderBlock(int blockIdx, Image canvas, int left, int top)
  {
    int ofsBlock = getBlockOffset(blockIdx);
    if (ofsBlock > 0 && canvas != null && left >= 0 && top >= 0) {
      int page = mosBuffer.getInt(ofsBlock);
      int srcX = mosBuffer.getInt(ofsBlock + 0x04);
      int srcY = mosBuffer.getInt(ofsBlock + 0x08);
      int blockWidth = mosBuffer.getInt(ofsBlock + 0x0c);
      int blockHeight = mosBuffer.getInt(ofsBlock + 0x10);
      PvrDecoder decoder = getPVR(page);
      if (decoder != null) {
        try {
          int w = (left + blockWidth < canvas.getWidth(null)) ? canvas.getWidth(null) - left : blockWidth;
          int h = (top + blockHeight < canvas.getHeight(null)) ? canvas.getHeight(null) - top : blockHeight;
          if (w > 0 && h > 0) {
            BufferedImage imgBlock = decoder.decode(srcX, srcY, blockWidth, blockHeight);
            Graphics2D g = (Graphics2D)canvas.getGraphics();
            try {
              g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
              g.drawImage(imgBlock, left, top, left + w, top + h, 0, 0, w, h, null);
            } finally {
              g.dispose();
              g = null;
            }
            imgBlock = null;
          }
          return true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return false;
  }

  // Writes the specified block into the buffer of specified dimensions at position (left, top)
  private boolean renderBlock(int blockIdx, int[] buffer, int width, int height, int left, int top)
  {
    int ofsBlock = getBlockOffset(blockIdx);
    if (ofsBlock > 0 && buffer != null && width > 0 && height > 0 && left >= 0 && top >= 0) {
      int page = mosBuffer.getInt(ofsBlock);
      int srcX = mosBuffer.getInt(ofsBlock + 0x04);
      int srcY = mosBuffer.getInt(ofsBlock + 0x08);
      int blockWidth = mosBuffer.getInt(ofsBlock + 0x0c);
      int blockHeight = mosBuffer.getInt(ofsBlock + 0x10);
      PvrDecoder decoder = getPVR(page);
      if (decoder != null) {
        try {
          int w = (left + blockWidth < width) ? width - left : blockWidth;
          int h = (top + blockHeight < height) ? height - top : blockHeight;
          if (w > 0 && h > 0) {
            BufferedImage imgBlock = decoder.decode(srcX, srcY, blockWidth, blockHeight);
            int[] srcData = ((DataBufferInt)imgBlock.getRaster().getDataBuffer()).getData();
            int srcOfs = 0;
            int dstOfs = top*width + left;
            for (int y = 0; y < h; y++) {
              System.arraycopy(srcData, srcOfs, buffer, dstOfs, w);
              srcOfs += blockWidth;
              dstOfs += width;
            }
            srcData = null;
            imgBlock = null;
            decoder = null;
            return true;
          }
        } catch (Exception e) {
          e.printStackTrace();
          decoder = null;
        }
      }
    }
    return false;
  }
}
