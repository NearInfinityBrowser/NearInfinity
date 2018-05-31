// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

/**
 * This class provides methods to determine character sets and conversions for selected
 * games and languages.
 */
public class CharsetDetector
{
  // Static list of dialog.tlk samples and associated character sets
  private static final List<Sample> samples = new ArrayList<>();

  // Only samples of non-standard codepages should be listed (i.e. all except windows-1252 and utf-8)
  static {
    // Polish BG1 (requires extra translation)
    samples.add(new Sample("windows-1250", 7, new byte[]{
        (byte)0x44, (byte)0x6F, (byte)0x73, (byte)0x6B, (byte)0x6F, (byte)0x6E,
        (byte)0x61, (byte)0x6C, (byte)0x65, (byte)0x2C, (byte)0x20, (byte)0x77,
        (byte)0x79, (byte)0xF6, (byte)0x79, (byte)0x77, (byte)0x61, (byte)0x6A,
        (byte)0x20, (byte)0x73, (byte)0x69, (byte)0xF0, (byte)0x20, (byte)0x6A,
        (byte)0x61, (byte)0x6B, (byte)0x20, (byte)0x70, (byte)0x6F, (byte)0x74,
        (byte)0x72, (byte)0x61, (byte)0x66, (byte)0x69, (byte)0x73, (byte)0x7A,
        (byte)0x2C, (byte)0x20, (byte)0x73, (byte)0x74, (byte)0x61, (byte)0x72,
        (byte)0x79, (byte)0x20, (byte)0x70, (byte)0x69, (byte)0x65, (byte)0x72,
        (byte)0x6E, (byte)0x69, (byte)0x6B, (byte)0x75, (byte)0x2E, (byte)0x0D,
        (byte)0x0A},
        // uses a custom translation table for selected character codes
        new CharLookup(new String(new byte[]{(byte)0xe5, (byte)0xe6, (byte)0xe7, (byte)0xe8, (byte)0xe9, (byte)0xea,
                                             (byte)0xeb, (byte)0xec, (byte)0xed, (byte)0xee, (byte)0xef, (byte)0xf0,
                                             (byte)0xf1, (byte)0xf2, (byte)0xf4, (byte)0xf5, (byte)0xf6}, Charset.forName("windows-1250")),
                       new String(new byte[]{(byte)0xa5, (byte)0xc6, (byte)0xca, (byte)0xa3, (byte)0xd3, (byte)0xd1,
                                             (byte)0x8c, (byte)0x8f, (byte)0xaf, (byte)0xb9, (byte)0xe6, (byte)0xea,
                                             (byte)0xb3, (byte)0xf1, (byte)0x9c, (byte)0x9f, (byte)0xbf}, Charset.forName("windows-1250")),
                       new int[]{10249, 20186, 20714})));
    // Polish BG2
    samples.add(new Sample("windows-1250", 2, new byte[]{
        (byte)0x47, (byte)0x72, (byte)0x61, (byte)0xB3, (byte)0x65, (byte)0x9C,
        (byte)0x20, (byte)0x45, (byte)0x6C, (byte)0x6D, (byte)0x69, (byte)0x6E,
        (byte)0x73, (byte)0x74, (byte)0x65, (byte)0x72, (byte)0x61, (byte)0x3F}, null));
    // Polish IWD
    samples.add(new Sample("windows-1250", 9, new byte[]{(
        byte)0x53, (byte)0x74, (byte)0x72, (byte)0x61, (byte)0xBF, (byte)0x6E,
        (byte)0x69, (byte)0x6B}, null));
    // Polish IWD2
    samples.add(new Sample("windows-1250", 9, new byte[]{
        (byte)0x43, (byte)0x68, (byte)0x61, (byte)0x68, (byte)0x6F, (byte)0x70,
        (byte)0x65, (byte)0x6B, (byte)0x20, (byte)0x53, (byte)0x74, (byte)0x72,
        (byte)0x61, (byte)0xBF, (byte)0x6E, (byte)0x69, (byte)0x6B}, null));
    // Polish PST
    samples.add(new Sample("windows-1250", 8, new byte[]{
        (byte)0x4E, (byte)0x69, (byte)0x65, (byte)0x73, (byte)0x6B, (byte)0x61,
        (byte)0x6C, (byte)0x61, (byte)0x6E, (byte)0x79, (byte)0x20, (byte)0x67,
        (byte)0x6F, (byte)0x72, (byte)0x73, (byte)0x65, (byte)0x74, (byte)0x20,
        (byte)0x4E, (byte)0x69, (byte)0x65, (byte)0x2D, (byte)0x53, (byte)0xB3,
        (byte)0x61, (byte)0x77, (byte)0x79}, null));
    // Czech BG1 (TODO: Confirm!)
    samples.add(new Sample("windows-1250", 1, new byte[]{
        (byte)0x44, (byte)0x6F, (byte)0x73, (byte)0x6B, (byte)0x6F, (byte)0x6E,
        (byte)0x61, (byte)0x6C, (byte)0x65, (byte)0x2C, (byte)0x20, (byte)0x77,
        (byte)0x79, (byte)0xF6, (byte)0x79, (byte)0x77, (byte)0x61, (byte)0x6A,
        (byte)0x20, (byte)0x73, (byte)0x69, (byte)0xF0, (byte)0x20, (byte)0x6A,
        (byte)0x61, (byte)0x6B, (byte)0x20, (byte)0x70, (byte)0x6F, (byte)0x74,
        (byte)0x72, (byte)0x61, (byte)0x66, (byte)0x69, (byte)0x73, (byte)0x7A,
        (byte)0x2C, (byte)0x20, (byte)0x73, (byte)0x74, (byte)0x61, (byte)0x72,
        (byte)0x79, (byte)0x20, (byte)0x70, (byte)0x69, (byte)0x65, (byte)0x72,
        (byte)0x6E, (byte)0x69, (byte)0x6B, (byte)0x75, (byte)0x2E, (byte)0x0D,
        (byte)0x0A}, null));
    // TODO: Czech BG2
    // Czech IWD (TODO: Confirm!)
    samples.add(new Sample("windows-1250", 10, new byte[]{
        (byte)0x52, (byte)0x6F, (byte)0x68, (byte)0xE1, (byte)0xE8, (byte)0x20,
        (byte)0x6F, (byte)0x62, (byte)0xF8, (byte)0xED}, null));
    // TODO: Czech IWD2
    // TODO: Czech PST
    // Hungarian BG1 (TODO: Confirm!)
    samples.add(new Sample("windows-1250", 6, new byte[]{
        (byte)0x44, (byte)0x65, (byte)0x20, (byte)0xE9, (byte)0x6E, (byte)0x20,
        (byte)0x73, (byte)0x65, (byte)0x6D, (byte)0x6D, (byte)0x69, (byte)0x20,
        (byte)0x72, (byte)0x6F, (byte)0x73, (byte)0x73, (byte)0x7A, (byte)0x61,
        (byte)0x74, (byte)0x20, (byte)0x6E, (byte)0x65, (byte)0x6D, (byte)0x20,
        (byte)0x74, (byte)0x65, (byte)0x74, (byte)0x74, (byte)0x65, (byte)0x6D,
        (byte)0x21, (byte)0x20, (byte)0x4D, (byte)0x69, (byte)0xE9, (byte)0x72,
        (byte)0x74, (byte)0x20, (byte)0x76, (byte)0xE1, (byte)0x64, (byte)0x6F,
        (byte)0x6C, (byte)0x73, (byte)0x7A, (byte)0x3F}, null));
    // Russian BG1 (TODO: Confirm!)
    samples.add(new Sample("windows-1251", 6, new byte[]{
        (byte)0xCD, (byte)0xEE, (byte)0x20, (byte)0xFF, (byte)0x20, (byte)0xED,
        (byte)0xE5, (byte)0x20, (byte)0xF1, (byte)0xE4, (byte)0xE5, (byte)0xEB,
        (byte)0xE0, (byte)0xEB, (byte)0x20, (byte)0xED, (byte)0xE8, (byte)0xF7,
        (byte)0xE5, (byte)0xE3, (byte)0xEE, (byte)0x20, (byte)0xEF, (byte)0xEB,
        (byte)0xEE, (byte)0xF5, (byte)0xEE, (byte)0xE3, (byte)0xEE, (byte)0x21,
        (byte)0x20, (byte)0xCF, (byte)0xEE, (byte)0xF7, (byte)0xE5, (byte)0xEC,
        (byte)0xF3, (byte)0x20, (byte)0xF2, (byte)0xFB, (byte)0x20, (byte)0xEE,
        (byte)0xE1, (byte)0xE2, (byte)0xE8, (byte)0xED, (byte)0xFF, (byte)0xE5,
        (byte)0xF8, (byte)0xFC, (byte)0x20, (byte)0xEC, (byte)0xE5, (byte)0xED,
        (byte)0xFF, (byte)0x20, (byte)0xE2, (byte)0x20, (byte)0xFD, (byte)0xF2,
        (byte)0xEE, (byte)0xEC, (byte)0x3F}, null));
    // TODO: Russian BG2
    // Russian IWD (TODO: Confirm!)
    samples.add(new Sample("windows-1251", 10, new byte[]{
        (byte)0xC6, (byte)0xF3, (byte)0xEA, (byte)0x2D, (byte)0xED, (byte)0xEE,
        (byte)0xF1, (byte)0xEE, (byte)0xF0, (byte)0xEE, (byte)0xE3}, null));
    // TODO: Russian IWD2
    // TODO: Russian PST
    // Turkish BG1 (TODO: Confirm!)
    samples.add(new Sample("windows-1254", 14, new byte[]{
        (byte)0x42, (byte)0x69, (byte)0x7A, (byte)0x65, (byte)0x20, (byte)0x79,
        (byte)0x61, (byte)0x72, (byte)0x64, (byte)0xFD, (byte)0x6D, (byte)0x20,
        (byte)0x65, (byte)0x74, (byte)0x74, (byte)0x69, (byte)0xF0, (byte)0x69,
        (byte)0x6E, (byte)0x20, (byte)0x69, (byte)0xE7, (byte)0x69, (byte)0x6E,
        (byte)0x20, (byte)0x74, (byte)0x65, (byte)0xFE, (byte)0x65, (byte)0x6B,
        (byte)0x6B, (byte)0xFC, (byte)0x72, (byte)0x20, (byte)0x65, (byte)0x64,
        (byte)0x65, (byte)0x72, (byte)0x69, (byte)0x7A, (byte)0x2E}, null));
  }

  private static String charset = null;
  private static CharLookup lookup = null;

  // not needed
  protected CharsetDetector() {}

  /** Resets autodetection of character set */
  public static void clearCache()
  {
    charset = null;
    lookup = null;
  }

  /**
   * Attempts to determine the right character set used by the current game.
   * @param detect {@code false} to return the default charset based on EE or non-EE.
   *               {@code true} to determine charset from the dialog.tlk based on sample data.
   * @return the assumed or detected character set.
   */
  public static String guessCharset(boolean detect)
  {
    if (charset != null) {
      return charset;
    }

    String retVal = Profile.isEnhancedEdition() ? Misc.CHARSET_UTF8.name() : Misc.CHARSET_DEFAULT.name();
    if (!Profile.isEnhancedEdition() && detect) {
      // finding dialog.tlk
      Path tlkFile = Profile.getProperty(Profile.Key.GET_GAME_DIALOG_FILE);
      if (tlkFile == null) {
        tlkFile = FileManager.queryExisting(Profile.getGameRoot(), "dialog.tlk");
      }

      if (tlkFile != null) {
        try (FileChannel ch = FileChannel.open(tlkFile, StandardOpenOption.READ)) {
          String sig = StreamUtils.readString(ch, 8);
          if ("TLK V1  ".equals(sig)) {
            ch.position(0x0a);
            int numEntries = StreamUtils.readInt(ch);
            int ofsStrings = StreamUtils.readInt(ch);

            // cycling through all available string samples to find a match
            for (final Sample sample: samples) {
              if (sample.strref < numEntries) {
                int ofs = 0x12 + (sample.strref * 0x1a);
                int lenString = StreamUtils.readInt(ch.position(ofs + 0x16));
                if (lenString > 0) {
                  int ofsString = ofsStrings + StreamUtils.readInt(ch.position(ofs + 0x12));
                  ByteBuffer buf = StreamUtils.getByteBuffer(lenString);
                  buf.position(0);
                  ch.position(ofsString).read(buf);
                  if (Arrays.equals(buf.array(), sample.data)) {
                    retVal = sample.charset;
                    charset = retVal;
                    lookup = sample.lookup;
                    break;
                  }
                }
              }
            }
          }
          ch.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  /**
   * Marks the specified charset as active charset.
   * @param charsetName The charset to activate.
   * @return Charset name.
   */
  public static String setCharset(String charsetName)
  {
    if (charset == null) {
      try {
        Charset.forName(charsetName);
      } catch (Throwable t) {
        charsetName = Misc.CHARSET_DEFAULT.name();
      }
      charset = charsetName;
    }

    return charset;
  }

  /**
   * Returns the translation table needed for the current dialog.tlk data. This method will only
   * return a meaningful translation table for Polish BG1. Any other game and language uses an
   * official character set.
   * @return A translation table to convert characters to and from an official character set.
   */
  public static CharLookup getLookup()
  {
    if (lookup == null) {
      guessCharset(true);
      lookup = new CharLookup();
    }
    return lookup;
  }

//-------------------------- INNER CLASSES --------------------------

  // Handles character decoding and encoding
  public static class CharLookup
  {
    private final String encoded, decoded;
    private final int[] excluded;

    /** Initializes an empty {@code CharLookup} object. */
    public CharLookup()
    {
      this(null, null, null);
    }

    /**
     * Initializes translation tables.
     * @param encoded Contains sequence of characters to decode.
     * @param decoded Contains sequence of corresponding decoded characters.
     * @param excludedStrrefs List of strrefs that should not be decoded.
     */
    public CharLookup(String encoded, String decoded, int[] excludedStrrefs)
    {
      if (encoded == null) encoded = "";
      if (decoded == null) decoded = "";
      if (excludedStrrefs == null) excludedStrrefs = new int[]{};
      if (decoded.length() < encoded.length()) {
        encoded = encoded.substring(0, decoded.length());
      } else if (encoded.length() < decoded.length()) {
        decoded = decoded.substring(0, encoded.length());
      }
      this.encoded = encoded;
      this.decoded = decoded;
      this.excluded = excludedStrrefs;
    }

    /** Returns whether the specified string is excluded from the encoding process. */
    public boolean isExcluded(int strref)
    {
      for (int i = 0; i < excluded.length; i++) {
        if (excluded[i] == strref) {
          return true;
        }
      }
      return false;
    }

    /** Decodes the specified string into an official charset. */
    public String decodeString(String text)
    {
      if (!encoded.isEmpty()) {
        StringBuilder sb = new StringBuilder(text);
        for (int idx = 0, cnt = sb.length(); idx < cnt; idx++) {
          sb.setCharAt(idx, decode(sb.charAt(idx)));
        }
        text = sb.toString();
      }
      return text;
    }

    /** Encodes the specified string with the character translation table. */
    public String encodeString(String text)
    {
      if (!decoded.isEmpty()) {
        StringBuilder sb = new StringBuilder(text);
        for (int idx = 0, cnt = sb.length(); idx < cnt; idx++) {
          sb.setCharAt(idx, encode(sb.charAt(idx)));
        }
        text = sb.toString();
      }
      return text;
    }

    /** Decodes a single character. */
    public char decode(char ch)
    {
      int idx = encoded.indexOf(ch);
      if (idx >= 0) {
        ch = decoded.charAt(idx);
      }
      return ch;
    }

    /** Encodes a single character. */
    public char encode(char ch)
    {
      int idx = decoded.indexOf(ch);
      if (idx >= 0) {
        ch = encoded.charAt(idx);
      }
      return ch;
    }

  }

  // Stores a single string -> charset pair
  private static class Sample
  {
    public CharLookup lookup;
    public String charset;
    public int strref;
    public byte[] data;

    public Sample(String charset, int strref, byte[] data, CharLookup lookup)
    {
      this.charset = charset;
      this.strref = strref;
      this.data = data;
      this.lookup = (lookup != null) ? lookup : new CharLookup();
    }
  }
}
