// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;

/**
 * A speed optimized alternative of the {@code DefaultListModel}.
 */
public class SimpleListModel<E> extends AbstractListModel<E>
{
  private final Vector<E> delegate = new Vector<>();

  /** Constructs an empty ListModel object with a default capacity of 10 elements. */
  public SimpleListModel()
  {
  }

  /** Constructs a ListModel object containing the elements from the specified array. */
  public SimpleListModel(E[] items)
  {
    delegate.ensureCapacity(items.length);
    for (int i = 0, c = items.length; i < c; i++) {
      delegate.add(items[i]);
    }
  }

  /** Constructs a ListModel object containing the elements from the specified collection. */
  public SimpleListModel(Collection<E> items)
  {
    delegate.addAll(items);
  }

  @Override
  public E getElementAt(int index)
  {
    return delegate.elementAt(index);
  }

  @Override
  public int getSize()
  {
    return delegate.size();
  }

  @Override
  public String toString()
  {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object o)
  {
    return delegate.equals(o);
  }

  @Override
  public int hashCode()
  {
    return delegate.hashCode();
  }

  /** Returns a view of the portion of this List between fromIndex, inclusive, and toIndex, exclusive. */
  public List<E> subList(int fromIndex, int toIndex)
  {
    return delegate.subList(fromIndex, toIndex);
  }

  /** Copies the components of this list into the specified array. */
  public void copyInto(E[] anArray)
  {
    delegate.copyInto(anArray);
  }

  /** Trims the capacity of this list to be the list's current size. */
  public void trimToSize()
  {
    delegate.trimToSize();
  }

  /**
   * Increases the capacity of this list, if necessary, to ensure that it can hold at least
   * the number of components specified by the minimum capacity argument.
   */
  public void ensureCapacity(int minCapacity)
  {
    delegate.ensureCapacity(minCapacity);
  }

  /** Sets the size of this list. */
  public void setSize(int newSize)
  {
    int oldSize = delegate.size();
    delegate.setSize(newSize);
    if (oldSize > newSize) {
      fireIntervalRemoved(this, newSize, oldSize - 1);
    } else if (oldSize < newSize) {
      fireIntervalAdded(this, oldSize, newSize - 1);
    }
  }

  /** Returns the current capacity of this list. */
  public int capacity()
  {
    return delegate.capacity();
  }

  /** Returns the number of components in this list. */
  public int size()
  {
    return delegate.size();
  }

  /** Tests if this list has no components. */
  public boolean isEmpty()
  {
    return delegate.isEmpty();
  }

  /** Returns an enumeration of the components of this list. */
  public Enumeration<E> elements()
  {
    return delegate.elements();
  }

  /** Returns {@code true} if this list contains the specified element. */
  public boolean contains(E item)
  {
    return delegate.contains(item);
  }

  /** Returns true if this list contains all of the elements in the specified Collection. */
  public boolean containsAll(Collection<?> c)
  {
    return delegate.containsAll(c);
  }

  /**
   * Returns the index of the first occurrence of the specified element in this list,
   * or -1 if this list does not contain the element.
   */
  public int indexOf(E item)
  {
    return delegate.indexOf(item);
  }

  /**
   * Returns the index of the first occurrence of the specified element in this list,
   * searching forwards from {@code index}, or returns -1 if the element is not found.
   */
  public int indexOf(E item, int index)
  {
    return delegate.indexOf(item, index);
  }

  /**
   * Returns the index of the last occurrence of the specified element in this list,
   * or -1 if this list does not contain the element.
   */
  public int lastIndexOf(E item)
  {
    return delegate.lastIndexOf(item);
  }

  /**
   * Returns the index of the last occurrence of the specified element in this list,
   * searching backwards from {@code index}, or returns -1 if the element is not found.
   */
  public int lastIndexOf(E item, int index)
  {
    return delegate.lastIndexOf(item, index);
  }

  /** Returns the component at the specified index. */
  public E elementAt(int index)
  {
    return delegate.elementAt(index);
  }

  /** Returns the first component (the item at index {@code 0}) of this list. */
  public E firstElement()
  {
    return delegate.firstElement();
  }

  /** Returns the last component of the list. */
  public E lastElement()
  {
    return delegate.lastElement();
  }

  /**
   * Sets the component at the specified {@code index} of this list to be the specified object.
   * The previous component at that position is discarded.
   */
  public void setElementAt(E item, int index)
  {
    delegate.setElementAt(item, index);
    fireContentsChanged(this, index, index);
  }

  /** Deletes the component at the specified index. */
  public void removeElementAt(int index)
  {
    delegate.removeElementAt(index);
    fireIntervalRemoved(this, index, index);
  }

  /** Inserts the specified object as a component in this list at the specified {@code index}. */
  public void insertElementAt(E item, int index)
  {
    delegate.insertElementAt(item, index);
    fireIntervalAdded(this, index, index);
  }

  /** Adds the specified component to the end of this list, increasing its size by one. */
  public void addElement(E item)
  {
    int index = delegate.size();
    delegate.addElement(item);
    fireIntervalAdded(this, index, index);
  }

  /** Removes the first (lowest-indexed) occurrence of the argument from this list. */
  public boolean removeElement(E item)
  {
    int index = delegate.indexOf(item);
    boolean ret = (index >= 0);
    if (index >= 0) {
      delegate.removeElementAt(index);
      fireIntervalRemoved(this, index, index);
    }
    return ret;
  }

  /** Removes all components from this list and sets its size to zero. */
  public void removeAllElements()
  {
    int index1 = delegate.size() - 1;
    delegate.removeAllElements();
    if (index1 > 0) {
      fireIntervalRemoved(this, 0, index1);
    }
  }

  /** Returns an array containing all of the elements in this list in the correct order. */
  public Object[] toArray()
  {
    return delegate.toArray();
  }

  /** Returns the element at the specified position in this list. */
  public E get(int index)
  {
    return delegate.get(index);
  }

  /** Replaces the element at the specified position in this list with the specified element. */
  public E set(int index, E newItem)
  {
    E retVal = delegate.get(index);
    delegate.set(index, newItem);
    fireContentsChanged(this, index, index);
    return retVal;
  }

  /** Appends the specified element to the end of this list. */
  public boolean add(E item)
  {
    int index = delegate.size();
    boolean retVal = delegate.add(item);
    fireIntervalAdded(this, index, index);
    return retVal;
  }

  /** Inserts the specified element at the specified position in this list. */
  public void add(int index, E item)
  {
    delegate.add(index, item);
    fireIntervalAdded(this, index, index);
  }

  /**
   * Appends all of the elements in the specified Array to the end of this list.
   */
  public boolean addAll(E[] items)
  {
    return addAll(delegate.size(), Arrays.asList(items));
  }

  /**
   * Inserts all of the elements in the specified Array into this list
   * at the specified position.
   */
  public boolean addAll(int index, E[] items)
  {
    return addAll(index, Arrays.asList(items));
  }

  /**
   * Appends all of the elements in the specified Collection to the end of this list,
   * in the order that they are returned by the specified Collection's Iterator.
   */
  public boolean addAll(Collection<E> coll)
  {
    return addAll(delegate.size(), coll);
  }

  /**
   * Inserts all of the elements in the specified Collection into this list
   * at the specified position.
   */
  public boolean addAll(int index, Collection<E> coll)
  {
    int index0 = index;
    int index1 = index0 + coll.size() - 1;
    delegate.addAll(index, coll);
    if (index1 >= index0) {
      fireIntervalAdded(this, index0, index1);
    }
    return (index1 >= index0);
  }

  /** Removes the element at the specified position in this list. */
  public E remove(int index)
  {
    E retVal = delegate.remove(index);
    fireIntervalRemoved(this, index, index);
    return retVal;
  }

  /** Removes the first occurrence of the specified element in this list. */
  public boolean remove(E item)
  {
    int index = delegate.indexOf(item);
    if (index >= 0) {
      delegate.remove(index);
      fireIntervalRemoved(this, index, index);
    }
    return (index >= 0);
  }

  /** Removes from this list all of its elements that are contained in the specified Collection. */
  public boolean removeAll(Collection<?> c)
  {
    boolean modified = false;
    for (int idx = delegate.size() - 1; idx >= 0; idx--) {
      if (c.contains(delegate.get(idx))) {
        delegate.remove(idx);
        fireIntervalRemoved(this, idx, idx);
        modified = true;
      }
    }
    return modified;
  }

  /** Retains only the elements in this list that are contained in the specified Collection. */
  public boolean retainAll(Collection<?> c)
  {
    boolean modified = false;
    for (int idx = delegate.size() - 1; idx >= 0; idx--) {
      if (!c.contains(delegate.get(idx))) {
        delegate.remove(idx);
        fireIntervalRemoved(this, idx, idx);
        modified = true;
      }
    }
    return modified;
  }

  /** Removes all of the elements from this list. */
  public void clear()
  {
    int index1 = delegate.size();
    delegate.clear();
    if (index1 > 0) {
      fireIntervalRemoved(this, 0, index1);
    }
  }

  /** Deletes the elements at the specified range of indexes. The removal is inclusive. */
  public void removeRange(int fromIndex, int toIndex)
  {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex must be <= toIndex");
    }
    for(int i = toIndex; i >= fromIndex; i--) {
      delegate.removeElementAt(i);
    }
    fireIntervalRemoved(this, fromIndex, toIndex);
  }
}
