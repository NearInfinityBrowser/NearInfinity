package org.infinity.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.swing.AbstractListModel;

import org.infinity.util.tuples.Couple;

/**
 * Specialized class for the {@link CheckBoxList} control that stores list items and their associated selection state.
 */
public class CheckBoxListModel<E> extends AbstractListModel<E> {
  private final ArrayList<Boolean> checks = new ArrayList<>();
  private final ArrayList<E> items = new ArrayList<>();

  private boolean defSelection;

  /** Creates an empty CheckBoxListModel instance. */
  public CheckBoxListModel() {
    this(false);
  }

  /**
   * Creates an empty CheckBoxListModel instance and sets the default selection state for new elements to the
   * specified value.
   */
  public CheckBoxListModel(boolean defSelection) {
    this.defSelection = defSelection;
  }

  /**
   * Creates a new CheckBoxListModel instance and initializes it with the items from the specified collection.
   * Their selection state is set to {@code defSelection}.
   */
  public CheckBoxListModel(Collection<? extends E> items, boolean defSelection) {
    this.items.addAll(Objects.requireNonNull(items));
    this.defSelection = defSelection;
    validate();
  }

  public CheckBoxListModel(Collection<? extends E> items, Collection<Boolean> selections, boolean defSelection) {
    this.items.addAll(Objects.requireNonNull(items));
    this.checks.addAll(Objects.requireNonNull(selections));
    this.defSelection = defSelection;
    validate();
  }

  @Override
  public int getSize() {
    return items.size();
  }

  @Override
  public E getElementAt(int index) {
    return items.get(index);
  }

  /** Trims the capacity of this list to be the list's current size. */
  public void trimToSize() {
    items.trimToSize();
    checks.trimToSize();
  }

  /**
   * Increases the capacity of this list, if necessary, to ensure that it can hold at least the number of components
   * specified by the minimum capacity argument.
   */
  public void ensureCapacity(int minCapacity) {
    items.ensureCapacity(minCapacity);
    checks.ensureCapacity(minCapacity);
  }

  /** Returns the default selection state that is set for new items. */
  public boolean getDefaultSelection() {
    return defSelection;
  }

  /** Specifies the default selection state for new items. */
  public void setDefaultSelection(boolean selected) {
    defSelection = selected;
  }

  /**
   * Returns the selection state of the list item at the specified index. Throws an {@link IndexOutOfBoundsException}
   * exception if index invalid.
   */
  public boolean isSelected(int index) {
    return checks.get(index);
  }

  /**
   * Assigns the selection state of the list item at the specified index. Throws an {@link IndexOutOfBoundsException}
   * exception if index invalid.
   */
  public void setSelected(int index, boolean selected) {
    final boolean oldSelection = checks.get(index);
    if (oldSelection != selected) {
      checks.set(index, selected);
      fireContentsChanged(this, index, index);
    }
  }

  /** Sets the selected state of all list items to {@code true}. */
  public void selectAll() {
    for (int i = 0, size = checks.size(); i < size; i++) {
      checks.set(i, Boolean.TRUE);
    }
    if (!checks.isEmpty()) {
      fireContentsChanged(this, 0, checks.size() - 1);
    }
  }

  /** Sets the selected state of all list items to {@code false}. */
  public void unselectAll() {
    for (int i = 0, size = checks.size(); i < size; i++) {
      checks.set(i, Boolean.FALSE);
    }
    if (!checks.isEmpty()) {
      fireContentsChanged(this, 0, checks.size() - 1);
    }
  }

  /** Inverts the selection states of all list items. */
  public void invertSelection() {
    for (int i = 0, size = checks.size(); i < size; i++) {
      checks.set(i, !checks.get(i));
    }
    if (!checks.isEmpty()) {
      fireContentsChanged(this, 0, checks.size() - 1);
    }
  }

  /**
   * Inverts the selection state of the list item at the specified position. Throws an
   * {@link IndexOutOfBoundsException} exception if index is invalid.
   */
  public void invertSelection(int index) {
    checks.set(index, !checks.get(index));
    fireContentsChanged(this, index, index);
  }

  /** Returns the number of components in this list. */
  public int size() {
    return items.size();
  }

  /** Returns {@code true} if the list is empty. Returns {@code false} otherwise. */
  public boolean isEmpty() {
    return items.isEmpty();
  }

  /** Tests whether the specified object is a component in this list. */
  public boolean contains(Object elem) {
    return items.contains(elem);
  }

  /** Searches for the first occurrence of <code>elem</code>. */
  public int indexOf(Object elem) {
    return items.indexOf(elem);
  }

  /** Searches for the last occurrence of <code>elem</code>. */
  public int lastIndexOf(Object elem) {
    return items.lastIndexOf(elem);
  }

  /** Removes the first (lowest-indexed) occurrence of the argument from this list. */
  public boolean removeElement(Object obj) {
    final int index = items.indexOf(obj);
    if (index != -1) {
      remove(index);
      return true;
    }
    return false;
  }

  /**
   * Returns the element at the specified position in this list. Throws an {@link IndexOutOfBoundsException} exception
   * if index is invalid.
   */
  public E get(int index) {
    return items.get(index);
  }

  /**
   * Replaces the element at the specified position in this list with the specified element without modifying the
   * associated selection state. Throws an {@link IndexOutOfBoundsException} exception if index is invalid.
   */
  public E set(int index, E element) {
    final E rv = items.get(index);
    items.set(index, element);
    fireContentsChanged(this, index, index);
    return rv;
  }

  /**
   * Replaces the element their selection state at the specified position in this list with the specified element.
   * Throws an {@link IndexOutOfBoundsException} exception if index is invalid.
   */
  public E set(int index, E element, boolean selected) {
    final E rv = items.get(index);
    items.set(index, element);
    checks.set(index, selected);
    fireContentsChanged(this, index, index);
    return rv;
  }

  /**
   * Inserts the specified element at the specified position in this list and sets the default selection state. Throws
   * an {@link IndexOutOfBoundsException} exception if index is invalid.
   */
  public void add(int index, E element) {
    add(index, element, defSelection);
  }

  /**
   * Inserts the specified element and their selection state at the specified position in this list. Throws an
   * {@link IndexOutOfBoundsException} exception if index is invalid.
   */
  public void add(int index, E element, boolean selected) {
    validate();
    items.add(index, element);
    checks.add(index, selected);
    fireIntervalAdded(this, index, index);
  }

  /** Adds the specified component to the end of this list and sets the default selection state. */
  public void add(E element) {
    add(element, defSelection);
  }

  /** Adds the specified component and their selection state to the end of this list. */
  public void add(E element, boolean selected) {
    validate();
    final int index = items.size();
    items.add(element);
    checks.add(selected);
    fireIntervalAdded(this, index, index);
  }

  /**
   * Removes the element at the specified position in this list. Returns the element that was removed from the list.
   * Throws an {@link IndexOutOfBoundsException} exception if index is invalid.
   */
  public E remove(int index) {
    final E rv = items.get(index);
    validate();
    items.remove(index);
    checks.remove(index);
    fireIntervalRemoved(this, index, index);
    return rv;
  }

  /** Removes all of the elements from this list. */
  public void clear() {
    int index1 = items.size() - 1;
    items.clear();
    checks.clear();
    if (index1 >= 0) {
      fireIntervalRemoved(this, 0, index1);
    }
  }

  /**
   * Deletes the components at the specified range of indexes. The removal is inclusive, so specifying a range of
   * (1,5) removes the component at index 1 and the component at index 5, as well as all components in between.
   *
   * @param fromIndex the index of the lower end of the range
   * @param toIndex   the index of the upper end of the range
   * @throws IllegalArgumentException if {@code fromIndex} is greater than {@code toIndex}.
   * @throws IndexOutOfBoundsException if either of the indices is invalid.
   */
  public void removeRange(int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex must be <= toIndex");
    }
    validate();
    for (int i = toIndex; i >= fromIndex; i--) {
      items.remove(i);
      checks.remove(i);
    }
    fireIntervalRemoved(this, fromIndex, toIndex);
  }

  /**
   * Returns an iterator over the elements in this list in proper sequence.
   * <p>
   * Elements are returned as {@link Couple} instances containing the element data and associated selection state.
   * </p>
   *
   * @return An iterator over the elements in this list in proper sequence.
   */
  public Iterator<Couple<E, Boolean>> iterator() {
    return new Itr();
  }

  @Override
  public int hashCode() {
    return Objects.hash(checks, items);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CheckBoxListModel<?> other = (CheckBoxListModel<?>)obj;
    return Objects.equals(checks, other.checks) && Objects.equals(items, other.items);
  }

  @Override
  public String toString() {
    if (items.isEmpty()) {
      return "[]";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0, size = items.size(); i < size; i++) {
      if (i > 0) {
        sb.append(',').append(' ');
      }
      sb.append('[');
      sb.append(items.get(i));
      sb.append(',').append(' ');
      sb.append(checks.get(i).booleanValue());
      sb.append(']');
    }
    sb.append(']');

    return sb.toString();
  }

  /**
   * Returns an array containing all of the elements in this list in the correct order. Selection states are not
   * included.
   */
  public Object[] toArray() {
    return items.toArray();
  }

  /** Used internally to ensure that items and selection states are synchronized. */
  private void validate() {
    while (checks.size() > items.size()) {
      checks.remove(checks.size() - 1);
    }
    while (checks.size() < items.size()) {
      checks.add(defSelection);
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  private class Itr implements Iterator<Couple<E, Boolean>> {
    private int cursor = 0;
    private int lastRet = -1;
    private int expectedModCount = items.size();

    @Override
    public boolean hasNext() {
      return cursor != items.size();
    }

    @Override
    public Couple<E, Boolean> next() {
      checkForComodification();
      try {
        int i = cursor;
        final E e = items.get(i);
        final Boolean b = checks.get(i);
        lastRet = i;
        cursor = i + 1;
        return new Couple<E, Boolean>(e, b);
      } catch (IndexOutOfBoundsException e) {
        checkForComodification();
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove() {
      if (lastRet < 0) {
        throw new IllegalStateException();
      }
      checkForComodification();

      try {
        CheckBoxListModel.this.remove(lastRet);
        if (lastRet < cursor) {
          cursor--;
        }
        lastRet = -1;
        expectedModCount--;
      } catch (IndexOutOfBoundsException e) {
        throw new ConcurrentModificationException();
      }
    }

    private void checkForComodification() {
      if (expectedModCount != items.size()) {
        throw new ConcurrentModificationException();
      }
    }
  }
}
