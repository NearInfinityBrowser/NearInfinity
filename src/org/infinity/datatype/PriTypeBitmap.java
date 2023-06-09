// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.TreeMap;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/** Specialized HashBitmap type for parsing "primary type" entries. */
public class PriTypeBitmap extends HashBitmap {
  private static final String TABLE_NAME;
  private static final String[] SCHOOL_TYPE = { "None", "Abjurer", "Conjurer", "Diviner", "Enchanter", "Illusionist",
                                                "Invoker", "Necromancer", "Transmuter", "Generalist" };
  private static final TreeMap<Long, String> TYPE_MAP = new TreeMap<>();

  static {
    if (ResourceFactory.resourceExists("MSCHOOL.2DA")) {
      TABLE_NAME = "MSCHOOL.2DA";
    } else if (ResourceFactory.resourceExists("SCHOOL.IDS")) {
      TABLE_NAME = "SCHOOL.IDS";
    } else {
      TABLE_NAME = "";
    }
  }

  public PriTypeBitmap(ByteBuffer buffer, int offset, int length, String name) {
    super(buffer, offset, length, name, getTypeTable());
  }

  public static String getTableName() {
    return ResourceFactory.resourceExists(TABLE_NAME) ? TABLE_NAME : "";
  }

  public static String[] getTypeArray() {
    final TreeMap<Long, String> map = getTypeTable();
    return map.values().toArray(new String[map.size()]);
  }

  private static synchronized TreeMap<Long, String> getTypeTable() {
    if (TYPE_MAP.isEmpty()) {
      if (ResourceFactory.resourceExists(TABLE_NAME) && TABLE_NAME.endsWith(".2DA")) {
        // using MSCHOOL.2DA
        Table2da table = Table2daCache.get(TABLE_NAME);
        if (table != null) {
          for (int row = 0, size = table.getRowCount(); row < size; row++) {
            long id = row;
            String label = table.get(row, 0).toUpperCase(Locale.ENGLISH);
            TYPE_MAP.put(id, label);
          }
          if (TYPE_MAP.size() == 10) {
            // XXX: Doesn't appear to be listed in unmodded games
            TYPE_MAP.put(10L, "WILDMAGE");
          }
        }
      } else if (ResourceFactory.resourceExists(TABLE_NAME) && TABLE_NAME.endsWith(".IDS")) {
        // using SCHOOL.IDS
        TYPE_MAP.put(0L, "NONE");
        IdsMap map = IdsMapCache.get(TABLE_NAME);
        for (final IdsMapEntry e : map.getAllValues()) {
          TYPE_MAP.put(e.getID(), e.getSymbol().toUpperCase(Locale.ENGLISH));
        }
      } else {
        // using predefined values
        for (int i = 0; i < SCHOOL_TYPE.length; i++) {
          TYPE_MAP.put((long) i, SCHOOL_TYPE[i].toUpperCase(Locale.ENGLISH));
        }
        if (TYPE_MAP.size() == 10 && Profile.getGame() == Profile.Game.PSTEE) {
          TYPE_MAP.put(10L, "WILDMAGE");
        }
      }
    }
    return TYPE_MAP;
  }

  public static synchronized void resetTypeTable() {
    TYPE_MAP.clear();
    if (TABLE_NAME.endsWith(".2DA")) {
      Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TABLE_NAME));
    } else if (TABLE_NAME.endsWith(".IDS")) {
      IdsMapCache.remove(ResourceFactory.getResourceEntry(TABLE_NAME));
    }
  }
}
