// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.infinity.datatype.EffectType;
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

/**
 * Provides useful information about a creature resource and their equipment.
 */
public class CreatureInfo
{
  /** Value to disable allegiance override. */
  public static final int ALLEGIANCE_OVERRIDE_NONE = -1;

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
  private final ColorInfo colorInfo = new ColorInfo();  // storage for color-related effects applied to the creature
  private final SpriteDecoder decoder;
  private final CreResource cre;

  private int allegianceOverride;

  public CreatureInfo(SpriteDecoder decoder, CreResource cre) throws Exception
  {
    this.decoder = Objects.requireNonNull(decoder, "SpriteDecoder instance cannot be null");
    this.cre = Objects.requireNonNull(cre, "CRE resource cannot be null");
    this.allegianceOverride = ALLEGIANCE_OVERRIDE_NONE;
    init();
  }

  /** Returns the {@code CreResource} instance of the creature resource. */
  public CreResource getCreResource() { return cre; }

  /** Returns creature flags. */
  public int getFlags() { return ((IsNumeric)cre.getAttribute(CreResource.CRE_FLAGS)).getValue(); }

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
        retVal = Math.min(v, 255);
      }
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
    int retVal = ALLEGIANCE_OVERRIDE_NONE;
    if (allowOverride) {
      retVal = getAllegianceOverride();
    }
    if (retVal == ALLEGIANCE_OVERRIDE_NONE) {
      retVal = ((IsNumeric)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
    }
    return retVal;
  }

  /** Returns the overridden allegiance. Returns {@link #ALLEGIANCE_OVERRIDE_NONE} if allegiance has not been overridden. */
  public int getAllegianceOverride()
  {
    return allegianceOverride;
  }

  /**
   * Overrides the creature's allegiance.
   * @param allegiance new allegiance of the creature. Uses the same values as defined in EA.IDS.
   *                   Specify {@link #ALLEGIANCE_OVERRIDE_NONE} to disable.
   */
  public void setAllegianceOverride(int allegiance)
  {
    allegiance = Math.max(ALLEGIANCE_OVERRIDE_NONE, Math.min(255, allegiance));
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

  /**
   * Provides access to the {@link ColorInfo} instance which contains color definitions
   * set by effects applied to the creature.
   */
  public ColorInfo getColorInfo() { return colorInfo; }

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
      for (Iterator<Integer> iter = itemInfo.getColorInfo().getLocationIterator(type); iter.hasNext(); ) {
        set.add(iter.next());
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
      retVal = itemInfo.getColorInfo().getValue(type, locationIndex);
    }

    return retVal;
  }

  /**
   * Returns the color entry of the specified location index for the avatar sprite
   * after applying all equipment and effect colors.
   * Returns -1 if color entry is not available.
   */
  public int getEffectiveColorValue(int locationIndex)
  {
    return getEffectiveColorValue(SegmentDef.SpriteType.AVATAR, locationIndex);
  }

  /**
   * Returns the color entry of the location index for the specified sprite overlay type
   * after applying all equipment and effect colors.
   * Returns -1 if color entry is not available.
   */
  public int getEffectiveColorValue(SegmentDef.SpriteType type, int locationIndex)
  {
    // using creature color by default
    int retVal = getColorValue(SegmentDef.SpriteType.AVATAR, locationIndex);

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    // checking equipped items
    ItemInfo[] itemInfos = getEffectiveItemInfo();
    for (final ItemInfo info : itemInfos) {
      int v = info.getColorInfo().getValue(type, locationIndex);
      if (v >= 0) {
        retVal = v;
      }
    }

    // checking creature effects
    int v = getColorInfo().getValue(type, locationIndex);
    if (v >= 0) {
      retVal = v;
    }

    return retVal;
  }


  /** Returns the creature resource version. */
  private String getCreatureVersion()
  {
    return ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText().toUpperCase(Locale.ENGLISH);
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
        initV10();
        break;
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
    int selectedWeaponSlot = 38;
    if (Profile.getGame() == Profile.Game.PSTEE) {
      Misc.requireCondition("V1.0".equalsIgnoreCase(((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText()),
                            "CRE version not supported");
      int numSlots = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEM_SLOTS)).getValue();
      if (numSlots > 0) {
        selectedWeaponSlot = numSlots - 1;
      }
    }

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

    initEquipment(slotMap, selectedWeaponSlot);
  }

  // initialize PST-style creature resources
  private void initV12() throws Exception
  {
    // initializing equipment
    int selectedWeaponSlot = 46;
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

    initEquipment(slotMap, selectedWeaponSlot);
  }

  // initialize IWD2-style creature resources
  private void initV22() throws Exception
  {
    // initializing equipment
    initEquipmentV22();
  }

  // initialize IWD-style creature resources
  private void initV90() throws Exception
  {
    // initializing equipment
    int selectedWeaponSlot = 38;
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

    initEquipment(slotMap, selectedWeaponSlot);
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
    if (as == null) {
      return;
    }
    StructEntry se = as.getField(EffectType.class, 0);
    if (!(se instanceof EffectType)) {
      return;
    }

    int fxType = (as instanceof Effect2) ? 1 : 0;

    int opcode = ((EffectType)se).getValue();
    if (opcode == 7) {
      int param1 = -1;
      int param2 = -1;
      if (fxType == 1) {
        // EFF V2
        se = as.getAttribute(0x14);
        if (se instanceof IsNumeric) {
          param1 = ((IsNumeric)se).getValue();
        }
        se = as.getAttribute(0x18);
        if (se instanceof IsNumeric) {
          param2 = ((IsNumeric)se).getValue();
        }
      } else {
        // EFF V1
        se = as.getAttribute(0x4);
        if (se instanceof IsNumeric) {
          param1 = ((IsNumeric)se).getValue();
        }
        se = as.getAttribute(0x8);
        if (se instanceof IsNumeric) {
          param2 = ((IsNumeric)se).getValue();
        }
      }

      if (param1 != -1 && param2 != -1) {
        SegmentDef.SpriteType type = null;
        int location = param2 & 0xf;
        switch ((param2 >> 4) & 0xf) {
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
            if ((param2 & 0xff) == 0xff) {
              // affect all sprite colors
              type = SegmentDef.SpriteType.AVATAR;
              location = -1;
            }
        }
        getColorInfo().add(type, location, param1);
      }
    }
  }

  private void initEquipment(HashMap<ItemSlots, Integer> slotMap, int selectedWeaponSlot)
  {
    List<StructEntry> itemList = cre.getFields(Item.class);
    int ofsSlots = cre.getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue();
    for (HashMap.Entry<ItemSlots, Integer> entry : slotMap.entrySet()) {
      ItemSlots slot = entry.getKey();
      int slotIdx = entry.getValue().intValue();
      int itemIdx = -1;
      if (slot == ItemSlots.WEAPON) {
        // special: determine weapon slot
        int weaponIdx = ((IsNumeric)cre.getAttribute(ofsSlots + selectedWeaponSlot * 2)).getValue();
        if (weaponIdx == 1000) {
          // selected weapon: fists
          itemIdx = weaponIdx;
        } else {
          weaponIdx = Math.max(0, Math.min(4, weaponIdx));
          for (int idx = weaponIdx; idx >= 0; idx--) {
            itemIdx = ((IsNumeric)cre.getAttribute(ofsSlots + (slotIdx + idx) * 2)).getValue();
            if (itemIdx >= 0 && itemIdx < itemList.size()) {
              break;
            }
          }
        }
      } else {
        itemIdx = ((IsNumeric)cre.getAttribute(ofsSlots + slotIdx * 2)).getValue();
      }

      initEquipmentItem(slot, itemIdx, itemList);
    }
  }

  private void initEquipmentV22()
  {
    int selectedWeaponSlot = 50;
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

    List<StructEntry> itemList = cre.getFields(Item.class);
    int ofsSlots = cre.getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue();
    for (HashMap.Entry<ItemSlots, Integer> entry : slotMap.entrySet()) {
      ItemSlots slot = entry.getKey();
      int slotIdx = entry.getValue().intValue();
      int itemIdx = -1;
      if (slot == ItemSlots.WEAPON || slot == ItemSlots.SHIELD) {
        // special: weapon/shield combinations are grouped and determined by selected weapon slot
        int weaponIdx = ((IsNumeric)cre.getAttribute(ofsSlots + selectedWeaponSlot * 2)).getValue();
        if (weaponIdx == 1000) {
          // selected weapon: fists
          itemIdx = weaponIdx;
        } else {
          weaponIdx = Math.max(0, Math.min(4, weaponIdx));
          for (int idx = weaponIdx; idx >= 0; idx--) {
            itemIdx = ((IsNumeric)cre.getAttribute(ofsSlots + (slotIdx + idx * 2) * 2)).getValue();
            if (itemIdx >= 0 && itemIdx < itemList.size()) {
              break;
            }
          }
        }
      } else {
        itemIdx = ((IsNumeric)cre.getAttribute(ofsSlots + slotIdx * 2)).getValue();
      }

      initEquipmentItem(slot, itemIdx, itemList);
    }
  }

  private void initEquipmentItem(ItemSlots slot, int itemIndex, List<StructEntry> itemList)
  {
    ResourceEntry itmEntry = null;
    if (itemIndex == 1000) {
      // weapon: fists
      itmEntry = getFistWeapon();
    } else if (itemIndex >= 0 && itemIndex < itemList.size()) {
      if (itemList.get(itemIndex) instanceof Item) {
        String itmResref = ((IsTextual)((Item)itemList.get(itemIndex)).getAttribute(Item.CRE_ITEM_RESREF)).getText();
        itmEntry = ResourceFactory.getResourceEntry(itmResref + ".ITM");
      }
    }

    if (itmEntry != null) {
      try {
        ItemInfo itemInfo = new ItemInfo(itmEntry);
        equipment.put(slot, itemInfo);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
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

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((equipment == null) ? 0 : equipment.hashCode());
    hash = 31 * hash + ((colorInfo == null) ? 0 : colorInfo.hashCode());
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
    retVal &= (this.colorInfo == null && other.colorInfo == null) ||
              (this.colorInfo != null && this.colorInfo.equals(other.colorInfo));
    retVal &= (this.decoder == null && other.decoder == null) ||
              (this.decoder != null && this.decoder.equals(other.decoder));
    retVal &= (this.cre == null && other.cre == null) ||
              (this.cre != null && this.cre.equals(other.cre));
    retVal &= (this.allegianceOverride == other.allegianceOverride);
    return retVal;
  }
}
