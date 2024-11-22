// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * This enum type acts as a boolean with a third, undefined, state.
 */
public enum TriState {
  /** Represents the boolean value of {@code true}. */
  TRUE(Boolean.TRUE),
  /** Represents the boolean value of {@code false}. */
  FALSE(Boolean.FALSE),
  /** Represent the absence of a defined boolean result. */
  UNDEFINED(null),
  ;

  /**
   * Returns a {@link TriState} enum instance that represents the specified parameter.
   *
   * @param b {@code boolean} value to represent. {@code null} is supported and represents the {@link #UNDEFINED} state.
   * @return {@link TriState} representation of the argument.
   */
  public static TriState of(Boolean b) {
    if (b == null) {
      return UNDEFINED;
    } else {
      return b ? TRUE : FALSE;
    }
  }

  private final Boolean value;

  private TriState(Boolean b) {
    this.value = b;
  }

  /**
   * Returns {@code true} if the specified parameter is equal to the state of this {@link TriState} enum instance.
   *
   * @param b {@code boolean} value to test. {@code null} is supported and represents the {@link #UNDEFINED} state.
   * @return {@code true} if the specified argument is equal to the current {@link TriState} enum, {@code false}
   *         otherwise.
   */
  public boolean is(Boolean b) {
    if (b == null) {
      return isUndefined();
    } else {
      return (b.equals(value));
    }
  }

  /**
   * Returns whether this {@link TriState} enum instance represents the boolean value {@code true}.
   *
   * @return {@code true} for {@link #TRUE}, {@code false} otherwise.
   */
  public boolean isTrue() {
    return (value != null && value.booleanValue());
  }

  /**
   * Returns whether this {@link TriState} enum instance represents the boolean value {@code false}.
   *
   * @return {@code true} for {@link #FALSE}, {@code false} otherwise.
   */
  public boolean isFalse() {
    return (value != null && !value.booleanValue());
  }

  /**
   * Returns whether this {@link TriState} enum instance represents the undefined state.
   *
   * @return {@code true} for {@link #UNDEFINED}, {@code false} otherwise.
   */
  public boolean isUndefined() {
    return (value == null);
  }

  @Override
  public String toString() {
    if (value != null) {
      return value.toString();
    } else {
      return "undefined";
    }
  }
}
