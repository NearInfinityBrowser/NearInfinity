// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import org.infinity.resource.StructEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.LongIntegerHashMap;

public class IdsBitmap extends HashBitmap
{
  public IdsBitmap(byte[] buffer, int offset, int length, String name, String resource)
  {
    this(null, buffer, offset, length, name, resource, 0);
  }

  public IdsBitmap(StructEntry parent, byte[] buffer, int offset, int length, String name, String resource)
  {
    this(parent, buffer, offset, length, name, resource, 0);
  }

  public IdsBitmap(byte[] buffer, int offset, int length, String name, String resource, int idsStart)
  {
    this(null, buffer, offset, length, name, resource, idsStart);
  }

  public IdsBitmap(StructEntry parent, byte[] buffer, int offset, int length, String name, String resource,
                   int idsStart)
  {
    super(parent, buffer, offset, length, name, createResourceList(resource, idsStart), true);
  }

  public int getIdsMapEntryCount()
  {
    return getHashBitmap().size();
  }

  public IdsMapEntry getIdsMapEntryByIndex(int index)
  {
    if (index >= 0 && index < getHashBitmap().size()) {
      return (IdsMapEntry)getHashBitmap().get(getHashBitmap().keys()[index]);
    } else {
      return null;
    }
  }

  public IdsMapEntry getIdsMapEntryById(long id)
  {
    return (IdsMapEntry)getHashBitmap().get(Long.valueOf(id));
  }

  public void addIdsMapEntry(IdsMapEntry entry)
  {
    if (entry != null) {
      @SuppressWarnings("unchecked")
      LongIntegerHashMap<IdsMapEntry>map = (LongIntegerHashMap<IdsMapEntry>)getHashBitmap();
      if (!map.containsKey(Long.valueOf(entry.getID()))) {
        map.put(Long.valueOf(entry.getID()), entry);
      }
    }
  }

  private static LongIntegerHashMap<IdsMapEntry> createResourceList(String resource, int idsStart)
  {
    LongIntegerHashMap<IdsMapEntry> retVal = null;
    IdsMap idsMap = IdsMapCache.get(resource);
    if (idsMap != null) {
      if (idsStart != 0) {
        LongIntegerHashMap<IdsMapEntry> orgMap = idsMap.getMap();
        retVal = new LongIntegerHashMap<IdsMapEntry>();

        long[] keys = orgMap.keys();
        for (final long id : keys) {
          if (id >= idsStart) {
            IdsMapEntry entry = orgMap.get(id);
            long newid = id - (long)idsStart;
            retVal.put(newid, new IdsMapEntry(newid, entry.getString(), entry.getParameters()));
          }
        }
      } else {
        retVal = idsMap.getMap();
      }
    }
    return retVal;
  }
}

