// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
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
  private ResourceEntry entry;      // TIS resource entry structure
  private byte[] tisBuffer;         // points to the data of the TIS resource entry
  private String tisName;           // TIS resource name without extension
  private ConcurrentHashMap<Integer, PvrDecoder> pvrTable;  // cache for associated PVR resources

  public TisDecoder()
  {
    close();
  }

  /**
   * Initialize this object using the specified filename.
   * @param tisName Filename of the TIS file
   * @throws Exception
   */
  public TisDecoder(String tisName) throws Exception
  {
    open(tisName);
  }

  /**
   * Initialize this object using the specified resource entry.
   * @param entry Resource entry structure of the TIS resource.
   * @throws Exception
   */
  public TisDecoder(ResourceEntry entry) throws Exception
  {
    open(entry);
  }

  /**
   * Closes the current TIS resource
   */
  public void close()
  {
    info = null;
    entry = null;
    tisName = null;
    tisBuffer = null;
    if (pvrTable != null) {
      for (final PvrDecoder pvr: pvrTable.values()) {
        try {
          pvr.close();
        } catch (Exception e) {
        }
      }
      pvrTable.clear();
    }
    pvrTable = null;
  }

  /**
   * Call this method if you want to free the TIS input data buffer from memory.
   */
  public void flush()
  {
    if (tisBuffer != null) {
      tisBuffer = null;
    }
  }

  /**
   * Initialize this object using the specified filename.
   * @param tisName Filename of the TIS file
   * @throws Exception
   */
  public void open(String tisName) throws Exception
  {
    open(ResourceFactory.getInstance().getResourceEntry(tisName));
  }

  /**
   * Initialize this object using the specified resource entry.
   * @param entry Resource entry structure of the TIS resource.
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
   * Returns whether this TisDecoder object has already been successfully initialized.
   * @return Whether this TisDecoder object has already been initialized.
   */
  public boolean isOpen()
  {
    return !empty();
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
   * Decodes the currently loaded TIS file and returns the result as a new BufferedImage object.
   * @param tileColumns Number of tile columns.
   * @param tileRows Number of tile rows (a value of 0 indicates <code>tileCount / tilesX</code>).
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception
   */
  public BufferedImage decode(int tileColumns, int tileRows) throws Exception
  {
    if (!empty()) {
      BufferedImage image = ColorConvert.createCompatibleImage(tileColumns*info().tileWidth(),
                                                               tileRows*info.tileHeight(),
                                                               Transparency.BITMASK);
      if (decode(image, tileColumns, tileRows)) {
        return image;
      } else {
        image = null;
      }
    }
    return null;
  }

  /**
   * Decodes the currently loaded TIS file and draws the result into the specified BufferedImage object.
   * The image will be clipped if the BufferedImage is too small to hold the complete graphics data.
   * @param image The BufferedImage object to draw the TIS map into.
   * @param tileCols Number of tile columns.
   * @param tileRows Number of tile rows (a value of 0 indicates <code>tileCount / tilesX</code>).
   * @return <code>true</code> if the image has been drawn successfully, <code>false</code> otherwise.
   * @throws Exception
   */
  public boolean decode(BufferedImage image, int tileCols, int tileRows) throws Exception
  {
    if (!empty()) {
      if (image == null)
        throw new NullPointerException();
      if (tileCols < 0 || tileRows < 0)
        throw new Exception("Invalid dimensions specified");

      if (tileCols == 0)
        tileCols = 1;
      if (tileRows == 0)
        tileRows = info().tileCount() / tileCols;

      if (image.getWidth() < tileCols*info().tileWidth() ||
          image.getHeight() < tileRows*info().tileHeight())
        throw new Exception("Image dimensions too small");
      if (tileCols*tileRows > info().tileCount())
        throw new Exception("Tiles dimension too big (" + tileCols + "x" + tileRows +
                            "=" + tileCols*tileRows + " tiles specified, but only " +
                            info().tileCount() + " tiles available)");

      int imgTileCols = image.getWidth() / info().tileWidth();
      if (image.getWidth() % info().tileWidth() != 0)
        imgTileCols++;
      int imgTileRows = image.getHeight() / info().tileHeight();
      if (image.getHeight() % info().tileHeight() != 0)
        imgTileRows++;

      BufferedImage imgTile =
          ColorConvert.createCompatibleImage(info().tileWidth(), info().tileHeight(), Transparency.BITMASK);
      Graphics2D g = (Graphics2D)image.getGraphics();
      for (int y = 0; y < imgTileRows; y++) {
        for (int x = 0; x < imgTileCols; x++) {
          int tileIdx = y*tileCols+x;
          if (decodeTile(imgTile, tileIdx)) {
            g.drawImage(imgTile, x*info().tileWidth(), y*info().tileHeight(), null);
          } else {
            g.dispose();
            return false;
          }
        }
      }
      g.dispose();
      imgTile.flush();
      imgTile = null;
      return true;
    }
    return false;
  }

  /**
   * Decodes the specified tile of the currently loaded TIS file and returns the result as a new
   * BufferedImage object.
   * @param tileIndex The tile to extract (index starting at 0).
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception
   */
  public BufferedImage decodeTile(int tileIndex) throws Exception
  {
    if (!empty()) {
      BufferedImage image = ColorConvert.createCompatibleImage(info().tileWidth(),
                                                               info().tileHeight(),
                                                               Transparency.BITMASK);
      if (decodeTile(image, tileIndex)) {
        return image;
      } else {
        image = null;
      }
    }
    return null;
  }

  /**
   * Decodes the specified tile of the currently loaded TIS file and draws the result into the
   * specified BufferedImage object.
   * @param image The BufferedImage object to draw the TIS tile into.
   * @param tileIndex The tile to extract (index starting at 0).
   * @return <code>true</code> if the tile has been drawn successfully, <code>false</code> otherwise.
   * @throws Exception
   */
  public boolean decodeTile(BufferedImage image, int tileIndex) throws Exception
  {
    if (!empty()) {
      if (image == null)
        throw new NullPointerException();
      if (image.getWidth() < info().tileWidth() || image.getHeight() < info().tileHeight())
        throw new Exception("Image dimensions too small");
      if (tileIndex < 0 || tileIndex > info().tileCount())
        throw new Exception("Tile index out of bounds");

      switch (info().type()) {
        case PALETTE:
          return decodeTileInPlace(image, tileIndex);
        case PVRZ:
          return decodeTilePVRZ(image, tileIndex);
        default:
          return false;
      }
    }
    return false;
  }

  // Returns TIS input data as byte buffer
  private byte[] getInputData()
  {
    if (!empty()) {
      if (tisBuffer == null) {
        try {
          tisBuffer = entry.getResourceData();
        } catch (Exception e) {
          tisBuffer = null;
        }
      }
      return tisBuffer;
    }
    return null;
  }

  private boolean empty()
  {
    if (entry != null && info != null)
      return info.empty();
    else
      return true;
  }

  // initializes TIS resource (header and data)
  private void init() throws Exception
  {
    if (entry == null)
      throw new NullPointerException();

    info = new TisInfo(entry);
    if (info.empty())
      throw new Exception("Error parsing TIS resource");

    if (!initPVR())
      close();
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
//          if (tisName != null && Pattern.matches("[a-zA-Z]{2}[0-9]{4}[nN]?", tisName)) {
          if (tisName != null && Pattern.matches(".{6}[nN]?", tisName)) {
            pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>(30, 0.75f);
            return true;
          }
        }
        return false;
      } else {
        return true;
      }
    } else {
      throw new Exception(NOT_INITIALIZED);
    }
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
              int size = DynamicArray.getInt(data, 0);
              int marker = DynamicArray.getUnsignedShort(data, 4);
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
      } else {
        return null;
      }
    } else {
      throw new Exception(NOT_INITIALIZED);
    }
  }

  private String getPVRZName(int page) throws Exception
  {
    if (page < 0 || page > info().tileCount())
      throw new Exception("PVR page #" + page + " not available");

    String pvrzBase = tisName.substring(0, 1) + tisName.substring(2);
    String pvrzPage = String.format("%1$02d", page);
    return pvrzBase + pvrzPage + ".PVRZ";
  }

  private boolean decodeTileInPlace(BufferedImage image, int tileIndex)
  {
    if (!empty()) {
      if (image == null)
        return false;
      if (image.getWidth() < info().tileWidth() || image.getHeight() < info().tileHeight())
        return false;
      if (tileIndex < 0 || tileIndex >= info().tileCount())
        return false;

      byte[] inBuffer = getInputData();
      if (inBuffer != null) {
        int inOfs = info().headerSize() + tileIndex*info().tileSize();
        if (inOfs + info().tileSize() > inBuffer.length)
          return false;

        int[] palette = new int[256];
        for (int i = 0; i < palette.length; i++, inOfs+=4) {
          palette[i] = DynamicArray.getInt(inBuffer, inOfs) | 0xff000000;
          if (i == 0 && (palette[i] & 0x00ffffff) == 0x0000ff00) {
            palette[i] &= 0x00ffffff;
          }
        }
        int[] dataBlock = new int[info().tileWidth()*info().tileHeight()];
        for (int i = 0; i < dataBlock.length; i++, inOfs++) {
          dataBlock[i] = palette[inBuffer[inOfs] & 0xff];
        }
        image.setRGB(0, 0, info().tileWidth(), info().tileHeight(), dataBlock, 0, info().tileWidth());
        palette = null;
        dataBlock = null;
        inBuffer = null;

        return true;
      }
    }
    return false;
  }

  private boolean decodeTilePVRZ(BufferedImage image, int tileIndex)
  {
    if (!empty()) {
      if (image == null)
        return false;
      if (image.getWidth() < info().tileWidth() || image.getHeight() < info().tileHeight())
        return false;
      if (tileIndex < 0 || tileIndex >= info().tileCount())
        return false;

      byte[] inBuffer = getInputData();
      if (inBuffer != null) {
        int inOfs = info().headerSize() + tileIndex*info().tileSize();
        if (inOfs + info().tileSize() > inBuffer.length)
          return false;

        int pvrPage = DynamicArray.getInt(inBuffer, inOfs);
        if (pvrPage < 0) {
          // special case: fill with black pixels
          Graphics2D g = (Graphics2D)image.getGraphics();
          g.setColor(Color.BLACK);
          g.fillRect(0, 0, info().tileWidth(), info().tileHeight());
          g.dispose();
        } else {
          try {
            // extract data block from associated PVR file
            int pvrX = DynamicArray.getInt(inBuffer, inOfs + 4);
            int pvrY = DynamicArray.getInt(inBuffer, inOfs + 8);
            PvrDecoder pvrDecoder = getPVR(pvrPage);
            if (pvrDecoder == null)
              return false;
            pvrDecoder.decode(image, pvrX, pvrY, info().tileWidth(), info().tileHeight());
          } catch (Exception e) {
            return false;
          }
        }
        inBuffer = null;
        return true;
      }
    }
    return false;
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

    private static int tileDim = 64;
    private static int headerSize = 24;

    private TisType type;
    private int tileCount, tileSize, dataSize;
    private boolean initialized = false;

    public TisInfo(ResourceEntry entry) throws Exception
    {
      if (entry == null)
        throw new NullPointerException();

      init(entry);
    }

    /**
     * Returns the TIS type (palette-based or PVRZ-based data blocks)
     * @return TIS type
     * @throws Exception
     */
    public TisType type()
    {
      if (!empty())
        return type;
      else
        return null;
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

    /**
     * Size of the TIS header in bytes.
     * @return TIS header size
     */
    public int headerSize()
    {
      return headerSize;
    }

    /**
     * Size of the TIS data in bytes.
     * @return TIS data size
     */
    public int dataSize()
    {
      return dataSize;
    }

    private void init(ResourceEntry entry) throws Exception
    {
      if (entry == null)
        throw new NullPointerException();

      int[] resInfo = entry.getResourceInfo();
      if (resInfo == null || resInfo.length < 2)
        throw new Exception("Error reading TIS header");

      tileCount = resInfo[0];
      if (tileCount <= 0)
        throw new Exception("Invalid tile count: " + tileCount);

      tileSize = resInfo[1];
      if (tileSize <= 0)
        throw new Exception("Invalid tile size: " + tileSize);

      if (tileSize == 1024 + tileDim * tileDim) {
        type = TisType.PALETTE;
      } else if (tileSize == 12) {
        type = TisType.PVRZ;
      } else {
        throw new Exception("TIS file type could not be determined");
      }

      dataSize = tileCount * tileSize;

      initialized = true;
    }

    private boolean empty()
    {
      return !initialized;
    }
  }
}
