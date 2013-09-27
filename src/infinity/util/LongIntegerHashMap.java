// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.util.*;

public final class LongIntegerHashMap<V> extends HashMap<Long, V>
{
  public LongIntegerHashMap()
  {
    super();
  }

  public LongIntegerHashMap(int initialCapacity)
  {
    super(initialCapacity);
  }

  public LongIntegerHashMap(int initialCapacity, float loadFactor)
  {
    super(initialCapacity, loadFactor);
  }

  public LongIntegerHashMap(Map<Long, ? extends V> m)
  {
    super(m);
  }

  public long[] keys()
  {
    Set<Long> set = keySet();
    long[] result = new long[set.size()];
    Iterator<Long> iter = set.iterator();
    int i = 0;
    while (iter.hasNext() && i < result.length) {
      result[i++] = iter.next().longValue();
    }
    return result;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append('{');
    Set<Map.Entry<Long, V>> set = entrySet();
    Iterator<Map.Entry<Long, V>> i = set.iterator();
    boolean hasNext = i.hasNext();
    while (hasNext) {
      Map.Entry<Long, V> e = i.next();
      long key = e.getKey();
      V value = e.getValue();
      buf.append(key);
      buf.append('=');
      if (value == this)
        buf.append("(this Map)");
      else
        buf.append(value);
      hasNext = i.hasNext();
      if (hasNext)
        buf.append(", ");
    }
    buf.append('}');
    return buf.toString();
  }
}
