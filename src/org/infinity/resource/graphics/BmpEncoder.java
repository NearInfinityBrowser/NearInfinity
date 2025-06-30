// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

/**
 * Encodes an image to a BMP file.
 */
public class BmpEncoder {
  public static void writeBmp(BmpDecoder bmp, File outFile) throws IOException {
    if (bmp == null) {
      throw new IllegalArgumentException("bmp is null");
    }

    writeBmp(bmp.getImage(), outFile);
  }

  public static void writeBmp(RenderedImage image, File outFile) throws IOException {
    if (outFile == null) {
      throw new IllegalArgumentException("outFile is null");
    }

    try (final OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile))) {
      writeBmp(image, os);
    }
  }

  public static void writeBmp(BmpDecoder bmp, OutputStream os) throws IOException {
    if (bmp == null) {
      throw new IllegalArgumentException("bmp is null");
    }

    writeBmp(bmp.getImage(), os);
  }

  /**
   * Converts an image to the Windows BMP format and writes it to the specified output stream.
   * <p>
   * This method does not close the provided {@code OutputStream} after the write operation has completed; it is the
   * responsibility of the caller to close the stream, if desired.
   * </p>
   *
   * @param image {@link RenderedImage} to be written.
   * @param os    {@link OutputStream} to be written to.
   * @throws IllegalArgumentException      if any argument is {@code null}.
   * @throws UnsupportedOperationException if the image type is not supported by the encoder.
   * @throws IOException                   if an error occurs during writing or when not able to create required
   *                                         {@code OutputStream}.
   */
  public static void writeBmp(RenderedImage image, OutputStream os) throws IOException {
    if (image == null) {
      throw new IllegalArgumentException("image is null");
    }
    if (os == null) {
      throw new IllegalArgumentException("output stream is null");
    }

    try {
      if (ImageIO.write(image, "BMP", os)) {
        return;
      }
    } catch (Exception e) {
      // no further action
    }

    final int dataType = image.getData().getDataBuffer().getDataType();
    if (dataType != DataBuffer.TYPE_INT) {
      throw new UnsupportedOperationException("Unsupported image pixel format");
    }

    final DataBufferInt dataBuffer = (DataBufferInt)image.getData().getDataBuffer();
    final int[] data = dataBuffer.getData();

    final int headerSize = 0x36;
    final ByteBuffer header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);
    header.putShort(0x00, (short)0x4d42); // BM
    header.putLong(0x02, data.length * 4 + headerSize); // total BMP file size
    header.putLong(0x0a, headerSize); // start offset of pixel data
    header.putLong(0x0e, 0x28); // bmp header size
    header.putLong(0x12, image.getWidth());   // image width in pixels
    header.putLong(0x16, image.getHeight());  // image height in pixels
    header.putShort(0x1a, (short)1);  // # planes
    header.putShort(0x1c, (short)32); // BPP
    header.putLong(0x26, 0xb12);  // horizontal pixels/meter
    header.putLong(0x2a, 0xb12);  // vertical pixels/meter
    os.write(header.array(), 0, header.capacity());

    final int pixelsPerRow = image.getWidth();
    final int bytesPerRow = pixelsPerRow * 4;
    final ByteBuffer byteBuffer = ByteBuffer.allocate(bytesPerRow).order(ByteOrder.LITTLE_ENDIAN);
    final IntBuffer intBuffer = byteBuffer.asIntBuffer();
    for (int y = 0, height = image.getHeight(); y < height; y++) {
      final int row = height - y - 1;
      final int rowOfs = row * pixelsPerRow;
      intBuffer.put(data, rowOfs, pixelsPerRow);
      intBuffer.flip();
      os.write(byteBuffer.array(), 0, byteBuffer.limit());
    }
  }
}
