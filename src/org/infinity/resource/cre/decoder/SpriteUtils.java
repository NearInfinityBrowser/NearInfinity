// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.graphics.BamV1Decoder;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;
import org.infinity.resource.itm.Ability;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

/**
 * Collection of helpful methods for Sprite rendering.
 */
public class SpriteUtils
{
  /** Cache for structured game resources */
  private static final HashMap<ResourceEntry, AbstractStruct> resourceCache = new HashMap<>();
  /** Cache for source BAM resources (decoder and attached controller). */
  private static final HashMap<ResourceEntry, Couple<BamV1Decoder, BamV1Decoder.BamV1Control>> bamCache = new HashMap<>();
  /** Cache for replacement palettes. */
  private static final HashMap<ResourceEntry, int[]> paletteCache = new HashMap<>();
  /** Cache for color gradients. */
  private static final HashMap<Integer, int[]> colorGradients = new HashMap<>(350);
  /** Cache for randomized color gradients. */
  private static final HashMap<Integer, int[]> randomGradientIndices = new HashMap<>();
  /** A random number generator for general use */
  private static final Random random = new Random();

  /** A stable pool of random numbers. */
  private static int[] randomPool;

  /** Clears cached resources. */
  public static void clearCache()
  {
    resourceCache.clear();
    bamCache.clear();
    paletteCache.clear();
    colorGradients.clear();
    randomGradientIndices.clear();
  }

  /**
   * Creates a pseudo CRE resource with the specified animation id and an optional set of creature colors.
   * @param animationId The animation id for the CRE.
   * @param colors Optional creature colors. Each entry uses the CRE_COLOR_xxx field names defined in {@link CreResource}
   *               associated with the numeric color value.
   * @param equipment Optional equipment. Each entry uses inventory slot index (as defined in CRE) as key and
   *                  item resref as value.
   *                  These items are only used as source for overlay bams (i.e. weapons, shields, helmets).
   * @return A {@code CreResource} instance with the virtual creature data.
   */
  public static CreResource getPseudoCre(int animationId, HashMap<String, Integer> colors, HashMap<Integer, String> equipment) throws Exception
  {
    CreResource entry = null;
    ByteBuffer buffer = null;
    if ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V90)) {
      // IWD
      int sizeBase = 0x33c;
      int slotsSize = 0x50; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V9.0".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          switch (e.getKey()) {
            case CreResource.CRE_COLOR_METAL:
              buffer.put(0x2c, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MINOR:
              buffer.put(0x2d, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MAJOR:
              buffer.put(0x2e, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_SKIN:
              buffer.put(0x2f, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_LEATHER:
              buffer.put(0x30, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_ARMOR:
              buffer.put(0x31, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_HAIR:
              buffer.put(0x32, e.getValue().byteValue());
              break;
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x2d8, (byte)128);
      // setting valid offsets
      for (int ofs : new int[] {0x308, 0x310, 0x318, 0x320, 0x324, 0x32c}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    } else if ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V22)) {
      // IWD2
      int sizeBase = 0x62e;
      int slotsSize = 0x68; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V2.2".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          switch (e.getKey()) {
            case CreResource.CRE_COLOR_METAL:
              buffer.put(0x2c, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MINOR:
              buffer.put(0x2d, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MAJOR:
              buffer.put(0x2e, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_SKIN:
              buffer.put(0x2f, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_LEATHER:
              buffer.put(0x30, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_ARMOR:
              buffer.put(0x31, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_HAIR:
              buffer.put(0x32, e.getValue().byteValue());
              break;
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x384, (byte)128);
      // setting valid offsets
      for (int i = 0; i < 63; i++) {
        buffer.putInt(0x3ba + (i * 4), sizeBase);
      }
      for (int i = 0; i < 9; i++) {
        buffer.putInt(0x5b2+ (i * 4), sizeBase);
      }
      for (int ofs : new int[] {0x5fa, 0x602, 0x60a, 0x612, 0x616, 0x61e}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    } else if ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V12)) {
      // PST
      int sizeBase = 0x378;
      int slotsSize = 0x60; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V1.2".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          for (int i = 0; i < 7; i++) {
            String labelColor = String.format(CreResource.CRE_COLOR_FMT, i + 1);
            String labelColorPlacement = String.format(CreResource.CRE_COLOR_PLACEMENT_FMT, i + 1);
            if (labelColor.equals(e.getKey())) {
              buffer.putShort(0x2e4 + (i * 2), e.getValue().shortValue());
            } else if (labelColorPlacement.equals(e.getKey())) {
              buffer.put(0x2f5 + i, e.getValue().byteValue());
            }
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x314, (byte)128);
      // setting valid offsets
      for (int ofs : new int[] {0x344, 0x34c, 0x354, 0x35c, 0x360, 0x368}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    } else {
      // BG, BG2, EE
      int sizeBase = 0x2d4;
      int slotsSize = 0x50; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V1.0".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          switch (e.getKey()) {
            case CreResource.CRE_COLOR_METAL:
              buffer.put(0x2c, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MINOR:
              buffer.put(0x2d, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MAJOR:
              buffer.put(0x2e, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_SKIN:
              buffer.put(0x2f, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_LEATHER:
              buffer.put(0x30, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_ARMOR:
              buffer.put(0x31, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_HAIR:
              buffer.put(0x32, e.getValue().byteValue());
              break;
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x270, (byte)128);
      // setting valid offsets
      for (int ofs : new int[] {0x2a0, 0x2a8, 0x2b0, 0x2b8, 0x2bc, 0x2c4}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    }

    if (buffer != null) {
      buffer.putInt(0x08, -1);  // creature name
      buffer.putInt(0x0c, -1);  // creature tooltip
      buffer.putInt(0x28, animationId);
      if (equipment != null) {
        for (final HashMap.Entry<Integer, String> itm : equipment.entrySet()) {
          addPseudoCreItem(buffer, itm.getKey().intValue(), itm.getValue());
        }
      }

      entry = new CreResource(null, String.format("%04X", animationId & 0xffff), buffer, 0);
    }

    return entry;
  }

  /**
   * Adds a new item to the pseudo CRE resource and assigns it to an item slot.
   * @param buffer the source CRE buffer.
   * @param slot slot to assign the new item. Specify negative value to skip.
   * @param resref The item resref.
   * @return the updated CRE buffer.
   */
  private static ByteBuffer addPseudoCreItem(ByteBuffer buffer, int slot, String resref)
  {
    ByteBuffer outBuffer = buffer;
    if (buffer == null || resref == null || resref.isEmpty()) {
      return outBuffer;
    }

    // preparing item entry
    ByteBuffer item = StreamUtils.getByteBuffer(20);
    item.position(0);
    item.put(resref.getBytes(Misc.CHARSET_ASCII));

    int numSlots = 0, ofsSlotsOffset = 0, ofsItemsCount = 0;
    String ver = StreamUtils.readString(buffer, 4, 4);
    switch (ver) {
      case "V9.0":
        numSlots = 38;
        ofsSlotsOffset = 0x320;
        ofsItemsCount = 0x328;
        break;
      case "V2.2":
        numSlots = 50;
        ofsSlotsOffset = 0x612;
        ofsItemsCount = 0x61a;
        break;
      case "V1.2":
        numSlots = 46;
        ofsSlotsOffset = 0x35c;
        ofsItemsCount = 0x364;
        break;
      case "V1.0":
        numSlots = 38;
        ofsSlotsOffset = 0x2b8;
        ofsItemsCount = 0x2c0;
        break;
    }

    if (ofsSlotsOffset > 0) {
      outBuffer = StreamUtils.getByteBuffer(buffer.limit() + item.limit());
      int ofsSlots = buffer.getInt(ofsSlotsOffset);
      int numItems = buffer.getInt(ofsItemsCount);
      outBuffer.position(0);
      // adding CRE base
      outBuffer.put(buffer.array(), 0, ofsSlots);
      // adding new item
      outBuffer.put(item.array(), 0, item.limit());
      // adding CRE inventory slots
      outBuffer.put(buffer.array(), ofsSlots, buffer.limit() - ofsSlots);
      // updating items count
      numItems++;
      outBuffer.putInt(ofsItemsCount, numItems);
      // updating slots offset
      ofsSlots += item.limit();
      outBuffer.putInt(ofsSlotsOffset, ofsSlots);
      // assigning item to slot
      if (slot >= 0 && slot < numSlots) {
        outBuffer.putShort(ofsSlots + (slot * 2), (short)(numItems - 1));
      }
    }

    return outBuffer;
  }

  /**
   * Attempts to fetch the requested resource structure from cache before creating a new instance.
   * @param <T> the desired type of the return value.
   * @param classType the specific class type of the resource entry.
   * @param entry the resource entry to load.
   * @return a structured resource of type {@code T} if successful. Returns {@code null} otherwise.
   * @throws Exception propagated exception from the instantiation process.
   */
  @SuppressWarnings("unchecked")
  public static <T extends AbstractStruct> T loadResource(Class<? extends AbstractStruct> classType, ResourceEntry entry) throws Exception
  {
    T retVal = null;
    if (classType == null || entry == null) {
      return retVal;
    }

    AbstractStruct as = resourceCache.get(entry);
    if (as == null) {
      try {
        Constructor<? extends AbstractStruct> ctor = classType.getConstructor(ResourceEntry.class);
        if (ctor != null) {
          as = ctor.newInstance(entry);
          resourceCache.put(entry, as);
        }
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      }
    }

    if (classType.isAssignableFrom(as.getClass())) {
      retVal = (T)as;
    }

    return retVal;
  }

  /**
   * Returns whether the specified item is a two-handed weapon.
   * @param itm the item resource
   * @param meleeOnly indicates whether to consider only melee weapons.
   * @return {@code true} if the item is a valid two-handed weapon. This also includes fake two-handed weapons in EE games.
   *         Returns {@code false} otherwise.
   */
  public static boolean isWeaponTwoHanded(ItmResource itm, boolean meleeOnly)
  {
    boolean retVal = false;
    if (itm == null) {
      return retVal;
    }

    int flags = ((IsNumeric)itm.getAttribute(ItmResource.ITM_FLAGS)).getValue();
    int mask = 1 << 1;  // two-handed
    if (Profile.isEnhancedEdition()) {
      mask |= 1 << 12;  // fake two-handed
    }
    retVal = (flags & mask) != 0;

    String appearance = ((IsTextual)itm.getAttribute(ItmResource.ITM_EQUIPPED_APPEARANCE)).getText().trim();
    retVal &= !appearance.isEmpty();

    if (retVal && meleeOnly) {
      int abilType = -1;
      int numAbil = ((IsNumeric)itm.getAttribute(ItmResource.ITM_NUM_ABILITIES)).getValue();
      if (numAbil > 0) {
        int ofsAbil = ((IsNumeric)itm.getAttribute(ItmResource.ITM_OFFSET_ABILITIES)).getValue();
        StructEntry se = itm.getAttribute(ofsAbil, true);
        if (se instanceof IsNumeric) {
          abilType = ((IsNumeric)se).getValue();
        }
      }
      retVal &= (abilType == 1);  // ability type "melee"
    }

    return retVal;
  }

  /**
   * Returns whether the specified item is a weapon that can be wielded with the left hand.
   * @param itm the item resource
   * @return {@code true} if the weapon can be wielded with the left hand. Returns {@code false} otherwise.
   */
  public static boolean isLeftHandedWeapon(ItmResource itm)
  {
    boolean retVal = false;
    if (itm != null) {
      int flags = ((IsNumeric)itm.getAttribute(ItmResource.ITM_FLAGS)).getValue();
      boolean isTwoHanded = (flags & (1 << 1)) != 0;  // two-handed
      boolean allowLeftHanded = !Profile.isEnhancedEdition() || ((flags & (1 << 13)) == 0);
      String appearance = ((IsTextual)itm.getAttribute(ItmResource.ITM_EQUIPPED_APPEARANCE)).getText().trim();
      int abilType = -1;
      int numAbil = ((IsNumeric)itm.getAttribute(ItmResource.ITM_NUM_ABILITIES)).getValue();
      if (numAbil > 0) {
        int ofsAbil = ((IsNumeric)itm.getAttribute(ItmResource.ITM_OFFSET_ABILITIES)).getValue();
        StructEntry se = itm.getAttribute(ofsAbil, true);
        if (se instanceof IsNumeric) {
          abilType = ((IsNumeric)se).getValue();
        }
        // eligible left-handed weapons: not two-handed; left-handed allowed; has equipped appearance; ability type = Melee
        retVal = !isTwoHanded && allowLeftHanded && !appearance.isEmpty() && (abilType == 1);
      }
    }
    return retVal;
  }


  /**
   * Returns the two-letter appearance code for the specified item.
   * @param itm The item resource.
   * @return A two-letter string indicating the item appearance for creature animations. Returns an empty string otherwise.
   */
  public static String getItemAppearance(ItmResource itm)
  {
    String retVal = "";
    if (itm != null) {
      retVal = ((IsTextual)itm.getAttribute(ItmResource.ITM_EQUIPPED_APPEARANCE)).getText().toUpperCase(Locale.ENGLISH);
      if (retVal.length() > 2) {
        retVal = retVal.substring(0, 2);
      } else {
        while (retVal.length() < 2) {
          retVal += " ";
        }
      }
    }
    return retVal;
  }

  /**
   * Returns the index of the selected weapon ability.
   * @param cre the CRE resource.
   * @return ability index. Returns 0 if ability could not be determined.
   */
  public static int getEquippedWeaponAbility(CreResource cre)
  {
    if (cre != null) {
      return ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_ABILITY)).getValue();
    }
    return 0;
  }

  /**
   * Determines the absolute item slot index of the specified weapon-related slot id.
   * Special slots that cannot be mapped to slot indices are returned as -1.
   * Only weapon-related slots are considered, which includes the actual weapon slot ids as well as the ammo ids.
   * @param cre the CRE resource
   * @param slotId the slot id (as defined in SLOTS.IDS)
   * @return absolute item slot index. Returns 1000 for fist slot. Returns -1 if slot id could not be mapped to a slot index.
   */
  public static int getWeaponSlotIndex(CreResource cre, int slotId)
  {
    int retVal = -1;
    if (slotId == 1000) {
      // fist weapon
      return slotId;
    }

    String creVer = ((IsTextual)Objects.requireNonNull(cre).getAttribute(CreResource.COMMON_VERSION)).getText().toUpperCase(Locale.ENGLISH);
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

  /**
   * Analyses the item at the specified slot index and returns the index if the item can be used directly.
   * If the item requires a launcher the method scans the weapon slots of the specified CRE resource and
   * returns the slot index of a matching launcher item.
   * @param cre the CRE resource
   * @param slotIndex the absolute slot index of the item to check
   * @return the absolute slot index of the effective weapon. Returns {@code -1} if no weapon could be determined.
   */
  public static int getEffectiveWeaponIndex(CreResource cre, int slotIndex)
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
    ItmResource itm = null;
    int ofsItems = Objects.requireNonNull(cre).getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEMS)).getValue();
    try {
      String itmResref = ((IsTextual)cre.getAttribute(ofsItems + itmIndex * 20, true)).getText();
      itm = loadResource(ItmResource.class, ResourceFactory.getResourceEntry(itmResref + ".ITM"));
    } catch (Exception e) {
      return retVal;
    }

    // check if item requires a launcher
    int abilityIndex = ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_ABILITY)).getValue();
    int numAbil = ((IsNumeric)Objects.requireNonNull(itm).getAttribute(ItmResource.ITM_NUM_ABILITIES)).getValue();
    abilityIndex = Math.min(abilityIndex, numAbil - 1);
    if (abilityIndex < 0) {
      return retVal;
    }
    int ofsAbil = ((IsNumeric)itm.getAttribute(ItmResource.ITM_OFFSET_ABILITIES)).getValue();
    Ability ability = itm.getAttribute(ofsAbil + abilityIndex * 0x38, Ability.class, false);
    if (ability == null) {
      return retVal;
    }
    int launcherType = ((IsNumeric)ability.getAttribute(Ability.ITM_ABIL_LAUNCHER_REQUIRED)).getValue();
    if (launcherType == 0) {
      // item can be used directly
      retVal = slotIndex;
    }

    if (retVal < 0) {
      // launcher required: find a weapon in weapon slots 1-4 with a matching item category
      String creVer = ((IsTextual)Objects.requireNonNull(cre).getAttribute(CreResource.COMMON_VERSION)).getText().toUpperCase(Locale.ENGLISH);
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
   * Returns the active weapon of the specified creature.
   * @param cre The CRE resource.
   * @return The ITM resource of the active weapon. Returns {@code null} if no weapon is active.
   * @throws NullPointerException if no creature resource specified.
   */
  public static ItmResource getEquippedWeapon(CreResource cre)
  {
    ItmResource retVal = null;

    // find selected weapon slot and determine the associated item
    int slotIndex = getWeaponSlotIndex(cre, ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_SLOT)).getValue());
    if (slotIndex == 1000) {
      // fist weapon
      retVal = getFistWeapon(cre);
    } else {
      if (slotIndex < 0) {
        return retVal;
      }

      int weaponSlotIdx = getEffectiveWeaponIndex(cre, slotIndex);
      if (weaponSlotIdx < 0) {
        return retVal;
      }

      retVal = getInventoryItem(cre, weaponSlotIdx);
    }
    return retVal;
  }

  /**
   * Returns the equipped helmet of the specified creature.
   * @param cre The CRE resource.
   * @return The ITM resource of the helmet. Returns {@code null} if no helmet is equipped.
   * @throws NullPointerException if no creature resource specified.
   */
  public static ItmResource getEquippedHelmet(CreResource cre)
  {
    if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
      // PST does not support helmets
      return null;
    }

    ItmResource retVal = getInventoryItem(cre, 0);
    if (retVal != null) {
      // checking item category
      switch (((IsNumeric)retVal.getAttribute(ItmResource.ITM_CATEGORY)).getValue()) {
        case 7:   // Headgear
        case 72:  // Hats
          break;
        default:
          retVal = null;
      }
    }
    return retVal;
  }

  /**
   * Returns the equipped shield or left-handed weapon of the specified creature.
   * @param cre The CRE resource.
   * @return The ITM resource of the shield or left-handed weapon. Returns {@code null} if left hand is empty.
   * @throws NullPointerException if no creature resource specified.
   */
  public static ItmResource getEquippedShield(CreResource cre)
  {
    ItmResource retVal = null;
    if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
      // PST does not support shields
      return retVal;
    }

    String creVer = ((IsTextual)cre.getAttribute(AbstractStruct.COMMON_VERSION)).getText().toUpperCase(Locale.ENGLISH);
    if ("V2.2".equals(creVer)) {
      int selectedSlot = ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_SLOT)).getValue();
      int itemSlotIdx = getWeaponSlotIndex(cre, selectedSlot);
      int weaponSlotIdx = getEffectiveWeaponIndex(cre, itemSlotIdx);
      if (weaponSlotIdx >= 0 && weaponSlotIdx < 1000) {
        // IWD2 uses weapon/shield slot combinations
        retVal = getInventoryItem(cre, weaponSlotIdx + 1);
      }
    } else {
      retVal = getInventoryItem(cre, 2);
    }

    return retVal;
  }

  /**
   * Returns the equipped armor or robe of the specified creature.
   * @param cre The CRE resource.
   * @return The ITM resource of armor or robe. Returns {@code null} if no armor is equipped.
   */
  public static ItmResource getEquippedArmor(CreResource cre)
  {
    return getInventoryItem(cre, 1);
  }

  /**
   * Returns the item resource at the specified item slot of the given creature.
   * @param cre the CRE resource
   * @param slotIndex absolute slot index of the requested item
   * @return the ITM resource. Returns {@code null} if item could not be determined.
   */
  public static ItmResource getInventoryItem(CreResource cre, int slotIndex)
  {
    ItmResource retVal = null;
    if (cre == null || slotIndex < 0) {
      return retVal;
    }

    // determining number of item slots
    String creVer = ((IsTextual)cre.getAttribute(AbstractStruct.COMMON_VERSION)).getText().toUpperCase(Locale.ENGLISH);
    int numSlots = 0;
    switch (creVer) {
      case "V1.2":
        numSlots = 48;
        break;
      case "V2.2":
        numSlots = 52;
        break;
      default:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          numSlots = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEM_SLOTS)).getValue();
        }
        if (numSlots == 0) {
          numSlots = 40;
        }
    }

    if (numSlots == 0 || slotIndex >= numSlots) {
      return retVal;
    }

    // loading referenced item
    try {
      int ofsSlots = cre.getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue();
      int itemIndex = ((IsNumeric)cre.getAttribute(ofsSlots + slotIndex * 2)).getValue();
      if (itemIndex >= 0) {
        int numItems = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEMS)).getValue();
        if (itemIndex < numItems) {
          int ofsItems = cre.getExtraOffset() + ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEMS)).getValue();
          String itemRef = ((IsTextual)cre.getAttribute(ofsItems + itemIndex * 20, true)).getText();
          retVal = loadResource(ItmResource.class, ResourceFactory.getResourceEntry(itemRef + ".ITM"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return retVal;
  }

  /** Attempts to find the virtual "fist" weapon item used for barehanded attacks. */
  public static ItmResource getFistWeapon(CreResource cre)
  {
    ItmResource retVal = null;

    int animId = ((IsNumeric)cre.getAttribute(CreResource.CRE_ANIMATION)).getValue();
    if ((animId & 0xff00) == 0x6500) {
      // monk: return fist item
      int level = ((IsNumeric)cre.getAttribute(CreResource.CRE_LEVEL_FIRST_CLASS)).getValue();

      // 1. try 2DA lookup
      Table2da table = Table2daCache.get("MONKFIST.2DA");
      if (table != null) {
        level = Math.max(1, Math.min(table.getRowCount(), level));
        String resref = table.get(level, 1);
        ResourceEntry entry = ResourceFactory.getResourceEntry(resref + ".ITM");
        try {
          retVal = loadResource(ItmResource.class, entry);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (retVal == null) {
        // 2. try hardcoded selection
        String resref = null;
        int[] minLevels = { Integer.MIN_VALUE, 3, 6, 9, 12, 15, 19, 25, Integer.MAX_VALUE };
        for (int i = 0; i < minLevels.length && resref == null; i++) {
          if (level < minLevels[i]) {
            resref = "MFIST" + i;
            ResourceEntry entry = ResourceFactory.getResourceEntry(resref + ".ITM");
            try {
              retVal = loadResource(ItmResource.class, entry);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    if (retVal == null) {
      // return default fist
      ResourceEntry entry = ResourceFactory.getResourceEntry("FIST.ITM");
      try {
        retVal = loadResource(ItmResource.class, entry);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }


  /**
   * Loads the decoder instance for the specified BAM V1 resource. Retrieves the decoder from cache if available.
   * @param entry the BAM resource entry.
   * @return the {@code BamV1Decoder} instance created from the BAM resource.
   *         Returns {@code null} if decoder could not be retrieved.
   */
  public static BamV1Decoder loadBamDecoder(ResourceEntry entry)
  {
    Couple<BamV1Decoder, BamV1Decoder.BamV1Control> retVal = loadBamDecoderController(entry);
    return (retVal != null) ? retVal.getValue0() : null;
  }

  /**
   * Loads the BAM controller instance for the specified BAM V1 resource. Retrieves the object from cache if available.
   * @param entry the BAM resource entry.
   * @return the {@code BamV1Control} instance created from the BAM resource.
   *         Returns {@code null} if the controller could not be retrieved.
   */
  public static BamV1Decoder.BamV1Control loadBamController(ResourceEntry entry)
  {
    Couple<BamV1Decoder, BamV1Decoder.BamV1Control> retVal = loadBamDecoderController(entry);
    return (retVal != null) ? retVal.getValue1() : null;
  }

  /**
   * Loads the decoder instance and an associated controller for the specified BAM V1 resource. Retrieves the objects
   * from cache if available.
   * @param entry the BAM resource entry.
   * @return {@code BamV1Decoder} and {@code BamV1Control} instances created from the BAM resource.
   *         Returns {@code null} if the objects could not be retrieved.
   */
  public static Couple<BamV1Decoder, BamV1Decoder.BamV1Control> loadBamDecoderController(ResourceEntry entry)
  {
    Couple<BamV1Decoder, BamV1Decoder.BamV1Control> retVal = bamCache.getOrDefault(entry, null);
    if (retVal == null) {
      try {
        BamV1Decoder decoder = new BamV1Decoder(entry);
        BamV1Decoder.BamV1Control control = decoder.createControl();
        retVal = Couple.with(decoder, control);
        bamCache.put(entry, retVal);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Returns whether the specified BAM V1 resource exists and contains the specified  range of cycles.
   * @param entry the BAM resource to check.
   * @param cycle start index of cycle range.
   * @param length number of cycles.
   * @return {@code true} if the BAM resource exists, is paletted and contains the specified cycles.
   */
  public static boolean bamCyclesExist(ResourceEntry entry, int cycle, int length)
  {
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      int numCycles = control.cycleCount();
      return (numCycles >= cycle + length && control.cycleFrameCount(cycle) > 0);
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Returns the number of cycles available in the specified BAM resource.
   * @param entry BAM resource to check.
   * @return number of cycles. Returns 0 if number of cycles could not be determined.
   */
  public static int getBamCycles(ResourceEntry entry)
  {
    int retVal = 0;
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      retVal = control.cycleCount();
    } catch (Exception e) {
    }
    return retVal;
  }

  /**
   * Returns the number of frames available in the specified BAM cycle.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle to check.
   * @return number of frames in the specified BAM cycle. Returns 0 if number of frames could not be determined.
   */
  public static int getBamCycleFrames(ResourceEntry entry, int cycleIdx)
  {
    int retVal = 0;
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      if (cycleIdx >= 0 && cycleIdx < control.cycleCount()) {
        retVal = control.cycleFrameCount(cycleIdx);
      }
    } catch (Exception e) {
    }
    return retVal;
  }

  /**
   * Returns whether the specified BAM contains a palette that is most likely replaced by external color ranges.
   * @param entry BAM resource to check
   * @return {@code true} if BAM contains a false color palette, {@code false} otherwise.
   */
  public static boolean bamHasFalseColors(ResourceEntry entry)
  {
    boolean retVal = false;
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      int[] palette = control.getCurrentPalette();
      if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
        retVal = (palette[224] & 0xffffff) == 0x0000ff;
        retVal &= (palette[240] & 0xffffff) == 0x00009f;
      } else {
        retVal = (palette[15] & 0xffffff) == 0x1e1e1e;
        retVal &= (palette[27] & 0xffffff) == 0x004040;
        retVal &= (palette[39] & 0xffffff) == 0x400040;
        retVal &= (palette[51] & 0xffffff) == 0x404000;
        retVal &= (palette[63] & 0xffffff) == 0x400000;
        retVal &= (palette[75] & 0xffffff) == 0x000040;
        retVal &= (palette[87] & 0xffffff) == 0x004000;
      }
    } catch (Exception e) {
    }
    return retVal;
  }

  /**
   * Ensures that "transparent" and "shadow" palette entries of the animation are properly set.
   * @param control the BAM controller associated with the animation.
   */
  public static void fixShadowColor(BamV1Control control)
  {
    if (control != null) {
      int[] palette = control.getCurrentPalette();
      palette[0] = 0x0000FF00;
      palette[1] = 0x80000000;
      control.setExternalPalette(palette);
    }
  }

  /**
   * Applies the specified palette to the BAM animation associated with the specified controller.
   * @param control the BAM controller associated with the animation.
   * @param palette the new palette data.
   */
  public static void applyNewPalette(BamV1Control control, int[] palette)
  {
    if (palette == null) {
      return;
    }
    if (palette.length < 256) {
      palette = Arrays.copyOf(palette, 256);
    }
    control.setExternalPalette(palette);
  }

  /**
   * Loads the palette specified by the resource reference.
   * It looks for a palette in the following order: [resref].BMP, [resref].BAM.
   * @param resref the resource reference to use for loading a palette.
   * @return palette data with 256 entries. Returns {@code null} if palette could not be loaded.
   */
  public static int[] loadReplacementPalette(String resref)
  {
    return loadReplacementPalette(resref, -1);
  }

  /**
   * Loads the palette specified by the resource reference and index.
   * It looks for a palette in the following order: [resref+suffix].BMP, [resref+suffix].BAM, [resref].BMP, [resref].BAM.
   * @param resref the resource reference to use for loading a palette.
   * @param index a numeric suffix added to the resref.
   * @return palette data with 256 entries. Returns {@code null} if palette could not be loaded.
   */
  public static int[] loadReplacementPalette(String resref, int index)
  {
    int[] retVal = null;
    if (resref == null || resref.isEmpty()) {
      return retVal;
    }

    String resName = resref;
    String suffix = (index >= 0) ? Integer.toString(index) : "";
    String[] suffixList = (suffix.isEmpty()) ? new String[] {""} : new String[] {suffix, ""};
    ResourceEntry entry = null;
    for (final String s : suffixList) {
      if (ResourceFactory.resourceExists(resName + s + ".BMP")) {
        entry = ResourceFactory.getResourceEntry(resName + s + ".BMP");
        break;
      }
    }
    if (entry == null) {
      return retVal;
    }

    retVal = paletteCache.getOrDefault(entry, null);
    if (retVal == null) {
      try {
        retVal = ColorConvert.loadPaletteBMP(entry);
        if (retVal != null) {
          if (retVal.length < 256) {
            retVal = Arrays.copyOf(retVal, 256);
          }
          paletteCache.put(entry, retVal);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }

  /**
   * Replaces a range of color entries in the palette with new entries.
   * @param palette the palette to modify.
   * @param newColors array of colors to add.
   * @param startOffset first color entry in {@code palette} to update.
   * @param len number of color entries to update. Parameter is adjusted if {@code newColors} array contains
   *            less entries than {@code len} or range would exceed palette size.
   * @param allowAlpha {@code true} to use alpha from {@code newColors}; {@code false} to discard alpha values.
   * @return the updated palette.
   */
  public static int[] replaceColors(int[] palette, int[] newColors, int startOffset, int len, boolean allowAlpha)
  {
    int[] retVal = palette;
    if (palette != null && newColors != null) {
      retVal = Arrays.copyOf(palette, palette.length);
      if (newColors.length > 0 && startOffset >= 0 && startOffset < retVal.length) {
        len = Math.min(len, newColors.length);
        len = Math.min(len, retVal.length - startOffset);
        int mask = allowAlpha ? 0xffffffff : 0x00ffffff;
        for (int i = 0; i < len; i++) {
          retVal[startOffset + i] = newColors[i] & mask;
        }
      }
    }
    return retVal;
  }

  /**
   * Interpolates two palette ranges and stores the result in a third palette range.
   * @param palette the palette to modify.
   * @param srcOfs1 start offset of first source color range.
   * @param srcOfs2 start offset of second source color range.
   * @param srcLen number of source color entries to read.
   * @param dstOfs start offset of target color range.
   * @param dstLen number of interpolated color entries to write.
   * @param allowAlpha {@code true} to interpolate alpha, {@code false} to discard alpha values.
   * @return the updated palette.
   */
  public static int[] interpolateColors(int[] palette, int srcOfs1, int srcOfs2, int srcLen,
                                        int dstOfs, int dstLen, boolean allowAlpha)
  {
    int[] retVal = palette;
    if (palette != null && srcLen > 0 && dstLen > 0 &&
        srcOfs1 >= 0 && srcOfs1 + srcLen <= palette.length &&
        srcOfs2 >= 0 && srcOfs2 + srcLen <= palette.length &&
        dstOfs >= 0 && dstOfs + dstLen <= palette.length) {
      retVal = Arrays.copyOf(palette, palette.length);
      for (int dstIdx = 0; dstIdx < dstLen; dstIdx++) {
        int srcIdx = dstIdx * srcLen / dstLen;
        int r = ((retVal[srcOfs1 + srcIdx] & 0xff) + (retVal[srcOfs2 + srcIdx] & 0xff)) >>> 1;
        int g = (((retVal[srcOfs1 + srcIdx] >> 8) & 0xff) + ((retVal[srcOfs2 + srcIdx] >> 8) & 0xff)) >>> 1;
        int b = (((retVal[srcOfs1 + srcIdx] >> 16) & 0xff) + ((retVal[srcOfs2 + srcIdx] >> 16) & 0xff)) >>> 1;
        int a = 0xff000000;
        if (allowAlpha) {
          a = (((retVal[srcOfs1 + srcIdx] >> 24) & 0xff) + ((retVal[srcOfs2 + srcIdx] >> 24) & 0xff)) >>> 1;
        }
        retVal[dstOfs + dstIdx] = (a << 24) | (b << 16) | (g << 8) | r;
      }
    }
    return retVal;
  }

  /**
   * Returns the specified color gradient. Colors are retrieved from the game-specific gradient resource.
   * Optionally takes random color entry definitions into account.
   * @param animType Indicates what type of color gradient should be loaded.
   * @param index Index of the color gradient.
   * @param allowRandom whether random color entries are taken into account.
   * @return the gradient as array of colors. Returns {@code null} if color index does not exist.
   */
  public static int[] getColorGradient(SpriteDecoder.AnimationType animType, int index, boolean allowRandom)
  {
    if (colorGradients.isEmpty()) {
      // initializing color gradient map on demand
      ResourceEntry palFile = null;
      if (animType == SpriteDecoder.AnimationType.MONSTER_PLANESCAPE) {
        palFile = ResourceFactory.getResourceEntry("PAL32.BMP");
      } else if (ResourceFactory.resourceExists("RANGES12.BMP")) {
        palFile = ResourceFactory.getResourceEntry("RANGES12.BMP");
      } else if (ResourceFactory.resourceExists("MPALETTE.BMP")) {
        palFile = ResourceFactory.getResourceEntry("MPALETTE.BMP");
      }

      if (palFile != null) {
        try {
          BufferedImage image = new GraphicsResource(palFile).getImage();
          for (int y = 0; y < image.getHeight(); y++) {
            int[] pixels = image.getRGB(0, y, image.getWidth(), 1, null, 0, image.getWidth());
            colorGradients.put(y, pixels);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    int[] retVal = allowRandom ? getRandomColorGradient(animType, index) : null;
    if (retVal == null) {
      retVal = colorGradients.getOrDefault(index, null);
    }

    return retVal;
  }

  /**
   * Attempts to resolve a random color entry to a real color entry.
   * @param index random color entry index.
   * @return a randomly chosen color gradient from the random color entry list.
   *         Returns {@code null} if no real color gradient could be determined.
   */
  public static int[] getRandomColorGradient(SpriteDecoder.AnimationType animType, int index)
  {
    if (randomGradientIndices.isEmpty()) {
      if (ResourceFactory.resourceExists("RANDCOLR.2DA")) {
        Table2da table = Table2daCache.get("RANDCOLR.2DA");
        if (table != null && table.getRowCount() > 1) {
          for (int col = 1, numCols = table.getColCount(); col < numCols; col++) {
            // random entry index
            int randIdx = Misc.toNumber(table.get(0, col), -1);
            if (randIdx >= 0) {
              int[] indices = new int[table.getRowCount() - 1];
              for (int row = 1, numRows = table.getRowCount(); row < numRows; row++) {
                indices[row - 1] = Misc.toNumber(table.get(row, col), 0);
              }
              randomGradientIndices.put(randIdx, indices);
            }
          }
        }
      }
    }

    int failCounter = 100;
    int[] retVal = null;
    int[] indices = randomGradientIndices.getOrDefault(index, null);
    while (retVal == null && indices != null && indices.length > 0 && failCounter-- > 0) {
      int idx = indices[getRandomInt(index, false) % indices.length];
      if (randomGradientIndices.containsKey(idx)) {
        // random color entries may refer to other random color entries
        indices = randomGradientIndices.getOrDefault(index, null);
      } else {
        retVal = getColorGradient(animType, idx, false);
      }
    }

    return retVal;
  }

  /**
   * Returns a random number from a predefined pool of random numbers.
   * @param index Index into the random number pool.
   * @param allowNegative whether negative return values are allowed.
   * @return the pseudo-random number specified by the index.
   */
  public static int getRandomInt(int index, boolean allowNegative)
  {
    if (randomPool == null) {
      updateRandomPool();
    }
    int retVal = randomPool[index % randomPool.length];
    if (!allowNegative && retVal < 0) {
      retVal = -retVal;
    }
    return retVal;
  }

  /** Recreates the pool of random numbers. */
  public static void updateRandomPool()
  {
    if (randomPool == null) {
      randomPool = new int[256];
    }
    for (int i = 0; i < randomPool.length; i++) {
      randomPool[i] = random.nextInt();
    }
  }

  private SpriteUtils()
  {
  }
}
