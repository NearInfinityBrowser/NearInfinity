// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.Byteconvert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decodes either a single data block or a whole MOS resource.
 * @author argent77
 */
public class MosDecoder
{
  private static final String NOT_INITIALIZED = "Not initialized";

  private MosInfo info;
  private ConcurrentHashMap<Integer, PvrDecoder> pvrTable;

  /**
   * Initialize this object using the specified filename.
   * @param mosName Filename of the MOS file
   * @throws Exception
   */
  public MosDecoder(String mosName) throws Exception
  {
    ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(mosName);
    if (entry == null)
      throw new NullPointerException();

    init(entry.getResourceData(), 0);
  }

  /**
   * Initialize this object using the specified resource entry.
   * @param entry Resource entry structure of the MOS resource.
   * @throws Exception
   */
  public MosDecoder(ResourceEntry entry) throws Exception
  {
    if (entry == null)
      throw new NullPointerException();

    init(entry.getResourceData(), 0);
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
   * Decodes the currently loaded MOS file as either raw pixel data using the
   * specified color format.
   * @param fmt Color format of the output data
   * @return A buffer containing the resulting image data.
   * @throws Exception
   */
  public byte[] decode(ColorConvert.ColorFormat fmt) throws Exception
  {
    if (!empty()) {

      int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
      int dataSize = info.width*info.height*outPixelSize;
      byte[] data = new byte[dataSize];

      for (final BlockInfo blockInfo: info.tiles) {
        byte[] block = decodeMOSBlock(blockInfo, fmt);
        copyBuffer(block, 0, blockInfo.width, blockInfo.height,
                   data, 0, info.width, info.height,
                   blockInfo.dstX, blockInfo.dstY, fmt);
      }

      return data;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  /**
   * Decodes a single data block of the MOS file and returns it as raw pixel data in
   * the specified color format.
   * @param blockIndex Refers to the block to decode.
   * @param fmt Color format of the output data
   * @return A buffer containing the decoded pixel data of the data block.
   * @throws Exception
   */
  public byte[] decodeBlock(int blockIndex, ColorConvert.ColorFormat fmt) throws Exception
  {
    if (!empty()) {
      if (blockIndex < 0 || blockIndex >= info.tiles.size())
        throw new IndexOutOfBoundsException();

      BlockInfo bi = info.tiles.get(blockIndex);
      byte[] block = decodeMOSBlock(bi, fmt);

      return block;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  private boolean empty()
  {
    if (info != null)
      return info.empty();
    else
      return true;
  }

  private void init(byte[] buffer, int ofs) throws Exception
  {
    if (buffer == null)
      throw new NullPointerException();

    info = new MosInfo(buffer, ofs);
    if (info.empty())
      throw new Exception("Error initializing MOS resource");

    pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>(6, 0.75f);
  }

  //returns a PVR object of the specified page
  private PvrDecoder getPVR(int page) throws Exception
  {
    if (!empty()) {
      if (info().type() == MosInfo.MosType.PVRZ) {
        synchronized (pvrTable) {
          if (pvrTable.contains(page))
            return pvrTable.get(page);

          String pvrzName = String.format("MOS%1$04d.PVRZ", page);
          ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(pvrzName);
          if (entry != null) {
            byte[] data = entry.getResourceData();
            if (data != null) {
              int size = Byteconvert.convertInt(data, 0);
              int marker = Byteconvert.convertShort(data, 4) & 0xffff;
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
  private byte[] decodeMOSBlock(BlockInfo info, ColorConvert.ColorFormat fmt) throws Exception
  {
    if (!empty()) {
      if (info == null)
        throw new NullPointerException();

      byte[] blockBuffer = null;
      switch (info.type) {
        case PALETTE:
          blockBuffer = decodeMOSBlockInPlace(info, fmt);
          break;
        case PVRZ:
          blockBuffer = decodeMOSBlockPVRZ(info, fmt);
          break;
      }
      if (blockBuffer == null)
        throw new Exception("Invalid MOS data block");

      return blockBuffer;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  // Decodes a palette-based MOS tile
  private byte[] decodeMOSBlockInPlace(BlockInfo info, ColorConvert.ColorFormat fmt) throws Exception
  {
    if (info == null || info.data == null || info.palette == null)
      throw new NullPointerException();
    if (info.type != MosInfo.MosType.PALETTE)
      throw new Exception("Incompatible format of MOS data block");
    if (info.data.length < info.width*info.height || info.palette.length < 1024)
      throw new Exception("MOS data block too small");

    ColorConvert.ColorFormat inFormat = ColorConvert.ColorFormat.A8R8G8B8;
    int inPixelSize = ColorConvert.ColorBits(inFormat) >> 3;
    int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
    int pixelCount= info.width*info.height;
    byte[] workingBuffer = new byte[pixelCount*inPixelSize];
    for (int i = 0; i < pixelCount; i++)
      System.arraycopy(info.palette, (info.data[i] & 0xff)*inPixelSize, workingBuffer, i*inPixelSize, inPixelSize);

    byte[] outBuffer = new byte[pixelCount*outPixelSize];
    if (ColorConvert.Convert(inFormat, workingBuffer, 0, fmt, outBuffer, 0, pixelCount) == pixelCount)
      return outBuffer;
    else
      throw new Exception("Error decoding pixel data");
  }

  // Decodes a PVRZ-based MOS chunk
  private byte[] decodeMOSBlockPVRZ(BlockInfo info, ColorConvert.ColorFormat fmt) throws Exception
  {
    if (info == null)
      throw new NullPointerException();
    if (info.type != MosInfo.MosType.PVRZ)
      throw new Exception("Incompatible format of MOS data block");
    if (info.width < 1 || info.height < 1)
      throw new Exception("Invalid block dimensions: " + info.width + "x" + info.height);

    PvrDecoder decoder = getPVR(info.page);
    if (decoder != null) {
      return decoder.decode(info.srcX, info.srcY, info.width, info.height, fmt);
    } else
      throw new Exception("Error decoding pixel data");
  }

  // Copies a pixel block to another, starting at the specified pixel position
  private void copyBuffer(byte[] srcBuffer, int srcOfs, int srcWidth, int srcHeight,
                          byte[] dstBuffer, int dstOfs, int dstWidth, int dstHeight,
                          int dstX, int dstY, ColorConvert.ColorFormat fmt) throws Exception
  {
    if (srcBuffer == null || dstBuffer == null)
      throw new NullPointerException();
    if (srcWidth < 0 || srcHeight < 0 || dstWidth < 0 || dstHeight < 0)
      throw new Exception("Invalid pixel block dimensions");
    if (dstX < 0 || dstX + srcWidth > dstWidth || dstY < 0 || dstY + srcHeight > dstHeight)
      throw new Exception("Clipping not supported");

    int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
    if (srcOfs < 0 || srcBuffer.length - srcOfs < srcWidth*srcHeight*outPixelSize)
      throw new Exception("Source buffer too small");
    if (dstOfs < 0 || dstBuffer.length - dstOfs < dstWidth*dstHeight*outPixelSize)
      throw new Exception("Target buffer too small");

    int srcLineSize = srcWidth*outPixelSize;
    int dstLineSize = dstWidth*outPixelSize;
    dstOfs = dstOfs + (dstY*dstWidth + dstX)*outPixelSize;
    for (int y = 0; y < srcHeight; y++) {
      System.arraycopy(srcBuffer, srcOfs, dstBuffer, dstOfs, srcLineSize);
      srcOfs += srcLineSize;
      dstOfs += dstLineSize;
    }
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

    public MosInfo(byte[] buffer, int ofs) throws Exception
    {
      init(buffer, ofs);
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

    private void init(byte[] buffer, int ofs) throws Exception
    {
      initialized = false;

      if (buffer == null)
        throw new NullPointerException();
      if (ofs < 0)
        throw new Exception("Invalid buffer offset");
      if (buffer.length - ofs < 0x18)
        throw new Exception("Input buffer too small");

      ByteBuffer header = ByteBuffer.wrap(buffer, ofs, buffer.length - ofs).order(ByteOrder.LITTLE_ENDIAN);

      // evaluating signature
      String s = new String(buffer, ofs, 4);
      if (s.equals("MOSC")) {
        compressed = true;
        buffer = Compressor.decompress(buffer, ofs+8);
        ofs = 0;
        if (buffer == null || buffer.length - ofs < 0x18)
          throw new Exception("Error decompressing MOS");
        header = ByteBuffer.wrap(buffer, ofs, buffer.length).order(ByteOrder.LITTLE_ENDIAN);
        s = new String(buffer, ofs, 4);
      } else
        compressed = false;

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
      } else
        throw new Exception("Invalid MOS version: '" + s + "'");

      header.position(header.position() + 8);   // skipping signature and version
      if (type == MosType.PALETTE) {
        // parsing palette-based MOS resource
        width = header.getShort();
        height = header.getShort();
        colCount = header.getShort();
        rowCount = header.getShort();
        blockCount = colCount*rowCount;
        tileDim = header.getInt();
        headerSize = header.getInt();
        dataSize = blockCount*(1024+4) + width*height;   // total size of palette, offset and data blocks
        if (buffer.length - ofs < dataSize + headerSize)
          throw new Exception("Input buffer too small");

        tiles = new ArrayList<BlockInfo>(blockCount);
        for (int i = 0; i < blockCount; i++)
          tiles.add(new BlockInfo());

        // initializing palette data
        header.position(headerSize);
        for (final BlockInfo info: tiles) {
          info.palette = new byte[1024];
          header.get(info.palette, 0, 1024);
        }

        // initializing tile data
        int[] blockOffsets = new int[blockCount];
        for (int i = 0; i < blockCount; i++)
          blockOffsets[i] = header.getInt();
        int dataOfs = header.position();
        for (int idx = 0; idx < blockCount; idx++) {
          int x = idx % colCount;
          int y = idx / colCount;
          BlockInfo info = tiles.get(idx);
          info.type = type;
          info.dstX = x*tileDim;
          info.dstY = y*tileDim;
          if (x == colCount - 1)
            info.width = width % tileDim != 0 ? width % tileDim : tileDim;
          else
            info.width = tileDim;
          if (y == rowCount - 1)
            info.height = (height % tileDim != 0) ? height % tileDim : tileDim;
          else
            info.height = tileDim;
          header.position(dataOfs + blockOffsets[idx]);
          info.data = new byte[info.width*info.height];
          header.get(info.data, 0, info.data.length);
        }

      } else {
        // parsing PVRZ-based MOS resource
        width = header.getInt();
        height = header.getInt();
        blockCount = header.getInt();
        headerSize = header.getInt();
        dataSize = blockCount*28;
        if (buffer.length - ofs < headerSize + dataSize)
          throw new Exception("Input buffer too small");

        header.position(headerSize);
        tiles = new ArrayList<MosDecoder.BlockInfo>(blockCount);
        for (int idx = 0; idx < blockCount; idx++) {
          BlockInfo info = new BlockInfo();
          info.type = type;
          info.page = header.getInt();
          info.srcX = header.getInt();
          info.srcY = header.getInt();
          info.width = header.getInt();
          info.height = header.getInt();
          info.dstX = header.getInt();
          info.dstY = header.getInt();
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
    private byte[] palette;       // 256 RGBA palette entries (Palette-based MOS only)
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
      palette = data = null;
      page = srcX = srcY = dstX = dstY = width = height = 0;
    }
  }
}
