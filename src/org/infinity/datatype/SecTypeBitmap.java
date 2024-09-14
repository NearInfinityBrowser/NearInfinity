// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.TreeMap;

import org.infinity.resource.ResourceFactory;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/** Specialized HashBitmap type for parsing "secondary type" entries. */
public class SecTypeBitmap extends HashBitmap {
  private static final String TABLE_NAME = ResourceFactory.resourceExists("MSECTYPE.2DA") ? "MSECTYPE.2DA" : "";

  private static final String[] CATEGORY_ARRAY = { "None", "Spell protections", "Specific protections",
      "Illusionary protections", "Magic attack", "Divination attack", "Conjuration", "Combat protections",
      "Contingency", "Battleground", "Offensive damage", "Disabling", "Combination", "Non-combat" };

  private static final TreeMap<Long, String> TYPE_MAP = new TreeMap<>();

  public SecTypeBitmap(ByteBuffer buffer, int offset, int length, String name) {
    super(buffer, offset, length, name, getTypeTable());
  }

  public static String getTableName() {
    return TABLE_NAME;
  }

  public static String[] getTypeArray() {
    final TreeMap<Long, String> map = getTypeTable();
    return map.values().toArray(new String[0]);
  }

  private static synchronized TreeMap<Long, String> getTypeTable() {
    if (TYPE_MAP.isEmpty()) {
      if (ResourceFactory.resourceExists(TABLE_NAME)) {
        // using MSECTYPE.2DA
        Table2da table = Table2daCache.get(TABLE_NAME);
        if (table != null) {
          for (int row = 0, size = table.getRowCount(); row < size; row++) {
            String label = table.get(row, 0).toUpperCase(Locale.ENGLISH);
            TYPE_MAP.put((long) row, label);
          }
        }
      } else {
        // using predefined values
        for (int i = 0; i < CATEGORY_ARRAY.length; i++) {
          TYPE_MAP.put((long) i, CATEGORY_ARRAY[i].toUpperCase(Locale.ENGLISH));
        }
      }
    }
    return TYPE_MAP;
  }

  public static synchronized void resetTypeTable() {
    TYPE_MAP.clear();
    if (TABLE_NAME.endsWith(".2DA")) {
      Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TABLE_NAME));
    }
  }
}
