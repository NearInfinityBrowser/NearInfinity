// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

/**
 * Implementation of a tree node that can form an n-ary tree.
 * Provides basic operations such as getting/setting keys, values and child nodes,
 * adding/removing child nodes or finding specific child nodes within the tree.
 *
 * @param <K> the type of the key to identifiy the node in a tree.
 * @param <V> the type of the data associated with a tree node.
 */
public class MapTree<K, V> implements Cloneable
{
  private final K key;
//  private final Collection<MapTree<K, V>> children;
  private final HashMap<K, MapTree<K, V>> children;

  private MapTree<K, V> parent;
  private V value;

  /** Creates a path of MapTree objects, starting from the root node up to the specified node. */
  public static <K, V> Collection<MapTree<K, V>> getNodePath(MapTree<K, V> node)
  {
    Collection<MapTree<K, V>> retVal = new Vector<>();
    if (node != null) {
      Stack<MapTree<K, V>> stack = new Stack<>();
      MapTree<K, V> curNode = node;
      while (curNode != null) {
        stack.push(curNode);
        curNode = curNode.getParent();
      }

      while (!stack.isEmpty()) {
        retVal.add(stack.pop());
      }
    }
    return retVal;
  }

  /** Returns the root of the specified node. */
  public static <K, V> MapTree<K, V> getRoot(MapTree<K, V> node)
  {
    if (node != null) {
      MapTree<K, V> curNode = node;
      while (curNode.getParent() != null) {
        curNode = curNode.getParent();
      }
      return curNode;
    }
    return null;
  }

  /**
   * Constructs a new MapNode object with the given key and value arguments.
   * @param key The node key.
   * @param value The associated value.
   */
  public MapTree(K key, V value)
  {
    if (key == null) {
      throw new NullPointerException("key must not be null");
    }
    this.parent = null;
    this.children = new HashMap<>();
    this.key = key;
    this.value = value;
  }

//--------------------- Begin Interface Cloneable ---------------------

  /**
   * Creates a copy of this node and all of its children.
   * @return a clone of this node and all of its children.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Object clone()
  {
    MapTree<K, V> node = new MapTree<>(key, value);
    for (Iterator<MapTree<K, V>> iter = children.values().iterator(); iter.hasNext();) {
      node.addChild((MapTree<K, V>)iter.next().clone());
    }
    return node;
  }

//--------------------- End Interface Cloneable ---------------------

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof MapTree<?, ?>) {
      MapTree<?, ?> node = (MapTree<?, ?>)o;
      boolean b = key.equals(node.getKey());
      if (b && value != null) {
        b &= value.equals(node.getValue());
      }
      return b;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    int hash = key.hashCode();
    if (value != null) {
      hash ^= value.hashCode();
    }
    return hash;
  }

  /** Adds a new child to this node. May overwrite a child node containing a matching key. */
  public boolean addChild(MapTree<K, V> child)
  {
    if (child != null) {
      removeChild(child.getKey());
      child.parent = this;
      children.put(child.getKey(), child);
      return true;
    }
    return false;
  }

  /**
   * Adds all children of the given collection to this node. Existing child nodes
   * containing a matching key will be overwritten. Returns the number of added child nodes.
   */
  public int addChildren(Collection<MapTree<K, V>> children)
  {
    int retVal = 0;
    if (children != null && !children.isEmpty()) {
      for (Iterator<MapTree<K, V>> iter = children.iterator(); iter.hasNext();) {
        MapTree<K, V> node = iter.next();
        if (addChild(node)) {
          retVal++;
        }
      }
    }
    return retVal;
  }

  /**
   * Searches for the first node containing a matching key.
   * @param key The key to search.
   * @return The first available node containing the specified key.
   */
  public MapTree<K, V> findNode(K key)
  {
    if (key != null) {
      Collection<MapTree<K, V>> retVal = findNodesRecursive(null, this, key, true);
      if (retVal != null && !retVal.isEmpty()) {
        return retVal.iterator().next();
      }
    }
    return null;
  }

  /**
   * Searches the whole tree, starting from the current node for nodes containing a matching key.
   * @param key The key to search.
   * @return A collection of nodes containing the specified key.
   */
  public Collection<MapTree<K, V>> findNodes(K key)
  {
    Collection<MapTree<K, V>> retVal = new Vector<>();
    if (key != null) {
      retVal = findNodesRecursive(retVal, parent, key, false);
    }
    return retVal;
  }

  /** Returns the number of child nodes. */
  public int getChildCount()
  {
    return children.size();
  }

  /** Returns the child node matching the given key, or null otherwise. */
  public MapTree<K, V> getChild(K key)
  {
    if (key != null) {
      return children.get(key);
    }
    return null;
  }

  /** Returns an unmodifiable collection of all children associated with this node. */
  public Collection<MapTree<K, V>> getChildren()
  {
    return Collections.unmodifiableCollection(children.values());
  }

  /** Returns the node key. */
  public K getKey()
  {
    return key;
  }

  /** Creates a node path, starting from the root node up to the current node. */
  public Collection<MapTree<K, V>> getNodePath()
  {
    return getNodePath(this);
  }

  /** Returns the parent node (if any). */
  public MapTree<K, V> getParent()
  {
    return parent;
  }

  /** Returns the value associated with the node (if any). */
  public V getValue()
  {
    return value;
  }

  /**
   * Removes the child node containing the matching key and returns it.
   * Does nothing if no matching child node exists.
   */
  public MapTree<K, V> removeChild(K key)
  {
    if (key != null) {
      MapTree<K, V> node = children.remove(key);
      if (node != null) {
        node.parent = null;
      }
      return node;
    }
    return null;
  }

  /**
   * Removes all children matching the keys in the given collection.
   * Returns a collection of child nodes which have been successfully removed.
   */
  public Collection<MapTree<K, V>> removeChildren(Collection<K> keys)
  {
    Collection<MapTree<K, V>> retVal = new Vector<>();
    if (keys != null && !keys.isEmpty()) {
      for (Iterator<K> iter = keys.iterator(); iter.hasNext();) {
        MapTree<K, V> node = removeChild(iter.next());
        if (node != null) {
          retVal.add(node);
        }
      }
    }
    return retVal;
  }

  /**
   * Removes all children from the current node.
   */
  public void removeAllChildren()
  {
    for (Iterator<K> iter = children.keySet().iterator(); iter.hasNext();) {
      K key = iter.next();
      MapTree<K, V> node = children.remove(key);
      if (node != null) {
        node.parent = null;
      }
    }
  }

  /**
   * Replaces the value of the child node containing a matching key.
   * Returns whether the operation was successful.
   */
  public boolean setChild(K key, V value)
  {
    MapTree<K, V> node = getChild(key);
    if (node != null) {
      node.setValue(value);
      return true;
    }
    return false;
  }

  /** Assigns a new value to this node. Returns the previously assigned value (if any). */
  public V setValue(V newValue)
  {
    V retVal = value;
    value = newValue;
    return retVal;
  }


  // Recursively searches all child nodes for the given key.
  // Returns either the first match only or a list of all available matches.
  private static<K, V> Collection<MapTree<K, V>> findNodesRecursive(Collection<MapTree<K, V>> retVal,
                                                                    MapTree<K, V> parent, K key,
                                                                    boolean firstMatch)
  {
    if (retVal == null) {
      retVal = new Vector<>();
    }

    if (firstMatch && !retVal.isEmpty()) {
      return retVal;
    }

    if (parent != null && key != null) {
      for (Iterator<MapTree<K, V>> iter = parent.getChildren().iterator();
           iter.hasNext() && (!firstMatch || retVal.isEmpty());) {
        MapTree<K, V> node = iter.next();
        if (node.getKey().equals(key)) {
          retVal.add(node);
        }
        findNodesRecursive(retVal, node, key, firstMatch);
      }
    }

    return retVal;
  }
}
