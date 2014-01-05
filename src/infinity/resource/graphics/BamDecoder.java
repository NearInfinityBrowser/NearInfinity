// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;
import infinity.util.Filereader;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decodes a BAM resource (Supported formats: BAMC V1, BAM V1, BAM V2)
 * @author argent77
 */
public class BamDecoder
{
  /** Describes the type of the BAM resource. */
  public enum BamType { INVALID, BAMC, BAMV1, BAMV2 }

  private BamData data;

  /**
   * Checks whether the specified resource is a valid and supported BAM resource.
   */
  public static boolean isValid(ResourceEntry entry)
  {
    return getType(entry) != BamType.INVALID;
  }

  /**
   * Returns the BAM type of the specified resource.
   */
  public static BamType getType(ResourceEntry entry)
  {
    if (entry != null) {
      try {
        InputStream is = entry.getResourceDataAsStream();
        if (is != null) {
          String sig = Filereader.readString(is, 4);
          String ver = Filereader.readString(is, 4);
          is.close();
          if ("BAMC".equals(sig)) {
            return BamType.BAMC;
          } else if ("BAM ".equals(sig)) {
            if ("V1  ".equals(ver)) {
              return BamType.BAMV1;
            } else if ("V2  ".equals(ver)) {
              return BamType.BAMV2;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return BamType.INVALID;
  }

  /**
   * Initializes a BAM resource. Transparency information will be used.
   * @param entry The BAM resource
   */
  public BamDecoder(ResourceEntry entry)
  {
    this(entry, false);
  }

  /**
   * Initializes a BAM resource.
   * @param entry The BAM resource
   * @param ignoreTransparency If <code>true</code>, transparency information is ignored
   *                           (affects BAM V1 resources only).
   */
  public BamDecoder(ResourceEntry entry, boolean ignoreTransparency)
  {
    if (entry == null)
      throw new NullPointerException();

    switch (getType(entry)) {
      case BAMC:
      case BAMV1:
        data = new BamDataV1(entry, ignoreTransparency);
        break;
      case BAMV2:
        data = new BamDataV2(entry, ignoreTransparency);
        break;
      default:
        data = null;
        throw new IllegalArgumentException("Invalid BAM resource");
    }
  }

  /**
   * Access to the BAM data interface.
   * @return The BAM data interface.
   */
  public BamData data()
  {
    return data;
  }

//-------------------------- INNER CLASSES --------------------------

  // Public interface to access BAM data
  public static interface BamData
  {
    /** Returns the type of the BAM resource */
    public BamType type();

    /** Returns the number of available cycles. */
    public int cycleCount();
    /** Sets the active cycle. The first available cycle will be automatically pre-selected. */
    public boolean cycleSet(int idx);
    /** Returns the active cycle. Defaults to the first available cycle. */
    public int cycleGet();
    /** Returns the number of available frames in the active cycle */
    public int cycleFrameCount();
    /** Returns whether the active cycle can be advanced by at least one more frame. */
    public boolean cycleHasNextFrame();
    /** Sets internal pointer to the next frame in the active cycle if available. */
    public boolean cycleNextFrame();
    /** Sets internal pointer to the first frame in the active cycle. */
    public void cycleReset();
    /** Returns the current frame in the active cycle. */
    public Image cycleGetFrame();
    /** Returns the current frame index in the active cycle. */
    public int cycleGetFrameIndex();
    /** Sets internal pointer to the specified frame index relative to the cycle's frame sequence. */
    public boolean cycleSetFrameIndex(int frameIdx);
    /** Returns the absolute frame index of the current frame in the active cycle. */
    public int cycleGetFrameIndexAbs();
    /** Returns the absolute frame index of the specified frame index relative to the cycle's frame sequence */
    public int cycleGetFrameIndexAbs(int frameIdx);

    /** Returns the total number of available frames. */
    public int frameCount();
    /** Returns the frame at the specified index. */
    public Image frameGet(int frameIdx);
    /** Returns the width of the specified frame in pixels. */
    public int frameWidth(int frameIdx);
    /** Returns the height of the specified frame in pixels. */
    public int frameHeight(int frameIdx);
    /** Returns the center x coordinate of the specified frame in pixels. */
    public int frameCenterX(int frameIdx);
    /** Returns the center y coordinate of the specified frame in pixels. */
    public int frameCenterY(int frameIdx);
    /** Returns whether the specified frame is compressed (BAM V1 only) */
    public boolean frameCompressed(int frameIdx);
  }

  // Handles BAM V1 and BAMC V1 resources
  private static class BamDataV1 implements BamData
  {
    private BamType type = null;
    private ArrayList<BamFrame> frames = null;
    private ArrayList<ArrayList<Integer>> cycles = null;
    private int curCycle = 0, curFrame = 0;

    public BamDataV1(ResourceEntry entry, boolean ignoreTransparency)
    {
      init(entry, ignoreTransparency);
    }

    @Override
    public BamType type()
    {
      if (!empty()) {
        return type;
      } else
        return null;
    }

    @Override
    public int cycleCount()
    {
      if (!empty()) {
        return cycles.size();
      } else
        return 0;
    }

    @Override
    public boolean cycleSet(int idx)
    {
      if (!empty()) {
        if (idx >= 0 && idx < cycles.size()) {
          curCycle = idx;
          curFrame = 0;
          return true;
        }
      }
      return false;
    }

    @Override
    public int cycleGet()
    {
      if (!empty()) {
        return curCycle;
      } else
        return -1;
    }

    @Override
    public int cycleFrameCount()
    {
      if (!empty()) {
        return cycles.get(curCycle).size();
      } else
        return 0;
    }

    @Override
    public boolean cycleHasNextFrame()
    {
      if (!empty()) {
        return (curFrame+1) < cycleFrameCount();
      } else
        return false;
    }

    @Override
    public boolean cycleNextFrame()
    {
      if (!empty()) {
        if (cycleHasNextFrame()) {
          curFrame++;
          return true;
        }
      }
      return false;
    }

    @Override
    public void cycleReset()
    {
      if (!empty()) {
        curFrame = 0;
      }
    }

    @Override
    public Image cycleGetFrame()
    {
      if (!empty()) {
        int idx = cycleGetFrameIndexAbs();
        if (idx >= 0) {
          return frames.get(idx).image;
        }
      }
      return null;
    }

    @Override
    public int cycleGetFrameIndex()
    {
      if (!empty()) {
        return curFrame;
      } else
        return 0;
    }

    @Override
    public boolean cycleSetFrameIndex(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < cycles.get(curCycle).size()) {
          curFrame = frameIdx;
          return true;
        }
      }
      return false;
    }

    @Override
    public int cycleGetFrameIndexAbs()
    {
      return cycleGetFrameIndexAbs(curFrame);
    }

    @Override
    public int cycleGetFrameIndexAbs(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < cycles.get(curCycle).size()) {
          int idx = cycles.get(curCycle).get(frameIdx);
          if (idx >= 0 && idx < frames.size()) {
            return idx;
          }
        }
      }
      return -1;
    }

    @Override
    public int frameCount()
    {
      if (!empty()) {
        return frames.size();
      } else
        return 0;
    }

    @Override
    public Image frameGet(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).image;
        }
      }
      return null;
    }

    @Override
    public int frameWidth(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).width;
        }
      }
      return 0;
    }

    @Override
    public int frameHeight(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).height;
        }
      }
      return 0;
    }

    @Override
    public int frameCenterX(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).centerX;
        }
      }
      return 0;
    }

    @Override
    public int frameCenterY(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).centerY;
        }
      }
      return 0;
    }

    @Override
    public boolean frameCompressed(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).isCompressed;
        }
      }
      return false;
    }

    private boolean empty()
    {
      return (type == null) || (frames == null) || (cycles == null);
    }

    private void init(ResourceEntry entry, boolean ignoreTransparency)
    {
      if (entry == null)
        throw new NullPointerException();

      try {
        byte[] buffer = entry.getResourceData();
        String signature = DynamicArray.getString(buffer, 0, 4);
        String version = DynamicArray.getString(buffer, 4, 4);
        if (signature.equals("BAMC") && version.equals("V1  ")) {
          type = BamType.BAMC;
          buffer = Compressor.decompress(buffer);
          signature = DynamicArray.getString(buffer, 0, 4);
          version = DynamicArray.getString(buffer, 4, 4);
        } else if (signature.equals("BAM ") && version.equals("V1  ")) {
          type = BamType.BAMV1;
        } else {
          type = BamType.INVALID;
          throw new Exception("Invalid BAM resource");
        }
        // Data should now be in BAM V1 format
        if (!(signature.equals("BAM ") && version.equals("V1  "))) {
          throw new Exception("Invalid BAM resource");
        }

        DynamicArray src = DynamicArray.wrap(buffer, DynamicArray.ElementType.BYTE);
        int numFrames = src.getUnsignedShort(0x08);
        if (numFrames <= 0)
          throw new Exception("Invalid number of frames: " + numFrames);
        int numCycles = src.getUnsignedByte(0x0a);
        if (numCycles <= 0)
          throw new Exception("Invalid number of cycles: " + numCycles);
        int compColor = src.getUnsignedByte(0x0b);    // specifies the compressed color index in compressed frames
        int ofsFrameEntries = src.getInt(0x0c);
        if (ofsFrameEntries < 0x18)
          throw new Exception(String.format("Invalid frame entries offset: 0x%1$x", ofsFrameEntries));
        int ofsCycleEntries = ofsFrameEntries + numFrames*0x0c;
        int ofsPalette = src.getInt(0x10);
        if (ofsPalette < 0x18)
          throw new Exception(String.format("Invalid palette offset: 0x%1$x", ofsPalette));
        int ofsLookupTable = src.getInt(0x14);
        if (ofsLookupTable < 0x18)
          throw new Exception(String.format("Invalid frame lookup table offset: 0x%1$x", ofsLookupTable));

        // processing palette
        int[] palette = new int[256];
        int transColor = -1;   // specifies the transparent color index
        src.setBaseOffset(ofsPalette);
        for (int i = 0; i < 256; i++) {
          int col = src.getInt(0); src.addToBaseOffset(4);
          col |= 0xff000000;
          if (!ignoreTransparency && transColor == -1) {
            // determining the transparent color index is very complicated
            int r = (col >> 16) & 0xff, g = (col >> 8) & 0xff, b = col & 0xff;
            if ((i > 0 && r <= 0x04 && g >= 0xfc && b <= 0x04) ||
                ((i == 0) &&
                 ((r <= 0x10 && g >= 0xfc && b <= 0x10) || (r >= 0xfc && g <= 0x10 && b >= 0xfc) ||
                 ((ResourceFactory.getGameID() == ResourceFactory.ID_BGEE ||
                   ResourceFactory.getGameID() == ResourceFactory.ID_BG2EE) &&
                  (r == 0x00 && g == 0x97 && b == 0x97))))) {
              transColor = i;
            }
          }
          palette[i] = col;
        }
        if (!ignoreTransparency && transColor >= 0)
          palette[transColor] &= 0x00ffffff;

        // processing frame entries
        frames = new ArrayList<BamDecoder.BamFrame>(numFrames);
        src.setBaseOffset(ofsFrameEntries);
        for (int i = 0; i < numFrames; i++) {
          int w = src.getUnsignedShort(0x00);
          int h = src.getUnsignedShort(0x02);
          int cx = src.getShort(0x04);
          int cy = src.getShort(0x06);
          int ofsData = src.getInt(0x08);
          boolean isCompressed = (ofsData & 0x80000000) == 0;
          ofsData &= 0x7fffffff;
          Image image = decodeImage(src.asByteArray(ofsData), w, h, palette, compColor, isCompressed);
          frames.add(new BamFrame(image, w, h, cx, cy, isCompressed));
          src.addToBaseOffset(0x0c);
        }

        // processing cycle entries (part 1: getting max. number of frame lookup entries)
        int numLookupEntries = 0;
        src.setBaseOffset(ofsCycleEntries);
        for (int i = 0; i < numCycles; i++) {
          int cnt = src.getUnsignedShort(0x00);
          int idx = src.getUnsignedShort(0x02);
          if (idx + cnt > numLookupEntries)
            numLookupEntries = idx + cnt;
          src.addToBaseOffset(0x04);
        }

        // processing frame lookup table
        ArrayList<Integer> frameIndices = new ArrayList<Integer>(numLookupEntries);
        src.setBaseOffset(ofsLookupTable);
        for (int i = 0; i < numLookupEntries; i++) {
          int idx = src.getUnsignedShort(0); src.addToBaseOffset(2);
          frameIndices.add(idx);
        }

        // processing cycle entries (part 2: assigning cycle frames)
        cycles = new ArrayList<ArrayList<Integer>>(numCycles);
        src.setBaseOffset(ofsCycleEntries);
        for (int i = 0; i < numCycles; i++) {
          int cnt = src.getUnsignedShort(0x00);
          int idx = src.getUnsignedShort(0x02);
          ArrayList<Integer> list = new ArrayList<Integer>(cnt);
          for (int j = idx; j < idx + cnt; j++) {
            list.add(frameIndices.get(j));
          }
          cycles.add(list);
          src.addToBaseOffset(0x04);
        }

        curCycle = 0;
        curFrame = 0;
      } catch (Exception e) {
        type = null;
        if (frames != null) {
          frames.clear();
          frames = null;
        }
        if (cycles != null) {
          cycles.clear();
          cycles = null;
        }
        curCycle = curFrame = 0;
        e.printStackTrace();
      }
    }

    // Decodes a frame
    private Image decodeImage(DynamicArray buffer, int w, int h, int[] palette, int compColor,
                              boolean isCompressed)
    {
      BufferedImage image = null;
      if (w > 0 && h > 0 && palette != null && palette.length >= 256) {
        image = ColorConvert.createCompatibleImage(w, h, Transparency.BITMASK);
        int[] dstData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

        if (isCompressed) {
          // transparent color is run-length encoded
          int srcIdx = 0, dstIdx = 0, dstIdxMax = w*h;
          while (dstIdx < dstIdxMax) {
            int v = buffer.getUnsignedByte(srcIdx++);
            int color = palette[v];
            if (v == compColor) {
              int cnt = buffer.getUnsignedByte(srcIdx++) + 1;
              if (dstIdx + cnt > dstIdxMax)
                cnt = dstIdxMax - dstIdx;
              Arrays.fill(dstData, dstIdx, dstIdx + cnt, color);
              dstIdx += cnt;
            } else {
              dstData[dstIdx++] = color;
            }
          }
        } else {
          // simply convert palette indices into color entries pixel by pixel
          byte[] srcData = buffer.get(0, w*h);
          for (int i = 0; i < w*h; i++) {
            int v = srcData[i] & 0xff;
            int color = palette[v];
            dstData[i] = color;
          }
        }
      }
      return image;
    }
  }


  // Handles BAM V2 resources
  private static class BamDataV2 implements BamData
  {
    private BamType type = null;
    private ArrayList<BamFrame> frames = null;
    private ArrayList<ArrayList<Integer>> cycles = null;
    private int curCycle = 0, curFrame = 0;
    private ConcurrentHashMap<Integer, PvrDecoder> pvrTable;

    // ignoreTransparency will be ignored
    public BamDataV2(ResourceEntry entry, boolean ignoreTransparency)
    {
      init(entry);
    }

    @Override
    public BamType type()
    {
      if (!empty()) {
        return type;
      } else
        return null;
    }

    @Override
    public int cycleCount()
    {
      if (!empty()) {
        return cycles.size();
      } else
        return 0;
    }

    @Override
    public boolean cycleSet(int idx)
    {
      if (!empty()) {
        if (idx >= 0 && idx < cycles.size()) {
          curCycle = idx;
          curFrame = 0;
          return true;
        }
      }
      return false;
    }

    @Override
    public int cycleGet()
    {
      if (!empty()) {
        return curCycle;
      } else
        return -1;
    }

    @Override
    public int cycleFrameCount()
    {
      if (!empty()) {
        return cycles.get(curCycle).size();
      } else
        return 0;
    }

    @Override
    public boolean cycleHasNextFrame()
    {
      if (!empty()) {
        return (curFrame+1) < cycleFrameCount();
      } else
        return false;
    }

    @Override
    public boolean cycleNextFrame()
    {
      if (!empty()) {
        if (cycleHasNextFrame()) {
          curFrame++;
          return true;
        }
      }
      return false;
    }

    @Override
    public void cycleReset()
    {
      if (!empty()) {
        curFrame = 0;
      }
    }

    @Override
    public Image cycleGetFrame()
    {
      if (!empty()) {
        int idx = cycleGetFrameIndexAbs();
        if (idx >= 0) {
          return frames.get(idx).image;
        }
      }
      return null;
    }

    @Override
    public int cycleGetFrameIndex()
    {
      if (!empty()) {
        return curFrame;
      } else
        return 0;
    }

    @Override
    public boolean cycleSetFrameIndex(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < cycles.get(curCycle).size()) {
          curFrame = frameIdx;
          return true;
        }
      }
      return false;
    }

    @Override
    public int cycleGetFrameIndexAbs()
    {
      return cycleGetFrameIndexAbs(curFrame);
    }

    @Override
    public int cycleGetFrameIndexAbs(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < cycles.get(curCycle).size()) {
          int idx = cycles.get(curCycle).get(frameIdx);
          if (idx >= 0 && idx < frames.size()) {
            return idx;
          }
        }
      }
      return -1;
    }

    @Override
    public int frameCount()
    {
      if (!empty()) {
        return frames.size();
      } else
        return 0;
    }

    @Override
    public Image frameGet(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).image;
        }
      }
      return null;
    }

    @Override
    public int frameWidth(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).width;
        }
      }
      return 0;
    }

    @Override
    public int frameHeight(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).height;
        }
      }
      return 0;
    }

    @Override
    public int frameCenterX(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).centerX;
        }
      }
      return 0;
    }

    @Override
    public int frameCenterY(int frameIdx)
    {
      if (!empty()) {
        if (frameIdx >= 0 && frameIdx < frames.size()) {
          return frames.get(frameIdx).centerY;
        }
      }
      return 0;
    }

    @Override
    public boolean frameCompressed(int frameIdx)
    {
      return false;
    }

    private boolean empty()
    {
      return (type == null) || (frames == null) || (cycles == null);
    }

    private void init(ResourceEntry entry)
    {
      if (entry == null)
        throw new NullPointerException();

      try {
        pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>();
        byte[] buffer = entry.getResourceData();
        String signature = DynamicArray.getString(buffer, 0, 4);
        String version = DynamicArray.getString(buffer, 4, 4);
        if (!(signature.equals("BAM ") && version.equals("V2  "))) {
          type = BamType.INVALID;
          throw new Exception("Invalid BAM resource");
        }
        type = BamType.BAMV2;

        DynamicArray src = DynamicArray.wrap(buffer, DynamicArray.ElementType.BYTE);
        final int dataBlockSize = 0x1c;   // size of a single data block
        int numFrames = src.getInt(0x08);
        if (numFrames <= 0)
          throw new Exception("Invalid number of frames: " + numFrames);
        int numCycles = src.getInt(0x0c);
        if (numCycles <= 0)
          throw new Exception("Invalid number of cycles: " + numCycles);
        int numBlocks = src.getInt(0x10);
        if (numBlocks <= 0)
          throw new Exception("Invalid number of data blocks: " + numBlocks);
        int ofsFrameEntries = src.getInt(0x14);
        if (ofsFrameEntries < 0x20)
          throw new Exception(String.format("Invalid frame entries offset: 0x%1$x", ofsFrameEntries));
        int ofsCycleEntries = src.getInt(0x18);
        if (ofsCycleEntries < 0x20)
          throw new Exception(String.format("Invalid cycle entries offset: 0x%1$x", ofsCycleEntries));
        int ofsDataBlocks = src.getInt(0x1C);
        if (ofsDataBlocks < 0x20)
          throw new Exception(String.format("Invalid data blocks offset: 0x%1$x", ofsDataBlocks));

        // processing frame entries
        frames = new ArrayList<BamDecoder.BamFrame>(numFrames);
        src.setBaseOffset(ofsFrameEntries);
        for (int i = 0; i < numFrames; i++) {
          int w = src.getUnsignedShort(0x00);
          int h = src.getUnsignedShort(0x02);
          int cx = src.getShort(0x04);
          int cy = src.getShort(0x06);
          int idxDataBlocks = src.getShort(0x08);
          int numDataBlocks = src.getShort(0x0a);
          frames.add(new BamFrame(decodeImage(src.asByteArray(ofsDataBlocks + idxDataBlocks*dataBlockSize),
                                              w, h, numDataBlocks),
                                  w, h, cx, cy));
          src.addToBaseOffset(0x0c);
        }
//        System.err.println("Number of PVR pages used: " + pvrTable.size());

        // processing cycle entries
        cycles = new ArrayList<ArrayList<Integer>>(numCycles);
        src.setBaseOffset(ofsCycleEntries);
        for (int i = 0; i < numCycles; i++) {
          int cnt = src.getUnsignedShort(0x00);
          int idx = src.getUnsignedShort(0x02);
          ArrayList<Integer> list = new ArrayList<Integer>(cnt);
          for (int j = idx; j < idx + cnt; j++) {
            list.add(j);
          }
          cycles.add(list);
          src.addToBaseOffset(0x04);
        }

        curCycle = 0;
        curFrame = 0;
      } catch (Exception e) {
        type = null;
        if (frames != null) {
          frames.clear();
          frames = null;
        }
        if (cycles != null) {
          cycles.clear();
          cycles = null;
        }
        curCycle = curFrame = 0;
        e.printStackTrace();
      }
    }

    // Returns a PVR object of the specified page
    private PvrDecoder getPVR(int page) throws Exception
    {
      synchronized (pvrTable) {
        if (pvrTable.containsKey(page))
          return pvrTable.get(page);

        String pvrzName = String.format("MOS%1$04d.PVRZ", page);
        ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(pvrzName);
        if (entry != null) {
          byte[] data = entry.getResourceData();
          if (data != null) {
            int size = DynamicArray.getInt(data, 0);
            int marker = DynamicArray.getUnsignedShort(data, 4);
            if ((size & 0xff) != 0x34 || marker != 0x9c78)
              throw new Exception("Invalid PVRZ resource: " + entry.getResourceName());
            data = Compressor.decompress(data, 0);
            PvrDecoder decoder = new PvrDecoder(data);
            pvrTable.put(page, decoder);
            return decoder;
          }
        }
      }
      throw new Exception("PVR page #" + page + " not found");
    }

    private Image decodeImage(DynamicArray buffer, int w, int h, int numBlocks)
    {
      BufferedImage image = null;
      if (w > 0 && h > 0) {
        image = ColorConvert.createCompatibleImage(w, h, Transparency.TRANSLUCENT);

        for (int i = 0; i < numBlocks; i++) {
          int page = buffer.getInt(0x00);
          int srcX = buffer.getInt(0x04);
          int srcY = buffer.getInt(0x08);
          int width = buffer.getInt(0x0c);
          int height = buffer.getInt(0x10);
          int dstX = buffer.getInt(0x14);
          int dstY = buffer.getInt(0x18);
          buffer.addToBaseOffset(0x1c);

          try {
            PvrDecoder decoder = getPVR(page);
            if (decoder != null) {
              BufferedImage srcImage = decoder.decode(srcX, srcY, width, height);
              Graphics2D g = (Graphics2D)image.getGraphics();
              g.drawImage(srcImage, dstX, dstY, null);
              g.dispose();
              g = null;
              decoder = null;
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

      }
      return image;
    }
  }


  // Stores BAM frame specific data
  private static class BamFrame
  {
    public Image image;
    public final int width, height, centerX, centerY;
    public final boolean isCompressed;

    public BamFrame(Image img, int w, int h, int cx, int cy)
    {
      this(img, w, h, cx, cy, false);
    }

    public BamFrame(Image img, int w, int h, int cx, int cy, boolean compressed)
    {
      image = img;
      width = w;
      height = h;
      centerX = cx;
      centerY = cy;
      isCompressed = compressed;
    }
  }
}
