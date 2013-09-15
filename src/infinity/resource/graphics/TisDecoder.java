// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.ResourceFactory;
import infinity.resource.graphics.ColorConvert.ColorFormat;
import infinity.resource.key.ResourceEntry;
import infinity.util.Byteconvert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Decodes either a single tile or a block of tiles from a TIS resource.
 * @author argent77
 */
public class TisDecoder
{
  private static final String NOT_INITIALIZED = "Not initialized";

  private TisInfo info;
  private final ResourceEntry entry;                  // TIS resource entry structure
  private final byte[] tisBuffer;                     // points to the data of the TIS resource entry
  private String tisName;                             // TIS resource name without extension
  private ConcurrentHashMap<Integer, PvrDecoder> pvrTable;  // cache for associated PVR resources

  /**
   * Initialize this object using the specified filename.
   * @param tisName Filename of the TIS file
   * @throws Exception
   */
  public TisDecoder(String tisName) throws Exception
  {
    entry = ResourceFactory.getInstance().getResourceEntry(tisName);
    if (entry == null)
      throw new NullPointerException();
    tisBuffer = entry.getResourceData();

    init();
  }

  /**
   * Initialize this object using the specified resource entry.
   * @param entry Resource entry structure of the TIS resource.
   * @throws Exception
   */
  public TisDecoder(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    if (this.entry == null)
      throw new NullPointerException();
    tisBuffer = this.entry.getResourceData();

    init();
  }

  /**
   * Returns a resource info interface for easy access of the resource-specific properties.
   * @return A resource info interface if available, null otherwise.
   */
  public TisInfo info()
  {
    if (!empty())
      return info;
    else
      return null;
  }

  /**
   * Decodes the currently loaded TIS file as raw pixel data using
   * the specified color format.<br>
   * <b>Note:</b> This method is thread-safe.
   * @param tilesX Number of tiles per line (if tilesX == 0, then tilesX = 1 assumed)
   * @param tilesY Number of tile lines (if tilesY == 0, then tilesY = tileCount / tilesX assumed)
   * @param fmt Color format of the output data
   * @return A buffer containing the resulting image data.
   * @throws Exception
   */
  public byte[] decode(int tilesX, int tilesY, ColorFormat fmt) throws Exception
  {
    if (!empty()) {
      if (tilesX < 0 || tilesY < 0)
        throw new Exception("Invalid dimensions specified");

      if (tilesX == 0)
        tilesX = 1;
      if (tilesY == 0)
        tilesY = info().tileCount() / tilesX;
      if (tilesX*tilesY > info().tileCount())
        throw new Exception("Tiles dimension too big (" + tilesX + "x" + tilesY + "=" + tilesX*tilesY +
                            " tiles specified, but only " + info().tileCount() + " tiles available)");

      int width = tilesX * info().tileWidth();
      int height = tilesY * info().tileHeight();

      int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
      // decode into raw pixel format
      byte[] outBuffer = new byte[width*height*outPixelSize];
      if (decodeTIS(outBuffer, 0, tilesX, tilesY, fmt))
        return outBuffer;
      return null;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  /**
   * Decodes the specified tile of the currently loaded TIS file and returns it as raw pixel data
   * using the specified color format.<br>
   * <b>Note:</b> This method is thread-safe.
   * @param tileIndex The tile to extract (index starting at 0).
   * @param fmt Color format of the output data
   * @return A buffer containing the resulting output data of the tile.
   * @throws Exception
   */
  public byte[] decodeTile(int tileIndex, ColorFormat fmt) throws Exception
  {
    if (!empty()) {
      if (tileIndex < 0 || tileIndex > info().tileCount())
        throw new Exception("Tile index out of bounds");

      int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
      byte[] outBuffer = new byte[info().tileWidth()*info().tileHeight()*outPixelSize];
      if (decodeTISTile(outBuffer, 0, tileIndex, fmt))
        return outBuffer;
      return null;
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

  // initializes TIS resource (header and data)
  private void init() throws Exception
  {
    if (entry == null)
      throw new NullPointerException();

    info = null;
    tisName = null;
    pvrTable = null;

    info = new TisInfo(tisBuffer, 0);
    if (info.empty())
      throw new Exception("Error parsing TIS resource");

    if (tisBuffer.length < info.headerSize + info.dataSize)
      throw new Exception("Unexpected end of TIS resource data");

    if (!initPVR())
      info = null;
  }

  // checks for PVRZ compatibility
  private boolean initPVR() throws Exception
  {
    if (!empty()) {
      if (info().type() == TisInfo.TisType.PVRZ) {
        if (entry.getResourceName() != null) {
          tisName = entry.getResourceName();
          int n = tisName.lastIndexOf('.');
          if (n > 0)
            tisName = tisName.substring(0, n);
          if (tisName != null && Pattern.matches("[a-zA-Z]{2}[0-9]{4}[nN]?", tisName)) {
            pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>(30, 0.75f);
            return true;
          }
        }
        return false;
      } else
        return true;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  // returns a PVR object of the specified page
  private PvrDecoder getPVR(int page) throws Exception
  {
    if (!empty()) {
      if (info().type() == TisInfo.TisType.PVRZ) {
        synchronized (pvrTable) {
          if (pvrTable.containsKey(page))
            return pvrTable.get(page);

          String pvrzName = getPVRZName(page);
          ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(pvrzName);
          if (entry != null) {
            byte[] data = entry.getResourceData();
            if (data != null) {
              int size = Byteconvert.convertInt(data, 0);
              int marker = Byteconvert.convertShort(data, 4) & 0xffff;
              if ((size & 0xff) != 0x34 || marker != 0x9c78)
                throw new Exception("Invalid PVRZ resource: " + entry.getResourceName());
              data = Compressor.decompress(data, 0);
              PvrDecoder d = new PvrDecoder(data);
              pvrTable.put(page, d);
              return d;
            }
          }
        }
        throw new Exception("PVR page #" + page + " not found");
      } else
        return null;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  private String getPVRZName(int page) throws Exception
  {
    if (page < 0 || page > info().tileCount())
      throw new Exception("PVR page #" + page + " not available");

    String pvrzBase = tisName.substring(0, 1) + tisName.substring(2);
    String pvrzPage = String.format("%1$02d", page);
    return pvrzBase + pvrzPage + ".PVRZ";
  }

  // Decode tilesX*tilesY tiles into outBuffer, starting at ofs
  private boolean decodeTIS(byte[] outBuffer, int ofs, int tilesX, int tilesY, ColorFormat fmt) throws Exception
  {
    if (!empty()) {
      if (outBuffer == null)
        throw new NullPointerException();

      int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
      int tileCount = tilesX*tilesY;
      int tileSize = info().tileWidth()*info().tileHeight()*outPixelSize;
      if (outBuffer.length - ofs < tileCount*tileSize)
        throw new Exception("Output buffer too small");

      // 1. parsing through TIS data and decode each tile in turn
      int tileIdx = 0;
      int inBufferOfs = info().headerSize();
      int tileLineSize = info().tileWidth()*outPixelSize;
      int outLineSize = tilesX*info().tileWidth()*outPixelSize;
      byte[] tileBuffer = new byte[tileSize];
      boolean bRet = true;
      for (int ty = 0; ty < tilesY; ty++) {
        for (int tx = 0; tx < tilesX; tx++, tileIdx++, inBufferOfs+=info().tileSize()) {
          // for each tile...
          // ...decompress...
          if (info().type() == TisInfo.TisType.PALETTE)
            bRet &= decodeTISTileInPlace(tisBuffer, inBufferOfs, tileBuffer, 0, fmt);
          else
            bRet &= decodeTISTilePVRZ(tisBuffer, inBufferOfs, tileBuffer, 0, fmt);
          if (tileBuffer != null) {
            // ...and copy to output buffer
            int tileOfs = 0;    // current offset into tile buffer
            int outOfs = ofs + ty*tilesX*tileSize + tx*tileLineSize;    // current offset into output buffer
            for (int i = 0; i < info().tileHeight(); i++, tileOfs+=tileLineSize, outOfs+=outLineSize) {
              System.arraycopy(tileBuffer, tileOfs, outBuffer, outOfs, tileLineSize);
            }
          } else
            throw new Exception("Error decoding tile #" + tileIdx);
        }
      }

      return bRet;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  // Decode the single tile tileIdx into outBuffer, starting at ofs
  private boolean decodeTISTile(byte[] outBuffer, int ofs, int tileIdx, ColorFormat fmt) throws Exception
  {
    if (!empty()) {
      if (outBuffer == null)
        throw new NullPointerException();
      if (tileIdx < 0 || tileIdx > info().tileCount())
        throw new Exception("Tile index out of bounds");

      int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
      int tileSize = info().tileWidth()*info().tileHeight()*outPixelSize;
      int inBufferOfs = info().headerSize() + tileIdx*info().tileSize();
      if (ofs + tileSize > outBuffer.length)
        throw new Exception("Output buffer too small");

      // Decoding a single tile is trivial
      boolean bRet = false;
      if (info().type() == TisInfo.TisType.PALETTE)
        bRet = decodeTISTileInPlace(tisBuffer, inBufferOfs, outBuffer, ofs, fmt);
      else
        bRet = decodeTISTilePVRZ(tisBuffer, inBufferOfs, outBuffer, ofs, fmt);

      return bRet;
    } else
      throw new Exception(NOT_INITIALIZED);
  }

  private boolean decodeTISTileInPlace(byte[] inBuffer, int inOfs, byte[] outBuffer, int outOfs, ColorFormat fmt) throws Exception
  {
    if (inBuffer == null || outBuffer == null)
      throw new NullPointerException();
    if (inBuffer.length - inOfs < info().tileSize())
      throw new Exception("Input buffer size too small");

    ColorConvert.ColorFormat inFormat = ColorConvert.ColorFormat.A8R8G8B8;
    int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
    if (outBuffer.length - outOfs < info().tileWidth()*info.tileHeight()*outPixelSize)
      throw new Exception("Output buffer size too small");

    byte[] palette = new byte[256*outPixelSize];
    for (int i = 0; i < 256; i++)
      inBuffer[inOfs+(i << 2)+3] = (byte)0xff;    // fixing alpha value
    if (ColorConvert.Convert(inFormat, inBuffer, inOfs, fmt, palette, 0, 256) != 256)
      return false;
    inOfs += 1024;

    int tileLength = info().tileWidth()*info().tileHeight();
    for (int i = 0; i < tileLength; i++)
      System.arraycopy(palette, (inBuffer[inOfs+i] & 0xff)*outPixelSize, outBuffer, outOfs + i*outPixelSize, outPixelSize);

    return true;
  }

  private boolean decodeTISTilePVRZ(byte[] inBuffer, int inOfs, byte[] outBuffer, int outOfs, ColorFormat fmt) throws Exception
  {
    if (inBuffer == null)
      throw new NullPointerException();
    if (inBuffer.length - inOfs < info().tileSize())
      throw new Exception("Buffer size too small");

    int outPixelSize = ColorConvert.ColorBits(fmt) >> 3;
    if (outBuffer.length - outOfs < info().tileWidth()*info.tileHeight()*outPixelSize)
      throw new Exception("Output buffer size too small");

    int page = Byteconvert.convertInt(inBuffer, inOfs);
    if (page < 0) {
      // special case: fill with black pixels
      byte[] outPixel = new byte[outPixelSize];
      ColorConvert.Convert(ColorConvert.ColorFormat.A8R8G8B8, new byte[]{0, 0, 0, (byte)255}, 0,
                           fmt, outPixel, 0, 1);
      for (int i = 0; i < info().tileWidth()*info().tileHeight(); i++)
        System.arraycopy(outPixel, 0, outBuffer, outOfs + i*outPixelSize, outPixelSize);;
      return true;
    } else {
      // extract data block from associated PVR file
      int xPos = Byteconvert.convertInt(inBuffer, inOfs+4);
      int yPos = Byteconvert.convertInt(inBuffer, inOfs+8);
      PvrDecoder decoder = getPVR(page);
      if (decoder != null) {
        System.arraycopy(decoder.decode(xPos, yPos, info().tileWidth(), info().tileHeight(), fmt),
                         0, outBuffer, outOfs, info().tileWidth()*info().tileHeight()*outPixelSize);
        return true;
      } else
        return false;
    }
  }


//-------------------------- INNER CLASSES --------------------------

  /**
   * Manages header information for TIS files.
   */
  public static class TisInfo
  {
    /**
     * Describes the type of the TIS file.<br>
     * PALETTE: TIS file consists of palette-based chunks, stored in-place.<br>
     * PVRZ:    TIS file consists of references to the chunks, stored in associated PVRZ files.
     */
    public enum TisType { PALETTE, PVRZ }

    private int version = 0, tileCount = 0, tileSize = 0, headerSize = 0, tileDim = 0, dataSize = 0;
    private TisType type;
    private boolean initialized = false;

    public TisInfo(byte[] buffer, int ofs) throws Exception
    {
      if (buffer == null)
        throw new NullPointerException();

      init(buffer, ofs);
    }

    /**
     * The TIS version.
     * @return The TIS version.
     */
    public int version()
    {
      return version;
    }

    public TisType type() throws Exception
    {
      if (!empty())
        return type;
      else
        throw new Exception(NOT_INITIALIZED);
    }

    /**
     * Number of tiles defined within the TIS file.
     * @return Number of tiles available.
     */
    public int tileCount()
    {
      return tileCount;
    }

    /**
     * Size of a tile section within the TIS file.
     * @return Tile size in bytes.
     */
    public int tileSize()
    {
      return tileSize;
    }

    /**
     * Width of a TIS tile in pixels.
     * @return Width of a TIS tile in pixels.
     */
    public int tileWidth()
    {
      return tileDim;
    }

    /**
     * Height of a TIS tile in pixels.
     * @return Height of a TIS tile in pixels.
     */
    public int tileHeight()
    {
      return tileDim;
    }

    public int headerSize()
    {
      return headerSize;
    }

    public int dataSize()
    {
      return dataSize;
    }

    private void init(byte[] buffer, int ofs) throws Exception
    {
      if (buffer == null)
        throw new NullPointerException();
      if (buffer.length - ofs < 0x18)
        throw new Exception("Input buffer too small");

      ByteBuffer header = ByteBuffer.wrap(buffer, ofs, buffer.length - ofs).order(ByteOrder.LITTLE_ENDIAN);
      String s = new String(buffer, ofs, 4);
      if (!s.equals("TIS "))
        throw new Exception("Invalid TIS signature: '" + s + "'");
      s = new String(buffer, ofs+4, 4);
      try {
        if (Integer.parseInt(s.substring(1).trim()) <= 0)
          throw new Exception("Invalid TIS version: '" + s + "'");
      } catch (NumberFormatException e) {
        throw new Exception("Invalid TIS version: '" + s + "'");
      }

      header.position(header.position() + 8);   // skipping signature and version
      tileCount = header.getInt();
      if (tileCount <= 0)
        throw new Exception("Invalid tile count: " + tileCount);

      tileSize = header.getInt();
      if (tileSize <= 0)
        throw new Exception("Invalid tile size: " + tileSize);

      headerSize = header.getInt();
      if (headerSize < 0x18)
        throw new Exception("Invalid TIS header size: " + headerSize + " byte(s)");

      tileDim = header.getInt();
      if (tileDim <= 0)
        throw new Exception("Invalid tile dimensions: " + tileDim + "x" + tileDim);

      if (tileSize == 1024 + tileDim * tileDim)
        type = TisType.PALETTE;
      else if (tileSize == 12)
        type = TisType.PVRZ;
      else
        throw new Exception("TIS file type could not be determined");

      dataSize = tileCount * tileSize;

      initialized = true;
    }

    private boolean empty()
    {
      return !initialized;
    }
  }
}
