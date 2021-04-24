// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
   * Definition of a list model that filters a collection of items based on a search pattern.
   * Filtering can be enabled and disabled. The model behaves like the {@link DefaultListModel}
   * if filtering is disabled.
 */
public class FilteredListModel<E> extends AbstractListModel<E>
{
  private final Vector<E> list = new Vector<>();
  private final Vector<E> filteredList = new Vector<>();
  private final ArrayList<ChangeListener> listeners = new ArrayList<>();

  private boolean filtered;
  private String pattern;

  /** Creates a new FilteredListModel instance with filtering initially disabled. */
  public FilteredListModel()
  {
    this(false);
  }

  /** Creates a new FilteredListModel instance with filtering set to the specified parameter. */
  public FilteredListModel(boolean filtered)
  {
    super();
    this.filtered = filtered;
    this.pattern = "";
  }

  /** Creates a new FilteredListModel instance with the specified reference items and filtering initially disabled. */
  public FilteredListModel(E[] items)
  {
    this(items, false);
  }

  /** Creates a new FilteredListModel instance with the specified reference items and filtering set to the specified parameter. */
  public FilteredListModel(E[] items, boolean filtered)
  {
    super();
    this.filtered = filtered;
    this.pattern = "";
    this.baseAddAll(items);
  }

  /** Creates a new FilteredListModel instance with the specified reference items and filtering initially disabled. */
  public FilteredListModel(Collection<E> items)
  {
    this(items, false);
  }

  /** Creates a new FilteredListModel instance with the specified reference items and filtering set to the specified parameter. */
  public FilteredListModel(Collection<E> items, boolean filtered)
  {
    super();
    this.filtered = filtered;
    this.pattern = "";
    this.baseAddAll(items);
  }

  /**
   * Adds the specified ChangeListener to the listener list.
   * A change listener is notified whenever the content of the filter list is changed.
   */
  public void addFilterChangeListener(ChangeListener listener)
  {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /** Returns an array of all the change listeners registered to this instance. */
  public ChangeListener[] getFilterChangeListeners()
  {
    return listeners.toArray(new ChangeListener[listeners.size()]);
  }

  /** Removes all instances of the specified ChangeListener from the listener list. */
  public void removeFilterChangeListener(ChangeListener listener)
  {
    if (listener != null) {
      while (listeners.remove(listener)) {}
    }
  }

  /** Called one per successful filter update. */
  protected void fireFilterChange()
  {
    ChangeEvent e = null;
    if (!listeners.isEmpty()) {
      e = new ChangeEvent(this);
    }

    for (int i = 0, cnt = listeners.size(); i < cnt; i++) {
      ChangeListener l = listeners.get(i);
      l.stateChanged(e);
    }
  }

  /**
   * Returns the current search pattern.
   * @return search pattern as string.
   */
  public String getPattern()
  {
    return pattern;
  }

  /**
   * Specifies the current search pattern. Empty string indicates "match all".
   * The pattern is only regarded when filter is active.
   * @param pattern The search pattern as string.
   */
  public void setPattern(String pattern)
  {
    updateFilter(this.filtered, pattern, false);
  }

  /**
   * Returns whether the item list is filtered by search pattern.
   * @return whether filtering is enabled.
   */
  public boolean isFiltered()
  {
    return filtered;
  }

  /**
   * Specifies whether to filter the item list.
   * @param filtered Set to {@code true} to filter list items by the current search pattern.
   *                 Set to {@code false} to output unfiltered list.
   */
  public void setFiltered(boolean filtered)
  {
    updateFilter(filtered, this.pattern, false);
  }

  /** Returns an unmodifiable collection of the components of the reference list. */
  public Collection<E> baseElements()
  {
    return Collections.unmodifiableCollection(list);
  }

  /** Returns the element at the specified position in the reference list. */
  public E baseGet(int index)
  {
    return list.get(index);
  }

  /** Replaces the element at the specified position in the reference list with the specified element. */
  public E baseSet(int index, E newItem)
  {
    E retVal = list.set(index, newItem);
    if (retVal != newItem)
      updateFilter();
    return retVal;
  }

  /** Appends the specified element to the end of the reference list. */
  public boolean baseAdd(E item)
  {
    boolean retVal = list.add(item);
    if (retVal)
      updateFilter();
    return retVal;
  }

  /** Inserts the specified element at the specified position in the reference list. */
  public void baseAdd(int index, E item)
  {
    list.add(index, item);
    updateFilter();
  }

  /** Appends all of the elements in the specified Array to the end of this list. */
  public boolean baseAddAll(E[] items)
  {
    return baseAddAll(list.size(), Arrays.asList(items));
  }

  /** Inserts all of the elements in the specified Array into the reference list at the specified position. */
  public boolean baseAddAll(int index, E[] items)
  {
    return baseAddAll(index, Arrays.asList(items));
  }

  /**
   * Appends all of the elements in the specified Collection to the end of the reference list,
   * in the order that they are returned by the specified Collection's Iterator.
   */
  public boolean baseAddAll(Collection<? extends E> coll)
  {
    return baseAddAll(list.size(), coll);
  }

  /** Inserts all of the elements in the specified Collection into the reference list at the specified position. */
  public boolean baseAddAll(int index, Collection<? extends E> coll)
  {
    boolean retVal = list.addAll(index, coll);
    if (retVal)
      updateFilter();
    return retVal;
  }

  /** Removes the element at the specified position in the reference list. */
  public E baseRemove(int index)
  {
    E retVal = list.remove(index);
    updateFilter();
    return retVal;
  }

  /** Removes the first occurrence of the specified element in the reference list. */
  public boolean baseRemove(E item)
  {
    boolean retVal = list.remove(item);
    if (retVal)
      updateFilter();
    return retVal;
  }

  /** Removes from the reference list all of its elements that are contained in the specified Collection. */
  public boolean baseRemoveAll(Collection<? extends E> c)
  {
    boolean retVal = list.removeAll(c);
    if (retVal)
      updateFilter();
    return retVal;
  }

  /** Retains only the elements in the reference list that are contained in the specified Collection. */
  public boolean baseRetainAll(Collection<? extends E> c)
  {
    boolean retVal = list.retainAll(c);
    if (retVal)
      updateFilter();
    return retVal;
  }

  /** Removes all of the elements from the reference list. */
  public void baseClear()
  {
    if (!list.isEmpty()) {
      list.clear();
      updateFilter();
    }
  }

  /** Deletes the elements at the specified range of indexes from the reference llist. The removal is inclusive. */
  public void baseRemoveRange(int fromIndex, int toIndex)
  {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex must be <= toIndex");
    }
    boolean modified = false;
    for(int i = toIndex; i >= fromIndex; i--) {
      list.removeElementAt(i);
      modified = true;
    }
    if (modified)
      updateFilter();
  }

  @Override
  public int getSize()
  {
    return filteredList.size();
  }

  @Override
  public E getElementAt(int index)
  {
    return filteredList.get(index);
  }

  @Override
  public String toString()
  {
    return filteredList.toString();
  }

  @Override
  public boolean equals(Object o)
  {
    return filteredList.equals(o);
  }

  @Override
  public int hashCode()
  {
    return filteredList.hashCode();
  }

  /** Returns an unmodifiable collection of the components of this list. */
  public Collection<E> elements()
  {
    return Collections.unmodifiableCollection(filteredList);
  }

  /** Returns {@code true} if this list contains the specified element. */
  public boolean contains(E item)
  {
    return filteredList.contains(item);
  }

  /** Returns true if this list contains all of the elements in the specified Collection. */
  public boolean containsAll(Collection<? extends E> c)
  {
    return filteredList.containsAll(c);
  }

  /**
   * Returns the index of the first occurrence of the specified element in this list,
   * or -1 if this list does not contain the element.
   */
  public int indexOf(E item)
  {
    return filteredList.indexOf(item);
  }

  /**
   * Returns the index of the first occurrence of the specified element in this list,
   * searching forwards from {@code index}, or returns -1 if the element is not found.
   */
  public int indexOf(E item, int index)
  {
    return filteredList.indexOf(item, index);
  }

  /**
   * Returns the index of the last occurrence of the specified element in this list,
   * or -1 if this list does not contain the element.
   */
  public int lastIndexOf(E item)
  {
    return filteredList.lastIndexOf(item);
  }

  /**
   * Returns the index of the last occurrence of the specified element in this list,
   * searching backwards from {@code index}, or returns -1 if the element is not found.
   */
  public int lastIndexOf(E item, int index)
  {
    return filteredList.lastIndexOf(item, index);
  }

  /** Returns the component at the specified index. */
  public E elementAt(int index)
  {
    return filteredList.elementAt(index);
  }

  /** Returns the first component (the item at index {@code 0}) of this list. */
  public E firstElement()
  {
    return filteredList.firstElement();
  }

  /** Returns the last component of the list. */
  public E lastElement()
  {
    return filteredList.lastElement();
  }

  /**
   * Sets the component at the specified {@code index} of this list to be the specified object.
   * The previous component at that position is discarded.
   */
  public void setElementAt(E item, int index)
  {
    filteredList.setElementAt(item, index);
    fireContentsChanged(this, index, index);
  }

  /** Deletes the component at the specified index. */
  public void removeElementAt(int index)
  {
    filteredList.removeElementAt(index);
    fireIntervalRemoved(this, index, index);
  }

  /** Inserts the specified object as a component in this list at the specified {@code index}. */
  public void insertElementAt(E item, int index)
  {
    filteredList.insertElementAt(item, index);
    fireIntervalAdded(this, index, index);
  }

  /** Adds the specified component to the end of this list, increasing its size by one. */
  public void addElement(E item)
  {
    int index = filteredList.size();
    filteredList.addElement(item);
    fireIntervalAdded(this, index, index);
  }

  /** Removes the first (lowest-indexed) occurrence of the argument from this list. */
  public boolean removeElement(E item)
  {
    int index = filteredList.indexOf(item);
    boolean ret = (index >= 0);
    if (index >= 0) {
      filteredList.removeElementAt(index);
      fireIntervalRemoved(this, index, index);
    }
    return ret;
  }

  /** Removes all components from this list and sets its size to zero. */
  public void removeAllElements()
  {
    int index1 = filteredList.size() - 1;
    filteredList.removeAllElements();
    if (index1 > 0) {
      fireIntervalRemoved(this, 0, index1);
    }
  }

  /** Returns an array containing all of the elements in this list in the correct order. */
  public Object[] toArray()
  {
    return filteredList.toArray();
  }

  /** Returns the element at the specified position in this list. */
  public E get(int index)
  {
    return filteredList.get(index);
  }

  /** Replaces the element at the specified position in this list with the specified element. */
  public E set(int index, E newItem)
  {
    E retVal = filteredList.set(index, newItem);
    if (retVal != newItem)
      fireContentsChanged(this, index, index);
    return retVal;
  }

  /** Appends the specified element to the end of this list. */
  public boolean add(E item)
  {
    int index = filteredList.size();
    boolean retVal = filteredList.add(item);
    fireIntervalAdded(this, index, index);
    return retVal;
  }

  /** Inserts the specified element at the specified position in this list. */
  public void add(int index, E item)
  {
    filteredList.add(index, item);
    fireIntervalAdded(this, index, index);
  }

  /**
   * Appends all of the elements in the specified Array to the end of this list.
   */
  public boolean addAll(E[] items)
  {
    return addAll(filteredList.size(), Arrays.asList(items));
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
  public boolean addAll(Collection<? extends E> coll)
  {
    return addAll(filteredList.size(), coll);
  }

  /**
   * Inserts all of the elements in the specified Collection into this list
   * at the specified position.
   */
  public boolean addAll(int index, Collection<? extends E> coll)
  {
    int index0 = index;
    int index1 = index0 + coll.size() - 1;
    filteredList.addAll(index, coll);
    if (index1 >= index0)
      fireIntervalAdded(this, index0, index1);
    return (index1 >= index0);
  }

  /** Removes the element at the specified position in this list. */
  public E remove(int index)
  {
    E retVal = filteredList.remove(index);
    fireIntervalRemoved(this, index, index);
    return retVal;
  }

  /** Removes the first occurrence of the specified element in this list. */
  public boolean remove(E item)
  {
    int index = filteredList.indexOf(item);
    if (index >= 0) {
      filteredList.remove(index);
      fireIntervalRemoved(this, index, index);
    }
    return (index >= 0);
  }

  /** Removes from this list all of its elements that are contained in the specified Collection. */
  public boolean removeAll(Collection<? extends E> c)
  {
    boolean modified = false;
    for (int idx = filteredList.size() - 1; idx >= 0; idx--) {
      if (c.contains(filteredList.get(idx))) {
        filteredList.remove(idx);
        fireIntervalRemoved(this, idx, idx);
        modified = true;
      }
    }
    return modified;
  }

  /** Retains only the elements in this list that are contained in the specified Collection. */
  public boolean retainAll(Collection<? extends E> c)
  {
    boolean modified = false;
    for (int idx = filteredList.size() - 1; idx >= 0; idx--) {
      if (!c.contains(filteredList.get(idx))) {
        filteredList.remove(idx);
        fireIntervalRemoved(this, idx, idx);
        modified = true;
      }
    }
    return modified;
  }

  /** Removes all of the elements from this list. */
  public void clear()
  {
    int index1 = filteredList.size() - 1;
    filteredList.clear();
    if (index1 >= 0)
      fireIntervalRemoved(this, 0, index1);
  }

  /** Deletes the elements at the specified range of indexes. The removal is inclusive. */
  public void removeRange(int fromIndex, int toIndex)
  {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex must be <= toIndex");
    }
    for(int i = toIndex; i >= fromIndex; i--) {
      filteredList.removeElementAt(i);
    }
    fireIntervalRemoved(this, fromIndex, toIndex);
  }

  /** Convenience method: Enforces a filter update with the global flags. */
  private void updateFilter()
  {
    updateFilter(filtered, pattern, true);
  }

  /** Performs an update on the filter list based on specified parameters. */
  private void updateFilter(boolean filtered, String pattern, boolean forced)
  {
    boolean filterModified = (this.filtered != filtered);
    if (filterModified)
      this.filtered = filtered;

    pattern = normalize(pattern);
    boolean patternModified = !this.pattern.equals(pattern);
    if (patternModified)
      this.pattern = pattern;

    if (!forced && !this.filtered && !filterModified)
      return;
    if (!forced && this.filtered && !filterModified && !patternModified)
      return;

    pattern = this.pattern.toLowerCase();
    boolean filter = this.filtered && !this.pattern.isEmpty();

    // Scanning list from back to front for performance reason.
    // A helper object keeps number of fired data events to a minimum for greatly improved performance.
    ListDataEventHelper helper = new ListDataEventHelper();
    int fidx = filteredList.size() - 1;
    for (int bidx = list.size() - 1; bidx >= 0; bidx--) {
      E item = list.get(bidx);
      boolean match = !filter || (item != null ? item : "").toString().toLowerCase().contains(pattern);
      if (match) {
        if (fidx < 0 || !item.equals(getElementAt(fidx))) {
          int idx = Math.max(-1, fidx) + 1;
          filteredList.add(idx, item);
          helper.updateEvent(ListDataEventHelper.EVENT_ADD, idx);
        } else if (fidx >= 0 && item.equals(getElementAt(fidx))) {
          helper.updateEvent(ListDataEventHelper.EVENT_NONE, -1);
          fidx--;
        }
      } else if (fidx >= 0 && item.equals(getElementAt(fidx))) {
        filteredList.remove(fidx);
        helper.updateEvent(ListDataEventHelper.EVENT_REMOVE, fidx);
        fidx--;
      }
    }
    helper.fireEvent(); // finalizing trailing event

    fireFilterChange();
  }

  /** Ensures that strings are not {@code null} and trimmed. */
  private String normalize(String s)
  {
    return (s != null) ? s.trim() : "";
  }


  /** Used internally to reduce the number of fired list data events when building the filter list. */
  private class ListDataEventHelper
  {
    /** This event type does nothing. */
    public static final int EVENT_NONE    = 0;
    /** Event for adding new list items. */
    public static final int EVENT_ADD     = 1;
    /** Event for removing existing list items. */
    public static final int EVENT_REMOVE  = 2;
    /** Event for updating existing list items. */
    public static final int EVENT_CHANGED = 3;

    private int event;
    private int index0, index1;

    public ListDataEventHelper()
    {
      this.event = EVENT_NONE;
      this.index0 = -1;
      this.index1 = -1;
    }

    /**
     * Registers the specified index for the event type.
     * A change of the event type will automatically fire the previous event.
     * This method should be called AFTER making the change to the list.
     * @param eventType See EVENT_xxx constants.
     * @param index The list item index associated with the event type.
     */
    public void updateEvent(int eventType, int index)
    {
      if (eventType != this.event) {
        fireEvent();
        event = eventType;
        index0 = index;
      }
      if (eventType != EVENT_NONE)
        index1 = index;
    }

    /**
     * Fires the currently active event with all accumulated indices at once.
     * If invoked manually it should be called AFTER making the change to the list.
     */
    public void fireEvent()
    {
      switch (event) {
        case EVENT_ADD:
          FilteredListModel.this.fireIntervalAdded(FilteredListModel.this, Math.min(index0, index1), Math.max(index0, index1));
          break;
        case EVENT_REMOVE:
          FilteredListModel.this.fireIntervalRemoved(FilteredListModel.this, Math.min(index0, index1), Math.max(index0, index1));
          break;
        case EVENT_CHANGED:
          FilteredListModel.this.fireContentsChanged(FilteredListModel.this, Math.min(index0, index1), Math.max(index0, index1));
          break;
      }
      event = EVENT_NONE;
      index0 = index1 = -1;
    }
  }
}
