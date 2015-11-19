// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.HashMap;

import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.IdsMap;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.Table2da;
import infinity.util.Table2daCache;

/**
 * Specialized BitmapEx type for translating SPLPROT.2DA data into human-readable descriptions.
 */
public class SpellProtBitmap extends Bitmap
{
  private static final String tableName = "SPLPROT.2DA";
  public static final String[] relation = { "<=", "=", "<", ">", ">=", "!=",
                                            "bit_l_e", "bit_g_e", "bit_eq",
                                            "bit_uneq", "bit_greater", "bit_less" };
  private static final HashMap<Integer, String> statIds = new HashMap<Integer, String>();
  private static String[] creType;

  static {
    statIds.put(152, "KIT.IDS");
    statIds.put(0x106, "AREATYPE.IDS");
//    statIds.put(0x107, "TIMEODAY.IDS");
    statIds.put(0x10a, "EA.IDS");
    statIds.put(0x10b, "GENERAL.IDS");
    statIds.put(0x10c, "RACE.IDS");
    statIds.put(0x10d, "CLASS.IDS");
    statIds.put(0x10e, "SPECIFIC.IDS");
    statIds.put(0x10f, "GENDER.IDS");
    statIds.put(0x110, "ALIGNMEN.IDS");
    statIds.put(0x111, "STATE.IDS");
    statIds.put(0x112, "SPLSTATE.IDS");
  }

  public SpellProtBitmap(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public SpellProtBitmap(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, getTypeTable());
  }

  /** Returns name of the 2DA resource used as reference for the list. */
  public static String getTableName()
  {
    return tableName;
  }

  /** Returns true if the current creature type value depends on a user-defined value. */
  public boolean useCustomValue()
  {
    if (ResourceFactory.resourceExists(tableName)) {
      Table2da table = Table2daCache.get(tableName);
      if (table != null) {
        return (-1 == toNumber(table.get(getValue(), 2), 0));
      }
    }
    return false;
  }

  /** Returns the IDS resource name used by the current creature type. May return empty string if unused. */
  public String getIdsFile()
  {
    if (ResourceFactory.resourceExists(tableName)) {
      Table2da table = Table2daCache.get(tableName);
      if (table != null) {
        String retVal = statIds.get(toNumber(table.get(getValue(), 1), -1));
        if (retVal != null) {
          return retVal;
        }
      }
    }
    return "";
  }

  /** Returns an array of descriptions based on the entries in the 2DA resource. */
  public static String[] getTypeTable()
  {
    if (creType == null) {
      if (ResourceFactory.resourceExists(tableName)) {
        Table2da table = Table2daCache.get(tableName);
        if (table != null) {
          creType = new String[table.getRowCount()];
          for (int row = 0, size = table.getRowCount(); row < size; row++) {
            String label;
            int stat = toNumber(table.get(row, 1), -1);
            int value = toNumber(table.get(row, 2), -1);
            int rel = toNumber(table.get(row, 3), -1);
            switch (stat) {
              case 0x100: // source equals target
                label = "Source";
                break;
              case 0x101: // source is not target
                label = "Not source";
                break;
              case 0x102: // circle size
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("Circle size %1$s %2$d [0x%3$x]", getRelation(rel), value, value);
                } else {
                  label = String.format("Circle size %1$s %2$d", getRelation(rel), value);
                }
                break;
              case 0x103: // use two rows of splprot.2da
                label = String.format("Match entries %1$d or %2$d", value, rel);
                break;
              case 0x104: // negate 0x103
                label = String.format("Not match entries %1$d or %2$d", value, rel);
                break;
              case 0x105: // source and target morale match
                switch (rel) {
                  case 1: label = "Same morale alignment"; break;
                  case 5: label = "Not same morale alignment"; break;
                  default: label = "Morale alignment relation: " + getRelation(rel); break;
                }
                break;
              case 0x106: // areatype (like outdoors, forest, etc)
                if (isBitwiseRelation(rel) && value != -1) {
                } else {
                  label = String.format("AREATYPE %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("AREATYPE.IDS", value, isBitwiseRelation(rel)),
                                        value);
                }
                  label = String.format("AREATYPE %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("AREATYPE.IDS", value, isBitwiseRelation(rel)));
                break;
              case 0x107: // daytime
                // TODO: confirm
                label = (value == 0) ? "Not daytime" : "Daytime";
                break;
              case 0x108: // source and target ethical match
                switch (rel) {
                  case 1: label = "Same ethical alignment"; break;
                  case 5: label = "Not same ethical alignment"; break;
                  default: label = "Ethical alignment: " + getRelation(rel); break;
                }
                break;
              case 0x109: // evasion
                label = "Evasion check";
                break;
              case 0x10a: // EA
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("EA %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("EA.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("EA %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("EA.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x10b: // GENERAL
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("GENERAL %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("GENERAL.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("GENERAL %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("GENERAL.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x10c: // RACE
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("RACE %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("RACE.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("RACE %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("RACE.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x10d: // CLASS
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("CLASS %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("CLASS.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("CLASS %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("CLASS.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x10e: // SPECIFIC
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("SPECIFIC %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("SPECIFIC.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("SPECIFIC %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("SPECIFIC.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x10f: // GENDER
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("GENDER %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("GENDER.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("GENDER %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("GENDER.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x110: // ALIGNMENT
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("ALIGNMENT %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("ALIGNMEN.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("ALIGNMENT %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("ALIGNMEN.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x111: // STATE
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("STATE %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("STATE.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("STATE %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("STATE.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x112: // SPELL STATE
                if (isBitwiseRelation(rel) && value != -1) {
                  label = String.format("SPLSTATE %1$s %2$s [0x%3$x]",
                                        getRelation(rel),
                                        getIdsValue("SPLSTATE.IDS", value, isBitwiseRelation(rel)),
                                        value);
                } else {
                  label = String.format("SPLSTATE %1$s %2$s",
                                        getRelation(rel),
                                        getIdsValue("SPLSTATE.IDS", value, isBitwiseRelation(rel)));
                }
                break;
              case 0x113: // source and target allies
                switch (rel) {
                  case 1: label = "Allies"; break;
                  case 5: label = "Not allies"; break;
                  default: label = "Allies match: " + getRelation(rel); break;
                }
                break;
              case 0x114: // source and target enemies
                switch (rel) {
                  case 1: label = "Enemies"; break;
                  case 5: label = "Not enemies"; break;
                  default: label = "Enemies match: " + getRelation(rel); break;
                }
                break;
              default:    // use values from STATS.IDS
                if (stat >= 0 && stat < 0x100) {
                  // valid stat
                  if (value == -1) {
                    label = String.format("STAT %1$s %2$s specified value",
                                          getIdsValue("STATS.IDS", stat, isBitwiseRelation(rel)),
                                          getRelation(rel));
                  } else {
                    if (isBitwiseRelation(rel)) {
                      label = String.format("STAT %1$s %2$s %3$x",
                                            getIdsValue("STATS.IDS", stat, isBitwiseRelation(rel)),
                                            getRelation(rel), value);
                    } else {
                      label = String.format("STAT %1$s %2$s %3$d",
                                            getIdsValue("STATS.IDS", stat, isBitwiseRelation(rel)),
                                            getRelation(rel), value);
                    }
                  }
                } else {
                  label = "Undefined";
                }
                break;
            }
            creType[row] = label;
          }
        }
      }
    }
    return creType;
  }

  public static void resetTypeTable()
  {
    creType = null;
    Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(tableName));
  }

  // Attempts to convert the specified string into a number. Returns the default value on error.
  private static int toNumber(String value, int defValue)
  {
    int retVal = defValue;
    if (value != null && !value.isEmpty()) {
      try {
        if (value.toLowerCase().startsWith("0x")) {
          retVal = Integer.parseInt(value.substring(2), 16);
        } else {
          retVal = Integer.parseInt(value);
        }
      } catch (NumberFormatException e) {
      }
    }
    return retVal;
  }

  // Returns the IDS symbol based on the specified arguments.
  // Returns a decimal or hexadecimal number as string if symbol not found.
  private static String getIdsValue(String idsFile, int value, boolean asHex)
  {
    if (value == -1) {
      return "specified value";
    } else {
      IdsMap map = IdsMapCache.get(idsFile);
      if (map != null) {
        IdsMapEntry entry = map.getValue((long)value);
        if (entry != null) {
          if (entry.getString() != null && !entry.getString().isEmpty()) {
            return entry.getString();
          }
        }
      }
      return asHex ? "0x" + Integer.toHexString(value) : Integer.toString(value);
    }
  }

  // Returns a textual representation of the specified relation code
  private static String getRelation(int rel)
  {
    return (rel >= 0 && rel < relation.length) ? relation[rel] : "";
  }

  // Returns whether specified code indicates a binary operator
  private static boolean isBitwiseRelation(int rel)
  {
    return (rel >= 6 && rel <= 11);
  }
}
