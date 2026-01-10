// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JComponent;

import org.infinity.datatype.AbstractBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Specialized bitmap type that represents a CRE inventory slot.
 */
public class InventorySlotIndex extends AbstractBitmap<Item> {
  private static final String INV_UNDEFINED = "(Undefined)";
  private static final String INV_EMPTY = "(Empty)";

  /** Specialized formatter object for {@link Item} objects. */
  public static final BiFunction<Long, Item, String> ITEM_FORMATTER = (index, item) -> {
    final String retVal;
    if (item != null) {
      retVal = Objects.toString(item.getAttribute(Item.CRE_ITEM_RESREF), INV_UNDEFINED);
    } else if (index == -1L) {
      retVal = INV_EMPTY;
    } else {
      retVal = INV_UNDEFINED;
    }
    return retVal + " - " + index;
  };

  public InventorySlotIndex(ByteBuffer buffer, int offset, int length, String name, CreResource cre) {
    super(buffer, offset, length, name, getUpdatedItemList(cre, null), ITEM_FORMATTER, true);
  }

  @Override
  public JComponent edit(ActionListener container) {
    // refreshing item list
    getUpdatedItemList((CreResource)getParent(), getBitmap());

    return super.edit(container);
  }

  /**
   * As {@link #updateValue(AbstractStruct)} but also recreates the list of item references if requested.
   *
   * @param struct      Structure that owns that object and must be updated.
   * @param updateItems Specifies whether the item list should be recreated.
   * @return {@code true} if object succesfully changed, {@code false} otherwise
   */
  public boolean updateValue(AbstractStruct struct, boolean updateItems) {
    if (updateItems) {
      // refreshing item list
      getUpdatedItemList((CreResource)getParent(), getBitmap());
    }
    return super.updateValue(struct);
  }

  /**
   * Recreates the list of item references from the item list of the current {@link CreResource}.
   *
   * @param cre {@link CreResource} instance owning the current datatype.
   * @param itemList Hash table with item associations. Specify {@code null} recreate it from scratch.
   * @return Initialized {@code itemList} instance if specified, a new {@code TreeMap} instance otherwise.
   */
  public static TreeMap<Long, Item> getUpdatedItemList(CreResource cre, TreeMap<Long, Item> itemList) {
    if (itemList == null) {
      itemList = new TreeMap<>();
    }
    itemList.clear();
    itemList.put(-1L, null);
    if (cre != null) {
      final List<StructEntry> items = cre.getFields(Item.class);
      if (items != null) {
        for (int idx = 0, size = items.size(); idx < size; ++idx) {
          itemList.put((long)idx, (Item)items.get(idx));
        }
      }
    }
    return itemList;
  }
}
