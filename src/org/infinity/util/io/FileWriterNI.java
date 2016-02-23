// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import org.infinity.util.DynamicArray;

/**
 * Overrides java.io.FileWriter to support case sensitive file systems.
 * Contains additional static methods for writing data to streams.
 */
public class FileWriterNI extends FileWriter
{
  public FileWriterNI(String fileName) throws IOException
  {
    super(FileLookup.getInstance().queryFilePath(fileName));
  }

  public FileWriterNI(File file) throws IOException
  {
    super(FileLookup.getInstance().queryFile(file));
  }

  public FileWriterNI(FileDescriptor fd)
  {
    // No wrapper needed
    super(fd);
  }

  public FileWriterNI(String fileName, boolean append) throws IOException
  {
    super(FileLookup.getInstance().queryFilePath(fileName), append);
  }

  public FileWriterNI(File file, boolean append) throws IOException
  {
    super(FileLookup.getInstance().queryFile(file), append);
  }

  /**
   * Writes the buffer content to the output stream.
   * @param os The output stream.
   * @param buffer The buffer to write.
   */
  public static void writeBytes(OutputStream os, byte buffer[]) throws IOException
  {
    os.write(buffer);
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

  public static void writeInt(RandomAccessFile ranfile, int b) throws IOException
  {
    ranfile.write(DynamicArray.convertInt(b));
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

  public static void writeString(OutputStream os, String s, int length) throws IOException
  {
    writeString(os, s, length, Charset.forName("windows-1252")); // TODO: reevalute the need for hardcoded charset
  }

  public static void writeString(OutputStream os, String s, int minLength, Charset charset) throws IOException
  {
    byte[] stringBytes = s.getBytes(charset);
    writeBytes(os, stringBytes);
    if (minLength > stringBytes.length) {
      byte buffer[] = new byte[minLength - stringBytes.length];
      writeBytes(os, buffer);
    }
  }
}
