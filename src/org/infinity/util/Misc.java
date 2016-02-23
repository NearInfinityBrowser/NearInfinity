// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A general-purpose class containing useful function not fitting elsewhere.
 */
public class Misc
{
  public static final Charset DEFAULT_CHARSET = Charset.forName("iso-8859-1");

  /** Compares the string representation of the specified objects, ignoring case considerations. */
  public static final Comparator<Object> IgnoreCaseComparator = new Comparator<Object>() {
    @Override
    public int compare(Object o1, Object o2)
    {
      return (o1.toString().compareToIgnoreCase(o2.toString()));
    }

    @Override
    public boolean equals(Object obj)
    {
      return toString().equalsIgnoreCase(obj.toString());
    }
  };

  /**
   * Replaces any file extension with the specified one.
   * @param fileName The original filename.
   * @param newExt The new file extension (specified without period).
   * @return The modified filename.
   */
  public static String replaceFileExtension(String fileName, String newExt)
  {
    if (fileName != null) {
      newExt = (newExt == null) ? "" : "." + newExt;
      int pos = fileName.lastIndexOf('.');
      if (pos >= 0) {
        fileName = fileName.substring(0, pos) + newExt;
      } else {
        fileName = fileName + newExt;
      }
    }
    return fileName;
  }

  /**
   * Replaces the file extension only if the old extension matches oldExt.
   * @param fileName The original filename.
   * @param oldExt The file extension to replace (specified without period).
   * @param newExt The new file extension (specified without period).
   * @return The modified filename.
   */
  public static String replaceFileExtension(String fileName, String oldExt, String newExt)
  {
    if (fileName != null) {
      if (oldExt == null) {
        oldExt = "";
      }
      newExt = (newExt == null) ? "" : "." + newExt;
      int pos = fileName.lastIndexOf('.');
      if (pos >= 0) {
        if (fileName.substring(pos+1).equalsIgnoreCase(oldExt)) {
          fileName = fileName.substring(0, pos) + newExt;
        }
      } else if (oldExt.isEmpty()) {
        fileName = fileName + newExt;
      }
    }
    return fileName;
  }

  /**
   * Attempts to detect the character set of the text data in the specified byte buffer.
   * @param data Text data as byte array.
   * @return The detected character set or the ANSI charset "iso-8859-1"
   *         if autodetection was not successful.
   */
  public static Charset detectCharset(byte[] data)
  {
    return detectCharset(data, DEFAULT_CHARSET);
  }

  /**
   * Attempts to detect the character set of the text data in the specified byte buffer.
   * @param data Text data as byte array.
   * @param defaultCharset The default charset to return if autodetection is not successful.
   *                       (Default: ISO-8859-1)
   * @return The detected character set or <code>defaultCharset</code>
   *         if autodetection was not successful.
   */
  public static Charset detectCharset(byte[] data, Charset defaultCharset)
  {
    if (defaultCharset == null) {
      defaultCharset = DEFAULT_CHARSET;
    }

    Charset retVal = null;
    if (data != null) {
      if (data.length >= 3 &&
          data[0] == -17 && data[1] == -69 && data[2] == -65) { // UTF-8 BOM (0xef, 0xbb, 0xbf)
        retVal = Charset.forName("utf-8");
      } else if (data.length >= 2 &&
                 data[0] == -2 && data[1] == -1) {  // UTF-16 BOM (0xfeff) in big-endian order
        retVal = Charset.forName("utf-16be");
      } else if (data.length >= 2 &&
                 data[0] == -1 && data[1] == -2) {  // UTF-16 BOM (0xfeff) in little-endian order
        retVal = Charset.forName("utf-16le");
      }
    }

    if (retVal == null) {
      retVal = defaultCharset;
    }

    return retVal;
  }


  /**
   * Attempts to convert the specified string into a numeric value. Returns defValue of value does
   * not contain a valid number.
   */
  public static int toNumber(String value, int defValue)
  {
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }

  /**
   * Attempts to convert the specified string of given base "radix" into a numeric value.
   * Returns defValue of value does not contain a valid number.
   */
  public static int toNumber(String value, int radix, int defValue)
  {
    if (value != null) {
      try {
        return Integer.parseInt(value, radix);
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }


  /**
   * Creates a thread pool with a pool size depending on the number of available CPU cores.<br>
   * <br>
   * <b>numThreads:</b>   Number of available CPU cores.<br>
   * <b>maxQueueSize:</b> 2 x <code>numThreads</code>.<br>
   * @return A ThreadPoolExecutor instance.
   */
  public static ThreadPoolExecutor createThreadPool()
  {
    int numThreads = Runtime.getRuntime().availableProcessors();
    return createThreadPool(numThreads, numThreads*2);
  }

  /**
   * Creates a thread pool with the specified parameters.
   * @param numThreads Max. number of parallel threads to execute. Must be >= 1.
   * @param maxQueueSize Max. size of the working queue. Must be >= <code>numThreads</code>.
   * @return A ThreadPoolExecutor instance.
   */
  public static ThreadPoolExecutor createThreadPool(int numThreads, int maxQueueSize)
  {
    numThreads = Math.max(1, numThreads);
    maxQueueSize = Math.max(numThreads, maxQueueSize);
    return new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                                  new ArrayBlockingQueue<Runnable>(maxQueueSize));
  }

  /**
   * Helper routine which can be used to check or block execution of new threads while the
   * blocking queue is full.
   * @param executor The executor to query.
   * @param block Specify <code>true</code> to block execution as long as the queue is full.
   * @param maxWaitMs Specify max. time to block queue, in milliseconds. Specify -1 to block indefinitely.
   * @return <code>true</code> if queue is ready for new elements, <code>false</code> otherwise.
   */
  public static boolean isQueueReady(ThreadPoolExecutor executor, boolean block, int maxWaitMs)
  {
    if (executor != null) {
      if (block) {
        if (maxWaitMs < 0) { maxWaitMs = Integer.MAX_VALUE; }
        int curWaitMs = 0;
        while (curWaitMs < maxWaitMs && executor.getQueue().size() > executor.getCorePoolSize()) {
          try { Thread.sleep(1); } catch (InterruptedException e) {}
          curWaitMs++;
        }
      }
      return executor.getQueue().size() <= executor.getCorePoolSize();
    }
    return false;
  }


  // Contains static functions only
  private Misc() {}
}
