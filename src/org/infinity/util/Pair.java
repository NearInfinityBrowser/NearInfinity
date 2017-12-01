// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * A simple Pair class to store arbitrary objects A and B.
 * Both objects must share a common base class.
 * @param <V> The common base class for both objects.
 */
public class Pair<V> implements Cloneable
{
  private V first, second;

  /**
   * Creates a pair object with both elements initialized to {@code null}.
   */
  public Pair()
  {
    this.first = null;
    this.second = null;
  }

  /**
   * Creates a pair object, setting the first element to the specified value and
   * setting the second element to {@code null}.
   * @param first The value of the first element.
   */
  public Pair(V first)
  {
    this.first = first;
    this.second = null;
  }

  /**
   * Creates a pair object, setting both elements to the specified values.
   * @param first The value of the first element.
   * @param second The value of the second element.
   */
  public Pair(V first, V second)
  {
    this.first = first;
    this.second = second;
  }

  /**
   * Returns the value of the specified element.
   * @param index Specifies the element to return (0=first, 1=second).
   * @return The value specified by index.
   */
  public V get(int index)
  {
    return ((index & 1) == 0) ? first : second;
  }

  /**
   * Sets the value of the element specified by index.
   * @param index Specifies the element to set (0=first, 1=second).
   * @param value The value to set to the specified element.
   * @return The old value of the specified element.
   */
  public V set(int index, V value)
  {
    V retVal;
    if ((index & 1) == 0) {
      retVal = first;
      first = value;
    } else {
      retVal = second;
      second = value;
    }
    return retVal;
  }

  /**
   * Returns the value of the first element.
   * @return The value of the first element.
   */
  public V getFirst()
  {
    return first;
  }

  /**
   * Sets the value of the first element.
   * @param value The value for the first element.
   * @return The old value of the first element.
   */
  public V setFirst(V value)
  {
    V retVal = first;
    first = value;
    return retVal;
  }

  /**
   * Returns the value of the second element.
   * @return The value of the second element.
   */
  public V getSecond()
  {
    return second;
  }

  /**
   * Sets the value of the second element.
   * @param value The value for the second element.
   * @return The old value of the second element.
   */
  public V setSecond(V value)
  {
    V retVal = second;
    second = value;
    return retVal;
  }

  /**
   * Swaps the content of both elements.
   * After the swap the first element contains the value of the second element
   * and the second element contains the value of the first element.
   * @return A reference to this object. Useful for chaining actions.
   */
  public Pair<V> swap()
  {
    V tmp = first;
    first = second;
    second = tmp;
    return this;
  }

  /**
   * Returns a shallow copy of this Pair&lt;V&gt; instance. (The elements themselves are not cloned.)
   * @return a clone of this Pair&lt;V&gt; instance
   */
  @Override
  public Object clone()
  {
    return new Pair<V>(getFirst(), getSecond());
  }

  @Override
  public boolean equals(Object o)
  {
    if (o != null && o instanceof Pair) {
      boolean retVal = true;
      if (getFirst() != null) {
        retVal &= getFirst().equals(((Pair<?>)o).getFirst());
      }
      if (getSecond() != null) {
        retVal &= getSecond().equals(((Pair<?>)o).getSecond());
      }
      return retVal;
    }
    return false;
  }

  @Override
  public String toString()
  {
    return String.format("[%s, %s]", getFirst(), getSecond());
  }
}
