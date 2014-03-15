// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;


/**
 * Handles BAM v2 resources.
 * @author argent77
 */
public class BamV2Decoder extends BamDecoder
{
  private final ConcurrentHashMap<Integer, PvrDecoder> pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>();
  private final List<BamV2FrameEntry> listFrames = new ArrayList<BamV2FrameEntry>();
  private final List<CycleEntry> listCycles = new ArrayList<CycleEntry>();

  private byte[] bamData;               // contains the raw (uncompressed) BAM v2 data
  private int currentCycle, currentFrame;
  private BufferedImage sharedCanvas;   // temporary buffer for drawing on a shared canvas

  public BamV2Decoder(ResourceEntry bamEntry)
  {
    super(bamEntry);
    init();
  }

  @Override
  public BamV2FrameEntry getFrameInfo(int frameIdx)
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
    listFrames.clear();
    listCycles.clear();
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
        renderFrame(frameIdx, image);
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
        renderFrame(frameIdx, canvas);
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
        renderFrame(frameIdx, buffer, w, h);
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
      return listCycles.get(currentCycle).framesCount;
    } else {
      return 0;
    }
  }

  @Override
  public int cycleFrameCount(int cycleIdx)
  {
    if (cycleIdx >= 0 && cycleIdx < listCycles.size()) {
      return listCycles.get(cycleIdx).framesCount;
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
    if (cycleIdx >= 0 && cycleIdx < listCycles.size() && currentCycle != cycleIdx) {
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
    if (currentCycle < listCycles.size()) {
      return (currentFrame < listCycles.get(currentCycle).framesCount - 1);
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
        frameIdx >= 0 && frameIdx < listCycles.get(currentCycle).framesCount) {
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
        currentFrame < listCycles.get(currentCycle).framesCount) {
      return listCycles.get(currentCycle).startIndex + currentFrame;
    } else {
      return -1;
    }
  }

  @Override
  public int cycleGetFrameIndexAbsolute(int frameIdx)
  {
    if (currentCycle < listCycles.size() &&
        frameIdx >= 0 && frameIdx < listCycles.get(currentCycle).framesCount) {
      return listCycles.get(currentCycle).startIndex + frameIdx;
    } else {
      return -1;
    }
  }

  @Override
  public int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx)
  {
    if (cycleIdx >= 0 && cycleIdx < listCycles.size() &&
        frameIdx >= 0 && frameIdx < listCycles.get(cycleIdx).framesCount) {
      return listCycles.get(cycleIdx).startIndex + frameIdx;
    } else {
      return -1;
    }
  }


  private void init()
  {
    // resetting data
    close();

    if (getResourceEntry() != null) {
      try {
        bamData = getResourceEntry().getResourceData();
        String signature = DynamicArray.getString(bamData, 0x00, 4);
        String version = DynamicArray.getString(bamData, 0x04, 4);
        if (!"BAM ".equals(signature) || !"V2  ".equals(version)) {
          throw new Exception("Invalid BAM type");
        }
        setType(Type.BAMV2);

        // evaluating header data
        int framesCount = DynamicArray.getInt(bamData, 0x08);
        if (framesCount <= 0) {
          throw new Exception("Invalid number of frames");
        }
        int cyclesCount = DynamicArray.getInt(bamData, 0x0c);
        if (cyclesCount <= 0) {
          throw new Exception("Invalid number of cycles");
        }
        int blockCount = DynamicArray.getInt(bamData, 0x10);
        if (blockCount <= 0) {
          throw new Exception("Invalid number of data blocks");
        }
        int ofsFrames = DynamicArray.getInt(bamData, 0x14);
        if (ofsFrames < 0x20) {
          throw new Exception("Invalid frames offset");
        }
        int ofsCycles = DynamicArray.getInt(bamData, 0x18);
        if (ofsCycles < 0x20) {
          throw new Exception("Invalid cycles offset");
        }
        int ofsBlocks = DynamicArray.getInt(bamData, 0x1c);
        if (ofsBlocks < 0x20) {
          throw new Exception("Invalid data blocks offset");
        }

        int ofs = ofsFrames;
        // processing frame entries
        for (int i = 0; i < framesCount; i++) {
          listFrames.add(new BamV2FrameEntry(bamData, ofs, ofsBlocks));
          ofs += 0x0c;
        }

        // processing cycle entries
        ofs = ofsCycles;
        for (int i = 0; i < cyclesCount; i++) {
          int cnt = DynamicArray.getUnsignedShort(bamData, ofs);
          int idx = DynamicArray.getUnsignedShort(bamData, ofs+2);
          listCycles.add(new CycleEntry(idx, cnt));
          ofs += 4;
        }

        pvrTable.clear();

        updateSharedBamSize();
        sharedCanvas = new BufferedImage(getSharedRectangle().width, getSharedRectangle().height,
                                         BufferedImage.TYPE_INT_ARGB);
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Returns and caches the PVRZ resource of the specified page
  private PvrDecoder getPVR(int page)
  {
    synchronized (pvrTable) {
      Integer key = Integer.valueOf(page);
      if (pvrTable.containsKey(key)) {
        return pvrTable.get(key);
      }

      try {
        String name = String.format("MOS%1$04d.PVRZ", key);
        ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(name);
        if (entry != null) {
          byte[] data = entry.getResourceData();
          if (data != null) {
            int size = DynamicArray.getInt(data, 0);
            int marker = DynamicArray.getUnsignedShort(data, 4);
            if ((size & 0xff) == 0x34 && marker == 0x9c78) {
              data = Compressor.decompress(data, 0);
              PvrDecoder decoder = new PvrDecoder(data);
              data = null;
              pvrTable.put(key, decoder);
              return decoder;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  // Draws the absolute frame onto the canvas. Takes BAM mode into account.
  private void renderFrame(int frameIdx, Image canvas)
  {
    BufferedImage image = null;
    if (canvas instanceof BufferedImage) {
      image = (BufferedImage)canvas;
    } else {
      image = sharedCanvas;
    }
    int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    renderFrame(frameIdx, buffer, image.getWidth(), image.getHeight());
    buffer = null;
    if (image != canvas) {
      Graphics g = canvas.getGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      image.flush();
      image = null;
    }
  }

  // Draws the absolute frame into the buffer. Takes BAM mode into account.
  private void renderFrame(int frameIdx, int[] buffer, int width, int height)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size() &&
        buffer != null && buffer.length >= width*height) {
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      int[] srcData = ((DataBufferInt)listFrames.get(frameIdx).frame.getRaster().getDataBuffer()).getData();

      if (getMode() == Mode.Shared) {
        // drawing on shared canvas
        int left = -getSharedRectangle().x - listFrames.get(frameIdx).centerX;
        int top = -getSharedRectangle().y - listFrames.get(frameIdx).centerY;
        int maxWidth = (width < srcWidth + left) ? width : srcWidth;
        int maxHeight = (height < srcHeight + top) ? height : srcHeight;
        int srcOfs = 0, dstOfs = top*width + left;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            buffer[dstOfs+x] = srcData[srcOfs+x];
          }
          srcOfs += srcWidth;
          dstOfs += width;
        }
      } else {
        // drawing on individual canvas
        int srcOfs = 0, dstOfs = 0;
        int maxWidth = (width < srcWidth) ? width : srcWidth;
        int maxHeight = (height < srcHeight) ? height : srcHeight;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            buffer[dstOfs+x] = srcData[srcOfs+x];
          }
          srcOfs += srcWidth;
          dstOfs += width;
        }
      }
    }
  }


//-------------------------- INNER CLASSES --------------------------

  // Stores information for a single frame entry
  public class BamV2FrameEntry implements BamDecoder.FrameEntry
  {
    private final int dataBlockSize = 0x1c;   // size of a single data block

    private int width, height, centerX, centerY;
    private BufferedImage frame;

    private BamV2FrameEntry(byte[] buffer, int ofsFrame, int ofsBlocks)
    {
      if (buffer != null && ofsFrame < buffer.length && ofsBlocks < buffer.length) {
        width = DynamicArray.getUnsignedShort(buffer, ofsFrame);
        height = DynamicArray.getUnsignedShort(buffer, ofsFrame+2);
        centerX = DynamicArray.getShort(buffer, ofsFrame+4);
        centerY = DynamicArray.getShort(buffer, ofsFrame+6);
        int blockStart = DynamicArray.getUnsignedShort(buffer, ofsFrame+8);
        int blockCount = DynamicArray.getUnsignedShort(buffer, ofsFrame+10);
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
    public int getCenterX() { return centerX; }
    @Override
    public int getCenterY() { return centerY; }

    public Image getImage() { return frame; }

    private void decodeImage(byte[] buffer, int ofsBlocks, int start, int count)
    {
      frame = null;
      if (width > 0 && height > 0) {
        frame = ColorConvert.createCompatibleImage(width, height, Transparency.TRANSLUCENT);

        int ofs = ofsBlocks + start*dataBlockSize;
        for (int i = 0; i < count; i++) {
          int page = DynamicArray.getInt(buffer, ofs);
          int srcX = DynamicArray.getInt(buffer, ofs+0x04);
          int srcY = DynamicArray.getInt(buffer, ofs+0x08);
          int w = DynamicArray.getInt(buffer, ofs+0x0c);
          int h = DynamicArray.getInt(buffer, ofs+0x10);
          int dstX = DynamicArray.getInt(buffer, ofs+0x14);
          int dstY = DynamicArray.getInt(buffer, ofs+0x18);
          ofs += dataBlockSize;

          PvrDecoder decoder = getPVR(page);
          if (decoder != null) {
            try {
              BufferedImage srcImage = decoder.decode(srcX, srcY, w, h);
              Graphics g = frame.getGraphics();
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
