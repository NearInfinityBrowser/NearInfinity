// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.ByteBufferInputStream;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

/**
 * Decodes a BMP file.
 */
public class BmpDecoder {
  private BufferedImage image;
  private Palette palette;
  private Info info;

  /**
   * Returns an initialized  {@code BmpDecoder} object with the specified {@code ResourceEntry} instance.
   *
   * @param entry The {@link ResourceEntry} instance of the BMP resource.
   * @return {@link BmpDecoder} instance with decoded BMP data.
   * @throws Exception If the BMP resource could not be loaded.
   */
  public static BmpDecoder loadBmp(ResourceEntry entry) throws Exception {
    return new BmpDecoder(Objects.requireNonNull(entry).getResourceBuffer());
  }

  /**
   * Returns an initialized  {@code BmpDecoder} object with the specified file.
   *
   * @param file Filename of the BMP resource as {@code String}.
   * @return {@link BmpDecoder} instance with decoded BMP data.
   * @throws Exception If the BMP resource could not be loaded.
   */
  public static BmpDecoder loadBmp(String file) throws Exception {
    return loadBmp(FileManager.resolve(file));
  }

  /**
   * Returns an initialized  {@code BmpDecoder} object with the specified {@code Path} instance.
   *
   * @param file Filename of the BMP resource as {@link Path} instance.
   * @return {@link BmpDecoder} instance with decoded BMP data.
   * @throws Exception If the BMP resource could not be loaded.
   */
  public static BmpDecoder loadBmp(Path file) throws Exception {
    try (InputStream is = StreamUtils.getInputStream(Objects.requireNonNull(file))) {
      return loadBmp(is);
    }
  }

  /**
   * Returns an initialized  {@code BmpDecoder} object with data from the specified {@code InputStream} instance.
   *
   * @param input The {@link InputStream} instance containing BMP data.
   * @return {@link BmpDecoder} instance with decoded BMP data.
   * @throws Exception If the BMP resource could not be loaded.
   */
  public static BmpDecoder loadBmp(InputStream input) throws Exception {
    Objects.requireNonNull(input);
    final int bufSize = 1024;
    final ArrayList<byte[]> bufList = new ArrayList<>();
    while (true) {
      final byte[] buf = new byte[bufSize];
      int len = input.read(buf);
      if (len == bufSize) {
        bufList.add(buf);
      } else if (len > 0) {
        bufList.add(Arrays.copyOf(buf, len));
      } else {
        break;
      }
    }

    int bufferSize = bufList.stream().mapToInt(b -> b.length).sum();
    final ByteBuffer bb = StreamUtils.getByteBuffer(bufferSize);
    bufList.forEach(bb::put);
    bb.rewind();

    return new BmpDecoder(bb);
  }

  private BmpDecoder(ByteBuffer buffer) throws Exception {
    init(buffer);
  }

  /** Returns information about the BMP image. */
  public Info getInfo() {
    return info;
  }

  /** Returns the decoded BMP resource as {@link Image} object. */
  public BufferedImage getImage() {
    return image;
  }

  /**
   * Returns the palette for indexed BMP resources.
   *
   * @return {@link Palette} object for indexed BMP resources, {@code null} otherwise.
   */
  public Palette getPalette() {
    return palette;
  }

  private void init(ByteBuffer buffer) throws Exception {
    Objects.requireNonNull(buffer);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    image = null;
    palette = null;
    info = null;

    buffer.rewind();

    // Initial checks
    final boolean isBMP = "BM".equals(StreamUtils.readString(buffer, 0, 2));
    final int bpp = isBMP ? buffer.getShort(28) : 0;

    // ImageIO seems to discard alpha information from 32-bit BMP files
    if (bpp < 32) {
      try (final ByteBufferInputStream bbis = new ByteBufferInputStream(buffer)) {
        image = ImageIO.read(bbis);
        // extracting palette
        if (image.getColorModel() instanceof IndexColorModel) {
          final IndexColorModel icm = (IndexColorModel) image.getColorModel();
          final int numColors = icm.getMapSize();
          final int[] colors = new int[numColors];
          icm.getRGBs(colors);
          final ByteBuffer pb = StreamUtils.getByteBuffer(colors.length * 4);
          for (final int color : colors) {
            pb.putInt(color);
          }
          pb.rewind();
          palette = new Palette(pb, 0, pb.capacity());
        }
        info = new Info(image);
      } catch (Exception e) {
        image = null;
        palette = null;
        info = null;
      }
    }

    if (image == null) {
      buffer.rewind();
      if (isBMP) {
        final int size = buffer.getInt(2);
        if (buffer.limit() < size) {
          throw new Exception("Unexpected end of file: " + buffer.limit() + " (expected: " + size + " )");
        }

        final int rasterOffset = buffer.getInt(10);
        final int headerSize = buffer.getInt(14);
        if (headerSize < 40) {   // not Windows Bitmap V1 or later
          throw new Exception("Unsupported BMP format");
        }

        final int width = buffer.getInt(18);
        final int height = buffer.getInt(22);
        final int bitCount = buffer.getShort(28);
        final int compression = buffer.getInt(30);
        if ((compression == 0 || compression == 3) && bitCount <= 32) {
          int colorsUsed = buffer.getInt(46);

          if (bitCount <= 8) {
            if (colorsUsed == 0) {
              colorsUsed = 1 << bitCount;
            }
            final int palSize = 4 * colorsUsed;
            final int palOffset = rasterOffset - palSize;
            palette = new Palette(buffer, palOffset, palSize);
          }

          final int bytesPerLine = bitCount * width / 8;
          final int padded = (4 - bytesPerLine % 4) % 4;

          image = ColorConvert.createCompatibleImage(width, height, bitCount >= 32);
          int offset = rasterOffset;
          for (int y = height - 1; y >= 0; y--) {
            setPixels(buffer, offset, bitCount, bytesPerLine, y, palette);
            offset += bytesPerLine + padded;
          }

          info = new Info(image, compression, bitCount);
        }
      }
    }

    if (image == null) {
      throw new Exception("Unsupported graphics format");
    }
  }

  private void setPixels(ByteBuffer buffer, int offset, int bitcount, int width, int y, Palette palette) {
    if (bitcount == 4) {
      int pix = 0;
      for (int x = 0; x < width; x++) {
        int color = buffer.get(offset + x) & 0xff;
        int color1 = (color >> 4) & 0x0f;
        image.setRGB(pix++, y, palette.getColor(color1));
        int color2 = color & 0x0f;
        image.setRGB(pix++, y, palette.getColor(color2));
      }
    } else if (bitcount == 8) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, palette.getColor(buffer.get(offset + x) & 0xff));
      }
    } else if (bitcount == 24) {
      for (int x = 0; x < width / 3; x++) {
        int rgb = (buffer.get(offset + 3 * x + 2) & 0xff) << 16;
        rgb |= (buffer.get(offset + 3 * x + 1) & 0xff) << 8;
        rgb |= buffer.get(offset + 3 * x) & 0xff;
        image.setRGB(x, y, rgb);
      }
    } else if (bitcount == 32) {
      for (int x = 0; x < width / 4; x++) {
        int rgb = buffer.getInt(offset + 4 * x);
        image.setRGB(x, y, rgb);
      }
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  /**
   * Provides basic information about the BMP resource.
   */
  public static class Info {
    /** Available bitmap compression types. */
    public enum Compression {
      /** Compression type could not be determined. */
      UNKNOWN(-1, "Unknown"),
      /** Uncompressed RGB pixel data. */
      RGB(0, "No compression"),
      /** RLE-encoded 8-bit paletted pixel data. */
      RLE8(1, "RLE encoded (8-bit)"),
      /** RLE-encoded 4-bit paletted pixel data. */
      RLE4(2, "RLE encoded (4-bit)"),
      /** Color components are defined by component masks. */
      BITFIELD(3, "Bitfield encoded"),
      ;

      private final int code;
      private final String label;

      Compression(int code, String label) {
        this.code = code;
        this.label = label;
      }

      /** Returns the numeric BMP compression code. Returns -1 for undetermined compression. */
      public int getCode() {
        return code;
      }

      /** Returns a descriptive label for the compression. */
      public String getLabel() {
        return label;
      }

      @Override
      public String toString() {
        return String.format("%s (%d)", getLabel(), getCode());
      }
    }

    private final Compression compression;
    private final int width;
    private final int height;
    private final int bpp;

    private Info(BufferedImage image) {
      this(image, -1, 0);
    }

    private Info(BufferedImage image, int compression, int bpp) {
      Objects.requireNonNull(image);

      this.compression = Arrays
          .stream(Compression.values())
          .filter(c -> c.getCode() == compression)
          .findFirst()
          .orElse(Compression.UNKNOWN);
      this.width = image.getWidth();
      this.height = image.getHeight();
      this.bpp = (bpp > 0) ? bpp : image.getColorModel().getPixelSize();
    }

    /** Returns the compression type of the BMP resource. */
    public Compression getCompression() {
      return compression;
    }

    /** Returns the image width, in pixels. */
    public int getWidth() {
      return width;
    }

    /** Returns the image height, in pixels. */
    public int getHeight() {
      return height;
    }

    /** Returns the number of bits per pixel. */
    public int getBitsPerPixel() {
      return bpp;
    }
  }
}
