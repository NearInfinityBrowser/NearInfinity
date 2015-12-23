// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.Locale;

import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.IdsMap;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;
import infinity.util.Table2da;
import infinity.util.Table2daCache;

/** Specialized HashBitmap type for parsing "primary type" entries. */
public class PriTypeBitmap extends HashBitmap
{
  private static final String TableName;
  private static final String[] s_school = {"None", "Abjurer", "Conjurer", "Diviner", "Enchanter",
                                            "Illusionist", "Invoker", "Necromancer", "Transmuter",
                                            "Generalist"};
  private static final LongIntegerHashMap<String> typeMap = new LongIntegerHashMap<String>();

  static {
    if (ResourceFactory.resourceExists("MSCHOOL.2DA")) {
      TableName = "MSCHOOL.2DA";
    } else if (ResourceFactory.resourceExists("SCHOOL.IDS")) {
      TableName = "SCHOOL.IDS";
    } else {
      TableName = "";
    }
  }

  public PriTypeBitmap(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public PriTypeBitmap(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, getTypeTable());
  }

  public static String getTableName()
  {
    return ResourceFactory.resourceExists(TableName) ? TableName : "";
  }

  public static String[] getTypeArray()
  {
    LongIntegerHashMap<String> map = getTypeTable();
    long[] keys = map.keys();
    String[] retVal = new String[keys.length];
    for (int i = 0; i < keys.length; i++) {
      retVal[i] = map.get(Long.valueOf(keys[i]));
    }
    return retVal;
  }

  private static synchronized LongIntegerHashMap<String> getTypeTable()
  {
    if (typeMap.isEmpty()) {
      if (ResourceFactory.resourceExists(TableName) && TableName.endsWith(".2DA")) {
        // using MSCHOOL.2DA
        Table2da table = Table2daCache.get(TableName);
        if (table != null) {
          for (int row = 0, size = table.getRowCount(); row < size; row++) {
            long id = row;
            String label = table.get(row, 0).toUpperCase(Locale.ENGLISH);
            typeMap.put(Long.valueOf(id), label);
          }
        }
      } else if (ResourceFactory.resourceExists(TableName) && TableName.endsWith(".IDS")) {
        // using SCHOOL.IDS
        typeMap.put(Long.valueOf(0L), "NONE");
        IdsMap map = IdsMapCache.get(TableName);
        LongIntegerHashMap<IdsMapEntry> entries = map.getMap();
        long[] keys = entries.keys();
        for (int i = 0; i < keys.length; i++) {
          Long id = Long.valueOf(keys[i]);
          typeMap.put(id, entries.get(id).getString().toUpperCase(Locale.ENGLISH));
        }
      } else {
        // using predefined values
        for (int i = 0; i < s_school.length; i++) {
          typeMap.put(Long.valueOf(i), s_school[i].toUpperCase(Locale.ENGLISH));
        }
      }
    }
    return typeMap;
  }

  public static synchronized void resetTypeTable()
  {
    typeMap.clear();
    if (TableName.endsWith(".2DA")) {
      Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TableName));
    } else if (TableName.endsWith(".IDS")) {
      IdsMapCache.cacheInvalid(ResourceFactory.getResourceEntry(TableName));
    }
  }
}
