// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2023 Jon Olav Hauglid
// See LICENSE.txt for license information
// ----------------------------------------------------------------
// PVRT format specifications and reference implementation:
// Copyright (c) Imagination Technologies Ltd. All Rights Reserved

package org.infinity.resource.graphics.decoder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

import org.infinity.util.DynamicArray;

/**
 * Storage of preprocessed data for a single PVR resource.
 */
public class PvrInfo {
  /** Flags indicates special properties of the color data. */
  public enum Flags {
    NONE, PRE_MULTIPLIED
  }

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
  public enum ColorSpace {
    RGB, SRGB
  }

  /** Datatype used to describe a color component. */
  public enum ChannelType {
    UBYTE_NORM, SBYTE_NORM, UBYTE, SBYTE,
    USHORT_NORM, SSHORT_NORM, USHORT, SSHORT,
    UINT_NORM, SINT_NORM, UINT, SINT,
    FLOAT
  }

  // Supported pixel formats
  private static final EnumSet<PixelFormat> SUPPORTED_FORMATS = EnumSet.of(
      PixelFormat.DXT1,
      PixelFormat.DXT3,
      PixelFormat.DXT5,
      PixelFormat.PVRTC_2BPP_RGB,
      PixelFormat.PVRTC_2BPP_RGBA,
      PixelFormat.PVRTC_4BPP_RGB,
      PixelFormat.PVRTC_4BPP_RGBA
  );

  private final Decodable decoder;

  /** PVR signature ("<code>PVR&#92;u0003</code>"). */
  int signature;
  /** PVR flags. */
  Flags flags;
  /** (Texture) pixel format of PVR data. */
  PixelFormat pixelFormat;
  /** Optional definition of a custom pixel format. */
  byte[] pixelFormatEx;
  /** Whether pixel data is encoded in RGB or sRGB color space. */
  ColorSpace colorSpace;
  /** Defines the decompressed color value format. */
  ChannelType channelType;
  /** Texture height, in pixels. */
  int height;
  /** Texture width, in pixels. */
  int width;
  /** (Average) color depth of a single decoded pixel. */
  int colorDepth;
  /** Average bits per pixel for encoded texture data. */
  int bitsPerInputPixel;
  /** Depth of the texture, for 3D textures. */
  int textureDepth;
  /** Number of texture surfaces. */
  int numSurfaces;
  /** Number of texture faces. */
  int numFaces;
  /** Number of mipmaps. */
  int numMipMaps;
  /** Size, in bytes, of an optional metadata block. */
  int metaSize;
  /** Optional metadata. */
  byte[] metaData;
  /** Total PVR header size, including metadata. */
  int headerSize;
  /** Encoded texture data. */
  byte[] data;

  /** Returns whether the specified pixel format is supported by the PVR decoder. */
  public static boolean isSupported(PixelFormat pixelFormat) {
    try {
      return SUPPORTED_FORMATS.contains(pixelFormat);
    } catch (Throwable t) {
      return false;
    }
  }

  /** Removes all PvrDecoder objects from the cache. */
  public static void flushCache() {
    PvrtcDecoder.flushCache();
  }

  /**
   * Initializes PVR information from the specified byte array.
   *
   * @param buffer Buffer containing PVR header data
   * @param size Size of the buffer data.
   * @throws Exception Thrown if the buffer doesn't contain valid PVR data.
   */
  public PvrInfo(byte[] buffer, int size) throws Exception {
    this.decoder = init(buffer, size);
  }

  /** Returns flags that indicate special properties of the color data. */
  public Flags getFlags() {
    return flags;
  }

  /** Returns the pixel format used to encode image data within the PVR file. */
  public PixelFormat getPixelFormat() {
    return pixelFormat;
  }

  /** Returns meaningful data only if pixelFormat() returns {@code PixelFormat.CUSTOM}. */
  public byte[] getPixelFormatEx() {
    return pixelFormatEx;
  }

  /** Returns the color space the image data is in. */
  public ColorSpace getColorSpace() {
    return colorSpace;
  }

  /** Returns the data type used to encode the image data within the PVR file. */
  public ChannelType getChannelType() {
    return channelType;
  }

  /** Returns the texture width in pixels. */
  public int getWidth() {
    return width;
  }

  /** Returns the texture height in pixels. */
  public int getHeight() {
    return height;
  }

  /** Returns the color depth of the pixel type used to encode the color data in bits/pixel. */
  public int getColorDepth() {
    return colorDepth;
  }

  /** Returns the average number of bits used for each input pixel. */
  public int getAverageBitsPerPixel() {
    return bitsPerInputPixel;
  }

  /** Returns the depth of the texture stored in the image data, in pixels. */
  public int getTextureDepth() {
    return textureDepth;
  }

  /** Returns the number of surfaces within the texture array. */
  public int getNumSurfaces() {
    return numSurfaces;
  }

  /** Returns the number of faces in a cube map. */
  public int getNumFaces() {
    return numFaces;
  }

  /** Returns the number of MIP-Map levels present including the top level. */
  public int getNumMipMaps() {
    return numMipMaps;
  }

  /** Returns the total size of meta data embedded in the PVR header. */
  public int getMetaSize() {
    return metaSize;
  }

  /** Provides access to the content of the meta data, embedded in the PVR header. Can be empty (size = 0). */
  public byte[] getMetaData() {
    return metaData;
  }

  /** Provides direct access to the content of the encoded pixel data. */
  public byte[] getData() {
    return data;
  }

  /**
   * Decodes PVR data of the given pixel format and draws the specified "region" into "image".
   *
   * @param image  The output image
   * @param region The PVR texture region to draw onto "image"
   * @return The success state of the operation.
   * @throws Exception on error.
   */
  public boolean decode(BufferedImage image, Rectangle region) throws Exception {
    return decoder.decode(image, region);
  }

  /** Returns whether the pixel format of the current texture is supported by the PVR decoder. */
  public boolean isSupported() {
    try {
      return SUPPORTED_FORMATS.contains(pixelFormat);
    } catch (Throwable t) {
      return false;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(data);
    result = prime * result + Arrays.hashCode(pixelFormatEx);
    result = prime * result + Objects.hash(bitsPerInputPixel, channelType, colorDepth, colorSpace, flags, headerSize,
        height, numFaces, numMipMaps, numSurfaces, pixelFormat, signature, textureDepth, width);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PvrInfo other = (PvrInfo) obj;
    return bitsPerInputPixel == other.bitsPerInputPixel && channelType == other.channelType
        && colorDepth == other.colorDepth && colorSpace == other.colorSpace && Arrays.equals(data, other.data)
        && flags == other.flags && headerSize == other.headerSize && height == other.height
        && numFaces == other.numFaces && numMipMaps == other.numMipMaps && numSurfaces == other.numSurfaces
        && pixelFormat == other.pixelFormat && Arrays.equals(pixelFormatEx, other.pixelFormatEx)
        && signature == other.signature && textureDepth == other.textureDepth && width == other.width;
  }

  // Initializes the PvrInfo structure and returns an associated Decodable object.
  private Decodable init(byte[] buffer, int size) throws Exception {
    if (buffer == null || size <= 0x34) {
      throw new Exception("Invalid or incomplete PVR input data");
    }

    signature = DynamicArray.getInt(buffer, 0);
    if (signature != 0x03525650) {
      throw new Exception("No PVR signature found");
    }

    int v = DynamicArray.getInt(buffer, 4);
    switch (v) {
      case 0:
        flags = Flags.NONE;
        break;
      case 1:
        flags = Flags.PRE_MULTIPLIED;
        break;
      default:
        throw new Exception(String.format("Unsupported PVR flags: %d", v));
    }

    long l = DynamicArray.getLong(buffer, 8);
    if ((l & 0xffffffff00000000L) != 0L) {
      // custom pixel format
      pixelFormat = PixelFormat.CUSTOM;
      pixelFormatEx = new byte[8];
      System.arraycopy(buffer, 8, pixelFormatEx, 0, 8);
    } else {
      // predefined pixel format
      switch ((int) l) {
        case 0:
          pixelFormat = PixelFormat.PVRTC_2BPP_RGB;
          bitsPerInputPixel = 2;
          break;
        case 1:
          pixelFormat = PixelFormat.PVRTC_2BPP_RGBA;
          bitsPerInputPixel = 2;
          break;
        case 2:
          pixelFormat = PixelFormat.PVRTC_4BPP_RGB;
          bitsPerInputPixel = 4;
          break;
        case 3:
          pixelFormat = PixelFormat.PVRTC_4BPP_RGBA;
          bitsPerInputPixel = 4;
          break;
        case 4:
          pixelFormat = PixelFormat.PVRTC2_2BPP;
          bitsPerInputPixel = 2;
          break;
        case 5:
          pixelFormat = PixelFormat.PVRTC2_4BPP;
          bitsPerInputPixel = 4;
          break;
        case 6:
          pixelFormat = PixelFormat.ETC1;
          bitsPerInputPixel = 4;
          break;
        case 7:
          pixelFormat = PixelFormat.DXT1;
          bitsPerInputPixel = 4;
          break;
        case 8:
          pixelFormat = PixelFormat.DXT2;
          bitsPerInputPixel = 8;
          break;
        case 9:
          pixelFormat = PixelFormat.DXT3;
          bitsPerInputPixel = 8;
          break;
        case 10:
          pixelFormat = PixelFormat.DXT4;
          bitsPerInputPixel = 8;
          break;
        case 11:
          pixelFormat = PixelFormat.DXT5;
          bitsPerInputPixel = 8;
          break;
        case 12:
          pixelFormat = PixelFormat.BC4;
          break;
        case 13:
          pixelFormat = PixelFormat.BC5;
          break;
        case 14:
          pixelFormat = PixelFormat.BC6;
          break;
        case 15:
          pixelFormat = PixelFormat.BC7;
          break;
        case 16:
          pixelFormat = PixelFormat.UYVY;
          break;
        case 17:
          pixelFormat = PixelFormat.YUY2;
          break;
        case 18:
          pixelFormat = PixelFormat.BW1BPP;
          break;
        case 19:
          pixelFormat = PixelFormat.R9G9B9E5;
          break;
        case 20:
          pixelFormat = PixelFormat.RGBG8888;
          break;
        case 21:
          pixelFormat = PixelFormat.GRGB8888;
          break;
        case 22:
          pixelFormat = PixelFormat.ETC2_RGB;
          bitsPerInputPixel = 4;
          break;
        case 23:
          pixelFormat = PixelFormat.ETC2_RGBA;
          bitsPerInputPixel = 8;
          break;
        case 24:
          pixelFormat = PixelFormat.ETC2_RGB_A1;
          bitsPerInputPixel = 4;
          break;
        case 25:
          pixelFormat = PixelFormat.EAC_R11_RGB_U;
          break;
        case 26:
          pixelFormat = PixelFormat.EAC_R11_RGB_S;
          break;
        case 27:
          pixelFormat = PixelFormat.EAC_RG11_RGB_U;
          break;
        case 28:
          pixelFormat = PixelFormat.EAC_RG11_RGB_S;
          break;
        default:
          throw new Exception(String.format("Unsupported pixel format: %s", Integer.toString((int) l)));
      }
      pixelFormatEx = new byte[0];
    }

    v = DynamicArray.getInt(buffer, 16);
    switch (v) {
      case 0:
        colorSpace = ColorSpace.RGB;
        break;
      case 1:
        colorSpace = ColorSpace.SRGB;
        break;
      default:
        throw new Exception(String.format("Unsupported color space: %d", v));
    }

    v = DynamicArray.getInt(buffer, 20);
    switch (v) {
      case 0:
        channelType = ChannelType.UBYTE_NORM;
        break;
      case 1:
        channelType = ChannelType.SBYTE_NORM;
        break;
      case 2:
        channelType = ChannelType.UBYTE;
        break;
      case 3:
        channelType = ChannelType.SBYTE;
        break;
      case 4:
        channelType = ChannelType.USHORT_NORM;
        break;
      case 5:
        channelType = ChannelType.SSHORT_NORM;
        break;
      case 6:
        channelType = ChannelType.USHORT;
        break;
      case 7:
        channelType = ChannelType.SSHORT;
        break;
      case 8:
        channelType = ChannelType.UINT_NORM;
        break;
      case 9:
        channelType = ChannelType.SINT_NORM;
        break;
      case 10:
        channelType = ChannelType.UINT;
        break;
      case 11:
        channelType = ChannelType.SINT;
        break;
      case 12:
        channelType = ChannelType.FLOAT;
        break;
      default:
        throw new Exception(String.format("Unsupported channel type: %d", v));
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
        colorDepth = 32; // most likely wrong, but not important for us anyway
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

    // returning associated Decodable object
    switch (pixelFormat) {
      case PVRTC_2BPP_RGB:
      case PVRTC_2BPP_RGBA:
      case PVRTC_4BPP_RGB:
      case PVRTC_4BPP_RGBA:
        return new PvrtcDecoder(this);
      case DXT1:
      case DXT3:
      case DXT5:
        return new DxtDecoder(this);
      default:
        return new DummyDecoder(this);
    }
  }
}
