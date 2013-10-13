// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;

public final class IdsFlag extends Flag
{
  public IdsFlag(byte buffer[], int offset, int length, String name, String resource)
  {
    super(buffer, offset, length, name);
    LongIntegerHashMap<IdsMapEntry> idsmap = IdsMapCache.get(resource).getMap();
    nodesc = idsmap.get(0L).getString();
    table = new String[8 * length];
    for (int i = 0; i < table.length; i++)
      table[i] = idsmap.get((long)Math.pow(2, i)).getString();
  }
}

