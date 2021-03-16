// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.internal;

import java.io.IOException;
import java.io.InputStream;
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
  /**
   * This predicate simply returns {@code false} for all items.
   */
  public static final ItemPredicate FILTER_NONE = (info) -> { return false; };

  /**
   * This predicate simply returns {@code true} for all items.
   */
  public static final ItemPredicate FILTER_ALL = (info) -> { return true; };

  /**
   * This predicate returns {@code true} only if the item can be equipped and unequipped in the character inventory.
   */
  public static final ItemPredicate FILTER_EQUIPPABLE = (info) -> {
    return (info.getFlags() & 0x04) == 0x04;  // droppable
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
      AbilityInfo ai = info.getAbility(0);
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
      AbilityInfo ai = info.getAbility(0);
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
      AbilityInfo ai = info.getAbility(0);
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

  private final ColorInfo colorInfo = new ColorInfo();
  private final List<AbilityInfo> abilityInfo = new ArrayList<>();
  private final List<EffectInfo> effectsInfo = new ArrayList<>();
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
   * @param entry the ITM {@code ResourceEntry}
   * @return the {@code ItemInfo} structure with relevant item details.
   * @throws Exception if the ITM resource could not be loaded.
   */
  public static ItemInfo get(ResourceEntry entry) throws Exception
  {
    ItemInfo retVal = null;
    if (entry == null) {
      return EMPTY;
    }
    synchronized (ITEM_CACHE) {
      retVal = ITEM_CACHE.get(entry);
      if (retVal == null) {
        retVal = new ItemInfo(entry);
        ITEM_CACHE.put(entry, retVal);
      }
    }
    return retVal;
  }

  /**
   * Functions the same as {@link #get(ResourceEntry)} excepts that available cache entries will be updated with the
   * new item data.
   * @param entry the ITM {@code ResourceEntry}
   * @return the {@code ItemInfo} structure with relevant item details.
   * @throws Exception if the ITM resource could not be loaded.
   */
  public static ItemInfo getValidated(ResourceEntry entry) throws Exception
  {
    ItemInfo retVal = null;
    if (entry == null) {
      return EMPTY;
    }
    synchronized (ITEM_CACHE) {
      retVal = new ItemInfo(entry);
      ITEM_CACHE.put(entry, retVal);
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

  /** Provides access to the {@link ColorInfo} instance associated with the item. */
  public ColorInfo getColorInfo() { return colorInfo; }

  /** Returns number of defined item abilities. */
  public int getAbilityCount() { return abilityInfo.size(); }

  /** Returns the specified ability structure. */
  public AbilityInfo getAbility(int index) throws IndexOutOfBoundsException { return abilityInfo.get(index); }

  /** Returns a sequential {@link Stream} of the {@code AbilityInfo} list. */
  public Stream<AbilityInfo> getAbilityStream() { return abilityInfo.stream(); }

  /** Returns the type of the specified ability. */
  public int getAbilityType(int index) { return (index >= 0 && index < abilityInfo.size()) ? abilityInfo.get(index).getAbilityType() : -1; }

  /** Returns the number of defined global item effects. */
  public int getGlobalEffectsCount() { return effectsInfo.size(); }

  /** Returns the specified global item effect. */
  public EffectInfo getGlobalEffect(int index) throws IndexOutOfBoundsException { return effectsInfo.get(index); }

  /** Returns a sequential {@link Stream} of the {@code EffectInfo} list. */
  public Stream<EffectInfo> getEffectStream() { return effectsInfo.stream(); }

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
          EffectInfo ei = new EffectInfo(effect);
          parseEffect(ei);
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
          abilityInfo.add(new AbilityInfo(ability));
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
          EffectInfo ei = new EffectInfo(effect);
          parseEffect(ei);
        }
      }
    }
  }

  // Processes a global effect: only "set color" effect is considered
  private void parseEffect(EffectInfo info)
  {
    if (info.getOpcode() == 7) {
      // set color
      if (info.getTarget() == 1 && info.getTiming() == 2) {
        // self target; on equip
        SegmentDef.SpriteType type = null;
        int location = info.getParameter2() & 0xf;
        switch ((info.getParameter2() >> 4) & 0xf) {
          case 0:
            type = SegmentDef.SpriteType.AVATAR;
            break;
          case 1:
            type = SegmentDef.SpriteType.WEAPON;
            break;
          case 2:
            type = SegmentDef.SpriteType.SHIELD;
            break;
          case 3:
            type = SegmentDef.SpriteType.HELMET;
            break;
          default:
            if ((info.getParameter2() & 0xff) == 0xff) {
              type = SegmentDef.SpriteType.AVATAR;
              location = -1;
            }
        }
        getColorInfo().add(type, location, info.getParameter1());
      }
    }
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
    hash = 31 * hash + colorInfo.hashCode();
    hash = 31 * hash + abilityInfo.hashCode();
    hash = 31 * hash + ((itmEntry == null) ? 0 : itmEntry.hashCode());
    hash = 31 * hash + flags;
    hash = 31 * hash + category;
    hash = 31 * hash + unusable;
    hash = 31 * hash + unusableKits;
    hash = 31 * hash + ((appearance == null) ? 0 : appearance.hashCode());
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
    boolean retVal = (this.colorInfo == null && other.colorInfo == null) ||
                     (this.colorInfo != null && this.colorInfo.equals(other.colorInfo));
    retVal &= (this.abilityInfo == null && other.abilityInfo == null ||
               this.abilityInfo != null && this.abilityInfo.equals(other.abilityInfo));
    retVal &= (this.itmEntry == null && other.itmEntry == null) ||
              (this.itmEntry != null && this.itmEntry.equals(other.itmEntry));
    retVal &= this.flags == other.flags;
    retVal &= this.category == other.category;
    retVal &= this.unusable == other.unusable;
    retVal &= this.unusableKits == other.unusableKits;
    retVal &= (this.appearance == null && other.appearance == null) ||
              (this.appearance != null && this.appearance.equals(other.appearance));
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
  public static class AbilityInfo
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
    private AbilityInfo(byte[] ability)
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
  public static class EffectInfo
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
    private EffectInfo(byte[] effect)
    {
      Objects.requireNonNull(effect);
      DynamicArray buf = DynamicArray.wrap(effect, DynamicArray.ElementType.BYTE);
      this.opcode = buf.getShort(0x0);
      this.target = buf.getByte(0x2);
      this.power = buf.getByte(0x3);
      this.parameter1 = buf.getShort(0x4);
      this.parameter2 = buf.getShort(0x8);
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
