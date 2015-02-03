// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.resource.ResourceFactory;
import infinity.util.io.FileReaderNI;
import infinity.util.io.RandomAccessFileNI;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.swing.JOptionPane;

public final class StringResource
{
  private static File ffile;
  private static RandomAccessFile file;
  private static String version;
  private static int maxnr, startindex;
  private static Charset cp1252Charset = Charset.forName("windows-1252");
  private static Charset utf8Charset = Charset.forName("utf8");
  private static Charset charset = cp1252Charset;
  private static Charset usedCharset = charset;

  /** Returns the charset used to decode strings of the string resource. */
  public static Charset getCharset() {
    return charset;
  }

  /** Specify the charset used to decode strings of the string resource. */
  public static void setCharset(String cs) {
    charset = Charset.forName(cs);
    usedCharset = charset;
  }

  /** Explicitly closes the dialog.tlk file handle. */
  public static void close()
  {
    if (file == null) return;
    try {
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    file = null;
  }

  /** Returns the File instance of the dialog.tlk */
  public static File getFile()
  {
    return ffile;
  }

  /** Returns the available number of strref entries in the dialog.tlk */
  public static int getMaxIndex()
  {
    return maxnr;
  }

  /** Returns the resource name of the sound file associated with the specified strref entry. */
  public static String getResource(int index)
  {
    try {
      if (file == null)
        open();
      if (index >= maxnr || index == 0xffffffff) return null;
      byte buffer[] = null;
      if (version.equalsIgnoreCase("V1  ")) {
        index *= 0x1a;
        file.seek((long)(0x12 + index + 0x02));
        buffer = new byte[8];
      }
      else if (version.equalsIgnoreCase("V3.0")) {
        index *= 0x28;
        file.seek((long)(0x14 + index + 0x04));
        buffer = new byte[16];
      }
      file.readFully(buffer);
      if (buffer[0] == 0)
        return null;
      int max = buffer.length;
      for (int i = 0; i < buffer.length; i++) {
        if (buffer[i] == 0x00) {
          max = i;
          break;
        }
      }
      if (max != buffer.length)
        buffer = Arrays.copyOfRange(buffer, 0, max);
      return new String(buffer);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error reading " + ffile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    return null;
  }

  /** Returns the string of the specified sttref entry. */
  public static String getStringRef(int index)
  {
    return getStringRef(index, false);
  }

  /** Returns the string of the specified sttref entry, optionally with an appended strref value. */
  public static String getStringRef(int index, boolean extended)
  {
    return getStringRef(index, extended, false);
  }

  /**
   * Returns the string of the specified sttrref entry. Optionally adds the specified
   * Strref entry to the returned string.
   * @param index The strref entry
   * @param extended If <code>true</code> adds the specified strref entry to the resulting string.
   * @param asPrefix Strref value is prepended (if <code>true</code>) or appended
   *                 (if <code>false</code>) to the string. Ignored if "extended" is <code>false</code>.
   * @return The string optionally including the strref entry.
   */
  public static String getStringRef(int index, boolean extended, boolean asPrefix)
  {
    final String fmtResult;
    if (extended) {
      fmtResult = asPrefix ? "(Strref: %2$d) %1$s" : "%1$s (Strref: %2$d)";
    } else {
      fmtResult = "%1$s";
    }

    int strref = index;
    try {
      if (file == null)
        open();
      if (index >= maxnr || index < 0) return String.format(fmtResult, "No such index", strref);
      if (version.equalsIgnoreCase("V1  ")) {
        index *= 0x1A;
        file.seek((long)(0x12 + index + 0x12));
      }
      else if (version.equalsIgnoreCase("V3.0")) {
        index *= 0x28;
        file.seek((long)(0x14 + index + 0x1C));
      }
      int offset = startindex + FileReaderNI.readInt(file);
      int length = FileReaderNI.readInt(file);
      file.seek((long)offset);
      return String.format(fmtResult, FileReaderNI.readString(file, length, usedCharset), strref);
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error reading " + ffile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    return "Error";
  }

  /** Specify a new dialog.tlk. */
  public static void init(File ffile)
  {
    close();
    StringResource.ffile = ffile;
  }

  private static void open() throws IOException
  {
    file = new RandomAccessFileNI(ffile, "r");
    file.seek((long)0x00);
    String signature = FileReaderNI.readString(file, 4);
    if (!signature.equalsIgnoreCase("TLK "))
      throw new IOException("Not valid TLK file");
    version = FileReaderNI.readString(file, 4);
    if (version.equalsIgnoreCase("V1  "))
      file.seek((long)0x0A);
    else if (version.equalsIgnoreCase("V3.0"))
      file.seek((long)0x0C);
    maxnr = FileReaderNI.readInt(file);
    startindex = FileReaderNI.readInt(file);
    if (ResourceFactory.isEnhancedEdition()) {
      usedCharset = utf8Charset;
    }
  }

  private StringResource(){}
}

