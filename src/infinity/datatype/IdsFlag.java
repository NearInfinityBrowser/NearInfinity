// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;

public final class IdsFlag extends Flag
{
  public IdsFlag(byte buffer[], int offset, int length, String name, String resource)
  {
    this(null, buffer, offset, length, name, resource);
  }

  public IdsFlag(StructEntry parent, byte buffer[], int offset, int length, String name, String resource)
  {
    super(parent, buffer, offset, length, name);
    LongIntegerHashMap<IdsMapEntry> idsmap = IdsMapCache.get(resource).getMap();
    setEmptyDesc(idsmap.get(0L).getString());
    // fetching flag labels from IDS
    String[] stable = new String[8 * length];
    for (int i = 0; i < stable.length; i++) {
      stable[i] = idsmap.get(Long.valueOf(1L << i)).getString();
    }
    setFlagDescriptions(length, stable, 0, ';');
  }
}

