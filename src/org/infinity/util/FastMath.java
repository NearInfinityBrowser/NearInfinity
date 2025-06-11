// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * Fast but less accurate table-based implementation of sine and cosine functions.
 * <p>
 * Based on Riven's sine/cosine implementation:
 * https://web.archive.org/web/20200202153540/http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html
 * </p>
 */
public class FastMath {
  /** Generate sine and cosine tables of <code>2<sup>TABLE_BITS</sup></code> precalculated entries. */
  private static final int TABLE_BITS = 16;

  private static final int TABLE_COUNT = 1 << TABLE_BITS;
  private static final int TABLE_MASK = TABLE_COUNT - 1;
  private static final int SIN_SHIFT = TABLE_COUNT >> 2;
  private static final double RAD_FULL = Math.PI * 2.0;
  private static final double RAD_TO_INDEX = TABLE_COUNT / RAD_FULL;
  private static final double[] COS_TABLE = new double[TABLE_COUNT];

  static {
    // precomputing cosine table entries
    for (int i = 0; i < TABLE_COUNT; i++) {
      COS_TABLE[i] = Math.cos((i + 0.5) / TABLE_COUNT * RAD_FULL);
    }

    // four cardinal directions should be accurately calculated
    final double degToIndex = TABLE_COUNT / 360.0;
    for (int i = 0; i < 360; i += 90) {
      COS_TABLE[(int)(i * degToIndex) & TABLE_MASK] = Math.cos(i * Math.PI / 180.0);
    }
  }

  /**
   * Returns the trigonometric sine of an angle.
   *
   * @param rad an angle, in radians.
   * @return the sine of the argument.
   */
  public static double sin(double rad) {
    final int index = ((int)(rad * RAD_TO_INDEX) + SIN_SHIFT) & TABLE_MASK;
    return COS_TABLE[index];
  }

  /**
   * Returns the trigonometric cosine of an angle.
   *
   * @param rad an angle, in radians.
   * @return the cosine of the argument.
   */
  public static double cos(double rad) {
  final int index = (int)(rad * RAD_TO_INDEX) & TABLE_MASK;
  return COS_TABLE[index];
  }
}
