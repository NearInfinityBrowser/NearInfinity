// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.mus;

import infinity.resource.key.ResourceEntry;
import infinity.util.*;
import infinity.resource.sound.AudioBuffer;
import infinity.resource.sound.AudioFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

public class Entry
{
  // Caches AudioBuffer objects for faster reload
  private static final LinkedHashMap<String, AudioBuffer> BufferCache =
      new LinkedHashMap<String, AudioBuffer>(100);
  private static final long MAX_CACHE_SIZE = getMaxCacheSize();
  private static long currentCacheSize = 0L;

  private final List<Entry> entryList;
  private final ResourceEntry entry;
  private final String line;
  private final String dir;
  private AudioBuffer audioBuffer, endBuffer;
  private String name;
  private int nextnr;

  /**
   * Clears the whole AudioBuffer cache
   */
  public static void clearCache()
  {
    BufferCache.clear();
    currentCacheSize = 0L;
  }

  // adds an AudioBuffer object to the cache
  private static void addCacheEntry(String dir, String name, AudioBuffer buffer)
  {
    if (name != null && buffer != null) {
      while (currentCacheSize >= MAX_CACHE_SIZE) {
        Iterator<String> iter = BufferCache.keySet().iterator();
        if (iter.hasNext()) {
          AudioBuffer ab = BufferCache.get(iter.next());
          iter.remove();
          currentCacheSize -= ab.getAudioData().length;
        }
      }
      BufferCache.put(getCacheKey(dir, name), buffer);
      currentCacheSize += buffer.getAudioData().length;
    }
  }

  // returns a cached AudioBuffer object or null if none found
  private static AudioBuffer getCacheEntry(String dir, String name)
  {
    if (name != null) {
      String key = getCacheKey(dir, name);
      if (BufferCache.containsKey(key)) {
        AudioBuffer ab = BufferCache.get(key);
        return ab;
      }
    }
    return null;
  }

  // internally used to create a valid cache key
  private static String getCacheKey(String dir, String name)
  {
    String key = "";
    if (name != null) {
      name = name.toUpperCase();
      key = (dir != null) ? dir + File.separator + name : File.separator + name;
    }
    return key;
  }

  private static long getMaxCacheSize()
  {
    // use max. 1/10th of max. available memory or 100MB for caching AudioBuffer objects
    long memSize = Runtime.getRuntime().maxMemory();
    if (memSize == Long.MAX_VALUE || memSize < (long)(512*1024*1024)) {
      return (long)(100*1024*1024);
    } else {
      return memSize / 10L;
    }
  }


  public Entry(ResourceEntry entry, String dir, List<Entry> entries, String line, int nr)
  {
    this.entry = entry;
    this.dir = dir;
    this.entryList = entries;
    this.line = line;
    this.nextnr = nr + 1;
  }

  public String toString()
  {
    return line;
  }

  public AudioBuffer getEndBuffer()
  {
    return endBuffer;
  }

  public int getNextNr()
  {
    return nextnr;
  }

  public AudioBuffer getAudioBuffer()
  {
    return audioBuffer;
  }

  public void init() throws IOException
  {
    StringTokenizer st = new StringTokenizer(line);
    name = st.nextToken();
    audioBuffer = getAudioBuffer(name);
    while (st.hasMoreTokens()) {
      String command = st.nextToken();
      if (command.equalsIgnoreCase("@TAG")) {
        String next = st.nextToken();
        if (next.equalsIgnoreCase("END")) {
          nextnr = -1;
        } else {
          endBuffer = getAudioBuffer(next);
        }
      } else {
        if (command.equalsIgnoreCase(dir)) {
          command = st.nextToken();
        }
        if (name.equalsIgnoreCase(command)) {
          nextnr--;
        } else {
          for (int i = 0; i < entryList.size(); i++) {
            if (entryList.get(i) == null) {
              nextnr = -1;
              break;
            } else if (entryList.get(i).name.equalsIgnoreCase(command)) {
              nextnr = i;
              break;
            }
          }
        }
      }
    }
  }

  private AudioBuffer getAudioBuffer(String fileName) throws IOException
  {
    // audio file can reside in a number of different locations
    // fileDir should have no terimal separator
    String fileDir = dir + File.separatorChar + dir;
    File acmFile = new FileCI(entry.getActualFile().getParentFile(), fileDir +
                            fileName + ".acm");
    if (!acmFile.exists() || !acmFile.isFile()) {
      fileDir = "";
      acmFile = new FileCI(entry.getActualFile().getParentFile(), fileName + ".acm");
    }
    if ((!acmFile.exists() || !acmFile.isFile()) && fileName.toUpperCase().startsWith("MX")) {
      fileDir = fileName.substring(0, 6) + File.separatorChar;
      acmFile = new FileCI(entry.getActualFile().getParentFile(), fileDir +
                         fileName + ".acm");
    }
    if (!acmFile.exists() || !acmFile.isFile())
      throw new IOException("Could not find " + fileName);

    // simplest case: grab AudioBuffer from cache
    AudioBuffer audio = getCacheEntry(fileDir, fileName);
    if (audio != null)
      return audio;

    FileInputStream fis = new FileInputStreamCI(acmFile);
    try {
      byte[] buffer = new byte[(int)acmFile.length()];
      int bytesRead = fis.read(buffer);
      fis.close();
      fis = null;
      if (bytesRead > 0) {
        // ignore # channels in header (only ACM will be affected)
        audio = AudioFactory.getAudioBuffer(buffer, 0, AudioBuffer.AudioOverride.overrideChannels(2));
        if (audio != null) {
          addCacheEntry(fileDir, fileName, audio);
        }
      } else {
        throw new IOException("Unexpected end of file");
      }
    } catch (IOException e) {
      if (fis != null) {
        fis.close();
      }
      throw new IOException("Error reading " + fileName);
    }

    return audio;
  }

}
