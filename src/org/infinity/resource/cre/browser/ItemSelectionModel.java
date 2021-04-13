// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.resource.cre.decoder.util.ItemInfo.ItemPredicate;
import org.infinity.resource.key.ResourceEntry;

/**
 * {@code ComboBoxModel} for item selection combo boxes used in the Creature Animation Browser.
 * The model allows to filter items by custom criteria.
 */
public class ItemSelectionModel extends AbstractListModel<ItemInfo> implements ComboBoxModel<ItemInfo>
{
  private final List<ItemInfo> itemList = new ArrayList<>(100);

  private boolean autoInit;
  private ItemPredicate filter;
  private Object selectedItem;

  public ItemSelectionModel(ItemPredicate filter, boolean autoInit)
  {
    super();
    this.autoInit = autoInit;
    setFilter(filter);
  }

  /** Returns whether content is automatically updated when the item filter is updated. */
  public boolean isAutoInit() { return autoInit; }

  /** Specify whether content is automatically updated when the item filter is updated. */
  public void setAutoInit(boolean b)
  {
    if (b != autoInit) {
      autoInit = b;
    }
  }

  /** Returns the current item filter. */
  public ItemPredicate getFilter() { return filter; }

  /** Sets a new item filter. Item list will be updated automatically. */
  public void setFilter(ItemPredicate filter)
  {
    if (filter == null) {
      filter = ItemInfo.FILTER_ALL;
    }
    if (!filter.equals(this.filter)) {
      this.filter = filter;
      if (isAutoInit()) {
        reload();
      }
    }
  }

  /** Discards the current content and loads new content based on current settings. */
  public void reload()
  {
    init();
  }

  /**
   * Returns the index-position of the specified object in the list.
   * @param anItem a {@code ItemInfo} object, {@code ResourceEntry} object or {@code String} specifying a ITM resref.
   * @return an int representing the index position, where 0 is the first position. Returns -1
   *         if the item could not be found in the list.
   */
  public int getIndexOf(Object anItem)
  {
    if (anItem instanceof ItemInfo) {
      return itemList.indexOf(anItem);
    } else if (anItem instanceof ResourceEntry) {
      final ResourceEntry entry = (ResourceEntry)anItem;
      return IntStream
          .range(0, itemList.size())
          .filter(i -> entry.equals(itemList.get(i).getResourceEntry()))
          .findAny()
          .orElse(-1);
    } else if (anItem != null) {
      final String itemName = anItem.toString().trim();
      return IntStream
          .range(0, itemList.size())
          .filter(i -> itemName.equalsIgnoreCase(itemList.get(i).getResourceEntry().getResourceRef()))
          .findAny()
          .orElse(-1);
    }
    return -1;
  }

  /** Empties the list. */
  public void removeAllElements()
  {
    if (!itemList.isEmpty()) {
      int oldSize = itemList.size();
      itemList.clear();
      selectedItem = null;
      if (oldSize > 0) {
        fireIntervalRemoved(this, 0, oldSize - 1);
      }
    } else {
      selectedItem = null;
    }
  }

//--------------------- Begin Interface ListModel ---------------------

  @Override
  public int getSize()
  {
    return itemList.size();
  }

  @Override
  public ItemInfo getElementAt(int index)
  {
    if (index >= 0 && index < itemList.size()) {
      return itemList.get(index);
    } else {
      return null;
    }
  }

//--------------------- End Interface ListModel ---------------------

//--------------------- Begin Interface ComboBoxModel ---------------------

  @Override
  public void setSelectedItem(Object anItem)
  {
    if ((selectedItem != null && !selectedItem.equals(anItem)) ||
        selectedItem == null && anItem != null) {
      selectedItem = anItem;
      fireContentsChanged(this, -1, -1);
    }
  }

  @Override
  public Object getSelectedItem()
  {
    return selectedItem;
  }

//--------------------- End Interface ComboBoxModel ---------------------

  private void init()
  {
    removeAllElements();

    itemList.add(ItemInfo.EMPTY);
    itemList.addAll(ItemInfo.getItemList(getFilter(), true));
    if (!itemList.isEmpty()) {
      fireIntervalAdded(this, 0, itemList.size() - 1);
    }

    setSelectedItem(getElementAt(0));
  }
}
