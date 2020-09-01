// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.infinity.util.Misc;

/**
 * Collection of useful stream- and buffer-based operations.
 */
public class StreamUtils
{
  /** Attempts to replace the file extension string of {@code fileName} by {@code newExt}. */
  public static String replaceFileExtension(String fileName, String newExt)
  {
    String retVal = fileName;
    if (retVal != null) {
      if (newExt == null) {
        newExt = "";
      }
      // making sure that our 'dot' belongs to the filename's extension
      Path file = FileManager.resolve(retVal);
      String name = file.getFileName().toString();
      int pos = name.lastIndexOf('.');
      if (pos > 0) {
        pos = retVal.lastIndexOf('.');
        if (pos > 0) {
          retVal = retVal.substring(0, pos);
        }
      }
      if (newExt.length() > 0 && newExt.charAt(0) != '.') {
        retVal += ".";
      }
      retVal += newExt;
    }
    return retVal;
  }

  /** Attempts to replace the file extension string of {@code file} by {@code newExt}. */
  public static Path replaceFileExtension(Path file, String newExt)
  {
    Path retVal = file;
    if (file != null) {
      if (newExt == null) {
        newExt = "";
      }
      String name = file.getFileName().toString();
      int pos = name.lastIndexOf('.');
      if (pos > 0) {
        // no need to replace if extensions are equal
        if (newExt.length() > 0 && newExt.charAt(0) == '.') {
          if (name.substring(pos).equalsIgnoreCase(newExt)) {
            return retVal;
          }
        } else {
          if (name.substring(pos+1).equalsIgnoreCase(newExt)) {
            return retVal;
          }
        }
        name = name.substring(0, pos);
      }
      if (newExt.length() > 0 && newExt.charAt(0) != '.') {
        name += ".";
      }
      name += newExt;
      if (file.getParent() != null) {
        retVal = file.getParent().resolve(name);
      } else {
        retVal = file.getFileSystem().getPath(name);
      }
    }
    return retVal;
  }

  /**
   * Splits {@code fileName} into a path, base and extension part and returns them as String array.
   * @param fileName The file name to split into its components.
   * @return A String array that always consists of three components.
   *         String[0] contains the path component. Can be empty (but is never {@code null}).
   *         String[1] contains the file base without path and extension.
   *         String[2] contains the file extension. Can be empty (but is never {@code null}).
   *         The concatenated string components are equal to the original {@code fileName} if
   *         {@code fileName} is not {@code null}.
   */
  public static String[] splitFileName(String fileName)
  {
    String[] retVal = {"", "", ""};
    if (fileName != null) {
      String temp = fileName.replace('\\', '/').replace(':', '/');
      // splitting path
      int p = temp.lastIndexOf('/');
      if (p >= 0) {
        retVal[0] = fileName.substring(0, p);
        if (p+1 < temp.length()) {
          temp = temp.substring(p+1);
        } else {
          temp = "";
        }
      }
      // splitting file base
      p = temp.lastIndexOf('.');
      if (p > 0) {  // p == 0 ? extension is file base
        retVal[1] = temp.substring(0, p);
        if (p < temp.length()) {
          temp = temp.substring(p);
        } else {
          temp = "";
        }
      }
      // determining file extension
      if (temp.length() > 0) {
        retVal[2] = temp;
      }
    }
    return retVal;
  }


  /** Returns a fully initialized empty {@link ByteBuffer} in little endian order. */
  public static ByteBuffer getByteBuffer(int size)
  {
    return ByteBuffer.allocate(Math.max(0, size)).order(ByteOrder.LITTLE_ENDIAN);
  }

  /** Returns a {@link ByteBuffer} based on {@code buffer} in little endian order. */
  public static ByteBuffer getByteBuffer(byte[] buffer)
  {
    if (buffer == null) {
      buffer = new byte[0];
    }
    return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
  }

  /**
   * Convenience method: Returns an input stream for the specified file.
   * OpenOptions used: {@code StandardOpenOption.READ}
   */
  public static InputStream getInputStream(Path file) throws IOException
  {
    return getInputStream(file, StandardOpenOption.READ);
  }

  /**
   * Convenience method: Returns an input stream for the specified file using the specified
   * {@link OpenOption}s.
   */
  public static InputStream getInputStream(Path file, OpenOption... options) throws IOException
  {
    return new BufferedInputStream(Files.newInputStream(file, options));
  }

  /**
   * Convenience method: Returns an output stream for the specified file.
   * OpenOptions used: {@code StandardOpenOption.WRITE} and {@code StandardOpenOption.CREATE}.
   */
  public static OutputStream getOutputStream(Path file) throws IOException
  {
    return getOutputStream(file, false);
  }

  /**
   * Convenience method: Returns an output stream for the specified file.
   * OpenOptions used: {@code StandardOpenOption.WRITE}, {@code StandardOpenOption.CREATE} and
   * {@code StandardOpenOption.TRUNCATE_EXISTING) (if truncate is {@code true}).
   *
   */
  public static OutputStream getOutputStream(Path file, boolean truncate) throws IOException
  {
    OpenOption[] options = new OpenOption[truncate ? 3 : 2];
    options[0] = StandardOpenOption.WRITE;
    options[1] = StandardOpenOption.CREATE;
    if (truncate) {
      options[2] = StandardOpenOption.TRUNCATE_EXISTING;
    }
    return getOutputStream(file, options);
  }

  /**
   * Convenience method: Returns an output stream for the specified file using the specified
   * {@link OpenOption}s.
   */
  public static OutputStream getOutputStream(Path file, OpenOption... options) throws IOException
  {
    return new BufferedOutputStream(Files.newOutputStream(file, options));
  }

  /**
   * Copies data from {@code src} buffer to {@code dst} buffer using their current positions as offsets.
   * Current positions of byte buffers will be advanted to the end of copied data.
   */
  public static int copyBytes(ByteBuffer src, ByteBuffer dst, int length)
  {
    int retVal = copyBytes(src, src.position(), dst, dst.position(), length);
    src.position(src.position() + retVal);
    dst.position(dst.position() + retVal);
    return retVal;
  }

  /**
   * Copies data from {@code src} buffer to {@code dst} buffer using the specified offsets.
   * Current positions of byte buffers are unaffected.
   */
  public static int copyBytes(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int length)
  {
    int srcPos = src.position();
    int dstPos = dst.position();
    int maxLength = 0;
    try {
      src.position(srcOffset);
      dst.position(dstOffset);
      maxLength = Math.min(length, Math.min(src.remaining(), dst.remaining()));
      ByteBuffer bufTmp = src.duplicate();  // to preserve limit
      bufTmp.limit(bufTmp.position() + maxLength);
      dst.put(bufTmp);
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      src.position(srcPos);
      dst.position(dstPos);
    }
    return maxLength;
  }

  /**
   * Reads "length" number of bytes from the specified input stream and returns them
   * as new {@link ByteBuffer} object.
   */
  public static ByteBuffer readBytes(InputStream is, int length) throws IOException
  {
    ByteBuffer bb = null;
    if (length > 0) {
      bb = getByteBuffer(length);
      readBytes(is, bb);
    } else {
      bb = getByteBuffer(0);
    }
    bb.position(0);
    return bb;
  }

  /**
   * Reads as many bytes from the input stream into the given ByteBuffer, starting at current
   * buffer position and ending at the current buffer's limit.
   */
  public static void readBytes(InputStream is, ByteBuffer buffer) throws IOException
  {
    byte[] buf = new byte[8192];
    while (buffer.remaining() > 0) {
      int len = Math.min(buf.length, buffer.remaining());
      int n = is.read(buf, 0, len);
      if (n < 0) {
        break;
      }
      buffer.put(buf, 0, n);
    }
  }

  /** Reads as many bytes from the input stream into the specified byte array. */
  public static void readBytes(InputStream is, byte[] buffer) throws IOException
  {
    readBytes(is, buffer, 0, buffer.length);
  }

  /**
   * Reads up to "length" bytes of data from the input stream into the given byte array,
   * starting at the specified offset.
   */
  public static void readBytes(InputStream is, byte[] buffer, int offset, int length) throws IOException
  {
    int bytesRead = 0;
    offset = Math.max(0, Math.min(offset, length));
    length = Math.min(buffer.length - offset, length);
    while (bytesRead < length) {
      int newRead = is.read(buffer, offset + bytesRead, length - bytesRead);
      if (newRead == -1) {
        throw new IOException("Unable to read remaining " + (buffer.length - bytesRead) + " bytes");
      }
      bytesRead += newRead;
    }
  }

  /**
   * Reads a byte (8 bit) from specified input stream.
   * @param is The input stream to read from.
   * @return The byte value from the stream.
   */
  public static byte readByte(InputStream is) throws IOException
  {
    byte res = 0;
    if (is != null) {
      int n = is.read();
      if (n != -1) {
        res = (byte)n;
      }
    }
    return res;
  }

  /**
   * Reads an unsigned byte (8 bit) from specified input stream.
   * @param is The input stream to read from.
   * @return The unsigned byte value from the stream.
   */
  public static short readUnsignedByte(InputStream is) throws IOException
  {
    return (short)(readByte(is) & 0xff);
  }

  /**
   * Reads a short (16 bit) from the specified byte channel.
   * @param channel The {@link ReadableByteChannel} to read from.
   * @return The short value from the channel.
   */
  public static short readShort(ReadableByteChannel channel) throws IOException
  {
    ByteBuffer bb2 = getByteBuffer(2);
    for (int cnt = 0; cnt < 2;) {
      int n = channel.read(bb2);
      if (n < 0) {
        throw new IOException("End of stream");
      }
      cnt += n;
    }
    bb2.position(0);
    return bb2.getShort();
  }

  /**
   * Reads a short (16 bit) from specified input stream.
   * @param is The input stream to read from.
   * @return The short value from the stream.
   */
  public static short readShort(InputStream is) throws IOException
  {
    ByteBuffer bb2 = getByteBuffer(2);
    for (int cnt = 0; cnt < 2;) {
      int n = is.read(bb2.array(), cnt, bb2.array().length - cnt);
      if (n < 0) {
        throw new IOException("End of stream");
      }
      cnt += n;
    }
    bb2.position(0);
    return bb2.getShort();
  }

  /**
   * Reads an unsigned short (16 bit) from specified input stream.
   * @param is The input stream to read from.
   * @return The unsigned short value from the stream.
   */
  public static int readUnsignedShort(InputStream is) throws IOException
  {
    return readShort(is) & 0xffff;
  }

  /**
   * Reads an integer (32 bit) from the specified byte channel.
   * @param channel The {@link ReadableByteChannel} to read from.
   * @return The integer value from the channel.
   */
  public static int readInt(ReadableByteChannel channel) throws IOException
  {
    ByteBuffer bb4 = getByteBuffer(4);
    bb4.position(0);
    for (int cnt = 0; cnt < 4;) {
      int n = channel.read(bb4);
      if (n < 0) {
        throw new IOException("End of stream");
      }
      cnt += n;
    }
    bb4.position(0);
    return bb4.getInt();
  }

  /**
   * Reads an integer (32 bit) from specified input stream.
   * @param is The input stream to read from.
   * @return The integer value from the stream.
   */
  public static int readInt(InputStream is) throws IOException
  {
    ByteBuffer bb4 = getByteBuffer(4);
    for (int cnt = 0; cnt < 4;) {
      int n = is.read(bb4.array(), cnt, bb4.array().length - cnt);
      if (n < 0) {
        throw new IOException("End of stream");
      }
      cnt += n;
    }
    bb4.position(0);
    return bb4.getInt();
  }

  /**
   * Reads an unsigned integer (32 bit) from specified input stream.
   * @param is The input stream to read from.
   * @return The unsigned integer value from the stream.
   */
  public static long readUnsignedInt(InputStream is) throws IOException
  {
    return (long)readInt(is) & 0xffffffffL;
  }

  /**
   * Reads an 24 bit integer from specified input stream.
   * @param is The input stream to read from.
   * @return The 24 bit integer value from the stream.
   */
  public static int readInt24(InputStream is) throws IOException
  {
    return signExtend(readUnsignedInt24(is), 24);
  }

  /**
   * Reads an unsigned 24 bit integer from specified input stream.
   * @param is The input stream to read from.
   * @return The unsigned 24 bit integer value from the stream.
   */
  public static int readUnsignedInt24(InputStream is) throws IOException
  {
    ByteBuffer bb4 = getByteBuffer(4);
    for (int cnt = 0; cnt < 3;) {
      int n = is.read(bb4.array(), cnt, bb4.array().length - cnt - 1);
      if (n < 0) {
        throw new IOException("End of stream");
      }
      cnt += n;
    }
    bb4.position(0);
    return bb4.getInt() & 0xffffff;
  }

  /**
   * Sign extends the specified value consisting of specified number of bits.
   * @param value The value to sign-extend
   * @param bits Size of {@code value} in bits.
   * @return A sign-extended version of {@code value}.
   */
  public static int signExtend(int value, int bits)
  {
    return (value << (32 - bits)) >> (32 - bits);
  }

  /**
   * Reads a string of given length from the specified byte channel using the
   * default charset "windows-1252".
   * @param channel The {@link ReadableByteChannel} to read from.
   * @param length Number of bytes to read.
   * @return The resulting String.
   */
  public static String readString(ReadableByteChannel channel, int length) throws IOException
  {
    return readString(channel, length, Misc.CHARSET_DEFAULT);
  }

  /**
   * Reads a string of given length and character set from the specified byte channel.
   * @param channel The {@link ReadableByteChannel} to read from.
   * @param length Number of bytes to read.
   * @param charset The charset used to encode bytes into characters.
   * @return The resulting String.
   */
  public static String readString(ReadableByteChannel channel, int length, Charset charset) throws IOException
  {
    if (length > 0) {
      ByteBuffer bb = ByteBuffer.wrap(new byte[length]);
      for (int cnt = 0; cnt < length;) {
        int n = channel.read(bb);
        if (n < 0) {
          throw new IOException("End of stream");
        }
        cnt += n;
      }
      return new String(bb.array(), charset);
    } else {
      return "";
    }
  }

  public static String readString(ByteBuffer buffer, int length)
  {
    return readString(buffer, buffer.position(), length, Misc.CHARSET_DEFAULT);
  }

  /**
   * Reads a string of given length from the specified {@link ByteBuffer}, starting at the
   * specified offset using the default charset "windows-1252".
   * @param buffer The buffer to read from.
   * @param offset Start offset in buffer.
   * @param length Number of bytes to read.
   * @return The resulting String.
   */
  public static String readString(ByteBuffer buffer, int offset, int length)
  {
    return readString(buffer, offset, length, Misc.CHARSET_DEFAULT);
  }

  /**
   * Reads a string of given length and character set from the specified {@link ByteBuffer}.
   * @param buffer The buffer to read from.
   * @param length Number of bytes to read.
   * @param charset The charset used to encode bytes into characters.
   * @return The resulting String.
   */
  public static String readString(ByteBuffer buffer, int length, Charset charset)
  {
    return readString(buffer, buffer.position(), length, charset);
  }

  /**
   * Reads a string of given length and character set from the specified {@link ByteBuffer},
   * starting at the specified offset.
   * @param buffer The buffer to read from.
   * @param offset Start offset in buffer.
   * @param length Number of bytes to read.
   * @param charset The charset used to encode bytes into characters.
   * @return The resulting String.
   */
  public static String readString(ByteBuffer buffer, int offset, int length, Charset charset)
  {
    if (length > 0 && offset >= 0 && offset < buffer.limit()) {
      buffer.position(offset);
      length = Math.min(length, buffer.remaining());
      byte[] buf = new byte[length];
      buffer.get(buf);
      for (int i = 0; i < buf.length; i++) {
        if (buf[i] == 0) {
          length = i;
          break;
        }
      }
      return new String(buf, 0, length, charset);
    }
    return "";
  }

  /**
   * Reads a string of given length from the specified input stream, using the default
   * charset "windows-1252".
   * @param is The input stream to read from.
   * @param length Number of bytes to read.
   * @return The resulting String.
   */
  public static String readString(InputStream is, int length) throws IOException
  {
    return readString(is, length, Misc.CHARSET_DEFAULT);
  }

  /**
   * Reads a string of given length and charset from the specified input stream.
   * @param is The input stream to read from.
   * @param length Number of bytes to read.
   * @param charset The charset used to encode bytes into characters.
   * @return The resulting String.
   */
  public static String readString(InputStream is, int length, Charset charset) throws IOException
  {
    ByteBuffer buffer = readBytes(is, length);
    return readString(buffer, length, charset);
  }


  /**
   * Writes the buffer content to the output stream.
   * @param os The output stream.
   * @param buffer The buffer to write.
   */
  public static void writeBytes(OutputStream os, byte[] buffer) throws IOException
  {
    os.write(buffer);
  }

  /**
   * Writes the buffer content to the output stream.
   * @param os The output stream.
   * @param buffer The ByteBuffer.
   */
  public static void writeBytes(OutputStream os, ByteBuffer buffer) throws IOException
  {
    WritableByteChannel ch = Channels.newChannel(os);
    ch.write(buffer);
  }

  /**
   * Writes all available data from the input stream to the output stream.
   * @param os The output stream.
   * @param is The input stream
   * @return Actual number of bytes written.
   */
  public static long writeBytes(OutputStream os, InputStream is) throws IOException
  {
    return writeBytes(os, is, -1L);
  }

  /**
   * Writes all available data from the input stream to the output stream.
   * @param os The output stream.
   * @param is The input stream
   * @param length Max. number of bytes to write. Specify {@code -1L} to ignore length.
   * @return Actual number of bytes written.
   */
  public static long writeBytes(OutputStream os, InputStream is, long length) throws IOException
  {
    long retVal = 0L;
    byte[] buffer = new byte[8192];

    while (length < 0L || retVal < length) {
      int numBytes = (length < 0) ? buffer.length : (int)Math.min(buffer.length, length - retVal);
      int n = is.read(buffer, 0, numBytes);
      if (n < 0) {
        break;
      }
      os.write(buffer, 0, n);
      retVal += n;
    }

    return retVal;
  }

  /**
   * Writes the specified byte value 'count' times to the output stream.
   * @param os The output stream.
   * @param value The value to write.
   * @param count The number of times to write 'value'.
   */
  public static void writeBytes(OutputStream os, byte value, int count) throws IOException
  {
    while (count-- > 0) {
      os.write(value);
    }
  }

  /**
   * Writes a byte (8 bit) to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeByte(OutputStream out, byte value) throws IOException
  {
    int res = 0;
    if (out != null) {
      out.write(value);
      res++;
    }
    return res;
  }

  /**
   * Writes a short (16 bit) to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeShort(OutputStream out, short value) throws IOException
  {
    int res = 0;
    if (out != null) {
      for (int i = 0, shift = 0; i < 2; i++, shift+=8) {
        out.write((value >>> shift) & 0xff);
        res++;
      }
    }
    return res;
  }

  /**
   * Writes an integer (32 bit) to the specified byte channel.
   * @param channel The {@link WritableByteChannel} to write to.
   * @param value The value to write.
   */
  public static void writeInt(WritableByteChannel channel, int value) throws IOException
  {
    ByteBuffer bb4 = getByteBuffer(4);
    bb4.position(0);
    bb4.putInt(value);
    bb4.position(0);
    for (int cnt = 0; cnt < 4;) {
      int n = channel.write(bb4);
      cnt += n;
    }
  }

  /**
   * Writes an integer (32 bit) to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeInt(OutputStream out, int value) throws IOException
  {
    int res = 0;
    if (out != null) {
      for (int i = 0, shift = 0; i < 4; i++, shift+=8) {
        out.write((value >>> shift) & 0xff);
        res++;
      }
    }
    return res;
  }

  /**
   * Writes a 24 bit integer to specified output stream.
   * @param out The output stream to write to.
   * @param value The value to write.
   * @return The actual number of bytes written.
   */
  public static int writeInt24(OutputStream out, int value) throws IOException
  {
    int res = 0;
    if (out != null) {
      for (int i = 0, shift = 0; i < 3; i++, shift+=8) {
        out.write((value >>> shift) & 0xff);
        res++;
      }
    }
    return res;
  }

  /**
   * Writes at least "length" bytes of the specified string into the output stream using the
   * default charset "windows-1252".
   * @param os The output stream to write to.
   * @param s The string to write.
   * @param length The minimum number of bytes to write. A shorter string will be padded with null bytes.
   * @return The number of bytes written to the stream.
   */
  public static int writeString(OutputStream os, String s, int length) throws IOException
  {
    return writeString(os, s, length, Misc.CHARSET_DEFAULT);
  }

  /**
   * Writes at least "length" bytes of the specified string into the output stream.
   * @param os The output stream to write to.
   * @param s The string to write.
   * @param length The minimum number of bytes to write. A shorter string will be padded with null bytes.
   * @param charset The characet set used to convert characters into bytes.
   * @return The number of bytes written to the stream.
   */
  public static int writeString(OutputStream os, String s, int length, Charset charset) throws IOException
  {
    byte[] stringBytes = s.getBytes(charset);
    writeBytes(os, stringBytes);
    if (length > stringBytes.length) {
      byte buffer[] = new byte[length - stringBytes.length];
      writeBytes(os, buffer);
    }
    return Math.max(length, stringBytes.length);
  }

  /** Returns the content of {@code buffer} as byte array. */
  public static byte[] toArray(ByteBuffer buffer)
  {
    byte[] retVal = null;
    try {
      retVal = buffer.array();
    } catch (Throwable t) {
    }
    if (retVal == null || retVal.length != buffer.limit()) {
      retVal = new byte[buffer.limit()];
      int pos = buffer.position();
      buffer.position(0);
      buffer.get(retVal);
      buffer.position(pos);
    }
    return retVal;
  }

  /**
   * Creates a zip file out of the content of {@code sourceDir}.
   * @param sourceDir The source directory to pack.
   * @param zipFile Path to the resulting zip file.
   * @param includeFolder Whether to include the top level folder in the zip archive.
   * @throws IOException thrown on I/O related errors.
   */
  public static void createZip(Path sourceDir, Path zipFile, boolean includeFolder) throws IOException
  {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      Path baseDir = includeFolder ? sourceDir.getParent() : sourceDir;
      Files.walk(sourceDir)
        .filter(path -> !FileEx.create(path).isDirectory())
        .forEach(path -> {
          ZipEntry ze = new ZipEntry(baseDir.relativize(path).toString());
          try {
            ze.setLastModifiedTime(Files.getLastModifiedTime(path));
          } catch (IOException e) {
          }
          try {
            zos.putNextEntry(ze);
            Files.copy(path, zos);
            zos.closeEntry();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    }
  }
}
