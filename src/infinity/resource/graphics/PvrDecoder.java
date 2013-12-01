// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.graphics.PvrDecoder.PVRInfo.PixelFormat;
import infinity.util.DynamicArray;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;

/**
 * Decodes a PVR file. (Note: Only a few selected pixel formats are supported.)
 * @author argent77
 */
public class PvrDecoder
{
  private PVRInfo info;
  private DynamicArray inBuffer;

  /**
   * Creates an uninitialized PvrDecoder object. Use <code>open()</code> to load a PVR resource.
   */
  public PvrDecoder()
  {
    close();
  }

  /**
   * Constructor takes a buffer containing the whole PVR data.
   * @param buffer The buffer containing the whole PVR data.
   * @throws Exception
   */
  public PvrDecoder(byte[] buffer) throws Exception
  {
    open(buffer);
  }

  /**
   * Constructor takes a buffer containing the whole PVR data.
   * @param buffer The buffer containing the whole PVR data.
   * @param ofs Start offset into the buffer.
   * @throws Exception
   */
  public PvrDecoder(byte[] buffer, int ofs) throws Exception
  {
    open(buffer, ofs);
  }

  /**
   * Closes the current PVR resource.
   */
  public void close()
  {
    info = null;
    inBuffer = null;
  }

  public void open(byte[] buffer) throws Exception
  {
    open(buffer, 0);
  }

  public void open(byte[] buffer, int ofs) throws Exception
  {
    close();

    if (buffer == null)
      throw new NullPointerException();

    init(buffer, ofs);
  }

  public boolean isOpen()
  {
    return !empty();
  }

  /**
   * Decodes the currently loaded PVR data and returns the result as a new BufferedImage object.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception
   */
  public BufferedImage decode() throws Exception
  {
    if (!empty()) {
      final BufferedImage image = ColorConvert.createCompatibleImage(info().width(),
                                                                     info().height(), true);
      if (decode(image)) {
        return image;
      }
    }
    return null;
  }

  /**
   * Decodes the currently loaded PVR data and draws the result into a BufferedImage object.
   * @param image The BufferedImage object to draw the PVR texture into.
   * @return <code>true</code> if the image has been drawn successfully, <code>false</code> otherwise.
   * @throws Exception
   */
  public boolean decode(BufferedImage image) throws Exception
  {
    if (!empty()) {
      return decode(image, 0, 0, info().width(), info().height());
    } else {
      return false;
    }
  }

  /**
   * Decodes a block of pixels of the currently loaded PVR data and returns it as a new
   * BufferedImage object.
   * @param x Left-most x coordinate of the pixel block.
   * @param y Top-most y coordinate of the pixel block.
   * @param width Width in pixels.
   * @param height Height in pixels.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception
   */
  public BufferedImage decode(int x, int y, int width, int height) throws Exception
  {
    if (!empty()) {
      if (width > 0 && height > 0) {
        BufferedImage image = ColorConvert.createCompatibleImage(width, height, true);
        if (decode(image, x, y, width, height)) {
          return image;
        }
      }
    }
    return null;
  }

  /**
   * Decodes a block of pixels of the currently loaded PVR data and draws it into a BufferedImage
   * object.
   * @param image The BufferedImage object to draw the pixel data into.
   * @param x Left-most x coordinate of the pixel block.
   * @param y Top-most y coordinate of the pixel block.
   * @param width Width in pixels.
   * @param height Height in pixels.
   * @return <code>true</code> if the image has been drawn successfully, <code>false</code> otherwise.
   * @throws Exception
   */
  public boolean decode(BufferedImage image, int x, int y, int width, int height) throws Exception
  {
    if (!empty()) {
      if (x < 0 || y < 0 || width < 1 || height < 1 ||
          x + width > info().width() || (y + height > info().height()))
        throw new Exception("Invalid dimensions specified");
      if (!info().isSupported())
        throw new Exception("Pixel format '" + info().pixelFormat().toString() + "' not supported");
      if (info().channelType() != PVRInfo.ChannelType.UBYTE_NORM)
        throw new Exception("Channel type not supported");

      return decodeBlock(image, x, y, width, height);
    }
    return false;
  }

  /**
   * Returns an interface to a PVR info structure.
   * @return PVR info structure
   */
  public PVRInfo info()
  {
    if (!empty())
      return info;
    else
      return null;
  }

  private void init(byte[] buffer, int ofs) throws Exception
  {
    close();

    if (buffer == null)
      throw new NullPointerException();

    inBuffer = DynamicArray.wrap(buffer, ofs, DynamicArray.ElementType.BYTE);

    info = new PVRInfo(inBuffer);
    if (!info().isSupported()) {
      String pf = info().pixelFormat.toString();
      info = null;
      throw new Exception("Pixel format '" + pf + "' not supported");
    }
    if (buffer.length - ofs < info().headerSize() + info().dataSize()) {
      info = null;
      throw new Exception("Input buffer too small");
    }

    inBuffer.addToBaseOffset(info().headerSize());
  }

  private boolean empty()
  {
    if (inBuffer != null && info != null)
      return info.empty();
    else
      return true;
  }

  /**
   * Decodes a part of the PVR file into the specified outBuffer, starting at ofs.
   * @param outBuffer The output buffer to write to.
   * @param ofs The start offset into the output buffer.
   * @param left left-most x coordinate of the pixel block
   * @param top top-most y coordinate of the pixel block
   * @param width width of the pixel block
   * @param height height of the pixel block
   * @return true if successful, false otherwise
   */
  private boolean decodeBlock(BufferedImage image, int left, int top, int width, int height) throws Exception
  {
    if (!empty()) {
      if (image == null)
        throw new NullPointerException();
      if (left < 0 || top < 0 || width < 0 || height < 0 ||
          left + width > info().width() || top + height > info().height ||
          width > image.getWidth() || height > image.getHeight())
        return false;

      // calculating block dimensions, aligned to a multiple of 4
      int alignedLeft = ((left & 3) != 0) ? (left & ~3) : left;
      int alignedTop = ((top & 3) != 0) ? (top & ~3) : top;
      int alignedWidth = width + left - alignedLeft;
      if ((alignedWidth & 3) != 0)
        alignedWidth = (alignedWidth & ~3) + 4;
      int alignedHeight = height + top - alignedTop;
      if ((alignedHeight & 3) != 0)
        alignedHeight = (alignedHeight & ~3) + 4;
      int ofsX = left - alignedLeft;
      int ofsY = top - alignedTop;

      // decoding aligned data block and drawing relevant pixels to target image
      int inBlocksX = info().width() >>> 2;
      int alignedBlocksX = alignedWidth >>> 2;
      int alignedBlocksY = alignedHeight >>> 2;
      final PixelFormat pf = info().pixelFormat();
      int bytesPerBlock = (pf == PixelFormat.DXT1) ? 8 : 16;
      for (int y = 0; y < alignedBlocksY; y++) {
        int inOfs = (((alignedTop >>> 2) + y) * inBlocksX + (alignedLeft >>> 2)) * bytesPerBlock;
        for (int x = 0; x < alignedBlocksX; x++, inOfs+=bytesPerBlock) {
          switch (pf) {
            case DXT1:
              decodeDXT1Block(inBuffer.asByteArray().addToBaseOffset(inOfs), image,
                              (x << 2) - ofsX, (y << 2) - ofsY);
              break;
            case DXT3:
              decodeDXT3Block(inBuffer.asByteArray().addToBaseOffset(inOfs), image,
                               (x << 2) - ofsX, (y << 2) - ofsY);
              break;
            case DXT5:
              decodeDXT5Block(inBuffer.asByteArray().addToBaseOffset(inOfs), image,
                               (x << 2) - ofsX, (y << 2) - ofsY);
              break;
            default:
              break;
          }
        }
      }
      return true;
    }
    return false;
  }

  // Decodes a single logical DXT1 data block.
  private boolean decodeDXT1Block(DynamicArray buffer, BufferedImage image, int startX, int startY)
  {
    if (buffer == null || image == null)
      throw new NullPointerException();
    if (buffer.getArray().length - buffer.getBaseOffset() < 8)
      return false;
    if (startX >= image.getWidth() || startY >= image.getHeight())
      return true;

    int imgWidth = image.getWidth();
    int imgHeight = image.getHeight();

    int c0 = buffer.getUnsignedShort(0);
    int c1 = buffer.getUnsignedShort(2);
    int code = buffer.getInt(4);
    buffer.addToBaseOffset(8);
    final int[] c = new int[6];
    unpack565(c0, c, 0);
    unpack565(c1, c, 3);

    int v, col = 0;
    for (int i = 0; i < 16; i++, code>>>=2) {
      int x = startX + (i & 3);
      int y = startY + (i >>> 2);
      if (x >= 0 && x < imgWidth && y >= 0 && y < imgHeight) {
        switch (code & 3) {
          case 0:
            // 100% c0, 0% c1
            col = 0xff000000 | (c[2] << 16) | (c[1] << 8) | c[0];
            break;
          case 1:
            // 0% c0, 100% c1
            col = 0xff000000 | (c[5] << 16) | (c[4] << 8) | c[3];
            break;
          case 2:
            if (c0 > c1) {
              // 66% c0, 33% c1
              col = 0xff000000;
              v = ((c[0] << 1) + c[3]) / 3;
              col |= (v > 255) ? 255 : v;
              v = ((c[1] << 1) + c[4]) / 3;
              col |= ((v > 255) ? 255 : v) << 8;
              v = ((c[2] << 1) + c[5]) / 3;
              col |= ((v > 255) ? 255 : v) << 16;
            } else {
              // 50% c0, 50% c1
              col = 0xff000000;
              v = (c[0] + c[3]) >>> 1;
              col |= (v > 255) ? 255 : v;
              v = (c[1] + c[4]) >>> 1;
              col |= ((v > 255) ? 255 : v) << 8;
              v = (c[2] + c[5]) >>> 1;
              col |= ((v > 255) ? 255 : v) << 16;
            }
            break;
          case 3:
            if (c0 > c1) {
              // 33% c0, 66% c1
              col = 0xff000000;
              v = (c[0] + (c[3] << 1)) / 3;
              col |= (v > 255) ? 255 : v;
              v = (c[1] + (c[4] << 1)) / 3;
              col |= ((v > 255) ? 255 : v) << 8;
              v = (c[2] + (c[5] << 1)) / 3;
              col |= ((v > 255) ? 255 : v) << 16;
            } else {
              // transparent
              col = 0;
            }
            break;
        }
        image.setRGB(x, y, col);
      }
    }
    return true;
  }

  //Decodes a single logical DXT3 data block.
  private boolean decodeDXT3Block(DynamicArray buffer, BufferedImage image, int startX, int startY)
  {
    if (buffer == null || image == null)
      throw new NullPointerException();
    if (buffer.getArray().length - buffer.getBaseOffset() < 16)
      return false;
    if (startX >= image.getWidth() || startY >= image.getHeight())
      return true;

    int imgWidth = image.getWidth();
    int imgHeight = image.getHeight();

    // loading 4-bit alpha values
    long tmp = buffer.getLong(0); buffer.addToBaseOffset(8);
    int[] alpha = new int[16];
    for (int i = 0; i < 16; i++) {
      int v = (int)(tmp >>> (i * 4)) & 0x0f;
      alpha[i] = (v << 4) | v;    // making full transparency and opacity possible
    }
    // loading reference color values
    int c0 = buffer.getUnsignedShort(0);
    int c1 = buffer.getUnsignedShort(2);
    int code = buffer.getInt(4);
    buffer.addToBaseOffset(8);
    final int[] c = new int[6];
    unpack565(c0, c, 0);
    unpack565(c1, c, 3);

    int v, col = 0;
    for (int i = 0; i < 16; i++, code>>>=2) {
      int x = startX + (i & 3);
      int y = startY + (i >>> 2);
      if (x >= 0 && x < imgWidth && y >= 0 && y < imgHeight) {
        int a = alpha[i] << 24;
        switch (code & 3) {
          case 0:
            // 100% c0, 0% c1
            col = a | (c[2] << 16) | (c[1] << 8) | c[0];
            break;
          case 1:
            // 0% c0, 100% c1
            col = a | (c[5] << 16) | (c[4] << 8) | c[3];
            break;
          case 2:
            // 66% c0, 33% c1
            col = a;
            v = ((c[0] << 1) + c[3]) / 3;
            col |= (v > 255) ? 255 : v;
            v = ((c[1] << 1) + c[4]) / 3;
            col |= ((v > 255) ? 255 : v) << 8;
            v = ((c[2] << 1) + c[5]) / 3;
            col |= ((v > 255) ? 255 : v) << 16;
            break;
          case 3:
            // 33% c0, 66% c1
            col = a;
            v = (c[0] + (c[3] << 1)) / 3;
            col |= (v > 255) ? 255 : v;
            v = (c[1] + (c[4] << 1)) / 3;
            col |= ((v > 255) ? 255 : v) << 8;
            v = (c[2] + (c[5] << 1)) / 3;
            col |= ((v > 255) ? 255 : v) << 16;
            break;
        }
        image.setRGB(x, y, col);
      }
    }
    return true;
  }

  // Decodes a single logical DXT5 data block.
  private boolean decodeDXT5Block(DynamicArray buffer, BufferedImage image, int startX, int startY)
  {
    if (buffer == null || image == null)
      throw new NullPointerException();
    if (buffer.getArray().length - buffer.getBaseOffset() < 16)
      return false;
    if (startX >= image.getWidth() || startY >= image.getHeight())
      return true;

    int imgWidth = image.getWidth();
    int imgHeight = image.getHeight();

    // generating alpha table
    final int[] alpha = new int[8];
    alpha[0] = buffer.getUnsignedByte(0); buffer.addToBaseOffset(1);
    alpha[1] = buffer.getUnsignedByte(0); buffer.addToBaseOffset(1);
    if (alpha[0] > alpha[1]) {
      alpha[2] = (6 * alpha[0] + 1 * alpha[1]) / 7;
      alpha[3] = (5 * alpha[0] + 2 * alpha[1]) / 7;
      alpha[4] = (4 * alpha[0] + 3 * alpha[1]) / 7;
      alpha[5] = (3 * alpha[0] + 4 * alpha[1]) / 7;
      alpha[6] = (2 * alpha[0] + 5 * alpha[1]) / 7;
      alpha[7] = (1 * alpha[0] + 6 * alpha[1]) / 7;
    } else {
      alpha[2] = (4 * alpha[0] + 1 * alpha[1]) / 5;
      alpha[3] = (3 * alpha[0] + 2 * alpha[1]) / 5;
      alpha[4] = (2 * alpha[0] + 3 * alpha[1]) / 5;
      alpha[5] = (1 * alpha[0] + 4 * alpha[1]) / 5;
      alpha[6] = 0;
      alpha[7] = 255;
    }
    // loading alpha table indices
    long tmp = buffer.getLong(0) & 0xffffffffffffL; buffer.addToBaseOffset(6);
    int[] tableIdx = new int[16];
    for (int i = 0; i < 16; i++) {
      tableIdx[i] = (int)((tmp >>> (i*3)) & 7);
    }
    // loading reference color values
    int c0 = buffer.getUnsignedShort(0);
    int c1 = buffer.getUnsignedShort(2);
    int code = buffer.getInt(4);
    buffer.addToBaseOffset(8);
    final int[] c = new int[6];
    unpack565(c0, c, 0);
    unpack565(c1, c, 3);

    int v, col = 0;
    for (int i = 0; i < 16; i++, code>>>=2) {
      int x = startX + (i & 3);
      int y = startY + (i >>> 2);
      if (x >= 0 && x < imgWidth && y >= 0 && y < imgHeight) {
        int a = alpha[tableIdx[i]] << 24;
        switch (code & 3) {
          case 0:
            // 100% c0, 0% c1
            col = a | (c[2] << 16) | (c[1] << 8) | c[0];
            break;
          case 1:
            // 0% c0, 100% c1
            col = a | (c[5] << 16) | (c[4] << 8) | c[3];
            break;
          case 2:
            // 66% c0, 33% c1
            col = a;
            v = ((c[0] << 1) + c[3]) / 3;
            col |= (v > 255) ? 255 : v;
            v = ((c[1] << 1) + c[4]) / 3;
            col |= ((v > 255) ? 255 : v) << 8;
            v = ((c[2] << 1) + c[5]) / 3;
            col |= ((v > 255) ? 255 : v) << 16;
            break;
          case 3:
            // 33% c0, 66% c1
            col = a;
            v = (c[0] + (c[3] << 1)) / 3;
            col |= (v > 255) ? 255 : v;
            v = (c[1] + (c[4] << 1)) / 3;
            col |= ((v > 255) ? 255 : v) << 8;
            v = (c[2] + (c[5] << 1)) / 3;
            col |= ((v > 255) ? 255 : v) << 16;
            break;
        }
        image.setRGB(x, y, col);
      }
    }
    return true;
  }

  // Converts RGB565 into 8 bit color components, ordered { B, G, R }
  private static int[] unpack565(int color, int[] components, int ofs)
  {
    components[ofs] = ((color << 3) & 0xf8) | (color >>> 2) & 0x07;       // b
    components[ofs+1] = ((color >>> 3) & 0xfc) | (color >>> 9) & 0x03;    // g
    components[ofs+2] = ((color >>> 8) & 0xf8) | (color >>> 13) & 0x07;   // r
    return components;
  }


  // ----------------------------- INNER CLASSES -----------------------------

  /**
   * Manages header information for PVR files.
   */
  public static class PVRInfo
  {
    /**
     * Flags indicate special properties of the color data
     */
    public enum Flags { NONE, PRE_MULTIPLIED }

    /**
     * Format specifies the pixel format of the color data.
     */
    public enum PixelFormat {
      PVRTC_2BPP_RGB, PVRTC_2BPP_RGBA,
      PVRTC_4BPP_RGB, PVRTC_4BPP_RGBA,
      PVRTC2_2BPP, PVRTC2_4BPP,
      ETC1,
      DXT1, DXT2, DXT3, DXT4, DXT5, BC4, BC5, BC6, BC7,
      UYVY, YUY2,
      BW1BPP, R9G9B9E5, RGBG8888, GRGB8888,
      ETC2_RGB, ETC2_RGBA, ETC2_RGB_A1,
      EAC_R11_RGB_U, EAC_R11_RGB_S, EAC_RG11_RGB_U, EAC_RG11_RGB_S,
      CUSTOM
    }

    /**
     * Color space of the color data.
     */
    public enum ColorSpace { RGB, SRGB }

    /**
     * Datatype used to describe a color component.
     */
    public enum ChannelType {
      UBYTE_NORM, SBYTE_NORM, UBYTE, SBYTE,
      USHORT_NORM, SSHORT_NORM, USHORT, SSHORT,
      UINT_NORM, SINT_NORM, UINT, SINT,
      FLOAT
    }

    // Supported pixel formats
    private static final EnumSet<PixelFormat> SupportedFormat =
        EnumSet.of(PixelFormat.DXT1, PixelFormat.DXT3, PixelFormat.DXT5);

    private int signature;
    private Flags flags;
    private PixelFormat pixelFormat;
    private byte[] pixelFormatEx;
    private ColorSpace colorSpace;
    private ChannelType channelType;
    private int height;
    private int width;
    private int colorDepth;           // the color depth of a single decoded pixel (without decompression-specific artefacts)
    private int bitsPerInputPixel;    // average bits/pixel for encoded pixel data
    private int textureDepth;         // NOT bits per pixel!
    private int numSurfaces;
    private int numFaces;
    private int numMipMaps;
    private int metaSize;
    private byte[] metaData;
    private int headerSize;           // size of the header incl. meta data
    private boolean initialized;


    public PVRInfo(DynamicArray buffer) throws Exception
    {
      if (buffer == null)
        throw new NullPointerException();

      init(buffer);
    }

    /**
     * Returns flags that indicate special properties of the color data.
     * @return Flags as enum.
     */
    public Flags flags()
    {
      if (!empty())
        return flags;
      else
        return null;
    }

    /**
     * Returns the pixel format used to encode image data within the PVR file.
     * Use pixelFormatEx() if Format.CUSTOM is returned.
     * @return Pixel format as enum.
     * @see pixelFormatEx
     */
    public PixelFormat pixelFormat()
    {
      if (!empty())
        return pixelFormat;
      else
        return null;
    }

    /**
     * Returns meaningful data only if pixelFormat() returns Format.CUSTOM.
     * @return A custom pixel format, not covered pixelFormat().
     * @see pixelFormat
     */
    public byte[] pixelFormatEx()
    {
      if (!empty())
        return pixelFormatEx;
      else
        return null;
    }

    /**
     * Returns the color space the image data is in.
     * @return Color space as enum.
     */
    public ColorSpace colorSpace()
    {
      if (!empty())
        return colorSpace;
      else
        return null;
    }

    /**
     * Returns the data type used to encode the image data within the PVR file.
     * @return Data type as enum.
     */
    public ChannelType channelType()
    {
      if (!empty())
        return channelType;
      else
        return null;
    }

    /**
     * Returns width in pixels of the stored image.
     * @return Width in pixels.
     */
    public int width()
    {
      if (!empty())
        return width;
      else
        return 0;
    }

    /**
     * Returns height in pixel of the stored image.
     * @return Height in pixels.
     */
    public int height()
    {
      if (!empty())
        return height;
      else
        return 0;
    }

    /**
     * Returns the color depth of the pixel type used to encode the color data in bits/pixel.
     * @return Color depth of the input data in bits/pixel.
     */
    public int bpp()
    {
      if (!empty())
        return colorDepth;
      else
        return 0;
    }

    /**
     * Returns the depth of the texture stored in the image data, in pixels.
     * @return Texture depth in pixels, stored in the image data.
     */
    public int textureDepth()
    {
      if (!empty())
        return textureDepth;
      else
        return 0;
    }

    /**
     * Returns the number of surfaces within the texture array.
     * @return Number of surfaces.
     */
    public int surfaceCount()
    {
      if (!empty())
        return numSurfaces;
      else
        return 0;
    }

    /**
     * Returns the number of faces in a cube map.
     * @return Number of faces.
     */
    public int faceCount()
    {
      if (!empty())
        return numFaces;
      else
        return 0;
    }

    /**
     * Returns the number of MIP-Map levels present including the top level. ()
     * @return
     */
    public int mipMapCount()
    {
      if (!empty())
        return numMipMaps;
      else
        return 0;
    }

    /**
     * Returns the total size of meta data embedded in the PVR header.
     * @return Size in bytes.
     */
    public int metaSize()
    {
      if (!empty())
        return metaSize;
      else
        return 0;
    }

    /**
     * Returns the content of the meta data, embedded in the PVR header. Can be empty (size = 0).
     * @return Byte array containing meta data.
     */
    public byte[] metaData()
    {
      if (!empty())
        return metaData;
      else
        return null;
    }

    /**
     * Returns the average number of bits used for each encoded pixel.
     * @return Bits per pixel.
     */
    public int bitsPerPixel()
    {
      if (!empty())
        return bitsPerInputPixel;
      else
        return 0;
    }

    /**
     * Returns whether the pixel format is supported by the PvrDecoder class.
     * @return
     */
    public boolean isSupported()
    {
      try {
        return SupportedFormat.contains(pixelFormat());
      } catch (Exception e) {
      }
      return false;
    }

    private void init(DynamicArray buffer) throws Exception
    {
      if (buffer == null)
        throw new NullPointerException();
      if (buffer.getArray().length - buffer.getBaseOffset() < 0x34)
        throw new Exception("Input buffer too small");

      signature = buffer.getInt(0);
      if (signature != 0x03525650)
        throw new Exception("No PVR signature found");

      int v = buffer.getInt(4);
      switch (v) {
        case 0: flags = Flags.NONE; break;
        case 1: flags = Flags.PRE_MULTIPLIED; break;
        default: throw new Exception("Unsupported PVR flags: " + Integer.toString(v));
      }

      long l = buffer.getLong(8);
      if ((l & 0xffffffff00000000L) != 0L) {
        // custom pixel format?
        pixelFormat = PixelFormat.CUSTOM;
        pixelFormatEx = new byte[8];
        ByteBuffer bb = ByteBuffer.wrap(pixelFormatEx).order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(l);
      } else {
        // predefined pixel format?
        switch ((int)l) {
          case  0: pixelFormat = PixelFormat.PVRTC_2BPP_RGB; break;
          case  1: pixelFormat = PixelFormat.PVRTC_2BPP_RGBA; break;
          case  2: pixelFormat = PixelFormat.PVRTC_4BPP_RGB; break;
          case  3: pixelFormat = PixelFormat.PVRTC_4BPP_RGBA; break;
          case  4: pixelFormat = PixelFormat.PVRTC2_2BPP; break;
          case  5: pixelFormat = PixelFormat.PVRTC2_4BPP; break;
          case  6: pixelFormat = PixelFormat.ETC1; break;
          case  7: pixelFormat = PixelFormat.DXT1; bitsPerInputPixel = 4; break;
          case  8: pixelFormat = PixelFormat.DXT2; bitsPerInputPixel = 8; break;
          case  9: pixelFormat = PixelFormat.DXT3; bitsPerInputPixel = 8; break;
          case 10: pixelFormat = PixelFormat.DXT4; bitsPerInputPixel = 8; break;
          case 11: pixelFormat = PixelFormat.DXT5; bitsPerInputPixel = 8; break;
          case 12: pixelFormat = PixelFormat.BC4; break;
          case 13: pixelFormat = PixelFormat.BC5; break;
          case 14: pixelFormat = PixelFormat.BC6; break;
          case 15: pixelFormat = PixelFormat.BC7; break;
          case 16: pixelFormat = PixelFormat.UYVY; break;
          case 17: pixelFormat = PixelFormat.YUY2; break;
          case 18: pixelFormat = PixelFormat.BW1BPP; break;
          case 19: pixelFormat = PixelFormat.R9G9B9E5; break;
          case 20: pixelFormat = PixelFormat.RGBG8888; break;
          case 21: pixelFormat = PixelFormat.GRGB8888; break;
          case 22: pixelFormat = PixelFormat.ETC2_RGB; break;
          case 23: pixelFormat = PixelFormat.ETC2_RGBA; break;
          case 24: pixelFormat = PixelFormat.ETC2_RGB_A1; break;
          case 25: pixelFormat = PixelFormat.EAC_R11_RGB_U; break;
          case 26: pixelFormat = PixelFormat.EAC_R11_RGB_S; break;
          case 27: pixelFormat = PixelFormat.EAC_RG11_RGB_U; break;
          case 28: pixelFormat = PixelFormat.EAC_RG11_RGB_S; break;
          default: throw new Exception("Unsupported pixel format: " + Integer.toString((int)l));
        }
        pixelFormatEx = new byte[0];
      }

      v = buffer.getInt(16);
      switch (v) {
        case 0: colorSpace = ColorSpace.RGB; break;
        case 1: colorSpace = ColorSpace.SRGB; break;
        default: throw new Exception("Unsupported color space: " + Integer.toString(v));
      }

      v = buffer.getInt(20);
      switch (v) {
        case  0: channelType = ChannelType.UBYTE_NORM; break;
        case  1: channelType = ChannelType.SBYTE_NORM; break;
        case  2: channelType = ChannelType.UBYTE; break;
        case  3: channelType = ChannelType.SBYTE; break;
        case  4: channelType = ChannelType.USHORT_NORM; break;
        case  5: channelType = ChannelType.SSHORT_NORM; break;
        case  6: channelType = ChannelType.USHORT; break;
        case  7: channelType = ChannelType.SSHORT; break;
        case  8: channelType = ChannelType.UINT_NORM; break;
        case  9: channelType = ChannelType.SINT_NORM; break;
        case 10: channelType = ChannelType.UINT; break;
        case 11: channelType = ChannelType.SINT; break;
        case 12: channelType = ChannelType.FLOAT; break;
        default: throw new Exception("Unsupported channel type: " + Integer.toString(v));
      }

      height = buffer.getInt(24);
      width = buffer.getInt(28);
      colorDepth = getColorDepth();
      textureDepth = buffer.getInt(32);
      numSurfaces = buffer.getInt(36);
      numFaces = buffer.getInt(40);
      numMipMaps = buffer.getInt(44);
      metaSize = buffer.getInt(48);
      if (metaSize > 0) {
        if (buffer.size() - buffer.getBaseOffset() < 0x34 + metaSize)
          throw new Exception("Input buffer too small");
        metaData = buffer.get(52, metaSize);
      } else
        metaData = new byte[0];

      headerSize = 0x34 + metaSize;

      initialized = true;
    }

    /**
     * Attempts to determine the color depth of the pixel type used to encode the color data.
     * @return Color depth in bits/pixel.
     */
    private int getColorDepth()
    {
      switch (pixelFormat) {
        case DXT1:
        case DXT2:
        case DXT3:
        case DXT4:
        case DXT5:
          return 16;
        default:
          return 32;    // most likely wrong, but not important for us anyway.
      }
    }

    private boolean empty()
    {
      return !initialized;
    }

    // PVR header size in bytes
    private int headerSize()
    {
      if (!empty())
        return headerSize;
      else
        return 0;
    }

    // Compressed image size in bytes
    private int dataSize()
    {
      if (!empty())
        return width*height*bitsPerInputPixel/8;
      else
        return 0;
    }
  }
}
