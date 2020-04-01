// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class LongIntegerHashMap<V> extends TreeMap<Long, V>
{
  public LongIntegerHashMap()
  {
    super();
  }

  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder();
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
