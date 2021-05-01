// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

/**
 * Allows read/write access to the fifth tuple element.
 */
public interface TupleValue4<T>
{
  /**
   * Returns the fifth element of the tuple.
   * @return fifth element of the tuple.
   */
  public T getValue4();

  /**
   * Assigns a new value to the fifth element of the tuple.
   * @param newValue the new value to assign.
   * @return the previously assigned value of the fifth tuple element.
   */
  public T setValue4(T newValue);
}
