// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.infinity.datatype.StringRef;

/**
 * Class, that represents collection of pairs {@code key = value}, grouped together
 * under some {@link #getName name}. This name appears in brackets before key-value
 * pairs.
 * <p>
 * This class can be iterated in for-loop, and its iterator returns all key-value
 * paramaters in the same order, in which they was appeared in the ini-file.
 * <p>
 * {@link #toString()} method of this class returns standard INI representation
 * of section, i.e. name in brackets (if section {@link #isUnnamedSection() has name})
 * followed by all key-value pairs in iteration order, each on own line. Example:
 * <code><pre>
 * [name]
 * key1 = value1
 * key2 = value2
 * ...
 * keyN = valueN
 * </pre></code>
 * For unnamed sections {@code [name]} part is skipped.
 */
public class IniMapSection implements Iterable<IniMapEntry>
{
  private final List<IniMapEntry> entries = new ArrayList<>();

  /**
   * Name of section (string between square brackets) or empty string, if this
   * section contains entries of ini file, that not included in any section.
   */
  private final String name;
  /** Line number of section header. */
  private final int line;

  public IniMapSection(String name, int line, List<IniMapEntry> entries)
  {
    this.name = (name != null) ? name : "";
    this.line = line;
    if (entries != null) {
      this.entries.addAll(entries);
    }
  }

  /** Returns the name of the section. Returns empty string for unnamed sections. */
  public String getName()
  {
    return name;
  }

  /** Returns the line number of the section header. */
  public int getLine()
  {
    return line;
  }

  /** Returns whether the specified section does not contain a section header. */
  public boolean isUnnamedSection()
  {
    return name.isEmpty();
  }

  /** Returns number of available section entries. */
  public int getEntryCount()
  {
    return entries.size();
  }

  /**
   * Returns the first instance with {@code "key"} matching the key value of the
   * entry. Performs case-insensitive search.
   *
   * @param key Name of value in key-value pair. This key must not contain spaces
   *        because all spaces are cut off during parsing if ini-file, so key with
   *        leading or trailing spaces never will be found
   *
   * @return First key-value pair, which key equals (ignoring case) parameter
   *         {@code key}, or {@code null}, if no such pair exists
   */
  public IniMapEntry getEntry(String key)
  {
    if (key != null) {
      for (final IniMapEntry e: entries) {
        if (key.equalsIgnoreCase(e.getKey())) {
          return e;
        }
      }
    }
    return null;
  }
  /**
   * Returns value for specified key as string.
   *
   * @param key Name of value in key-value pair. This key must not contain spaces
   *        because all spaces are cut off during parsing if ini-file, so key with
   *        leading or trailing spaces never will be found
   *
   * @return Value of first key-value pair, which key equals (ignoring case) parameter
   *         {@code key}, or {@code null}, if no such pair exists
   */
  public String getAsString(String key)
  {
    return getAsString(key, null);
  }
  /**
   * Returns value for specified key as string. Returns a default value if key or
   * value are not present.
   *
   * @param key Name of value in key-value pair. This key must not contain spaces
   *        because all spaces are cut off during parsing if ini-file, so key with
   *        leading or trailing spaces never will be found
   *
   * @return Value of first key-value pair, which key equals (ignoring case) parameter
   *         {@code key}, or {@code defValue}, if no such pair exists or value is
   *         not defined.
   */
  public String getAsString(String key, String defValue)
  {
    final IniMapEntry entry = getEntry(key);
    return entry == null ? defValue : entry.getValue();
  }
  /**
   * Returns value for specified key as integer or returns default value, if key
   * or value is not presented in the file.
   *
   * @param key Name of value in key-value pair. This key must not contain spaces
   *        because all spaces are cut off during parsing if ini-file, so key with
   *        leading or trailing spaces never will be found
   * @param defValue Default value that will be returned, if key or value does not exist
   *
   * @return Value of first key-value pair, which key equals (ignoring case) parameter
   *         {@code key}, converted to integer, or {@code defValue}, if no such pair
   *         exists or value is not defined
   *
   * @throws NumberFormatException If value is not an integer
   */
  public int getAsInteger(String key, int defValue)
  {
    final IniMapEntry entry = getEntry(key);
    return entry == null ? defValue : entry.getIntValue(defValue);
  }
  /**
   * Returns value for specified key as double or returns default value, if key
   * or value is not presented in the file.
   *
   * @param key Name of value in key-value pair. This key must not contain spaces
   *        because all spaces are cut off during parsing if ini-file, so key with
   *        leading or trailing spaces never will be found
   * @param defValue Default value that will be returned, if key or value does not exist
   *
   * @return Value of first key-value pair, which key equals (ignoring case) parameter
   *         {@code key}, converted to integer, or {@code defValue}, if no such pair
   *         exists or value is not defined
   *
   * @throws NumberFormatException If value is not a double
   */
  public double getAsDouble(String key, double defValue)
  {
    final IniMapEntry entry = getEntry(key);
    return entry == null ? defValue : entry.getDoubleValue(defValue);
  }
  /**
   * Returns value for specified key as string reference. {@link StringRef#getName}
   * will return {@code key} as its name.
   *
   * @param key Name of value in key-value pair. This key must not contain spaces
   *        because all spaces are cut off during parsing if ini-file, so key with
   *        leading or trailing spaces never will be found
   *
   * @return String reference, represented by integer value of first key-value pair,
   *         which key equals (ignoring case) parameter {@code key}, or {@code null},
   *         if no such pair exists or value is not defined
   *
   * @throws NumberFormatException If value is not an integer
   */
  public StringRef getAsStringRef(String key)
  {
    final IniMapEntry entry = getEntry(key);
    return entry == null ? null : entry.getStringRefValue();
  }

  @Override
  public Iterator<IniMapEntry> iterator() { return entries.iterator(); }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (!isUnnamedSection()) {
      sb.append('[').append(getName()).append(']').append('\n');
    }
    for (final IniMapEntry e: entries) {
      sb.append(e.toString()).append('\n');
    }
    return sb.toString();
  }
}
