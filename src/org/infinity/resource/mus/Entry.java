// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.mus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sound.AudioBuffer;
import org.infinity.resource.sound.AudioFactory;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public class Entry
{
  // Caches AudioBuffer objects for faster reload
  private static final LinkedHashMap<Path, AudioBuffer> BufferCache = new LinkedHashMap<>(100);
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
  private static void addCacheEntry(Path path, String name, AudioBuffer buffer)
  {
    if (name != null && buffer != null) {
      while (currentCacheSize + buffer.getAudioData().length > MAX_CACHE_SIZE &&
             !BufferCache.isEmpty()) {
        Iterator<Path> iter = BufferCache.keySet().iterator();
        if (iter.hasNext()) {
          AudioBuffer ab = BufferCache.get(iter.next());
          iter.remove();
          currentCacheSize -= ab.getAudioData().length;
        }
      }
      BufferCache.put(getCacheKey(path, name), buffer);
      currentCacheSize += buffer.getAudioData().length;
    }
  }

  // returns a cached AudioBuffer object or null if none found
  private static AudioBuffer getCacheEntry(Path path, String name)
  {
    if (name != null) {
      Path key = getCacheKey(path, name);
      if (BufferCache.containsKey(key)) {
        AudioBuffer ab = BufferCache.get(key);
        return ab;
      }
    }
    return null;
  }

  // internally used to create a valid cache key
  private static Path getCacheKey(Path path, String name)
  {
    Path key = null;
    if (name != null) {
      name = name.toUpperCase(Locale.ENGLISH);
      key = (path != null) ? path.resolve(name) : FileManager.resolve(name);
      key = key.toAbsolutePath();
    }
    return key;
  }

  private static long getMaxCacheSize()
  {
    // use max. 1/10th of max. available memory or 100MB for caching AudioBuffer objects
    long memSize = Runtime.getRuntime().maxMemory();
    if (memSize == Long.MAX_VALUE || memSize < (long)(256*1024*1024)) {
      return (long)(32*1024*1024);
    } else {
      return Math.max(memSize / 8L, (long)(256*1024*1024));
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

  public void close()
  {
    audioBuffer = null;
    endBuffer = null;
    nextnr = -1;
  }

  @Override
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
    Path acmFile = FileManager.query(entry.getActualPath().getParent(), dir, dir + fileName + ".acm");
    if (!FileEx.create(acmFile).isFile()) {
      acmFile = FileManager.query(entry.getActualPath().getParent(), fileName + ".acm");
    }
    if (!FileEx.create(acmFile).isFile() && fileName.toUpperCase(Locale.ENGLISH).startsWith("MX")) {
      acmFile = FileManager.query(entry.getActualPath().getParent(), fileName.substring(0, 6), fileName + ".acm");
    }
    if (!FileEx.create(acmFile).isFile()) {
      throw new IOException("Could not find " + fileName);
    }

    // simplest case: grab AudioBuffer from cache
    AudioBuffer audio = getCacheEntry(acmFile, fileName);
    if (audio != null) {
      return audio;
    }

    try (InputStream is = StreamUtils.getInputStream(acmFile)) {
      byte[] buffer = new byte[(int)Files.size(acmFile)];
      int bytesRead = is.read(buffer);
      if (bytesRead > 0) {
        // ignore # channels in header (only ACM will be affected)
        audio = AudioFactory.getAudioBuffer(buffer, 0, AudioBuffer.AudioOverride.overrideChannels(2));
        if (audio != null) {
          addCacheEntry(acmFile.getParent(), fileName, audio);
        }
      } else {
        throw new IOException("Unexpected end of file");
      }
    } catch (IOException e) {
      throw new IOException("Error reading " + fileName);
    }
    return audio;
  }

}
