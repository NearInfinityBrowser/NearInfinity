// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterator;

public final class ResourceTreeFolder implements Comparable<ResourceTreeFolder>
{
  private final SortedListSet<ResourceEntry> resourceEntries = new SortedListSet<>();
  private final List<ResourceTreeFolder> folders = new ArrayList<>();
  private final ResourceTreeFolder parentFolder;
  private final String folderName;

  public ResourceTreeFolder(ResourceTreeFolder parentFolder, String folderName)
  {
    this.parentFolder = parentFolder;
    this.folderName = folderName;
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ResourceTreeFolder o)
  {
    return folderName.compareToIgnoreCase(o.folderName);
  }

// --------------------- End Interface Comparable ---------------------

  @Override
  public String toString()
  {
    return folderName + " - " + getChildCount();
  }

  public String folderName()
  {
    return folderName;
  }

  public List<ResourceEntry> getResourceEntries()
  {
    return Collections.unmodifiableList(new ArrayList<>(resourceEntries));
  }

  public List<ResourceEntry> getResourceEntries(String type)
  {
    List<ResourceEntry> list = new ArrayList<>();
    resourceEntries.forEach((entry) -> {
      if (entry.getExtension().equalsIgnoreCase(type)) {
        list.add(entry);
      }
    });
    folders.forEach((folder) -> list.addAll(folder.getResourceEntries(type)));
    return list;
  }

  public void addFolder(ResourceTreeFolder folder)
  {
    folders.add(folder);
  }

  public void addResourceEntry(ResourceEntry entry, boolean overwrite)
  {
    if (entry.isVisible()) {
      if (overwrite) {
        resourceEntries.remove(entry);
      }
      resourceEntries.add(entry);
    }
  }

  public Object getChild(int index)
  {
    if (index >= 0) {
      if (index < folders.size()) {
        return folders.get(index);
      }

      index -= folders.size();
      if (index < resourceEntries.size()) {
        return resourceEntries.get(index);
      }
    }
    return null;
  }

  public int getChildCount()
  {
    return folders.size() + resourceEntries.size();
  }

  public List<ResourceTreeFolder> getFolders()
  {
    return Collections.unmodifiableList(folders);
  }

  public int getIndexOfChild(Object node)
  {
    if (node instanceof ResourceTreeFolder) {
      return folders.indexOf(node);
    }
    int index = resourceEntries.indexOf(node);
    if (index >= 0) {
      return folders.size() + index;
    }
    return -1;
  }

  public ResourceTreeFolder getParentFolder()
  {
    return parentFolder;
  }

  public void removeFolder(ResourceTreeFolder folder)
  {
    folders.remove(folder);
  }

  public void removeResourceEntry(ResourceEntry entry)
  {
    resourceEntries.remove(entry);
  }

  public void sortChildren(boolean recursive)
  {
    Collections.sort(folders);
    if (recursive) {
      folders.forEach((folder) -> folder.sortChildren(recursive));
    }
  }


//-------------------------- INNER CLASSES --------------------------

  // A thread-safe sorted set using an ArrayList as backend for indexed element access
  private static class SortedListSet<T extends Comparable<? super T>> extends ArrayList<T> implements SortedSet<T>
  {
    public SortedListSet()
    {
      super();
    }

    @Override
    public synchronized boolean add(T item)
    {
      int index = Collections.binarySearch(this, item);
      if (index >= 0) {
        return false;
      }
      super.add(~index, item);
      return true;
    }

    @Override
    public void add(int index, T element)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c)
    {
      boolean bRet = false;
      for (final T o: c) {
        bRet |= add(o);
      }
      return bRet;
    }

    @Override
    public boolean contains(Object o)
    {
      return (indexOf(o) != -1);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
      boolean bRet = true;
      for (final Object o: c) {
        bRet &= contains(o);
        if (!bRet) {
          break;
        }
      }
      return bRet;
    }

    @Override
    public int indexOf(Object o)
    {
      @SuppressWarnings("unchecked")
      int index = Collections.binarySearch(this, (T)o);
      return (index >= 0) ? index : -1;
    }

    @Override
    public synchronized boolean remove(Object o)
    {
      int idx = indexOf(o);
      if (idx >= 0) {
        remove(idx);
        return true;
      }
      return false;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c)
    {
      boolean bRet = false;
      for (final Object o: c) {
        bRet |= remove(o);
      }
      return bRet;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c)
    {
      boolean bRet = false;
      int idx = size() - 1;
      while (idx >= 0) {
        Object o = get(idx);
        if (!c.contains(o)) {
          remove(o);
          bRet = true;
        }
        idx--;
      }
      return bRet;
    }

    /**
     * Replaces the element at the specified position in this set with the specified one.
     * The new element will be moved to the correct location to preserve the sorted state
     * of the list.
     * @param index index of the element to replace.
     * @param element element to be stored in the list.
     * @return element previously at the specified position.
     */
    @Override
    public T set(int index, T element)
    {
      T o = remove(index);
      add(element);
      return o;
    }

    @Override
    public Spliterator<T> spliterator()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public Comparator<? super T> comparator()
    {
      return new Comparator<T>() {
        @Override
        public int compare(T o1, T o2)
        {
          return o1.compareTo(o2);
        }
      };
    }

    @Override
    public T first()
    {
      if (size() == 0) {
        throw new NoSuchElementException();
      }
      return get(0);
    }

    @Override
    public synchronized SortedSet<T> headSet(T toElement)
    {
      int toIdx = Collections.binarySearch(this, toElement);
      if (toIdx < 0) {
        toIdx = ~toIdx;
      } else {
        toIdx--;
      }
      return getSortedSet(0, toIdx);
    }

    @Override
    public T last()
    {
      if (size() == 0) {
        throw new NoSuchElementException();
      }
      return get(size() - 1);
    }

    @Override
    public synchronized SortedSet<T> subSet(T fromElement, T toElement)
    {
      int fromIdx = Collections.binarySearch(this, fromElement);
      if (fromIdx < 0) {
        fromIdx = ~fromIdx;
      }
      int toIdx = Collections.binarySearch(this, toElement);
      if (toIdx < 0) {
        toIdx = ~toIdx;
      } else {
        toIdx--;
      }
      return getSortedSet(fromIdx, toIdx);
    }

    @Override
    public synchronized SortedSet<T> tailSet(T fromElement)
    {
      int fromIdx = Collections.binarySearch(this, fromElement);
      if (fromIdx < 0) {
        fromIdx = ~fromIdx;
      }
      return getSortedSet(fromIdx, size() - 1);
    }

    private SortedSet<T> getSortedSet(int fromIdx, int toIdx)
    {
      SortedListSet<T> retVal = new SortedListSet<>();
      for (int idx = fromIdx; idx <= toIdx; idx++) {
        retVal.add(get(idx));
      }
      return retVal;
    }
  }

}

