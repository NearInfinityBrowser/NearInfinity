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

/** Specialized HashBitmap type for parsing {@code SMTABLES.2DA} from IWDEE. */
public class Summon2daBitmap extends HashBitmap {
  private static final String TABLE_NAME = "SMTABLES.2DA";
  private static final TreeMap<Long, String> SUMMON_MAP = new TreeMap<>();

  public Summon2daBitmap(ByteBuffer buffer, int offset, int length, String name) {
    super(buffer, offset, length, name, getSummonTable());
  }

  public static String getTableName() {
    return TABLE_NAME;
  }

  private static synchronized TreeMap<Long, String> getSummonTable() {
    if (SUMMON_MAP.isEmpty()) {
      if (ResourceFactory.resourceExists(TABLE_NAME)) {
        Table2da table = Table2daCache.get(TABLE_NAME);
        if (table != null) {
          for (int row = 0, size = table.getRowCount(); row < size; row++) {
            String[] sid = table.get(row, 0).split("_");
            if (sid.length > 0) {
              try {
                long id = Long.parseLong(sid[0]);
                String resref = table.get(row, 1).toUpperCase(Locale.ENGLISH) + ".2DA";
                SUMMON_MAP.put(id, resref);
                if (!ResourceFactory.resourceExists(resref)) {
                  System.err.println("Resource does not exist: " + resref);
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }
    return SUMMON_MAP;
  }

  public static synchronized void resetSummonTable() {
    SUMMON_MAP.clear();
    Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TABLE_NAME));
  }
}
