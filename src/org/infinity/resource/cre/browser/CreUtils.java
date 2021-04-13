// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.Readable;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.Item;
import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

/**
 * Collection of methods for creating and manipulating CRE resources.
 */
public class CreUtils
{
  private static ByteBuffer BUFFER;

  /** Returns the animation id for the specified CRE resource. */
  public static int getAnimation(CreResource cre)
  {
    return ((IsNumeric)Objects.requireNonNull(cre).getAttribute(CreResource.CRE_ANIMATION)).getValue();
  }

  /** Sets a new animation id for the specified CRE resource. */
  public static void setAnimation(CreResource cre, int animId) throws IllegalArgumentException
  {
    if (animId >= 0 && animId <= 0xffff) {
      int oldValue = getAnimation(cre);
      if (oldValue != animId) {
        setFieldValue(Objects.requireNonNull(cre).getAttribute(CreResource.CRE_ANIMATION), null, animId);
      }
    } else {
      throw new IllegalArgumentException(String.format("Invalid animation id specified: 0x%04X", animId));
    }
  }

  /** Returns the allegiance value for the specified CRE resource. */
  public static int getAllegiance(CreResource cre)
  {
    return ((IsNumeric)Objects.requireNonNull(cre).getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
  }

  /** Sets a new allegiance value for the specified CRE resource. */
  public static void setAllegiance(CreResource cre, int ea) throws IllegalArgumentException
  {
    if (ea >= -128 && ea <= 255) {
      ea &= 0xff;
      int oldValue = getAllegiance(cre);
      if (oldValue != ea) {
        setFieldValue(Objects.requireNonNull(cre).getAttribute(CreResource.CRE_ALLEGIANCE), null, ea);
      }
    } else {
      throw new IllegalArgumentException("Invalid allegiance value specified: " + ea);
    }
  }

  /** Returns whether the panic status is enabled for the specified CRE resource. */
  public static boolean getStatusPanic(CreResource cre)
  {
    int status = ((IsNumeric)Objects.requireNonNull(cre).getAttribute(CreResource.CRE_STATUS)).getValue();
    return (status & (1 << 2)) != 0;
  }

  /** Sets the panic status for the specified CRE resource. */
  public static void setStatusPanic(CreResource cre, boolean b)
  {
    int oldStatus = ((IsNumeric)Objects.requireNonNull(cre).getAttribute(CreResource.CRE_STATUS)).getValue();
    int newStatus = oldStatus;
    if (b) {
      newStatus |= 1 << 2;
    } else {
      newStatus &= ~(1 << 2);
    }
    if (newStatus != oldStatus) {
      setFieldValue(cre.getAttribute(CreResource.CRE_STATUS), null, newStatus);
    }
  }

  /** Returns the color value at the specified location index for the specified CRE resource. */
  public static int getColor(CreResource cre, int locationIdx) throws IllegalArgumentException
  {
    if (locationIdx >= 0 && locationIdx < 7) {
      StructEntry se = getColorField(cre, locationIdx);
      if (se instanceof IsNumeric) {
        return ((IsNumeric)se).getValue();
      } else {
        throw new IllegalArgumentException("Color location at index " + locationIdx + " does not exist");
      }
    } else {
      throw new IllegalArgumentException("Color location index out of bounds: " + locationIdx);
    }
  }

  /** Sets a new color value to the field specified by the location index. */
  public static void setColor(CreResource cre, int locationIdx, int value) throws IllegalArgumentException
  {
    if (locationIdx >= 0 && locationIdx < 7) {
      StructEntry se = getColorField(cre, locationIdx);
      if (se instanceof IsNumeric) {
        setFieldValue(se, null, value);
      }
    } else {
      throw new IllegalArgumentException("Invalid color location index specified: " + locationIdx);
    }
  }

  /** Returns the item equipped in the helmet slot of the specified CRE resource. Returns {@code null} if no item is equipped. */
  public static ItemInfo getEquipmentHelmet(CreResource cre)
  {
    Objects.requireNonNull(cre);
    ItemInfo retVal = null;
    boolean isPST = (Profile.getGame() == Profile.Game.PSTEE) ||
                    ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText().equalsIgnoreCase("V1.2");
    String fieldName = isPST ? CreResource.CRE_ITEM_SLOT_RIGHT_EARRING : CreResource.CRE_ITEM_SLOT_HELMET;
    Item item = getEquippedItem(cre, fieldName);
    if (item != null) {
      ResourceEntry itemEntry = ResourceFactory.getResourceEntry(((IsReference)item.getAttribute(Item.CRE_ITEM_RESREF)).getResourceName());
      if (itemEntry != null) {
        try {
          retVal = ItemInfo.get(itemEntry);
          retVal.overrideDroppableFlag(((Flag)item.getAttribute(Item.CRE_ITEM_FLAGS)).isFlagSet(3));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  /**
   * Equips the given item in the helmet slot of the specified CRE resource.
   * @param cre the CRE resource.
   * @param item the {@code ItemInfo} object. Specify {@code null} to unequip the current helmet.
   */
  public static void setEquipmentHelmet(CreResource cre, ItemInfo item)
  {
    Objects.requireNonNull(cre);
    boolean isPST = (Profile.getGame() == Profile.Game.PSTEE) ||
                    ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText().equalsIgnoreCase("V1.2");
    String fieldName = isPST ? CreResource.CRE_ITEM_SLOT_RIGHT_EARRING : CreResource.CRE_ITEM_SLOT_HELMET;
    setEquippedItem(cre, item, fieldName);
  }

  /** Returns the item equipped in the armor slot of the specified CRE resource. Returns {@code null} if no item is equipped. */
  public static ItemInfo getEquipmentArmor(CreResource cre)
  {
    Objects.requireNonNull(cre);
    ItemInfo retVal = null;
    boolean isPST = (Profile.getGame() == Profile.Game.PSTEE) ||
                    ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText().equalsIgnoreCase("V1.2");
    String fieldName = isPST ? CreResource.CRE_ITEM_SLOT_CHEST : CreResource.CRE_ITEM_SLOT_ARMOR;
    Item item = getEquippedItem(cre, fieldName);
    if (item != null) {
      ResourceEntry itemEntry = ResourceFactory.getResourceEntry(((IsReference)item.getAttribute(Item.CRE_ITEM_RESREF)).getResourceName());
      if (itemEntry != null) {
        try {
          retVal = ItemInfo.get(itemEntry);
          retVal.overrideDroppableFlag(((Flag)item.getAttribute(Item.CRE_ITEM_FLAGS)).isFlagSet(3));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  /**
   * Equips the given item in the armor slot of the specified CRE resource.
   * @param cre the CRE resource.
   * @param item the {@code ItemInfo} object. Specify {@code null} to unequip the current helmet.
   */
  public static void setEquipmentArmor(CreResource cre, ItemInfo item)
  {
    Objects.requireNonNull(cre);
    boolean isPST = (Profile.getGame() == Profile.Game.PSTEE) ||
                    ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText().equalsIgnoreCase("V1.2");
    String fieldName = isPST ? CreResource.CRE_ITEM_SLOT_CHEST : CreResource.CRE_ITEM_SLOT_ARMOR;
    setEquippedItem(cre, item, fieldName);
  }

  /** Returns the item equipped in the shield slot of the specified CRE resource. Returns {@code null} if no item is equipped. */
  public static ItemInfo getEquipmentShield(CreResource cre)
  {
    Objects.requireNonNull(cre);
    ItemInfo retVal = null;
    String creVer = ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText();
    boolean isPST = (Profile.getGame() == Profile.Game.PSTEE) || creVer.equalsIgnoreCase("V1.2");
    boolean isIWD2 = creVer.equalsIgnoreCase("V2.2");
    String fieldName;
    if (isPST)
      fieldName = CreResource.CRE_ITEM_SLOT_RIGHT_TATTOO_LOWER;
    else if (isIWD2) {
      fieldName = String.format(CreResource.CRE_ITEM_SLOT_SHIELD_FMT, 1);
    } else {
      fieldName = CreResource.CRE_ITEM_SLOT_SHIELD;
    }
    Item item = getEquippedItem(cre, fieldName);
    if (item != null) {
      ResourceEntry itemEntry = ResourceFactory.getResourceEntry(((IsReference)item.getAttribute(Item.CRE_ITEM_RESREF)).getResourceName());
      if (itemEntry != null) {
        try {
          retVal = ItemInfo.get(itemEntry);
          retVal.overrideDroppableFlag(((Flag)item.getAttribute(Item.CRE_ITEM_FLAGS)).isFlagSet(3));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  /**
   * Equips the given item in the shield slot of the specified CRE resource.
   * @param cre the CRE resource.
   * @param item the {@code ItemInfo} object. Specify {@code null} to unequip the current helmet.
   */
  public static void setEquipmentShield(CreResource cre, ItemInfo item)
  {
    Objects.requireNonNull(cre);
    String creVer = ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText();
    boolean isPST = (Profile.getGame() == Profile.Game.PSTEE) || creVer.equalsIgnoreCase("V1.2");
    boolean isIWD2 = creVer.equalsIgnoreCase("V2.2");
    String fieldName;
    if (isPST)
      fieldName = CreResource.CRE_ITEM_SLOT_RIGHT_TATTOO_LOWER;
    else if (isIWD2) {
      fieldName = String.format(CreResource.CRE_ITEM_SLOT_SHIELD_FMT, 1);
    } else {
      fieldName = CreResource.CRE_ITEM_SLOT_SHIELD;
    }
    setEquippedItem(cre, item, fieldName);
  }

  /** Returns the item equipped in the weapon slot of the specified CRE resource. Returns {@code null} if no item is equipped. */
  public static ItemInfo getEquipmentWeapon(CreResource cre)
  {
    Objects.requireNonNull(cre);
    ItemInfo retVal = null;
    String fieldName = String.format(CreResource.CRE_ITEM_SLOT_WEAPON_FMT, 1);
    Item item = getEquippedItem(cre, fieldName);
    if (item != null) {
      ResourceEntry itemEntry = ResourceFactory.getResourceEntry(((IsReference)item.getAttribute(Item.CRE_ITEM_RESREF)).getResourceName());
      if (itemEntry != null) {
        try {
          retVal = ItemInfo.get(itemEntry);
          retVal.overrideDroppableFlag(((Flag)item.getAttribute(Item.CRE_ITEM_FLAGS)).isFlagSet(3));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return retVal;
  }

  /**
   * Equips the given item in the weapon slot of the specified CRE resource.
   * @param cre the CRE resource.
   * @param item the {@code ItemInfo} object. Specify {@code null} to unequip the current helmet.
   */
  public static void setEquipmentWeapon(CreResource cre, ItemInfo item)
  {
    Objects.requireNonNull(cre);
    String fieldName = String.format(CreResource.CRE_ITEM_SLOT_WEAPON_FMT, 1);
    setEquippedItem(cre, item, fieldName);
    // selecting first weapon slot
    setFieldValue(cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_SLOT), null, 0);
  }

  /**
   * Helper method: Equips the specified item to the item slot of given name.
   * A previously equipped item will be overwritten. Otherwise a new item will be added and item slot indices are updated.
   * @param cre the CRE resource
   * @param item the item to equip. Specify {@code null} to unequip current item without equipping a new one.
   * @param slotName the item slot name
   */
  private static void setEquippedItem(CreResource cre, ItemInfo item, String slotName)
  {
    StructEntry se = Objects.requireNonNull(cre).getAttribute(slotName);
    if (se == null) {
      return;
    }

    if (item != null && item.getResourceEntry() != null) {
      // equip new item
      addEquippedItem(cre, item, slotName);
    } else {
      // unequip current item
      removeEquippedItem(cre, slotName);
    }
  }

  /**
   * Helper method: Updates an existing equipped item in the specified slot.
   * @param cre the CRE resource
   * @param item the item to equip
   * @param slotName the item slot name
   * @return {@code true} if item slot could be updated. {@code false} otherwise.
   */
  private static boolean updateEquippedItem(CreResource cre, ItemInfo item, String slotName)
  {
    boolean retVal = false;
    if (item == null) {
      return retVal;
    }

    StructEntry se = Objects.requireNonNull(cre).getAttribute(slotName);
    if (se == null) {
      return retVal;
    }

    int idx = ((IsNumeric)se).getValue();
    int numItems = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEMS)).getValue();
    if (idx < 0 || idx >= numItems) {
      return retVal;
    }

    List<StructEntry> itemList = cre.getFields(Item.class);
    if (idx < itemList.size() && itemList.get(idx) instanceof Item) {
      Item curItem = (Item)itemList.get(idx);
      String resref = item.getResourceEntry().getResourceRef();
      setFieldValue(curItem.getAttribute(Item.CRE_ITEM_RESREF), null, resref, 8);
      retVal = true;
    }

    return retVal;
  }

  /**
   * Helper method: Adds a new equipped item and assigns it to the specified slot.
   * @param cre the CRE resource
   * @param item the item to equip
   * @param slotName the item slot name
   */
  private static void addEquippedItem(CreResource cre, ItemInfo item, String slotName)
  {
    if (item == null) {
      return;
    }

    StructEntry se = Objects.requireNonNull(cre).getAttribute(slotName);
    if (se == null) {
      return;
    }

    int idx = ((IsNumeric)se).getValue();
    int numItems = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEMS)).getValue();
    if (idx >= 0 && idx < numItems) {
      // Old item exists? Update!
      if (updateEquippedItem(cre, item, slotName)) {
        return;
      }
    }

    ByteBuffer buf = getByteBuffer();
    buf.putLong(0L);
    buf.putLong(0L);
    buf.putInt(1);  // flags: identified
    try {
      Item newItem = new Item(cre, getByteBuffer(), 0, numItems);
      String resref = item.getResourceEntry().getResourceRef();
      setFieldValue(newItem.getAttribute(Item.CRE_ITEM_RESREF), buf, resref, 8);
      cre.addDatatype(newItem);
      setFieldValue(cre.getAttribute(slotName), null, numItems);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Helper method: Removes the currently equipped item from the specified slot.
   * @param cre the CRE resource
   * @param slotName the item slot name
   */
  private static void removeEquippedItem(CreResource cre, String slotName)
  {
    StructEntry se = cre.getAttribute(slotName);
    if (se == null) {
      return;
    }

    int idx = ((IsNumeric)se).getValue();
    if (idx < 0) {
      return;
    }

    // clearing item reference
    setFieldValue(se, null, -1);
    int numItems = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEMS)).getValue();
    if (idx < numItems) {
      List<StructEntry> itemList = cre.getFields(Item.class);
      if (idx < itemList.size() && itemList.get(idx) instanceof AddRemovable) {
        // removing item structure
        cre.removeDatatype((AddRemovable)itemList.get(idx), false);
        // updating slot indices
        updateItemSlots(cre, idx + 1, -1);
      }
    }
  }

  /**
   * Helper method: Updates item slot references greater than or equal to {@code startIndex} by {@code diff}.
   * @param cre the CRE resource
   * @param startIndex lower bound of item index to update.
   * @param diff this value is added to the item index.
   */
  private static void updateItemSlots(CreResource cre, int startIndex, int diff)
  {
    int ofsItemSlots = Objects.requireNonNull(cre).getExtraOffset() +
                       ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue();
    int numItemSlots = getItemSlotCount(cre, false);

    for (int i = 0; i < numItemSlots; i++) {
      StructEntry se = cre.getAttribute(ofsItemSlots + i * 2);
      int slotIdx = ((IsNumeric)se).getValue();
      if (slotIdx >= startIndex) {
        slotIdx += diff;
        setFieldValue(se, null, slotIdx);
      }
    }
  }

  /**
   * Helper method: Determines the number of available item slots.
   * @param cre the CRE resource.
   * @param includeSelectionSlots whether selected weapon slot and weapon ability should be included in the count.
   * @return number of available item slots.
   */
  private static int getItemSlotCount(CreResource cre, boolean includeSelectionSlots)
  {
    int retVal = 0;
    if (cre != null) {
      String ver = ((IsTextual)cre.getAttribute(CreResource.COMMON_VERSION)).getText().toUpperCase();
      switch (ver) {
        case "V1.2":
          retVal = 48;
          break;
        case "V2.2":
          retVal = 52;
          break;
        default:
          if (Profile.getGame() == Profile.Game.PSTEE) {
            int numSlots = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEM_SLOTS)).getValue();
            if (numSlots > 0) {
              retVal = numSlots;
            }
          }
          if (retVal == 0) {
            retVal = 40;
          }
      }
    }

    if (!includeSelectionSlots) {
      retVal -= 2;
    }

    return retVal;
  }

  /** Helper method: Returns the item structure referenced by the specified item slot. */
  private static Item getEquippedItem(CreResource cre, String slotName)
  {
    Item retVal = null;
    int idx = ((IsNumeric)Objects.requireNonNull(cre).getAttribute(Objects.requireNonNull(slotName))).getValue();
    if (idx >= 0) {
      int numItems = ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_ITEMS)).getValue();
      if (idx < numItems) {
        List<StructEntry> itemList = cre.getFields(Item.class);
        if (idx < itemList.size() && itemList.get(idx) instanceof Item) {
          retVal = (Item)itemList.get(idx);
//          retVal = ResourceFactory.getResourceEntry(((IsReference)item.getAttribute(Item.CRE_ITEM_RESREF)).getResourceName());
        }
      }
    }
    return retVal;
  }


  /** Helper method: Returns the specified color field. */
  private static StructEntry getColorField(CreResource cre, int locationIdx)
  {
    StructEntry retVal = null;
    if (locationIdx >= 0 && locationIdx < 7) {
      boolean isV12 = ((IsTextual)Objects.requireNonNull(cre).getAttribute(CreResource.COMMON_VERSION)).getText().equalsIgnoreCase("V1.2");
      int ofsBase = isV12 ? 0x2e4 : 0x2c;
      int size = isV12 ? 2 : 1;
      retVal = cre.getAttribute(ofsBase + locationIdx * size);
    }
    return retVal;
  }

  /**
   * Attempts to write the specified numeric value into the given field.
   * @param field the {@code Readable} instance to update.
   * @param buf the {@code ByteBuffer} object to use as temporary storage.
   * @param value the value to set.
   * @return number of bytes written.
   */
  private static int setFieldValue(Readable field, ByteBuffer buf, int value)
  {
    int retVal = 0;
    if (field != null) {
      if (buf == null) {
        buf = getByteBuffer();
      }
      int pos = buf.position();
      buf.putInt(value);
      try {
        retVal = field.read(buf, pos);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Attempts to write the specified textual value into the given field.
   * @param field the {@code Readable} instance to update.
   * @param buf the {@code ByteBuffer} object to use as temporary storage.
   * @param value the value to set.
   * @param maxLen the maximum number of characters to write. String is truncated or padded to fit the required length.
   * @return number of bytes written.
   */
  private static int setFieldValue(Readable field, ByteBuffer buf, String value, int maxLen)
  {
    int retVal = 0;
    if (field != null) {
      maxLen = Math.max(0, Math.min(getByteBuffer().limit(), maxLen));
      byte[] arr = Arrays.copyOfRange(value.getBytes(Misc.CHARSET_DEFAULT), 0, maxLen);
      if (buf == null) {
        buf = getByteBuffer();
      }
      int pos = buf.position();
      buf.put(arr);
      try {
        retVal = field.read(buf, pos);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /** Returns a {@code ByteBuffer} instance for temporary storage with little endian byte order and position set to 0. */
  private static ByteBuffer getByteBuffer()
  {
    if (BUFFER == null) {
      BUFFER = ByteBuffer.allocate(1024);
      BUFFER.order(ByteOrder.LITTLE_ENDIAN);
    }
    BUFFER.position(0);
    return BUFFER;
  }

  private CreUtils() { }
}
