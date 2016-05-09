package org.infinity.util;

import org.junit.Assert;
import org.junit.Test;
import junit.framework.TestCase;

public class ArrayUtilTest {
  //public static byte[] mergeArrays(byte[] first, byte[] second, byte[]... more)
  @Test
  public void testMergeArraysWithTwoArrays() {
    Assert.assertArrayEquals(ArrayUtil.mergeArrays(new byte[]{1, 2}, new byte[]{3, 4}), new byte[]{1, 2, 3, 4});
  }

  @Test
  public void testMergeArraysWithThreeArrays() {
    Assert.assertArrayEquals(ArrayUtil.mergeArrays(new byte[]{1, 2}, new byte[]{3, 4}, new byte[]{5, 6}), new byte[]{1, 2, 3, 4, 5, 6});
  }

  @Test(expected=NullPointerException.class)
  public void testMergeArraysWithNullFirst() {
    ArrayUtil.mergeArrays(null, new byte[]{1, 2});
  }

  @Test(expected=NullPointerException.class)
  public void testMergeArraysWithNullSecond() {
    ArrayUtil.mergeArrays(new byte[]{1, 2}, null);
  }

  @Test(expected=NullPointerException.class)
  public void testMergeArraysWithNullInMore() {
    ArrayUtil.mergeArrays(new byte[]{1, 2}, new byte[]{3, 4}, null, new byte[]{5, 6});
  }

  //public static<T> int indexOf(T[] array, T obj)
  @Test
  public void testIndexOfWithExistingEntryInFirst() {
    Assert.assertEquals(0, ArrayUtil.indexOf(new String[]{"A", "B", "C"}, "A"));
  }

  @Test
  public void testIndexOfWithExistingEntryInMiddle() {
    Assert.assertEquals(1, ArrayUtil.indexOf(new String[]{"A", "B", "C"}, "B"));
  }

  @Test
  public void testIndexOfWithExistingEntryInLast() {
    Assert.assertEquals(2, ArrayUtil.indexOf(new String[]{"A", "B", "C"}, "C"));
  }

  @Test
  public void testIndexOfWithNonExistentEntry() {
    Assert.assertEquals(-1, ArrayUtil.indexOf(new String[]{"A", "B", "C"}, "Z"));
  }

  @Test
  public void testIndexOfWithEmptyArray() {
    Assert.assertEquals(-1, ArrayUtil.indexOf(new String[]{}, "A"));
  }

  @Test
  public void testIndexOfWithNullArray() {
    Assert.assertEquals(-1, ArrayUtil.indexOf(null, "A"));
  }

  @Test
  public void testIndexOfWithNonExistentNullSearch() {
    Assert.assertEquals(-1, ArrayUtil.indexOf(new String[]{"A", "B", "C"}, null));
  }

  @Test
  public void testIndexOfWithExistentNullSearch() {
    Assert.assertEquals(1, ArrayUtil.indexOf(new String[]{"A", null, "C"}, null));
  }
}
