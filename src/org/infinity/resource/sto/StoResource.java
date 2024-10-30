// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

import org.infinity.NearInfinity;
import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.ItemCategoryOrderDialog;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.SearchOptions;
import org.infinity.util.Logger;
import org.infinity.util.StringTable;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.infinity.util.io.StreamUtils;

/**
 * These resource contains a description of the types of items and services available for sale in a given store, inn,
 * tavern, or temple.
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/sto_v1.htm">
 *      https://gibberlings3.github.io/iesdp/file_formats/ie_formats/sto_v1.htm</a>
 */
public final class StoResource extends AbstractStruct
    implements Resource, HasChildStructs, HasViewerTabs, ActionListener {
  // STO-specific field labels
  public static final String STO_TYPE                   = "Type";
  public static final String STO_NAME                   = "Name";
  public static final String STO_FLAGS                  = "Flags";
  public static final String STO_MARKUP_SELL            = "Sell markup";
  public static final String STO_MARKUP_BUY             = "Buy markup";
  public static final String STO_DEPRECIATION_RATE      = "Depreciation rate";
  public static final String STO_STEALING_DIFFICULTY    = "Stealing difficulty";
  public static final String STO_STORAGE_CAPACITY       = "Storage capacity";
  public static final String STO_OFFSET_ITEMS_PURCHASED = "Items purchased offset";
  public static final String STO_NUM_ITEMS_PURCHASED    = "# items purchased";
  public static final String STO_OFFSET_ITEMS_FOR_SALE  = "Items for sale offset";
  public static final String STO_NUM_ITEMS_FOR_SALE     = "# items for sale";
  public static final String STO_LORE                   = "Lore";
  public static final String STO_COST_TO_IDENTIFY       = "Cost to identify";
  public static final String STO_RUMORS_DRINKS          = "Rumors (drinks)";
  public static final String STO_OFFSET_DRINKS          = "Drinks for sale offset";
  public static final String STO_NUM_DRINKS             = "# drinks for sale";
  public static final String STO_RUMORS_DONATIONS       = "Rumors (donations)";
  public static final String STO_ROOMS_AVAILABLE        = "Available rooms";
  public static final String STO_PRICE_ROOM_PEASANT     = "Price peasant room";
  public static final String STO_PRICE_ROOM_MERCHANT    = "Price merchant room";
  public static final String STO_PRICE_ROOM_NOBLE       = "Price noble room";
  public static final String STO_PRICE_ROOM_ROYAL       = "Price royal room";
  public static final String STO_OFFSET_CURES           = "Cures for sale offset";
  public static final String STO_NUM_CURES              = "# cures for sale";

  private static final String SORT_ORDER_SUGGESTED      = "sortOrderSuggested";
  private static final String SORT_ORDER_ASCENDING      = "sortOrderAscending";
  private static final String SORT_ORDER_DESCENDING     = "sortOrderDescending";
  private static final String SORT_ORDER_CUSTOMIZE      = "sortOrderCustomize";

  // private static final String[] TYPE_ARRAY = {"Store", "Tavern", "Inn", "Temple"};
  public static final String[] TYPE9_ARRAY = { "Store", "Tavern", "Inn", "Temple", "Container" };

  public static final String[] TYPE_BG2_ARRAY = { "Store", "Tavern", "Inn", "Temple", "", "Container" };

  // private static final String[] FLAGS_ARRAY = {"Can't do anything", "Can buy", "Can sell", "Can identify",
  // "Can steal", "Can buy cures", "Can donate",
  // "Can buy drinks", "", "", "Quality Bit 0 (BAM)", "Quality Bit 1 (BAM)"};

  public static final String[] FLAGS_BG2_ARRAY = { "User can only rest", "User can buy", "User can sell",
      "User can identify", "User can steal", "User can donate;Unused in Enhanced Editions", "User can purchase cures",
      "User can purchase drinks", null, "EE: Disable donation screen;Disables donation screen in temple stores",
      "Tavern quality 1", "Tavern quality 2", null, "User can sell stolen goods", "EE: Ignore reputation",
      "Ex: Toggle item recharge", "EE: User can sell critical items" };

  public static final String[] ROOMS_ARRAY = { "No rooms available", "Peasant", "Merchant", "Noble", "Royal" };

  private StructHexViewer hexViewer;

  public static String getSearchString(InputStream is) throws IOException {
    is.skip(12);
    return StringTable.getStringRef(StreamUtils.readInt(is)).trim();
  }

  public StoResource(ResourceEntry entry) throws Exception {
    super(entry);
  }

  @Override
  public JComponent makeViewer(ViewableContainer container) {
    final JComponent c = super.makeViewer(container);
    if (c instanceof StructViewer) {
      final StructViewer viewer = (StructViewer) c;
      addSortItemsButton(viewer);
    }
    return c;
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception {
    TextString version = (TextString) getAttribute(COMMON_VERSION);
    if (version.toString().equals("V1.1")) {
      return new AddRemovable[] { new Purchases(), new ItemSale11(), new Drink(), new Cure() };
    } else {
      return new AddRemovable[] { new Purchases(), new ItemSale(), new Drink(), new Cure() };
    }
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception {
    return entry;
  }

  @Override
  public int getViewerTabCount() {
    return 2;
  }

  @Override
  public String getViewerTabName(int index) {
    switch (index) {
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return StructViewer.TAB_RAW;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index) {
    switch (index) {
      case 0: {
        JScrollPane scroll = new JScrollPane(new Viewer(this));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
      }
      case 1: {
        if (hexViewer == null) {
          hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
      }
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index) {
    return (index == 0);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    TextString version = new TextString(buffer, offset + 4, 4, COMMON_VERSION);
    addField(version);
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new Bitmap(buffer, offset + 8, 4, STO_TYPE, TYPE_BG2_ARRAY));
      addField(new StringRef(buffer, offset + 12, STO_NAME));
    } else {
      addField(new Bitmap(buffer, offset + 8, 4, STO_TYPE, TYPE9_ARRAY));
      addField(new StringRef(buffer, offset + 12, STO_NAME));
    }
    addField(new Flag(buffer, offset + 16, 4, STO_FLAGS, FLAGS_BG2_ARRAY));
    addField(new DecNumber(buffer, offset + 20, 4, STO_MARKUP_SELL));
    addField(new DecNumber(buffer, offset + 24, 4, STO_MARKUP_BUY));
    addField(new DecNumber(buffer, offset + 28, 4, STO_DEPRECIATION_RATE));
    // addField(new Unknown(buffer, offset + 30, 2));
    addField(new DecNumber(buffer, offset + 32, 2, STO_STEALING_DIFFICULTY));
    if (version.toString().equalsIgnoreCase("V9.0")) {
      addField(new Unknown(buffer, offset + 34, 2));
    } else {
      addField(new UnsignDecNumber(buffer, offset + 34, 2, STO_STORAGE_CAPACITY));
    }
    addField(new Unknown(buffer, offset + 36, 8));
    SectionOffset offsetPurchased = new SectionOffset(buffer, offset + 44, STO_OFFSET_ITEMS_PURCHASED,
        Purchases.class);
    addField(offsetPurchased);
    SectionCount countPurchased = new SectionCount(buffer, offset + 48, 4, STO_NUM_ITEMS_PURCHASED, Purchases.class);
    addField(countPurchased);
    SectionOffset offsetSale;
    SectionCount countSale;
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      offsetSale = new SectionOffset(buffer, offset + 52, STO_OFFSET_ITEMS_FOR_SALE, ItemSale.class);
      addField(offsetSale);
      countSale = new SectionCount(buffer, offset + 56, 4, STO_NUM_ITEMS_FOR_SALE, ItemSale.class);
      addField(countSale);
    } else if (version.toString().equals("V1.1")) {
      offsetSale = new SectionOffset(buffer, offset + 52, STO_OFFSET_ITEMS_FOR_SALE, ItemSale11.class);
      addField(offsetSale);
      countSale = new SectionCount(buffer, offset + 56, 4, STO_NUM_ITEMS_FOR_SALE, ItemSale11.class);
      addField(countSale);
    } else {
      clearFields();
      throw new Exception("Unsupported version: " + version);
    }
    addField(new DecNumber(buffer, offset + 60, 4, STO_LORE));
    addField(new DecNumber(buffer, offset + 64, 4, STO_COST_TO_IDENTIFY));
    addField(new ResourceRef(buffer, offset + 68, STO_RUMORS_DRINKS, "DLG"));
    SectionOffset offsetDrinks = new SectionOffset(buffer, offset + 76, STO_OFFSET_DRINKS, Drink.class);
    addField(offsetDrinks);
    SectionCount countDrinks = new SectionCount(buffer, offset + 80, 4, STO_NUM_DRINKS, Drink.class);
    addField(countDrinks);
    addField(new ResourceRef(buffer, offset + 84, STO_RUMORS_DONATIONS, "DLG"));
    addField(new Flag(buffer, offset + 92, 4, STO_ROOMS_AVAILABLE, ROOMS_ARRAY));
    addField(new DecNumber(buffer, offset + 96, 4, STO_PRICE_ROOM_PEASANT));
    addField(new DecNumber(buffer, offset + 100, 4, STO_PRICE_ROOM_MERCHANT));
    addField(new DecNumber(buffer, offset + 104, 4, STO_PRICE_ROOM_NOBLE));
    addField(new DecNumber(buffer, offset + 108, 4, STO_PRICE_ROOM_ROYAL));
    SectionOffset offsetCures = new SectionOffset(buffer, offset + 112, STO_OFFSET_CURES, Cure.class);
    addField(offsetCures);
    SectionCount countCures = new SectionCount(buffer, offset + 116, 4, STO_NUM_CURES, Cure.class);
    addField(countCures);
    addField(new Unknown(buffer, offset + 120, 36));
    if (version.toString().equals("V9.0")) {
      addField(new UnsignDecNumber(buffer, offset + 156, 2, STO_STORAGE_CAPACITY));
      addField(new Unknown(buffer, offset + 158, 82));
    }

    offset = offsetDrinks.getValue();
    for (int i = 0; i < countDrinks.getValue(); i++) {
      Drink drink = new Drink(this, buffer, offset, i);
      offset = drink.getEndOffset();
      addField(drink);
    }

    offset = offsetSale.getValue();
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      for (int i = 0; i < countSale.getValue(); i++) {
        ItemSale sale = new ItemSale(this, buffer, offset, i);
        offset = sale.getEndOffset();
        addField(sale);
      }
    } else if (version.toString().equals("V1.1")) {
      for (int i = 0; i < countSale.getValue(); i++) {
        ItemSale11 sale = new ItemSale11(this, buffer, offset, i);
        offset = sale.getEndOffset();
        addField(sale);
      }
    }

    offset = offsetCures.getValue();
    for (int i = 0; i < countCures.getValue(); i++) {
      Cure cure = new Cure(this, buffer, offset, i);
      offset = cure.getEndOffset();
      addField(cure);
    }

    offset = offsetPurchased.getValue();
    for (int i = 0; i < countPurchased.getValue(); i++) {
      Purchases pur = new Purchases(buffer, offset, i);
      offset += pur.getSize();
      addField(pur);
    }

    int endoffset = offset;
    for (final StructEntry entry : getFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset) {
        endoffset = entry.getOffset() + entry.getSize();
      }
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer) {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype) {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype) {
    super.datatypeAddedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype) {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype) {
    super.datatypeRemovedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
      case SORT_ORDER_ASCENDING:
        sortItems((a, b) -> a - b);
        break;
      case SORT_ORDER_DESCENDING:
        sortItems((a, b) -> b - a);
        break;
      case SORT_ORDER_SUGGESTED:
      case SORT_ORDER_CUSTOMIZE:
      {
        final int[] indexMap;
        if (SORT_ORDER_SUGGESTED.equals(e.getActionCommand())) {
          indexMap = ItemCategoryOrderDialog.getDefaultCategoryIndices(isPstStore());
        } else {
          final boolean interactive = (e.getModifiers() & KeyEvent.CTRL_MASK) == 0;
          indexMap = getCustomSortOrder(interactive);
        }
        sortItems((a, b) -> {
          final int a1 = (a >= 0 && a < indexMap.length) ? indexMap[a] : a;
          final int b1 = (b >= 0 && b < indexMap.length) ? indexMap[b] : b;
          return a1 - b1;
        });
        break;
      }
    }
  }

  /** Returns {@code true} if the current resource is a STOR V1.1 resource. */
  private boolean isPstStore() {
    boolean retVal = false;
    final StructEntry se = getAttribute(COMMON_VERSION);
    if (se instanceof TextString) {
      retVal = ((TextString)se).getText().equals("V1.1");
    }
    return retVal;
  }

  /**
   * Returns a custom load order to use by the sort operation.
   *
   * @param interactive Indicates whether to open a dialog where the user can further customize the sort order.
   * @return An index map for item categories. Returns {@code null} if the user cancelled the operation.
   */
  private int[] getCustomSortOrder(boolean interactive) {
    final boolean isPST = isPstStore();
    int[] indexMap = null;
    if (interactive) {
      final Window wnd;
      if (getViewer().getTopLevelAncestor() instanceof Window) {
        wnd = (Window)getViewer().getTopLevelAncestor();
      } else {
        wnd = NearInfinity.getInstance();
      }
      final ItemCategoryOrderDialog dlg = new ItemCategoryOrderDialog(wnd, isPST);
      if (dlg.isAccepted()) {
        indexMap = dlg.getCategoryIndices();
      }
    } else {
      indexMap = ItemCategoryOrderDialog.loadCategoryIndices(ItemCategoryOrderDialog.getPreferencesKey(isPST));
      if (indexMap == null) {
        indexMap = ItemCategoryOrderDialog.getDefaultCategoryIndices(isPST);
      }
    }

    return indexMap;
  }

  /**
   * Adds a new menu button to the button bar that allows the user to sort items for sale.
   *
   * @param viewer {@link StructViewer} instance of the STO resource.
   */
  private void addSortItemsButton(StructViewer viewer) {
    if (viewer != null) {
      final ButtonPanel buttons = viewer.getButtonPanel();
      final ButtonPopupMenu bpmSort = new ButtonPopupMenu("Sort items...");
      final JMenuItem miSuggested = new JMenuItem("In suggested order");
      miSuggested.setToolTipText("Sort order: weapons, armor, jewelry, potions, scrolls, wands, ...");
      miSuggested.setActionCommand(SORT_ORDER_SUGGESTED);
      miSuggested.addActionListener(this);
      final JMenuItem miAscending = new JMenuItem("In ascending order");
      miAscending.setToolTipText("Sort by item category number in ascending order.");
      miAscending.setActionCommand(SORT_ORDER_ASCENDING);
      miAscending.addActionListener(this);
      final JMenuItem miDescending = new JMenuItem("In descending order");
      miDescending.setToolTipText("Sort by item category number in descending order.");
      miDescending.setActionCommand(SORT_ORDER_DESCENDING);
      miDescending.addActionListener(this);
      final JMenuItem miCustomize = new JMenuItem("In user-defined order...");
      final String keyName = (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == KeyEvent.CTRL_MASK) ? "CTRL" : "COMMAND";
      miCustomize.setToolTipText("Specify sort order manually. Press " + keyName + " key to auto-apply last used item category order.");
      miCustomize.setActionCommand(SORT_ORDER_CUSTOMIZE);
      miCustomize.addActionListener(this);
      bpmSort.addItem(miSuggested);
      bpmSort.addItem(miAscending);
      bpmSort.addItem(miDescending);
      bpmSort.addItem(miCustomize);
      buttons.addControl(0, bpmSort, ButtonPanel.Control.CUSTOM_1);
    }
  }

  /**
   * Sorts {@link ItemSale} or{@link ItemSale11} structures by their item category index.
   * Category index of non-existing items is treated as 0.
   *
   * @param cmp A {@link Comparator} object that compares item category indices.
   */
  private void sortItems(Comparator<Integer> cmp) {
    // assembling item list
    final SectionOffset soItemsForSale = (SectionOffset) getAttribute(STO_OFFSET_ITEMS_FOR_SALE);
    if (soItemsForSale == null) {
      return;
    }
    final Class<? extends StructEntry> itemClass = soItemsForSale.getSection();
    final List<StructEntry> fieldList = getFields(itemClass);

    final ArrayList<SortableItem<? extends AbstractStruct>> itemList = new ArrayList<>();
    for (final StructEntry se : fieldList) {
      try {
        if (se instanceof ItemSale) {
          final ItemSale newItem = (ItemSale) ((ItemSale)se).clone();
          itemList.add(new SortableItem<ItemSale>(newItem));
        } else if (se instanceof ItemSale11) {
          itemList.add(new SortableItem<ItemSale11>(new ItemSale11(null, se.getDataBuffer(), 0, 0)));
        }
      } catch (Exception e) {
        Logger.error(e);
      }
    }

    // sorting item list
    itemList.sort((a, b) -> cmp.compare(a.getCategory(), b.getCategory()));

    // replacing existing item list
    for (int i = 0, size = Math.min(itemList.size(), fieldList.size()); i < size; i++) {
      final AbstractStruct curItem = (AbstractStruct) fieldList.get(i);
      final AbstractStruct newItem = itemList.get(i).getItem();
      newItem.setOffset(curItem.getOffset());
      // TODO: externalize name generation to separate function
      if (newItem instanceof ItemSale11) {
        newItem.setName(ItemSale11.STO_SALE + " " + i);
      } else {
        newItem.setName(ItemSale.STO_SALE + " " + i);
      }
      replaceField(newItem);
    }
    fireTableDataChanged();
    setStructChanged(!fieldList.isEmpty());
  }

  /**
   * Checks whether the specified resource entry matches all available search options. Called by "Extended Search"
   */
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions) {
    if (entry != null && searchOptions != null) {
      try {
        StoResource sto = new StoResource(entry);
        Bitmap[] purchases;
        ResourceRef[] items;
        boolean retVal = true;
        String key;
        Object o;

        // preparations
        IsNumeric ofs = (IsNumeric) sto.getAttribute(STO_OFFSET_ITEMS_FOR_SALE, false);
        IsNumeric cnt = (IsNumeric) sto.getAttribute(STO_NUM_ITEMS_FOR_SALE, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          String itemLabel = SearchOptions.getResourceName(SearchOptions.STO_Item_Item1);
          items = new ResourceRef[cnt.getValue()];
          for (int i = 0; i < cnt.getValue(); i++) {
            String itemStruct = String.format(SearchOptions.getResourceName(SearchOptions.STO_Item), i);
            if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
              ItemSale11 item = (ItemSale11) sto.getAttribute(itemStruct, false);
              if (item != null) {
                items[i] = (ResourceRef) item.getAttribute(itemLabel, false);
              } else {
                items[i] = null;
              }
            } else {
              ItemSale item = (ItemSale) sto.getAttribute(itemStruct, false);
              if (item != null) {
                items[i] = (ResourceRef) item.getAttribute(itemLabel, false);
              } else {
                items[i] = null;
              }
            }
          }
        } else {
          items = new ResourceRef[0];
        }

        ofs = (IsNumeric) sto.getAttribute(STO_OFFSET_ITEMS_PURCHASED, false);
        cnt = (IsNumeric) sto.getAttribute(STO_NUM_ITEMS_PURCHASED, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          purchases = new Bitmap[cnt.getValue()];
          for (int i = 0; i < cnt.getValue(); i++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.STO_Purchased), i);
            purchases[i] = (Bitmap) sto.getAttribute(label, false);
          }
        } else {
          purchases = new Bitmap[0];
        }

        if (retVal) {
          key = SearchOptions.STO_Name;
          o = searchOptions.getOption(key);
          StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key), false);
          retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
        }

        if (retVal) {
          key = SearchOptions.STO_Type;
          o = searchOptions.getOption(key);
          StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key), false);
          retVal = SearchOptions.Utils.matchNumber(struct, o);
        }

        String[] keyList = new String[] { SearchOptions.STO_Purchased1, SearchOptions.STO_Purchased2,
            SearchOptions.STO_Purchased3, SearchOptions.STO_Purchased4, SearchOptions.STO_Purchased5 };
        for (String element : keyList) {
          if (retVal) {
            key = element;
            o = searchOptions.getOption(key);
            boolean found = false;
            for (Bitmap element2 : purchases) {
              if (element2 != null) {
                found |= SearchOptions.Utils.matchNumber(element2, o);
              }
            }
            retVal = found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[] { SearchOptions.STO_Flags, SearchOptions.STO_RoomsAvailable };
        for (String element : keyList) {
          if (retVal) {
            key = element;
            o = searchOptions.getOption(key);
            StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key), false);
            retVal = SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[] { SearchOptions.STO_Depreciation, SearchOptions.STO_SellMarkup,
            SearchOptions.STO_BuyMarkup, SearchOptions.STO_Stealing, SearchOptions.STO_Capacity };
        for (String element : keyList) {
          if (retVal) {
            key = element;
            o = searchOptions.getOption(key);
            StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key), false);
            retVal = SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[] { SearchOptions.STO_Item_Item1, SearchOptions.STO_Item_Item2,
            SearchOptions.STO_Item_Item3, SearchOptions.STO_Item_Item4 };
        for (String element : keyList) {
          if (retVal) {
            key = element;
            o = searchOptions.getOption(key);
            boolean found = false;
            for (ResourceRef item : items) {
              if (item != null) {
                found |= SearchOptions.Utils.matchResourceRef(item, o, false);
              }
            }
            retVal = found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[] { SearchOptions.STO_Custom1, SearchOptions.STO_Custom2, SearchOptions.STO_Custom3,
            SearchOptions.STO_Custom4 };
        for (String element : keyList) {
          if (retVal) {
            key = element;
            o = searchOptions.getOption(key);
            retVal = SearchOptions.Utils.matchCustomFilter(sto, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
        Logger.trace(e);
      }
    }
    return false;
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Helper class that associates an {@link ItemSale} or {@link ItemSale11} instance with the related item category. */
  private static class SortableItem<T extends AbstractStruct> {
    private final T item;
    private final int category;

    public SortableItem(T item) {
      this.item = Objects.requireNonNull(item);
      this.category = readItemCategory(this.item, Integer.MAX_VALUE);
    }

    /** Returns the wrapped item instance ({@link ItemSale} or {@link ItemSale11}). */
    public T getItem() {
      return item;
    }

    /** Returns the category index of the item. Returns {@link Integer#MAX_VALUE} if not available. */
    public int getCategory() {
      return category;
    }

    /**
     * Fetches the item category from the ITM resref stored in the item's resref field. Returns a default category if
     * not available.
     *
     * @param item   The {@link ItemSale} or {@link ItemSale11} object.
     * @param defCat A default category to return if ITM resource is not available.
     * @return Item category index.
     */
    private int readItemCategory(T item, int defCat) {
      if (item != null) {
        final StructEntry se = item.getAttribute(ItemSale.STO_SALE_ITEM);
        if (se instanceof ResourceRef) {
          final String itemResref = resolveRandomItem(((ResourceRef)se).getText());
          if (itemResref != null && !itemResref.isEmpty()) {
            final ResourceEntry re = ResourceFactory.getResourceEntry(itemResref + ".ITM");
            if (re != null) {
              try {
                final ByteBuffer bb = re.getResourceBuffer();
                return bb.order(ByteOrder.LITTLE_ENDIAN).getShort(0x1c);
              } catch (Exception e) {
                Logger.info("Could not read item category from {}: {}", re, e.getMessage());
              }
            }
          }
        }
      }
      return defCat;
    }

    /**
     * Attempts to resolve random treasure to actual items.
     * This is currently only performed for IWD2.
     * Random treasure in other games doesn't appear to be resolved in stores.
     */
    private String resolveRandomItem(String itmResref) {
      if (itmResref == null || itmResref.isEmpty()) {
        return itmResref;
      }

      String retVal = itmResref;
      if (Profile.getGame() == Profile.Game.IWD2) {
        final Table2da table = Table2daCache.get("rt_norm.2da");
        if (table != null) {
          for (int row = 0, numRows = table.getRowCount(); row < numRows; ++row) {
            final String s = table.get(row, 0);
            if (itmResref.equalsIgnoreCase(s)) {
              if (table.getColCount(row) > 1) {
                retVal = table.get(row, 1);
              }
              break;
            }
          }
        }
      }

      return retVal;
    }
  }
}
