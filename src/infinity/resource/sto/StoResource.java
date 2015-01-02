// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public final class StoResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
//  private static final String[] s_type = {"Store", "Tavern", "Inn", "Temple"};
  public static final String[] s_type9 = {"Store", "Tavern", "Inn", "Temple", "Container"};
  public static final String[] s_type_bg2 = {"Store", "Tavern", "Inn", "Temple", "", "Container"};
//  private static final String[] s_flag = {"Can't do anything", "Can buy", "Can sell", "Can identify",
//                                          "Can steal", "Can buy cures", "Can donate",
//                                          "Can buy drinks", "", "", "Quality Bit 0 (BAM)", "Quality Bit 1 (BAM)"};
  public static final String[] s_flag_bg2 = {"Can only rest", "Can buy", "Can sell", "Can identify",
                                              "Can steal", "Can donate", "Can buy cures",
                                              "Can buy drinks", "", "", "Tavern quality 1", "Tavern quality 2",
                                              "", "Fence", "EE: Ignore reputation", "Ex: Toggle recharge",
                                              "EE: Can sell critical"};
  public static final String[] s_rooms = {"No rooms available", "Peasant", "Merchant", "Noble", "Royal"};

  private HexViewer hexViewer;

  public static String getSearchString(byte buffer[])
  {
    return new StringRef(buffer, 12, "").toString().trim();
  }

  public StoResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    TextString version = (TextString)getAttribute("Version");
    if (version.toString().equals("V1.1"))
      return new AddRemovable[]{new Purchases(), new ItemSale11(), new Drink(), new Cure()};
    else
      return new AddRemovable[]{new Purchases(), new ItemSale(), new Drink(), new Cure()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

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
          hexViewer = new HexViewer(this, new BasicColorMap(this, true));
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

// --------------------- End Interface HasViewerTabs ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    addField(version);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.isEnhancedEdition()) {
      addField(new Bitmap(buffer, offset + 8, 4, "Type", s_type_bg2));
      addField(new StringRef(buffer, offset + 12, "Name"));
      addField(new Flag(buffer, offset + 16, 4, "Flags", s_flag_bg2));
    }
//    else if (version.toString().equalsIgnoreCase("V9.0")) {
//      addField(new Bitmap(buffer, offset + 8, 4, "Type", s_type9));
//      addField(new StringRef(buffer, offset + 12, "Name"));
//      addField(new Flag(buffer, offset + 16, 4, "Flags", s_flag));
//    }
    else {
      addField(new Bitmap(buffer, offset + 8, 4, "Type", s_type9));
      addField(new StringRef(buffer, offset + 12, "Name"));
      addField(new Flag(buffer, offset + 16, 4, "Flags", s_flag_bg2));
    }
    addField(new DecNumber(buffer, offset + 20, 4, "Sell markup"));
    addField(new DecNumber(buffer, offset + 24, 4, "Buy markup"));
    addField(new DecNumber(buffer, offset + 28, 4, "Depreciation rate"));
//    addField(new Unknown(buffer, offset + 30, 2));
    addField(new DecNumber(buffer, offset + 32, 2, "Stealing difficulty"));
    if (version.toString().equalsIgnoreCase("V9.0")) {
      addField(new Unknown(buffer, offset + 34, 2));
    } else {
      addField(new UnsignDecNumber(buffer, offset + 34, 2, "Storage capacity"));
    }
    addField(new Unknown(buffer, offset + 36, 8));
    SectionOffset offset_purchased = new SectionOffset(buffer, offset + 44, "Items purchased offset",
                                                       Purchases.class);
    addField(offset_purchased);
    SectionCount count_purchased = new SectionCount(buffer, offset + 48, 4, "# items purchased",
                                                    Purchases.class);
    addField(count_purchased);
    SectionOffset offset_sale;
    SectionCount count_sale;
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      offset_sale = new SectionOffset(buffer, offset + 52, "Items for sale offset",
                                      ItemSale.class);
      addField(offset_sale);
      count_sale = new SectionCount(buffer, offset + 56, 4, "# items for sale",
                                    ItemSale.class);
      addField(count_sale);
    }
    else if (version.toString().equals("V1.1")) {
      offset_sale = new SectionOffset(buffer, offset + 52, "Items for sale offset",
                                      ItemSale11.class);
      addField(offset_sale);
      count_sale = new SectionCount(buffer, offset + 56, 4, "# items for sale",
                                    ItemSale11.class);
      addField(count_sale);
    }
    else {
      clearFields();
      throw new Exception("Unsupported version: " + version);
    }
    addField(new DecNumber(buffer, offset + 60, 4, "Lore"));
    addField(new DecNumber(buffer, offset + 64, 4, "Cost to identify"));
    addField(new ResourceRef(buffer, offset + 68, "Rumors (drinks)", "DLG"));
    SectionOffset offset_drinks = new SectionOffset(buffer, offset + 76, "Drinks offset",
                                                    Drink.class);
    addField(offset_drinks);
    SectionCount count_drinks = new SectionCount(buffer, offset + 80, 4, "# drinks for sale",
                                                 Drink.class);
    addField(count_drinks);
    addField(new ResourceRef(buffer, offset + 84, "Rumors (donations)", "DLG"));
    addField(new Flag(buffer, offset + 92, 4, "Available rooms", s_rooms));
    addField(new DecNumber(buffer, offset + 96, 4, "Price peasant room"));
    addField(new DecNumber(buffer, offset + 100, 4, "Price merchant room"));
    addField(new DecNumber(buffer, offset + 104, 4, "Price noble room"));
    addField(new DecNumber(buffer, offset + 108, 4, "Price royal room"));
    SectionOffset offset_cures = new SectionOffset(buffer, offset + 112, "Cures offset",
                                                   Cure.class);
    addField(offset_cures);
    SectionCount count_cures = new SectionCount(buffer, offset + 116, 4, "# cures for sale",
                                                Cure.class);
    addField(count_cures);
    addField(new Unknown(buffer, offset + 120, 36));
    if (version.toString().equals("V9.0")) {
      addField(new UnsignDecNumber(buffer, offset + 156, 2, "Storage capacity"));
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
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
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

  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
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
        DecNumber ofs = (DecNumber)sto.getAttribute("Items for sale offset");
        DecNumber cnt = (DecNumber)sto.getAttribute("# items for sale");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          boolean isPST = (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT);
          String itemLabel = SearchOptions.getResourceName(SearchOptions.STO_Item_Item1);
          items = new ResourceRef[cnt.getValue()];
          for (int i = 0; i < cnt.getValue(); i++) {
            String itemStruct = String.format(SearchOptions.getResourceName(SearchOptions.STO_Item), i);
            if (isPST) {
              ItemSale11 item = (ItemSale11)sto.getAttribute(itemStruct);
              if (item != null) {
                items[i] = (ResourceRef)item.getAttribute(itemLabel);
              } else {
                items[i] = null;
              }
            } else {
              ItemSale item = (ItemSale)sto.getAttribute(itemStruct);
              if (item != null) {
                items[i] = (ResourceRef)item.getAttribute(itemLabel);
              } else {
                items[i] = null;
              }
            }
          }
        } else {
          items = new ResourceRef[0];
        }

        ofs = (DecNumber)sto.getAttribute("Items purchased offset");
        cnt = (DecNumber)sto.getAttribute("# items purchased");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          purchases = new Bitmap[cnt.getValue()];
          for (int i = 0; i < cnt.getValue(); i++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.STO_Purchased), i);
            purchases[i] = (Bitmap)sto.getAttribute(label);
          }
        } else {
          purchases = new Bitmap[0];
        }

        if (retVal) {
          key = SearchOptions.STO_Name;
          o = searchOptions.getOption(key);
          StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key));
          retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
        }

        if (retVal) {
          key = SearchOptions.STO_Type;
          o = searchOptions.getOption(key);
          StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key));
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
            StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key));
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
            StructEntry struct = sto.getAttribute(SearchOptions.getResourceName(key));
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

