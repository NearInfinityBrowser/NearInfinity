// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.LongIntegerHashMap;
import infinity.util.Table2da;
import infinity.util.Table2daCache;

/** Specialized HashBitmap type for parsing SONGLIST.2DA entries. */
public class Song2daBitmap extends HashBitmap
{
  private static final String TableName = "SONGLIST.2DA";
  private static final LongIntegerHashMap<String> songMap = new LongIntegerHashMap<String>();

  public Song2daBitmap(byte buffer[], int offset, int length)
  {
    this(null, buffer, offset, length);
  }

  public Song2daBitmap(StructEntry parent, byte buffer[], int offset, int length)
  {
    this(parent, buffer, offset, length, "Song");
  }

  public Song2daBitmap(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public Song2daBitmap(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, getSongTable());
  }

  public static String getTableName()
  {
    return TableName;
  }

  private static LongIntegerHashMap<String> getSongTable()
  {
    if (songMap.isEmpty()) {
      Table2da table = Table2daCache.get(TableName);
      if (table != null) {
        for (int row = 0, size = table.getRowCount(); row < size; row++) {
          String s = table.get(row, 0);
          try {
            long id = Long.parseLong(s);
            songMap.put(Long.valueOf(id), table.get(row, 1));
          } catch (NumberFormatException e) {
          }
        }
      }
      songMap.put(Long.valueOf(0xfffffffeL), "Continue area music");
      songMap.put(Long.valueOf(0xffffffffL), "Continue outside music");
    }
    return songMap;
  }

  public static void resetSonglist()
  {
    songMap.clear();
    Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TableName));
  }
}
