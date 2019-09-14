// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

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

  /**
   * Reimplementation of {@code Arrays.compare} from Java 9, because minimum
   * supported version now is Java 8.
   *
   * @param a the first array to compare
   * @param b the second array to compare
   * @param cmp the comparator to compare array elements
   * @param <T> the type of array elements
   *
   * @return the value {@code 0} if the first and second array are equal and
   *         contain the same elements in the same order;
   *         a value less than {@code 0} if the first array is
   *         lexicographically less than the second array; and
   *         a value greater than {@code 0} if the first array is
   *         lexicographically greater than the second array
   *
   * @throws NullPointerException if the comparator is {@code null}
   */
  public static <T> int compare(T[] a, T[] b, Comparator<? super T> cmp)
  {
    //TODO: Replace with Arrays.compare when minimum supported version raised to Java 9
    Objects.requireNonNull(cmp);
    if (a == b) return 0;
    if (a == null) return -1;
    if (b == null) return 1;

    final int length = Math.min(a.length, b.length);
    for (int i = 0; i < length; ++i) {
      final int res = cmp.compare(a[i], b[i]);

      if (res != 0) { return res; }
    }

    return a.length - b.length;
  }

  private ArrayUtil(){}
}

