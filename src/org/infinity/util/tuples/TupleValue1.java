// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

/**
 * Allows read/write access to the second tuple element.
 */
public interface TupleValue1<T>
{
  /**
   * Returns the second element of the tuple.
   * @return second element of the tuple.
   */
  public T getValue1();

  /**
   * Assigns a new value to the second element of the tuple.
   * @param newValue the new value to assign.
   * @return the previously assigned value of the second tuple element.
   */
  public T setValue1(T newValue);
}
