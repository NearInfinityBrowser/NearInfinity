// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;

public final class IdsMapCache
{
  private static final Map<String, IdsMap> common = new HashMap<String, IdsMap>();

  public static void cacheInvalid(ResourceEntry entry)
  {
    if (entry != null) {
      common.remove(entry.toString().toUpperCase(Locale.ENGLISH));
    }
  }

  public static void clearCache()
  {
    common.clear();
  }

  public static synchronized IdsMap get(String name)
  {
    IdsMap retVal = null;
    if (name != null) {
      name = name.trim().toUpperCase(Locale.ENGLISH);
      retVal = common.get(name);
      if (retVal == null) {
        ResourceEntry resEntry = ResourceFactory.getResourceEntry(name);
        if (resEntry == null && name.equals("ATTSTYLE.IDS")) {
          resEntry = ResourceFactory.getResourceEntry("ATTSTYL.IDS");
        }
        if (resEntry == null) {
          System.err.println("Could not find " + name);
        } else {
          retVal = new IdsMap(resEntry);
          common.put(name, retVal);
        }
      }
    }
    return retVal;
  }

  private IdsMapCache(){}
}

