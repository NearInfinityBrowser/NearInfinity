// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public final class ResourceTreeModel implements TreeModel
{
  private final List<TreeModelListener> treeModelListeners = new ArrayList<TreeModelListener>();
  private final Map<String, ResourceEntry> entries = new HashMap<String, ResourceEntry>(25000);
  private final Map<String, ResourceTreeFolder> folders = new HashMap<String, ResourceTreeFolder>();
  private final ResourceTreeFolder root = new ResourceTreeFolder(null, "");

  public ResourceTreeModel()
  {
  }

// --------------------- Begin Interface TreeModel ---------------------

  @Override
  public Object getRoot()
  {
    return root;
  }

  @Override
  public Object getChild(Object parent, int index)
  {
    if (parent instanceof ResourceTreeFolder)
      return ((ResourceTreeFolder)parent).getChild(index);
    return null;
  }

  @Override
  public int getChildCount(Object parent)
  {
    if (parent instanceof ResourceTreeFolder)
      return ((ResourceTreeFolder)parent).getChildCount();
    return 0;
  }

  @Override
  public boolean isLeaf(Object node)
  {
    return !(node instanceof ResourceTreeFolder);
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newvalue)
  {
    throw new IllegalArgumentException(); // Not allowed
  }

  @Override
  public int getIndexOfChild(Object parent, Object child)
  {
    if (parent instanceof ResourceTreeFolder)
      return ((ResourceTreeFolder)parent).getIndexOfChild(child);
    return -1;
  }

  @Override
  public void addTreeModelListener(TreeModelListener l)
  {
    treeModelListeners.add(l);
  }

  @Override
  public void removeTreeModelListener(TreeModelListener l)
  {
    treeModelListeners.remove(l);
  }

// --------------------- End Interface TreeModel ---------------------

  public void addDirectory(ResourceTreeFolder parentFolder, Path directory)
  {
    try (DirectoryStream<Path> dstream = Files.newDirectoryStream(directory)) {
      Iterator<Path> iter = dstream.iterator();
      if (iter.hasNext()) {
        ResourceTreeFolder folder = getFolder(parentFolder, directory.getFileName().toString());
        if (folder == null) {
          folder = new ResourceTreeFolder(parentFolder, directory.getFileName().toString());
          folders.put(directory.getFileName().toString(), folder);
          parentFolder.addFolder(folder);
        }

        while (iter.hasNext()) {
          final Path path = iter.next();
          if (Files.isDirectory(path)) {
            addDirectory(folder, path);
          } else {
            folder.addResourceEntry(new FileResourceEntry(path), true);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  public void addResourceEntry(ResourceEntry entry, String folderName)
  {
    ResourceTreeFolder folder = folders.get(folderName);
    if (folder == null) {
      folder = new ResourceTreeFolder(root, folderName);
      folders.put(folderName, folder);
      root.addFolder(folder);
    }
    folder.addResourceEntry(entry, true);
    if (entry.isVisible()) {
      entries.put(entry.getResourceName().toUpperCase(Locale.ENGLISH), entry);
    }
  }

  public List<BIFFResourceEntry> getBIFFResourceEntries()
  {
    List<BIFFResourceEntry> list = new ArrayList<BIFFResourceEntry>();
    for (int i = 0; i < root.getFolders().size(); i++) {
      List<ResourceEntry> entries = root.getFolders().get(i).getResourceEntries();
      for (int j = 0; j < entries.size(); j++) {
        ResourceEntry o = entries.get(j);
        if (o instanceof BIFFResourceEntry)
          list.add((BIFFResourceEntry)o);
      }
    }
    return list;
  }

  public ResourceTreeFolder getFolder(ResourceTreeFolder parentFolder, String name)
  {
    ResourceTreeFolder folder = null;
    if (parentFolder != null) {
      for (final ResourceTreeFolder rtf: parentFolder.getFolders()) {
        if (rtf.folderName().equalsIgnoreCase(name)) {
          folder = rtf;
          break;
        }
      }
    }
    return folder;
  }

  public ResourceTreeFolder getFolder(String text)
  {
    return folders.get(text);
  }

  public TreePath getPathToNode(ResourceEntry entry)
  {
    List<Object> path = new ArrayList<Object>(4);
    path.add(entry);
    ResourceTreeFolder parent = folders.get(entry.getTreeFolder());
    while (parent != null) {
      path.add(parent);
      parent = parent.getParentFolder();
    }
    Collections.reverse(path);
    return new TreePath(path.toArray());
  }

  public Collection<ResourceEntry> getResourceEntries()
  {
    return entries.values();
  }

  public ResourceEntry getResourceEntry(String entryname)
  {
    if (entryname != null) {
      return entries.get(entryname.toUpperCase(Locale.ENGLISH));
    } else {
      return null;
    }
  }

  public void removeResourceEntry(ResourceEntry entry)
  {
    removeResourceEntry(entry, entry.getTreeFolder());
  }

  public void removeResourceEntry(ResourceEntry entry, String folder)
  {
    ResourceTreeFolder parent = folders.get(folder);
    if (parent == null)
      return;
    TreePath path = getPathToNode(entry).getParentPath();
    TreeModelEvent event = new TreeModelEvent(this, path, new int[]{getIndexOfChild(parent, entry)},
                                              new Object[]{entry});
    parent.removeResourceEntry(entry);
    entries.remove(entry.toString().toUpperCase(Locale.ENGLISH));
    if (parent.getChildCount() == 0) {
      root.removeFolder(parent);
      folders.remove(parent.folderName());
    }
    for (int i = 0; i < treeModelListeners.size(); i++)
      treeModelListeners.get(i).treeNodesRemoved(event);
  }

  public void resourceEntryChanged(FileResourceEntry entry)
  {
    TreePath parentPath = getPathToNode(entry).getParentPath();
    ResourceTreeFolder parentFolder = (ResourceTreeFolder)parentPath.getLastPathComponent();
    TreeModelEvent event = new TreeModelEvent(this, parentPath, new int[]{
      getIndexOfChild(parentFolder, entry)},
                                              new Object[]{entry});
    for (int i = 0; i < treeModelListeners.size(); i++)
      treeModelListeners.get(i).treeNodesChanged(event);
  }

  public int size()
  {
    int size = 0;
    for (int i = 0; i < root.getChildCount(); i++)
      size += ((ResourceTreeFolder)root.getChild(i)).getChildCount();
    return size;
  }

  public void sort()
  {
    root.sortChildren();
    fireTreeStructureChanged(new TreePath(new Object[]{root}));
  }

  private void fireTreeStructureChanged(TreePath changed)
  {
    TreeModelEvent event = new TreeModelEvent(this, changed);
    for (int i = 0; i < treeModelListeners.size(); i++)
      treeModelListeners.get(i).treeStructureChanged(event);
  }
}

