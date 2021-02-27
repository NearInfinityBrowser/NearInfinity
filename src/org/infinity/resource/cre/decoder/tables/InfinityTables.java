// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.tables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.Profile;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.IniMap;
import org.infinity.util.Misc;

/**
 * A static class dedicated to processing Infinity Animation slots.
 */
public class InfinityTables
{
  private static final String[] TABLE_INFINITY_ANIMATIONS = { "", // not installed
                                                              "infinityanimations-v5.ids",  // IA v5 or earlier
                                                              "infinityanimations-v6.ids",  // IA v6 or later
                                                            };

  /**
   * Creates a creature animation INI definition from the Infinity Animations table.
   * @param animationId the creature animation id
   * @return a list of {@link IniMap} instances containing animation information.
   *         Returns an empty {@code IniMap} list if no information could be determined.
   */
  public static List<IniMap> createIniMaps(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();

    // determine correct IA version
    int ver = Profile.<Integer>getProperty(Profile.Key.GET_INFINITY_ANIMATIONS).intValue();
    if (ver < 1 || ver >= TABLE_INFINITY_ANIMATIONS.length) {
      return retVal;
    }

    ResourceEntry entry = SpriteTables.getTableResource(TABLE_INFINITY_ANIMATIONS[ver]);
    if (entry == null) {
      return retVal;
    }

    IdsMap table = new IdsMap(entry);
    retVal.addAll(processTable(table, animationId));

    return retVal;
  }

  /** Processes table data. */
  private static List<IniMap> processTable(IdsMap idsMap, int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    if (idsMap == null) {
      return retVal;
    }

    // finding entry matching the specified animation id
    animationId &= 0xffff;
    IdsMapEntry idsEntry = idsMap.get(animationId);
    if (idsEntry == null) {
      return retVal;
    }

    try {
      // transforming IA table entry into internal table entry
      String[] data = parseTableEntry(animationId, idsEntry.getSymbol());

      // determining SpriteDecoder class instance
      int classType = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TYPE, -1);
      SpriteDecoder.AnimationType animType = SpriteDecoder.AnimationType.values()[classType];
      Class<? extends SpriteDecoder> cls = SpriteDecoder.getSpriteDecoderClass(animType);
      if (cls != null) {
        // calling method of signature: public static IniMap processTableData(String[] data);
        Method method = cls.getMethod("processTableData", String[].class);
        Object o = method.invoke(null, new Object[] { data });
        if (o instanceof IniMap) {
          retVal.add((IniMap)o);
        }
      }
    } catch (InvocationTargetException ite) {
      if (ite.getCause() != null) {
        ite.getCause().printStackTrace();
      } else {
        ite.printStackTrace();
      }
    } catch (Exception e) {
      return retVal;
    }



    return retVal;
  }

  private static String[] parseTableEntry(int animationId, String entry) throws Exception
  {
    final String[] retVal = new String[SpriteTables.NUM_COLUMNS];

    String[] items = Objects.requireNonNull(entry).split("\\s+");
    Misc.requireCondition(items.length > 5, "Infinity Animations table entry: too few entries");

    String prefix = items[0].trim();
    Misc.requireCondition(prefix.length() > 0, "Animation prefix not available");

    int space = Misc.toNumber(SpriteTables.valueToString(items, 3, "3"), 16, 3);
    Misc.requireCondition(space > 0, "Invalid personal space: " + space);
    int ellipse;
    if (space <= 3) {
      ellipse = 16;
    } else if (space <= 5) {
      ellipse = 24;
    } else if (space <= 10) {
      ellipse = 64;
    } else {
      ellipse = 72;
    }

//    char type = SpriteTables.valueToString(items, 4, " ").charAt(0);

    final HashMap<String, SpriteDecoder.AnimationType> animationTypes = new HashMap<String, SpriteDecoder.AnimationType>() {{
      put("BGI MONSTER LONG 4 PART", SpriteDecoder.AnimationType.MONSTER_QUADRANT);
      put("DRAGONS", SpriteDecoder.AnimationType.MONSTER_MULTI);
      put("BGII SPLIT 4 PART", SpriteDecoder.AnimationType.MONSTER_MULTI_NEW);
      put("BGI SIMPLE CASTER", SpriteDecoder.AnimationType.MONSTER_LAYERED_SPELL);
      put("BROKEN ANKHEG", SpriteDecoder.AnimationType.MONSTER_ANKHEG);
      put("CHARACTER BGII", SpriteDecoder.AnimationType.CHARACTER);
      put("CHARACTER BGI", SpriteDecoder.AnimationType.CHARACTER_OLD);
      put("BGII UNSPLIT EXT.", SpriteDecoder.AnimationType.MONSTER);
      put("BGII SPLIT", SpriteDecoder.AnimationType.MONSTER);
      put("BGI SIMPLE MONSTER", SpriteDecoder.AnimationType.MONSTER_OLD);
      put("BGI MONSTER LONG", SpriteDecoder.AnimationType.MONSTER_LARGE_16);
      put("IWD", SpriteDecoder.AnimationType.MONSTER_ICEWIND);
    }};

    String typeString = null;
    SpriteDecoder.AnimationType animType = null;
    for (int len = items.length - 5; len > 0 && animType == null; len--) {
      typeString = concatItems(items, 5, len);
      animType = animationTypes.get(typeString);
    }

    int type = -1;
    for (int i = SpriteDecoder.AnimationType.values().length - 1; i >= 0; i--) {
      if (SpriteDecoder.AnimationType.values()[i] == animType) {
        type = i;
        break;
      }
    }

    if (type < 0) {
      throw new Exception("Could not determine animation type");
    }

    int clown = SpriteTables.valueToString(items, items.length - 1, "unpaletted").equalsIgnoreCase("paletted") ? 1 : 0;

    int split = -1;
    String height = "";
    String heightShield = "";
    switch (animType) {
      case MONSTER_MULTI:
      case MONSTER_MULTI_NEW:
        split = 1;
        break;
      case MONSTER_LAYERED_SPELL:
      {
        String s = SpriteTables.valueToString(items, 8, "");
        if ("(BOW)".equalsIgnoreCase(s)) {
          heightShield = "BW";
        } else if ("(S1)".equalsIgnoreCase(s)) {
          height = "S1";
        }
        break;
      }
      case CHARACTER:
      case CHARACTER_OLD:
      {
        split = 0;
        String s = SpriteTables.valueToString(items, 7, "");
        if (s.length() == 3) {
          // height code specified
          height = s;
        } else {
          // height code derived from prefix
          height = prefix.substring(1);
        }
        break;
      }
      case MONSTER:
      {
        switch (typeString) {
          case "BGII UNSPLIT EXT.":
            split = 0;
            break;
          case "BGII SPLIT":
            split = 1;
            break;
        }
        break;
      }
      default:
    }

    Arrays.fill(retVal, "");
    retVal[SpriteTables.COLUMN_ID] = String.format("0x%04x", animationId);
    retVal[SpriteTables.COLUMN_RESREF] = prefix;
    retVal[SpriteTables.COLUMN_TYPE] = Integer.toString(type);
    retVal[SpriteTables.COLUMN_ELLIPSE] = Integer.toString(ellipse);
    retVal[SpriteTables.COLUMN_SPACE] = Integer.toString(space);
    retVal[SpriteTables.COLUMN_BLENDING] = "0";
    if (clown >= 0) {
      retVal[SpriteTables.COLUMN_CLOWN] = Integer.toString(clown);
    }
    if (split >= 0) {
      retVal[SpriteTables.COLUMN_SPLIT] = Integer.toString(split);
    }
    retVal[SpriteTables.COLUMN_HEIGHT] = height;
    retVal[SpriteTables.COLUMN_HEIGHT_SHIELD] = heightShield;

    return retVal;
  }

  /** Concatenates the specified strings to a single string, separated by a single space. */
  private static String concatItems(String[] data, int idx, int len)
  {
    StringBuilder sb = new StringBuilder();
    if (data == null || idx < 0 || idx >= data.length || len <= 0) {
      return sb.toString();
    }

    if (data[idx] != null) {
      sb.append(data[idx]);
    }

    for (int i = 1; i < len; i++) {
      if (data[idx + i] != null) {
        sb.append(' ').append(data[idx + i]);
      }
    }

    return sb.toString();
  }

  private InfinityTables() { }
}
