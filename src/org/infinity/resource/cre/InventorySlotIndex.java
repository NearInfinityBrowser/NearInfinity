// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.datatype.AbstractBitmap;
import org.infinity.gui.TextListPanel;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.StructEntry;

/**
 * Specialized bitmap type that represents a CRE inventory slot.
 */
public class InventorySlotIndex extends AbstractBitmap<Item> implements ActionListener, ListSelectionListener {
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

  private final JButton bView;

  public InventorySlotIndex(ByteBuffer buffer, int offset, int length, String name, CreResource cre) {
    super(buffer, offset, length, name, getUpdatedItemList(cre, null), ITEM_FORMATTER, true);
    bView = new JButton("View/Edit", Icons.ICON_ZOOM_16.getIcon());
    bView.addActionListener(this);
    addButtons(bView);
  }

  @Override
  public JComponent edit(ActionListener container) {
    // refreshing item list
    getUpdatedItemList((CreResource)getParent(), getBitmap());

    final JComponent retVal = super.edit(container);

    // customizing list control
    final JComponent ctrl = getUiControl();
    if (ctrl instanceof TextListPanel<?>) {
      ((TextListPanel<?>)ctrl).addListSelectionListener(this);
      bView.setEnabled(getSelectedCreItem() != null);
    }

    return retVal;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == bView) {
      final Item item = getSelectedCreItem();
      if (item != null) {
        new ViewFrame(getUiControl().getTopLevelAncestor(), item);
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e) {
    bView.setEnabled(getSelectedCreItem() != null);
  }

  // --------------------- Begin Interface ListSelectionListener ---------------------

  /**
   * Returns the currently selected {@link Item} instance.
   *
   * @return Selected {@link Item} if available, {@code null} otherwise.
   */
  private Item getSelectedCreItem() {
    if (getUiControl() instanceof TextListPanel<?>) {
      final TextListPanel<?> listPanel = (TextListPanel<?>)getUiControl();
      if (listPanel.getSelectedValue() instanceof FormattedData<?>) {
        final FormattedData<?> data = (FormattedData<?>)listPanel.getSelectedValue();
        if (data.getData() instanceof Item) {
          return (Item)data.getData();
        }
      }
    }
    return null;
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
