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
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/**
 * Specialized {@link HashBitmap} that uses a mix of hardcoded entries and custom entries from ITEMTYPE.2DA
 * if available.
 */
public class ItemTypeBitmap extends HashBitmap {
  private static final String TABLE_NAME = "ITEMTYPE.2DA";

  public static final String[] CATEGORIES_ARRAY = { "Miscellaneous", "Amulets and necklaces", "Armor",
      "Belts and girdles", "Boots", "Arrows", "Bracers and gauntlets", "Headgear", "Keys", "Potions", "Rings",
      "Scrolls", "Shields", "Food", "Bullets", "Bows", "Daggers", "Maces", "Slings", "Small swords", "Large swords",
      "Hammers", "Morning stars", "Flails", "Darts", "Axes", "Quarterstaves", "Crossbows", "Hand-to-hand weapons",
      "Spears", "Halberds", "Bolts", "Cloaks and robes", "Gold pieces", "Gems", "Wands", "Containers", "Books",
      "Familiars", "Tattoos", "Lenses", "Bucklers", "Candles", "Child bodies", "Clubs", "Female bodies", "Keys (old)",
      "Large shields", "Male bodies", "Medium shields", "Notes", "Rods", "Skulls", "Small shields", "Spider bodies",
      "Telescopes", "Bottles", "Greatswords", "Bags", "Furs and pelts", "Leather armor", "Studded leather",
      "Chain mail", "Splint mail", "Plate mail", "Full plate", "Hide armor", "Robes", "Scale mail", "Bastard swords",
      "Scarves", "Rations", "Hats", "Gloves", "Eyeballs", "Earrings", "Teeth", "Bracelets" };

  public static final String[] CATEGORIES11_ARRAY = { "Miscellaneous", "Amulets and necklaces", "Armor",
      "Belts and girdles", "Boots", "Arrows", "Bracers and gauntlets", "Headgear", "Keys", "Potions", "Rings",
      "Scrolls", "Shields", "Spells", "Bullets", "Bows", "Daggers", "Maces", "Slings", "Small swords", "Large swords",
      "Hammers", "Morning stars", "Flails", "Darts", "Axes", "Quarterstaves", "Crossbows", "Hand-to-hand weapons",
      "Greatswords", "Halberds", "Bolts", "Cloaks and robes", "Copper commons", "Gems", "Wands", "Eyeballs",
      "Bracelets", "Earrings", "Tattoos", "Lenses", "Teeth" };

  /** Category order map for non-PST item categories. */
  public static final int[] SUGGESTED_CATEGORY_ORDER;

  /** Category order map for PST item categories. */
  public static final int[] SUGGESTED_CATEGORY_ORDER_PST;

  // Creating inverse category index maps
  static {
    // non-PST item categories
    final int[] items = {
     // Weapons
        25, 44, 17, 23, 22, 15, 16, 24, 30, 21, 18, 29, 26, 57, 69, 20, 19, 28, 27,
        // Ammo
        14, 31, 5,
        // Armor
        2, 60, 61, 66, 62, 68, 63, 64, 65, 67, 32, 12, 41, 53, 49, 47, 7, 72, 6, 70, 73, 3, 4,
        // Jewelry
        10, 1,
        // Potions, scrolls, books, containers, ...
        9, 56, 11, 37, 50, 35, 51, 36, 58,
        // Misc. stuff
        0, 13, 71, 34, 39, 55, 59, 74, 75, 76, 77,
        // uncategorized
        8, 33, 38, 40, 42, 43, 45, 46, 48, 52, 54,
    };
    SUGGESTED_CATEGORY_ORDER = new int[items.length];
    for (int i = 0; i < items.length; i++) {
      SUGGESTED_CATEGORY_ORDER[items[i]] = i;
    }

    // PST item categories
    final int[] items11 = {
        // Weapons
        25, 17, 23, 22, 15, 16, 24, 30, 21, 18, 26, 29, 20, 19, 28, 27,
        // Ammo
        14, 31, 5,
        // Armor
        2, 32, 12, 7, 6, 3, 4, 36, 38, 40, 41,
        // Misc and jewelry
        0, 1, 37, 10,
        // Potions, scrolls, wands, tattoos, ...
        9, 11, 13, 35, 39,
        // uncategorized
        34, 8, 33,
    };
    SUGGESTED_CATEGORY_ORDER_PST = new int[items11.length];
    for (int i = 0; i < items11.length; i++) {
      SUGGESTED_CATEGORY_ORDER_PST[items11[i]] = i;
    }
  }

  private static TreeMap<Long, String> CATEGORIES = null;

  public ItemTypeBitmap(ByteBuffer buffer, int offset, int length, String name) {
    super(buffer, offset, length, name, getItemCategories(), false, false);
  }

  /**
   * Returns a list of available item categories. List entries depend on the detected game and may include
   * static and dynamic elements.
   *
   * @return Map of strings with item categories.
   */
  public static TreeMap<Long, String> getItemCategories() {
    synchronized (TABLE_NAME) {
      if (Profile.isEnhancedEdition() && !Table2daCache.isCached(TABLE_NAME)) {
        CATEGORIES = null;
      }
      if (CATEGORIES == null) {
        CATEGORIES = buildCategories();
      }
    }
    return CATEGORIES;
  }

  /** Rebuilds the list of item categories. */
  private static TreeMap<Long, String> buildCategories() {
    final TreeMap<Long, String> retVal = new TreeMap<>();
    if (Profile.isEnhancedEdition() && ResourceFactory.resourceExists(TABLE_NAME)) {
      final IdsMap slots = IdsMapCache.get("SLOTS.IDS");
      final Table2da table = Table2daCache.get(TABLE_NAME);
      final String baseName = "Extra category ";
      int baseIndex = 1;
      for (int row = 0, count = table.getRowCount(); row < count; row++) {
        final String idxValue = table.get(row, 0);
        final int radix = idxValue.startsWith("0x") ? 16 : 10;
        String catName = "";
        try {
          int idx = Integer.parseInt(idxValue, radix);
          if (idx >= 0 && idx < CATEGORIES_ARRAY.length) {
            // looking up hardcoded category name
            catName = CATEGORIES_ARRAY[idx];
          } else {
            // generating custom category name
            catName = baseName + baseIndex;
            baseIndex++;
          }

          // adding slot restriction if available
          int slot = Misc.toNumber(table.get(row, 3), -1);
          if (slot >= 0) {
            final IdsMapEntry slotEntry = slots.get(slot);
            if (slotEntry != null) {
              final String slotName = beautifyString(slotEntry.getSymbol(), "SLOT");
              if (slotName != null && !slotName.isEmpty()) {
                catName = catName + " [" + slotName + " slot" + "]";
              }
            }
          }

          retVal.put((long) idx, catName);
        } catch (NumberFormatException e) {
          // skip entry with log message
          Logger.warn("{}: Invalid index at row={} (value={})", TABLE_NAME, row, idxValue);
        }
      }
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      // PST
      for (long idx = 0, count = CATEGORIES11_ARRAY.length; idx < count; idx++) {
        retVal.put(idx, CATEGORIES11_ARRAY[(int) idx]);
      }
    } else {
      // Any non-EE games except PST
      for (long idx = 0, count = CATEGORIES_ARRAY.length; idx < count; idx++) {
        retVal.put(idx, CATEGORIES_ARRAY[(int) idx]);
      }
    }

    return retVal;
  }

  private static String beautifyString(String s, String removedPrefix) {
    String retVal = s;
    if (retVal != null) {
      retVal = retVal.replaceFirst(removedPrefix + "_?", "");
      final String[] words = retVal.split("[ _]+");
      for (int i = 0; i < words.length; i++) {
        words[i] = words[i].charAt(0) + words[i].substring(1).toLowerCase(Locale.ENGLISH);
      }
      retVal = String.join(" ", words);
      retVal = retVal.replaceFirst("(\\D)(\\d)", "$1 $2");
      retVal = retVal.trim();
    }
    return retVal;
  }
}
