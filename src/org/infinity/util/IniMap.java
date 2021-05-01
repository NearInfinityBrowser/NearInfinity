// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private static final Pattern LINE_SPLIT = Pattern.compile("\r?\n");
  private final List<IniMapSection> entries = new ArrayList<>();

  /**
   * Creates a {@code IniMap} instance from the specified collection of strings.
   * Each string is considered as a separate line.
   * @param lines Collection of strings
   * @return an {@code IniMap} object with the content of the strings. Returns {@code null} if no strings were provided.
   */
  public static IniMap from(List<String> lines)
  {
    IniMap ini = null;
    if (lines != null) {
      StringBuilder sb = new StringBuilder();
      for(final String line : lines) {
        sb.append(line != null ? line : "").append('\n');
      }
      if (sb.length() > 0) {
        ini = new IniMap(sb.toString(), true);
      }
    }
    return ini;
  }

  /**
   * Parses specified text content as {@code ini} file with comments (comment
   * starts from {@code //} and continues to end of string).
   *
   * @param content Text to parse
   */
  public IniMap(CharSequence content)
  {
    this(content, false);
  }

  /**
   * Parses specified text content as {@code ini} file.
   *
   * @param content Text to parse
   * @param ignoreComments If {@code true}, comments (part of string from {@code //}
   *        to end of line) will not be treated specially (i.e. will not be considered
   *        as comments)
   *
   * @throws NullPointerException If {@code content} si {@code null}
   */
  public IniMap(CharSequence content, boolean ignoreComments)
  {
    final String[] lines = LINE_SPLIT.split(content);
    String curSection = null;
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
        IniMapEntry entry = parseEntry(line, i, ignoreComments);
        if (entry != null) {
          section.add(entry);
        }
      }
    }

    // adding last section
    if (curSection != null || !section.isEmpty()) {
      entries.add(new IniMapSection(curSection, curSectionLine, section));
    }
  }

  public IniMap(ResourceEntry entry)
  {
    this(entry, false);
  }

  public IniMap(ResourceEntry entry, boolean ignoreComments)
  {
    this(readResource(entry), ignoreComments);
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
   * @return New object, that represent entry in INI. Returns {@code null} if entry is not valid.
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

    if (key != null || value != null) {
      return new IniMapEntry(key, value, lineNr);
    } else {
      return null;
    }
  }

  private static String readResource(ResourceEntry entry)
  {
    if (entry != null) {
      try {
        final ByteBuffer bb = entry.getResourceBuffer();
        return StreamUtils.readString(bb, bb.limit(), Misc.CHARSET_DEFAULT);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
