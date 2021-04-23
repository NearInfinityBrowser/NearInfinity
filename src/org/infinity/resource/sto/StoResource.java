// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

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
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.SearchOptions;
import org.infinity.util.StringTable;
import org.infinity.util.io.StreamUtils;

/**
 * These resource contains a description of the types of items and services
 * available for sale in a given store, inn, tavern, or temple.
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/sto_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/sto_v1.htm</a>
 */
public final class StoResource extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs
{
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

//  private static final String[] s_type = {"Store", "Tavern", "Inn", "Temple"};
  public static final String[] s_type9 = {"Store", "Tavern", "Inn", "Temple", "Container"};
  public static final String[] s_type_bg2 = {"Store", "Tavern", "Inn", "Temple", "", "Container"};
//  private static final String[] s_flag = {"Can't do anything", "Can buy", "Can sell", "Can identify",
//                                          "Can steal", "Can buy cures", "Can donate",
//                                          "Can buy drinks", "", "", "Quality Bit 0 (BAM)", "Quality Bit 1 (BAM)"};
  public static final String[] s_flag_bg2 = {"User can only rest", "User can buy", "User can sell", "User can identify",
                                              "User can steal", "User can donate;Unused in Enhanced Editions", "User can purchase cures",
                                              "User can purchase drinks", null, "EE: Disable donation screen;Disables donation screen in temple stores",
                                              "Tavern quality 1", "Tavern quality 2", null, "User can sell stolen goods", "EE: Ignore reputation",
                                              "Ex: Toggle item recharge", "EE: User can sell critical items"};
  public static final String[] s_rooms = {"No rooms available", "Peasant", "Merchant", "Noble", "Royal"};

  private StructHexViewer hexViewer;

  public static String getSearchString(InputStream is) throws IOException
  {
    is.skip(12);
    return StringTable.getStringRef(StreamUtils.readInt(is)).trim();
  }

  public StoResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    TextString version = (TextString)getAttribute(COMMON_VERSION);
    if (version.toString().equals("V1.1"))
      return new AddRemovable[]{new Purchases(), new ItemSale11(), new Drink(), new Cure()};
    else
      return new AddRemovable[]{new Purchases(), new ItemSale(), new Drink(), new Cure()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public int getViewerTabCount()
  {
    return 2;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return StructViewer.TAB_RAW;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
      {
        JScrollPane scroll = new JScrollPane(new Viewer(this));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
      }
      case 1:
      {
        if (hexViewer == null) {
          hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
      }
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    TextString version = new TextString(buffer, offset + 4, 4, COMMON_VERSION);
    addField(version);
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new Bitmap(buffer, offset + 8, 4, STO_TYPE, s_type_bg2));
      addField(new StringRef(buffer, offset + 12, STO_NAME));
    } else {
      addField(new Bitmap(buffer, offset + 8, 4, STO_TYPE, s_type9));
      addField(new StringRef(buffer, offset + 12, STO_NAME));
    }
    addField(new Flag(buffer, offset + 16, 4, STO_FLAGS, s_flag_bg2));
    addField(new DecNumber(buffer, offset + 20, 4, STO_MARKUP_SELL));
    addField(new DecNumber(buffer, offset + 24, 4, STO_MARKUP_BUY));
    addField(new DecNumber(buffer, offset + 28, 4, STO_DEPRECIATION_RATE));
//    addField(new Unknown(buffer, offset + 30, 2));
    addField(new DecNumber(buffer, offset + 32, 2, STO_STEALING_DIFFICULTY));
    if (version.toString().equalsIgnoreCase("V9.0")) {
      addField(new Unknown(buffer, offset + 34, 2));
    } else {
      addField(new UnsignDecNumber(buffer, offset + 34, 2, STO_STORAGE_CAPACITY));
    }
    addField(new Unknown(buffer, offset + 36, 8));
    SectionOffset offset_purchased = new SectionOffset(buffer, offset + 44, STO_OFFSET_ITEMS_PURCHASED,
                                                       Purchases.class);
    addField(offset_purchased);
    SectionCount count_purchased = new SectionCount(buffer, offset + 48, 4, STO_NUM_ITEMS_PURCHASED,
                                                    Purchases.class);
    addField(count_purchased);
    SectionOffset offset_sale;
    SectionCount count_sale;
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      offset_sale = new SectionOffset(buffer, offset + 52, STO_OFFSET_ITEMS_FOR_SALE,
                                      ItemSale.class);
      addField(offset_sale);
      count_sale = new SectionCount(buffer, offset + 56, 4, STO_NUM_ITEMS_FOR_SALE,
                                    ItemSale.class);
      addField(count_sale);
    }
    else if (version.toString().equals("V1.1")) {
      offset_sale = new SectionOffset(buffer, offset + 52, STO_OFFSET_ITEMS_FOR_SALE,
                                      ItemSale11.class);
      addField(offset_sale);
      count_sale = new SectionCount(buffer, offset + 56, 4, STO_NUM_ITEMS_FOR_SALE,
                                    ItemSale11.class);
      addField(count_sale);
    }
    else {
      clearFields();
      throw new Exception("Unsupported version: " + version);
    }
    addField(new DecNumber(buffer, offset + 60, 4, STO_LORE));
    addField(new DecNumber(buffer, offset + 64, 4, STO_COST_TO_IDENTIFY));
    addField(new ResourceRef(buffer, offset + 68, STO_RUMORS_DRINKS, "DLG"));
    SectionOffset offset_drinks = new SectionOffset(buffer, offset + 76, STO_OFFSET_DRINKS,
                                                    Drink.class);
    addField(offset_drinks);
    SectionCount count_drinks = new SectionCount(buffer, offset + 80, 4, STO_NUM_DRINKS,
                                                 Drink.class);
    addField(count_drinks);
    addField(new ResourceRef(buffer, offset + 84, STO_RUMORS_DONATIONS, "DLG"));
    addField(new Flag(buffer, offset + 92, 4, STO_ROOMS_AVAILABLE, s_rooms));
    addField(new DecNumber(buffer, offset + 96, 4, STO_PRICE_ROOM_PEASANT));
    addField(new DecNumber(buffer, offset + 100, 4, STO_PRICE_ROOM_MERCHANT));
    addField(new DecNumber(buffer, offset + 104, 4, STO_PRICE_ROOM_NOBLE));
    addField(new DecNumber(buffer, offset + 108, 4, STO_PRICE_ROOM_ROYAL));
    SectionOffset offset_cures = new SectionOffset(buffer, offset + 112, STO_OFFSET_CURES,
                                                   Cure.class);
    addField(offset_cures);
    SectionCount count_cures = new SectionCount(buffer, offset + 116, 4, STO_NUM_CURES,
                                                Cure.class);
    addField(count_cures);
    addField(new Unknown(buffer, offset + 120, 36));
    if (version.toString().equals("V9.0")) {
      addField(new UnsignDecNumber(buffer, offset + 156, 2, STO_STORAGE_CAPACITY));
      addField(new Unknown(buffer, offset + 158, 82));
    }

    offset = offset_drinks.getValue();
    for (int i = 0; i < count_drinks.getValue(); i++) {
      Drink drink = new Drink(this, buffer, offset, i);
      offset = drink.getEndOffset();
      addField(drink);
    }

    offset = offset_sale.getValue();
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      for (int i = 0; i < count_sale.getValue(); i++) {
        ItemSale sale = new ItemSale(this, buffer, offset, i);
        offset = sale.getEndOffset();
        addField(sale);
      }
    }
    else if (version.toString().equals("V1.1")) {
      for (int i = 0; i < count_sale.getValue(); i++) {
        ItemSale11 sale = new ItemSale11(this, buffer, offset, i);
        offset = sale.getEndOffset();
        addField(sale);
      }
    }

    offset = offset_cures.getValue();
    for (int i = 0; i < count_cures.getValue(); i++) {
      Cure cure = new Cure(this, buffer, offset, i);
      offset = cure.getEndOffset();
      addField(cure);
    }

    offset = offset_purchased.getValue();
    for (int i = 0; i < count_purchased.getValue(); i++) {
      Purchases pur = new Purchases(buffer, offset, i);
      offset += pur.getSize();
      addField(pur);
    }

    int endoffset = offset;
    for (final StructEntry entry : getFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  /**
   * Checks whether the specified resource entry matches all available search options.
   * Called by "Extended Search"
   */
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        StoResource sto = new StoResource(entry);
        Bitmap[] purchases;
        ResourceRef[] items;
        boolean retVal = true;
        String key;
        Object o;

        // preparations
        IsNumeric ofs = (IsNumeric)sto.getAttribute(STO_OFFSET_ITEMS_FOR_SALE, false);
        IsNumeric cnt = (IsNumeric)sto.getAttribute(STO_NUM_ITEMS_FOR_SALE, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          String itemLabel = SearchOptions.getResourceName(SearchOptions.STO_Item_Item1);
          items = new ResourceRef[cnt.getValue()];
          for (int i = 0; i < cnt.getValue(); i++) {
            String itemStruct = String.format(SearchOptions.getResourceName(SearchOptions.STO_Item), i);
            if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
              ItemSale11 item = (ItemSale11)sto.getAttribute(itemStruct, false);
              if (item != null) {
                items[i] = (ResourceRef)item.getAttribute(itemLabel, false);
              } else {
                items[i] = null;
              }
            } else {
              ItemSale item = (ItemSale)sto.getAttribute(itemStruct, false);
              if (item != null) {
                items[i] = (ResourceRef)item.getAttribute(itemLabel, false);
              } else {
                items[i] = null;
              }
            }
          }
        } else {
          items = new ResourceRef[0];
        }

        ofs = (IsNumeric)sto.getAttribute(STO_OFFSET_ITEMS_PURCHASED, false);
        cnt = (IsNumeric)sto.getAttribute(STO_NUM_ITEMS_PURCHASED, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          purchases = new Bitmap[cnt.getValue()];
          for (int i = 0; i < cnt.getValue(); i++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.STO_Purchased), i);
            purchases[i] = (Bitmap)sto.getAttribute(label, false);
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
          retVal &= SearchOptions.Utils.matchNumber(struct, o);
        }

        String[] keyList = new String[]{SearchOptions.STO_Purchased1, SearchOptions.STO_Purchased2,
                                        SearchOptions.STO_Purchased3, SearchOptions.STO_Purchased4,
                                        SearchOptions.STO_Purchased5};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            boolean found = false;
            for (int idx2 = 0; idx2 < purchases.length; idx2++) {
              if (purchases[idx2] != null) {
                found |= SearchOptions.Utils.matchNumber(purchases[idx2], o);
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.STO_Flags, SearchOptions.STO_RoomsAvailable};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key), false);
            retVal &= SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.STO_Depreciation, SearchOptions.STO_SellMarkup,
                               SearchOptions.STO_BuyMarkup, SearchOptions.STO_Stealing,
                               SearchOptions.STO_Capacity};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key), false);
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.STO_Item_Item1, SearchOptions.STO_Item_Item2,
                               SearchOptions.STO_Item_Item3, SearchOptions.STO_Item_Item4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            boolean found = false;
            for (int idx2 = 0; idx2 < items.length; idx2++) {
              if (items[idx2] != null) {
                found |= SearchOptions.Utils.matchResourceRef(items[idx2], o, false);
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.STO_Custom1, SearchOptions.STO_Custom2,
                               SearchOptions.STO_Custom3, SearchOptions.STO_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(sto, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}
