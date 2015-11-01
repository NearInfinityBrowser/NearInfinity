// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.resource.Profile;
import infinity.util.io.FileReaderNI;
import infinity.util.io.RandomAccessFileNI;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JOptionPane;

public final class StringResource
{
  private static final HashMap<Integer, StringEntry> cachedEntry = new HashMap<Integer, StringResource.StringEntry>(1000);
  private static final Charset cp1252Charset = Charset.forName("windows-1252");
  private static final Charset utf8Charset = Charset.forName("utf8");

  private static File ffile;
  private static RandomAccessFile file;
  private static String version;
  private static int maxnr, startindex;
  private static Charset charset = cp1252Charset;
  private static Charset usedCharset = charset;

  /** Returns the charset used to decode strings of the string resource. */
  public static Charset getCharset() {
    return charset;
  }

  /** Specify the charset used to decode strings of the string resource. */
  public static synchronized void setCharset(String cs) {
    charset = Charset.forName(cs);
    usedCharset = charset;
  }

  /** Explicitly closes the dialog.tlk file handle. */
  public static synchronized void close()
  {
    if (file != null) {
      try {
        file.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      file = null;
    }
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
  public static String getWavResource(int index)
  {
    try {
      StringEntry entry = fetchStringEntry(index);
      return entry.soundRes;
    } catch (IOException e) {
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
   * Returns the string of the specified strref entry. Optionally adds the specified
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

    try {
      StringEntry entry = fetchStringEntry(index);
      if (entry != null) {
        return String.format(fmtResult, entry.text, entry.strref);
      }
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error reading " + ffile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    return "Error";
  }

  /** Returns message type of specified strref. */
  public short getFlags(int index)
  {
    try {
      StringEntry entry = fetchStringEntry(index);
      if (entry != null) {
        return entry.type;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /** Returns volume variance of the specified strref. */
  public int getVolume(int index)
  {
    try {
      StringEntry entry = fetchStringEntry(index);
      if (entry != null) {
        return entry.volume;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /** Returns pitch variance of the specified strref. */
  public int getPitch(int index)
  {
    try {
      StringEntry entry = fetchStringEntry(index);
      if (entry != null) {
        return entry.pitch;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /** Specify a new dialog.tlk. */
  public static void init(File ffile)
  {
    close();
    StringResource.ffile = ffile;
  }

  private static synchronized void open() throws IOException
  {
    if (file == null) {
      file = new RandomAccessFileNI(ffile, "r");
      file.seek((long)0x00);
      String signature = FileReaderNI.readString(file, 4);
      if (!signature.equalsIgnoreCase("TLK "))
        throw new IOException("Not valid TLK file");
      version = FileReaderNI.readString(file, 4);
      if (version.equalsIgnoreCase("V1  ")) {
        file.seek((long)0x0A);
      } else {
        file.close();
        file = null;
        throw new IOException("Invalid TLK version");
      }
      maxnr = FileReaderNI.readInt(file);
      startindex = FileReaderNI.readInt(file);
      if (Profile.isEnhancedEdition()) {
        usedCharset = utf8Charset;
      }
    }
  }

  private static synchronized StringEntry fetchStringEntry(int index) throws IOException
  {
    StringEntry entry = cachedEntry.get(Integer.valueOf(index));
    if (entry == null) {
      entry = new StringEntry(index);
      cachedEntry.put(Integer.valueOf(index), entry);
    }
    return entry;
  }

  private StringResource(){}


//-------------------------- INNER CLASSES --------------------------

  private static class StringEntry
  {
    public final int strref;
    public final short type;
    public final String soundRes;
    public final int volume, pitch;
    public final String text;

    private StringEntry(int index) throws IOException
    {
      open();
      if (index >= 0 && index < maxnr ) {
        strref = index;
        index *= 0x1a;
        file.seek((long)(0x12 + index));
        type = FileReaderNI.readShort(file);
        byte[] buffer = new byte[8];
        file.read(buffer);
        int len = buffer.length;
        for (int i = 0; i < buffer.length; i++) {
          if (buffer[i] == 0) {
            len = i;
            break;
          }
        }
        soundRes = new String(Arrays.copyOf(buffer, len));
        volume = FileReaderNI.readInt(file);
        pitch = FileReaderNI.readInt(file);
        long offset = startindex + FileReaderNI.readInt(file);
        int length = FileReaderNI.readInt(file);
        file.seek(offset);
        text = FileReaderNI.readString(file, length, usedCharset);
      } else {
        strref = -1;
        type = 0;
        volume = pitch = 0;
        soundRes = null;
        text = "No such index";
      }
    }
  }
}

