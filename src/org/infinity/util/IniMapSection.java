// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.List;

public class IniMapSection
{
  private final List<IniMapEntry> entries = new ArrayList<IniMapEntry>();

  private String name;
  private int line;   // line number of section header

  public IniMapSection(String name, int line, List<IniMapEntry> entries)
  {
    this.name = (name != null) ? name : "";
    this.line = line;
    if (entries != null) {
      for (final IniMapEntry e: entries) {
        this.entries.add(e);
      }
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

  /** Returns the specified section entry. */
  public IniMapEntry getEntry(int index)
  {
    if (index >= 0 && index < getEntryCount()) {
      return entries.get(index);
    }
    return null;
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
