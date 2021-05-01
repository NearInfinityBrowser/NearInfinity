// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information
// ----------------------------------------------------------------
// PVRT format specifications and reference implementation:
// Copyright (c) Imagination Technologies Ltd. All Rights Reserved

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

/**
 * Decodes a PVR(Z) file.
 * Note: Supports only the minimal set of PVR-specific features required to decode the BGEE's
 * PVRZ resources (this includes only a selected number of supported pixel formats).
 */
public class PvrDecoder
{
  /** Flags indicates special properties of the color data. */
  public enum Flags { NONE, PRE_MULTIPLIED }

  /** Format specifies the pixel format of the color data. */
  public enum PixelFormat {
    PVRTC_2BPP_RGB, PVRTC_2BPP_RGBA, PVRTC_4BPP_RGB, PVRTC_4BPP_RGBA,
    PVRTC2_2BPP, PVRTC2_4BPP,
    ETC1,
    DXT1, DXT2, DXT3, DXT4, DXT5, BC4, BC5, BC6, BC7,
    UYVY, YUY2,
    BW1BPP, R9G9B9E5, RGBG8888, GRGB8888,
    ETC2_RGB, ETC2_RGBA, ETC2_RGB_A1,
    EAC_R11_RGB_U, EAC_R11_RGB_S, EAC_RG11_RGB_U, EAC_RG11_RGB_S,
    CUSTOM
  }

  /** Color space of the color data. */
  public enum ColorSpace { RGB, SRGB }

  /** Datatype used to describe a color component. */
  public enum ChannelType {
    UBYTE_NORM, SBYTE_NORM, UBYTE, SBYTE,
    USHORT_NORM, SSHORT_NORM, USHORT, SSHORT,
    UINT_NORM, SINT_NORM, UINT, SINT,
    FLOAT
  }

  // The global cache list for PVR objects. The "key" has to be a unique String (e.g. filename or integer as string)
  private static final Map<String, PvrDecoder> pvrCache = new LinkedHashMap<>();
  // The max. number of cache entries to hold
  private static int MaxCacheEntries = 32;

  // Supported pixel formats
  private static final EnumSet<PixelFormat> SupportedFormat =
      EnumSet.of(PixelFormat.DXT1, PixelFormat.DXT3, PixelFormat.DXT5,
                 PixelFormat.PVRTC_2BPP_RGB, PixelFormat.PVRTC_2BPP_RGBA,
                 PixelFormat.PVRTC_4BPP_RGB, PixelFormat.PVRTC_4BPP_RGBA);

  private PvrInfo info;


  /**
   * Returns an initialized PvrDecoder object with the specified resource (if available).
   * @param entry The ResourceEntry object of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(ResourceEntry entry)
  {
    if (entry == null) {
      throw new NullPointerException();
    }
    try (InputStream is = entry.getResourceDataAsStream()) {
      String key = null;
      if (entry instanceof FileResourceEntry) {
        key = ((FileResourceEntry)entry).getActualPath().toString();
      } else {
        key = entry.getResourceName();
      }
      PvrDecoder decoder = createPvrDecoder(key, is);
      if (decoder != null) {
        return decoder;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns an initialized PvrDecoder object with the specified file (if available).
   * @param fileName The filename of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(String fileName)
  {
    if (fileName == null) {
      throw new NullPointerException();
    }
    try (InputStream is = StreamUtils.getInputStream(FileManager.resolve(fileName))) {
      String key = fileName;
      PvrDecoder decoder = createPvrDecoder(key, is);
      if (decoder != null) {
        return decoder;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns an initialized PvrDecoder object with the specified file (if available).
   * @param file The file object of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(Path file)
  {
    try (InputStream is = StreamUtils.getInputStream(file)) {
      String key = file.getFileName().toString();
      PvrDecoder decoder = createPvrDecoder(key, is);
      if (decoder != null) {
        return decoder;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns an initialized PvrDecoder object with the specified input stream (if available).
   * @param input The input stream of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(InputStream input)
  {
    if (input == null) {
      throw new NullPointerException();
    }
    try {
      String key = Integer.valueOf(input.hashCode()).toString();
      PvrDecoder decoder = createPvrDecoder(key, input);
      if (decoder != null) {
        return decoder;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /** Returns the max. number of PvrDecoder objects to cache. */
  public static int getMaxCacheEntries()
  {
    return MaxCacheEntries;
  }

  /** Specify the new max. number of PvrDecoder objects to cache. Specifying 0 disables the cache. */
  public static synchronized void setMaxCacheEntries(int maxValue)
  {
    if (maxValue < 0) maxValue = 0; else if (maxValue > 65535) maxValue = 65535;
    if (maxValue != MaxCacheEntries) {
      MaxCacheEntries = maxValue;
      while (pvrCache.size() > MaxCacheEntries) {
        pvrCache.remove(pvrCache.keySet().iterator().next());
      }
    }
  }

  /** Clears all available caches. */
  public static synchronized void flushCache()
  {
    pvrCache.clear();
    DecodePVRT.flushCache();
  }

  /** Returns the current cache load as percentage value. */
  public static int getCacheLoad()
  {
    if (MaxCacheEntries > 0) {
      return (pvrCache.size()*100) / MaxCacheEntries;
    } else {
      return 0;
    }
  }

//  // Returns a PvrDecoder object only if it already exists in the cache.
//  private static synchronized PvrDecoder getCachedPvrDecoder(String key)
//  {
//    PvrDecoder retVal = null;
//    if (key != null && !key.isEmpty()) {
//      key = key.toUpperCase(Locale.ENGLISH);
//      if (pvrCache.containsKey(key)) {
//        retVal = pvrCache.get(key);
//        // re-inserting entry to prevent premature removal from cache
//        pvrCache.remove(key);
//        pvrCache.put(key, retVal);
//      }
//    }
//    return retVal;
//  }

  // Returns a PvrDecoder object of the specified key if available, or creates and returns a new one otherwise.
  private static synchronized PvrDecoder createPvrDecoder(String key, InputStream input)
  {
    PvrDecoder retVal = null;
    if (key != null && !key.isEmpty()) {
      key = key.toUpperCase(Locale.ENGLISH);
      if (pvrCache.containsKey(key)) {
        retVal = pvrCache.get(key);
        // re-inserting entry to prevent premature removal from cache
        pvrCache.remove(key);
        pvrCache.put(key, retVal);
      } else {
        try {
          retVal = new PvrDecoder(input);
          if (retVal != null) {
            pvrCache.put(key, retVal);
            // removing excess cache entries
            while (pvrCache.size() > MaxCacheEntries) {
              pvrCache.remove(pvrCache.keySet().iterator().next());
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
//    PvrDecoder retVal = getCachedPvrDecoder(key);
//    if (retVal == null && input != null) {
//      try {
//        retVal = new PvrDecoder(input);
//        if (retVal != null) {
//          pvrCache.put(key, retVal);
//          // removing excess cache entries
//          while (pvrCache.size() > MaxCacheEntries) {
//            pvrCache.remove(pvrCache.keySet().iterator().next());
//          }
//        }
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
//    return retVal;
  }

  // Returns a rectangle that is aligned to the values specified as arguments 2 and 3
  private static Rectangle alignRectangle(Rectangle rect, int alignX, int alignY)
  {
    if (rect == null) return null;

    Rectangle retVal = new Rectangle(rect);
    if (alignX < 1) alignX = 1;
    if (alignY < 1) alignY = 1;
    if (rect.x < 0) { rect.width -= -rect.x; rect.x = 0; }
    if (rect.y < 0) { rect.height -= -rect.y; rect.y = 0; }

    int diffX = retVal.x % alignX;
    if (diffX != 0) {
      retVal.x -= diffX;
      retVal.width += diffX;
    }
    int diffY = retVal.y % alignY;
    if (diffY != 0) {
      retVal.y -= diffY;
      retVal.height += diffY;
    }

    diffX = (alignX - (retVal.width % alignX)) % alignX;
    retVal.width += diffX;

    diffY = (alignY - (retVal.height % alignY)) % alignY;
    retVal.height += diffY;

    return retVal;
  }


  /** Returns flags that indicate special properties of the color data. */
  public Flags getFlags() { return info.flags; }

  /** Returns the pixel format used to encode image data within the PVR file. */
  public PixelFormat getPixelFormat() { return info.pixelFormat; }

  /** Returns meaningful data only if pixelFormat() returns {@code PixelFormat.CUSTOM}. */
  public byte[] getPixelFormatEx() { return info.pixelFormatEx; }

  /** Returns the color space the image data is in. */
  public ColorSpace getColorSpace() { return info.colorSpace; }

  /** Returns the data type used to encode the image data within the PVR file. */
  public ChannelType getChannelType() { return info.channelType; }

  /** Returns the texture width in pixels. */
  public int getWidth() { return info.width; }

  /** Returns the texture height in pixels. */
  public int getHeight() { return info.height; }

  /** Returns the color depth of the pixel type used to encode the color data in bits/pixel. */
  public int getColorDepth() { return info.colorDepth; }

  /** Returns the average number of bits used for each input pixel. */
  public int getAverageBitsPerPixel() { return info.bitsPerInputPixel; }

  /** Returns the depth of the texture stored in the image data, in pixels. */
  public int getTextureDepth() { return info.textureDepth; }

  /** Returns the number of surfaces within the texture array. */
  public int getNumSurfaces() { return info.numSurfaces; }

  /** Returns the number of faces in a cube map. */
  public int getNumFaces() { return info.numFaces; }

  /** Returns the number of MIP-Map levels present including the top level. */
  public int getNumMipMaps() { return info.numMipMaps; }

  /** Returns the total size of meta data embedded in the PVR header. */
  public int getMetaSize() { return info.metaSize; }

  /** Provides access to the content of the meta data, embedded in the PVR header. Can be empty (size = 0). */
  public byte[] getMetaData() { return info.metaData; }

  /** Provides direct access to the content of the encoded pixel data. */
  public byte[] getData() { return info.data; }

  /** Returns whether the pixel format of the current texture is supported by the PvrDecoder. */
  public boolean isSupported()
  {
    try {
      return SupportedFormat.contains(getPixelFormat());
    } catch (Throwable t) {
    }
    return false;
  }


  /**
   * Decodes the currently loaded PVR data and returns the result as a new BufferedImage object.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception on error.
   */
  public BufferedImage decode() throws Exception
  {
    return decode(0, 0, getWidth(), getHeight());
  }

  /**
   * Decodes the currently loaded PVR data and draws the result into a BufferedImage object.
   * @param image The BufferedImage object to draw the PVR texture into.
   * @return {@code true} if the image has been drawn successfully, {@code false} otherwise.
   * @throws Exception on error.
   */
  public boolean decode(BufferedImage image) throws Exception
  {
    return decode(image, 0, 0, getWidth(), getHeight());
  }

  /**
   * Decodes a rectangular block of pixels of the currently loaded PVR data and returns it as a new
   * BufferedImage object.
   * @param x Left-most x coordinate of the pixel block.
   * @param y Top-most y coordinate of the pixel block.
   * @param width Width in pixels.
   * @param height Height in pixels.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception on error.
   */
  public BufferedImage decode(int x, int y, int width, int height) throws Exception
  {
    if (width < 1) width = 1;
    if (height < 1) height = 1;
    int imgType = (getFlags() == Flags.PRE_MULTIPLIED) ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_ARGB;
    BufferedImage image = new BufferedImage(width, height, imgType);
    if (decode(image, x, y, width, height)) {
      return image;
    } else {
      image = null;
    }
    return null;
  }

  /**
   * Decodes a rectangular block of pixels of the currently loaded PVR data and draws it into a
   * BufferedImage object.
   * @param image The BufferedImage object to draw the pixel data into.
   * @param x Left-most x coordinate of the pixel block.
   * @param y Top-most y coordinate of the pixel block.
   * @param width Width in pixels.
   * @param height Height in pixels.
   * @return {@code true} if the image has been drawn successfully, {@code false} otherwise.
   * @throws Exception
   */
  public boolean decode(BufferedImage image, int x, int y, int width, int height) throws Exception
  {
    if (image == null) {
      throw new Exception("No target image specified");
    }
    if (x < 0 || y < 0 || width < 1 || height < 1 || x+width > getWidth() || y+height > getHeight()) {
      throw new Exception("Invalid dimensions specified");
    }
    if (!isSupported()) {
      throw new Exception(String.format("Pixel format '%s' not supported", getPixelFormat().toString()));
    }
    if (getChannelType() != ChannelType.UBYTE_NORM) {
      throw new Exception(String.format("Channel type '%s' not supported", getChannelType().toString()));
    }
    Rectangle region = new Rectangle(x, y, width, height);
    switch (getPixelFormat()) {
      case PVRTC_2BPP_RGB:
      case PVRTC_2BPP_RGBA:
        return DecodePVRT.decodePVRT2bpp(info, image, region);
      case PVRTC_4BPP_RGB:
      case PVRTC_4BPP_RGBA:
        return DecodePVRT.decodePVRT4bpp(info, image, region);
      case DXT1:
        return DecodeDXT.decodeDXT1(info, image, region);
      case DXT3:
        return DecodeDXT.decodeDXT3(info, image, region);
      case DXT5:
        return DecodeDXT.decodeDXT5(info, image, region);
      default:
        return DecodeDummy.decode(info, image, region);
    }
  }


  private PvrDecoder(InputStream input) throws Exception
  {
    if (input == null) {
      throw new NullPointerException();
    }

    try {
      // determine whether input contains PVR or PVRZ data
      if (!input.markSupported()) {
        throw new Exception("Unsupported stream operations: mark/reset");
      }
      input.mark(8);
      byte[] buf = new byte[4];
      if (input.read(buf) < 4) {
        throw new Exception("Error reading input data");
      }
      boolean isPvr = (buf[0] == (byte)0x50 && buf[1] == (byte)0x56 && buf[2] == (byte)0x52 && buf[3] == (byte)0x03);
      input.reset();

      final int headerSize = 128;
      int size = 0;
      byte[] buffer = null;
      if (!isPvr) {
        // wrapping input stream
        byte[] sizeBuf = new byte[4];
        input.read(sizeBuf);
        size = DynamicArray.getInt(sizeBuf, 0);
        buffer = new byte[size];
        input = new InflaterInputStream(input);
      }
      // read data into buffer
      if (buffer == null) {
        size = 1 << 12;   // starting with size = 4096 bytes
        buffer = new byte[size+headerSize];
      }
      int ofs = 0, len;
      while (true) {
        do {
          len = input.read(buffer, ofs, buffer.length - ofs);
          if (len > 0) {
            ofs += len;
          }
        } while (len > 0 && ofs < buffer.length);
        if (len < 0 || ofs >= buffer.length) break;
        size <<= 1;
        byte[] tmp = new byte[size+headerSize];
        System.arraycopy(buffer, 0, tmp, 0, buffer.length);
        buffer = tmp;
        ofs += len;
      }
      info = new PvrInfo(buffer, ofs);
      buffer = null;
    } finally {
      input.close();
    }
  }

// ----------------------------- INNER CLASSES -----------------------------

  // Contains preprocessed data of a single PVR resource
  private class PvrInfo
  {
    public int signature;             // the "PVR\u0003" signature
    public Flags flags;
    public PixelFormat pixelFormat;
    public byte[] pixelFormatEx;
    public ColorSpace colorSpace;
    public ChannelType channelType;
    public int height;                // texture height in pixels
    public int width;                 // texture width in pixels
    public int colorDepth;            // the color depth of a single decoded pixel (without decompression-specific artefacts)
    public int bitsPerInputPixel;     // average bits/pixel for encoded pixel data
    public int textureDepth;          // NOT bits per pixel!
    public int numSurfaces;
    public int numFaces;
    public int numMipMaps;
    public int metaSize;              // metadata block size
    public byte[] metaData;           // optional metadata
    public int headerSize;            // size of the header incl. meta data
    public byte[] data;               // the encoded pixel data

    public PvrInfo(byte[] buffer, int size) throws Exception
    {
      init(buffer, size);
    }


    private void init(byte[] buffer, int size) throws Exception
    {
      if (buffer == null || size <= 0x34) {
        throw new Exception("Invalid or incomplete PVR input data");
      }

      signature = DynamicArray.getInt(buffer, 0);
      if (signature != 0x03525650) {
        throw new Exception("No PVR signature found");
      }

      int v = DynamicArray.getInt(buffer, 4);
      switch (v) {
        case 0: flags = Flags.NONE; break;
        case 1: flags = Flags.PRE_MULTIPLIED; break;
        default: throw new Exception(String.format("Unsupported PVR flags: %d", v));
      }

      long l = DynamicArray.getLong(buffer, 8);
      if ((l & 0xffffffff00000000L) != 0L) {
        // custom pixel format
        pixelFormat = PixelFormat.CUSTOM;
        pixelFormatEx = new byte[8];
        System.arraycopy(buffer, 8, pixelFormatEx, 0, 8);
      } else {
        // predefined pixel format
        switch ((int)l) {
          case  0: pixelFormat = PixelFormat.PVRTC_2BPP_RGB; bitsPerInputPixel = 2; break;
          case  1: pixelFormat = PixelFormat.PVRTC_2BPP_RGBA; bitsPerInputPixel = 2; break;
          case  2: pixelFormat = PixelFormat.PVRTC_4BPP_RGB; bitsPerInputPixel = 4; break;
          case  3: pixelFormat = PixelFormat.PVRTC_4BPP_RGBA; bitsPerInputPixel = 4; break;
          case  4: pixelFormat = PixelFormat.PVRTC2_2BPP; bitsPerInputPixel = 2; break;
          case  5: pixelFormat = PixelFormat.PVRTC2_4BPP; bitsPerInputPixel = 4; break;
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
          default: throw new Exception(String.format("Unsupported pixel format: %s", Integer.toString((int)l)));
        }
        pixelFormatEx = new byte[0];
      }

      v = DynamicArray.getInt(buffer, 16);
      switch (v) {
        case 0: colorSpace = ColorSpace.RGB; break;
        case 1: colorSpace = ColorSpace.SRGB; break;
        default: throw new Exception(String.format("Unsupported color space: %d", v));
      }

      v = DynamicArray.getInt(buffer, 20);
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
        default: throw new Exception(String.format("Unsupported channel type: %d", v));
      }

      switch (pixelFormat) {
        case PVRTC_2BPP_RGB:
        case PVRTC_2BPP_RGBA:
        case PVRTC_4BPP_RGB:
        case PVRTC_4BPP_RGBA:
        case DXT1:
        case DXT2:
        case DXT3:
        case DXT4:
        case DXT5:
          colorDepth = 16;
          break;
        default:
          colorDepth = 32;    // most likely wrong, but not important for us anyway
      }

      height = DynamicArray.getInt(buffer, 24);
      width = DynamicArray.getInt(buffer, 28);
      textureDepth = DynamicArray.getInt(buffer, 32);
      numSurfaces = DynamicArray.getInt(buffer, 36);
      numFaces = DynamicArray.getInt(buffer, 40);
      numMipMaps = DynamicArray.getInt(buffer, 44);
      metaSize = DynamicArray.getInt(buffer, 48);
      if (metaSize > 0) {
        if (metaSize + 0x34 > size) {
          throw new Exception("Input buffer too small");
        }
        metaData = new byte[metaSize];
        System.arraycopy(buffer, 52, metaData, 0, metaSize);
      } else {
        metaData = new byte[0];
      }
      headerSize = 0x34 + metaSize;

      // storing pixel data
      data = new byte[size - headerSize];
      System.arraycopy(buffer, headerSize, data, 0, data.length);
    }
  }


  // "Decodes" unsupported pixel data.
  private static class DecodeDummy
  {
    /**
     * Decodes PVR data in "unknown" format and draws the specified "region" into "image".
     * @param pvr The PVR data
     * @param image The output image
     * @param region The of the PVR texture region to draw onto "image"
     * @return The success state of the operation.
     * @throws Exception on error.
     */
    public static boolean decode(PvrInfo pvr, BufferedImage image, Rectangle region) throws Exception
    {
      if (pvr == null || image == null || region == null) {
        return false;
      }

      int[] imgData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      int ofs = 0;
      int maxX = (image.getWidth() < region.width) ? image.getWidth() : region.width;
      int maxY = (image.getHeight() < region.height) ? image.getHeight() : region.height;
      for (int y = 0; y < maxY; y++) {
        for (int x = 0; x < maxX; x++) {
          imgData[ofs+x] = 0;
        }
        ofs += image.getWidth();
      }
      imgData = null;

      return true;
    }
  }


  // Decodes DXTn pixel data.
  private static class DecodeDXT
  {
    /**
     * Decodes PVR data in DXT1 format and draws the specified "region" into "image".
     * @param pvr The PVR data
     * @param image The output image
     * @param region The of the PVR texture region to draw onto "image"
     * @return The success state of the operation.
     * @throws Exception on error.
     */
    public static boolean decodeDXT1(PvrInfo pvr, BufferedImage image, Rectangle region) throws Exception
    {
      if (pvr == null || image == null || region == null) {
        return false;
      }

      int imgWidth = image.getWidth();
      int imgHeight = image.getHeight();
      int[] imgData = null;

      // checking region bounds and alignment
      if (region.x < 0) {region.width += -region.x; region.x = 0; }
      if (region.y < 0) {region.height += -region.y; region.y = 0; }
      if (region.x + region.width > pvr.width) region.width = pvr.width - region.x;
      if (region.y + region.height > pvr.height) region.height = pvr.height - region.y;
      Rectangle rect = alignRectangle(region, 4, 4);

      // preparing aligned image buffer for faster rendering
      BufferedImage alignedImage = null;
      int imgWidthAligned;
      if (!region.equals(rect)) {
        alignedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        imgWidthAligned = alignedImage.getWidth();
        imgData = ((DataBufferInt)alignedImage.getRaster().getDataBuffer()).getData();
        // translating "region" to be relative to "rect"
        region.x -= rect.x;
        region.y -= rect.y;
        if (imgWidth < region.width) {
          region.width= imgWidth;
        }
        if (imgHeight < region.height) {
          region.height = imgHeight;
        }
      } else {
        imgWidthAligned = imgWidth;
        imgData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      }

      final int wordSize = 8;                   // data size of an encoded 4x4 pixel block
      int wordImageWidth = pvr.width >>> 2;     // the image width in data blocks
      int wordRectWidth = rect.width >>> 2;     // the aligned region's width in data blocks
      int wordRectHeight = rect.height >>> 2;   // the aligned region's height in data blocks
      int wordPosX = rect.x >>> 2;
      int wordPosY = rect.y >>> 2;

      int[] colors = new int[8];
      int pvrOfs = (wordPosY*wordImageWidth + wordPosX)*wordSize;
      int imgOfs = 0;
      for (int y = 0; y < wordRectHeight; y++) {
        for (int x = 0; x < wordRectWidth; x++) {
          // decoding single DXT1 block
          int c = DynamicArray.getInt(pvr.data, pvrOfs);
          unpackColors565(c, colors);
          int code = DynamicArray.getInt(pvr.data, pvrOfs+4);
          for (int idx = 0; idx < 16; idx++, code >>>= 2) {
            int ofs = imgOfs + (idx >>> 2)*imgWidthAligned + (idx & 3);
            if ((code & 3) == 0) {
              // 100% c0, 0% c1
              imgData[ofs] = 0xff000000 | (colors[2] << 16) | (colors[1] << 8) | colors[0];
            } else if ((code & 3) == 1) {
              // 0% c0, 100% c1
              imgData[ofs] = 0xff000000 | (colors[6] << 16) | (colors[5] << 8) | colors[4];
            } else if ((code & 3) == 2) {
              if ((c & 0xffff) > ((c >>> 16) & 0xffff)) {
                // 66% c0, 33% c1
                int v = 0xff000000;
                v |= (((colors[2] << 1) + colors[6]) / 3) << 16;
                v |= (((colors[1] << 1) + colors[5]) / 3) << 8;
                v |=  ((colors[0] << 1) + colors[4]) / 3;
                imgData[ofs] = v;
              } else {
                // 50% c0, 50% c1
                int v = 0xff000000;
                v |= ((colors[2] + colors[6]) >>> 1) << 16;
                v |= ((colors[1] + colors[5]) >>> 1) << 8;
                v |=  (colors[0] + colors[4]) >>> 1;
                imgData[ofs] = v;
              }
            } else {
              if ((c & 0xffff) > ((c >>> 16) & 0xffff)) {
                // 33% c0, 66% c1
                int v = 0xff000000;
                v |= ((colors[2] + (colors[6] << 1)) / 3) << 16;
                v |= ((colors[1] + (colors[5] << 1)) / 3) << 8;
                v |=  (colors[0] + (colors[4] << 1)) / 3;
                imgData[ofs] = v;
              } else {
                // transparent
                imgData[ofs] = 0;
              }
            }
          }

          pvrOfs += wordSize;
          imgOfs += 4;
        }
        pvrOfs += (wordImageWidth - wordRectWidth)*wordSize;
        imgOfs += imgWidthAligned*4 - rect.width;
      }
      imgData = null;

      // rendering aligned image to target image
      if (alignedImage != null) {
        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.drawImage(alignedImage, 0, 0, region.width, region.height,
                      region.x, region.y, region.x+region.width, region.y+region.height, null);
        } finally {
          g.dispose();
          g = null;
        }
        alignedImage = null;
      }
      return true;
    }

    /**
     * Decodes PVR data in DXT3 format and draws the specified "region" into "image".
     * @param pvr The PVR data
     * @param image The output image
     * @param region The of the PVR texture region to draw onto "image"
     * @return The success state of the operation.
     * @throws Exception on error.
     */
    public static boolean decodeDXT3(PvrInfo pvr, BufferedImage image, Rectangle region) throws Exception
    {
      if (pvr == null || image == null || region == null) {
        return false;
      }

      int imgWidth = image.getWidth();
      int imgHeight = image.getHeight();
      int[] imgData = null;

      // checking region bounds and alignment
      if (region.x < 0) {region.width += -region.x; region.x = 0; }
      if (region.y < 0) {region.height += -region.y; region.y = 0; }
      if (region.x + region.width > pvr.width) region.width = pvr.width - region.x;
      if (region.y + region.height > pvr.height) region.height = pvr.height - region.y;
      Rectangle rect = alignRectangle(region, 4, 4);

      // preparing aligned image buffer for faster rendering
      BufferedImage alignedImage = null;
      int imgWidthAligned;
      if (!region.equals(rect)) {
        alignedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        imgWidthAligned = alignedImage.getWidth();
        imgData = ((DataBufferInt)alignedImage.getRaster().getDataBuffer()).getData();
        // translating "region" to be relative to "rect"
        region.x -= rect.x;
        region.y -= rect.y;
        if (imgWidth < region.width) {
          region.width= imgWidth;
        }
        if (imgHeight < region.height) {
          region.height = imgHeight;
        }
      } else {
        imgWidthAligned = imgWidth;
        imgData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      }

      final int wordSize = 16;                  // data size of an encoded 4x4 pixel block
      int wordImageWidth = pvr.width >>> 2;     // the image width in data blocks
      int wordRectWidth = rect.width >>> 2;     // the aligned region's width in data blocks
      int wordRectHeight = rect.height >>> 2;   // the aligned region's height in data blocks
      int wordPosX = rect.x >>> 2;
      int wordPosY = rect.y >>> 2;

      int[] colors = new int[8];
      int pvrOfs = (wordPosY*wordImageWidth + wordPosX)*wordSize;
      int imgOfs = 0;
      for (int y = 0; y < wordRectHeight; y++) {
        for (int x = 0; x < wordRectWidth; x++) {
          // decoding single DXT3 block
          long alpha = DynamicArray.getByte(pvr.data, pvrOfs);
          int c = DynamicArray.getInt(pvr.data, pvrOfs+8);
          unpackColors565(c, colors);
          int code = DynamicArray.getInt(pvr.data, pvrOfs+12);
          for (int idx = 0; idx < 16; idx++, code >>>= 2, alpha >>>= 4) {
            // calculating alpha (4 bit -> 8 bit)
            int ofs = imgOfs + (idx >>> 2)*imgWidthAligned + (idx & 3);
            int color = (int)(alpha & 0xf) << 24;
            color |= color << 4;
            // decoding pixels
            if ((code & 3) == 0) {
              // 100% c0, 0% c1
              color |= (colors[2] << 16) | (colors[1] << 8) | colors[0];
            } else if ((code & 3) == 1) {
              // 0% c0, 100% c1
              color |= (colors[6] << 16) | (colors[5] << 8) | colors[4];
            } else if ((code & 3) == 2) {
              // 66% c0, 33% c1
              int v = (((colors[2] << 1) + colors[6]) / 3) << 16;
              color |= v;
              v = (((colors[1] << 1) + colors[5]) / 3) << 8;
              color |= v;
              v = ((colors[0] << 1) + colors[4]) / 3;
              color |= v;
            } else {
              // 33% c0, 66% c1
              int v = ((colors[2] + (colors[6] << 1)) / 3) << 16;
              color |= v;
              v = ((colors[1] + (colors[5] << 1)) / 3) << 8;
              color |= v;
              v = (colors[0] + (colors[4] << 1)) / 3;
              if (v > 255) v = 255;
              color |= v;
            }
            imgData[ofs] = color;
          }

          pvrOfs += wordSize;
          imgOfs += 4;
        }
        pvrOfs += (wordImageWidth - wordRectWidth)*wordSize;
        imgOfs += (imgWidthAligned << 2) - rect.width;
      }
      imgData = null;

      // rendering aligned image to target image
      if (alignedImage != null) {
        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.drawImage(alignedImage, 0, 0, region.width, region.height,
                      region.x, region.y, region.x+region.width, region.y+region.height, null);
        } finally {
          g.dispose();
          g = null;
        }
        alignedImage = null;
      }
      return true;
    }

    /**
     * Decodes PVR data in DXT5 format and draws the specified "region" into "image".
     * @param pvr The PVR data
     * @param image The output image
     * @param region The of the PVR texture region to draw onto "image"
     * @return The success state of the operation.
     * @throws Exception on error.
     */
    public static boolean decodeDXT5(PvrInfo pvr, BufferedImage image, Rectangle region) throws Exception
    {
      if (pvr == null || image == null || region == null) {
        return false;
      }

      int imgWidth = image.getWidth();
      int imgHeight = image.getHeight();
      int[] imgData = null;

      // checking region bounds and alignment
      if (region.x < 0) {region.width += -region.x; region.x = 0; }
      if (region.y < 0) {region.height += -region.y; region.y = 0; }
      if (region.x + region.width > pvr.width) region.width = pvr.width - region.x;
      if (region.y + region.height > pvr.height) region.height = pvr.height - region.y;
      Rectangle rect = alignRectangle(region, 4, 4);

      // preparing aligned image buffer for faster rendering
      BufferedImage alignedImage = null;
      int imgWidthAligned;
      if (!region.equals(rect)) {
        alignedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        imgWidthAligned = alignedImage.getWidth();
        imgData = ((DataBufferInt)alignedImage.getRaster().getDataBuffer()).getData();
        // translating "region" to be relative to "rect"
        region.x -= rect.x;
        region.y -= rect.y;
        if (imgWidth < region.width) {
          region.width= imgWidth;
        }
        if (imgHeight < region.height) {
          region.height = imgHeight;
        }
      } else {
        imgWidthAligned = imgWidth;
        imgData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      }

      final int wordSize = 16;                  // data size of an encoded 4x4 pixel block
      int wordImageWidth = pvr.width >>> 2;     // the image width in data blocks
      int wordRectWidth = rect.width >>> 2;     // the aligned region's width in data blocks
      int wordRectHeight = rect.height >>> 2;   // the aligned region's height in data blocks
      int wordPosX = rect.x >>> 2;
      int wordPosY = rect.y >>> 2;

      int[] alpha = new int[8];
      int[] colors = new int[8];
      int pvrOfs = (wordPosY*wordImageWidth + wordPosX)*wordSize;
      int imgOfs = 0;
      for (int y = 0; y < wordRectHeight; y++) {
        for (int x = 0; x < wordRectWidth; x++) {
          // creating alpha table
          alpha[0] = DynamicArray.getByte(pvr.data, pvrOfs) & 0xff;
          alpha[1] = DynamicArray.getByte(pvr.data, pvrOfs+1) & 0xff;
          if (alpha[0] > alpha[1]) {
            alpha[2] = (6*alpha[0] +   alpha[1]) / 7;
            alpha[3] = (5*alpha[0] + 2*alpha[1]) / 7;
            alpha[4] = (4*alpha[0] + 3*alpha[1]) / 7;
            alpha[5] = (3*alpha[0] + 4*alpha[1]) / 7;
            alpha[6] = (2*alpha[0] + 5*alpha[1]) / 7;
            alpha[7] = (  alpha[0] + 6*alpha[1]) / 7;
          } else {
            alpha[2] = (4*alpha[0] +   alpha[1]) / 5;
            alpha[3] = (3*alpha[0] + 2*alpha[1]) / 5;
            alpha[4] = (2*alpha[0] + 3*alpha[1]) / 5;
            alpha[5] = (  alpha[0] + 4*alpha[1]) / 5;
            alpha[6] = 0;
            alpha[7] = 255;
          }

          // decoding single DXT5 block
          long ctrl = DynamicArray.getLong(pvr.data, pvrOfs+2) & 0xffffffffffffL;
          int c = DynamicArray.getInt(pvr.data, pvrOfs+8);
          unpackColors565(c, colors);
          int code = DynamicArray.getInt(pvr.data, pvrOfs+12);
          for (int idx = 0; idx < 16; idx++, code >>>= 2, ctrl >>>= 3) {
            int ofs = imgOfs + (idx >>> 2)*imgWidthAligned + (idx & 3);
            int color = alpha[(int)(ctrl & 7L)] << 24;
            if ((code & 3) == 0) {
              // 100% c0, 0% c1
              color |= (colors[2] << 16) | (colors[1] << 8) | colors[0];
            } else if ((code & 3) == 1) {
              // 0% c0, 100% c1
              color |= (colors[6] << 16) | (colors[5] << 8) | colors[4];
            } else if ((code & 3) == 2) {
              // 66% c0, 33% c1
              int v = (((colors[2] << 1) + colors[6]) / 3) << 16;
              color |= v;
              v = (((colors[1] << 1) + colors[5]) / 3) << 8;
              color |= v;
              v = ((colors[0] << 1) + colors[4]) / 3;
              color |= v;
            } else {
              // 33% c0, 66% c1
              int v = ((colors[2] + (colors[6] << 1)) / 3) << 16;
              color |= v;
              v = ((colors[1] + (colors[5] << 1)) / 3) << 8;
              color |= v;
              v = (colors[0] + (colors[4] << 1)) / 3;
              if (v > 255) v = 255;
              color |= v;
            }
            imgData[ofs] = color;
          }

          pvrOfs += wordSize;
          imgOfs += 4;
        }
        pvrOfs += (wordImageWidth - wordRectWidth)*wordSize;
        imgOfs += (imgWidthAligned << 2) - rect.width;
      }
      imgData = null;

      // rendering aligned image to target image
      if (alignedImage != null) {
        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.drawImage(alignedImage, 0, 0, region.width, region.height,
                      region.x, region.y, region.x+region.width, region.y+region.height, null);
        } finally {
          g.dispose();
          g = null;
        }
        alignedImage = null;
      }
      return true;
    }


    // Converts two RGB565 words into separate components, ordered { B, G, R, A, B, G, R, A }
    private static void unpackColors565(int inData, int[] outData)
    {
      outData[0] = ((inData << 3)  & 0xf8) | (inData >>> 2)  & 0x07;      // b1
      outData[1] = ((inData >>> 3) & 0xfc) | (inData >>> 9)  & 0x03;      // g1
      outData[2] = ((inData >>> 8) & 0xf8) | (inData >>> 13) & 0x07;      // r1
      outData[3] = 255;                                                   // a1
      outData[4] = ((inData >>> 13) & 0xf8) | (inData >>> 18) & 0x07;     // b2
      outData[5] = ((inData >>> 19) & 0xfc) | (inData >>> 25) & 0x03;     // g2
      outData[6] = ((inData >>> 24) & 0xf8) | (inData >>> 29) & 0x07;     // r2
      outData[7] = 255;                                                   // a2
    }
  }


  // Decodes PVRTC pixel data.
  private static class DecodePVRT
  {
    // The local cache list for decoded PVR textures. The "key" has to be a unique PvrInfo structure.
    private static final Map<PvrInfo, BufferedImage> textureCache =
        Collections.synchronizedMap(new LinkedHashMap<PvrInfo, BufferedImage>());
    // The max. number of cache entries to hold
    private static int MaxCacheEntries = 8;

    // Datatypes as used in the reference implementation:
    // Pixel32/128S:      int[]{red, green, blue, alpha}
    // PVRTCWord:         int[]{modulation, color}
    // PVRTCWordIndices:  int[]{p0, p1, q0, q1, r0, r1, s0, s1}

    // color channel indices into an array of pixel values
    private static final int CH_R     = 0;
    private static final int CH_G     = 1;
    private static final int CH_B     = 2;
    private static final int CH_A     = 3;
    // start indices into an array of PVRTC blocks
    private static final int IDX_P    = 0;
    private static final int IDX_Q    = 2;
    private static final int IDX_R    = 4;
    private static final int IDX_S    = 6;
    // indices into a PVRTC data block
    private static final int BLK_MOD  = 0;
    private static final int BLK_COL  = 1;


    /** Removes all PvrDecoder objects from the cache. */
    public static void flushCache()
    {
      textureCache.clear();
    }

    // Returns a PvrDecoder object only if it already exists in the cache.
    private static BufferedImage getCachedImage(PvrInfo pvr)
    {
      BufferedImage retVal = null;
      if (pvr != null) {
        if (textureCache.containsKey(pvr)) {
          retVal = textureCache.get(pvr);
          // re-inserting entry to prevent premature removal from cache
          textureCache.remove(pvr);
          textureCache.put(pvr, retVal);
        }
      }
      return retVal;
    }

    // Returns a PvrDecoder object of the specified key if available, or creates and returns a new one otherwise.
    private static void registerCachedImage(PvrInfo pvr, BufferedImage image)
    {
      if (pvr != null && getCachedImage(pvr) == null && image != null) {
        textureCache.put(pvr, image);
        // removing excess cache entries
        while (textureCache.size() > MaxCacheEntries) {
          textureCache.remove(textureCache.keySet().iterator().next());
        }
      }
    }


    /**
     * Decodes PVR data in PVRT 2bpp format and draws the specified "region" into "image".
     * @param pvr The PVR data
     * @param image The output image
     * @param region The of the PVR texture region to draw onto "image"
     * @return The success state of the operation.
     * @throws Exception on error.
     */
    public static boolean decodePVRT2bpp(PvrInfo pvr, BufferedImage image, Rectangle region) throws Exception
    {
      return decodePVRT(pvr, image, region, true);
    }

    /**
     * Decodes PVR data in PVRT 4bpp format and draws the specified "region" into "image".
     * @param pvr The PVR data
     * @param image The output image
     * @param region The of the PVR texture region to draw onto "image"
     * @return The success state of the operation.
     * @throws Exception on error.
     */
    public static boolean decodePVRT4bpp(PvrInfo pvr, BufferedImage image, Rectangle region) throws Exception
    {
      return decodePVRT(pvr, image, region, false);
    }

    // Decodes both 2bpp and 4bpp versions of the PVRT format
    private static boolean decodePVRT(PvrInfo pvr, BufferedImage image, Rectangle region, boolean is2bpp)
        throws Exception
    {
      if (pvr == null || image == null || region == null) {
        return false;
      }

      int imgWidth = image.getWidth();
      int imgHeight = image.getHeight();
      int[] imgData = null;

      // bounds checking
      if (region.x < 0) { region.width += -region.x; region.x = 0; }
      if (region.y < 0) { region.height += -region.y; region.y = 0; }
      if (region.x + region.width > pvr.width) region.width = pvr.width - region.x;
      if (region.y + region.height > pvr.height) region.height = pvr.height - region.y;


      // preparing image buffer for faster rendering
      BufferedImage alignedImage = getCachedImage(pvr);
      if (alignedImage == null) {
        if (!region.equals(new Rectangle(0, 0, pvr.width, pvr.height))) {
          alignedImage = new BufferedImage(pvr.width, pvr.height, BufferedImage.TYPE_INT_ARGB);
          imgData = ((DataBufferInt)alignedImage.getRaster().getDataBuffer()).getData();
          if (imgWidth < region.width) {
            region.width= imgWidth;
          }
          if (imgHeight < region.height) {
            region.height = imgHeight;
          }
        } else {
          imgData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        }

        int wordWidth = is2bpp ? 8 : 4;
        int wordHeight = 4;
        int numXWords = pvr.width / wordWidth;
        int numYWords = pvr.height / wordHeight;
        int[] indices = new int[8];
        int[] p = new int[2], q = new int[2], r = new int[2], s = new int[2];
        int[][] pixels = new int[wordWidth*wordHeight][4];

        for (int wordY = -1; wordY < numYWords-1; wordY++) {
          for (int wordX = -1; wordX < numXWords-1; wordX++) {
            indices[IDX_P] =   wrapWordIndex(numXWords, wordX);
            indices[IDX_P+1] = wrapWordIndex(numYWords, wordY);
            indices[IDX_Q] =   wrapWordIndex(numXWords, wordX+1);
            indices[IDX_Q+1] = wrapWordIndex(numYWords, wordY);
            indices[IDX_R] =   wrapWordIndex(numXWords, wordX);
            indices[IDX_R+1] = wrapWordIndex(numYWords, wordY+1);
            indices[IDX_S] =   wrapWordIndex(numXWords, wordX+1);
            indices[IDX_S+1] = wrapWordIndex(numYWords, wordY+1);

            // work out the offsets into the twiddle structs, multiply by two as there are two members per word
            int[] wordOffsets = new int[]{
                twiddleUV(numXWords, numYWords, indices[IDX_P], indices[IDX_P+1]) << 1,
                twiddleUV(numXWords, numYWords, indices[IDX_Q], indices[IDX_Q+1]) << 1,
                twiddleUV(numXWords, numYWords, indices[IDX_R], indices[IDX_R+1]) << 1,
                twiddleUV(numXWords, numYWords, indices[IDX_S], indices[IDX_S+1]) << 1
            };

            // access individual elements to fill out input words
            p[BLK_MOD] = DynamicArray.getInt(pvr.data,  wordOffsets[0]    << 2);
            p[BLK_COL] = DynamicArray.getInt(pvr.data, (wordOffsets[0]+1) << 2);
            q[BLK_MOD] = DynamicArray.getInt(pvr.data,  wordOffsets[1]    << 2);
            q[BLK_COL] = DynamicArray.getInt(pvr.data, (wordOffsets[1]+1) << 2);
            r[BLK_MOD] = DynamicArray.getInt(pvr.data,  wordOffsets[2]    << 2);
            r[BLK_COL] = DynamicArray.getInt(pvr.data, (wordOffsets[2]+1) << 2);
            s[BLK_MOD] = DynamicArray.getInt(pvr.data,  wordOffsets[3]    << 2);
            s[BLK_COL] = DynamicArray.getInt(pvr.data, (wordOffsets[3]+1) << 2);

            // assemble four words into struct to get decompressed pixels from
            getDecompressedPixels(p, q, r, s, pixels, is2bpp);
            mapDecompressedData(imgData, pvr.width, pixels, indices, is2bpp);
          }
        }
        imgData = null;
        registerCachedImage(pvr, alignedImage);
      } else {
        if (imgWidth < region.width) {
          region.width= imgWidth;
        }
        if (imgHeight < region.height) {
          region.height = imgHeight;
        }
      }

      // rendering aligned image to target image
      if (alignedImage != null) {
        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.drawImage(alignedImage, 0, 0, region.width, region.height,
                      region.x, region.y, region.x+region.width, region.y+region.height, null);
        } finally {
          g.dispose();
          g = null;
        }
        alignedImage = null;
      }
      return true;
    }


    // Decodes the first color in a PVRT data word
    private static int[] getColorA(int colorData)
    {
      int[] retVal = new int[4];
      if ((colorData & 0x8000) != 0) {
        // opaque color mode: RGB554
        retVal[CH_R] = (colorData & 0x7c00) >>> 10;                     // red: 5->5 bits
        retVal[CH_G] = (colorData & 0x3e0)  >>> 5;                      // green: 5->5 bits
        retVal[CH_B] = (colorData & 0x1e) | ((colorData & 0x1e) >>> 4); // blue: 4->5 bits
        retVal[CH_A] = 0x0f;                                            // alpha: 0->4 bits
      } else {
        // transparent color mode: ARGB3443
        retVal[CH_R] = ((colorData & 0xf00)  >>> 7) | ((colorData & 0xf00) >>> 11);  // red: 4->5 bits
        retVal[CH_G] = ((colorData & 0xf0)   >>> 3) | ((colorData & 0xf0)  >>> 7);   // green: 4->5 bits
        retVal[CH_B] = ((colorData & 0xe)     << 1) | ((colorData & 0xe)   >>> 2);   // blue: 3->5 bits
        retVal[CH_A] = ((colorData & 0x7000) >>> 11);                                // alpha: 3->4 bits
      }
      return retVal;
    }

    // Decodes the second color in a PVRT data word
    private static int[] getColorB(int colorData)
    {
      int[] retVal = new int[4];
      if ((colorData & 0x80000000) != 0) {
        // opaque color mode: RGB555
        retVal[CH_R] = (colorData & 0x7c000000) >>> 26;    // red: 5->5 bits
        retVal[CH_G] = (colorData & 0x3e00000)  >>> 21;    // green: 5->5 bits
        retVal[CH_B] = (colorData & 0x1f0000)   >>> 16;    // blue: 5->5 bits
        retVal[CH_A] = 0x0f;                               // alpha: 0->4 bits
      } else {
        // transparent color mode: ARGB3444
        retVal[CH_R] = ((colorData & 0xf000000)  >>> 23) | ((colorData & 0xf000000) >>> 27); // red: 4->5 bits
        retVal[CH_G] = ((colorData & 0xf00000)   >>> 19) | ((colorData & 0xf00000)  >>> 23); // green: 4->5 bits
        retVal[CH_B] = ((colorData & 0xf0000)    >>> 15) | ((colorData & 0xf0000)   >>> 19); // blue: 4->5 bits
        retVal[CH_A] = ((colorData & 0x70000000) >>> 27);                                    // alpha: 3->4 bits
      }
      return retVal;
    }

    // Bilinear upscale from 2x2 pixels to 4x4/8x4 pixels (depending on is2bpp argument)
    // p, q, r, s = [channels]
    // outBlock = [pixels][channels]
    // is2bpp: true=2bpp mode, false=4bpp mode
    private static void interpolateColors(int[] p, int[] q, int[] r, int[] s, int[][] outBlock, boolean is2bpp)
    {
      int wordWidth = is2bpp ? 8 : 4;
      int wordHeight = 4;

      // making working copy
      int[] hp = Arrays.copyOf(p, p.length);
      int[] hq = Arrays.copyOf(q, q.length);
      int[] hr = Arrays.copyOf(r, r.length);
      int[] hs = Arrays.copyOf(s, s.length);

      // get vectors
      int[] qmp = new int[]{hq[CH_R] - hp[CH_R], hq[CH_G] - hp[CH_G], hq[CH_B] - hp[CH_B], hq[CH_A] - hp[CH_A]};
      int[] smr = new int[]{hs[CH_R] - hr[CH_R], hs[CH_G] - hr[CH_G], hs[CH_B] - hr[CH_B], hs[CH_A] - hr[CH_A]};

      // multiply colors
      for (int i = 0; i < 4; i++) {
        hp[i] *= wordWidth;
        hr[i] *= wordWidth;
      }

      int[] result = new int[4], dy = new int[4];
      if (is2bpp) {
        // loop through pixels to achieve results
        for (int x = 0; x < wordWidth; x++) {
          for (int i = 0; i < 4; i++) {
            result[i] = hp[i] << 2;
            dy[i] = hr[i] - hp[i];
          }

          for (int y = 0; y < wordHeight; y++) {
            outBlock[y*wordWidth+x][CH_R] = (result[CH_R] >> 7) + (result[CH_R] >> 2);
            outBlock[y*wordWidth+x][CH_G] = (result[CH_G] >> 7) + (result[CH_G] >> 2);
            outBlock[y*wordWidth+x][CH_B] = (result[CH_B] >> 7) + (result[CH_B] >> 2);
            outBlock[y*wordWidth+x][CH_A] = (result[CH_A] >> 5) + (result[CH_A] >> 1);

            result[CH_R] += dy[CH_R];
            result[CH_G] += dy[CH_G];
            result[CH_B] += dy[CH_B];
            result[CH_A] += dy[CH_A];
          }

          hp[CH_R] += qmp[CH_R];
          hp[CH_G] += qmp[CH_G];
          hp[CH_B] += qmp[CH_B];
          hp[CH_A] += qmp[CH_A];
          hr[CH_R] += smr[CH_R];
          hr[CH_G] += smr[CH_G];
          hr[CH_B] += smr[CH_B];
          hr[CH_A] += smr[CH_A];
        }
      } else {
        // loop through pixels to achieve results
        for (int y = 0; y < wordHeight; y++) {
          for (int i = 0; i < 4; i++) {
            result[i] = hp[i] << 2;
            dy[i] = hr[i] - hp[i];
          }

          for (int x = 0; x < wordWidth; x++) {
            outBlock[y*wordWidth+x][CH_R] = (result[CH_R] >> 6) + (result[CH_R] >> 1);
            outBlock[y*wordWidth+x][CH_G] = (result[CH_G] >> 6) + (result[CH_G] >> 1);
            outBlock[y*wordWidth+x][CH_B] = (result[CH_B] >> 6) + (result[CH_B] >> 1);
            outBlock[y*wordWidth+x][CH_A] = (result[CH_A] >> 4) + result[CH_A];

            result[CH_R] += dy[CH_R];
            result[CH_G] += dy[CH_G];
            result[CH_B] += dy[CH_B];
            result[CH_A] += dy[CH_A];
          }

          hp[CH_R] += qmp[CH_R];
          hp[CH_G] += qmp[CH_G];
          hp[CH_B] += qmp[CH_B];
          hp[CH_A] += qmp[CH_A];
          hr[CH_R] += smr[CH_R];
          hr[CH_G] += smr[CH_G];
          hr[CH_B] += smr[CH_B];
          hr[CH_A] += smr[CH_A];
        }
      }
    }

    // Reads out and decodes the modulation values within the specified data word
    // modValues, modModes = [x][y]
    private static void unpackModulations(int[] word, int ofsX, int ofsY, int[][] modValues,
                                              int[][] modModes, boolean is2bpp)
    {
      int modMode = word[BLK_COL] & 1;
      int modBits = word[BLK_MOD];

      // unpack differently depending on 2bpp or 4bpp modes
      if (is2bpp) {
        if (modMode != 0) {
          // determine which of the three modes are in use:
          if ((modBits & 1) != 0) {
            // look at the LSB for the center (V=2, H=4) texel. Its LSB is now actually used to
            // indicate whether it's the H-only mode or the V-only

            // the center texel data is at (y=2, x=4) and so its LSB is at bit 20
            if ((modBits & (1 << 20)) != 0) {
              // this is V-only mode
              modMode = 3;
            } else {
              // this is H-only mode
              modMode = 2;
            }

            // create an extra bit for the center pixel so that it looks like we have 2 actual bits
            // for this texel. It makes later coding much easier.
            if ((modBits & (1 << 21)) != 0) {
              modBits |= (1 << 20);
            } else {
              modBits &= ~(1 << 20);
            }
          }

          if ((modBits & 2) != 0) {
            modBits |= 1;     // set it
          } else {
            modBits &= ~1;    // clear it
          }

          // run through all the pixels in the block. Note we can now treat all the stored values as
          // if they have 2 bits (even when they didn't!)
          for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 8; x++) {
              modModes[x+ofsX][y+ofsY] = modMode;

              // if this is a stored value...
              if (((x^y) & 1) == 0) {
                modValues[x+ofsX][y+ofsY] = modBits & 3;
                modBits >>>= 2;
              }
            }
          }
        } else {
          // if direct encoded 2bpp mode - i.e. mode bit per pixel
          for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 8; x++) {
              modModes[x+ofsX][y+ofsY] = modMode;

              // double the bits, so 0 -> 00, and 1 -> 11
              modValues[x+ofsX][y+ofsY] = ((modBits & 1) != 0) ? 3 : 0;
              modBits >>>= 1;
            }
          }
        }
      } else {
        // much simpler than 2bpp decompression, only two modes, so the n/8 values are set directly
        // run through all the pixels in the word
        if (modMode != 0) {
          for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
              modValues[y+ofsY][x+ofsX] = modBits & 3;
              if (modValues[y+ofsY][x+ofsX] == 1) {
                modValues[y+ofsY][x+ofsX] = 4;
              } else if (modValues[y+ofsY][x+ofsX] == 2) {
                modValues[y+ofsY][x+ofsX] = 14;   // +10 tells the decompressor to punch through alpha
              } else if (modValues[y+ofsY][x+ofsX] == 3) {
                modValues[y+ofsY][x+ofsX] = 8;
              }
              modBits >>>= 2;
            }
          }
        } else {
          for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
              modValues[y+ofsY][x+ofsX] = modBits & 3;
              modValues[y+ofsY][x+ofsX] *= 3;
              if (modValues[y+ofsY][x+ofsX] > 3) {
                modValues[y+ofsY][x+ofsX] -= 1;
              }
              modBits >>>= 2;
            }
          }
        }
      }
    }

    // Gets the effective modulation values for the given pixel
    // modValues, modModes = [x][y]
    // xPos, yPos = x, y positions within the current data word
    private static int getModulationValues(int[][] modValues, int[][] modModes, int xPos, int yPos, boolean is2bpp)
    {
      if (is2bpp) {
        final int[] repVals0 = new int[]{0, 3, 5, 8};

        // extract the modulation value...
        if (modModes[xPos][yPos] == 0) {
          // ...if a simple encoding
          return repVals0[modValues[xPos][yPos]];
        } else {
          // ...if this is a stored value
          if (((xPos^yPos) & 1) == 0) {
            return repVals0[modValues[xPos][yPos]];

            // else average from the neighbors
          } else if (modModes[xPos][yPos] == 1) {
            // if H&V interpolation
            return (repVals0[modValues[xPos][yPos-1]] +
                    repVals0[modValues[xPos][yPos+1]] +
                    repVals0[modValues[xPos-1][yPos]] +
                    repVals0[modValues[xPos+1][yPos]] + 2) >> 2;
          } else if (modModes[xPos][yPos] == 2) {
            // if H-only
            return (repVals0[modValues[xPos-1][yPos]] +
                    repVals0[modValues[xPos+1][yPos]] + 1) >> 1;
          } else {
            // if V-only
            return (repVals0[modValues[xPos][yPos-1]] +
                    repVals0[modValues[xPos][yPos+1]] + 1) >> 1;
          }
        }
      } else {
        return modValues[xPos][yPos];
      }
    }

    // Gets decompressed pixels for a given decompression area
    // p, q, r, s = [block word]
    // outBlock = [pixels][channels]
    // is2bpp: true=2bpp mode, false=4bpp mode
    private static void getDecompressedPixels(int[] p, int[] q, int[] r, int[] s, int[][] outData, boolean is2bpp)
    {
      // 4bpp only needs 8*8 values, but 2bpp needs 16*8, so rather than wasting processor time we just statically allocate 16*8
      int[][] modValues = new int[16][8];
      // Only 2bpp needs this
      int[][] modModes = new int[16][8];
      // 4bpp only needs 16 values, but 2bpp needs 32, so rather than wasting processor time we just statically allocate 32.
      int[][] upscaledColorA = new int[32][4];
      int[][] upscaledColorB = new int[32][4];

      int wordWidth = is2bpp ? 8 : 4;
      int wordHeight = 4;

      // get modulation from each word
      unpackModulations(p, 0, 0, modValues, modModes, is2bpp);
      unpackModulations(q, wordWidth, 0, modValues, modModes, is2bpp);
      unpackModulations(r, 0, wordHeight, modValues, modModes, is2bpp);
      unpackModulations(s, wordWidth, wordHeight, modValues, modModes, is2bpp);

      // bilinear upscale image data from 2x2 -> 4x4
      interpolateColors(getColorA(p[BLK_COL]), getColorA(q[BLK_COL]),
                        getColorA(r[BLK_COL]), getColorA(s[BLK_COL]), upscaledColorA, is2bpp);
      interpolateColors(getColorB(p[BLK_COL]), getColorB(q[BLK_COL]),
                        getColorB(r[BLK_COL]), getColorB(s[BLK_COL]), upscaledColorB, is2bpp);

      int[] result = new int[4];
      for (int y = 0; y < wordHeight; y++) {
        for (int x = 0; x < wordWidth; x++) {
          int mod = getModulationValues(modValues, modModes, x+(wordWidth >>> 1), y+(wordHeight >>> 1), is2bpp);
          boolean punchThroughAlpha = false;
          if (mod > 10) {
            punchThroughAlpha = true;
            mod -= 10;
          }

          result[CH_R] = (upscaledColorA[y*wordWidth+x][CH_R]*(8-mod) + upscaledColorB[y*wordWidth+x][CH_R]*mod) >> 3;
          result[CH_G] = (upscaledColorA[y*wordWidth+x][CH_G]*(8-mod) + upscaledColorB[y*wordWidth+x][CH_G]*mod) >> 3;
          result[CH_B] = (upscaledColorA[y*wordWidth+x][CH_B]*(8-mod) + upscaledColorB[y*wordWidth+x][CH_B]*mod) >> 3;
          if (punchThroughAlpha) {
            result[CH_A] = 0;
          } else {
            result[CH_A] = (upscaledColorA[y*wordWidth+x][CH_A]*(8-mod) + upscaledColorB[y*wordWidth+x][CH_A]*mod) >> 3;
          }

          // convert the 32bit precision result to 8 bit per channel color
          if (is2bpp) {
            outData[y*wordWidth+x][CH_R] = result[CH_R];
            outData[y*wordWidth+x][CH_G] = result[CH_G];
            outData[y*wordWidth+x][CH_B] = result[CH_B];
            outData[y*wordWidth+x][CH_A] = result[CH_A];
          } else {
            outData[y+x*wordHeight][CH_R] = result[CH_R];
            outData[y+x*wordHeight][CH_G] = result[CH_G];
            outData[y+x*wordHeight][CH_B] = result[CH_B];
            outData[y+x*wordHeight][CH_A] = result[CH_A];
          }
        }
      }
    }

    // Maps decompressed data to the correct location in the output buffer
    private static int wrapWordIndex(int numWords, int word)
    {
      return ((word + numWords) % numWords);
    }

    // Given the word coordinates and the dimension of the texture in words, this returns the twiddled
    // offset of the word from the start of the map
    private static int twiddleUV(int xSize, int ySize, int xPos, int yPos)
    {
      // initially assume x is the larger size
      int minDimension = xSize;
      int maxValue = yPos;
      int twiddled = 0;
      int srcBitPos = 1;
      int dstBitPos = 1;
      int shiftCount = 0;

      // if y is the larger dimension - switch the min/max values
      if (ySize < xSize) {
        minDimension = ySize;
        maxValue = xPos;
      }

      // step through all the bits in the "minimum" dimension
      while (srcBitPos < minDimension) {
        if ((yPos & srcBitPos) != 0) {
          twiddled |= dstBitPos;
        }

        if ((xPos & srcBitPos) != 0) {
          twiddled |= (dstBitPos << 1);
        }

        srcBitPos <<= 1;
        dstBitPos <<= 2;
        shiftCount++;
      }

      // prepend any unused bits
      maxValue >>>= shiftCount;
      twiddled |= (maxValue << (shiftCount << 1));

      return twiddled;
    }

    // Maps decompressed data to the correct location in the output buffer
    // outBuffer = [pixel]
    // inData = [pixel][channel]
    // indices = [two per p, q, r, s]
    private static void mapDecompressedData(int[] outBuffer, int width, int[][] inData, int[] indices, boolean is2bpp)
    {
      int wordWidth = is2bpp ? 8 : 4;
      int wordHeight = 4;

      for (int y = 0; y < (wordHeight >>> 1); y++) {
        for (int x = 0; x < (wordWidth >>> 1); x++) {
          // map p
          int outOfs = (((indices[IDX_P+1]*wordHeight) + y + (wordHeight >>> 1))*width +
                          indices[IDX_P+0]*wordWidth + x + (wordWidth >>> 1));
          int inOfs = y*wordWidth + x;
          outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) |
                              (inData[inOfs][CH_G] << 8)  | inData[inOfs][CH_B];

          // map q
          outOfs = (((indices[IDX_Q+1]*wordHeight) + y + (wordHeight >>> 1))*width +
                      indices[IDX_Q+0]*wordWidth + x);
          inOfs = y*wordWidth + x + (wordWidth >>> 1);
          outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) |
                              (inData[inOfs][CH_G] << 8)  | inData[inOfs][CH_B];

          // map r
          outOfs = (((indices[IDX_R+1]*wordHeight) + y)*width +
                      indices[IDX_R+0]*wordWidth + x + (wordWidth >>> 1));
          inOfs = (y + (wordHeight >>> 1))*wordWidth + x;
          outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) |
                              (inData[inOfs][CH_G] << 8)  | inData[inOfs][CH_B];

          // map s
          outOfs = (((indices[IDX_S+1]*wordHeight) + y)*width +
                      indices[IDX_S+0]*wordWidth + x);
          inOfs = (y + (wordHeight >>> 1))*wordWidth + x + (wordWidth >>> 1);
          outBuffer[outOfs] = (inData[inOfs][CH_A] << 24) | (inData[inOfs][CH_R] << 16) |
                              (inData[inOfs][CH_G] << 8)  | inData[inOfs][CH_B];
        }
      }
    }
  }
}
