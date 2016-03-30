// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.StreamUtils;

/**
 * Parses Infinity Engine INI files.
 *
 * The format differs from the original Windows INI file format, as it uses double slashes
 * instead of semicolons to start comments.
 */
public class IniMap
{
  private final List<IniMapSection> entries = new ArrayList<IniMapSection>();

  public IniMap(String name)
  {
    this(ResourceFactory.getResourceEntry(name), false);
  }

  public IniMap(String name, boolean ignoreComments)
  {
    this(ResourceFactory.getResourceEntry(name), ignoreComments);
  }

  public IniMap(ResourceEntry entry)
  {
    this(entry, false);
  }

  public IniMap(ResourceEntry entry, boolean ignoreComments)
  {
    init(entry, ignoreComments);
  }

  /** Returns number of available INI sections. */
  public int getSectionCount()
  {
    return entries.size();
  }

  /** Returns the specified INI section. */
  public IniMapSection getSection(int index)
  {
    if (index >= 0 && index < getSectionCount()) {
      return entries.get(index);
    }
    return null;
  }

  /** Returns the INI section with the specified section name. */
  public IniMapSection getSection(String name)
  {
    if (name == null) {
      name = "";
    }
    for (final IniMapSection section: entries) {
      if (section.getName().equalsIgnoreCase(name)) {
        return section;
      }
    }
    return null;
  }

  /**
   * Returns the section instance for an unnamed (or empty) section, if available.
   */
  public IniMapSection getUnnamedSection()
  {
    if (entries.size() > 0 && entries.get(0).getName().isEmpty()) {
      return entries.get(0);
    }
    return null;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for (final IniMapSection s: entries) {
      sb.append(s.toString()).append('\n');
    }
    return sb.toString();
  }

  private void init(ResourceEntry entry, boolean ignoreComments)
  {
    // reading and storing unprocessed lines of text
    String[] lines = null;
    if (entry != null) {
      try {
        ByteBuffer bb = entry.getResourceBuffer();
        lines = StreamUtils.readString(bb, bb.limit(), Misc.CHARSET_DEFAULT).split("\r?\n");
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }

    // parsing lines
    String curSection = null;
    int curSectionLine = 0;
    List<IniMapEntry> section = new ArrayList<IniMapEntry>();
    for (int i = 0, count = lines.length; i < count; i++) {
      final String line = lines[i].trim();
      if (Pattern.matches("^\\[.+\\]$", line)) {  // new section found
        // storing content of previous section
        if (curSection != null || section.size() > 0) {
          entries.add(new IniMapSection(curSection, curSectionLine, section));
        }
        curSection = line.substring(1, line.length() - 1);
        curSectionLine = i;
        section.clear();
      } else {    // potential section entry
        IniMapEntry e = parseEntry(line, i, ignoreComments);
        if (e != null) {
          section.add(e);
        }
      }
    }

    // adding last section
    if (curSection != null || section.size() > 0) {
      entries.add(new IniMapSection(curSection, curSectionLine, section));
    }
  }

  private IniMapEntry parseEntry(String line, int lineNr, boolean ignoreComments)
  {
    IniMapEntry retVal = null;
    if (line != null && !line.isEmpty()) {
      String key = null, value = null;
      boolean isValue = false;
      int start = 0, pos = 0;
      for (; pos < line.length(); pos++) {
        char ch = line.charAt(pos);
        if (!isValue && ch == '=') {
          key = line.substring(start, pos).trim();
          isValue = true;
          start = pos + 1;
        } else if (!ignoreComments &&
                   ch == '/' && pos+1 < line.length() && line.charAt(pos+1) == '/') {
          break;  // skip comments
        }
      }

      // End of line: only "value" tokens are valid
      if (isValue) {
        value = line.substring(start, pos).trim();
      }

      retVal = new IniMapEntry(key, value, lineNr);
    }
    return retVal;
  }
}
