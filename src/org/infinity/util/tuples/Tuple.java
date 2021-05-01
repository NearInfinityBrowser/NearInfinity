// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.tuples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public abstract class Tuple implements Iterable<Object>, Comparable<Tuple>
{
  public static final Function<List<Object>, String> FMT_DEFAULT = (list) -> list.toString();

  private final Object[] values;
  private final List<Object> valueList;

  private Function<List<Object>, String> formatter;

  protected Tuple(Object... values)
  {
    this.values = values;
    this.valueList = Arrays.asList(this.values);
  }

  /**
   * Returns the number of elements that can be stored in the tuple.
   * @return size of the tuple.
   */
  public abstract int size();

  /**
   * Returns the value at the specified element position.
   * @param pos the element position in the tuple.
   * @return the value.
   */
  public Object getValue(int pos)
  {
    if (pos < 0 || pos >= size()) {
      throw new IllegalArgumentException(String.format("Invalid position: %d. Valid range: [0, %d]", pos, size() - 1));
    }
    return values[pos];
  }

  /**
   * Assigns a new value to specified element position.
   * @param pos the element position in the tuple.
   * @param value the new value.
   * @return the previous value.
   */
  public Object setValue(int pos, Object value)
  {
    if (pos < 0 || pos >= size()) {
      throw new IllegalArgumentException(String.format("Invalid position: %d. Valid range: [0, %d]", pos, size() - 1));
    }
    Object retVal = values[pos];
    values[pos] = pos;
    valueList.set(pos, value);
    return retVal;
  }

  /**
   * Returns an iterator over the elements in this tuple in proper sequence.
   */
  @Override
  public Iterator<Object> iterator()
  {
    return this.valueList.iterator();
  }

  /**
   * Returns a functional interface used to generate the return value of the {@link #toString()} method.
   * <p>It takes a list of {@code Objects} which represent the values assigned to the tuple and returns a
   * textual representation of the values.
   * @return Function to generate the return value of the {@link #toString()} method.
   */
  public Function<List<Object>, String> getFormatter()
  {
    return (formatter != null) ? formatter : FMT_DEFAULT;
  }

  /**
   * Assigns a new functional interface responsible for generating the return value of the {@link #toString()} method.
   * <p>It takes a list of {@code Objects} which represent the values assigned to the tuple and returns a
   * textual representation of the values.
   * @param formatter the {@code Function} to generate the return value of the {@link #toString()} method.
   *                  Specify {@code null} to revert to the default function.
   */
  public void setFormatter(Function<List<Object>, String> formatter)
  {
    this.formatter = formatter;
  }

  @Override
  public String toString()
  {
    return getFormatter().apply(valueList);
  }

  /**
   * Returns {@code true} if this tuple contains the specified element.
   * More formally, returns {@code true} if and only if this tuple contains at least one element {@code e}
   * such that {@code (o==null ? e==null : o.equals(e))}.
   * @param o  element whose presence in this tuple is to be tested.
   * @return {@code true} if this list contains the specified element.
   */
  public boolean contains(Object o)
  {
    return indexOf(o) >= 0;
  }

  /**
   * Returns {@code true} if this tuple contains all of the elements of the specified collection.
   * @param collection collection to be checked for containment in this tuple.
   * @return {@code true} if this tuple contains all of the elements of the specified collection.
   */
  public boolean containsAll(Collection<?> collection)
  {
    for (final Object o : collection) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if this tuple contains all of the elements of the specified array.
   * @param values array to be checked for containment in this tuple.
   * @return {@code true} if this tuple contains all of the elements of the specified array.
   */
  public boolean containsAll(Object... values)
  {
    if (values == null) {
      throw new IllegalArgumentException("Specified argument cannot be null");
    }
    for (final Object o : values) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the index of the first occurrence of the specified element in this tuple, or -1 if this tuple
   * does not contain the element.
   * More formally, returns the lowest index {@code i} such that {@code (o==null ? get(i)==null : o.equals(get(i)))},
   * or -1 if there is no such index.
   * @param o element to search for.
   * @return the index of the first occurrence of the specified element in this tuple,
   *         or -1 if this tuple does not contain the element.
   */
  public int indexOf(Object o)
  {
    int retVal = 0;
    for (final Object v : valueList) {
      if (v == null) {
        if (o == null) {
          return retVal;
        }
      } else {
        if (v.equals(o)) {
          return retVal;
        }
      }
      retVal++;
    }
    return -1;
  }

  /**
   * Returns the index of the last occurrence of the specified element in this tuple, or -1 if this tuple
   * does not contain the element.
   * More formally, returns the highest index {@code i} such that {@code (o==null ? get(i)==null : o.equals(get(i)))},
   * or -1 if there is no such index.
   * @param o element to search for.
   * @return the index of the last occurrence of the specified element in this tuple,
   *         or -1 if this tuple does not contain the element.
   */
  public int lastIndexOf(Object o)
  {
    for (int i = size() - 1; i >= 0; i--) {
      final Object v = valueList.get(i);
      if (v == null) {
        if (o == null) {
          return i;
        }
      } else {
        if (v.equals(o)) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns an unmodifiable list of the elements stored in this tuple.
   * @return unmodifiable list with the elements of this tuple.
   */
  public List<Object> toList()
  {
    return Collections.unmodifiableList(new ArrayList<>(valueList));
  }

  /**
   * Returns an array with the elements stored in this tuple.
   * @return array with the elements of this tuple.
   */
  public Object[] toArray()
  {
    return values.clone();
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (getClass() != o.getClass()) {
      return false;
    }
    Tuple other = (Tuple)o;
    return valueList.equals(other.valueList);
  }

  @Override
  public int hashCode()
  {
    return valueList.hashCode();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public int compareTo(Tuple o)
  {
    int len = values.length;
    Object[] values2 = o.values;
    int len2 = values2.length;

    for (int i = 0; i < len && i < len2; i++) {
      Comparable el = (Comparable)values[i];
      Comparable el2 = (Comparable)values2[i];

      int cmp = el.compareTo(el2);
      if (cmp != 0) {
        return cmp;
      }
    }

    return (len - len2);
  }
}
