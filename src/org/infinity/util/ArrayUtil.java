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
   * @param more More byte arrays to merge.
   * @return A new byte array containing the data of every specified array.
   */
  public static byte[] mergeArrays(byte[] first, byte[]... more)
  {
    int totalLength = first.length;
    for (byte[] ar: more) {
      totalLength += ar.length;
    }

    byte[] res = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (byte[] ar: more) {
      System.arraycopy(ar, 0, res, offset, ar.length);
      offset += ar.length;
    }

    return res;
  }

  /**
   * Merges two or more generic arrays of the same type into one.
   * Note: The result for arrays of different types but common base type is undefined.
   * @param first The first array to be placed into the new array.
   * @param more More arrays of the same type to merge.
   * @return A new array containing the data of all specified arrays.
   */
  public static <T> T[] mergeArrays(T[] first, T[]... more)
  {
    int totalLength = first.length;
    for (T[] ar: more) {
      totalLength += ar.length;
    }

    T[] res = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (T[] ar: more) {
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

