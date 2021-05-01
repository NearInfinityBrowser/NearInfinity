// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.StringEditor;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.updater.Utils;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;

/**
 * Provides operations for reading, writing and querying information about string tables.
 */
public class StringTable
{
  /** Shorthand selector for TLK files. */
  public enum Type {
    /** Refers to the male dialog.tlk */
    MALE,
    /** Refers to the female dialogf.tlk (only available for specific languages) */
    FEMALE,
  }

  /** Defines available string formats. */
  public enum Format {
    /** String is returned without further modifications. */
    NONE("%1$s"),
    /** Strref value is added in front of the returned string. */
    STRREF_PREFIX("(Strref: %2$d) %1$s"),
    /** Strref value is added behind the returned string. */
    STRREF_SUFFIX("%1$s (Strref: %2$d)"),
    /** Strref value is added in front of the returned string (without additional text). */
    STRREF_PREFIX_SHORT("%2$d : %1$s"),
    /** Strref value is added behind the returned string (without additional text). */
    STRREF_SUFFIX_SHORT("%1$s : %2$d");

    /**
     * Format string for {@link String#format} method. Can have up to 2 arguments:
     * first - localized text, second - integer, representing StrRef.
     */
    final String format;

    private Format(String format)
    {
      this.format = format;
    }

    public String format(String text, int strRef)
    {
      return String.format(format, text, strRef);
    }
  }

  /** String entry flag: Text is used if available */
  public static final short FLAGS_HAS_TEXT  = 0x01;
  /** String entry flag: Associated sound reference is used if available */
  public static final short FLAGS_HAS_SOUND = 0x02;
  /** String entry flag: Available tokens will be resolved */
  public static final short FLAGS_HAS_TOKEN = 0x04;
  /** The default flags value includes all supported bits */
  public static final short FLAGS_DEFAULT   = 0x07;

  /** Strref start index for virtual strings referenced by ENGINEST.2DA (EE only) */
  public static final int STRREF_VIRTUAL = 0xf00000;

  private static final EnumMap<Type, StringTable> TLK_TABLE = new EnumMap<>(Type.class);

  private static Charset charset = null;
  private static Format format = Format.NONE;
  private static Boolean hasFemaleTable = null;

  /**
   * Returns whether the current language provides a separate string table for female text.
   * <b>Important:</b> This is the only way to determine whether a method deals with the female
   * string table when {@code Type.FEMALE} is specified.
   * @return {@code true} if female string table exists, {@code false} otherwise.
   */
  public static boolean hasFemaleTable()
  {
    if (hasFemaleTable == null) {
      hasFemaleTable = Boolean.valueOf(Profile.getProperty(Profile.Key.GET_GAME_DIALOGF_FILE) != null);
    }
    return hasFemaleTable;
  }

  public static Charset getCharset()
  {
    if (charset == null) {
      try {
        setCharset(BrowserMenuBar.getInstance().getSelectedCharset());
      } catch (Throwable t) {
        // returns a temporary value if BrowserMenuBar has not yet been initialized
        return Profile.isEnhancedEdition() ? Misc.CHARSET_UTF8 : Misc.CHARSET_DEFAULT;
      }
    }
    return charset;
  }

  public static boolean setCharset(String cs)
  {
    boolean retVal = false;

    if (cs != null) {
      try {
        Charset tmp = Charset.forName(cs);
        if (!tmp.equals(charset)) {
          if (charset != null) {
            reset(Type.MALE);
            reset(Type.FEMALE);
          }
          charset = tmp;
          retVal = true;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }

  /** Returns the current display format of returned strings. */
  public static Format getDisplayFormat()
  {
    return format;
  }

  /** Specify a new display format for returned strings. */
  public static void setDisplayFormat(Format newFormat)
  {
    if (newFormat == null) { newFormat = Format.NONE; }
    format = newFormat;
  }

  /**
   * Removes all cached string tables and resets global variables.
   * Call this after opening a new game.
   */
  public static void resetAll()
  {
    synchronized (TLK_TABLE) {
      TLK_TABLE.clear();
    }
    charset = null;
    hasFemaleTable = null;
  }

  /** Resets a single string table. Call this when refreshing an opened game. */
  public static void reset(Type type)
  {
    if (type != null) {
      StringTable table = TLK_TABLE.get(type);
      if (table != null) {
        table._reset();
      }
    }
  }

  /**
   * Resets all modified entries in the specified string table.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   */
  public static void resetModified(Type type)
  {
    instance(type)._resetEntries();
  }

  /**
   * Makes sure that current dialog.tlk and dialogf.tlk contain the same number of string entries.
   * Returns number of string entries added to dialogf.tlk. Returns negative number if entries
   * have been added to dialog.tlk instead. Returns 0 if both files contain same amount of entries.
   */
  public static int syncTables()
  {
    int retVal = 0;

    if (hasFemaleTable()) {
      int numEntriesMale = getNumEntries(Type.MALE);
      int numEntriesFemale = getNumEntries(Type.FEMALE);
      retVal = numEntriesMale - numEntriesFemale;

      // syncing female string table
      while (numEntriesFemale < numEntriesMale) {
        int index = addEntry(Type.FEMALE);
        if (index >= 0) {
          setStringRef(Type.FEMALE, index, getStringRef(Type.MALE, index));
          setSoundResource(Type.FEMALE, index, getSoundResource(Type.MALE, index));
          setFlags(Type.FEMALE, index, getFlags(Type.MALE, index));
          setVolume(Type.FEMALE, index, getVolume(Type.MALE, index));
          setPitch(Type.FEMALE, index, getVolume(Type.MALE, index));
          numEntriesFemale++;
        } else {
          throw new Error("Critical error while adding new string entry to dialogf.tlk");
        }
      }

      // syncing male string table
      while (numEntriesMale < numEntriesFemale) {
        int index = addEntry(Type.MALE);
        if (index >= 0) {
          setStringRef(Type.MALE, index, getStringRef(Type.FEMALE, index));
          setSoundResource(Type.MALE, index, getSoundResource(Type.FEMALE, index));
          setFlags(Type.MALE, index, getFlags(Type.FEMALE, index));
          setVolume(Type.MALE, index, getVolume(Type.FEMALE, index));
          setPitch(Type.MALE, index, getVolume(Type.FEMALE, index));
          numEntriesMale++;
        } else {
          throw new Error("Critical error while adding new string entry to dialog.tlk");
        }
      }
    }

    return retVal;
  }

  /** Returns the path to the male string table. */
  public static Path getPath()
  {
    return getPath(Type.MALE);
  }

  /**
   * Returns the path to the string table of specified type.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   */
  public static Path getPath(Type type)
  {
    return instance(type)._getPath();
  }

  /** Return language id of male string table. */
  public static int getLanguageId()
  {
    return getLanguageId(Type.MALE);
  }

  /**
   * Return language id of specified string table.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   */
  public static int getLanguageId(Type type)
  {
    return instance(type)._getLanguageId();
  }

  /**
   * Returns whether the specified string reference points to a valid string entry.
   * @param index The string reference to check.
   * @return {@code true} if valid, {@code false} otherwise.
   */
  public static boolean isValidStringRef(int index)
  {
    return (getStringEntry(Type.MALE, index) != StringEntry.INVALID);
  }

  /** Returns number of string entries in male string table. */
  public static int getNumEntries()
  {
    return getNumEntries(Type.MALE);
  }

  /**
   * Returns number of string entries in string table of specified type.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   */
  public static int getNumEntries(Type type)
  {
    return instance(type)._getNumEntries();
  }

  /**
   * Returns the male string of the given index using default formatting.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static String getStringRef(int index) throws IndexOutOfBoundsException
  {
    return getStringRef(Type.MALE, index);
  }

  /**
   * Returns the string of the given index of specified type using default formatting.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static String getStringRef(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._getStringRef(index, getDisplayFormat());
  }

  /**
   * Returns the male string of the given index using specified formatting.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static String getStringRef(int index, Format fmt) throws IndexOutOfBoundsException
  {
    return getStringRef(Type.MALE, index, fmt);
  }

  /**
   * Returns the string of the given index and type using specified formatting.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static String getStringRef(Type type, int index, Format fmt) throws IndexOutOfBoundsException
  {
    return instance(type)._getStringRef(index, fmt);
  }

  /**
   * Translates virtual string references into real string references.
   * Returns <pre>index</pre> as is, otherwise.
   */
  public static int getTranslatedIndex(int index)
  {
    if (index >= STRREF_VIRTUAL) {
      index = instance(Type.MALE)._getTranslatedIndex(index);
    }
    return index;
  }

  /**
   * Sets the specified text to the male string entry.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setStringRef(int index, String text) throws IndexOutOfBoundsException
  {
    setStringRef(Type.MALE, index, text);
    if (hasFemaleTable()) {
      setStringRef(Type.FEMALE, index, text);
    }
  }

  /**
   * Sets the specified text to the string entry.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setStringRef(Type type, int index, String text) throws IndexOutOfBoundsException
  {
    instance(type)._setStringRef(index, text);
  }

  /**
   * Returns the sound reference (without extension) of the specified male string.
   * Returns empty string if no reference is defined.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static String getSoundResource(int index) throws IndexOutOfBoundsException
  {
    return getSoundResource(Type.MALE, index);
  }

  /**
   * Returns the sound reference (without extension) of the specified string.
   * Returns empty string if no reference is defined.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static String getSoundResource(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._getSoundResource(index);
  }

  /**
   * Applies the specified sound resource (without extension) to the male string entry.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setSoundResource(int index, String resRef) throws IndexOutOfBoundsException
  {
    setSoundResource(Type.MALE, index, resRef);
    if (hasFemaleTable()) {
      setSoundResource(Type.FEMALE, index, resRef);
    }
  }

  /**
   * Applies the specified sound resource (without extension) to the string entry.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setSoundResource(Type type, int index, String resRef) throws IndexOutOfBoundsException
  {
    instance(type)._setSoundResource(index, resRef);
  }

  /**
   * Returns flags of specified male string.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static short getFlags(int index) throws IndexOutOfBoundsException
  {
    return getFlags(Type.MALE, index);
  }

  /**
   * Returns flags of specified string.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static short getFlags(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._getFlags(index);
  }

  /**
   * Applies the specified flags to the male string entry.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setFlags(int index, short value) throws IndexOutOfBoundsException
  {
    setFlags(Type.MALE, index, value);
    if (hasFemaleTable()) {
      setFlags(Type.FEMALE, index, value);
    }
  }

  /**
   * Applies the specified flags to the string entry.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setFlags(Type type, int index, short value) throws IndexOutOfBoundsException
  {
    instance(type)._setFlags(index, value);
  }

  /**
   * Returns volume of specified male string.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int getVolume(int index) throws IndexOutOfBoundsException
  {
    return getVolume(Type.MALE, index);
  }

  /**
   * Returns volume of specified string.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int getVolume(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._getVolume(index);
  }

  /**
   * Applies the specified volume to the male string entry.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setVolume(int index, int value) throws IndexOutOfBoundsException
  {
    setVolume(Type.MALE, index, value);
    if (hasFemaleTable()) {
      setVolume(Type.FEMALE, index, value);
    }
  }

  /**
   * Applies the specified volume to the string entry.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setVolume(Type type, int index, int value) throws IndexOutOfBoundsException
  {
    instance(type)._setVolume(index, value);
  }

  /**
   * Returns pitch of specified male string.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int getPitch(int index) throws IndexOutOfBoundsException
  {
    return getPitch(Type.MALE, index);
  }

  /**
   * Returns pitch of specified string.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int getPitch(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._getPitch(index);
  }

  /**
   * Applies the specified pitch to the male string entry.
   * Returns {@code true} if operation was successful.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setPitch(int index, int value) throws IndexOutOfBoundsException
  {
    setPitch(Type.MALE, index, value);
    if (hasFemaleTable()) {
      setPitch(Type.FEMALE, index, value);
    }
  }

  /**
   * Applies the specified pitch to the string entry.
   * Returns {@code true} if operation was successful.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void setPitch(Type type, int index, int value) throws IndexOutOfBoundsException
  {
    instance(type)._setPitch(index, value);
  }

  /**
   * Returns the StringEntry instance that is used internally to store string information.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static StringEntry getStringEntry(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._getEntry(index);
  }

  /**
   * Returns whether the male string table contains non-saved modifications.
   */
  public static boolean isModified()
  {
    boolean retVal = isModified(Type.MALE);
    if (hasFemaleTable()) {
      retVal |= isModified(Type.FEMALE);
    }

    return retVal;
  }

  /**
   * Returns whether the specified string table contains non-saved modifications.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   */
  public static boolean isModified(Type type)
  {
    return instance(type)._isModified();
  }

  /**
   * Ensures that all available entries of the specified string table are fully loaded into memory.
   * @param type The string table
   */
  public static void ensureFullyLoaded(Type type)
  {
    instance(type)._ensureFullyLoaded();
  }

  /**
   * Ensures that all available entries of all available string table are fully loaded into memory.
   */
  public static void ensureFullyLoaded()
  {
    ensureFullyLoaded(Type.MALE);
    if (hasFemaleTable()) {
      ensureFullyLoaded(Type.FEMALE);
    }
  }

  /**
   * Adds a new empty string entry to the male string table and returns its index.
   * @return index of the new string entry, or -1 on error.
   */
  public static int addEntry()
  {
    return insertEntry(getNumEntries());
  }

  /**
   * Adds a new empty string entry to the string table of specified type and returns its index.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @return index of the new string entry, or -1 on error.
   */
  public static int addEntry(Type type)
  {
    return insertEntry(type, getNumEntries(type));
  }

  /**
   * Adds the specified string entry to the male string table and returns its index.
   * @return index of the string entry, or -1 on error.
   */
  public static int addEntry(StringEntry newEntry)
  {
    return insertEntry(getNumEntries(), newEntry);
  }

  /**
   * Adds the specified string entry to the string table of specified type and returns its index.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @return index of the string entry, or -1 on error.
   */
  public static int addEntry(Type type, StringEntry newEntry)
  {
    return insertEntry(type, getNumEntries(type), newEntry);
  }

  /**
   * Inserts a new empty string entry to the male string table at the specified position.
   * @param index the index of the new string entry.
   * @return index of the new string entry, or -1 on error.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int insertEntry(int index) throws IndexOutOfBoundsException
  {
    syncTables();
    int retVal = insertEntry(Type.MALE, index);
    if (retVal >= 0 && hasFemaleTable()) {
      retVal = insertEntry(Type.FEMALE, index);
    }

    return retVal;
  }

  /**
   * Inserts a new empty string entry to the male string table at the specified position.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @param index the index of the new string entry.
   * @return index of the new string entry, or -1 on error.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int insertEntry(Type type, int index) throws IndexOutOfBoundsException
  {
    return instance(type)._insertEntry(index);
  }

  /**
   * Inserts a the specified string entry to the male string table at the specified position.
   * @param index the index of the new string entry.
   * @param newEntry The new string entry to insert.
   * @return index of the new string entry, or -1 on error.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int insertEntry(int index, StringEntry newEntry) throws IndexOutOfBoundsException
  {
    syncTables();
    int retVal = insertEntry(Type.MALE, index, newEntry);
    if (retVal >= 0 && hasFemaleTable()) {
      retVal = insertEntry(Type.FEMALE, index, newEntry);
    }

    return retVal;
  }

  /**
   * Inserts a the specified string entry to the specified string table at the specified position.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @param type The string table where the entry should be added.
   * @param index the index of the new string entry.
   * @param newEntry The new string entry to insert.
   * @return index of the new string entry, or -1 on error.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static int insertEntry(Type type, int index, StringEntry newEntry) throws IndexOutOfBoundsException
  {
    return instance(type)._insertEntry(index, newEntry);
  }

  /**
   * Removes the given string entry from the male string table.
   * @param index the index of the string entry that should be removed.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void removeEntry(int index) throws IndexOutOfBoundsException
  {
    syncTables();
    removeEntry(Type.MALE, index);
    if (hasFemaleTable()) {
      removeEntry(Type.FEMALE, index);
    }
  }

  /**
   * Removes the given string entry from the string table of specified type.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @param index the index of the string entry that should be removed.
   * @throws IndexOutOfBoundsException if index is outside of range.
   */
  public static void removeEntry(Type type, int index) throws IndexOutOfBoundsException
  {
    instance(type)._removeEntry(index);
  }

  /**
   * Writes changes made to either of the string tables back to disk.
   * Does nothing if no changes have been made.
   * @param callback An optional callback interface that can be used to track the whole operation.
   * @return {@code true} if write operations were successful, {@code false} otherwise.
   */
  public static boolean writeModified(ProgressCallback callback)
  {
    boolean retVal = writeModified(Type.MALE, callback);
    if (retVal && hasFemaleTable()) {
      retVal = writeModified(Type.FEMALE, callback);
    }

    return retVal;
  }

  /**
   * Writes changes made to the specified string table back to disk.
   * Does nothing if no changes have been made.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @param callback An optional callback interface that can be used to track the whole operation.
   * @return {@code true} if write operation was successful, {@code false} otherwise.
   */
  public static boolean writeModified(Type type, ProgressCallback callback)
  {
    boolean retVal = false;

    try {
      instance(type)._writeModified(callback);
      retVal = true;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return retVal;
  }

  /**
   * Writes male string table and female string table (if exists) to disk, regardless of whether
   * changes have been made.
   * @param callback An optional callback interface that can be used to track the whole operation.
   * @return {@code true} if write operation was successful, {@code false} otherwise.
   */
  public static boolean write(ProgressCallback callback)
  {
    boolean retVal = write(Type.MALE, callback);
    if (retVal && hasFemaleTable()) {
      retVal = write(Type.FEMALE, callback);
    }

    return retVal;
  }

  /**
   * Writes the specified string table to disk, regardless of whether changes have been made.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @param callback An optional callback interface that can be used to track the whole operation.
   * @return {@code true} if write operation was successful, {@code false} otherwise.
   */
  public static boolean write(Type type, ProgressCallback callback)
  {
    boolean retVal = false;

    try {
      instance(type)._write(callback);
      retVal = true;
    } catch (IOException e) {
    }

    return retVal;
  }

  /**
   * Writes the specified string table into the specified file, regardless of whether changes
   * have been made.
   * (Defaults to {@code Type.MALE} if specified type is not available.)
   * @param callback An optional callback interface that can be used to track the whole operation.
   * @return {@code true} if write operation was successful, {@code false} otherwise.
   */
  public static boolean write(Type type, Path tlkFile, ProgressCallback callback)
  {
    boolean retVal = false;

    try {
      instance(type)._write(tlkFile, callback);
      retVal = true;
    } catch (IOException e) {
    }

    return retVal;
  }

  /**
   * Exports specified string table into a human-readable text file.
   * @param callback An optional callback interface that can be used to track the whole operation.
   */
  public static boolean exportText(Type type, Path outFile, ProgressCallback callback)
  {
    boolean retVal = false;

    try {
      instance(type)._exportText(outFile, callback);
      retVal = true;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return retVal;
  }

  /**
   * Imports strings from specified WeiDU TRA file. Both male string table and female string table
   * (if available) are updated. Existing string will be overwritten and new strings
   * can be added. If {@code reset} is {@code true} then old string entries are removed before
   * importing data from the tra file. Undefined entries are filled with empty placeholders.<br><br>
   * The text file must follow the general WeiDU TRA format with the following limitations:
   *  <ul>
   *  <li>{@code %Variables%} (will not be evaluated, may cause issues if % is used as string delimiter)</li>
   *  <li>negative indices (will be ignored)</li>
   *  </ul>
   * @param inFile Path to text file that should be imported.
   * @param reset Indicates whether original entries are removed before the import operation.
   * @param callback An optional callback interface that can be used to track the whole operation.
   * @return {@code true} if the operation finished successfully.
   *         {@code false} if the operation could not be completed. The string table may be incomplete
   *         in this case.
   */
  public static boolean importTra(Path inFile, boolean reset, ProgressCallback callback)
  {
    boolean retVal = false;

//    Pattern regCmtLine = Pattern.compile("//.*$");
//    Pattern regCmtBlock = Pattern.compile("/\\*.*\\*/", Pattern.DOTALL);
//    Pattern[] regStrings = new Pattern[]{ Pattern.compile("\"[^\"]*\"", Pattern.DOTALL),
//                                          Pattern.compile("~~~~~.*?~~~~~", Pattern.DOTALL),
//                                          Pattern.compile("~[^~]*~", Pattern.DOTALL),
//                                          Pattern.compile("%[^%]*%", Pattern.DOTALL) };
//    Pattern regResRef = Pattern.compile("\\[[^\\[\\]]{1,8}\\]");

    // TODO: create separate TRA importer class

    return retVal;
  }

  /**
   * Exports current state of all available string tables into a single WeiDU TRA file.
   * @param callback An optional callback interface that can be used to track the whole operation.
   */
  public static boolean exportTra(Path outFile, ProgressCallback callback)
  {
    if (outFile == null) {
      return false;
    }

    StringTable tableMale = instance(Type.MALE);
    tableMale._ensureFullyLoaded();
    StringTable tableFemale = hasFemaleTable() ? instance(Type.FEMALE) : null;
    if (tableFemale != null) {
      tableFemale._ensureFullyLoaded();
    }

    if (callback != null) { callback.init(tableMale._getNumEntries()); }
    boolean retVal = true;
    try (PrintWriter writer = new PrintWriter(outFile.toFile(), getCharset().name())) {
      String newline = System.getProperty("line.separator");
      // writing header
      String niPath = Utils.getJarFileName(NearInfinity.class);
      if (niPath == null || niPath.isEmpty()) {
        niPath = "Near Infinity";
      }
      niPath += " (" + NearInfinity.getVersion() + ")";
      writer.println("// creator : " + niPath);
      writer.println("// game    : " + Profile.getGameRoot().toString());
      writer.println("// dialog  : " + Profile.getGameRoot().relativize(tableMale._getPath()));
      writer.print("// dialogF : ");
      if (tableFemale != null) {
        writer.println(Profile.getGameRoot().relativize(tableFemale._getPath()));
      } else {
        writer.println("(none)");
      }
      writer.println();

      // writing tra lines
      int numEntries = tableMale._getNumEntries();
      int colWidth = Integer.toString(numEntries - 1).length() + 2;
      for (int idx = 0; idx < numEntries; idx++) {
        if (callback != null) {
          retVal = callback.progress(idx);
          if (!retVal) {
            break;
          }
        }

        String traNum = "@" + idx;
        writer.print(traNum);
        for (int n = 0, cnt = colWidth - traNum.length(); n < cnt; n++) { writer.print(' '); }
        writer.print("= ");

        // writing male string
        String msg1 = tableMale._getStringRef(idx, Format.NONE).replaceAll("\r?\n", newline);
        String res1 = tableMale._getSoundResource(idx);
        String delim = "~";
        if (msg1.indexOf('~') >= 0) {
          if (msg1.indexOf('"') < 0) {
            delim = "\"";
          } else {
            delim = "~~~~~";
          }
        }
        writer.print(delim + msg1 + delim);
        if (!res1.isEmpty()) {
          writer.print(" [" + res1 + "]");
        }

        // writing female string
        if (tableFemale != null) {
          String msg2 = tableFemale._getStringRef(idx, Format.NONE).replaceAll("\r?\n", newline);
          String res2 = tableFemale._getSoundResource(idx);
          if (!msg2.equals(msg1) || !res2.equals(res1)) {
            delim = "~";
            if (msg1.indexOf('~') >= 0) {
              if (msg1.indexOf('"') < 0) {
                delim = "\"";
              } else {
                delim = "~~~~~";
              }
            }
            writer.print(" " + delim + msg2 + delim);
            if (!res2.isEmpty()) {
              writer.print(" [" + res2 + "]");
            }
          }
        }
        writer.println();
      }
    } catch (IOException e) {
      e.printStackTrace();
      retVal = false;
    } finally {
      if (callback != null) { callback.done(retVal); }
    }

    return retVal;
  }


  // Returns specified talk table instance (defaults to male if specified type not available)
  private static StringTable instance(Type type)
  {
    if (type == null) { type = Type.MALE; }
    if (type == Type.FEMALE && !hasFemaleTable()) { type = Type.MALE; }

    StringTable retVal = TLK_TABLE.get(type);
    if (retVal == null) {
      Path tlkPath = Profile.getProperty((type == Type.FEMALE) ? Profile.Key.GET_GAME_DIALOGF_FILE
                                                               : Profile.Key.GET_GAME_DIALOG_FILE);
      retVal = new StringTable(type, tlkPath);
      TLK_TABLE.put(type, retVal);
    }

    return retVal;
  }

  private final ArrayList<StringEntry> entries = new ArrayList<>();
  private final HashMap<Integer, Integer> entriesVirtual = new HashMap<>();
  private final Path tlkPath;
  private final StringTable.Type tlkType;

  // cached TLK header data
  private int numEntries, ofsStrings;
  private ByteBuffer headerData;
  private int entriesPending;
//  private boolean fullyLoaded;

  private short langId;
  private boolean initialized;
  private boolean modified;

  private StringTable(Type tlkType, Path tlkPath)
  {
    if (tlkPath == null) {
      throw new NullPointerException();
    }
    this.tlkType = tlkType;
    this.tlkPath = tlkPath;
    this.numEntries = 0;
    _init();
  }

  private Type _getType()
  {
    return tlkType;
  }

  private Path _getPath()
  {
    return tlkPath;
  }

  private short _getLanguageId()
  {
    return langId;
  }

  private int _getNumEntries()
  {
    return entries.size();
  }

  private int _getTranslatedIndex(int index)
  {
    if (Profile.isEnhancedEdition() && index >= STRREF_VIRTUAL) {
      if (entriesVirtual.containsKey(Integer.valueOf(index))) {
        index = entriesVirtual.get(Integer.valueOf(index)).intValue();
      } else {
        final Table2da engineTable = Table2daCache.get("ENGINEST.2DA");
        int row = index - STRREF_VIRTUAL;
        if (engineTable != null && row < engineTable.getRowCount()) {
          try {
            int strref = Integer.parseInt(engineTable.get(row, 1));
            entriesVirtual.put(Integer.valueOf(index), Integer.valueOf(strref));
            index = strref;
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return index;
  }

  private String _getStringRef(int index, Format fmt) throws IndexOutOfBoundsException
  {
    index = _getTranslatedIndex(index);
    StringEntry entry = _getEntry(index);
    return (fmt == null ? format : fmt).format(entry.getText(), index);
  }

  private void _setStringRef(int index, String text) throws IndexOutOfBoundsException
  {
    _getEntry(_getTranslatedIndex(index)).setText(text);
  }

  private String _getSoundResource(int index) throws IndexOutOfBoundsException
  {
    return _getEntry(_getTranslatedIndex(index)).getSoundRef();
  }

  private void _setSoundResource(int index, String resRef) throws IndexOutOfBoundsException
  {
    _getEntry(_getTranslatedIndex(index)).setSoundRef(resRef);
  }

  private short _getFlags(int index) throws IndexOutOfBoundsException
  {
    return _getEntry(_getTranslatedIndex(index)).getFlags();
  }

  private void _setFlags(int index, short value) throws IndexOutOfBoundsException
  {
    _getEntry(_getTranslatedIndex(index)).setFlags(value);
  }

  private int _getVolume(int index) throws IndexOutOfBoundsException
  {
    return _getEntry(_getTranslatedIndex(index)).getVolume();
  }

  private void _setVolume(int index, int value) throws IndexOutOfBoundsException
  {
    _getEntry(_getTranslatedIndex(index)).setVolume(value);
  }

  private int _getPitch(int index) throws IndexOutOfBoundsException
  {
    return _getEntry(_getTranslatedIndex(index)).getPitch();
  }

  private void _setPitch(int index, int value) throws IndexOutOfBoundsException
  {
    _getEntry(_getTranslatedIndex(index)).setPitch(value);
  }

  // Always returns a non-null StringEntry instance
  private StringEntry _getEntry(int index) throws IndexOutOfBoundsException
  {
    index = _getTranslatedIndex(index);
    _ensureIndexIsLoaded(index);
    StringEntry entry;
    if (index >= 0 && index < entries.size()) {
      entry = entries.get(index);
    } else {
      entry = StringEntry.getInvalidEntry();
    }
    return entry;
  }

  private void _init()
  {
    if (!_initialized()) {
      synchronized (entries) {
        try (FileChannel ch = _open()) {
          // parsing header
          String sig = StreamUtils.readString(ch, 8);
          if (!"TLK V1  ".equals(sig)) {
            throw new Exception("Invalid TLK signature");
          }

          langId = StreamUtils.readShort(ch);
          numEntries = StreamUtils.readInt(ch);
          ofsStrings = StreamUtils.readInt(ch);

          int bufferSize = 26 * numEntries;
          headerData = StreamUtils.getByteBuffer(bufferSize);
          if (ch.read(headerData) < bufferSize) {
            throw new Exception("Not enough data");
          }
          headerData.position(0);

          // fill cache with placeholder string entries
          entries.ensureCapacity(numEntries + 10);
          while (entries.size() < numEntries) {
            entries.add(null);
          }

          entriesPending = numEntries;
          initialized = true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private boolean _initialized()
  {
    return initialized;
  }

  private void _reset()
  {
    synchronized (entries) {
      entries.clear();
      headerData = null;
      ofsStrings = numEntries = entriesPending = -1;
      initialized = false;
      _resetModified();
    }
    _init();
  }

  private FileChannel _open() throws IOException
  {
    return FileChannel.open(_getPath(), StandardOpenOption.READ);
  }

  private StringEntry _loadEntry(FileChannel ch, int index) throws IOException, IndexOutOfBoundsException,
                                                            IllegalArgumentException
  {
    if (index < 0 || index >= _getNumEntries()) {
      throw new IndexOutOfBoundsException();
    }

    StringEntry entry = null;
    if (ch != null) {
      int ofs = index * 26; // rel. offset entry
      headerData.position(ofs);
      short flags = headerData.getShort();
      String soundRef = StreamUtils.readString(headerData, 8);
      int volume = headerData.getInt();
      int pitch = headerData.getInt();
      int ofsString = ofsStrings + headerData.getInt();
      int lenString = headerData.getInt();
      headerData.position(0);
      String text = null;
      if (lenString > 0) {
        try {
          ch.position(ofsString);
          text = StreamUtils.readString(ch, lenString, getCharset());
          if (!CharsetDetector.getLookup().isExcluded(index)) {
            text = CharsetDetector.getLookup().decodeString(text);
          }
        } catch (IllegalArgumentException e) {
          System.err.println("Error: Illegal offset " + ofsString + " for string entry " + index);
          text = "";
        }
      } else {
        text = "";
      }
      entry = new StringEntry(this, flags, soundRef, volume, pitch, text);
    }
    return entry;
  }

  private int _insertEntry(int index) throws IndexOutOfBoundsException
  {
    return _insertEntry(index, new StringEntry(this));
  }

  private int _insertEntry(int index, StringEntry newEntry) throws IndexOutOfBoundsException
  {
    if (index < 0 || index > _getNumEntries()) {
      throw new IndexOutOfBoundsException();
    }
    if (newEntry == null) {
      newEntry = new StringEntry(this);
    } else {
      newEntry.parent = this;
    }

    _ensureFullyLoaded();
    newEntry.setModified();
    synchronized (entries) {
      entries.add(index, newEntry);
    }

    return index;
  }

  private void _removeEntry(int index) throws IndexOutOfBoundsException
  {
    if (index < 0 || index >= _getNumEntries()) {
      throw new IndexOutOfBoundsException();
    }

    _ensureFullyLoaded();
    synchronized (entries) {
      entries.remove(index);
    }
  }

  // Loads all remaining string entries from file
  private void _ensureFullyLoaded()
  {
    if (entriesPending > 0) {
      synchronized (entries) {
        try (FileChannel ch = _open()) {
          for (int idx = 0, num = _getNumEntries(); idx < num; idx++) {
            if (entries.get(idx) == null) {
              StringEntry entry = _loadEntry(ch, idx);
              if (entry != null) {
                entries.set(idx, entry);
              } else {
                throw new Exception();
              }
            }
          }
          entriesPending = 0;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  // Makes sure the specified string entry is loaded into memory
  private void _ensureIndexIsLoaded(int index)
  {
    index = _getTranslatedIndex(index);
    if (entriesPending > 0 && index >= 0 && index < _getNumEntries() && entries.get(index) == null) {
      synchronized (entries) {
        try (FileChannel ch = _open()) {
          StringEntry entry = _loadEntry(ch, index);
          if (entry != null) {
            entries.set(index, entry);
            entriesPending--;
          } else {
            throw new Exception();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void _resetEntries()
  {
    if (_isModified()) {
      synchronized (entries) {
        for (int idx = 0, cnt = entries.size(); idx < cnt; idx++) {
          if (entries.get(idx) != null && entries.get(idx).isModified()) {
            entries.set(idx, null);
            entriesPending++;
          }
        }
        _resetModified();
      }
    }
  }

  private boolean _isModified()
  {
    return modified;
  }

  private void _setModified()
  {
    modified = true;
  }

  private void _resetModified()
  {
    modified = false;
  }

  // Writes data back to disk only if entries have been modified
  private void _writeModified(ProgressCallback callback) throws IOException
  {
    if (_isModified()) {
      _write(callback);
    }
  }

  // Writes currently loaded data to disk regardless of its modified state
  private void _write(ProgressCallback callback) throws IOException
  {
    _write(_getPath(), callback);
  }

  // Writes currently loaded data to disk under specified filename regardless of its modified state
  private void _write(Path tlkPath, ProgressCallback callback) throws IOException
  {
    if (tlkPath == null) {
      throw new NullPointerException();
    }

    _ensureFullyLoaded();
    synchronized (entries) {
      boolean success = false;

      // 1. backing up current string table file if needed
      Path pathBackup = null;
      if (FileEx.create(tlkPath).isFile()) {
        String name = tlkPath.getFileName().toString();
        for (int i = 0; i < 999; i++) {
          Path path = tlkPath.getParent().resolve(name + "-" + i);
          if (!FileEx.create(path).exists()) {
            pathBackup = path;
            break;
          }
        }
        if (pathBackup != null) {
          Files.move(tlkPath, pathBackup);
        }
      }

      // 2. writing changes to disk
      if (callback != null) { callback.init(_getNumEntries()); }
      try (FileChannel ch = FileChannel.open(tlkPath, StandardOpenOption.CREATE,
                                                      StandardOpenOption.WRITE,
                                                      StandardOpenOption.TRUNCATE_EXISTING)) {
        int headerSize = 18;
        int entrySize = 26;
        int numEntries = _getNumEntries();
        int ofsStrings = headerSize + (numEntries * entrySize);

        // write global header
        ByteBuffer buffer = StreamUtils.getByteBuffer(headerSize);
        buffer.position(0);
        buffer.put("TLK V1  ".getBytes(Misc.CHARSET_DEFAULT));
        buffer.putShort(_getLanguageId());
        buffer.putInt(numEntries);
        buffer.putInt(ofsStrings);
        buffer.position(0);
        ch.write(buffer);

        // write entry headers
        ArrayList<byte[]> stringList = new ArrayList<>(numEntries);
        buffer = StreamUtils.getByteBuffer(entrySize);
        int curStringOfs = 0;
        CharsetDetector.CharLookup lookup = CharsetDetector.getLookup();
        for (int idx = 0, count = entries.size(); idx < count; idx++) {
          final StringEntry entry = entries.get(idx);
          // apply character encoding if required
          String text;
          if (lookup.isExcluded(idx)) {
            text = entry.getText();
          } else {
            text = lookup.encodeString(entry.getText());
          }
          byte[] data = entry.getTextBytes(text);
          byte[] soundRef = entry.getSoundRefBytes();
          buffer.position(0);
          buffer.putShort(entry.getFlags());
          buffer.put(soundRef);
          buffer.putInt(entry.getVolume());
          buffer.putInt(entry.getPitch());
          buffer.putInt(curStringOfs);
          buffer.putInt(data.length);
          buffer.position(0);
          ch.write(buffer);
          stringList.add(data);
          curStringOfs += data.length;
        }

        // write strings
        int index = 0;
        for (final byte[] data: stringList) {
          if (callback != null) {
            success = callback.progress(index++);
            if (!success) {
              throw new Exception("Operation cancelled");
            }
          }
          buffer = StreamUtils.getByteBuffer(data);
          buffer.position(0);
          buffer.put(data);
          buffer.position(0);
          ch.write(buffer);
        }

        _resetModified();
        success = true;
      } catch (IOException | UnsupportedOperationException e) {
        throw e;
      } catch (Exception e) {
      } finally {
        // 3. removing or restoring backup
        if (pathBackup != null) {
          if (success) {
            Files.delete(pathBackup);
          } else {
            Files.move(pathBackup, _getPath(), StandardCopyOption.REPLACE_EXISTING);
          }
        }

        if (callback != null) { callback.done(success); }
      }
    }
  }

  // Export as list of human-readable text entries
  private void _exportText(Path outFile, ProgressCallback callback) throws IOException
  {
    if (outFile == null) {
      throw new IOException("Output file not specified");
    }

    _ensureFullyLoaded();
    synchronized (entries) {
      if (callback != null) { callback.init(_getNumEntries()); }
      boolean success = false;
      try (PrintWriter writer = new PrintWriter(outFile.toFile(), getCharset().name())) {
        String newline = System.getProperty("line.separator");
        for (int idx = 0; idx < _getNumEntries(); idx++) {
          if (callback != null) {
            success = callback.progress(idx);
            if (!success) {
              break;
            }
          }
          StringEntry entry = entries.get(idx);
          writer.println(idx + ":");
          writer.println(entry.getText().replaceAll("\r?\n", newline));
          writer.println();
        }
      } catch (Exception e) {
        success = false;
        throw e;
      } finally {
        if (callback != null) { callback.done(success); }
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  // Manages a single string entry
  public static class StringEntry extends AbstractStruct
  {
    // Default entry for non-existing indices
    private static final StringEntry INVALID = new StringEntry(null, FLAGS_HAS_TEXT, "", 0, 0, "No such index");

    private StringTable parent;
    private short flags;
    private String soundRef;
    private int volume, pitch;
    private String text;
    private boolean modified;

    public static StringEntry getInvalidEntry()
    {
      return INVALID;
    }

    public StringEntry(StringTable parent)
    {
      super(null, null, 0, 4);
      this.parent = parent;
      this.flags = 0;
      this.soundRef = "";
      this.volume = 0;
      this.pitch = 0;
      this.text = "";
      resetModified();
    }

    public StringEntry(StringTable parent, short flags, String soundRef, int volume, int pitch, String text)
    {
      super(null, null, 0, 4);
      this.parent = parent;
      this.flags = flags;
      this.soundRef = (soundRef != null) ? soundRef : "";
      this.volume = volume;
      this.pitch = pitch;
      this.text = text;
      resetModified();
    }

    public StringTable getTable() { return parent; }

    public StringTable.Type getTableType()
    {
      if (parent != null) {
        return parent._getType();
      } else {
        return StringTable.Type.MALE;
      }
    }

    public short getFlags() { return flags; }
    public void setFlags(short newFlags)
    {
      if (newFlags != flags) {
        flags = newFlags;
        setModified();
      }
    }

    public String getSoundRef() { return soundRef; }
    public void setSoundRef(String ref)
    {
      if (ref == null) {
        ref = "";
      }
      if (!ref.equals(soundRef)) {
        if (ref.length() > 8) {
          ref = ref.substring(0, 8);
        }
        soundRef = ref;
        setModified();
      }
    }

    public int getVolume() { return volume; }
    public void setVolume(int newValue)
    {
      if (newValue != volume) {
        volume = newValue;
        setModified();
      }
    }

    public int getPitch() { return pitch; }
    public void setPitch(int newValue)
    {
      if (newValue != pitch) {
        pitch = newValue;
        setModified();
      }
    }

    public String getText() { return text; }
    public void setText(String newText)
    {
      if (newText == null) {
        newText = "";
      }

      if (!normalizedText(newText).equals(normalizedText(text))) {
        text = newText;
        setModified();
      }
    }

    public boolean isModified()
    {
      return modified;
    }

    public void setModified()
    {
      modified = true;
      if (parent != null) {
        parent._setModified();
      }
    }

    public void resetModified()
    {
      modified = false;
    }

    public byte[] getSoundRefBytes()
    {
      byte[] retVal = new byte[8];
      System.arraycopy(soundRef.getBytes(Misc.CHARSET_DEFAULT), 0, retVal, 0, soundRef.length());
      return retVal;
    }

    public byte[] getTextBytes()
    {
      return text.getBytes(StringTable.getCharset());
    }

    public byte[] getTextBytes(String text)
    {
      return text.getBytes(StringTable.getCharset());
    }

    // Newline conversion should not affect string comparison
    public String normalizedText(String text)
    {
      StringBuilder sb = new StringBuilder(text);
      int idx;
      while ((idx = sb.indexOf("\r")) >= 0) {
        sb.deleteCharAt(idx);
      }
      return sb.toString();
    }

    @Override
    public String toString()
    {
      return text;
    }

    @Override
    public StringEntry clone()
    {
      return new StringEntry(parent, flags, soundRef, volume, pitch, text);
    }

    @Override
    public int read(ByteBuffer buffer, int offset) throws Exception
    {
      // needed to meet implementation requirements
      return offset + 26;
    }

    public void clearList()
    {
      if (!getFields().isEmpty()) {
        clearFields();
      }
    }

    public void fillList(int index)
    {
      try {
        if (getFields().isEmpty()) {
          ByteBuffer buffer = StreamUtils.getByteBuffer(26);
          buffer.position(0);
          buffer.putShort(flags);
          byte[] resref = soundRef.getBytes();
          buffer.put(resref);
          buffer.position(buffer.position() + 8 - resref.length);
          buffer.putInt(volume);
          buffer.putInt(pitch);
          int ofs = 18 + (index * 26);
          StructEntry e = new Flag(buffer, 0, 2, StringEditor.TLK_FLAGS, StringEditor.s_flags);
          e.setOffset(ofs + e.getOffset());
          addField(e);
          e = new ResourceRef(buffer, 2, StringEditor.TLK_SOUND, "WAV");
          e.setOffset(ofs + e.getOffset());
          addField(e);
          e = new DecNumber(buffer, 10, 4, StringEditor.TLK_VOLUME);
          e.setOffset(ofs + e.getOffset());
          addField(e);
          e = new DecNumber(buffer, 14, 4, StringEditor.TLK_PITCH);
          e.setOffset(ofs + e.getOffset());
          addField(e);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Used by save, import and export operations to allow tracking the progress of the operation.
   */
  public static abstract class ProgressCallback
  {
    /**
     * Called once before the operation starts.
     * It can be used to initialize a progress monitor or other kinds of tracking mechanisms.
     * @param numEntries Number of strings to process.
     */
    public abstract void init(int numEntries);

    /**
     * (Optional) Called once after operation has ended.
     * @param success Whether operation has been finished successfully.
     */
    public void done(boolean success) {}

    /**
     * Called before processing the string of specified index.
     * @param index The string index.
     * @return {@code false} to cancel current operation, {@code true} to continue.
     */
    public abstract boolean progress(int index);
  }
}
