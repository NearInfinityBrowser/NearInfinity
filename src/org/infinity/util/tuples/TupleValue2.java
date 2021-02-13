// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

/**
 * Allows read/write access to the third tuple element.
 */
public interface TupleValue2<T>
{
  /**
   * Returns the third element of the tuple.
   * @return third element of the tuple.
   */
  public T getValue2();

  /**
   * Assigns a new value to the third element of the tuple.
   * @param newValue the new value to assign.
   * @return the previously assigned value of the third tuple element.
   */
  public T setValue2(T newValue);
}
