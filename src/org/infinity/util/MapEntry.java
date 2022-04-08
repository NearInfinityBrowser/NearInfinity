// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Map.Entry;

/**
 * Stores a key/value pair.
 *
 * @param <K> The type of key.
 * @param <V> The type of value.
 */
public class MapEntry<K, V> implements Entry<K, V> {
  private K key;
  private V value;

  public MapEntry() {
    this(null, null);
  }

  public MapEntry(K key) {
    this(key, null);
  }

  public MapEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  // --------------------- Begin Interface Map.Entry ---------------------

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V value) {
    V retVal = this.value;
    this.value = value;
    return retVal;
  }

  // --------------------- End Interface Map.Entry ---------------------

  /**
   * Replaces the current key with the specified argument.
   *
   * @param key The new key.
   * @return The old key.
   */
  public K setKey(K key) {
    K retVal = this.key;
    this.key = key;
    return retVal;
  }
}
