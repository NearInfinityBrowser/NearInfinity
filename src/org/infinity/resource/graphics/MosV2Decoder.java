// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.StreamUtils;

public class MosV2Decoder extends MosDecoder {
  private static final int HEADER_SIZE = 16;  // size of the MOS header

  private final List<MosBlock> dataBlocks = new ArrayList<>();

  private ByteBuffer mosBuffer;
  private int width;
  private int height;
  private int blockCount;
  private int ofsData;

  public MosV2Decoder(ResourceEntry mosEntry) {
    super(mosEntry);
    init();
  }

  /**
   * Forces the internal cache to be filled with all PVRZ resources required by this MOS resource. This will ensure that
   * tiles are decoded at constant speed.
   */
  public void preloadPvrz() {
    dataBlocks.stream().filter((e) -> e.page >= 0).forEach((e) -> getPVR(e.page));
  }

  @Override
  public void close() {
    PvrDecoder.flushCache();
    dataBlocks.clear();
    mosBuffer = null;
    width = height = blockCount = 0;
    ofsData = 0;
  }

  @Override
  public void reload() {
    init();
  }

  @Override
  public ByteBuffer getResourceBuffer() {
    return mosBuffer;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public Image getImage() {
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
  public boolean getImage(Image canvas) {
    if (isInitialized() && canvas != null) {
      return dataBlocks.stream().allMatch(e -> renderBlock(e, canvas));
    }
    return false;
  }

  @Override
  public int[] getImageData() {
    if (isInitialized()) {
      int[] buffer = new int[getWidth() * getHeight()];
      if (getImageData(buffer)) {
        return buffer;
      } else {
        buffer = null;
      }
    }
    return null;
  }

  @Override
  public boolean getImageData(int[] buffer) {
    if (isInitialized() && buffer != null) {
      return dataBlocks.stream().allMatch(e -> renderBlock(e, buffer, getWidth(), getHeight()));
    }
    return false;
  }

  @Override
  public int getBlockCount() {
    return blockCount;
  }

  @Override
  public int getBlockWidth(int blockIdx) {
    if (blockIdx >= 0 && blockIdx < dataBlocks.size()) {
      return dataBlocks.get(blockIdx).pvrzRect.width;
    }
    return 0;
  }

  @Override
  public int getBlockHeight(int blockIdx) {
    if (blockIdx >= 0 && blockIdx < dataBlocks.size()) {
      return dataBlocks.get(blockIdx).pvrzRect.height;
    }
    return 0;
  }

  @Override
  public Image getBlock(int blockIdx) {
    if (isValidBlock(blockIdx)) {
      Image image = ColorConvert.createCompatibleImage(getBlockWidth(blockIdx), getBlockHeight(blockIdx), true);
      if (renderBlock(dataBlocks.get(blockIdx), image, 0, 0)) {
        return image;
      } else {
        image = null;
      }
    }
    return null;
  }

  @Override
  public boolean getBlock(int blockIdx, Image canvas) {
    if (isValidBlock(blockIdx) && canvas != null) {
      return renderBlock(dataBlocks.get(blockIdx), canvas, 0, 0);
    }
    return false;
  }

  @Override
  public int[] getBlockData(int blockIdx) {
    if (isValidBlock(blockIdx)) {
      int w = getBlockWidth(blockIdx);
      int h = getBlockHeight(blockIdx);
      int[] buffer = new int[w * h];
      if (renderBlock(dataBlocks.get(blockIdx), buffer, 0, 0)) {
        return buffer;
      } else {
        buffer = null;
      }
    }
    return null;
  }

  @Override
  public boolean getBlockData(int blockIdx, int[] buffer) {
    if (isValidBlock(blockIdx) && buffer != null) {
      int w = getBlockWidth(blockIdx);
      int h = getBlockHeight(blockIdx);
      if (buffer.length >= w * h) {
        return renderBlock(dataBlocks.get(blockIdx), buffer, 0, 0);
      }
    }
    return false;
  }

  /**
   * Returns the MOS block index at the specified image location.
   *
   * @param location Pixel position within the MOS graphics.
   * @return The MOS block index if available, -1 otherwise.
   */
  public int getBlockIndexAt(Point location) {
    for (int i = 0, count = dataBlocks.size(); i < count; i++) {
      if (dataBlocks.get(i).mosRect.contains(location)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns {@code MosBlock} information for the pixel at the specified image location.
   *
   * @param location Pixel position within the MOS graphics.
   * @return A {@code MosBlock} instance if available, {@code null} otherwise.
   */
  public MosBlock getBlockInfoAt(Point location) {
    return dataBlocks.stream().filter(e -> e.mosRect.contains(location)).findFirst().orElse(null);
  }

  /**
   * Returns {@code MosBlock} information for the specified MOS block.
   *
   * @param index The MOS block index.
   * @return A {@code MosBlock} instance if available, {@code null} otherwise.
   */
  public MosBlock getBlockInfo(int index) {
    if (index >= 0 && index < dataBlocks.size()) {
      return dataBlocks.get(index);
    }
    return null;
  }

  /** Returns the set of referenced PVRZ pages by this MOS. */
  public Set<Integer> getReferencedPVRZPages() {
    return dataBlocks.stream().map((e) -> e.page).collect(Collectors.toSet());
  }

  private void init() {
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
        if (width < HEADER_SIZE) {
          throw new Exception("Invalid data offset: " + ofsData);
        }
        // collecting referened pvrz pages
        dataBlocks.clear();
        for (int idx = 0; idx < blockCount; idx++) {
          int ofs = ofsData + (idx * 28);
          int page = mosBuffer.getInt(ofs);
          int sx = mosBuffer.getInt(ofs + 0x04);
          int sy = mosBuffer.getInt(ofs + 0x08);
          int w = mosBuffer.getInt(ofs + 0x0c);
          int h = mosBuffer.getInt(ofs + 0x10);
          int dx = mosBuffer.getInt(ofs + 0x14);
          int dy = mosBuffer.getInt(ofs + 0x18);
          final MosBlock block = new MosBlock(page, sx, sy, w, h, dx, dy);
          dataBlocks.add(block);
        }
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Returns and caches the PVRZ resource of the specified page
  private PvrDecoder getPVR(int page) {
    try {
      ResourceEntry entry = ResourceFactory.getResourceEntry(getPvrzFileName(page));
      if (entry != null) {
        return PvrDecoder.loadPvr(entry);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // Returns if a valid MOS has been initialized
  private boolean isInitialized() {
    return (mosBuffer != null && blockCount > 0 && width > 0 && height > 0);
  }

  // Returns whether the specified block index is valid
  private boolean isValidBlock(int blockIdx) {
    return (blockIdx >= 0 && blockIdx < blockCount);
  }

  // Renders the specified block onto the canvas at predefined position
  private boolean renderBlock(MosBlock block, Image canvas) {
    if (block != null) {
      return renderBlock(block, canvas, block.mosRect.x, block.mosRect.y);
    }
    return false;
  }

  // Renders the specified block onto the canvas at position (left, top)
  private boolean renderBlock(MosBlock block, Image canvas, int left, int top) {
    if (block == null) {
      return false;
    }

    final PvrDecoder decoder = getPVR(block.page);
    if (decoder != null) {
      try {
        final Rectangle pvrzRect = block.pvrzRect;
        final Rectangle mosRect = block.mosRect;

        int w = (left + mosRect.width < canvas.getWidth(null)) ? canvas.getWidth(null) - left : mosRect.width;
        int h = (top + mosRect.height < canvas.getHeight(null)) ? canvas.getHeight(null) - top : mosRect.height;
        if (w > 0 && h > 0) {
          BufferedImage imgBlock = decoder.decode(pvrzRect.x, pvrzRect.y, pvrzRect.width, pvrzRect.height);
          Graphics2D g = (Graphics2D) canvas.getGraphics();
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
    return false;
  }

  // Writes the specified block into the buffer of specified dimensions at predefined position
  private boolean renderBlock(MosBlock block, int[] buffer, int width, int height) {
    if (block != null) {
      return renderBlock(block, buffer, width, height, block.mosRect.x, block.mosRect.y);
    }
    return false;
  }

  // Writes the specified block into the buffer of specified dimensions at position (left, top)
  private boolean renderBlock(MosBlock block, int[] buffer, int width, int height, int left, int top) {
    if (block == null || buffer == null || width < 0 || height < 0) {
      return false;
    }

    final PvrDecoder decoder = getPVR(block.page);
    if (decoder != null) {
      try {
        final Rectangle pvrzRect = block.pvrzRect;
        final Rectangle mosRect = block.mosRect;

        int w = (left + mosRect.width < width) ? width - left : mosRect.width;
        int h = (top + mosRect.height < height) ? height - top : mosRect.height;
        if (w > 0 && h > 0) {
          BufferedImage imgBlock = decoder.decode(pvrzRect.x, pvrzRect.y, pvrzRect.width, pvrzRect.height);
          int[] srcData = ((DataBufferInt) imgBlock.getRaster().getDataBuffer()).getData();
          int srcOfs = 0;
          int dstOfs = top * width + left;
          for (int y = 0; y < h; y++) {
            System.arraycopy(srcData, srcOfs, buffer, dstOfs, w);
            srcOfs += pvrzRect.width;
            dstOfs += width;
          }
          srcData = null;
          imgBlock = null;
          return true;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   * Returns the PVRZ resource filename for the specified pvrz page.
   *
   * @param page A page index between 0 and 10000.
   * @return The PVRZ resource filename. Returns empty string if {@code page} is out of bounds.
   */
  public static String getPvrzFileName(int page) {
    if (page >= 0 && page < 100000) {
      return String.format("MOS%04d.PVRZ", page);
    }
    return "";
  }

  // -------------------------- INNER CLASSES --------------------------

  /**
   * Provides information about a single MOS data block.
   */
  public static class MosBlock {
    private final int page;
    private final Rectangle pvrzRect;
    private final Rectangle mosRect;

    /**
     * Creates a new MosBlock instance.
     *
     * @param page The PVRZ page.
     * @param srcX Source (PVRZ) x coordinate of the graphics block.
     * @param srcY Source (PVRZ) y coordinate of the graphics block.
     * @param width Block width, in pixels.
     * @param height Block height, in pixels.
     * @param dstX Destination (MOS) x coordinate of the graphics block.
     * @param dstY Destination (MOS) x coordinate of the graphics block.
     */
    public MosBlock(int page, int srcX, int srcY, int width, int height, int dstX, int dstY) {
      this.page = page;
      this.pvrzRect = new Rectangle(srcX, srcY, width, height);
      this.mosRect = new Rectangle(dstX, dstY, width, height);
    }

    /** Returns the page index of the PVRZ containing this graphics block. */
    public int getPage() {
      return page;
    }

    /** Returns the source (PVRZ) rectangle of pixel data for this graphics block. */
    public Rectangle getPvrzRect() {
      return pvrzRect;
    }

    /** Returns the destination (MOS) rectangle of pixel data for this graphics block. */
    public Rectangle getMosRect() {
      return mosRect;
    }

    /** Returns the width of the graphics block, in pixels. */
    public int getWidth() {
      return pvrzRect.width;
    }

    /** Returns the height of the graphics block, in pixels. */
    public int getHeight() {
      return pvrzRect.height;
    }
  }

}
