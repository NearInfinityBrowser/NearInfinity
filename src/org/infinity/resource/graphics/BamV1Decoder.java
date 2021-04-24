// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.StreamUtils;


/**
 * Handles BAM v1 resources (both BAMC and uncompressed BAM V1).
 */
public class BamV1Decoder extends BamDecoder
{
  private final List<BamV1FrameEntry> listFrames = new ArrayList<>();
  private final List<CycleEntry> listCycles = new ArrayList<>();
  private final BamV1FrameEntry defaultFrameInfo = new BamV1FrameEntry(null, 0);

  private BamV1Control defaultControl;
  private ByteBuffer bamBuffer;   // contains the raw (uncompressed) data of the BAM resource
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
    bamBuffer = null;
    bamPalette = null;
    listFrames.clear();
    listCycles.clear();
    rleIndex = 0;
  }

  @Override
  public boolean isOpen()
  {
    return (bamBuffer != null);
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public ByteBuffer getResourceBuffer()
  {
    return bamBuffer;
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
        bamBuffer = getResourceEntry().getResourceBuffer();
        String signature = StreamUtils.readString(bamBuffer, 0, 4);
        String version = StreamUtils.readString(bamBuffer, 4, 4);
        if ("BAMC".equals(signature)) {
          setType(Type.BAMC);
          bamBuffer = Compressor.decompress(bamBuffer);
          signature = StreamUtils.readString(bamBuffer, 00, 4);
          version = StreamUtils.readString(bamBuffer, 4, 4);
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
        int framesCount = bamBuffer.getShort(8) & 0xffff;
        if (framesCount <= 0) {
          throw new Exception("Invalid number of frames");
        }
        int cyclesCount = bamBuffer.get(0x0a) & 0xff;
        if (cyclesCount <= 0) {
          throw new Exception("Invalid number of cycles");
        }
        rleIndex = bamBuffer.get(0x0b) & 0xff;
        int ofsFrames = bamBuffer.getInt(0x0c);
        if (ofsFrames < 0x18) {
          throw new Exception("Invalid frames offset");
        }
        int ofsPalette = bamBuffer.getInt(0x10);
        if (ofsPalette < 0x18) {
          throw new Exception("Invalid palette offset");
        }
        int ofsLookup = bamBuffer.getInt(0x14);
        if (ofsLookup < 0x18) {
          throw new Exception("Invalid frame lookup table offset");
        }

        int ofs = ofsFrames;
        // initializing frames
        for (int i = 0; i < framesCount; i++) {
          listFrames.add(new BamV1FrameEntry(bamBuffer, ofs));
          ofs += 0x0c;
        }

        // initializing cycles
        for (int i = 0; i < cyclesCount; i++) {
          int cnt = bamBuffer.getShort(ofs) & 0xffff;
          int idx = bamBuffer.getShort(ofs+2) & 0xffff;
          listCycles.add(new CycleEntry(bamBuffer, ofsLookup, cnt, idx));
          ofs += 0x04;
        }

        // initializing palette (number of palette entries can be less than 256)
        int[] offsets = {ofsFrames, ofsPalette, ofsLookup, bamBuffer.limit()};
        Arrays.sort(offsets);
        int idx = Arrays.binarySearch(offsets, ofsPalette);
        int numEntries = 256;
        if (idx >= 0 && idx + 1 < offsets.length) {
          numEntries = Math.min(256, (offsets[idx+1] - offsets[idx]) / 4);
        }
        bamPalette = new int[256];
        Arrays.fill(bamPalette, 0xff000000);
        for (int i = 0; i < numEntries; i++) {
          int col = bamBuffer.getInt(ofsPalette + 4*i);
          // handling alpha backwards compatibility with non-enhanced games
          if ((col & 0xff000000) == 0) {
            col |= 0xff000000;
          }
          bamPalette[i] = col;
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
      try {
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
              pixel = bamBuffer.get(srcOfs++);
              color = palette[pixel & 0xff];
              if (isCompressed && (pixel & 0xff) == rleIndex) {
                count = bamBuffer.get(srcOfs++) & 0xff;
              }
              if (x < maxWidth) {
                if (bufferB != null) bufferB[dstOfs] = pixel;
                if (bufferI != null) bufferI[dstOfs] = color;
              }
            }
          }
          dstOfs += dstWidth - srcWidth;
        }
      } catch (Exception e) {
        System.err.printf("Error [%s]: input (offset=%d, size=%d), output (offset=%d, size=%d)\n",
                          e.getClass().getName(), srcOfs, bamBuffer.limit(), dstOfs,
                          bufferB != null ? bufferB.length : bufferI.length);
      }
      bufferB = null;
      bufferI = null;

      // rendering resulting image onto the canvas if needed
      if (image != canvas) {
        Graphics2D g = (Graphics2D)canvas.getGraphics();
        try {
          if (getComposite() != null) {
            g.setComposite(getComposite());
          }
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

  @Override
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = 31 * hash + ((listFrames == null) ? 0 : listFrames.hashCode());
    hash = 31 * hash + ((listCycles == null) ? 0 : listCycles.hashCode());
    hash = 31 * hash + ((bamBuffer == null) ? 0 : bamBuffer.hashCode());
    hash = 31 * hash + ((bamPalette == null) ? 0 : bamPalette.hashCode());
    hash = 31 * hash + rleIndex;
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof BamV1Decoder)) {
      return false;
    }
    boolean retVal = super.equals(o);
    if (retVal) {
      BamV1Decoder other = (BamV1Decoder)o;
      retVal &= (this.listFrames == null && other.listFrames == null) ||
                (this.listFrames != null && this.listFrames.equals(other.listFrames));
      retVal &= (this.listCycles == null && other.listCycles == null) ||
                (this.listCycles != null && this.listCycles.equals(other.listCycles));
      retVal &= (this.bamBuffer == null && other.bamBuffer == null) ||
                (this.bamBuffer != null && this.bamBuffer.equals(other.bamBuffer));
      retVal &= (this.bamPalette == null && other.bamPalette == null) ||
                (this.bamPalette != null && this.bamPalette.equals(other.bamPalette));
      retVal &= (this.rleIndex == other.rleIndex);
    }
    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  /** Provides information for a single frame entry */
  public class BamV1FrameEntry implements BamDecoder.FrameEntry
  {
    private int width, height, centerX, centerY, ofsData;
    private int overrideCenterX, overrideCenterY;
    private boolean compressed;

    private BamV1FrameEntry(ByteBuffer buffer, int ofs)
    {
      if (buffer != null && ofs + 12 <= buffer.limit()) {
        width = buffer.getShort(ofs + 0) & 0xffff;
        height = buffer.getShort(ofs + 2) & 0xffff;
        centerX = overrideCenterX = buffer.getShort(ofs + 4);
        centerY = overrideCenterY = buffer.getShort(ofs + 6);
        ofsData = buffer.getInt(ofs + 8) & 0x7fffffff;
        compressed = (buffer.getInt(ofs + 8) & 0x80000000) == 0;
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
    public int getCenterX() { return overrideCenterX; }
    @Override
    public int getCenterY() { return overrideCenterY; }

    @Override
    public void setCenterX(int x) { overrideCenterX = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, x)); }
    @Override
    public void setCenterY(int y) { overrideCenterY = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, y)); }
    @Override
    public void resetCenter() { overrideCenterX = centerX; overrideCenterY = centerY; }

    public boolean isCompressed() { return compressed; }

    @Override
    public String toString()
    {
      return "[width=" + getWidth() + ", height=" + getHeight() +
             ", centerX=" + getCenterX() + ", centerY=" + getCenterY() +
             ", compressed=" + Boolean.toString(isCompressed()) + "]" ;
    }
  }


  /** Provides access to cycle-specific functionality. */
  public static class BamV1Control extends BamControl
  {
    private int[] currentPalette, externalPalette;
    private boolean transparencyEnabled;
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

    /** Returns the transparency index of the current palette. */
    public int getTransparencyIndex()
    {
      int idx = currentPalette.length - 1;
      for (; idx > 0; idx--)
        if ((currentPalette[idx] & 0xff000000) == 0)
          break;
      return idx;
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
          externalPalette[i] = palette[i];
          if ((externalPalette[i] & 0xff000000) == 0) {
            externalPalette[i] |= 0xff000000;
          }
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
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        if (currentCycle != cycleIdx) {
          currentCycle = cycleIdx;
          if (isSharedPerCycle()) {
            updateSharedBamSize();
          }
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
      transparencyEnabled = true;
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
      if (currentPalette == null)
        currentPalette = new int[256];

      // some optimizations: don't prepare if the palette hasn't changed
      int idx = 0;
      List<Integer> transIndices = new ArrayList<>(); // multiple transparent palette indices are supported
      int alphaMask = Profile.isEnhancedEdition() ? 0 : 0xff000000;
      boolean alphaUsed = false;  // determines whether alpha is actually used

      if (externalPalette != null) {
        // filling palette entries from external palette, as much as possible
        for (; idx < externalPalette.length && idx < 256; idx++) {
          currentPalette[idx] = externalPalette[idx];
          if ((currentPalette[idx] & 0xff000000) == 0) {
            currentPalette[idx] |= alphaMask;
          }
          alphaUsed |= (currentPalette[idx] & 0xff000000) != 0;
          if (idx == 0 || (currentPalette[idx] & 0x00ffffff) == 0x0000ff00)
            transIndices.add(idx);
        }
      }

      if (getDecoder().bamPalette != null) {
        // filling remaining entries with BAM palette
        for (; idx < getDecoder().bamPalette.length; idx++) {
          currentPalette[idx] = getDecoder().bamPalette[idx];
          if ((currentPalette[idx] & 0xff000000) == 0) {
            currentPalette[idx] |= alphaMask;
          }
          alphaUsed |= (currentPalette[idx] & 0xff000000) != 0;
          if (idx == 0 || (currentPalette[idx] & 0x00ffffff) == 0x0000ff00)
            transIndices.add(idx);
        }
      }

      if (!alphaUsed) {
        // discarding alpha
        for (int i = 0; i < currentPalette.length; i++) {
          currentPalette[i] |= 0xff000000;
        }
      }

      // applying transparent indices
      for (int i : transIndices) {
        if (transparencyEnabled)
          currentPalette[i] = 0;
        else
          currentPalette[i] |= 0xff000000;
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
    private CycleEntry(ByteBuffer buffer, int ofsLookup, int idxCount, int idxLookup)
    {
      if (buffer != null && idxCount >= 0 && idxLookup >= 0 &&
          ofsLookup + 2*(idxLookup+idxCount) <= buffer.limit()) {
        indexCount = idxCount;
        lookupIndex = idxLookup;
        frames = new int[indexCount];
        for (int i = 0; i < indexCount; i++) {
          frames[i] = buffer.getShort(ofsLookup + 2*(lookupIndex+i));
        }
      } else {
        frames = new int[0];
        indexCount = lookupIndex = 0;
      }
    }
  }
}
