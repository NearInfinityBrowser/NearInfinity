// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import java.util.*;

public final class ResourceTreeFolder implements Comparable<ResourceTreeFolder>
{
  private final List<ResourceEntry> resourceEntries = new ArrayList<ResourceEntry>();
  private final List<ResourceTreeFolder> folders = new ArrayList<ResourceTreeFolder>();
  private final ResourceTreeFolder parentFolder;
  private final String folderName;

  public ResourceTreeFolder(ResourceTreeFolder parentFolder, String folderName)
  {
    this.parentFolder = parentFolder;
    this.folderName = folderName;
  }

// --------------------- Begin Interface Comparable ---------------------

  public int compareTo(ResourceTreeFolder o)
  {
    return folderName.compareToIgnoreCase(o.folderName);
  }

// --------------------- End Interface Comparable ---------------------

  public String toString()
  {
    return folderName + " - " + getChildCount();
  }

  public List<ResourceEntry> getResourceEntries()
  {
    return Collections.unmodifiableList(resourceEntries);
  }

  public List<ResourceEntry> getResourceEntries(String type)
  {
    List<ResourceEntry> list = new ArrayList<ResourceEntry>();
    for (int i = 0; i < resourceEntries.size(); i++) {
      ResourceEntry entry = resourceEntries.get(i);
      if (entry.getExtension().equalsIgnoreCase(type))
        list.add(entry);
    }
    for (int i = 0; i < folders.size(); i++) {
      ResourceTreeFolder folder = folders.get(i);
      list.addAll(folder.getResourceEntries(type));
    }
    return list;
  }

  void addFolder(ResourceTreeFolder folder)
  {
    folders.add(folder);
  }

  void addResourceEntry(ResourceEntry entry)
  {
    resourceEntries.add(entry);
  }

  Object getChild(int index)
  {
    if (index < folders.size())
      return folders.get(index);
    return resourceEntries.get(index - folders.size());
  }

  int getChildCount()
  {
    return folders.size() + resourceEntries.size();
  }

  List<ResourceTreeFolder> getFolders()
  {
    return Collections.unmodifiableList(folders);
  }

  int getIndexOfChild(Object node)
  {
    if (node instanceof ResourceTreeFolder)
      return folders.indexOf(node);
    return folders.size() + resourceEntries.indexOf(node);
  }

  ResourceTreeFolder getParentFolder()
  {
    return parentFolder;
  }

  void removeFolder(ResourceTreeFolder folder)
  {
    folders.remove(folder);
  }

  void removeResourceEntry(ResourceEntry entry)
  {
    resourceEntries.remove(entry);
  }

  void sortChildren()
  {
    Collections.sort(resourceEntries);
    Collections.sort(folders);
    for (int i = 0; i < folders.size(); i++)
      folders.get(i).sortChildren();
  }
}

