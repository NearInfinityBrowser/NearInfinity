// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.HashMap;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;

public class Table2daCache {
  private static final HashMap<ResourceEntry, Table2da> MAP = new HashMap<>();

  /** Removes the specified 2DA resource from the cache. */
  public static synchronized void cacheInvalid(ResourceEntry entry) {
    if (entry != null) {
      MAP.remove(entry);
    }
  }

  /** Removes all cached 2DA resources. */
  public static synchronized void clearCache() {
    MAP.clear();
  }

  /**
   * Returns a Table2da object based on the specified 2DA resource.
   *
   * @param resource 2DA resource name.
   * @return 2DA content as Table2da object or {@code null} on error.
   */
  public static Table2da get(String resource) {
    return get(ResourceFactory.getResourceEntry(resource));
  }

  /**
   * Returns a Table2da object based on the specified 2DA resource.
   *
   * @param entry 2DA resource entry.
   * @return 2DA content as Table2da object or {@code null} on error.
   */
  public static synchronized Table2da get(ResourceEntry entry) {
    return get(entry, true);
  }

  /**
   * Returns a Table2da object based on the specified 2DA resource.
   *
   * @param entry 2DA resource entry.
   * @param strict Indicates whether a valid file signature check should be enforced.
   * @return 2DA content as Table2da object or {@code null} on error.
   */
  public static synchronized Table2da get(ResourceEntry entry, boolean strict) {
    Table2da table = null;
    if (entry != null) {
      table = MAP.get(entry);
      if (table == null) {
        table = new Table2da(entry, strict);
        if (!table.isEmpty()) {
          MAP.put(table.getResourceEntry(), table);
        } else {
          table = null;
        }
      }
    }
    return table;
  }

  private Table2daCache() {
  }
}
