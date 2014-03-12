// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;


public class BamV1Decoder extends BamDecoder
{
  private final List<BamV1FrameEntry> listFrames = new ArrayList<BamV1FrameEntry>();
  private final List<CycleEntry> listCycles = new ArrayList<CycleEntry>();

  private byte[] bamData;           // contains the raw (uncompressed) data of the BAM resource
  private int[] palette;            // BAM palette
  private int rleIndex;             // color index for RLE compressed pixels
  private int[] externalPalette;    // optional external palette
  private int[] currentPalette;     // the currently used palette (either original, external or mixed palette)
  private boolean paletteEnabled;   // indicates whether to apply the external palette
  private boolean transparencyEnabled;
  private int currentCycle, currentFrame;
  private BufferedImage sharedCanvas;   // temporary buffer for drawing on a shared canvas

  /**
   * Loads and decodes a BAM v1 resource. This includes both compressed (BAMC) and uncompressed BAM
   * resource.
   * @param bamEntry The BAM resource entry.
   */
  public BamV1Decoder(ResourceEntry bamEntry)
  {
    this(bamEntry, null);
  }

  /**
   * Loads and decodes a BAM v1 resource. This includes both compressed (BAMC) and uncompressed BAM
   * resource.
   * @param bamEntry The BAM resource entry.
   * @param palette An optional external palette that can be used to change the colors of the BAM frames.
   */
  public BamV1Decoder(ResourceEntry bamEntry, int[] palette)
  {
    super(bamEntry);
    transparencyEnabled = true;
    paletteEnabled = false;
    setPalette(palette);
    init();
    preparePalette();
  }

  /**
   * Returns an external palette that has been assigned previously.
   * @return An external palette as int array, or an empty int array if no palette data has been assigned.
   */
  public int[] getPalette()
  {
    return externalPalette;
  }

  /**
   * Assigns a new external palette to the BAM. Any old external palette data will be discarded.
   * @param palette The new external palette as int array (format: ARGB).
   */
  public void setPalette(int[] palette)
  {
    this.externalPalette = null;
    if (palette != null) {
      this.externalPalette = new int[palette.length];
      System.arraycopy(palette, 0, this.externalPalette, 0, palette.length);
    } else {
      this.externalPalette = new int[0];
    }
  }

  /**
   * Returns whether the BAM uses an external palette if available.
   */
  public boolean isPaletteEnabled()
  {
    return paletteEnabled;
  }

  /**
   * Specify whether to use an external palette to draw the BAM frames.
   */
  public void setPaletteEnabled(boolean enable)
  {
    if (enable != paletteEnabled) {
      paletteEnabled = enable;
      preparePalette();
    }
  }

  /**
   * Returns whether the transparent palette entry is drawn or not.
   */
  public boolean isTransparencyEnabled()
  {
    return transparencyEnabled;
  }

  /**
   * Specify whether to draw the transparent palette entry.
   */
  public void setTransparencyEnabled(boolean enable)
  {
    if (enable != transparencyEnabled) {
      transparencyEnabled = enable;
    }
  }

  @Override
  public BamV1FrameEntry getFrameInfo(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx);
    } else {
      return null;
    }
  }

  @Override
  public void close()
  {
    setType(Type.INVALID);
    bamData = null;
    palette = null;
    listFrames.clear();
    listCycles.clear();
    rleIndex = 0;
    currentCycle = currentFrame = 0;
    if (sharedCanvas != null) {
      sharedCanvas.flush();
      sharedCanvas = null;
    }
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public byte[] getResourceData()
  {
    return bamData;
  }

  @Override
  public int frameCount()
  {
    return listFrames.size();
  }

  @Override
  public int frameImageWidth(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (frameWidth(frameIdx) > 0) {
        if (getMode() == Mode.Shared) {
          return getSharedRectangle().width;
        } else {
          return frameWidth(frameIdx);
        }
      }
    }
    return 0;
  }

  @Override
  public int frameImageHeight(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (frameHeight(frameIdx) > 0) {
        if (getMode() == Mode.Shared) {
          return getSharedRectangle().height;
        } else {
          return frameHeight(frameIdx);
        }
      }
    }
    return 0;
  }

  @Override
  public Image frameGet(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      int w = frameImageWidth(frameIdx);
      int h = frameImageHeight(frameIdx);
      if (w > 0 && h > 0) {
        BufferedImage image = ColorConvert.createCompatibleImage(w, h, true);
        decodeFrame(frameIdx, image);
        return image;
      }
    }
    return ColorConvert.createCompatibleImage(1, 1, true);
  }

  @Override
  public void frameGet(int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      int w = frameImageWidth(frameIdx);
      int h = frameImageHeight(frameIdx);
      if (w > 0 && h > 0 && canvas.getWidth(null) >= w && canvas.getHeight(null) >= h) {
        decodeFrame(frameIdx, canvas);
      }
    }
  }

  @Override
  public int[] frameGetData(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      int w = frameImageWidth(frameIdx);
      int h = frameImageHeight(frameIdx);
      if (w > 0 && h > 0) {
        int[] buffer = new int[w*h];
        decodeFrame(frameIdx, buffer, w, h);
        return buffer;
      }
    }
    return new int[0];
  }

  @Override
  public int frameWidth(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getWidth();
    } else {
      return 0;
    }
  }

  @Override
  public int frameHeight(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getHeight();
    } else {
      return 0;
    }
  }

  @Override
  public int frameCenterX(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getCenterX();
    } else {
      return 0;
    }
  }

  @Override
  public int frameCenterY(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getCenterY();
    } else {
      return 0;
    }
  }

  @Override
  public int cycleCount()
  {
    return listCycles.size();
  }

  @Override
  public int cycleFrameCount()
  {
    if (currentCycle < listCycles.size()) {
      return listCycles.get(currentCycle).frames.length;
    } else {
      return 0;
    }
  }

  @Override
  public int cycleFrameCount(int cycleIdx)
  {
    if (cycleIdx >= 0 && cycleIdx < listCycles.size()) {
      return listCycles.get(cycleIdx).frames.length;
    } else {
      return 0;
    }
  }

  @Override
  public int cycleGet()
  {
    return currentCycle;
  }

  @Override
  public boolean cycleSet(int cycleIdx)
  {
    if (cycleIdx >= 0 && cycleIdx < listCycles.size()) {
      currentCycle = cycleIdx;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean cycleHasNextFrame()
  {
    if (currentCycle < listCycles.size()) {
      return (currentFrame < listCycles.get(currentCycle).frames.length - 1);
    } else {
      return false;
    }
  }

  @Override
  public boolean cycleNextFrame()
  {
    if (cycleHasNextFrame()) {
      currentFrame++;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void cycleReset()
  {
    currentFrame = 0;
  }

  @Override
  public Image cycleGetFrame()
  {
    int frameIdx = cycleGetFrameIndexAbsolute();
    return frameGet(frameIdx);
  }

  @Override
  public void cycleGetFrame(Image canvas)
  {
    int frameIdx = cycleGetFrameIndexAbsolute();
    frameGet(frameIdx, canvas);
  }

  @Override
  public Image cycleGetFrame(int frameIdx)
  {
    frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
    return frameGet(frameIdx);
  }

  @Override
  public void cycleGetFrame(int frameIdx, Image canvas)
  {
    frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
    frameGet(frameIdx, canvas);
  }

  @Override
  public int[] cycleGetFrameData()
  {
    int frameIdx = cycleGetFrameIndexAbsolute();
    return frameGetData(frameIdx);
  }

  @Override
  public int[] cycleGetFrameData(int frameIdx)
  {
    frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
    return frameGetData(frameIdx);
  }

  @Override
  public int cycleGetFrameIndex()
  {
    return currentFrame;
  }

  @Override
  public boolean cycleSetFrameIndex(int frameIdx)
  {
    if (currentCycle < listCycles.size() &&
        frameIdx >= 0 && frameIdx < listCycles.get(currentCycle).frames.length) {
      currentFrame = frameIdx;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int cycleGetFrameIndexAbsolute()
  {
    if (currentCycle < listCycles.size() &&
        currentFrame < listCycles.get(currentCycle).frames.length) {
      return listCycles.get(currentCycle).frames[currentFrame];
    } else {
      return 0;
    }
  }

  @Override
  public int cycleGetFrameIndexAbsolute(int frameIdx)
  {
    if (currentCycle < listCycles.size() &&
        frameIdx >= 0 && frameIdx < listCycles.get(currentCycle).frames.length) {
      return listCycles.get(currentCycle).frames[frameIdx];
    } else {
      return 0;
    }
  }

  @Override
  public int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx)
  {
    if (cycleIdx >= 0 && cycleIdx < listCycles.size() &&
        frameIdx >= 0 && frameIdx < listCycles.get(cycleIdx).frames.length) {
      return listCycles.get(cycleIdx).frames[frameIdx];
    } else {
      return 0;
    }
  }


  // Initializes the current BAM
  private void init()
  {
    // resetting data
    close();

    if (getResourceEntry() != null) {
      try {
        bamData = getResourceEntry().getResourceData();
        String signature = DynamicArray.getString(bamData, 0x00, 4);
        String version = DynamicArray.getString(bamData, 0x04, 4);
        if ("BAMC".equals(signature)) {
          setType(Type.BAMC);
          bamData = Compressor.decompress(bamData);
          signature = DynamicArray.getString(bamData, 0x00, 4);
          version = DynamicArray.getString(bamData, 0x04, 4);
        } else if ("BAM ".equals(signature)) {
          setType(Type.BAMV1);
        } else {
          throw new Exception("Invalid BAM type");
        }
        // Data should now be in BAM v1 format
        if (!"BAM ".equals(signature) || !"V1  ".equals(version)) {
          throw new Exception("Invalid BAM type");
        }

        // evaluating header data
        int framesCount = DynamicArray.getUnsignedShort(bamData, 0x08);
        if (framesCount <= 0) {
          throw new Exception("Invalid number of frames");
        }
        int cyclesCount = DynamicArray.getUnsignedByte(bamData, 0x0a);
        if (cyclesCount <= 0) {
          throw new Exception("Invalid number of cycles");
        }
        rleIndex = DynamicArray.getUnsignedByte(bamData, 0x0b);
        int ofsFrames = DynamicArray.getInt(bamData, 0x0c);
        if (ofsFrames < 0x18) {
          throw new Exception("Invalid frames offset");
        }
        int ofsPalette = DynamicArray.getInt(bamData, 0x10);
        if (ofsPalette < 0x18) {
          throw new Exception("Invalid palette offset");
        }
        int ofsLookup = DynamicArray.getInt(bamData, 0x14);
        if (ofsLookup < 0x18) {
          throw new Exception("Invalid frame lookup table offset");
        }

        int ofs = ofsFrames;
        // initializing frames
        for (int i = 0; i < framesCount; i++) {
          listFrames.add(new BamV1FrameEntry(bamData, ofs));
          ofs += 0x0c;
        }

        // initializing cycles
        for (int i = 0; i < cyclesCount; i++) {
          int cnt = DynamicArray.getUnsignedShort(bamData, ofs);
          int idx = DynamicArray.getUnsignedShort(bamData, ofs+2);
          listCycles.add(new CycleEntry(bamData, ofsLookup, cnt, idx));
          ofs += 0x04;
        }

        // initializing palette
        palette = new int[256];
        for (int i = 0; i < 256; i++) {
          palette[i] = DynamicArray.getInt(bamData, ofsPalette + 4*i);
        }

        updateSharedBamSize();
        sharedCanvas = new BufferedImage(getSharedRectangle().width, getSharedRectangle().height,
                                         BufferedImage.TYPE_INT_ARGB);
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Prepares the palette to be used for decoding BAM frames
  private void preparePalette()
  {
    if (currentPalette == null) {
      currentPalette = new int[256];
    }
    if (isPaletteEnabled()) {
      int idx = 0;
      // filling palette entries from external palette, as much as possible
      for (; idx < externalPalette.length && idx < 256; idx++) {
        currentPalette[idx] = externalPalette[idx];
      }
      // filling remaining entried from original palette
      for (; idx < palette.length; idx++) {
        currentPalette[idx] = palette[idx];
      }
    } else {
      System.arraycopy(palette, 0, currentPalette, 0, palette.length);
    }
  }

  // Determines the transparent color index for the specified frame
  private int getTransparencyIndex(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      for (int i = 0; i < currentPalette.length; i++) {
        if ((currentPalette[i] & 0x00ffffff) == 0x0000ff00) {
          return i;
        }
      }
    }
    return 0;
  }

  // Draws the absolute frame onto the canvas.
  private void decodeFrame(int frameIdx, Image canvas)
  {
    BufferedImage image = null;
    if (canvas instanceof BufferedImage) {
      image = (BufferedImage)canvas;
    } else {
      image = sharedCanvas;
    }
    int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    decodeFrame(frameIdx, buffer, image.getWidth(), image.getHeight());
    buffer = null;
    if (image != canvas) {
      Graphics g = canvas.getGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      image.flush();
      image = null;
    }
  }

  // Draws the absolute frame into the buffer. Takes BAM mode, transparency and external palette into account.
  private void decodeFrame(int frameIdx, int[] buffer, int width, int height)
  {
    Arrays.fill(buffer, 0);
    if (frameIdx >= 0 && frameIdx < listFrames.size() &&
        buffer != null && buffer.length >= width*height) {
      boolean isTransparent = isTransparencyEnabled();
      boolean isCompressed = listFrames.get(frameIdx).compressed;
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      byte[] data = bamData;
      int ofsData = listFrames.get(frameIdx).ofsData;
      int transIndex = getTransparencyIndex(frameIdx);

      int left, top, maxWidth, maxHeight, srcOfs, dstOfs;
      int count = 0, color = 0;
      if (getMode() == Mode.Shared) {
        left = -getSharedRectangle().x - listFrames.get(frameIdx).centerX;
        top = -getSharedRectangle().y - listFrames.get(frameIdx).centerY;
        maxWidth = (width < srcWidth + left) ? width : srcWidth;
        maxHeight = (height < srcHeight + top) ? height : srcHeight;
        srcOfs = ofsData;
        dstOfs = top*width + left;
      } else {
        left = top = 0;
        maxWidth = (width < srcWidth) ? width : srcWidth;
        maxHeight = (height < srcHeight) ? height : srcHeight;
        srcOfs = ofsData;
        dstOfs = 0;
      }
      for (int y = 0; y < maxHeight; y++) {
        for (int x = 0; x < srcWidth; x++) {
          if (count > 0) {
            // writing remaining RLE compressed pixels
            count--;
            if (x < maxWidth) {
              buffer[dstOfs+x] = color;
            }
          } else {
            int pixel = data[srcOfs++] & 0xff;
            color = currentPalette[pixel] | 0xff000000;
            if (isTransparent && pixel == transIndex) {
              color = 0;
            }
            if (isCompressed && pixel == rleIndex) {
              count = data[srcOfs++] & 0xff;
            }
            if (x < maxWidth) {
              buffer[dstOfs+x] = color;
            }
          }
        }
        dstOfs += width;
      }
    }
  }


//-------------------------- INNER CLASSES --------------------------

  // Stores information for a single frame entry
  public class BamV1FrameEntry implements BamDecoder.FrameEntry
  {
    private int width, height, centerX, centerY, ofsData;
    private boolean compressed;

    private BamV1FrameEntry(byte[] buffer, int ofs)
    {
      if (buffer != null && ofs + 12 <= buffer.length) {
        width = DynamicArray.getUnsignedShort(buffer, ofs + 0);
        height = DynamicArray.getUnsignedShort(buffer, ofs + 2);
        centerX = DynamicArray.getShort(buffer, ofs + 4);
        centerY = DynamicArray.getShort(buffer, ofs + 6);
        ofsData = DynamicArray.getInt(buffer, ofs + 8) & 0x7fffffff;
        compressed = (DynamicArray.getInt(buffer, ofs + 8) & 0x80000000) == 0;
      } else {
        width = height = centerX = centerY = ofsData = 0;
        compressed = false;
      }
    }

    @Override
    public int getWidth() { return width; }
    @Override
    public int getHeight() { return height; }
    @Override
    public int getCenterX() { return centerX; }
    @Override
    public int getCenterY() { return centerY; }

    public boolean isCompressed() { return compressed; }
  }

  // Stores information for a single cycle
  private class CycleEntry
  {
    private final int[] frames;    // list of frame indices used in this cycle

    private int indexCount;        // number of frame indices in this cycle
    private int lookupIndex;       // index into frame lookup table

    /**
     * @param buffer The BAM data buffer
     * @param ofsLookup Offset of frame lookup table
     * @param idxCount Number of frame indices in this cycle
     * @param idxLookup Index into frame lookup table of first frame in this cycle
     */
    private CycleEntry(byte[] buffer, int ofsLookup, int idxCount, int idxLookup)
    {
      if (buffer != null && idxCount >= 0 && idxLookup >= 0 &&
          ofsLookup + 2*(idxLookup+idxCount) <= buffer.length) {
        indexCount = idxCount;
        lookupIndex = idxLookup;
        frames = new int[indexCount];
        for (int i = 0; i < indexCount; i++) {
          frames[i] = DynamicArray.getUnsignedShort(buffer, ofsLookup + 2*(lookupIndex+i));
        }
      } else {
        frames = new int[0];
        indexCount = lookupIndex = 0;
      }
    }
  }
}
