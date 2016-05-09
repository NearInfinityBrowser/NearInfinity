// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Arrays;

public final class ArrayUtil
{
  /**
   * Merges two or more byte arrays into one.
   * @param first The first array to be placed into the new array.
   * @param second The second array, needed to ensure a minimum parameter count of 2.
   * @param more More byte arrays to merge.
   * @return A new byte array containing the data of every specified array.
   */
  public static byte[] mergeArrays(byte[] first, byte[] second, byte[]... more)
  {
    int totalLength = first.length + second.length;
    for (byte[] ar: more) {
      totalLength += ar.length;
    }

    byte[] res = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    System.arraycopy(second, 0, res, offset, second.length);
    offset += second.length;
    for (byte[] ar: more) {
      System.arraycopy(ar, 0, res, offset, ar.length);
      offset += ar.length;
    }

    return res;
  }

  /**
   * Searches an unsorted array of objects for a specific element in linear time.
   * @param array The object array to search.
   * @param obj The object to find.
   * @return The array index of the element if found, -1 otherwise.
   */
  public static<T> int indexOf(T[] array, T obj)
  {
    if (array != null && array.length > 0) {
      for (int i = 0; i < array.length; i++) {
        if (array[i] == obj) {
          return i;
        }
      }
    }
    return -1;
  }

  private ArrayUtil(){}
}

