// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;

import java.util.HashMap;
import java.util.Map;

public final class IdsMapCache
{
  private static final Map<String, IdsMap> common = new HashMap<String, IdsMap>();

  public static void cacheInvalid(ResourceEntry entry)
  {
    common.remove(entry.toString().toUpperCase());
  }

  public static void clearCache()
  {
    common.clear();
  }

  public static IdsMap get(String name) // name must be in UPPER CASE
  {
    IdsMap map = common.get(name);
    if (map == null) {
      ResourceEntry resEntry = ResourceFactory.getInstance().getResourceEntry(name);
      if (resEntry == null && name.equalsIgnoreCase("ATTSTYLE.IDS"))
        resEntry = ResourceFactory.getInstance().getResourceEntry("ATTSTYL.IDS");
      if (resEntry == null)
        System.err.println("Could not find " + name);
      else {
        map = new IdsMap(resEntry);
        common.put(name, map);
      }
    }
    return map;
  }

  private IdsMapCache(){}
}

