// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

import java.util.Collection;
import java.util.Iterator;

/**
 * A tuple class that can store two elements.
 */
public class Couple<A, B> extends Tuple implements TupleValue0<A>, TupleValue1<B>
{
  private static final int SIZE = 2;

  private A value0;
  private B value1;

  /**
   * Creates a new tuple instance with the specified elements.
   * @param <A> the tuple element type.
   * @param value0 The first element to store in the tuple.
   * @param value1 The second element to store in the tuple.
   * @return A new tuple instance.
   */
  public static <A, B> Couple<A, B> with(A value0, B value1)
  {
    return new Couple<>(value0, value1);
  }

  /**
   * Creates a new tuple from the array. The array must contain at least 2 elements.
   * @param <A> the tuple element type.
   * @param arr The array to be used as source for the tuple.
   * @return A new tuple instance.
   */
  public static <T> Couple<T, T> fromArray(T[] arr)
  {
    if (arr == null) {
      throw new IllegalArgumentException("Array cannot be null");
    }
    if (arr.length < SIZE) {
      throw new IllegalArgumentException(String.format("Array must contain at least %d elements", SIZE));
    }
    return new Couple<>(arr[0], arr[1]);
  }

  /**
   * Creates a new tuple from the collection. The collection must contain at least 2 elements.
   * @param <A> the tuple element type.
   * @param col the collection to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Couple<T, T> fromCollection(Collection<T> col)
  {
    if (col == null) {
      throw new IllegalArgumentException("Collection cannot be null");
    }
    if (col.size() < SIZE) {
      throw new IllegalArgumentException(String.format("Collection must contain at least %d elements", SIZE));
    }
    Iterator<T> iter = col.iterator();
    T el0 = iter.next();
    T el1 = iter.next();
    return new Couple<>(el0, el1);
  }

  /**
   * Creates a new tuple from the {@code Iterable} object.
   * @param <A> the tuple element type.
   * @param iterator the {@code Iterable} object to be used as source for the tuple.
   * @return a new tuple instance.
   */
  public static <T> Couple<T, T> fromIterable(Iterable<T> iterator)
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
  public static <T> Couple<T, T> fromIterable(Iterable<T> iterator, int index)
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

    T el0 = iter.hasNext() ? iter.next() : null;
    T el1 = iter.hasNext() ? iter.next() : null;
    return new Couple<>(el0, el1);
  }

  /**
   * Constructs a new Couple instance and initializes it with the specified arguments.
   * @param value0 the first value of the Couple.
   * @param value1 the second value of the Couple.
   */
  public Couple(A value0, B value1)
  {
    super(value0, value1);
    this.value0 = value0;
    this.value1 = value1;
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

  @Override
  public B getValue1()
  {
    return value1;
  }

  @Override
  public B setValue1(B newValue)
  {
    B retVal = value1;
    setValue(1, newValue);
    value1 = newValue;
    return retVal;
  }
}
