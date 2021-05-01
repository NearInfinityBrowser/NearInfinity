// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A global storage class that caches data objects of supported types associated with a unique key.
 */
public class SharedResourceCache
{
  // Identifies the type of cache object to retrieve
  public static enum Type {
    ICON,
    ANIMATION,
    ACTOR
  }

  private static EnumMap<Type, HashMap<Object, DataWrapper>> tables = new EnumMap<>(Type.class);

  static {
    for (final Type type : Type.values()) {
      tables.put(type, new HashMap<Object, DataWrapper>());
    }
  }

  /** Removes all entries from the cache. */
  public static void clearCache()
  {
    for (final Type type : Type.values()) {
      tables.get(type).clear();
    }
  }

  /**
   * Generates a simple key from the hash code of the specified object.
   * @param o The object to create a key for.
   * @return A key generated from the hash code of the specified object, or 0 on {@code null}.
   */
  public static String createKey(Object o)
  {
    return String.format("%08x", (o != null) ? o.hashCode() : 0);
  }


  /**
   * Adds the data object to the cache.
   * @param key A unique key for identifying the data object.
   * @param data The data object to store.
   * @return {@code true} if a new entry has been created for the data object.
   *         {@code false} if the entry already exists in the cache.
   * @exception NullPointerException if key is {@code null}.
   */
  public static synchronized boolean add(Type type, Object key, Object data)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (tables.get(type).containsKey(key)) {
      // add reference only
      tables.get(type).get(key).incRefCount();
      return false;
    } else {
      // add new entry
      tables.get(type).put(key, new DataWrapper(data));
      return true;
    }
  }

  /**
   * Adds another reference to the cache entry specified by the key.
   * @param key A unique key for identifying a data object.
   * @return {@code true} if the key exists, {@code false} otherwise.
   * @exception NullPointerException if key is {@code null}.
   */
  public static synchronized boolean add(Type type, Object key)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (tables.get(type).containsKey(key)) {
      tables.get(type).get(key).incRefCount();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Removes one reference from the entry identified by the specified key. If the reference count for
   * the entry is 0, the whole entry will be removed from cache.
   * @param key The key identifying the data object.
   * @return {@code true} if the entry has been removed completely, {@code false} otherwise.
   * @exception NullPointerException if key is {@code null}.
   */
  public static synchronized boolean remove(Type type, Object key)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (tables.get(type).containsKey(key)) {
      DataWrapper dw = tables.get(type).get(key);
      dw.decRefCount();
      if (!dw.isReferenced()) {
        tables.get(type).remove(key);
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the data object identified by the specified key.
   * @param key The key identifying the data object.
   * @return The data object identified by the specified key.
   * @exception NullPointerException if key is {@code null}.
   */
  public static Object get(Type type, Object key)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (tables.get(type).containsKey(key)) {
      return tables.get(type).get(key).getData();
    }
    return null;
  }

  /**
   * Returns whether a cached data object of the specified key exists.
   * @param key The key identifying a cached data object.
   * @return {@code true} if a cached entry exists, {@code false} otherwise.
   * @exception NullPointerException if key is {@code null}.
   */
  public static boolean contains(Type type, Object key)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    return tables.get(type).containsKey(key);
  }

  /**
   * Attempts to find the first key that is associated with the specified data object.
   * @param data The data object.
   * @return The first key that is associated with the specified data object,
   *         or {@code null} if no key has been found.
   */
  public static Object getKey(Type type, Object data)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    Iterator<Object> iter = tables.get(type).keySet().iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      DataWrapper dw = tables.get(type).get(key);
      if ((dw.getData() == null && data == null) ||
          (dw.getData() != null && dw.getData().equals(data))) {
        return key;
      }
    }
    return null;
  }

  /**
   * Returns the number of references for the data object identified by the specified key.
   * @param key The key identifying the cached data object.
   * @return Number of references for the specified data object.
   * @exception NullPointerException if key is {@code null}.
   */
  public static int getReferenceCount(Type type, Object key)
  {
    if (type == null) {
      throw new NullPointerException("type is null");
    }
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (key != null) {
      if (tables.get(type).containsKey(key)) {
        return tables.get(type).get(key).getRefCount();
      }
    }
    return 0;
  }


  /** Not needed. Contains only static methods and data. */
  private SharedResourceCache() {}


//----------------------------- INNER CLASSES -----------------------------

  /** Wrapper for data objects that supports reference counting. */
  private static class DataWrapper
  {
    private Object data;
    private int refCount;

    /**
     * Initializes a new data wrapper object. Reference counter is set to 1.
     * @param data The associated data object.
     */
    public DataWrapper(Object data)
    {
      this.data = data;
      refCount = 1;
    }

    /**
     * Returns the data object.
     */
    public Object getData()
    {
      return data;
    }

    /**
     * Returns the current reference counter value.
     */
    public int getRefCount()
    {
      return refCount;
    }

    /**
     * Increases the reference counter by 1.
     */
    public synchronized void incRefCount()
    {
      refCount++;
    }

    /**
     * Decreases the reference counter by 1 and returns whether the data is still referenced by any
     * external object.
     * @return {@code true} if the data is referenced, {@code false} otherwise.
     */
    public synchronized boolean decRefCount()
    {
      if (refCount > 0) {
        refCount--;
      }
      return (refCount > 0);
    }

    /**
     * Returns whether the data is externally referenced.
     * @return {@code true} if the data is referenced, {@code false} otherwise.
     */
    public boolean isReferenced()
    {
      return (refCount > 0);
    }
  }
}
