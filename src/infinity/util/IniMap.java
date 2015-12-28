// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;

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
    this(ResourceFactory.getResourceEntry(name));
  }

  public IniMap(ResourceEntry entry)
  {
    init(entry);
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

  private void init(ResourceEntry entry)
  {
    // reading and storing unprocessed lines of text
    List<String> lines = new ArrayList<String>();
    if (entry != null) {
      try {
        byte[] data = entry.getResourceData();
        int ofs = 0;
        int start = 0;
        while (ofs < data.length) {
          byte b = data[ofs];
          if (b == 0x0d || b == 0x0a) { // newline
            String s = new String(data, start, ofs - start, Charset.forName("windows-1252"));
            lines.add(s.trim());
            if (b == 0x0d && ofs+1 < data.length && data[ofs+1] == 0x0a) {
              ofs++;
            }
            start = ofs;
          }
          ofs++;
        }

        // adding last line if available
        if (ofs > start) {
          String s = new String(data, start, ofs - start, Charset.forName("windows-1252"));
          lines.add(s.trim());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }


    // parsing lines
    String curSection = null;
    int curSectionLine = 0;
    List<IniMapEntry> section = new ArrayList<IniMapEntry>();
    for (int i = 0, count = lines.size(); i < count; i++) {
      final String line = lines.get(i);
      if (Pattern.matches("^\\[.+\\]$", line)) {  // new section found
        // storing content of previous section
        if (curSection != null || section.size() > 0) {
          entries.add(new IniMapSection(curSection, curSectionLine, section));
        }
        curSection = line.substring(1, line.length() - 1);
        curSectionLine = i;
        section.clear();
      } else {    // potential section entry
        IniMapEntry e = parseEntry(line, i);
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

  private IniMapEntry parseEntry(String line, int lineNr)
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
        } else if (ch == '/' && pos+1 < line.length() && line.charAt(pos+1) == '/') {
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
