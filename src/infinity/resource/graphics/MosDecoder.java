// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decodes either a single data block or a whole MOS resource.
 * @author argent77
 */
public class MosDecoder
{
  private static final String NOT_INITIALIZED = "Not initialized";

  private ResourceEntry entry;
  private MosInfo info;
  private ConcurrentHashMap<Integer, PvrDecoder> pvrTable;

  /**
   * Creates an uninitialized MosDecoder object. Use <code>open()</code> to load a MOS resource.
   */
  public MosDecoder()
  {
    close();
  }

  /**
   * Initialize this object using the specified filename.
   * @param mosName Filename of the MOS file
   * @throws Exception
   */
  public MosDecoder(String mosName) throws Exception
  {
    open(mosName);
  }

  /**
   * Initialize this object using the specified resource entry.
   * @param entry Resource entry structure of the MOS resource.
   * @throws Exception
   */
  public MosDecoder(ResourceEntry entry) throws Exception
  {
    open(entry);
  }

  public void close()
  {
    entry = null;
    info = null;
    if (pvrTable != null) {
      for (final PvrDecoder pvr: pvrTable.values()) {
        pvr.close();
      }
      pvrTable.clear();
    }
    pvrTable = null;
  }

  /**
   * Initialize this object using the specified filename.
   * @param mosName Filename of the MOS file
   * @throws Exception
   */
  public void open(String mosName) throws Exception
  {
    open(ResourceFactory.getInstance().getResourceEntry(mosName));
  }

  /**
   * Initialize this object using the specified resource entry.
   * @param entry Resource entry structure of the MOS resource.
   * @throws Exception
   */
  public void open(ResourceEntry entry) throws Exception
  {
    close();

    this.entry = entry;
    if (this.entry == null)
      throw new NullPointerException();
    init();
  }

  /**
   * Returns whether this MosDecoder object has already been successfully initialized.
   * @return Whether this MosDecoder object has already been initialized.
   */
  public boolean isOpen()
  {
    return !empty();
  }

  /**
   * Returns an interface to MOS-specific properties.
   * @return Interface to MOS-specific properties if available, null otherwise.
   */
  public MosInfo info()
  {
    if (!empty())
      return info;
    else
      return null;
  }

  /**
   * Decodes the currently loaded MOS file and returns the result as a new BufferedImage object.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception
   */
  public BufferedImage decoder(boolean ignoreTransparency) throws Exception
  {
    if (!empty()) {
      BufferedImage image = ColorConvert.createCompatibleImage(info().width(), info().height(), true);
      if (decode(image, ignoreTransparency)) {
        return image;
      } else {
        image = null;
      }
    }
    return null;
  }

  /**
   * Decodes the currently loaded MOS file and draws the result into the specified BufferedImage
   * object.
   * @param image The BufferedImage object to draw the MOS image into.
   * @return <code>true</code> if the image has been drawn successfully, <code>false</code> otherwise.
   * @throws Exception
   */
  public boolean decode(BufferedImage image, boolean ignoreTransparency) throws Exception
  {
    if (!empty()) {
      if (image == null)
        throw new NullPointerException();
      if (image.getWidth() < info().width() || image.getHeight() < info().height())
        throw new Exception("Image dimensions too small");

      for (final BlockInfo blockInfo: info.tiles) {
        if (!decodeMosBlock(image, blockInfo.dstX, blockInfo.dstY, blockInfo, ignoreTransparency)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Decodes a single data block of the MOS file and returns the result as a new BufferedImage object.
   * @param blockIndex Refers to the block to decode.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception
   */
  public BufferedImage decodeBlock(int blockIndex, boolean ignoreTransparency) throws Exception
  {
    if (!empty()) {
      if (blockIndex >= 0 && blockIndex < info.tiles.size()) {
        BlockInfo bi = info.tiles.get(blockIndex);
        if (bi != null) {
          BufferedImage image = ColorConvert.createCompatibleImage(bi.width, bi.height, true);
          if (decodeBlock(image, blockIndex, ignoreTransparency)) {
            return image;
          } else {
            image = null;
          }
        }
      }
    }
    return null;
  }

  /**
   * Decodes a single data block of the MOS file and draws the result into the specified
   * BufferedImage object.
   * @param image The BufferedImage object to draw the MOS data block into.
   * @param blockIndex Refers to the block to decode.
   * @return <code>true</code> if the image has been drawn successfully, <code>false</code> otherwise.
   * @throws Exception
   */
  public boolean decodeBlock(BufferedImage image, int blockIndex, boolean ignoreTransparency) throws Exception
  {
    if (!empty()) {
      if (image == null)
        throw new NullPointerException();
      if (blockIndex < 0 || blockIndex >= info.tiles.size())
        throw new IndexOutOfBoundsException();

      BlockInfo bi = info.tiles.get(blockIndex);
      if (bi != null) {
        if (image.getWidth() < bi.width || image.getHeight() < bi.height)
          throw new Exception("Image dimensions too small");

        return decodeMosBlock(image, 0, 0, bi, ignoreTransparency);
      }
    }
    return false;
  }

  private boolean empty()
  {
    if (info != null)
      return info.empty();
    else
      return true;
  }

  private void init() throws Exception
  {
    if (entry == null)
      throw new NullPointerException();

    info = new MosInfo(entry);
    if (info.empty())
      throw new Exception("Error parsing MOS resource");

    pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>(6, 0.75f);
  }

  //returns a PVR object of the specified page
  private PvrDecoder getPVR(int page) throws Exception
  {
    if (!empty()) {
      if (info().type() == MosInfo.MosType.PVRZ) {
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
      } else
        return null;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  // Decodes a single MOS data block
  private boolean decodeMosBlock(BufferedImage image, int startX, int startY,
                                 BlockInfo info, boolean ignoreTransparency) throws Exception
  {
    if (!empty()) {
      switch (info.type) {
        case PALETTE:
          return decodeMosBlockInPlace(image, startX, startY, info, ignoreTransparency);
        case PVRZ:
          return decodeMosBlockPVRZ(image, startX, startY, info);
      }
    }
    return false;
  }

  // Decodes a palette-based MOS tile
  private boolean decodeMosBlockInPlace(BufferedImage image, int startX, int startY,
                                        BlockInfo info, boolean ignoreTransparency) throws Exception
  {
    if (!empty()) {
      if (image == null || info == null || info.data == null || info.palette == null)
        throw new NullPointerException();
      if (info.type != MosInfo.MosType.PALETTE)
        throw new Exception("Incompatible format of MOS data block");
      if (info.data.length < info.width*info.height || info.palette.length < 256)
        throw new Exception("MOS data block too small");
      if (startX < 0 || startX + info.width > image.getWidth() ||
          startY < 0 || startY + info.height > image.getHeight())
        throw new Exception("Image dimensions too small");

      int[] dataBlock = new int[info.width*info.height];
      int alphaMask = ignoreTransparency ? 0xff000000 : 0;
      for (int i = 0; i < dataBlock.length; i++) {
        dataBlock[i] = info.palette[info.data[i] & 0xff] | alphaMask;
      }
      image.setRGB(startX, startY, info.width, info.height, dataBlock, 0, info.width);
      dataBlock = null;

      return true;
    }
    return false;
  }

  // Decodes a PVRZ-based MOS chunk
  private boolean decodeMosBlockPVRZ(BufferedImage image, int startX, int startY,
                                     BlockInfo info) throws Exception
  {
    if (!empty()) {
      if (image == null || info == null)
        throw new NullPointerException();
      if (info.type != MosInfo.MosType.PVRZ)
        throw new Exception("Incompatible format of MOS data block");
      if (info.width < 1 || info.height < 1)
        throw new Exception("Invalid block dimensions: " + info.width + "x" + info.height);
      if (startX < 0 || startX + info.width > image.getWidth() ||
          startY < 0 || startY + info.height > image.getHeight())
        throw new Exception("Image dimensions too small");

      PvrDecoder decoder = getPVR(info.page);
      if (decoder != null) {
        BufferedImage imgTile = decoder.decode(info.srcX, info.srcY,
                                               info.width, info.height);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.drawImage(imgTile, startX, startY, null);
        g.dispose();
        g = null;
        decoder = null;
        return true;
      }
    }
    return false;
  }


//-------------------------- INNER CLASSES --------------------------

  public static class MosInfo
  {
    /**
     * Describes the type of the MOS file.<br>
     * PALETTE: MOS file consists of palette-based blocks, stored in-place.<br>
     * PVRZ:    MOS file consists of references to data blocks, stored in associated PVRZ files.
     */
    public enum MosType { PALETTE, PVRZ }

    private int version, width, height, colCount, rowCount, blockCount, tileDim, headerSize, dataSize;
    private MosType type;
    private boolean initialized, compressed;
    private ArrayList<BlockInfo> tiles;

    public MosInfo(ResourceEntry entry) throws Exception
    {
      init(entry);
    }

    /**
     * Returns the specific MOS type.
     * (Note: Does not indicate whether the MOS is compressed. Use isCompressed() instead.)
     * @return The specific MOS type
     */
    public MosType type()
    {
      return type;
    }

    /**
     * Returns the MOS version (1 or 2).
     * @return The MOS version
     */
    public int version()
    {
      return version;
    }

    /**
     * Returns the image width in pixels.
     * @return Image width in pixels
     */
    public int width()
    {
      return width;
    }

    /**
     * Returns the image height in pixels.
     * @return Image height in pixels
     */
    public int height()
    {
      return height;
    }

    /**
     * Tells whether the MOS is in compressed format (MOSC).
     * @return Whether the MOS is compressed.
     */
    public boolean isCompressed()
    {
      return compressed;
    }

    /**
     * Returns the number of tiles per row (PALETTE type only).
     * @return Number of tiles per row
     */
    public int columnCount()
    {
      return colCount;
    }

    /**
     * Returns the number of rows (PALETTE type only).
     * @return Number of rows
     */
    public int rowCount()
    {
      return rowCount;
    }

    /**
     * Returns the number of data blocks (PVRZ type only).
     * @return Number of data blocks
     */
    public int blockCount()
    {
      return blockCount;
    }

    /**
     * Returns additional information about a specific MOS data block.
     * @param index Index of the MOS data block
     * @return Information structure about specific MOS data block
     */
    public BlockInfo blockInfo(int index)
    {
      if (!empty() && (index >= 0 && index < tiles.size()))
        return tiles.get(index);
      else
        throw new IndexOutOfBoundsException();
    }

    private void init(ResourceEntry entry) throws Exception
    {
      initialized = false;

      if (entry == null)
        throw new NullPointerException();

      byte[] buffer = entry.getResourceData();
      if (buffer == null)
        throw new NullPointerException();

      if (buffer.length < 0x18)
        throw new Exception("Input buffer too small");

      int ofs = 0;

      // evaluating signature
      String s = new String(buffer, ofs, 4);
      if (s.equals("MOSC")) {
        compressed = true;
        buffer = Compressor.decompress(buffer, ofs+8);
        if (buffer == null || buffer.length - ofs < 0x18)
          throw new Exception("Error decompressing MOS");
        s = new String(buffer, ofs, 4);
      } else {
        compressed = false;
      }

      if (!s.equals("MOS "))
        throw new Exception("Invalid MOS signature: '" + s + "'");

      // evaluating version
      s = new String(buffer, ofs+4, 4);
      if (s.equals("V1  ")) {
        type = MosType.PALETTE;
        version = 1;
      } else if (s.equals("V2  ")) {
        type = MosType.PVRZ;
        version = 2;
      } else {
        throw new Exception("Invalid MOS version: '" + s + "'");
      }

      if (type == MosType.PALETTE) {
        // parsing palette-based MOS resource
        width = DynamicArray.getShort(buffer, ofs + 8);
        height = DynamicArray.getShort(buffer, ofs + 10);
        colCount = DynamicArray.getShort(buffer, ofs + 12);
        rowCount = DynamicArray.getShort(buffer, ofs + 14);
        blockCount = colCount*rowCount;
        tileDim = DynamicArray.getInt(buffer, ofs + 16);
        headerSize = DynamicArray.getInt(buffer, ofs + 20);
        dataSize = blockCount*(1024+4) + width*height;   // total size of palette, offset and data blocks
        if (buffer.length < dataSize + headerSize)
          throw new Exception("Input buffer too small");

        tiles = new ArrayList<BlockInfo>(blockCount);
        for (int i = 0; i < blockCount; i++)
          tiles.add(new BlockInfo());

        // initializing palette data
        ofs = headerSize;
        for (final BlockInfo info: tiles) {
          info.palette = new int[256];
          for (int i = 0; i < 256; i++, ofs+=4) {
            info.palette[i] = (buffer[ofs] & 0xff) | ((buffer[ofs+1] & 0xff) << 8) |
                              ((buffer[ofs+2] & 0xff) << 16) | 0xff000000;
            // making palette indices matching "green" transparent
            int r = (info.palette[i] >>> 16) & 0xff, g = (info.palette[i] >>> 8) & 0xff, b = info.palette[i] & 0xff;
            if ((r < 8 && g > 248 && b < 8)) {
              info.palette[i] &= 0x00ffffff;
            }
          }
        }

        // initializing tile data
        int[] blockOffsets = new int[blockCount];
        for (int i = 0; i < blockCount; i++, ofs+=4) {
          blockOffsets[i] = DynamicArray.getInt(buffer, ofs);
        }
        for (int idx = 0; idx < blockCount; idx++) {
          int x = idx % colCount;
          int y = idx / colCount;
          BlockInfo info = tiles.get(idx);
          info.type = type;
          info.dstX = x*tileDim;
          info.dstY = y*tileDim;
          if (x == colCount - 1) {
            info.width = width % tileDim != 0 ? width % tileDim : tileDim;
          } else {
            info.width = tileDim;
          }
          if (y == rowCount - 1) {
            info.height = (height % tileDim != 0) ? height % tileDim : tileDim;
          } else {
            info.height = tileDim;
          }
          info.data = new byte[info.width*info.height];
          System.arraycopy(buffer, ofs + blockOffsets[idx], info.data, 0, info.data.length);
        }
      } else {
        // parsing PVRZ-based MOS resource
        width = DynamicArray.getInt(buffer, ofs + 8);
        height = DynamicArray.getInt(buffer, ofs + 12);
        blockCount = DynamicArray.getInt(buffer, ofs + 16);
        headerSize = DynamicArray.getInt(buffer, ofs + 20);
        dataSize = blockCount*28;
        if (buffer.length - ofs < headerSize + dataSize)
          throw new Exception("Input buffer too small");

        ofs = headerSize;
        tiles = new ArrayList<MosDecoder.BlockInfo>(blockCount);
        for (int idx = 0; idx < blockCount; idx++, ofs+=28) {
          BlockInfo info = new BlockInfo();
          info.type = type;
          info.page = DynamicArray.getInt(buffer, ofs);
          info.srcX = DynamicArray.getInt(buffer, ofs + 4);
          info.srcY = DynamicArray.getInt(buffer, ofs + 8);
          info.width = DynamicArray.getInt(buffer, ofs + 12);
          info.height = DynamicArray.getInt(buffer, ofs + 16);
          info.dstX = DynamicArray.getInt(buffer, ofs + 20);
          info.dstY = DynamicArray.getInt(buffer, ofs + 24);
          tiles.add(info);
        }
      }

      initialized = true;
    }

    private boolean empty()
    {
      return !initialized;
    }
  }


  // Info structure for a single MOS data block
  public static class BlockInfo
  {
    private MosInfo.MosType type; // MOS type
    private int[] palette;        // 256 RGBA palette entries (Palette-based MOS only)
    private byte[] data;          // the raw tile data (Palette-based MOS only)
    private int page;             // PVRZ page (PVRZ-based MOS only)
    private int srcX, srcY;       // pixel coordinate in PVRZ (PVRZ-based MOS only)
    private int dstX, dstY;       // pixel coordinate in MOS
    private int width, height;    // width and height of the tile

    // public read-only interface
    public MosInfo.MosType type() { return type; }
    public int width() { return width; }
    public int height() { return height; }
    public int sourceX() { return srcX; }
    public int sourceY() { return srcY; }
    public int x() { return dstX; }
    public int y() { return dstY; }
    public int page() { return page; }

    private BlockInfo()
    {
      palette = null;
      data = null;
      page = srcX = srcY = dstX = dstY = width = height = 0;
    }
  }
}
