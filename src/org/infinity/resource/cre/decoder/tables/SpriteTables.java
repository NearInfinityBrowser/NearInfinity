// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.tables;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.resource.cre.decoder.MonsterPlanescapeDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.BufferedResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.Table2da;

/**
 * A static class that provides information and methods for hardcoded creature animations.
 */
public class SpriteTables
{
  // Number of non-PST table columns (id column included)
  public static final int NUM_COLUMNS           = 16;
  // Number of PST table columns (id column included)
  public static final int NUM_COLUMNS_PST       = 9;

  // Header column index
  public static final int COLUMN_ID             = 0;    // int (hex/composite)
  // Column indices for non-PST sprite tables
  public static final int COLUMN_RESREF         = 1;    // string
  public static final int COLUMN_TYPE           = 2;    // int
  public static final int COLUMN_ELLIPSE        = 3;    // int
  public static final int COLUMN_SPACE          = 4;    // int
  public static final int COLUMN_BLENDING       = 5;    // int (bitfield)
  public static final int COLUMN_PALETTE        = 6;    // string
  public static final int COLUMN_PALETTE2       = 7;    // string
  public static final int COLUMN_RESREF2        = 8;    // string
  public static final int COLUMN_TRANSLUCENT    = 9;    // bool
  public static final int COLUMN_CLOWN          = 10;   // bool
  public static final int COLUMN_SPLIT          = 11;   // bool
  public static final int COLUMN_HELMET         = 12;   // bool
  public static final int COLUMN_WEAPON         = 13;   // bool
  public static final int COLUMN_HEIGHT         = 14;   // string
  public static final int COLUMN_HEIGHT_SHIELD  = 15;   // string
  // Column indices for PST-related sprite tables
  public static final int COLUMN_PST_RESREF     = 1;    // string
  public static final int COLUMN_PST_RESREF2    = 2;    // string
  public static final int COLUMN_PST_TYPE       = 3;    // int
  public static final int COLUMN_PST_ELLIPSE    = 4;    // int
  public static final int COLUMN_PST_SPACE      = 5;    // int
  public static final int COLUMN_PST_CLOWN      = 6;    // int
  public static final int COLUMN_PST_ARMOR      = 7;    // int
  public static final int COLUMN_PST_BESTIARY   = 8;    // int

  private static final EnumMap<Profile.Game, List<String>> TableMaps = new EnumMap<Profile.Game, List<String>>(Profile.Game.class) {{
    put(Profile.Game.BG1, Arrays.asList("avatars-bg1.2da"));
    put(Profile.Game.BG1TotSC, get(Profile.Game.BG1));

    put(Profile.Game.IWD, Arrays.asList("avatars-iwd.2da"));
    put(Profile.Game.IWDHoW, Arrays.asList("avatars-iwdhow.2da", "avatars-iwd.2da"));
    put(Profile.Game.IWDHowTotLM, get(Profile.Game.IWDHoW));

    put(Profile.Game.IWD2, Arrays.asList("avatars-iwd2.2da"));

    put(Profile.Game.PST, Arrays.asList("avatars-pst.2da"));

    put(Profile.Game.BG2SoA, Arrays.asList("avatars-bg2soa.2da"));
    put(Profile.Game.BG2ToB, Arrays.asList("avatars-bg2tob.2da", "avatars-bg2soa.2da"));
    put(Profile.Game.Tutu, get(Profile.Game.BG2ToB));
    put(Profile.Game.BGT, get(Profile.Game.BG2ToB));
    put(Profile.Game.Unknown, get(Profile.Game.BG2ToB));

    put(Profile.Game.BG1EE, Arrays.asList("avatars-bgee.2da", "avatars-bg2ee.2da", "avatars-bg2tob.2da"));
    put(Profile.Game.BG1SoD, get(Profile.Game.BG1EE));
    put(Profile.Game.BG2EE, Arrays.asList("avatars-bg2ee.2da", "avatars-bgee.2da", "avatars-bg2tob.2da"));
    put(Profile.Game.EET, Arrays.asList("avatars-eet.2da", "avatars-bgee.2da", "avatars-bg2ee.2da", "avatars-bg2tob.2da"));

    put(Profile.Game.IWDEE, Arrays.asList("avatars-iwdee.2da", "avatars-bgee.2da", "avatars-bg2ee.2da"));

    put(Profile.Game.PSTEE, Arrays.asList("avatars-pstee.2da"));
  }};


  /**
   * Creates a creature animation INI definition from the table associated with the specified animation id.
   * @param animationId the creature animation id
   * @return a list of {@link IniMap} instances containing animation information.
   *         Returns an empty {@code IniMap} list if no information could be determined.
   */
  public static List<IniMap> createIniMaps(int animationId)
  {
    return createIniMaps(Profile.getGame(), animationId);
  }

  /**
   * Creates a creature animation INI definition from the game-specific table associated with the specified animation id.
   * @param game considers only table definitions for the specified game.
   * @param animationId the creature animation id
   * @return a list of {@link IniMap} instances containing animation information.
   *         Returns {@code null} if no information could be determined.
   */
  public static List<IniMap> createIniMaps(Profile.Game game, int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    if (game == null) {
      game = Profile.getGame();
    }

    retVal.addAll(processInfinityAnimations(animationId));

    if (retVal.isEmpty()) {
      List<String> tableNames = findTables(game);
      for (final String tableName : tableNames) {
        ResourceEntry tableEntry = getTableResource(tableName);
        if (tableEntry != null) {
          Table2da table = new Table2da(tableEntry);
          if (table != null) {
            List<IniMap> inis = processTable(game, table, animationId);
            if (inis != null && !inis.isEmpty()) {
              retVal.addAll(inis);
              break;
            }
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Attempts to create a list of potential {@link IniMap} instances based on the specified arguments.
   * @param game considers only table definitions for the specified game.
   * @param table hardcoded table data used as source for generating a list of {@code IniMap} instances.
   * @param animationId the creature animation id.
   * @return a list of {@link IniMap} instances containing animation information.
   *         Returns an empty {@code IniMap} list if no information could be determined.
   */
  public static List<IniMap> processTable(Profile.Game game, Table2da table, int animationId)
  {
    List<IniMap> retVal = null;
    if (game == null || table == null) {
      return retVal;
    }

    if (game == Profile.Game.PST || game == Profile.Game.PSTEE) {
      retVal = processTablePst(table, animationId);
    } else {
      retVal = processTable(table, animationId);
    }

    return retVal;
  }

  /** Processes tables for non-PST games. */
  private static List<IniMap> processTable(Table2da table, int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    if (table == null) {
      return retVal;
    }

    // finding entry matching the specified animation id
    int rowIndex = -1;
    for (int row = 0, rowCount = table.getRowCount(); row < rowCount; row++) {
      if (valueMatchesAnimationId(table.get(row, 0), animationId)) {
        rowIndex = row;
        break;
      }
    }
    if (rowIndex < 0) {
      return retVal;
    }

    // loading all data into a String[] array
    String[] data = new String[table.getColCount()];
    String defValue = table.getDefaultValue();
    for (int col = 0, colCount = table.getColCount(); col < colCount; col++) {
      String value = table.get(rowIndex, col);
      if (value.equalsIgnoreCase(defValue)) {
        value = "";
      }
      data[col] = value;
    }

    // determining SpriteDecoder class instance
    Class<? extends SpriteDecoder> cls = SpriteUtils.getSpriteDecoderClass(animationId);
    if (cls != null) {
      try {
        // calling method of signature: public static IniMap processTableData(String[] data);
        Method method = cls.getMethod("processTableData", String[].class);
        Object o = method.invoke(null, new Object[] { data });
        if (o instanceof IniMap) {
          retVal.add((IniMap)o);
        }
      } catch (InvocationTargetException ite) {
        if (ite.getCause() != null) {
          ite.getCause().printStackTrace();
        } else {
          ite.printStackTrace();
        }
      } catch (NoSuchMethodException | IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }

  /** Processes PST-style tables. */
  private static List<IniMap> processTablePst(Table2da table, int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    if (table == null) {
      return retVal;
    }

    // finding entry matching the specified animation id
    int rowIndex = -1;
    // try special animations first
    int id = animationId;
    for (int row = 0, rowCount = table.getRowCount(); row < rowCount; row++) {
      int v = valueToInt(new String[] {table.get(row, 0)}, 0, -1);
      if (v == id) {
        rowIndex = row;
        break;
      }
    }

    // try regular animations next
    if (rowIndex < 0) {
      id = animationId & 0x0fff;
      for (int row = 0, rowCount = table.getRowCount(); row < rowCount; row++) {
        int v = valueToInt(new String[] {table.get(row, 0)}, 0, -1);
        if (v == id) {
          rowIndex = row;
          break;
        }
      }
    }

    if (rowIndex < 0) {
      // no luck :(
      return retVal;
    }

    // loading all data into a String[] array
    String[] data = new String[table.getColCount()];
    String defValue = table.getDefaultValue();
    for (int col = 0, colCount = table.getColCount(); col < colCount; col++) {
      String value = table.get(rowIndex, col);
      if (value.equalsIgnoreCase(defValue)) {
        value = "";
      }
      data[col] = value;
    }

    // delegate parsing to SpriteDecoder class
    retVal.add(MonsterPlanescapeDecoder.processTableData(data));

    return retVal;
  }

  /** Helper method for finding matching Infinity Animations entries. */
  private static List<IniMap> processInfinityAnimations(int animationId)
  {
    if (Profile.<Integer>getProperty(Profile.Key.GET_INFINITY_ANIMATIONS) > 0) {
      return InfinityTables.createIniMaps(animationId);
    }
    return new ArrayList<>();
  }


  /**
   * Convenience method: Safely retrieves an array item and returns it.
   * Returns a default value if the item could not be retrieved or is {@code null}.
   */
  public static String valueToString(String[] arr, int arrIdx, String defValue)
  {
    String retVal = defValue;
    try {
      retVal = arr[arrIdx];
      if (retVal == null) {
        retVal = defValue;
      }
    } catch (Exception e) {
    }
    return retVal;
  }

  /** Convenience method: Converts an array item into a numeric value. */
  public static int valueToInt(String[] arr, int arrIdx, int defValue)
  {
    int retVal = defValue;
    try {
      String s = arr[arrIdx];
      if (s.startsWith("0x") || s.startsWith("0X")) {
        retVal = Integer.parseInt(s.substring(2), 16);
      } else {
        retVal = Integer.parseInt(s);
      }
    } catch (NullPointerException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
    }
    return retVal;
  }

  /** Convenience method: Returns the animation id from the specified array value. */
  public static int valueToAnimationId(String[] arr, int arrIdx, int defValue)
  {
    try {
      return valueToInt(arr[arrIdx].split("_"), 0, defValue);
    } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
      return defValue;
    }
  }

  /** Convenience method: Checks if the specified animation id is covered by the id value (and mask) from the table. */
  public static boolean valueMatchesAnimationId(String value, int animationId)
  {
    boolean retVal = false;
    if (value == null) {
      return retVal;
    }

    String[] parts = value.split("_");
    if (parts.length > 0) {
      int base = valueToInt(new String[] {parts[0]}, 0, -1);
      if (base >= 0) {
        int range = 0;
        if (parts.length > 1) {
          range = valueToInt(new String[] {"0x" + parts[1]}, 0, 0);
          base &= ~range;
        }
        retVal = (animationId >= base && animationId <= (base + range));
      }
    }

    return retVal;
  }

  /**
   * Returns a list of names for tables associated with the specified game.
   * @param game The game type.
   * @return List of table names ordered by relevance. Returns an empty list if no tables could be determined.
   */
  public static List<String> findTables(Profile.Game game)
  {
    List<String> retVal = TableMaps.get(game);
    if (retVal == null) {
      retVal = new ArrayList<>();
    }
    return retVal;
  }

  /**
   * Returns a virtual {@code ResourceEntry} instance providing access to the underlying table data.
   * @param fileName The filename of the table (without path).
   * @return a {@link ResourceEntry} instance linked to the specified table.
   *         Returns {@code null} if table could not be opened.
   */
  public static ResourceEntry getTableResource(String fileName)
  {
    ResourceEntry entry = null;
    try {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream(128 * 1024)) {
        try (InputStream is = getResourceAsStream(null, fileName)) {
          byte[] buf = new byte[65536];
          int len;
          while ((len = is.read(buf)) > 0) {
            bos.write(buf, 0, len);
          }
        }
        entry = new BufferedResourceEntry(ByteBuffer.wrap(bos.toByteArray()), fileName);
      }
    } catch (Exception e) {
    }
    return entry;
  }

  /** Returns an InputStream instance for reading from the specified resource. */
  static InputStream getResourceAsStream(Class<?> c, String fileName)
  {
    InputStream retVal = null;
    if (!fileName.isEmpty()) {
      if (c == null) {
        retVal = ClassLoader.getSystemResourceAsStream(fileName);
      }
      if (retVal == null) {
        if (c == null) {
          c = SpriteTables.class;
        }
        String basePath = c.getPackage().getName().replace('.', '/');
        String separator = (fileName.charAt(0) == '/') ? "" : "/";
        retVal = ClassLoader.getSystemResourceAsStream(basePath + separator + fileName);
      }
    }
    return retVal;
  }

  private SpriteTables() { }
}
