// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

/**
 * Available cardinal directions for animation sequences.
 */
public enum Direction {
  /** South */
  S(0),
  /** South-southwest */
  SSW(1),
  /** Southwest */
  SW(2),
  /** West-southwest */
  WSW(3),
  /** West */
  W(4),
  /** West-northwest */
  WNW(5),
  /** Northwest */
  NW(6),
  /** North-northwest */
  NNW(7),
  /** North */
  N(8),
  /** North-northeast */
  NNE(9),
  /** Northeast */
  NE(10),
  /** East-northeast */
  ENE(11),
  /** East */
  E(12),
  /** East-southeast */
  ESE(13),
  /** Southeast */
  SE(14),
  /** South-southeast */
  SSE(15);

  private final int dir;
  private Direction(int dir) { this.dir = dir; }

  /** Returns the numeric direction value. */
  public int getValue() { return dir; }

  /**
   * Determines the {@link Direction} instance associated with the specified numeric value and returns it.
   * Return {@code null} if association could not be determined.
   */
  public static Direction from(int value) {
    for (final Direction d : Direction.values()) {
      if (d.getValue() == value) {
        return d;
      }
    }
    return null;
  }
}
