// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Cache for file paths on case-sensitive filesystems.
 * @author argent77
 */
public final class FileLookup
{
  private static final FileLookup instance = new FileLookup();

  private final boolean isCaseSensitive;

  // Note: The filesystem has to conform to the FHS (Filesystem Hierarchy Standard) to work correctly
  private final PathNode rootNode;
  private boolean toLowered;

  /** Returns the current FileLookup instance. */
  public static FileLookup getInstance()
  {
    return instance;
  }

  /**
   * Convenience method of {@link #queryFilePath(File)}.
   * Encapsulates and returns the resulting path string in a file object.
   */
  public File queryFile(File file)
  {
    if (file instanceof FileNI) {
      return file;
    } else {
      return new File(queryFilePath(file));
    }
  }

  /**
   * Convenience method of {@link #queryFilePath(String)}.
   * Encapsulates and returns the resulting path string in a file object.
   */
  public File queryFile(String fileName)
  {
    return new File(queryFilePath(fileName));
  }

  /**
   * Attempts to find a file matching the path specified in the given File object regardless of
   * case-sensitivity. If no file is found, returns a File object that takes the current to-lowered
   * state into account.
   * @param file A File object.
   * @return A path string pointing to an existing path or a non-existing (to-lowered) path.
   *         Returns the given fileName argument on error.
   */
  public String queryFilePath(File file)
  {
    if (file instanceof FileNI) {
      return file.getPath();
    } else if (file != null) {
      return queryFilePath(file.getPath());
    } else {
      return null;
    }
  }

  /**
   * Attempts to find a file matching the path specified in the given File object regardless of
   * case-sensitivity. If no file is found, returns a File object that takes the current to-lowered
   * state into account.
   * @param fileName A relative or absolute file path.
   * @return A path string pointing to an existing path or a non-existing (to-lowered) path.
   *         Returns the given fileName argument on error.
   */
  public String queryFilePath(String fileName)
  {
    if (fileName != null && !fileName.isEmpty()) {
      try {
        if (isCaseSensitive()) {
          File file = (new File(fileName)).getCanonicalFile();
          List<PathNode> nodes = PathNode.createPath(file);
          if (!nodes.isEmpty()) {
            fileName = lookupPath(nodes, false);
          }
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
    return fileName;
  }

  /**
   * Manually adds the specified file to the file cache.
   * Note: Entry will only be added if the specified file physically exists.
   */
  public void add(File file)
  {
    if (file != null) {
      add(file.getPath());
    }
  }

  /**
   * Manually adds the specified filename to the file cache.
   * Note: Entry will only be added if the specified filename physically exists.
   */
  public void add(String fileName)
  {
    if (isCaseSensitive() && fileName != null && !fileName.isEmpty()) {
      try {
        File file = (new File(fileName)).getCanonicalFile();
        List<PathNode> nodes = PathNode.createPath(file);
        lookupPath(nodes, true);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /** Manually removes the specified file from the file cache. */
  public void remove(File file)
  {
    if (file != null) {
      remove(file.getPath());
    }
  }

  /** Manually removes the filename from the file cache. */
  public void remove(String fileName)
  {
    if (isCaseSensitive() && fileName != null && !fileName.isEmpty()) {
      try {
        File file = (new File(fileName)).getCanonicalFile();
        List<PathNode> nodes = PathNode.createPath(file);
        PathNode foundNode = PathNode.findPathNode(rootNode, nodes);
        if (foundNode != null && foundNode.getParent() != null) {
          foundNode.getParent().remove(foundNode);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /** Convenience method. Removes "from" from and adds "to" to the file cache. */
  public void rename(String from, String to)
  {
    remove(from);
    add(to);
  }

  /** Removes all nodes from the file cache. */
  public void clearCache()
  {
    if (isCaseSensitive()) {
      rootNode.clear();
    }
  }

  /** Returns whether the current system uses case sensitive filesystems. */
  public boolean isCaseSensitive()
  {
    return isCaseSensitive;
  }

  /** Returns whether non-existing filenames are automatically to-lowered when querying a File object. */
  public boolean isToLowered()
  {
    return toLowered;
  }

  /** Set whether new filenames should automatically be to-lowered. */
  public void setToLowered(boolean toLower)
  {
    toLowered = toLower;
  }


  // Internally used constructor
  private FileLookup()
  {
    this.toLowered = true;
    this.isCaseSensitive = checkCaseSensitivity();
    this.rootNode = new PathNode(null);
  }

  // Determines whether the current filesystem is case sensitive.
  private boolean checkCaseSensitivity()
  {
    boolean retVal = true;
    try {
      // checking case-sensitivity
      File tmpFile = File.createTempFile("Ni_AbC", ".tmp");
      try {
        File tmpUpper = new File(tmpFile.getParent(), tmpFile.getName().toUpperCase(Locale.ENGLISH));
        File tmpLower = new File(tmpFile.getParent(), tmpFile.getName().toLowerCase(Locale.ENGLISH));
        retVal = !(tmpUpper.isFile() && tmpLower.isFile());

        // Checking for FHS compatibility
        if (retVal) {
          File[] rootFiles = File.listRoots();
          if (rootFiles == null || rootFiles.length == 0 ||
              rootFiles[0] == null || !rootFiles[0].toString().equals("/")) {
            // Critical error: filesystem is incompatible
            throw new IOError(new IOException("Filesystem does not conform to the FHS"));
          }
        }
      } finally {
        tmpFile.delete();
        tmpFile = null;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return retVal;
  }


  // Method responsible for looking up existing path nodes and possibly extending the file cache.
  // If forceUpdate is true, then nodes will be always update regardless of their cached state.
  private String lookupPath(List<PathNode> srcPath, boolean forceUpdate)
  {
    String retVal = null;
    if (srcPath != null && !srcPath.isEmpty()) {
      Iterator<PathNode> iter = srcPath.iterator();
      if (iter.hasNext()) {
        PathNode srcNode = iter.next();
        if (rootNode.isEqual(srcNode)) {
          retVal = lookupPathInternal(rootNode, iter, forceUpdate);
        }
      }
    }
    return retVal;
  }

  private String lookupPathInternal(PathNode parentNode, Iterator<PathNode> iter, boolean forceUpdate)
  {
    if (parentNode != null && iter != null) {
      if (iter.hasNext()) {
        PathNode srcNode = iter.next();   // the requested path element
        if (parentNode.hasChild(srcNode)) {
          // child node found -> go to next node
          parentNode = parentNode.getChild(srcNode);
        } else {
          // child node not found -> appending file cache
          PathNode childNode = null;
          if (!parentNode.isCached() || forceUpdate) {
            File parentFile = new File(parentNode.getPath());
            File[] listFiles = parentFile.listFiles();
            if (listFiles != null) {
              // adding list of file entries to cache
              if (iter.hasNext()) {
                // node: adding only requested directory to file cache
                for (int i = 0; i < listFiles.length; i++) {
                  if (srcNode.isEqual(listFiles[i])) {
                    childNode = parentNode.insert(listFiles[i]);
                    break;
                  }
                }
              } else {
                // leaf: adding whole file list to file cache
                for (int i = 0; i < listFiles.length; i++) {
                  if (childNode == null && srcNode.isEqual(listFiles[i])) {
                    childNode = parentNode.insert(listFiles[i]);
                  } else {
                    parentNode.insert(listFiles[i]);
                  }
                }
                parentNode.setCached(true);
              }
            }
          }

          if (childNode != null) {
            parentNode = childNode;
          } else {
            // remaining path elements are assumed to not exist
            String retVal = createLookupPath(parentNode, srcNode, iter);
            return retVal;
          }
        }
        return lookupPathInternal(parentNode, iter, forceUpdate);
      } else {
        // returning looked up path
        String retVal = parentNode.getPath();
        return retVal;
      }
    }
    return null;
  }

  private String createLookupPath(PathNode parentNode, PathNode srcNode, Iterator<PathNode> iter)
  {
    StringBuilder sb = new StringBuilder();
    // adding processed path elements
    if (parentNode != null) {
      sb.append(parentNode.getPath());

      // adding current path element
      if (srcNode != null) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
          sb.append('/');
        }
        if (isToLowered()) {
          sb.append(srcNode.getName().toLowerCase(Locale.ENGLISH));
        } else {
          sb.append(srcNode.getName());
        }

        // adding remaining path elements
        if (iter != null) {
          while (iter.hasNext()) {
            sb.append('/');
            if (isToLowered()) {
              sb.append(iter.next().getName().toLowerCase(Locale.ENGLISH));
            } else {
              sb.append(iter.next().getName());
            }
          }
        }
      }
    }
    return sb.toString();
  }

//----------------------------- INNER CLASSES -----------------------------

  // A tree node that holds a specific path element. Can be either directory or file.
  private static class PathNode implements Comparable<PathNode>
  {
    private final ConcurrentSkipListSet<PathNode> children = new ConcurrentSkipListSet<PathNode>();

    private final String name;

    private PathNode parent;
    private boolean isCached;

    /**
     * Constructs a new path node with the given name.
     * @param name The path element name without path separators.
     */
    public PathNode(String name)
    {
      this.name = normalizedName(name);
      this.parent = null;
      this.isCached = false;
    }

    //--------------------- Begin Class Object ---------------------

    @Override
    public String toString()
    {
      return getPath();
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof PathNode) {
        return (getName().equals(((PathNode)o).getName()));
      } else {
        return false;
      }
    }

    //--------------------- End Class Object ---------------------

    //--------------------- Begin Interface Comparable ---------------------

    @Override
    public int compareTo(PathNode o)
    {
      return getName().compareTo(o.getName());
    }

    //--------------------- Begin Interface Comparable ---------------------

    /** Check if the given node is equal with the current one regardless of case. */
    public boolean isEqual(PathNode node)
    {
      if (node != null) {
        return (getName().equalsIgnoreCase(node.getName()));
      } else {
        return false;
      }
    }

    /** Check if the given File object matches the filename of the current node regardless of case. */
    public boolean isEqual(File file)
    {
      if (file != null) {
        return (name.equalsIgnoreCase(file.getName()));
      } else {
        return false;
      }
    }

    /** Returns a path string from root to this node. */
    public String getPath()
    {
      Stack<String> stack = new Stack<String>();
      PathNode curNode = this;
      while (curNode != null) {
        stack.push(curNode.getName());
        curNode = curNode.getParent();
      }
      StringBuilder sb = new StringBuilder(stack.pop());
      while (!stack.isEmpty()) {
        sb.append('/');
        sb.append(stack.pop());
      }
      if (sb.length() == 0) {
        // make sure we have created an absolute path
        sb.append('/');
      }
      return sb.toString();
    }

    /** Returns the name of the path element. */
    public String getName()
    {
      return name;
    }

    /** Indicates whether this node already contains a list of cached child nodes. */
    public boolean isCached()
    {
      return isCached;
    }

    /** Set whether this node is assumed to contained a list of cached child nodes. */
    public void setCached(boolean set)
    {
      isCached = set;
    }

    /** Returns whether the node is empty (i.e. counted as root). */
    public boolean isEmpty()
    {
      return (name == null || name.isEmpty());
    }

    /** Recursively removes all children from this node. */
    public void clear()
    {
      if (!children.isEmpty()) {
        Iterator<PathNode> iter = children.iterator();
        while (iter.hasNext()) {
          PathNode curNode = iter.next();
          curNode.clear();
        }
        children.clear();
        setCached(false);
      }
    }

    /** Returns the child node matching the given node or null if not found. */
    public PathNode getChild(PathNode node)
    {
      PathNode retVal = null;
      if (node != null) {
        Iterator<PathNode> iter = children.iterator();
        while (iter.hasNext()) {
          retVal = iter.next();
          if (node.isEqual(retVal)) {
            break;
          }
        }
      }
      return retVal;
    }

//    /** Returns the number of available direct child nodes. */
//    public int getChildCount()
//    {
//      return children.size();
//    }

    /** Returns the parent path node. */
    public PathNode getParent()
    {
      return parent;
    }

    /** Returns whether the node contains a child matching the specified node. */
    public boolean hasChild(PathNode node)
    {
      if (node != null) {
        return node.isEqual(getChild(node));
      } else {
        return false;
      }
    }

    /**
     * Adds the given node as a child of this node. Returns the added node.
     * Note: Replaces existing child nodes of equal name.
     */
    public PathNode insert(PathNode node)
    {
      if (node != null && !node.isEmpty()) {
        node.setParent(this);
        children.add(node);
        return node;
      }
      return null;
    }

    /**
     * Adds the last path element of the given file object to this node. Returns the added node.
     * Note: Replaces existing child nodes of equal name.
     */
    public PathNode insert(File file)
    {
      if (file != null) {
        return insert(new PathNode(file.getName()));
      }
      return null;
    }

    /** Removes the given node if it's a child node and returns it. */
    public PathNode remove(PathNode node)
    {
      PathNode retVal = null;
      if (node != null) {
        children.contains(node);
        retVal = getChild(node);
        if (retVal != null) {
          retVal.setParent(null);
          children.remove(retVal);
        }
      }
      return retVal;
    }

    /**
     * Creates a sorted array of PathNode objects, starting from the root up to the last element.
     * @param file
     * @return An array of PathNode object containing the individual path elements from root to leaf.
     *         Returns {@code null} on error.
     */
    public static List<PathNode> createPath(File file)
    {
      ArrayList<PathNode> retVal = new ArrayList<PathNode>();
      if (file != null) {
        try {
          String path = file.getCanonicalPath();
          if (path != null && !path.isEmpty()) {
            // adding path elements
            int startOfs = 0, curOfs = 0;
            while (curOfs < path.length()) {
              char ch = path.charAt(curOfs);
              if (isSeparator(ch)) {
                String name = path.substring(startOfs, curOfs);
                retVal.add(new PathNode(name));
                startOfs = curOfs+1;
              }
              curOfs++;
            }

            // adding final path element (if any)
            if (startOfs < curOfs) {
              String name = path.substring(startOfs, curOfs);
              retVal.add(new PathNode(name));
            }
          }
        } catch (IOException ioe) {
          ioe.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return retVal;
    }

    /** Attempts to find the PathNode object specified by pathList, starting at rootNode. */
    public static PathNode findPathNode(PathNode rootNode, List<PathNode> pathList)
    {
      PathNode retVal = null;
      if (rootNode != null && pathList != null && !pathList.isEmpty()) {
        retVal = rootNode;
        if (retVal.isEqual(pathList.get(0))) {
          for (int i = 1; i < pathList.size(); i++) {
            retVal = retVal.getChild(pathList.get(i));
            if (retVal == null || !retVal.isEqual(pathList.get(i))) {
              retVal = null;
              break;
            }
          }
        }
      }
      return retVal;
    }

    // Set parent for this node.
    private void setParent(PathNode parent)
    {
      this.parent = parent;
    }

    // Returns whether the given character is a path name separator
    private static boolean isSeparator(char ch)
    {
      return (ch == '/' || ch == '\\');
    }

    // Returns the first available path element without separator characters
    private static String normalizedName(String name)
    {
      if (name == null) name = "";
      int startIdx = 0, endIdx = name.length();
      int idx = Math.max(name.indexOf('/'), name.indexOf('\\'));
      if (idx == 0) {
        startIdx = 1;
        endIdx = Math.max(name.indexOf('/', startIdx), name.indexOf('\\', startIdx));
        if (endIdx < startIdx) {
          endIdx = name.length();
        }
      } else if (idx > 0) {
        endIdx = idx;
      }
      return name.substring(startIdx, endIdx);
    }
  }
}
