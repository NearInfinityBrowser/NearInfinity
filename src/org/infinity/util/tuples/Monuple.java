// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

import java.util.Collection;
import java.util.Iterator;

/**
 * A tuple class that can store one element.
 */
public class Monuple<A> extends Tuple implements TupleValue0<A>
{
  private static final int SIZE = 1;

  private A value0;

  /**
   * Creates a new tuple instance with the specified element.
   * @param <A> the tuple element type.
   * @param value0 The element to store in the tuple.
   * @return A new tuple instance.
   */
  public static <A> Monuple<A> with(A value0)
  {
    return new Monuple<>(value0);
  }

  /**
   * Creates a new tuple from the array. The array must contain at least 1 element.
   * @param <A> the tuple element type.
   * @param arr the array to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Monuple<T> fromArray(T[] arr)
  {
    if (arr == null) {
      throw new IllegalArgumentException("Array cannot be null");
    }
    if (arr.length < SIZE) {
      throw new IllegalArgumentException("Array must contain at least 1 element");
    }
    return new Monuple<>(arr[0]);
  }

  /**
   * Creates a new tuple from the collection. The collection must contain at least 1 element.
   * @param <A> the tuple element type.
   * @param col the collection to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Monuple<T> fromCollection(Collection<T> col)
  {
    if (col == null) {
      throw new IllegalArgumentException("Collection cannot be null");
    }
    if (col.size() < SIZE) {
      throw new IllegalArgumentException("Collection must contain at least 1 element");
    }
    Iterator<T> iter = col.iterator();
    return new Monuple<>(iter.next());
  }

  /**
   * Creates a new tuple from the {@code Iterable} object.
   * @param <A> the tuple element type.
   * @param iterator the {@code Iterable} object to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Monuple<T> fromIterable(Iterable<T> iterator)
  {
    return fromIterable(iterator, 0);
  }

  /**
   * Creates a new tuple from the {@code Iterable} object, starting the specified index.
   * @param <A> the tuple element type.
   * @param iterator the {@code Iterable} object to be used as source for the tuple.
   * @param index start index in {@code Iterable} object.
   * @return A new tuple instance.
   */
  public static <T> Monuple<T> fromIterable(Iterable<T> iterator, int index)
  {
    if (iterator == null) {
      throw new IllegalArgumentException("Iterator cannot be null");
    }

    Iterator<T> iter = iterator.iterator();
    for (int i = 0; i < index; i++) {
      if (iter.hasNext()) {
        iter.next();
      }
      i++;
    }

    T el0;
    if (iter.hasNext()) {
      el0 = iter.next();
    } else {
      el0 = null;
    }
    return new Monuple<>(el0);
  }

  /**
   * Constructs a new Monuple instance and initializes it with the specified arguments.
   * @param value0 the value of the Monuple.
   */
  public Monuple(A value0)
  {
    super(value0);
    this.value0 = value0;
  }

  @Override
  public int size()
  {
    return SIZE;
  }

  @Override
  public A getValue0()
  {
    return value0;
  }

  @Override
  public A setValue0(A newValue)
  {
    A retVal = value0;
    setValue(0, newValue);
    value0 = newValue;
    return retVal;
  }
}
