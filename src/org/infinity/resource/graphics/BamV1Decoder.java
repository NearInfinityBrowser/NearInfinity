// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;


/**
 * Handles BAM v1 resources (both BAMC and uncompressed BAM V1).
 * @author argent77
 */
public class BamV1Decoder extends BamDecoder
{
  /**
   * Definitions on how to handle palette transparency:<br>
   * {@code Normal} looks for the first entry containing RGB(0, 255, 0). It falls back to palette
   * index 0 if no entry has been found.<br>
   * {@code FirstIndexOnly} automatically uses palette index 0 without looking for entries
   * containing RGB(0, 255, 0).
   */
  public enum TransparencyMode { NORMAL, FIRST_INDEX_ONLY }

  private final List<BamV1FrameEntry> listFrames = new ArrayList<BamV1FrameEntry>();
  private final List<CycleEntry> listCycles = new ArrayList<CycleEntry>();
  private final BamV1FrameEntry defaultFrameInfo = new BamV1FrameEntry(null, 0);

  private BamV1Control defaultControl;
  private byte[] bamData;   // contains the raw (uncompressed) data of the BAM resource
  private int[] bamPalette;    // BAM palette
  private int rleIndex;     // color index for RLE compressed pixels

  /**
   * Loads and decodes a BAM v1 resource. This includes both compressed (BAMC) and uncompressed BAM
   * resource.
   * @param bamEntry The BAM resource entry.
   */
  public BamV1Decoder(ResourceEntry bamEntry)
  {
    super(bamEntry);
    init();
  }


  @Override
  public BamV1Control createControl()
  {
    return new BamV1Control(this);
  }

  @Override
  public BamV1FrameEntry getFrameInfo(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx);
    } else {
      return defaultFrameInfo;
    }
  }

  @Override
  public void close()
  {
    bamData = null;
    bamPalette = null;
    listFrames.clear();
    listCycles.clear();
    rleIndex = 0;
  }

  @Override
  public boolean isOpen()
  {
    return (bamData != null);
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
  public Image frameGet(BamControl control, int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (control == null) {
        control = defaultControl;
      }
      int w, h;
      if (control.getMode() == BamDecoder.BamControl.Mode.SHARED) {
        Dimension d = control.getSharedDimension();
        w = d.width;
        h = d.height;
      } else {
        w = getFrameInfo(frameIdx).getWidth();
        h = getFrameInfo(frameIdx).getHeight();
      }
      if (w > 0 && h > 0) {
        BufferedImage image = ColorConvert.createCompatibleImage(w, h, true);
        frameGet(control, frameIdx, image);
        return image;
      }
    }
    return ColorConvert.createCompatibleImage(1, 1, true);
  }

  @Override
  public void frameGet(BamControl control, int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      if(control == null) {
        control = defaultControl;
      }
      int w, h;
      if (control.getMode() == BamDecoder.BamControl.Mode.SHARED) {
        Dimension d = control.getSharedDimension();
        w = d.width;
        h = d.height;
      } else {
        w = getFrameInfo(frameIdx).getWidth();
        h = getFrameInfo(frameIdx).getHeight();
      }
      if (w > 0 && h > 0 && canvas.getWidth(null) >= w && canvas.getHeight(null) >= h) {
        decodeFrame(control, frameIdx, canvas);
      }
    }
  }

  /** Returns the compressed color index for compressed BAM v1 resources. */
  public int getRleIndex()
  {
    return rleIndex;
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
        } else if ("BAM ".equals(signature) && "V1  ".equals(version)) {
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
        bamPalette = new int[256];
        int alphaMask = Profile.isEnhancedEdition() ? 0 : 0xff000000;
        boolean alphaUsed = false;  // determines whether alpha is actually used
        for (int i = 0; i < 256; i++) {
          bamPalette[i] = alphaMask | DynamicArray.getInt(bamData, ofsPalette + 4*i);
          alphaUsed |= (bamPalette[i] & 0xff000000) != 0;
        }
        if (!alphaUsed) {
          // fix palette if needed
          for (int i = 0; i < bamPalette.length; i++) {
            bamPalette[i] |= 0xff000000;
          }
        }

        // creating default bam control instance as a fallback option
        defaultControl = new BamV1Control(this);
        defaultControl.setMode(BamControl.Mode.SHARED);
        defaultControl.setSharedPerCycle(false);
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Draws the absolute frame onto the canvas.
  private void decodeFrame(BamControl control, int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (control == null) {
        control = defaultControl;
      }
      int[] palette;
      if (control instanceof BamV1Control) {
        palette = ((BamV1Control)control).getCurrentPalette();
      } else {
        palette = bamPalette;
      }

      // decoding frame data
      BufferedImage image = ColorConvert.toBufferedImage(canvas, true, false);
      byte[] bufferB = null;
      int[] bufferI = null;
      if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        bufferB = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
      } else {
        bufferI = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      }
      int dstWidth = image.getWidth();
      int dstHeight = image.getHeight();
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      boolean isCompressed = listFrames.get(frameIdx).compressed;
      int ofsData = listFrames.get(frameIdx).ofsData;

      int left, top, maxWidth, maxHeight, srcOfs, dstOfs;
      int count = 0, color = 0;
      byte pixel = 0;
      if (control.getMode() == BamControl.Mode.SHARED) {
        left = -control.getSharedRectangle().x - listFrames.get(frameIdx).centerX;
        top = -control.getSharedRectangle().y - listFrames.get(frameIdx).centerY;
        maxWidth = (dstWidth < srcWidth + left) ? dstWidth : srcWidth;
        maxHeight = (dstHeight < srcHeight + top) ? dstHeight : srcHeight;
        srcOfs = ofsData;
        dstOfs = top*dstWidth + left;
      } else {
        left = top = 0;
        maxWidth = (dstWidth < srcWidth) ? dstWidth : srcWidth;
        maxHeight = (dstHeight < srcHeight) ? dstHeight : srcHeight;
        srcOfs = ofsData;
        dstOfs = 0;
      }
      for (int y = 0; y < maxHeight; y++) {
        for (int x = 0; x < srcWidth; x++, dstOfs++) {
          if (count > 0) {
            // writing remaining RLE compressed pixels
            count--;
            if (x < maxWidth) {
              if (bufferB != null) bufferB[dstOfs] = pixel;
              if (bufferI != null) bufferI[dstOfs] = color;
            }
          } else {
            pixel = bamData[srcOfs++];
            color = palette[pixel & 0xff];
            if (isCompressed && (pixel & 0xff) == rleIndex) {
              count = bamData[srcOfs++] & 0xff;
            }
            if (x < maxWidth) {
              if (bufferB != null) bufferB[dstOfs] = pixel;
              if (bufferI != null) bufferI[dstOfs] = color;
            }
          }
        }
        dstOfs += dstWidth - srcWidth;
      }
      bufferB = null;
      bufferI = null;

      // rendering resulting image onto the canvas if needed
      if (image != canvas) {
        Graphics g = canvas.getGraphics();
        try {
          g.drawImage(image, 0, 0, null);
        } finally {
          g.dispose();
          g = null;
        }
        image.flush();
        image = null;
      }
    }
  }


//-------------------------- INNER CLASSES --------------------------

  /** Provides information for a single frame entry */
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


  /** Provides access to cycle-specific functionality. */
  public static class BamV1Control extends BamControl
  {
    private int[] currentPalette, externalPalette;
    private boolean transparencyEnabled;
    private TransparencyMode transparencyMode;
    private int currentCycle, currentFrame;

    protected BamV1Control(BamV1Decoder decoder)
    {
      super(decoder);
      init();
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
        preparePalette(externalPalette);
      }
    }

    /**
     * Returns the currently used transparency mode for palettes.
     */
    public TransparencyMode getTransparencyMode()
    {
      return transparencyMode;
    }

    /**
     * Sets the mode on how to handle transparency in palettes.
     * @param transparencyMode The transparency mode to set.
     */
    public void setTransparencyMode(TransparencyMode transparencyMode)
    {
      if (transparencyMode != null) {
        if (this.transparencyMode != transparencyMode) {
          this.transparencyMode = transparencyMode;
          preparePalette(externalPalette);
        }
      }
    }

    /** Returns the transparency index of the current palette. */
    public int getTransparencyIndex()
    {
      for (int i = 0; i < currentPalette.length; i++) {
        if ((currentPalette[i] & 0xff000000) == 0) {
          return i;
        }
      }
      return 0;
    }

    /** Returns whether the palette makes use of alpha transparency. */
    public boolean isAlphaEnabled()
    {
      if (Profile.isEnhancedEdition()) {
        for (int i = 0; i < currentPalette.length; i++) {
          int mask = currentPalette[i] & 0xff000000;
          if (mask != 0 && mask != 0xff000000) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Returns the currently assigned external palette.
     * @return The currently assigned external palette, or {@code null} if not available.
     */
    public int[] getExternalPalette()
    {
      return externalPalette;
    }

    /**
     * Applies the colors of the specified palette to the active BAM palette.
     * <b>Note:</b> Must be called whenever any changes to the external palette have been done.
     * @param palette An external palette. Specify {@code null} to use the default palette.
     */
    public void setExternalPalette(int[] palette)
    {
      if (palette != null) {
        externalPalette = new int[palette.length];
        for (int i = 0; i < palette.length; i++) {
          externalPalette[i] = 0xff000000 | palette[i];
        }
      }
      preparePalette(externalPalette);
    }

    /** Returns the original and unmodified palette as defined in the BAM resource. */
    public int[] getPalette()
    {
      return getDecoder().bamPalette;
    }

    /**
     * Returns the currently used palette. This is either an external palette, the default palette,
     * or a combination of both.
     */
    public int[] getCurrentPalette()
    {
      return currentPalette;
    }

    @Override
    public BamV1Decoder getDecoder()
    {
      return (BamV1Decoder)super.getDecoder();
    }

    @Override
    public int cycleCount()
    {
      return getDecoder().listCycles.size();
    }

    @Override
    public int cycleFrameCount()
    {
      if (currentCycle < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(currentCycle).frames.length;
      } else {
        return 0;
      }
    }

    @Override
    public int cycleFrameCount(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(cycleIdx).frames.length;
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
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size() && currentCycle != cycleIdx) {
        currentCycle = cycleIdx;
        if (isSharedPerCycle()) {
          updateSharedBamSize();
        }
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean cycleHasNextFrame()
    {
      if (currentCycle < getDecoder().listCycles.size()) {
        return (currentFrame < getDecoder().listCycles.get(currentCycle).frames.length - 1);
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
      return getDecoder().frameGet(this, frameIdx);
    }

    @Override
    public void cycleGetFrame(Image canvas)
    {
      int frameIdx = cycleGetFrameIndexAbsolute();
      getDecoder().frameGet(this, frameIdx, canvas);
    }

    @Override
    public Image cycleGetFrame(int frameIdx)
    {
      frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
      return getDecoder().frameGet(this, frameIdx);
    }

    @Override
    public void cycleGetFrame(int frameIdx, Image canvas)
    {
      frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
      getDecoder().frameGet(this, frameIdx, canvas);
    }

    @Override
    public int cycleGetFrameIndex()
    {
      return currentFrame;
    }

    @Override
    public boolean cycleSetFrameIndex(int frameIdx)
    {
      if (currentCycle < getDecoder().listCycles.size() &&
          frameIdx >= 0 && frameIdx < getDecoder().listCycles.get(currentCycle).frames.length) {
        currentFrame = frameIdx;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public int cycleGetFrameIndexAbsolute()
    {
      return cycleGetFrameIndexAbsolute(currentCycle, currentFrame);
    }

    @Override
    public int cycleGetFrameIndexAbsolute(int frameIdx)
    {
      return cycleGetFrameIndexAbsolute(currentCycle, frameIdx);
    }

    @Override
    public int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size() &&
          frameIdx >= 0 && frameIdx < getDecoder().listCycles.get(cycleIdx).frames.length) {
        return getDecoder().listCycles.get(cycleIdx).frames[frameIdx];
      } else {
        return -1;
      }
    }


    private void init()
    {
      this.transparencyEnabled = true;
      this.transparencyMode = TransparencyMode.NORMAL;
      currentPalette = null;
      externalPalette = null;
      currentCycle = currentFrame = 0;
      // preparing the default palette
      preparePalette(null);
      updateSharedBamSize();
    }


    // Prepares the palette to be used for decoding BAM frames
    private void preparePalette(int[] externalPalette)
    {
      if (currentPalette == null) {
        currentPalette = new int[256];
      }

      // some optimizations: don't prepare if the palette hasn't change
      boolean isNormalMode = (getTransparencyMode() == TransparencyMode.NORMAL);
      int idx = 0;
      int transIndex = -1;
      int alphaMask = Profile.isEnhancedEdition() ? 0 : 0xff000000;
      boolean alphaUsed = false;  // determines whether alpha is actually used
      if (externalPalette != null) {
        // filling palette entries from external palette, as much as possible
        for (; idx < externalPalette.length && idx < 256; idx++) {
          currentPalette[idx] = alphaMask | externalPalette[idx];
          alphaUsed |= (currentPalette[idx] & 0xff000000) != 0;
          if (isNormalMode && transIndex < 0 && (currentPalette[idx] & 0x00ffffff) == 0x0000ff00) {
            transIndex = idx;
          }
        }
      }
      // filling remaining entries with BAM palette
      if (getDecoder().bamPalette != null) {
        for (; idx < getDecoder().bamPalette.length; idx++) {
          currentPalette[idx] = alphaMask | getDecoder().bamPalette[idx];
          alphaUsed |= (currentPalette[idx] & 0xff000000) != 0;
          if (isNormalMode && transIndex < 0 && (currentPalette[idx] & 0x00ffffff) == 0x0000ff00) {
            transIndex = idx;
          }
        }
      }

      // removing alpha support if needed
      if (!alphaUsed) {
        for (int i = 0; i < currentPalette.length; i++) {
          currentPalette[i] |= 0xff000000;
        }
      }

      // applying transparent index
      if (isNormalMode && transIndex >= 0) {
        if (transparencyEnabled) {
          currentPalette[transIndex] = 0;
        } else {
          currentPalette[transIndex] |= 0xff000000;
        }
      }

      // falling back to transparency at color index 0
      if (transparencyEnabled && transIndex < 0) {
        currentPalette[0] = 0;
      }
    }
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
          frames[i] = DynamicArray.getShort(buffer, ofsLookup + 2*(lookupIndex+i));
        }
      } else {
        frames = new int[0];
        indexCount = lookupIndex = 0;
      }
    }
  }
}
