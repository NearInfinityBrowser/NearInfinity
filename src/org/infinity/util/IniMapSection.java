// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

  /** Returns the line number of the section header */
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

  /** Returns the first instance with "key" matching the key value of the entry. */
  public IniMapEntry getEntry(String key)
  {
    if (key != null) {
      for (final IniMapEntry e: entries) {
        if (e.getKey().equalsIgnoreCase(key)) {
          return e;
        }
      }
    }
    return null;
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
