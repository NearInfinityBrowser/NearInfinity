// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

/**
 * Allows read/write access to the fourth tuple element.
 */
public interface TupleValue3<T>
{
  /**
   * Returns the fourth element of the tuple.
   * @return fourth element of the tuple.
   */
  public T getValue3();

  /**
   * Assigns a new value to the fourth element of the tuple.
   * @param newValue the new value to assign.
   * @return the previously assigned value of the fourth tuple element.
   */
  public T setValue3(T newValue);
}
