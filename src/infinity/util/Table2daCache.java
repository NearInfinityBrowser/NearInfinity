// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;

import java.util.HashMap;

public class Table2daCache
{
  private static final HashMap<ResourceEntry, Table2da> map = new HashMap<ResourceEntry, Table2da>();

  /** Removes the specified 2DA resource from the cache. */
  public static synchronized void cacheInvalid(ResourceEntry entry)
  {
    map.remove(entry);
  }

  /** Removes all cached 2DA resources. */
  public static synchronized void clearCache()
  {
    map.clear();
  }

  /**
   * Returns a Table2da object based on the specified 2DA resource.
   * @param entry 2DA resource name.
   * @return 2DA content as Table2da object or <code>null</code> on error.
   */
  public static Table2da get(String resource)
  {
    return get(ResourceFactory.getResourceEntry(resource));
  }

  /**
   * Returns a Table2da object based on the specified 2DA resource.
   * @param entry 2DA resource entry.
   * @return 2DA content as Table2da object or <code>null</code> on error.
   */
  public static synchronized Table2da get(ResourceEntry entry)
  {
    Table2da table = null;
    if (entry != null) {
      table = map.get(entry);
      if (table == null) {
        table = new Table2da(entry);
        if (!table.isEmpty()) {
          map.put(table.getResourceEntry(), table);
        } else {
          table = null;
        }
      }
    }
    return table;
  }

  private Table2daCache() {}
}
