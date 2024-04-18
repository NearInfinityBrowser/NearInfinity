// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import org.infinity.resource.graphics.decoder.PvrInfo;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

/**
 * Decodes a PVR(Z) file. Note: Supports only the minimal set of PVR-specific features required to decode the BGEE's
 * PVRZ resources (this includes only a selected number of supported pixel formats).
 */
public class PvrDecoder {
  // The global cache list for PVR objects. The "key" has to be a unique String (e.g. filename or integer as string)
  private static final Map<String, PvrDecoder> PVR_CACHE = new LinkedHashMap<>();

  // The max. number of cache entries to hold
  private static int MaxCacheEntries = 32;

  private PvrInfo info;

  /**
   * Returns an initialized PvrDecoder object with the specified resource (if available).
   *
   * @param entry The ResourceEntry object of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(ResourceEntry entry) {
    if (entry == null) {
      throw new NullPointerException();
    }
    final String key;
    if (entry instanceof FileResourceEntry) {
      key = ((FileResourceEntry) entry).getActualPath().toString();
    } else {
      key = entry.getResourceName();
    }
    PvrDecoder decoder = getCachedPvrDecoder(key);
    if (decoder == null) {
      try (InputStream is = entry.getResourceDataAsStream()) {
        decoder = createPvrDecoder(key, is);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return decoder;
  }

  /**
   * Returns an initialized PvrDecoder object with the specified file (if available).
   *
   * @param fileName The filename of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(String fileName) {
    if (fileName == null) {
      throw new NullPointerException();
    }
    final String key = fileName;
    PvrDecoder decoder = getCachedPvrDecoder(key);
    if (decoder == null) {
      try (InputStream is = StreamUtils.getInputStream(FileManager.resolve(fileName))) {
        decoder = createPvrDecoder(key, is);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return decoder;
  }

  /**
   * Returns an initialized PvrDecoder object with the specified file (if available).
   *
   * @param file The file object of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(Path file) {
    final String key = file.getFileName().toString();
    PvrDecoder decoder = getCachedPvrDecoder(key);
    if (decoder == null) {
      try (InputStream is = StreamUtils.getInputStream(file)) {
        decoder = createPvrDecoder(key, is);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return decoder;
  }

  /**
   * Returns an initialized PvrDecoder object with the specified input stream (if available).
   *
   * @param input The input stream of the pvr(z) resource to load.
   * @return the PvrDecoder object containing the decoded PVR resource, or {@code null} on error.
   */
  public static PvrDecoder loadPvr(InputStream input) {
    if (input == null) {
      throw new NullPointerException();
    }
    final String key = Integer.valueOf(input.hashCode()).toString();
    PvrDecoder decoder = getCachedPvrDecoder(key);
    if (decoder == null) {
      try {
        decoder = createPvrDecoder(key, input);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return decoder;
  }

  /** Returns the max. number of PvrDecoder objects to cache. */
  public static int getMaxCacheEntries() {
    return MaxCacheEntries;
  }

  /** Specify the new max. number of PvrDecoder objects to cache. Specifying 0 disables the cache. */
  public static synchronized void setMaxCacheEntries(int maxValue) {
    if (maxValue < 0)
      maxValue = 0;
    else if (maxValue > 65535)
      maxValue = 65535;
    if (maxValue != MaxCacheEntries) {
      MaxCacheEntries = maxValue;
      while (PVR_CACHE.size() > MaxCacheEntries) {
        PVR_CACHE.remove(PVR_CACHE.keySet().iterator().next());
      }
    }
  }

  /** Clears all available caches. */
  public static synchronized void flushCache() {
    PVR_CACHE.clear();
    PvrInfo.flushCache();
  }

  /** Returns the current cache load as percentage value. */
  public static int getCacheLoad() {
    if (MaxCacheEntries > 0) {
      return (PVR_CACHE.size() * 100) / MaxCacheEntries;
    } else {
      return 0;
    }
  }

  // Returns a cached PvrDecoder object if available, null otherwise.
  private static synchronized PvrDecoder getCachedPvrDecoder(String key) {
    final PvrDecoder retVal = PVR_CACHE.get(key);
    if (retVal != null) {
      // re-inserting entry to prevent premature removal from cache
      PVR_CACHE.remove(key);
      PVR_CACHE.put(key, retVal);
    }
    return retVal;
  }

  // Returns a PvrDecoder object of the specified key if available, or creates and returns a new one otherwise.
  private static synchronized PvrDecoder createPvrDecoder(String key, InputStream input) {
    PvrDecoder retVal = null;
    if (key != null && !key.isEmpty()) {
      key = key.toUpperCase(Locale.ENGLISH);
      if (PVR_CACHE.containsKey(key)) {
        retVal = PVR_CACHE.get(key);
        // re-inserting entry to prevent premature removal from cache
        PVR_CACHE.remove(key);
        PVR_CACHE.put(key, retVal);
      } else {
        try {
          retVal = new PvrDecoder(input);
          if (retVal != null) {
            PVR_CACHE.put(key, retVal);
            // removing excess cache entries
            while (PVR_CACHE.size() > MaxCacheEntries) {
              PVR_CACHE.remove(PVR_CACHE.keySet().iterator().next());
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  /** Provides access to the PVR information data structure. */
  public PvrInfo getInfo() {
    return info;
  }

  /**
   * Decodes the currently loaded PVR data and returns the result as a new BufferedImage object.
   *
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception on error.
   */
  public BufferedImage decode() throws Exception {
    return decode(0, 0, info.getWidth(), info.getHeight());
  }

  /**
   * Decodes the currently loaded PVR data and draws the result into a BufferedImage object.
   *
   * @param image The BufferedImage object to draw the PVR texture into.
   * @return {@code true} if the image has been drawn successfully, {@code false} otherwise.
   * @throws Exception on error.
   */
  public boolean decode(BufferedImage image) throws Exception {
    return decode(image, 0, 0, info.getWidth(), info.getHeight());
  }

  /**
   * Decodes a rectangular block of pixels of the currently loaded PVR data and returns it as a new BufferedImage
   * object.
   *
   * @param x      Left-most x coordinate of the pixel block.
   * @param y      Top-most y coordinate of the pixel block.
   * @param width  Width in pixels.
   * @param height Height in pixels.
   * @return A BufferedImage object of the resulting image data.
   * @throws Exception on error.
   */
  public BufferedImage decode(int x, int y, int width, int height) throws Exception {
    if (width < 1)
      width = 1;
    if (height < 1)
      height = 1;
    int imgType = (info.getFlags() == PvrInfo.Flags.PRE_MULTIPLIED) ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_ARGB;
    BufferedImage image = new BufferedImage(width, height, imgType);
    if (decode(image, x, y, width, height)) {
      return image;
    } else {
      image = null;
    }
    return null;
  }

  /**
   * Decodes a rectangular block of pixels of the currently loaded PVR data and draws it into a BufferedImage object.
   *
   * @param image  The BufferedImage object to draw the pixel data into.
   * @param x      Left-most x coordinate of the pixel block.
   * @param y      Top-most y coordinate of the pixel block.
   * @param width  Width in pixels.
   * @param height Height in pixels.
   * @return {@code true} if the image has been drawn successfully, {@code false} otherwise.
   * @throws Exception
   */
  public boolean decode(BufferedImage image, int x, int y, int width, int height) throws Exception {
    if (image == null) {
      throw new Exception("No target image specified");
    }
    if (x < 0 || y < 0 || width < 1 || height < 1 || x + width > info.getWidth() || y + height > info.getHeight()) {
      throw new Exception("Invalid dimensions specified");
    }
    if (!info.isSupported()) {
      throw new Exception(String.format("Pixel format '%s' not supported", info.getPixelFormat().toString()));
    }
    if (info.getChannelType() != PvrInfo.ChannelType.UBYTE_NORM) {
      throw new Exception(String.format("Channel type '%s' not supported", info.getChannelType().toString()));
    }
    Rectangle region = new Rectangle(x, y, width, height);
    return info.decode(image, region);
  }

  private PvrDecoder(InputStream input) throws Exception {
    if (input == null) {
      throw new NullPointerException();
    }

    try {
      // determine whether input contains PVR or PVRZ data
      if (!input.markSupported()) {
        throw new Exception("Unsupported stream operations: mark/reset");
      }
      input.mark(8);
      byte[] buf = new byte[4];
      if (input.read(buf) < 4) {
        throw new Exception("Error reading input data");
      }
      boolean isPvr = (buf[0] == (byte) 0x50 && buf[1] == (byte) 0x56 && buf[2] == (byte) 0x52
          && buf[3] == (byte) 0x03);
      input.reset();

      final int headerSize = 128;
      int size = 0;
      byte[] buffer = null;
      if (!isPvr) {
        // wrapping input stream
        byte[] sizeBuf = new byte[4];
        input.read(sizeBuf);
        size = DynamicArray.getInt(sizeBuf, 0);
        buffer = new byte[size];
        input = new InflaterInputStream(input);
      }
      // read data into buffer
      if (buffer == null) {
        size = 1 << 12; // starting with size = 4096 bytes
        buffer = new byte[size + headerSize];
      }
      int ofs = 0, len;
      while (true) {
        do {
          len = input.read(buffer, ofs, buffer.length - ofs);
          if (len > 0) {
            ofs += len;
          }
        } while (len > 0 && ofs < buffer.length);
        if (len < 0 || ofs >= buffer.length)
          break;
        size <<= 1;
        byte[] tmp = new byte[size + headerSize];
        System.arraycopy(buffer, 0, tmp, 0, buffer.length);
        buffer = tmp;
        ofs += len;
      }
      info = new PvrInfo(buffer, ofs);
      buffer = null;
    } finally {
      input.close();
    }
  }
}
