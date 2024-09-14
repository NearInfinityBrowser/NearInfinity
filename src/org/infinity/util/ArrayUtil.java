// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public final class ArrayUtil {
  /**
   * Merges two or more byte arrays into one.
   *
   * @param first  The first array to be placed into the new array.
   * @param more   More byte arrays to merge.
   * @return A new byte array containing the data of every specified array.
   */
  public static byte[] mergeArrays(byte[] first, byte[]... more) {
    int totalLength = Objects.requireNonNull(first).length;
    for (final byte[] ar : more) {
      totalLength += Objects.requireNonNull(ar).length;
    }

    final byte[] res = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (final byte[] ar : more) {
      System.arraycopy(ar, 0, res, offset, ar.length);
      offset += ar.length;
    }

    return res;
  }

  /**
   * Merges one or more arrays into a single array.
   *
   * @param first The first array to be placed into the new array.
   * @param more  More arrays to merge.
   * @param <T>   The array type.
   * @return A new array containing the data of every specified array.
   * @throws IllegalArgumentException if any of the arguments is not an array or if not all specified arrays are of
   *                                  the the same component type.
   */
  @SafeVarargs
  public static <T> T mergeArrays(T first, T... more) {
    if (!first.getClass().isArray()) {
      throw new IllegalArgumentException("Argument is not an array");
    }
    final Class<?> compTypeFirst = first.getClass().getComponentType();

    int totalSize = Array.getLength(first);
    for (final T item : more) {
      if (!Objects.requireNonNull(item).getClass().isArray()) {
        throw new IllegalArgumentException("Argument is not an array");
      }

      final Class<?> compType = item.getClass().getComponentType();
      if (!compTypeFirst.equals(compType)) {
        throw new IllegalArgumentException("Arrays have different type");
      }

      totalSize += Array.getLength(item);
    }

    @SuppressWarnings("unchecked")
    final T retVal = (T) Array.newInstance(compTypeFirst, totalSize);

    int ofs = Array.getLength(first);
    System.arraycopy(first, 0, retVal, 0, ofs);
    for (final T item : more) {
      final int size = Array.getLength(item);
      System.arraycopy(item, 0, retVal, ofs, size);
      ofs += size;
    }

    return retVal;
  }

  /**
   * Searches an unsorted array of objects for a specific element in linear time.
   *
   * @param array The object array to search.
   * @param obj   The object to find.
   * @return The array index of the element if found, -1 otherwise.
   */
  public static <T> int indexOf(T[] array, T obj) {
    if (array != null && array.length > 0) {
      for (int i = 0; i < array.length; i++) {
        if (obj == null && array[i] == null) {
          return i;
        } else if (obj != null && obj.equals(array[i])) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Reimplementation of {@code Arrays.compare} from Java 9, because minimum supported version now is Java 8.
   *
   * @param a   the first array to compare
   * @param b   the second array to compare
   * @param <T> the type of array elements which must support the {@link Comparable} interface.
   *
   * @return the value {@code 0} if the first and second array are equal and contain the same elements in the same
   *         order; a value less than {@code 0} if the first array is lexicographically less than the second array; and
   *         a value greater than {@code 0} if the first array is lexicographically greater than the second array
   *
   * @throws NullPointerException if the comparator is {@code null}
   */
  // TODO: Replace with Arrays.compare when minimum supported version raised to Java 9+
  public static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
    return compare(a, b, Comparator.naturalOrder());
  }

  /**
   * Reimplementation of {@code Arrays.compare} from Java 9, because minimum supported version now is Java 8.
   *
   * @param a   the first array to compare
   * @param b   the second array to compare
   * @param cmp the comparator to compare array elements
   * @param <T> the type of array elements
   *
   * @return the value {@code 0} if the first and second array are equal and contain the same elements in the same
   *         order; a value less than {@code 0} if the first array is lexicographically less than the second array; and
   *         a value greater than {@code 0} if the first array is lexicographically greater than the second array
   *
   * @throws NullPointerException if the comparator is {@code null}
   */
  // TODO: Replace with Arrays.compare when minimum supported version raised to Java 9+
  public static <T> int compare(T[] a, T[] b, Comparator<? super T> cmp) {
    Objects.requireNonNull(cmp);
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }

    final int length = Math.min(a.length, b.length);
    for (int i = 0; i < length; ++i) {
      final int res = cmp.compare(a[i], b[i]);

      if (res != 0) {
        return res;
      }
    }

    return a.length - b.length;
  }

  private ArrayUtil() {
  }
}
