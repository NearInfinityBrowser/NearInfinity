// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class ResourceTreeFolder implements Comparable<ResourceTreeFolder>
{
  private final SortedSet<ResourceEntry> resourceEntries =
      Collections.synchronizedSortedSet(new TreeSet<ResourceEntry>());
  private final List<ResourceTreeFolder> folders = new ArrayList<ResourceTreeFolder>();
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
    List<ResourceEntry> list = new ArrayList<ResourceEntry>();
    for (final ResourceEntry entry: resourceEntries) {
      if (entry.getExtension().equalsIgnoreCase(type)) {
        list.add(entry);
      }
    }
    for (final ResourceTreeFolder folder: folders) {
      list.addAll(folder.getResourceEntries(type));
    }
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
        for (final ResourceEntry entry: resourceEntries) {
          if (index-- == 0) {
            return entry;
          }
        }
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
    int index = folders.size();
    for (final ResourceEntry entry: resourceEntries) {
      if (entry.equals(node)) {
        return index;
      }
      index++;
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

  public void sortChildren()
  {
    Collections.sort(folders);
    for (final ResourceTreeFolder folder: folders) {
      folder.sortChildren();
    }
  }
}

