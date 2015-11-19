// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;

public final class ProRef extends ResourceBitmap
{
  public ProRef(byte[] buffer, int offset, String name)
  {
    this(null, buffer, offset, name);
  }

  public ProRef(StructEntry parent, byte[] buffer, int offset, String name)
  {
    this(parent, buffer, offset, 2, name);
  }

  public ProRef(byte[] buffer, int offset, int size, String name)
  {
    this(null, buffer, offset, size, name);
  }

  public ProRef(StructEntry parent, byte[] buffer, int offset, int size, String name)
  {
    this(parent, buffer, offset, size, name, 0L);
  }

  public ProRef(byte[] buffer, int offset, String name, long minValue)
  {
    this(null, buffer, offset, 2, name, minValue);
  }

  public ProRef(StructEntry parent, byte[] buffer, int offset, String name, long minValue)
  {
    this(parent, buffer, offset, 2, name, minValue);
  }

  public ProRef(byte[] buffer, int offset, int size, String name, long minValue)
  {
    this(null, buffer, offset, size, name, minValue);
  }

  public ProRef(StructEntry parent, byte[] buffer, int offset, int size, String name, long minValue)
  {
    super(parent, buffer, offset, size, name, createProRefList(minValue), FMT_REF_VALUE);
  }

  public ResourceEntry getSelectedEntry()
  {
    return ResourceFactory.getResourceEntry(getResourceName(), true);
  }

  private static List<RefEntry> createProRefList(long minValue)
  {
    minValue = Math.max(0L, minValue);

    LongIntegerHashMap<IdsMapEntry> map = IdsMapCache.get("PROJECTL.IDS").getMap();
    List<RefEntry> retVal = new ArrayList<RefEntry>(2 + map.size());
    retVal.add(new RefEntry(0L, "Default"));
    retVal.add(new RefEntry(1L, "None"));

    long[] keys = map.keys();
    for (final long key: keys) {
      if (key >= minValue) {
        RefEntry entry = new RefEntry(key + 1L, map.get(key).getString().toUpperCase(Locale.ENGLISH) + ".PRO");
        if ((key + 1L) > 1L) {
          retVal.add(entry);
        } else {
          retVal.set((int)(key + 1L), entry);
        }
      }
    }

    return retVal;
  }
}

