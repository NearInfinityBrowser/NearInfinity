// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.mus;

import infinity.resource.key.ResourceEntry;
import infinity.resource.sound.SoundUtilities;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

public final class Entry
{
  private final Entry[] entries;
  private final ResourceEntry entry;
  private final String line;
  private final String dir;
  private File wavfile, endfile;
  private String name;
  private int nextnr;

  public Entry(ResourceEntry entry, String dir, Entry entries[], String line, int nr)
  {
    this.entry = entry;
    this.dir = dir;
    this.entries = entries;
    this.line = line;
    nextnr = nr + 1;
  }

  public String toString()
  {
    return line;
  }

  public void close()
  {
    if (wavfile != null)
      if (!wavfile.delete())
        wavfile.deleteOnExit();
    if (endfile != null)
      if (!endfile.delete())
        endfile.deleteOnExit();
  }

  public File getEndfile()
  {
    return endfile;
  }

  public int getNextNr()
  {
    return nextnr;
  }

  public File getWavFile()
  {
    return wavfile;
  }

  public void init() throws IOException
  {
    StringTokenizer st = new StringTokenizer(line);
    name = st.nextToken();
    wavfile = getWavFile(name);
    while (st.hasMoreTokens()) {
      String command = st.nextToken();
      if (command.equalsIgnoreCase("@TAG")) {
        String next = st.nextToken();
        if (next.equalsIgnoreCase("END"))
          nextnr = -1;
        else
          endfile = getWavFile(next);
      }
      else {
        if (command.equalsIgnoreCase(dir))
          command = st.nextToken();
        if (name.equalsIgnoreCase(command))
          nextnr--;
        else {
          for (int i = 0; i < entries.length; i++)
            if (entries[i] == null) {
              nextnr = -1;
              break;
            }
            else if (entries[i].name.equalsIgnoreCase(command)) {
              nextnr = i;
              break;
            }
        }
      }
    }
  }

  private File getWavFile(String filename) throws IOException
  {
    File acmfile = new File(entry.getActualFile().getParent() + File.separatorChar + dir +
                            File.separatorChar + dir + filename + ".acm");
    if (!acmfile.exists())
      acmfile = new File(entry.getActualFile().getParentFile(), filename + ".acm");
    if (!acmfile.exists() && filename.toUpperCase().startsWith("MX"))
      acmfile = new File(entry.getActualFile().getParent() + File.separatorChar + filename.substring(0, 6) +
                         File.separatorChar + filename + ".acm");
    if (!acmfile.exists())
      throw new IOException("Could not find " + filename);
    return SoundUtilities.convert(acmfile, false);
  }
}

