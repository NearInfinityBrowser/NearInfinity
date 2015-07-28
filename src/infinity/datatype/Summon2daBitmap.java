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

/** Specialized HashBitmap type for parsing SMTABLES.2DA from IWDEE. */
public class Summon2daBitmap extends HashBitmap
{
  private static final String TableName = "SMTABLES.2DA";
  private static final LongIntegerHashMap<String> summonMap = new LongIntegerHashMap<String>();

  public Summon2daBitmap(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public Summon2daBitmap(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, getSummonTable());
  }

  public static String getTableName()
  {
    return TableName;
  }

  private static LongIntegerHashMap<String> getSummonTable()
  {
    if (summonMap.isEmpty()) {
      if (ResourceFactory.resourceExists(TableName)) {
        Table2da table = Table2daCache.get(TableName);
        if (table != null) {
          for (int row = 1, size = table.getRowCount(); row < size; row++) {
            String[] sid = table.get(row, 0).split("_");
            if (sid.length > 0) {
              try {
                long id = Long.parseLong(sid[0]);
                String resref = table.get(row, 1).toUpperCase(Locale.ENGLISH) + ".2DA";
                summonMap.put(Long.valueOf(id), resref);
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
    return summonMap;
  }

  public static void resetSummonTable()
  {
    summonMap.clear();
    Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TableName));
  }
}
