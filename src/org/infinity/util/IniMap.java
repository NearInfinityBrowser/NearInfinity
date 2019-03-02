// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
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
public class IniMap implements Iterable<IniMapSection>
{
  private static final Pattern SECTION_NAME = Pattern.compile("^\\[(.+)\\].*$");
  private final List<IniMapSection> entries = new ArrayList<>();

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
    if (!entries.isEmpty() && entries.get(0).isUnnamedSection()) {
      return entries.get(0);
    }
    return null;
  }

  @Override
  public Iterator<IniMapSection> iterator() { return entries.iterator(); }

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
    final String[] lines = readLines(entry);
    if (lines == null) {
      return;
    }

    // parsing lines
    String curSection = null;
    String curSectionComment = null;
    int curSectionLine = 0;
    final List<IniMapEntry> section = new ArrayList<>();
    for (int i = 0, count = lines.length; i < count; i++) {
      final String line = lines[i].trim();
      if (line.isEmpty()) continue;

      final Matcher m = SECTION_NAME.matcher(line);
      if (m.matches()) {  // new section found
        // storing content of previous section
        if (curSection != null || !section.isEmpty()) {
          entries.add(new IniMapSection(curSection, curSectionLine, section));
        }
        curSection = m.group(1);
        curSectionLine = i;
        section.clear();
      } else {    // potential section entry
        section.add(parseEntry(line, i, ignoreComments));
      }
    }

    // adding last section
    if (curSection != null || !section.isEmpty()) {
      entries.add(new IniMapSection(curSection, curSectionLine, section));
    }
  }

  /**
   * Parses key-value pair, delimited with {@code '='} symbol. If {@code '='}
   * not be found in the meaning part of line (i.e. not in comment), then method
   * returns entry with {@code null} value.
   *
   * @param line Line from file with stripped spaces. This line never {@code null}
   *        and never empty
   * @param lineNr Line number in the file
   * @param ignoreComments If {@code true}, comments (part of string from {@code //}
   *        to end of line) will not be treated specially (i.e. will not be considered
   *        as comments)
   *
   * @return New object, that represent entry in INI. Never {@code null}
   */
  private IniMapEntry parseEntry(String line, int lineNr, boolean ignoreComments)
  {
    String key = null, value = null;
    boolean isValue = false;
    int start = 0, pos = 0;
    for (; pos < line.length(); pos++) {
      final char ch = line.charAt(pos);
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

    return new IniMapEntry(key, value, lineNr);
  }

  private static String[] readLines(ResourceEntry entry)
  {
    if (entry != null) {
      try {
        final ByteBuffer bb = entry.getResourceBuffer();
        return StreamUtils.readString(bb, bb.limit(), Misc.CHARSET_DEFAULT).split("\r?\n");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
