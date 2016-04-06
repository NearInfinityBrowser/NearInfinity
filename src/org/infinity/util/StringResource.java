// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.infinity.resource.Profile;
import org.infinity.util.io.StreamUtils;

public final class StringResource
{
  private static final HashMap<Integer, StringEntry> cachedEntry = new HashMap<Integer, StringResource.StringEntry>(1000);

  private static Path dlgPath;
  private static ByteBuffer buffer;
  private static String version;
  private static int maxnr, startindex;
  private static Charset charset = Misc.CHARSET_DEFAULT;
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
    if (buffer != null) {
      buffer = null;
    }
    cachedEntry.clear();
  }

  /** Returns the {@link Path} instance of the dialog.tlk */
  public static Path getPath()
  {
    return dlgPath;
  }

  /** Returns the available number of strref entries in the dialog.tlk */
  public static int getMaxIndex()
  {
    return maxnr;
  }

  /** Returns whether the specified strref entry contains a sound resource. */
  public static boolean hasWavResource(int index)
  {
    try {
      StringEntry entry = fetchStringEntry(index);
      return (entry.soundRes != null && !entry.soundRes.isEmpty());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  /** Returns the resource name of the sound file associated with the specified strref entry. */
  public static String getWavResource(int index)
  {
    try {
      StringEntry entry = fetchStringEntry(index);
      if (entry.soundRes != null && !entry.soundRes.isEmpty()) {
        return entry.soundRes;
      }
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error reading " + dlgPath.getFileName().toString(),
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
   * @param extended If {@code true} adds the specified strref entry to the resulting string.
   * @param asPrefix Strref value is prepended (if {@code true}) or appended
   *                 (if {@code false}) to the string. Ignored if "extended" is {@code false}.
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
      JOptionPane.showMessageDialog(null, "Error reading " + dlgPath.getFileName().toString(),
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
  public static void init(Path dlgPath)
  {
    close();
    StringResource.dlgPath = dlgPath;
  }

  private static synchronized void open() throws IOException
  {
    if (buffer == null) {
      try (SeekableByteChannel ch = Files.newByteChannel(dlgPath, StandardOpenOption.READ)) {
        buffer = StreamUtils.getByteBuffer((int)ch.size());
        if (ch.read(buffer) < ch.size()) {
          throw new IOException();
        }
        buffer.position(0);
        String sig = StreamUtils.readString(buffer, 4);
        if (!sig.equals("TLK ")) {
          buffer = null;
          throw new IOException("Not valid TLK file");
        }
        version = StreamUtils.readString(buffer, 4);
        if (version.equals("V1  ")) {
          buffer.position(0x0a);
        } else {
          buffer = null;
          throw new IOException("Invalid TLK version");
        }
        maxnr = buffer.getInt();
        startindex = buffer.getInt();
        if (Profile.isEnhancedEdition()) {
          usedCharset = Misc.CHARSET_UTF8;
        }
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
        buffer.position(0x12 + index);
        type = buffer.getShort();
        byte[] buf = new byte[8];
        buffer.get(buf);
        int len = buf.length;
        for (int i = 0; i < buf.length; i++) {
          if (buf[i] == 0) {
            len = i;
            break;
          }
        }
        soundRes = new String(Arrays.copyOf(buf, len));
        volume = buffer.getInt();
        pitch = buffer.getInt();
        long offset = startindex + buffer.getInt();
        int length = buffer.getInt();
        buffer.position((int)offset);
        text = StreamUtils.readString(buffer, length, usedCharset);
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

