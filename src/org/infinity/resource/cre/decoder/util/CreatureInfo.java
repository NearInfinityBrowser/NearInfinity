// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Effect;
import org.infinity.resource.Effect2;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.Item;
import org.infinity.resource.cre.decoder.MonsterPlanescapeDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.infinity.util.tuples.Couple;

/**
 * Provides useful information about a creature resource and their equipment.
 */
public class CreatureInfo
{
  /** Value to disable allegiance override. */
  public static final int OVERRIDE_NONE = -1;

  /**
   * Identifies the equipment slot for an item.
   * Entries in this enumeration are sorted by effect application order.
   */
  public enum ItemSlots {
    /** Slot for helmets and ioun stones. */
    HELMET,
    /** Slot for armor or robes. */
    ARMOR,
    /** Slot for shields or left-handed weapons. */
    SHIELD,
    /** Slot for gauntlets and bracers. */
    GAUNTLETS,
    /** Right slot for rings. */
    RING_RIGHT,
    /** Left slot for rings. */
    RING_LEFT,
    /** Slot for amulets or necklaces. */
    AMULET,
    /** Slot for belts. */
    BELT,
    /** Slot for boots. */
    BOOTS,
    /** Slot for cloaks and mantles. */
    CLOAK,
    /** Slot for the currently active weapon. */
    WEAPON,
  }

  private final EnumMap<ItemSlots, ItemInfo> equipment = new EnumMap<>(ItemSlots.class);
  private final EffectInfo effectInfo;
  private final SpriteDecoder decoder;
  private final CreResource cre;

  private int allegianceOverride;

  public CreatureInfo(SpriteDecoder decoder, CreResource cre) throws Exception
  {
    this.effectInfo = new EffectInfo();
    this.decoder = Objects.requireNonNull(decoder, "SpriteDecoder instance cannot be null");
    this.cre = Objects.requireNonNull(cre, "CRE resource cannot be null");
    this.allegianceOverride = OVERRIDE_NONE;
    init();
  }

  /** Returns the {@code SpriteDecoder} instance associated with the creature resource. */
  public SpriteDecoder getDecoder() { return decoder; }

  /** Returns the {@code CreResource} instance of the creature resource. */
  public CreResource getCreResource() { return cre; }

  /** Returns creature flags. */
  public int getFlags() { return ((IsNumeric)cre.getAttribute(CreResource.CRE_FLAGS)).getValue(); }

  /** Returns the creature status. */
  public int getStatus() { return ((IsNumeric)cre.getAttribute(CreResource.CRE_STATUS)).getValue(); }

  /** Returns whether the creature is panicked. */
  public boolean isStatusPanic()
  {
    return ((getStatus() & (1 << 2)) != 0);
  }

  /**
   * Returns {@code true} if the creature is under the stoneskin effect or status is set to petrification/stone death.
   */
  public boolean isStoneEffect()
  {
    return ((getStatus() & (1 << 7)) != 0) || isEffectActive(SegmentDef.SpriteType.AVATAR, EffectInfo.FILTER_STONE_EFFECT);
  }

  /** Returns {@code true} if the creature status is set to "frozen death". */
  public boolean isFrozenEffect()
  {
    return ((getStatus() & (1 << 6)) != 0);
  }

  /** Returns {@code true} if the creature status is set to "flame death". */
  public boolean isBurnedEffect()
  {
    return ((getStatus() & (1 << 9)) != 0);
  }

  /** Returns {@code true} if the creature is under the effect of blur. */
  public boolean isBlurEffect()
  {
    return isEffectActive(SegmentDef.SpriteType.AVATAR, EffectInfo.FILTER_BLUR_EFFECT);
  }

  /** Returns the creature animation id. */
  public int getAnimationId() { return ((IsNumeric)cre.getAttribute(CreResource.CRE_ANIMATION)).getValue(); }

  /**
   * Returns the translucency strength of the creature. Values can range from 0 (fully opaque) to 255 (fully transparent).
   * The method takes creature animation attributes and (EE only) creature attributes into account.
   */
  public int getTranslucency()
  {
    int retVal = 0;
    if (decoder.<Boolean>getAttribute(SpriteDecoder.KEY_TRANSLUCENT)) { // avoid circular dependencies
      retVal = 128;
    }
    if (Profile.isEnhancedEdition()) {
      int v = ((IsNumeric)cre.getAttribute(CreResource.CRE_TRANSLUCENCY)).getValue();
      if (v > 0) {
        retVal = 255 - Math.min(v, 255);
      }
    }
    return retVal;
  }

  /**
   * Returns the translucency strength of the creature after evaluating all potential sources of translucency,
   * which includes creature animation properties, creature properties (EE only) and item effects.
   * Values can range from 0 (fully opaque) to 255 (fully transparent).
   */
  public int getEffectiveTranslucency()
  {
    int retVal = -1;

    // getting translucency from item effect
    for (final ItemSlots slot : ItemSlots.values()) {
      ItemInfo info = getItemInfo(slot);
      if (info != null) {
        EffectInfo.Effect fx = info.getEffectInfo().getFirstEffect(this, SegmentDef.SpriteType.AVATAR, EffectInfo.FILTER_TRANSLUCENCY_EFFECT);
        if (fx != null && fx.getParameter2() == 0) {
          int amount = Math.max(0, Math.min(255, fx.getParameter1()));
          retVal = Math.max(retVal, 255 - amount);
        }
      }
    }

    // getting translucency from creature effect
    EffectInfo.Effect fx = getEffectInfo().getFirstEffect(this, SegmentDef.SpriteType.AVATAR, EffectInfo.FILTER_TRANSLUCENCY_EFFECT);
    if (fx != null && fx.getParameter2() == 0) {
      int amount = Math.max(0, Math.min(255, fx.getParameter1()));
      retVal = Math.max(retVal, 255 - amount);
    }

    if (retVal < 0) {
      // falling back to default translucency
      retVal = getTranslucency();
    }

    return retVal;
  }

  /**
   * Returns the (average or highest) class level of the creature. Returns 0 if level could not be determined.
   * @param highestOnly specify {@code false} to determine the average level of all (active and inactive) classes.
   *                    Specify {@code true} to return the highest level of all (active and inactive) classes.
   */
  public int getClassLevel(boolean highestOnly)
  {
    int retVal = 0;
    int ofsBase = cre.getExtraOffset();

    String creVersion = getCreatureVersion();
    if ("V2.2".equals(creVersion)) {
      // IWD2
      int level = 0;
      int maxLevel = 0;
      int numClasses = 0;
      for (int i = 0; i < 11; i++) {
        int v = ((IsNumeric)cre.getAttribute(ofsBase + 0x8b + i)).getValue();
        if (v > 0) {
          level += v;
          maxLevel = Math.max(maxLevel, v);
          numClasses++;
        }
      }
      if (highestOnly) {
        retVal = maxLevel;
      } else {
        retVal = level;
        if (numClasses > 0) {
          retVal /= numClasses;
        }
      }
    } else {
      // non-IWD2
      int cls = ((IsNumeric)cre.getAttribute(CreResource.CRE_CLASS)).getValue();
      int numClasses;
      switch (cls) {
        case 7:
        case 8:
        case 9:
        case 13:
        case 14:
        case 15:
        case 16:
        case 18:
          numClasses = 2;
          break;
        case 10:
        case 17:
          numClasses =3;
          break;
        default:
          numClasses = 1;
      }
      int level = 0;
      int maxLevel = 0;
      for (int i = 0; i < numClasses; i++) {
        int v = ((IsNumeric)cre.getAttribute(ofsBase + 0x234 + i)).getValue();
        level += v;
        maxLevel = Math.max(maxLevel, v);
      }
      if (highestOnly) {
        retVal = maxLevel;
      } else {
        retVal = level / numClasses;
      }
    }

    return retVal;
  }

  /** Returns the allegiance of the creature. May return overridden allegiance if set. */
  public int getAllegiance()
  {
    return getAllegiance(true);
  }

  /**
   * Returns the allegiance of the creature.
   * @param allowOverride whether allegiance override of the parent sprite decoder is considered.
   * @return the (overridden) allegiance value of the creature.
   */
  public int getAllegiance(boolean allowOverride)
  {
    int retVal = OVERRIDE_NONE;
    if (allowOverride) {
      retVal = getAllegianceOverride();
    }
    if (retVal == OVERRIDE_NONE) {
      retVal = ((IsNumeric)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
    }
    return retVal;
  }

  /** Returns the overridden allegiance. Returns {@link #OVERRIDE_NONE} if allegiance has not been overridden. */
  public int getAllegianceOverride()
  {
    return allegianceOverride;
  }

  /**
   * Overrides the creature's allegiance.
   * @param allegiance new allegiance of the creature. Uses the same values as defined in EA.IDS.
   *                   Specify {@link #OVERRIDE_NONE} to disable.
   */
  public void setAllegianceOverride(int allegiance)
  {
    allegiance = Math.max(OVERRIDE_NONE, Math.min(255, allegiance));
    if (allegianceOverride != allegiance) {
      allegianceOverride = allegiance;
      decoder.allegianceChanged();
    }
  }

  /**
   * Returns the ability index of the currently selected weapon.
   * Returns -1 if no weapon (not even fist) is available or could not be determined.
   */
  public int getSelectedWeaponAbility()
  {
    int retVal = -1;

    ItemInfo itemInfo = equipment.get(ItemSlots.WEAPON);
    if (itemInfo != null) {
      int numAbils = itemInfo.getAbilityCount();
      int idxAbil = ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_ABILITY)).getValue();
      retVal = Math.max(0, Math.min(numAbils - 1, idxAbil));
    }
    return retVal;
  }

  /**
   * Returns the active weapon of the specified creature.
   * @return The {@code ItemInfo} object for the item resource of the active weapon.
   *         Returns {@code null} if no weapon is active.
   */
  public ItemInfo getEquippedWeapon()
  {
    return getItemInfo(ItemSlots.WEAPON);
  }

  /**
   * Returns the equipped helmet of the specified creature.
   * @return The {@code ItemInfo} object for the item resource of the helmet.
   *         Returns {@code null} if no helmet is equipped.
   */
  public ItemInfo getEquippedHelmet()
  {
    return getItemInfo(ItemSlots.HELMET);
  }

  /**
   * Returns the equipped shield or left-handed weapon of the specified creature.
   * @return The {@code ItemInfo} object for the item resource of the shield or left-handed weapon.
   *         Returns {@code null} if left hand is empty.
   */
  public ItemInfo getEquippedShield()
  {
    return getItemInfo(ItemSlots.SHIELD);
  }

  /**
   * Returns the equipped armor or robe.
   * @return The {@code ItemInfo} object for the item resource of armor or robe.
   *         Returns {@code null} if no armor is equipped.
   */
  public ItemInfo getEquippedArmor()
  {
    return getItemInfo(ItemSlots.ARMOR);
  }

  /**
   * Returns the {@link ItemInfo} instance associated with the specified item slot.
   * @param slot the slot where the item is equipped.
   * @return {@code ItemInfo} instance of the item, {@code null} if no item equipped.
   */
  public ItemInfo getItemInfo(ItemSlots slot)
  {
    if (slot == null) {
      return null;
    }
    return equipment.get(slot);
  }

  /**
   * Returns a list of equipped items in the order of effect application.
   * @return List of {@code ItemInfo} instances in the order of effect application.
   */
  public ItemInfo[] getEffectiveItemInfo()
  {
    ArrayList<ItemInfo> items = new ArrayList<>();

    for (final ItemSlots slot : ItemSlots.values()) {
      ItemInfo info = equipment.get(slot);
      if (info != null) {
        items.add(info);
      }
    }

    return items.toArray(new ItemInfo[items.size()]);
  }

  /** Provides access to the {@link EffectInfo} instance which manages effects attached to the current creature. */
  public EffectInfo getEffectInfo() { return effectInfo; }

  /**
   * Returns the number of defined color entries for the creature.
   * Number can vary for PST or PST:EE creatures. Otherwise it always returns 7.
   */
  public int getColorCount()
  {
    int retVal = 7;
    String creVersion = getCreatureVersion();
    if ("V1.1".equals(creVersion) || "V1.2".equals(creVersion)) {
      if (decoder instanceof MonsterPlanescapeDecoder && decoder.isFalseColor()) {
        retVal = ((MonsterPlanescapeDecoder)decoder).getColorLocationCount();
      } else {
        retVal = 0;
      }
    } else if (Profile.getGame() == Profile.Game.PSTEE) {
      if (decoder instanceof MonsterPlanescapeDecoder) {
        retVal = ((MonsterPlanescapeDecoder)decoder).getColorLocationCount();
      }
    }
    return retVal;
  }

  /**
   * Returns the number of defined color entries for the specified sprite overlay type.
   * @param type the sprite overlay type.
   * @return number of defined color entries for the sprite overlay type.
   */
  public int getColorCount(SegmentDef.SpriteType type)
  {
    int retVal = 0;

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    if (type == SegmentDef.SpriteType.AVATAR) {
      return getColorCount();
    }

    ItemInfo itemInfo = null;
    switch (type) {
      case HELMET:
        itemInfo = equipment.get(ItemSlots.HELMET);
        break;
      case SHIELD:
        itemInfo = equipment.get(ItemSlots.SHIELD);
        break;
      case WEAPON:
        itemInfo = equipment.get(ItemSlots.WEAPON);
        break;
      default:
    }

    if (itemInfo != null) {
      HashSet<Integer> set = new HashSet<>();
      List<EffectInfo.Effect> fxList = itemInfo.getEffectInfo().getEffects(this, type, EffectInfo.FILTER_COLOR_EFFECT);
      for (final EffectInfo.Effect fx : fxList) {
        set.add(fx.getParameter2() & 0xf);
      }
      retVal = set.size();
    }

    return retVal;
  }

  /**
   * Returns the color entry of the specified location index as defined by the creature resource.
   * Color locations range from 0 to 6. For PST and PST:EE range can differ depending on creature animation.
   * Returns -1 if color entry is not available.
   */
  public int getColorValue(int locationIndex)
  {
    int retVal = -1;
    if (locationIndex >= 0 && locationIndex < getColorCount()) {
      String creVersion = getCreatureVersion();
      if ("V1.1".equals(creVersion) || "V1.2".equals(creVersion)) {
        retVal = ((IsNumeric)cre.getAttribute(String.format(CreResource.CRE_COLOR_FMT, locationIndex + 1))).getValue();
      } else {
        int ofsBase = cre.getExtraOffset();
        retVal = ((IsNumeric)cre.getAttribute(ofsBase + 0x2c + locationIndex)).getValue();
      }
    }
    return retVal;
  }

  /**
   * Returns the color entry of the specified location index for the specified sprite overlay type.
   * @param type the sprite overlay type.
   * @param locationIndex the color location index.
   * @return color entry index. Returns -1 if color entry is not available.
   */
  public int getColorValue(SegmentDef.SpriteType type, int locationIndex)
  {
    int retVal = -1;

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    if (type == SegmentDef.SpriteType.AVATAR) {
      return getColorValue(locationIndex);
    }

    ItemInfo itemInfo = null;
    switch (type) {
      case HELMET:
        itemInfo = equipment.get(ItemSlots.HELMET);
        break;
      case SHIELD:
        itemInfo = equipment.get(ItemSlots.SHIELD);
        break;
      case WEAPON:
        itemInfo = equipment.get(ItemSlots.WEAPON);
        break;
      default:
    }

    if (itemInfo != null) {
      EffectInfo.Effect fx = itemInfo.getEffectInfo().getColorByLocation(this, type, EffectInfo.OPCODE_SET_COLOR, locationIndex);
      if (fx != null) {
        retVal = fx.getParameter1();
      }
    }

    return retVal;
  }

  /**
   * Returns the color entry of the specified location index for the avatar sprite
   * after applying all equipment and effect colors as well as the source of the color value.
   * @param opcode filter by this opcode.
   * @param locationIndex the color location index. Available range: [-1, 6]
   * @return a tuple with the color entry as well as a {@code Boolean} value indicating whether random colors are allowed.
   * Returns {@code -1} for the color entry if value could not be determined.
   */
  public Couple<Integer, Boolean> getEffectiveColorValue(int locationIndex)
  {
    return getEffectiveColorValue(SegmentDef.SpriteType.AVATAR, locationIndex);
  }

  /**
   * Returns the color entry of the location index for the specified sprite overlay type
   * after applying all equipment and effect colors as well as the source of the color value.
   * @param type the {@link SegmentDef.SpriteType SpriteType} target
   * @param locationIndex the color location index. Available range: [-1, 6]
   * @return a tuple with the color entry as well as a {@code Boolean} value indicating whether random colors are allowed.
   * Returns {@code -1} for the color entry if value could not be determined.
   */
  public Couple<Integer, Boolean> getEffectiveColorValue(SegmentDef.SpriteType type, int locationIndex)
  {
    Couple<Integer, Boolean> retVal = Couple.with(-1, true);

    // using creature color by default
    int value = getColorValue(SegmentDef.SpriteType.AVATAR, locationIndex);
    if (value >= 0) {
      retVal.setValue0(value);
      retVal.setValue1(Boolean.TRUE);
    }

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    // checking equipped items
    ItemInfo[] itemInfos = getEffectiveItemInfo();
    for (final ItemInfo info : itemInfos) {
      EffectInfo.Effect fx = info.getEffectInfo().getColorByLocation(this, type, EffectInfo.OPCODE_SET_COLOR, locationIndex);
      if (fx != null) {
        retVal.setValue0(fx.getParameter1());
        retVal.setValue1(Boolean.FALSE);
      }
    }

    // checking creature effects
    EffectInfo.Effect fx = getEffectInfo().getColorByLocation(this, type, EffectInfo.OPCODE_SET_COLOR, locationIndex);
    if (fx != null) {
      retVal.setValue0(fx.getParameter1());
      retVal.setValue1(Boolean.FALSE);
    }

    return retVal;
  }

  /**
   * Returns the color value of the specified location index for the specified sprite overlay type.
   * @param type the sprite overlay type.
   * @param opcode the opcode to filter.
   * @param locationIndex the color location index.
   * @return color entry index. Returns -1 if color value is not available.
   */
  public int getTintValue(SegmentDef.SpriteType type, int opcode, int locationIndex)
  {
    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    EffectInfo.Effect fx = getEffectInfo().getColorByLocation(this, type, opcode, locationIndex);
    return (fx != null) ? EffectInfo.swapBytes(fx.getParameter1()) : -1;
  }

  /**
   * Returns the color tint value of the location index for the specified sprite overlay type
   * after applying all equipment and effect colors as well as the source of the color value.
   * @param type the {@link SegmentDef.SpriteType SpriteType} target
   * @param locationIndex the color location index. Available range: [-1, 6]
   * @return a tuple with the effect opcode as well as the RGB color value.
   * Returns {@code -1} for each of the tuple elements if value could not be determined.
   */
  public Couple<Integer, Integer> getEffectiveTintValue(SegmentDef.SpriteType type, int locationIndex)
  {
    Couple<Integer, Integer> retVal = Couple.with(-1, -1);

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    final int[] opcodes = {EffectInfo.OPCODE_TINT_BRIGHT, EffectInfo.OPCODE_TINT_SOLID, EffectInfo.OPCODE_SET_COLOR_GLOW};

    // checking equipped items
    int opcode = -1, value = -1;
    ItemInfo[] itemInfos = getEffectiveItemInfo();
    for (final ItemInfo info : itemInfos) {
      for (final int code : opcodes) {
        final EffectInfo.Effect fx = info.getEffectInfo().getColorByLocation(this, type, code, locationIndex);
        if (fx != null) {
          opcode = code;
          value = EffectInfo.swapBytes(fx.getParameter1());
          break;
        }
      }
      if (value != -1) {
        retVal.setValue0(opcode);
        retVal.setValue1(value);
      }
    }

    // checking creature effects
    opcode = value = -1;
    for (final int code : opcodes) {
      final EffectInfo.Effect fx = getEffectInfo().getColorByLocation(this, type, code, locationIndex);
      if (fx != null) {
        opcode = code;
        value = EffectInfo.swapBytes(fx.getParameter1());
        break;
      }
    }
    if (value != -1) {
      retVal.setValue0(opcode);
      retVal.setValue1(value);
    }

    return retVal;
  }

  /**
   * Returns whether the specified effect is active.
   * @param type the {@link SegmentDef.SpriteType SpriteType} target.
   * @param opcode the effect opcode to filter.
   * @return {@code true} if the effect with matching parameters exists. Returns {@code false} otherwise.
   */
  public boolean isEffectActive(SegmentDef.SpriteType type, int opcode)
  {
    return isEffectActive(type, (fx) -> fx.getOpcode() == opcode);
  }

  /**
   * Returns whether the specified effect is active with the given parameters.
   * @param type the {@link SegmentDef.SpriteType SpriteType} target
   * @param pred the predicate to filter.
   * @return {@code true} if the effect with matching parameters exists. Returns {@code false} otherwise.
   */
  public boolean isEffectActive(SegmentDef.SpriteType type, Predicate<EffectInfo.Effect> pred)
  {
    boolean retVal = false;

    // checking creature effects
    EffectInfo.Effect fx = getEffectInfo().getFirstEffect(this, type, pred);
    retVal = (fx != null);

    if (!retVal) {
      // checking equipped items
      ItemInfo[] itemInfos = getEffectiveItemInfo();
      for (final ItemInfo info : itemInfos) {
        fx = info.getEffectInfo().getFirstEffect(this, type, pred);
        if (fx != null) {
          retVal = true;
          break;
        }
      }
    }

    return retVal;
  }

  /** Returns the creature resource version. */
  private String getCreatureVersion()
  {
    String sig = ((IsTextual)cre.getAttribute(CreResource.COMMON_SIGNATURE)).getText().toUpperCase(Locale.ENGLISH);
    String versionLabel = sig.equals("CHR ") ? CreResource.CHR_VERSION_2 : CreResource.COMMON_VERSION;
    return ((IsTextual)cre.getAttribute(versionLabel)).getText().toUpperCase(Locale.ENGLISH);
  }

  private void init() throws Exception
  {
    // initialize attributes
    initCommon();

    switch (getCreatureVersion()) {
      case "V1.0":
        initV10();
        break;
      case "V1.1":  // non-standard PST format
      case "V1.2":
        initV12();
        break;
      case "V2.2":
        initV22();
        break;
      case "V9.0":
      case "V9.1":
        initV90();
        break;
      default:
        throw new Exception("Unsupported creature resource: " + cre.getResourceEntry().getResourceName());
    }
  }

  // initialize common section of creature resources
  private void initCommon() throws Exception
  {
    // collecting opcode 7 effects
    initEffects();
  }

  // initialize BG/EE-style creature resources
  private void initV10() throws Exception
  {
    // initializing equipment
    HashMap<ItemSlots, Integer> slotMap = new HashMap<ItemSlots, Integer>() {{
      put(ItemSlots.HELMET, 0);
      put(ItemSlots.ARMOR, 1);
      put(ItemSlots.SHIELD, 2);
      put(ItemSlots.GAUNTLETS, 3);
      put(ItemSlots.RING_LEFT, 4);
      put(ItemSlots.RING_RIGHT, 5);
      put(ItemSlots.AMULET, 6);
      put(ItemSlots.BELT, 7);
      put(ItemSlots.BOOTS, 8);
      put(ItemSlots.WEAPON, 9);
      put(ItemSlots.CLOAK, 17);
    }};

    initEquipment(slotMap);
  }

  // initialize PST-style creature resources
  private void initV12() throws Exception
  {
    // initializing equipment
    HashMap<ItemSlots, Integer> slotMap = new HashMap<ItemSlots, Integer>() {{
      put(ItemSlots.HELMET, 0);
      put(ItemSlots.ARMOR, 1);
      put(ItemSlots.SHIELD, 2);
      put(ItemSlots.GAUNTLETS, 3);
      put(ItemSlots.RING_LEFT, 4);
      put(ItemSlots.RING_RIGHT, 5);
      put(ItemSlots.AMULET, 6);
      put(ItemSlots.BELT, 7);
      put(ItemSlots.BOOTS, 8);
      put(ItemSlots.WEAPON, 9);
      put(ItemSlots.CLOAK, 19);
    }};

    initEquipment(slotMap);
  }

  // initialize IWD2-style creature resources
  private void initV22() throws Exception
  {
    // initializing equipment
    HashMap<ItemSlots, Integer> slotMap = new HashMap<ItemSlots, Integer>() {{
      put(ItemSlots.HELMET, 0);
      put(ItemSlots.ARMOR, 1);
      put(ItemSlots.GAUNTLETS, 3);
      put(ItemSlots.RING_LEFT, 4);
      put(ItemSlots.RING_RIGHT, 5);
      put(ItemSlots.AMULET, 6);
      put(ItemSlots.BELT, 7);
      put(ItemSlots.BOOTS, 8);
      put(ItemSlots.WEAPON, 9);
      put(ItemSlots.SHIELD, 10);
      put(ItemSlots.CLOAK, 21);
    }};

    initEquipment(slotMap);
  }

  // initialize IWD-style creature resources
  private void initV90() throws Exception
  {
    // initializing equipment
    HashMap<ItemSlots, Integer> slotMap = new HashMap<ItemSlots, Integer>() {{
      put(ItemSlots.HELMET, 0);
      put(ItemSlots.ARMOR, 1);
      put(ItemSlots.SHIELD, 2);
      put(ItemSlots.GAUNTLETS, 3);
      put(ItemSlots.RING_LEFT, 4);
      put(ItemSlots.RING_RIGHT, 5);
      put(ItemSlots.AMULET, 6);
      put(ItemSlots.BELT, 7);
      put(ItemSlots.BOOTS, 8);
      put(ItemSlots.WEAPON, 9);
      put(ItemSlots.CLOAK, 17);
    }};

    initEquipment(slotMap);
  }

  private void initEffects()
  {
    int fxType = ((IsNumeric)cre.getAttribute(CreResource.CRE_EFFECT_VERSION)).getValue();
    Class<? extends AbstractStruct> fxClass = (fxType == 1) ? Effect2.class : Effect.class;
    List<StructEntry> fxList= cre.getFields(fxClass);
    if (fxList != null) {
      for (final StructEntry se : fxList) {
        if (se instanceof AbstractStruct) {
          initEffect((AbstractStruct)se);
        }
      }
    }
  }

  private void initEffect(AbstractStruct as)
  {
    if (as instanceof Effect) {
      getEffectInfo().add(new EffectInfo.Effect((Effect)as));
    } else if (as instanceof Effect2) {
      getEffectInfo().add(new EffectInfo.Effect((Effect2)as));
    }
  }

  private void initEquipment(HashMap<ItemSlots, Integer> slotMap)
  {
    List<StructEntry> itemList = cre.getFields(Item.class);
    int ofsSlots = cre.getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue();
    for (HashMap.Entry<ItemSlots, Integer> entry : slotMap.entrySet()) {
      ItemSlots slot = entry.getKey();
      int slotIdx = entry.getValue().intValue();
      int itemIdx = -1;
      if (slot == ItemSlots.WEAPON) {
        // special: determine active weapon slot
        int selectedWeaponSlot = ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_SLOT)).getValue();
        int weaponIdx = getWeaponSlotIndex(selectedWeaponSlot);
        if (weaponIdx == 1000) {
          // selected weapon: fists
          itemIdx = weaponIdx;
        } else if (weaponIdx >= 0) {
          weaponIdx = getEffectiveWeaponIndex(weaponIdx);
          if (weaponIdx >= 0) {
            itemIdx = ((IsNumeric)cre.getAttribute(ofsSlots + weaponIdx * 2)).getValue();
          }
        }
      } else {
        itemIdx = ((IsNumeric)cre.getAttribute(ofsSlots + slotIdx * 2)).getValue();
      }

      initEquipmentItem(slot, itemIdx, itemList);
    }

    // Make sure that shield slot is empty when two-handed weapon is equipped
    if (getEquippedWeapon() != null) {
      if (ItemInfo.FILTER_WEAPON_2H.test(getEquippedWeapon())) {
        unsetItemSlot(ItemSlots.SHIELD);
      }
    }
  }

  private void initEquipmentItem(ItemSlots slot, int itemIndex, List<StructEntry> itemList)
  {
    ResourceEntry itmEntry = null;
    boolean isUndroppable = false;
    if (itemIndex == 1000) {
      // weapon: fists
      itmEntry = getFistWeapon();
    } else if (itemIndex >= 0 && itemIndex < itemList.size()) {
      if (itemList.get(itemIndex) instanceof Item) {
        Item itm = (Item)itemList.get(itemIndex);
        String itmResref = ((IsTextual)itm.getAttribute(Item.CRE_ITEM_RESREF)).getText();
        itmEntry = ResourceFactory.getResourceEntry(itmResref + ".ITM");
        isUndroppable = ((Flag)itm.getAttribute(Item.CRE_ITEM_FLAGS)).isFlagSet(3);
      }
    }

    if (itmEntry != null) {
      try {
        ItemInfo itemInfo = ItemInfo.get(itmEntry);
        itemInfo.overrideDroppableFlag(isUndroppable);
        equipment.put(slot, itemInfo);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // Removes any equipment set in the specified item slot. Returns the removed equipment.
  private ItemInfo unsetItemSlot(ItemSlots slot)
  {
    ItemInfo retVal = null;
    if (slot != null) {
      retVal = equipment.remove(slot);
    }
    return retVal;
  }

  private ResourceEntry getFistWeapon()
  {
    ResourceEntry retVal = null;

    if ((getAnimationId() & 0xff00) == 0x6500) {
      // hardcoded: monk animation activates special fists
      int level = getClassLevel(false);

      // try 2DA table first
      Table2da table = Table2daCache.get("MONKFIST.2DA");
      if (table != null) {
        level = Math.max(1, Math.min(table.getRowCount(), level));
        String resref = table.get(level, 1);
        retVal = ResourceFactory.getResourceEntry(resref + ".ITM");
      }

      // 2. fall back to hardcoded fists
      if (retVal == null) {
        int[] minLevels = { Integer.MIN_VALUE, 1, 3, 6, 9, 12, 15, 19, 25, Integer.MAX_VALUE };
        for (int i = 0; i < minLevels.length && retVal == null; i++) {
          if (level < minLevels[i]) {
            retVal = ResourceFactory.getResourceEntry("MFIST" + i + ".ITM");
          }
        }
      }
    }

    // default fists
    if (retVal == null) {
      retVal = ResourceFactory.getResourceEntry("FIST.ITM");
    }

    return retVal;
  }

  /**
   * Analyses the item at the specified slot index and returns the same index if the item can be used directly.
   * If the item requires a launcher the method scans the weapon slots of the specified CRE resource and
   * returns the slot index of a matching launcher item.
   * @param slotIndex the absolute slot index of the item to check
   * @return the absolute slot index of the effective weapon. Returns {@code -1} if no weapon could be determined.
   */
  private int getEffectiveWeaponIndex(int slotIndex)
  {
    int retVal = -1;
    if (slotIndex < 0) {
      return retVal;
    }

    // getting item entry index
    int ofsSlots = cre.getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue();
    int itmIndex = ((IsNumeric)cre.getAttribute(ofsSlots + slotIndex * 2)).getValue();
    int numItems = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEMS)).getValue();
    if (itmIndex < 0 || itmIndex >= numItems) {
      return retVal;
    }

    // loading referenced item
    ItemInfo info = null;
    int ofsItems = Objects.requireNonNull(cre).getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEMS)).getValue();
    try {
      String itmResref = ((IsTextual)cre.getAttribute(ofsItems + itmIndex * 20, true)).getText();
      info = ItemInfo.get(ResourceFactory.getResourceEntry(itmResref + ".ITM"));
    } catch (Exception e) {
      return retVal;
    }

    // check if item requires a launcher
    int abilityIndex = ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_ABILITY)).getValue();
    abilityIndex = Math.max(0, abilityIndex);
    int numAbil = info.getAbilityCount();
    abilityIndex = Math.min(abilityIndex, numAbil - 1);
    if (abilityIndex < 0) {
      return retVal;
    }
    int launcherType = info.getAbility(abilityIndex).getLauncher();
    if (launcherType == 0) {
      // item can be used directly
      retVal = slotIndex;
    }

    if (retVal < 0) {
      // launcher required: find a weapon in weapon slots 1-4 with a matching item category
      String creVer = getCreatureVersion();
      int idxWeaponSlots = 9;
      int slotGroupSize = creVer.equals("V2.2") ? 2 : 1;  // IWD2 uses weapon/shield pairs
      for (int i = 0; i < 4; i++) {
        int ofs = ofsSlots + (idxWeaponSlots + i * slotGroupSize) * 2;
        itmIndex = ((IsNumeric)cre.getAttribute(ofs)).getValue();
        if (itmIndex >= 0 && itmIndex < numItems) {
          int cat = -1;
          try {
            String itmResref = ((IsTextual)cre.getAttribute(ofsItems + itmIndex * 20, true)).getText();
            ResourceEntry itmEntry = ResourceFactory.getResourceEntry(itmResref + ".ITM");
            if (itmEntry != null) {
              try (InputStream is = itmEntry.getResourceDataAsStream()) {
                Misc.requireCondition(is.skip(0x1c) == 0x1c, "Could not read item category", IOException.class);
                cat = is.read();
                cat |= is.read() << 8;
              }
            }
            // checking if launcher type corresponds with item category
            if (launcherType == 1 && cat == 15 ||   // Bow
                launcherType == 2 && cat == 27 ||   // Crossbow
                launcherType == 3 && cat == 18) {   // Sling
              retVal = idxWeaponSlots + i * slotGroupSize;
              break;
            }
          } catch (Exception e) {
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Determines the absolute item slot index of the specified weapon-related slot id.
   * Special slots that cannot be mapped to slot indices are returned as -1.
   * Only weapon-related slots are considered, which includes the actual weapon slot ids as well as the ammo ids.
   * @param slotId the slot id (as defined in SLOTS.IDS)
   * @return absolute item slot index. Returns 1000 for fist slot. Returns -1 if slot id could not be mapped to a slot index.
   */
  private int getWeaponSlotIndex(int slotId)
  {
    int retVal = -1;
    if (slotId == 1000) {
      // fist weapon
      return slotId;
    }

    String creVer = getCreatureVersion();
    slotId += 35;   // determine SLOTS.IDS value
    switch (slotId) {
      case 3:   // IWD2: ammo1
      case 4:   // IWD2: ammo2
      case 5:   // IWD2: ammo3
      case 6:   // IWD2: ammo4
        if (creVer.equals("V2.2")) {
          retVal = slotId + 14;
        }
        break;
      case 11:  // ammo1
      case 12:  // ammo2
      case 13:  // ammo3
      case 14:  // ammo4
        if (!creVer.equals("V2.2")) {
          retVal = slotId + 2;
        }
        break;
      case 15:
      case 16:
        if (creVer.equals("V1.2")) {
          retVal = slotId + 2;
        }
        break;
      case 34:  // magically created weapon
      {
        switch (creVer) {
          case "V1.2":
            retVal = slotId + 11;
            break;
          case "V2.2":
            retVal = slotId + 15;
            break;
          default:
          {
            if (Profile.getGame() == Profile.Game.PSTEE) {
              // special: PSTEE party members have customized item slots
              int numSlots = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEM_SLOTS)).getValue();
              if (numSlots > 0) {
                Table2da table = Table2daCache.get("ITMSLOTS.2DA");
                if (table != null) {
                  for (int row = 0, rowCount = table.getRowCount(); row < rowCount; row++) {
                    if (Misc.toNumber(table.get(row, 1), -1) == 13) { // magic weapon slot?
                      retVal = Misc.toNumber(table.get(row, 5), -1);
                    }
                  }
                }
              }
            }
            if (retVal < 0) {
              retVal = slotId + 3;
            }
          }
        }
        break;
      }
      case 35:  // weapon1
      case 36:  // weapon2 (IWD2: shield1)
      case 37:  // weapon3 (IWD2: weapon2)
      case 38:  // weapon4 (IWD2: shield2)
      case 39:  // IWD2: weapon3
      case 40:  // IWD2: shield3
      case 41:  // IWD2: weapon4
      case 42:  // IWD2: shield4
        retVal = slotId - 26;
        break;
    }
    return retVal;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((equipment == null) ? 0 : equipment.hashCode());
    hash = 31 * hash + ((effectInfo == null) ? 0 : effectInfo.hashCode());
    hash = 31 * hash + ((decoder == null) ? 0 : decoder.hashCode());
    hash = 31 * hash + ((cre == null) ? 0 : cre.hashCode());
    hash = 31 * hash + Integer.valueOf(allegianceOverride).hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CreatureInfo)) {
      return false;
    }
    CreatureInfo other = (CreatureInfo)o;
    boolean retVal = (this.equipment == null && other.equipment == null) ||
                     (this.equipment != null && this.equipment.equals(other.equipment));
    retVal &= (this.effectInfo == null && other.effectInfo == null) ||
              (this.effectInfo != null && this.effectInfo.equals(other.effectInfo));
    retVal &= (this.decoder == null && other.decoder == null) ||
              (this.decoder != null && this.decoder.equals(other.decoder));
    retVal &= (this.cre == null && other.cre == null) ||
              (this.cre != null && this.cre.equals(other.cre));
    retVal &= (this.allegianceOverride == other.allegianceOverride);
    return retVal;
  }
}
