// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;


/**
 * Handles BAM v2 resources.
 */
public class BamV2Decoder extends BamDecoder
{
  private final TreeSet<Integer> pvrIndices = new TreeSet<>();
  private final List<BamV2FrameEntry> listFrames = new ArrayList<>();
  private final List<CycleEntry> listCycles = new ArrayList<>();
  private final BamV2FrameEntry defaultFrameInfo = new BamV2FrameEntry(null, 0, 0);

  private BamV2Control defaultControl;
  private ByteBuffer bamBuffer;           // contains the raw (uncompressed) BAM v2 data
  private Path bamPath;                   // base path of the BAM resource (or null if BAM is biffed)
  private int numDataBlocks;              // number of PVRZ data blocks

  public BamV2Decoder(ResourceEntry bamEntry)
  {
    super(bamEntry);
    init();
  }

  @Override
  public BamV2Control createControl()
  {
    return new BamV2Control(this);
  }

  @Override
  public BamV2FrameEntry getFrameInfo(int frameIdx)
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
    PvrDecoder.flushCache();
    bamBuffer = null;
    listFrames.clear();
    listCycles.clear();
    pvrIndices.clear();
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
        renderFrame(control, frameIdx, canvas);
      }
    }
  }

  /** Returns the number of PVRZ data blocks referred to in this BAM. */
  public int getDataBlockCount()
  {
    return numDataBlocks;
  }

  /** Returns the set of referenced PVRZ pages by this BAM. */
  public Set<Integer> getReferencedPVRZPages()
  {
    return Collections.unmodifiableSet(pvrIndices);
  }

  private void init()
  {
    // resetting data
    close();

    ResourceEntry entry = getResourceEntry();
    if (entry != null) {
      try {
        Path bamFile = entry.getActualPath();
        if (bamFile != null) {
          bamPath = bamFile.getParent();
          // Skip path if it denotes an override folder of the game
          List<Path> list = Profile.getOverrideFolders(true);
          if (list != null) {
            for (final Path path: list) {
              if (bamPath.equals(path)) {
                bamPath = null;
                break;
              }
            }
          }
        }
        bamBuffer = entry.getResourceBuffer();
        String signature = StreamUtils.readString(bamBuffer, 0, 4);
        String version = StreamUtils.readString(bamBuffer, 4, 4);
        if (!"BAM ".equals(signature) || !"V2  ".equals(version)) {
          throw new Exception("Invalid BAM type");
        }
        setType(Type.BAMV2);

        // evaluating header data
        int framesCount = bamBuffer.getInt(8);
        if (framesCount <= 0) {
          throw new Exception("Invalid number of frames");
        }
        int cyclesCount = bamBuffer.getInt(0x0c);
        if (cyclesCount <= 0) {
          throw new Exception("Invalid number of cycles");
        }
        numDataBlocks = bamBuffer.getInt(0x10);
        if (numDataBlocks <= 0) {
          throw new Exception("Invalid number of data blocks");
        }
        int ofsFrames = bamBuffer.getInt(0x14);
        if (ofsFrames < 0x20) {
          throw new Exception("Invalid frames offset");
        }
        int ofsCycles = bamBuffer.getInt(0x18);
        if (ofsCycles < 0x20) {
          throw new Exception("Invalid cycles offset");
        }
        int ofsBlocks = bamBuffer.getInt(0x1c);
        if (ofsBlocks < 0x20) {
          throw new Exception("Invalid data blocks offset");
        }

        int ofs = ofsFrames;
        // processing frame entries
        for (int i = 0; i < framesCount; i++) {
          listFrames.add(new BamV2FrameEntry(bamBuffer, ofs, ofsBlocks));
          ofs += 0x0c;
        }

        // processing cycle entries
        ofs = ofsCycles;
        for (int i = 0; i < cyclesCount; i++) {
          int cnt = bamBuffer.getShort(ofs) & 0xffff;
          int idx = bamBuffer.getShort(ofs+2) & 0xffff;
          listCycles.add(new CycleEntry(idx, cnt));
          ofs += 4;
        }

        // creating default bam control instance as a fallback option
        defaultControl = new BamV2Control(this);
        defaultControl.setMode(BamControl.Mode.SHARED);
        defaultControl.setSharedPerCycle(false);
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
      pvrIndices.add(Integer.valueOf(page));
      String name = String.format("MOS%04d.PVRZ", page);
      ResourceEntry entry = null;
      if (bamPath != null) {
        // preferring PVRZ files from the BAM's base path
        Path pvrzFile = FileManager.resolve(bamPath.resolve(name));
        if (FileEx.create(pvrzFile).isFile()) {
          entry = new FileResourceEntry(pvrzFile);
        }
      }
      if (entry == null) {
        // fallback: use PVRZ resources from game
        entry = ResourceFactory.getResourceEntry(name);
      }
      if (entry != null) {
        return PvrDecoder.loadPvr(entry);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // Draws the absolute frame onto the canvas. Takes BAM mode into account.
  private void renderFrame(BamControl control, int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (control == null) {
        control = defaultControl;
      }

      // decoding frame data
      BufferedImage image = ColorConvert.toBufferedImage(canvas, true, true);
      int dstWidth = image.getWidth();
      int dstHeight = image.getHeight();
      int[] dstBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      int[] srcBuffer = ((DataBufferInt)listFrames.get(frameIdx).frame.getRaster().getDataBuffer()).getData();
      if (control.getMode() == BamControl.Mode.SHARED) {
        // drawing on shared canvas
        int left = -control.getSharedRectangle().x - listFrames.get(frameIdx).centerX;
        int top = -control.getSharedRectangle().y - listFrames.get(frameIdx).centerY;
        int maxWidth = (dstWidth < srcWidth + left) ? dstWidth : srcWidth;
        int maxHeight = (dstHeight < srcHeight + top) ? dstHeight : srcHeight;
        int srcOfs = 0, dstOfs = top*dstWidth + left;
        for (int y = 0; y < maxHeight; y++) {
          System.arraycopy(srcBuffer, srcOfs, dstBuffer, dstOfs, maxWidth);
          srcOfs += srcWidth;
          dstOfs += dstWidth;
        }
      } else {
        // drawing on individual canvas
        int srcOfs = 0, dstOfs = 0;
        int maxWidth = (dstWidth < srcWidth) ? dstWidth : srcWidth;
        int maxHeight = (dstHeight < srcHeight) ? dstHeight : srcHeight;
        for (int y = 0; y < maxHeight; y++) {
          System.arraycopy(srcBuffer, srcOfs, dstBuffer, dstOfs, maxWidth);
          srcOfs += srcWidth;
          dstOfs += dstWidth;
        }
      }
      dstBuffer = null;

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
    hash = 31 * hash + ((pvrIndices == null) ? 0 : pvrIndices.hashCode());
    hash = 31 * hash + ((listFrames == null) ? 0 : listFrames.hashCode());
    hash = 31 * hash + ((listCycles == null) ? 0 : listCycles.hashCode());
    hash = 31 * hash + ((bamBuffer == null) ? 0 : bamBuffer.hashCode());
    hash = 31 * hash + ((bamPath == null) ? 0 : bamPath.hashCode());
    hash = 31 * hash + numDataBlocks;
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof BamV2Decoder)) {
      return false;
    }
    boolean retVal = super.equals(o);
    if (retVal) {
      BamV2Decoder other = (BamV2Decoder)o;
      retVal &= (this.pvrIndices == null && other.pvrIndices == null) ||
                (this.pvrIndices != null && this.pvrIndices.equals(other.pvrIndices));
      retVal &= (this.listFrames == null && other.listFrames == null) ||
                (this.listFrames != null && this.listFrames.equals(other.listFrames));
      retVal &= (this.listCycles == null && other.listCycles == null) ||
                (this.listCycles != null && this.listCycles.equals(other.listCycles));
      retVal &= (this.bamBuffer == null && other.bamBuffer == null) ||
                (this.bamBuffer != null && this.bamBuffer.equals(other.bamBuffer));
      retVal &= (this.bamPath == null && other.bamPath == null) ||
                (this.bamPath != null && this.bamPath.equals(other.bamPath));
      retVal &= (this.numDataBlocks == other.numDataBlocks);
    }
    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  // Stores information for a single frame entry
  public class BamV2FrameEntry implements BamDecoder.FrameEntry
  {
    private final int dataBlockSize = 0x1c;   // size of a single data block

    private int width, height, centerX, centerY;
    private int overrideCenterX, overrideCenterY;
    private BufferedImage frame;

    private BamV2FrameEntry(ByteBuffer buffer, int ofsFrame, int ofsBlocks)
    {
      if (buffer != null && ofsFrame < buffer.limit() && ofsBlocks < buffer.limit()) {
        width = buffer.getShort(ofsFrame) & 0xffff;
        height = buffer.getShort(ofsFrame+2) & 0xffff;
        centerX = overrideCenterX = buffer.getShort(ofsFrame+4);
        centerY = overrideCenterY = buffer.getShort(ofsFrame+6);
        int blockStart = buffer.getShort(ofsFrame+8) & 0xffff;
        int blockCount = buffer.getShort(ofsFrame+10) & 0xffff;
        decodeImage(buffer, ofsBlocks, blockStart, blockCount);
      } else {
        width = height = centerX = centerY = 0;
        frame = null;
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

    public Image getImage() { return frame; }

    @Override
    public String toString()
    {
      return "[width=" + getWidth() + ", height=" + getHeight() +
             ", centerX=" + getCenterX() + ", centerY=" + getCenterY() + "]" ;
    }

    private void decodeImage(ByteBuffer buffer, int ofsBlocks, int start, int count)
    {
      frame = null;
      if (width > 0 && height > 0) {
        frame = ColorConvert.createCompatibleImage(width, height, Transparency.TRANSLUCENT);

        int ofs = ofsBlocks + start*dataBlockSize;
        for (int i = 0; i < count; i++) {
          int page = buffer.getInt(ofs);
          int srcX = buffer.getInt(ofs+0x04);
          int srcY = buffer.getInt(ofs+0x08);
          int w = buffer.getInt(ofs+0x0c);
          int h = buffer.getInt(ofs+0x10);
          int dstX = buffer.getInt(ofs+0x14);
          int dstY = buffer.getInt(ofs+0x18);
          ofs += dataBlockSize;

          PvrDecoder decoder = getPVR(page);
          if (decoder != null) {
            try {
              BufferedImage srcImage = decoder.decode(srcX, srcY, w, h);
              Graphics2D g = (Graphics2D)frame.getGraphics();
              if (getComposite() != null) {
                g.setComposite(getComposite());
              }
              g.drawImage(srcImage, dstX, dstY, null);
              g.dispose();
              g = null;
              decoder = null;
              srcImage = null;
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }


  /** Provides access to cycle-specific functionality. */
  public static class BamV2Control extends BamControl
  {
    private int currentCycle, currentFrame;

    protected BamV2Control(BamV2Decoder decoder)
    {
      super(decoder);
      init();
    }

    @Override
    public BamV2Decoder getDecoder()
    {
      return (BamV2Decoder)super.getDecoder();
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
        return getDecoder().listCycles.get(currentCycle).framesCount;
      } else {
        return 0;
      }
    }

    @Override
    public int cycleFrameCount(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(cycleIdx).framesCount;
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
        return (currentFrame < getDecoder().listCycles.get(currentCycle).framesCount - 1);
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
          frameIdx >= 0 && frameIdx < getDecoder().listCycles.get(currentCycle).framesCount) {
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
          frameIdx >= 0 && frameIdx < getDecoder().listCycles.get(cycleIdx).framesCount) {
        return getDecoder().listCycles.get(cycleIdx).startIndex + frameIdx;
      } else {
        return -1;
      }
    }


    private void init()
    {
      currentCycle = currentFrame = 0;
      updateSharedBamSize();
    }
  }


  // Stores information for a single cycle
  private class CycleEntry
  {
    public int startIndex, framesCount;

    public CycleEntry(int startIndex, int framesCount)
    {
      if (startIndex >= 0 && framesCount > 0) {
        this.startIndex = startIndex;
        this.framesCount = framesCount;
      } else {
        this.startIndex = this.framesCount = 0;
      }
    }
  }
}
