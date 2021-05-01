// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

/**
 * Allows read/write access to the sixth tuple element.
 */
public interface TupleValue5<T>
{
  /**
   * Returns the sixth element of the tuple.
   * @return sixth element of the tuple.
   */
  public T getValue5();

  /**
   * Assigns a new value to the sixth element of the tuple.
   * @param newValue the new value to assign.
   * @return the previously assigned value of the sixth tuple element.
   */
  public T setValue5(T newValue);
}
