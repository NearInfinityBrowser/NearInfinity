// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.Locale;

import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.LongIntegerHashMap;
import infinity.util.Table2da;
import infinity.util.Table2daCache;

/** Specialized HashBitmap type for parsing MSECTYPE.2DA (Secondary type) entries. */
public class SecType2daBitmap extends HashBitmap
{
  private static final String TableName = "MSECTYPE.2DA";
  private static final LongIntegerHashMap<String> typeMap = new LongIntegerHashMap<String>();

  public SecType2daBitmap(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public SecType2daBitmap(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, getTypeTable());
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

  private static LongIntegerHashMap<String> getTypeTable()
  {
    if (typeMap.isEmpty()) {
      if (ResourceFactory.resourceExists(TableName)) {
        Table2da table = Table2daCache.get(TableName);
        if (table != null) {
          for (int row = 1, size = table.getRowCount(); row < size; row++) {
            long id = row - 1;
            String label = table.get(row, 0).toUpperCase(Locale.ENGLISH);
            typeMap.put(Long.valueOf(id), label);
          }
        }
      }
    }
    return typeMap;
  }

  public static void resetSummonTable()
  {
    typeMap.clear();
  }
}
