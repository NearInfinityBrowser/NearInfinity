// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.infinity.util.io.StreamUtils;

/**
 * Provides useful information about equippable items.
 */
public class ItemInfo implements Comparable<ItemInfo>
{
  /** Generalized categories based on equipment slots. */
  public enum SlotType {
    /** Helmet slot (PST: Right earring/lens/helmet) */
    HELMET,
    /** Armor slot */
    ARMOR,
    /** Shield slot (PST: left tattoo) */
    SHIELD,
    /** Gloves slot (PST: hand) */
    GLOVES,
    /** Left/right ring slot */
    RING,
    /** Amulet slot (PST: Left earring/eyeball) */
    AMULET,
    /** Belt slot (PST: Right lower tattoo) */
    BELT,
    /** Boots slot */
    BOOTS,
    /** Weapon slots */
    WEAPON,
    /** Quiver slots */
    QUIVER,
    /** Cloak slot (PST: Right upper tattoo) */
    CLOAK,
    /** (PST only) Covers the following slots: {@code SHIELD}, {@code AMULET}, {@code CLOAK} */
    TATTOO,
    /** (PST only) Covers the following slots: {@code HELMET}, {@code AMULET} */
    EARRING,
    /** Quick item slot */
    QUICK_ITEM,
    /** Generic inventory slot (default slot if none other are matching) */
    INVENTORY,
  }

  /**
   * This predicate simply returns {@code false} for all items.
   */
  public static final ItemPredicate FILTER_NONE = (info) -> { return false; };

  /**
   * This predicate simply returns {@code true} for all items.
   */
  public static final ItemPredicate FILTER_ALL = (info) -> { return true; };

  /**
   * This predicate returns {@code true} only if the item can be equipped in an equipment slot (except item slots).
   */
  public static final ItemPredicate FILTER_EQUIPPABLE = (info) -> {
    if ((info.getFlags() & (1 << 2)) == 0) {
      // cleared bit 2 (droppable) indicates "meta-equipment"
      return true;
    }
    switch (info.getCategory()) {
      case 1:   // Amulets
      case 2:   // Armor
      case 3:   // Belts
      case 4:   // Boots
      case 5:   // Arrows
      case 6:   // Bracers
      case 7:   // Headgear
      case 10:  // Rings
      case 12:  // Shields
      case 14:  // Bullets
      case 15:  // Bows
      case 16:  // Daggers
      case 17:  // Maces
      case 18:  // Slings
      case 19:  // Small swords
      case 20:  // Large swords
      case 21:  // Hammers
      case 22:  // Morning stars
      case 23:  // Flails
      case 24:  // Darts
      case 25:  // Axes
      case 26:  // Quarterstaves
      case 27:  // Crossbows
      case 28:  // Hand-to-hand weapons
      case 29:  // Spears
      case 30:  // Halberds
      case 31:  // Bolts
      case 32:  // Cloaks/Robes
      case 39:  // Tattoos
      case 41:  // Bucklers
      case 44:  // Clubs
      case 47:  // Large shields
      case 49:  // Medium shields
      case 53:  // Small shields
      case 57:  // Greatswords
      case 60:  // Leather armor
      case 61:  // Studded leather
      case 62:  // Chain mail
      case 63:  // Splint mail
      case 64:  // Plate mail
      case 65:  // Full plate
      case 66:  // Hide armor
      case 67:  // Robes
      case 68:  // Scale mail
      case 69:  // Bastard swords
      case 70:  // Scarves
      case 72:  // Hats
      case 73:  // Gloves
      case 74:  // Eyeballs
      case 75:  // Earrings
      case 76:  // Teeth
      case 77:  // Bracelets
        return true;
      default:
        return false;
    }
  };

  /**
   * This predicate returns {@code true} only if the item can be equipped in a weapon slot.
   */
  public static final ItemPredicate FILTER_WEAPON = (info) -> {
    boolean retVal = FILTER_EQUIPPABLE.test(info);
    retVal &= (info.getAbilityCount() > 0);
    if (retVal) {
      retVal &= (info.getAbility(0).getLocation() == 1);  // weapon slot
      retVal &= (info.getAbility(0).getLauncher() == 0);  // no launcher required
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} if the item is a two-handed weapon (melee or ranged).
   */
  public static final ItemPredicate FILTER_WEAPON_2H = (info) -> {
    boolean retVal = FILTER_WEAPON.test(info);
    retVal &= (info.getAbilityCount() > 0);
    if (retVal) {
      int mask = Profile.isEnhancedEdition() ? 0x1002 : 0x2;  // two-handed, fake two-handed
      retVal &= (info.getFlags() & mask) != 0;
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if the item can be placed in a weapon slot and the default ability
   * is defined as melee type.
   */
  public static final ItemPredicate FILTER_WEAPON_MELEE = (info) -> {
    boolean retVal = FILTER_WEAPON.test(info);
    retVal &= (info.getAbilityCount() > 0);
    if (retVal) {
      AbilityEntry ai = info.getAbility(0);
      retVal = (ai.getAbilityType() == 1) &&
               (ai.getLauncher() == 0);
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if {@link #FILTER_WEAPON_MELEE} passes the test and the item is flagged
   * as two-handed or fake two-handed (e.g. monk fists).
   */
  public static final ItemPredicate FILTER_WEAPON_MELEE_2H = (info) -> {
    boolean retVal = FILTER_WEAPON_MELEE.test(info);
    if (retVal) {
      int mask = Profile.isEnhancedEdition() ? 0x1002 : 0x2;
      retVal &= (info.getFlags() & mask) != 0;
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if {@link #FILTER_WEAPON_MELEE} passes the test and the item can be
   * equipped in the shield slot.
   */
  public static final ItemPredicate FILTER_WEAPON_MELEE_LEFT_HANDED = (info) -> {
    boolean retVal = FILTER_WEAPON_MELEE.test(info);
    if (retVal) {
      boolean isTwoHanded = (info.getFlags() & 2) != 0;
      boolean allowLeftHanded = !Profile.isEnhancedEdition() || ((info.getFlags() & (1 << 13)) == 0);
      retVal = !isTwoHanded && allowLeftHanded;
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if the item can be placed in a weapon slot and the default ability
   * is defined as ranged or launcher type.
   */
  public static final ItemPredicate FILTER_WEAPON_RANGED = (info) -> {
    boolean retVal = FILTER_WEAPON.test(info);
    retVal &= (info.getAbilityCount() > 0);
    if (retVal) {
      AbilityEntry ai = info.getAbility(0);
      retVal &= (ai.getLauncher() == 0);
      retVal &= (ai.getAbilityType() == 2) || (ai.getAbilityType() == 4);
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if the item can be placed in a weapon slot and is a
   * ranged launcher-type weapon.
   */
  public static final ItemPredicate FILTER_WEAPON_RANGED_LAUNCHER = (info) -> {
    boolean retVal = FILTER_WEAPON.test(info);
    retVal &= (info.getAbilityCount() > 0);
    if (retVal) {
      AbilityEntry ai = info.getAbility(0);
      retVal &= (ai.getLauncher() == 0);
      retVal &= (ai.getAbilityType() == 4);
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if the item is a shield that can be placed in the shield slot.
   */
  public static final ItemPredicate FILTER_SHIELD = (info) -> {
    boolean retVal = FILTER_EQUIPPABLE.test(info);
    if (retVal) {
      switch (info.getCategory()) {
        case 12:  // Shields
        case 41:  // Bucklers
        case 47:  // Large shields
        case 49:  // Medium shields
        case 53:  // Small shields
          break;
        default:
          retVal = false;
      }
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if the item can be placed in the armor slot.
   */
  public static final ItemPredicate FILTER_ARMOR = (info) -> {
    boolean retVal = FILTER_EQUIPPABLE.test(info);
    if (retVal) {
      switch (info.getCategory()) {
        case 2:   // Armor
        case 60:  // Leather armor
        case 61:  // Studded leather
        case 62:  // Chain mail
        case 63:  // Split mail
        case 64:  // Plate mail
        case 65:  // Full plate
        case 66:  // Hide armor
        case 67:  // Robes
        case 68:  // Scale mail
          break;
        default:
          retVal = false;
      }
    }
    return retVal;
  };

  /**
   * This predicate returns {@code true} only if the item can be placed in the helmet slot.
   */
  public static final ItemPredicate FILTER_HELMET = (info) -> {
    boolean retVal = FILTER_EQUIPPABLE.test(info);
    if (retVal) {
      switch (info.getCategory()) {
        case 7:   // Headgear
        case 72:  // Hats
          break;
        default:
          retVal = false;
      }
    }
    return retVal;
  };

  /** Predefined {@code ItemInfo} structure without associated ITM resource. */
  public static final ItemInfo EMPTY = new ItemInfo();

  private static final HashMap<ResourceEntry, ItemInfo> ITEM_CACHE = new HashMap<>();

  private final EffectInfo effectInfo = new EffectInfo();
  private final List<AbilityEntry> abilityEntries = new ArrayList<>();
  private final List<EffectEntry> effectsEntries = new ArrayList<>();
  private final ResourceEntry itmEntry;

  private String name;
  private String nameIdentified;
  private int flags;
  private int category;
  private int unusable;
  private int unusableKits;
  private String appearance;
  private int proficiency;
  private int stack;
  private int enchantment;

  /**
   * Returns the {@code ItemInfo} structure based on the specified item resource. Entries are retrieved from cache
   * for improved performance.
   * @param itmEntry the ITM {@code ResourceEntry}
   * @return the {@code ItemInfo} structure with relevant item details.
   * @throws Exception if the ITM resource could not be loaded.
   */
  public static ItemInfo get(ResourceEntry itmEntry) throws Exception
  {
    ItemInfo retVal = null;
    if (itmEntry == null) {
      return EMPTY;
    }
    synchronized (ITEM_CACHE) {
      retVal = ITEM_CACHE.get(itmEntry);
      if (retVal == null) {
        retVal = new ItemInfo(itmEntry);
        ITEM_CACHE.put(itmEntry, retVal);
      }
    }
    return retVal;
  }

  /**
   * Functions the same as {@link #get(ResourceEntry)} excepts that available cache entries will be updated with the
   * new item data.
   * @param itmEntry the ITM {@code ResourceEntry}
   * @return the {@code ItemInfo} structure with relevant item details.
   * @throws Exception if the ITM resource could not be loaded.
   */
  public static ItemInfo getValidated(ResourceEntry itmEntry) throws Exception
  {
    ItemInfo retVal = null;
    if (itmEntry == null) {
      return EMPTY;
    }
    synchronized (ITEM_CACHE) {
      retVal = new ItemInfo(itmEntry);
      ITEM_CACHE.put(itmEntry, retVal);
    }
    return retVal;
  }

  /** Clears the item cache. */
  public static void clearCache()
  {
    synchronized (ITEM_CACHE) {
      ITEM_CACHE.clear();
    }
  }

  /**
   * Returns an {@code ItemInfo} list filtered by the specified predicate.
   * @param pred the predicate used to decide whether an item is included in the returned list.
   *             Specify {@code null} to return all available items.
   * @param sorted whether the returned list is sorted by {@code ResourceEntry} in ascending order.
   * @return list of matching {@code ItemInfo} structures.
   */
  public static List<ItemInfo> getItemList(ItemPredicate pred, boolean sorted)
  {
    List<ItemInfo> retVal = new ArrayList<>();

    if (pred == null) {
      pred = i -> true;
    }

    List<ResourceEntry> entries = ResourceFactory.getResources("ITM");
    for (Iterator<ResourceEntry> iter = entries.iterator(); iter.hasNext(); ) {
      try {
        final ItemInfo ii = ItemInfo.get(iter.next());
        if (pred.test(ii)) {
          retVal.add(ii);
        }
      } catch (Exception e) {
      }
    }

    if (sorted) {
      Collections.sort(retVal);
    }

    return retVal;
  }

  /**
   * Convenience method: Returns {@code true} if the given item passes all the specified tests.
   * Returns {@code false} if info is {@code null}.
   * Returns {@code true} if no predicate is specified.
   */
  public static boolean testAll(ItemInfo info, ItemPredicate... pred)
  {
    boolean retVal = (info != null);
    if (retVal) {
      for (final ItemPredicate p : pred) {
        if (p != null) {
          retVal &= p.test(info);
        }
        if (!retVal) {
          break;
        }
      }
    }
    return retVal;
  }

  /**
   * Convenience method: Returns {@code true} if the given item passes at least one of the specified tests.
   * Returns {@code false} if info is {@code null}.
   * Returns {@code true} if no predicate is specified.
   */
  public static boolean testAny(ItemInfo info, ItemPredicate... pred)
  {
    boolean retVal = (info != null);
    if (retVal && pred.length > 0) {
      for (final ItemPredicate p : pred) {
        if (p != null) {
          retVal |= p.test(info);
        }
        if (retVal) {
          break;
        }
      }
    }
    return retVal;
  }

  /**
   * This is a convenience method to speed up the process.
   * Returns the category of the specified item resource. Returns -1 if category could not be determined.
   */
  public static int getItemCategory(ResourceEntry itmEntry)
  {
    if (itmEntry != null) {
      try (final InputStream is = itmEntry.getResourceDataAsStream()) {
        byte[] sig = new byte[8];
        Misc.requireCondition(is.read(sig) == 8, "Could not read signature field");
        final String signature = new String(sig).toUpperCase(Locale.ENGLISH);
        switch (signature) {
          case "ITM V1  ":
          case "ITM V1.1":
          case "ITM V2.0":
            break;
          default:
            throw new Exception("Not an item resource: " + itmEntry.getResourceName());
        }
        Misc.requireCondition(is.skip(0x14) == 0x14, "Could not advance in data stream");
        return StreamUtils.readShort(is);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return -1;
  }

  private ItemInfo()
  {
    this.itmEntry = null;
    initDefault();
  }

  private ItemInfo(ResourceEntry itmEntry) throws Exception
  {
    this.itmEntry = itmEntry;
    init();
  }

  /** Returns whether this {@code ItemInfo} instance contains a valid item. */
  public boolean isEmpty() { return (itmEntry == null); }

  /** Returns the {@code ResourceEntry} instance of the item resource. */
  public ResourceEntry getResourceEntry() { return itmEntry; }

  /** Returns the general name of the item. */
  public String getName() { return name; }

  /** Returns the identified name of the item. */
  public String getIdentifiedName() { return nameIdentified; }

  /** Returns the item flags. */
  public int getFlags() { return flags; }

  /**
   * Updates the undroppable flag (bit 2) of the item. The undroppable flag can be overridden by CRE item structures.
   * @param override specify whether droppable flag should be overridden.
   */
  public void overrideDroppableFlag(boolean override)
  {
    if (override) {
      flags &= ~(1 << 2);   // Note: item flags specify "droppable" bit
    }
  }

  /** Returns the item category. */
  public int getCategory() { return category; }

  /** Returns the general usability flags. */
  public int getUnusable() { return unusable; }

  /** Returns the kit-specific usability flags (packed value, kits #1 at bit 24, ..., kits #4 at bit 0). */
  public int getUnusableKits() { return unusableKits; }

  /** Returns the two-letter appearance code. */
  public String getAppearance() { return appearance; }

  /** Returns the proficiency id associated with the item. */
  public int getProficiency() { return proficiency; }

  /** Returns the max. stack amount available for the the item. */
  public int getStack() { return stack; }

  /** Returns the enchantment value of the item. */
  public int getEnchantment() { return enchantment; }

  /** Provides access to the {@link EffectInfo} instance associated with the item. */
  public EffectInfo getEffectInfo() { return effectInfo; }

  /** Returns number of defined item abilities. */
  public int getAbilityCount() { return abilityEntries.size(); }

  /** Returns the specified ability structure. */
  public AbilityEntry getAbility(int index) throws IndexOutOfBoundsException { return abilityEntries.get(index); }

  /** Returns a sequential {@link Stream} of the {@code AbilityInfo} list. */
  public Stream<AbilityEntry> getAbilityStream() { return abilityEntries.stream(); }

  /** Returns the type of the specified ability. */
  public int getAbilityType(int index) { return (index >= 0 && index < abilityEntries.size()) ? abilityEntries.get(index).getAbilityType() : -1; }

  /** Returns the number of defined global item effects. */
  public int getGlobalEffectsCount() { return effectsEntries.size(); }

  /** Returns the specified global item effect. */
  public EffectEntry getGlobalEffect(int index) throws IndexOutOfBoundsException { return effectsEntries.get(index); }

  /** Returns a sequential {@link Stream} of the {@code EffectInfo} list. */
  public Stream<EffectEntry> getEffectStream() { return effectsEntries.stream(); }

  /** Returns the most suitable item slot type compatible with the current item. */
  public SlotType getSlotType()
  {
    return getSlotType(getCategory());
  }

  /** Returns the most suitable item slot type compatible with the specified item category. */
  public static SlotType getSlotType(int category)
  {
    switch (category) {
      case 7:   // headgear
      case 40:  // lenses
      case 72:  // hats
        return SlotType.HELMET;
      case 2:   // armor
      case 60:  // leather armor
      case 61:  // studded leather
      case 62:  // chain mail
      case 63:  // splint mail
      case 64:  // plate mail
      case 65:  // full plate
      case 66:  // hide armor
      case 67:  // robes
      case 68:  // scale mail
        return SlotType.ARMOR;
      case 12:  // shields
      case 41:  // bucklers
      case 47:  // large shields
      case 49:  // medium shields
      case 53:  // small shields
        return SlotType.SHIELD;
      case 6:   // bracers/gauntlets
      case 70:  // scarves
      case 73:  // gloves
      case 77:  // bracelets
        return SlotType.GLOVES;
      case 10:  // rings
        return SlotType.RING;
      case 1:   // amulets
      case 74:  // eyeballs
        return SlotType.AMULET;
      case 3:   // belts
        return SlotType.BELT;
      case 4:   // boots
        return SlotType.BOOTS;
      case 15:  // bows
      case 16:  // daggers
      case 17:  // maces
      case 18:  // slings
      case 19:  // small swords
      case 20:  // large swords
      case 21:  // hammers
      case 22:  // morning stars
      case 23:  // flails
      case 24:  // darts
      case 25:  // axes
      case 26:  // quarterstaves
      case 27:  // crossbows
      case 28:  // hand-to-hand weapons
      case 29:  // spears
      case 30:  // halberds
      case 44:  // clubs
      case 57:  // greatswords
      case 69:  // bastard swords
      case 76:  // teeth
        return SlotType.WEAPON;
      case 5:   // arrows
      case 14:  // bullets
      case 31:  // bolts
        return SlotType.QUIVER;
      case 32:  // cloaks and robes
        return SlotType.CLOAK;
      case 39:  // tattoos
        return SlotType.TATTOO;
      case 9:   // potions
      case 11:  // scrolls
      case 35:  // wands
        return SlotType.QUICK_ITEM;
      case 75:  // earrings
        return SlotType.EARRING;
      default:
        return SlotType.INVENTORY;
    }
  }

  /** Invoked for {@code null} items. */
  private void initDefault()
  {
    name = nameIdentified = appearance = "";
    flags = category = unusable = unusableKits = proficiency = stack = enchantment = 0;
  }

  /** Initializes relevant item attributes. */
  private void init() throws IOException, Exception
  {
    if (itmEntry == null) {
      initDefault();
      return;
    }

    try (final InputStream is = itmEntry.getResourceDataAsStream()) {
      byte[] sig = new byte[8];
      // offset = 0x00
      Misc.requireCondition(is.read(sig) == 8, "Could not read signature field: " + itmEntry);
      final String signature = new String(sig).toUpperCase(Locale.ENGLISH);
      switch (signature) {
        case "ITM V1  ":
        case "ITM V1.1":
        case "ITM V2.0":
          break;
        default:
          throw new Exception("Not an item resource: " + itmEntry.getResourceName());
      }

      // general name
      int strref = StreamUtils.readInt(is);
      this.name = StringTable.isValidStringRef(strref) ? StringTable.getStringRef(strref) : "";

      // identified name
      strref = StreamUtils.readInt(is);
      this.nameIdentified = StringTable.isValidStringRef(strref) ? StringTable.getStringRef(strref) : "";

      if (this.nameIdentified.isEmpty()) {
        this.nameIdentified = this.name;
      } else if (this.name.isEmpty()) {
        this.name = this.nameIdentified;
      }

      // flags (common)
      Misc.requireCondition(is.skip(0x8) == 0x8, "Could not advance in data stream: " + itmEntry);
      // offset = 0x18
      this.flags = StreamUtils.readInt(is);

      // category (common)
      // offset = 0x1c
      this.category = StreamUtils.readShort(is);

      // unusable (common)
      // offset = 0x1e
      this.unusable = StreamUtils.readInt(is);

      // appearance (common)
      // offset = 0x22
      this.appearance = StreamUtils.readString(is, 2);

      // unusableKits and proficiency (common for V1/V2.0 only)
      this.unusableKits = 0;
      if (!"ITM V1.1".equals(signature)) {
        int v;
        Misc.requireCondition(is.skip(5) == 5, "Could not advance in data stream: " + itmEntry);
        // offset = 0x29
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field: " + itmEntry);
        this.unusableKits |= (v << 24);
        Misc.requireCondition(is.skip(1) == 1, "Could not advance in data stream: " + itmEntry);
        // offset = 0x2b
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field: " + itmEntry);
        this.unusableKits |= (v << 16);
        Misc.requireCondition(is.skip(1) == 1, "Could not advance in data stream: " + itmEntry);
        // offset = 0x2d
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field: " + itmEntry);
        this.unusableKits |= (v << 8);
        Misc.requireCondition(is.skip(1) == 1, "Could not advance in data stream: " + itmEntry);
        // offset = 0x2f
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field: " + itmEntry);
        this.unusableKits |= v;

        // proficiency
        Misc.requireCondition(is.skip(0x1) == 0x1, "Could not advance in data stream: " + itmEntry);
        this.proficiency = is.read();
      } else {
        // to keep stream position synchronized
        Misc.requireCondition(is.skip(0xe) == 0xe, "Could not advance in data stream: " + itmEntry);
      }

      // stack amount
      Misc.requireCondition(is.skip(0x6) == 0x6, "Could not advance in data stream: " + itmEntry);
      this.stack = StreamUtils.readShort(is);

      // enchantment
      Misc.requireCondition(is.skip(0x26) == 0x26, "Could not advance in data stream: " + itmEntry);
      this.enchantment = StreamUtils.readInt(is);

      // abilities (common: ofs, num, header)
      // offset = 0x64
      int ofsAbil = StreamUtils.readInt(is);    // abilities offset
      int numAbil = StreamUtils.readShort(is);  // abilities count
      int ofsFx = StreamUtils.readInt(is);      // effects offset
      int idxFx = StreamUtils.readShort(is);    // start index of global effects
      int numFx = StreamUtils.readShort(is);    // total effects count
      ofsFx += idxFx * 0x30;                    // adjusting global effects offset
      numFx -= idxFx;                           // adjusting global effects count
      int curOfs = 0x72;                        // end of main structure
      byte[] effect = new byte[48];             // buffer for effect entry
      byte[] ability = new byte[56];            // buffer for ability entry

      // reading global color effects (attempt 1)
      int skip;
      if (numFx > 0 && ofsFx < ofsAbil) {
        skip = ofsFx - curOfs;
        Misc.requireCondition(is.skip(skip) == skip, "Could not advance in data stream: " + itmEntry);
        curOfs += skip;
        // offset = [ofsFx]
        for (int i = 0; i < numFx; i++) {
          Misc.requireCondition(is.read(effect) == effect.length, "Could not read effect " + i + ": " + itmEntry);
          curOfs += effect.length;
          EffectEntry ei = new EffectEntry(effect);
          effectsEntries.add(ei);
          parseEffect(effect);
        }
      }

      // reading ability types
      if (numAbil > 0) {
        skip = ofsAbil - curOfs;
        Misc.requireCondition(skip >= 0 && is.skip(skip) == skip, "Could not advance in data stream: " + itmEntry);
        curOfs += skip;
        // offset = [ofsAbil]
        for (int i = 0; i < numAbil; i++) {
          Misc.requireCondition(is.read(ability) == ability.length, "Could not read ability " + i + ": " + itmEntry);
          curOfs += ability.length;
          abilityEntries.add(new AbilityEntry(ability));
        }
      }

      // reading global color effects (attempt 2)
      if (numFx > 0 && ofsFx >= ofsAbil) {
        skip = ofsFx - curOfs;
        Misc.requireCondition(skip >= 0 && is.skip(skip) == skip, "Could not advance in data stream: " + itmEntry);
        curOfs += skip;
        // offset = [ofsFx]
        for (int i = 0; i < numFx; i++) {
          Misc.requireCondition(is.read(effect) == effect.length, "Could not read effect " + i + ": " + itmEntry);
          curOfs += effect.length;
          EffectEntry ei = new EffectEntry(effect);
          effectsEntries.add(ei);
          parseEffect(effect);
        }
      }
    }
  }

  // Processes a global effect
  private void parseEffect(byte[] data)
  {
    getEffectInfo().add(EffectInfo.Effect.fromEffectV1(ByteBuffer.wrap(data), 0));
  }

//--------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ItemInfo o)
  {
    if (itmEntry == null) {
      return (o.itmEntry == null) ? 0 : -1;
    } else {
      return (o.itmEntry == null) ? 1 : itmEntry.compareTo(o.itmEntry);
    }
  }

//--------------------- End Interface Comparable ---------------------

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + effectInfo.hashCode();
    hash = 31 * hash + abilityEntries.hashCode();
    hash = 31 * hash + effectsEntries.hashCode();
    hash = 31 * hash + ((itmEntry == null) ? 0 : itmEntry.hashCode());
    hash = 31 * hash + ((name == null) ? 0 : name.hashCode());
    hash = 31 * hash + ((nameIdentified == null) ? 0 : nameIdentified.hashCode());
    hash = 31 * hash + flags;
    hash = 31 * hash + category;
    hash = 31 * hash + unusable;
    hash = 31 * hash + unusableKits;
    hash = 31 * hash + ((appearance == null) ? 0 : appearance.hashCode());
    hash = 31 * hash + proficiency;
    hash = 31 * hash + stack;
    hash = 31 * hash + enchantment;
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ItemInfo)) {
      return false;
    }
    ItemInfo other = (ItemInfo)o;
    boolean retVal = (this.effectInfo == null && other.effectInfo == null) ||
                     (this.effectInfo != null && this.effectInfo.equals(other.effectInfo));
    retVal &= (this.abilityEntries == null && other.abilityEntries == null) ||
              (this.abilityEntries != null && this.abilityEntries.equals(other.abilityEntries));
    retVal &= (this.effectsEntries == null && other.effectsEntries == null) ||
              (this.effectsEntries != null && this.effectsEntries.equals(other.effectsEntries));
    retVal &= (this.itmEntry == null && other.itmEntry == null) ||
              (this.itmEntry != null && this.itmEntry.equals(other.itmEntry));
    retVal &= (this.name == null && other.name == null) ||
              (this.name != null && this.name.equals(other.name));
    retVal &= (this.nameIdentified == null && other.nameIdentified == null) ||
              (this.nameIdentified != null && this.nameIdentified.equals(other.nameIdentified));
    retVal &= this.flags == other.flags;
    retVal &= this.category == other.category;
    retVal &= this.unusable == other.unusable;
    retVal &= this.unusableKits == other.unusableKits;
    retVal &= (this.appearance == null && other.appearance == null) ||
              (this.appearance != null && this.appearance.equals(other.appearance));
    retVal &= this.proficiency == other.proficiency;
    retVal &= this.stack == other.stack;
    retVal &= this.enchantment == other.enchantment;
    return retVal;
  }

  @Override
  public String toString()
  {
    if (isEmpty()) {
      return "None";
    } else if (getIdentifiedName().isEmpty()) {
      return getResourceEntry().getResourceName();
    } else {
      String name = getIdentifiedName();
      if (name.length() > 80) {
        name = name.substring(0, 80) + "...";
      }
      return getResourceEntry().getResourceName() + " (" + name + ")";
    }
  }

//-------------------------- INNER CLASSES --------------------------

  /** Storage class for relevant ability attributes. */
  public static class AbilityEntry
  {
    private final int type;
    private final int location;
    private final int target;
    private final int launcher;
    private final int damageType;
    private final int flags;
    private final int projectile;
    private final int probabilitySlash;
    private final int probabilityBackslash;
    private final int probabilityJab;
    private final boolean isArrow;
    private final boolean isBolt;
    private final boolean isBullet;

    /** Parses the item ability structure described by the byte array. */
    private AbilityEntry(byte[] ability)
    {
      Objects.requireNonNull(ability);
      DynamicArray buf = DynamicArray.wrap(ability, DynamicArray.ElementType.BYTE);
      this.type = buf.getByte(0x0);
      this.location = buf.getByte(0x2);
      this.target = buf.getByte(0xc);
      this.launcher = buf.getByte(0x10);
      this.damageType = buf.getShort(0x1c);
      this.flags = buf.getInt(0x26);
      this.projectile = buf.getShort(0x2a);
      this.probabilitySlash = buf.getShort(0x2c);
      this.probabilityBackslash = buf.getShort(0x2e);
      this.probabilityJab = buf.getShort(0x30);
      this.isArrow = buf.getShort(0x32) != 0;
      this.isBolt = buf.getShort(0x34) != 0;
      this.isBullet = buf.getShort(0x36) != 0;
    }

    /** Returns the ability type (1=melee, 2=ranged, 3=magical, 4=launcher). */
    public int getAbilityType() { return type; }

    /** Returns the ability location slot (1=weapon, 2=spell, 3=item, 4=ability). */
    public int getLocation() { return location; }

    /** Returns the target (1=living actor, 2=inventory, 3=dead actor, 4=any point, 5=caster, 7=caster immediately). */
    public int getTarget() { return target; }

    /** Returns the required launcher type (0=none, 1=bow, 2=crossbow, 3=sling). */
    public int getLauncher() { return launcher; }

    /**
     * Returns the damage type (1=piercing, 2=crushing, 3=slashing, 4=missile, 5=fist, 6=piercing/crushing,
     *                          7=piercing/slashing, 8=crushing/slashing, 9=blunt missile).
     */
    public int getDamageType() { return damageType; }

    /** Returns ability flags. */
    public int getFlags() { return flags; }

    /** Returns the projectile id. */
    public int getProjectile() { return projectile; }

    /** Returns the probability of triggering the slash attack animation. */
    public int getProbabilitySlash() { return probabilitySlash; }

    /** Returns the probability of triggering the backslash attack animation. */
    public int getProbabilityBackslash() { return probabilityBackslash; }

    /** Returns the probability of triggering the jab attack animation. */
    public int getProbabilityJab() { return probabilityJab; }

    /** Indicates whether the ability is an arrow. */
    public boolean isArrow() { return this.isArrow; }

    /** Indicates whether the ability is a bolt. */
    public boolean isBolt() { return this.isBolt; }

    /** Indicates whether the ability is a bullet. */
    public boolean isBullet() { return this.isBullet; }
  }

  /** Storage class for relevant global effects attributes. */
  public static class EffectEntry
  {
    private final int opcode;
    private final int target;
    private final int power;
    private final int parameter1;
    private final int parameter2;
    private final int timing;
    private final int dispelResist;
    private final int duration;
    private final String resource;
    private final int savingThrowFlags;
    private final int savingThrow;
    private final int special;

    /** Parses the EFF V1 structure described by the byte array. */
    private EffectEntry(byte[] effect)
    {
      Objects.requireNonNull(effect);
      DynamicArray buf = DynamicArray.wrap(effect, DynamicArray.ElementType.BYTE);
      this.opcode = buf.getShort(0x0);
      this.target = buf.getByte(0x2);
      this.power = buf.getByte(0x3);
      this.parameter1 = buf.getInt(0x4);
      this.parameter2 = buf.getInt(0x8);
      this.timing = buf.getByte(0xc);
      this.dispelResist = buf.getByte(0xd);
      this.duration = buf.getByte(0xe);
      this.resource = DynamicArray.getString(effect, 0x14, 8, Charset.forName("windows-1252"));
      this.savingThrowFlags = buf.getInt(0x24);
      this.savingThrow = buf.getInt(0x28);
      this.special = buf.getInt(0x2c);
    }

    /** Returns the effect opcode. */
    public int getOpcode() { return opcode; }

    /** Returns the effect target. */
    public int getTarget() { return target; }

    /** Returns the effect power. */
    public int getPower() { return power; }

    /** Returns parameter1 of the effect (meaning depends on opcode). */
    public int getParameter1() { return parameter1; }

    /** Returns parameter2 of the effect (meaning depends on opcode). */
    public int getParameter2() { return parameter2; }

    /** Returns the timing mode. */
    public int getTiming() { return timing; }

    /** Returns the dispel and resistance mode. */
    public int getDispelResist() { return dispelResist; }

    /** Returns the effect duration. */
    public int getDuration() { return duration; }

    /** Returns the resource resref of the effect. Returns empty string otherwise. */
    public String getResource() { return resource; }

    /** Returns the saving throw flags. */
    public int getSavingThrowFlags() { return savingThrowFlags; }

    /** Returns the saving throw bonus. */
    public int getSavingThrow() { return savingThrow; }

    /** Returns the special parameter used by selected effects. */
    public int getSpecial() { return special; }
  }

  /**
   * Represents a {@link Predicate} about an {@code ItemInfo} object.
   */
  public interface ItemPredicate extends Predicate<ItemInfo>
  {
    @Override
    boolean test(ItemInfo info);

    default ItemPredicate and(ItemPredicate other)
    {
      Objects.requireNonNull(other);
      return (t) -> test(t) && other.test(t);
    }

    @Override
    default ItemPredicate negate()
    {
      return (t) -> !test(t);
    }

    default ItemPredicate or(ItemPredicate other)
    {
      Objects.requireNonNull(other);
      return (t) -> test(t) || other.test(t);
    }
  }
}
