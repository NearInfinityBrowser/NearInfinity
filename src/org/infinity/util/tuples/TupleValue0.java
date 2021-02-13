// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

/**
 * Allows read/write access to the first tuple element.
 */
public interface TupleValue0<T>
{
  /**
   * Returns the first element of the tuple.
   * @return first element of the tuple.
   */
  public T getValue0();

  /**
   * Assigns a new value to the first element of the tuple.
   * @param newValue the new value to assign.
   * @return the previously assigned value of the first tuple element.
   */
  public T setValue0(T newValue);
}
