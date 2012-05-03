// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;

public final class StoResource extends AbstractStruct implements Resource, HasAddRemovable, HasDetailViewer
{
//  private static final String[] s_type = {"Store", "Tavern", "Inn", "Temple"};
  private static final String[] s_type9 = {"Store", "Tavern", "Inn", "Temple", "Container"};
  private static final String[] s_type_bg2 = {"Store", "Tavern", "Inn", "Temple", "", "Container"};
//  private static final String[] s_flag = {"Can't do anything", "Can buy", "Can sell", "Can identify",
//                                          "Can steal", "Can buy cures", "Can donate",
//                                          "Can buy drinks", "", "", "Quality Bit 0 (BAM)", "Quality Bit 1 (BAM)"};
  private static final String[] s_flag_bg2 = {"Can only rest", "Can buy", "Can sell", "Can identify",
                                              "Can steal", "Can donate", "Can buy cures",
                                              "Can buy drinks", "", "", "Tavern quality 1", "Tavern quality 2",
                                              "", "Fence", "", "Ex: toggle recharge"};
  private static final String[] s_rooms = {"No rooms available", "Peasant", "Merchant", "Noble", "Royal"};

  public static String getSearchString(byte buffer[])
  {
    return new StringRef(buffer, 12, "").toString().trim();
  }

  public StoResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    TextString version = (TextString)getAttribute("Version");
    if (version.toString().equals("V1.1"))
      return new AddRemovable[]{new Purchases(), new ItemSale11(), new Drink(), new Cure()};
    else
      return new AddRemovable[]{new Purchases(), new ItemSale(), new Drink(), new Cure()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    JScrollPane scroll = new JScrollPane(new Viewer(this));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

// --------------------- End Interface HasDetailViewer ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    list.add(version);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_TUTU) {
      list.add(new Bitmap(buffer, offset + 8, 4, "Type", s_type_bg2));
      list.add(new StringRef(buffer, offset + 12, "Name"));
      list.add(new Flag(buffer, offset + 16, 4, "Flags", s_flag_bg2));
    }
//    else if (version.toString().equalsIgnoreCase("V9.0")) {
//      list.add(new Bitmap(buffer, offset + 8, 4, "Type", s_type9));
//      list.add(new StringRef(buffer, offset + 12, "Name"));
//      list.add(new Flag(buffer, offset + 16, 4, "Flags", s_flag));
//    }
    else {
      list.add(new Bitmap(buffer, offset + 8, 4, "Type", s_type9));
      list.add(new StringRef(buffer, offset + 12, "Name"));
      list.add(new Flag(buffer, offset + 16, 4, "Flags", s_flag_bg2));
    }
    list.add(new DecNumber(buffer, offset + 20, 4, "Sell markup"));
    list.add(new DecNumber(buffer, offset + 24, 4, "Buy markup"));
    list.add(new DecNumber(buffer, offset + 28, 4, "Depreciation rate"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Stealing difficulty"));
    if (version.toString().equalsIgnoreCase("V9.0"))
      list.add(new Unknown(buffer, offset + 34, 2));
    else
      list.add(new UnsignDecNumber(buffer, offset + 34, 2, "Storage capacity"));
    list.add(new Unknown(buffer, offset + 36, 8));
    SectionOffset offset_purchased = new SectionOffset(buffer, offset + 44, "Items purchased offset",
                                                       Purchases.class);
    list.add(offset_purchased);
    SectionCount count_purchased = new SectionCount(buffer, offset + 48, 4, "# items purchased",
                                                    Purchases.class);
    list.add(count_purchased);
    SectionOffset offset_sale;
    SectionCount count_sale;
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      offset_sale = new SectionOffset(buffer, offset + 52, "Items for sale offset",
                                      ItemSale.class);
      list.add(offset_sale);
      count_sale = new SectionCount(buffer, offset + 56, 4, "# items for sale",
                                    ItemSale.class);
      list.add(count_sale);
    }
    else if (version.toString().equals("V1.1")) {
      offset_sale = new SectionOffset(buffer, offset + 52, "Items for sale offset",
                                      ItemSale11.class);
      list.add(offset_sale);
      count_sale = new SectionCount(buffer, offset + 56, 4, "# items for sale",
                                    ItemSale11.class);
      list.add(count_sale);
    }
    else {
      list.clear();
      throw new Exception("Unsupported version: " + version);
    }
    list.add(new DecNumber(buffer, offset + 60, 4, "Lore"));
    list.add(new DecNumber(buffer, offset + 64, 4, "Cost to identify"));
    list.add(new ResourceRef(buffer, offset + 68, "Rumors (drinks)", "DLG"));
    SectionOffset offset_drinks = new SectionOffset(buffer, offset + 76, "Drinks offset",
                                                    Drink.class);
    list.add(offset_drinks);
    SectionCount count_drinks = new SectionCount(buffer, offset + 80, 4, "# drinks for sale",
                                                 Drink.class);
    list.add(count_drinks);
    list.add(new ResourceRef(buffer, offset + 84, "Rumors (donations)", "DLG"));
    list.add(new Flag(buffer, offset + 92, 4, "Available rooms", s_rooms));
    list.add(new DecNumber(buffer, offset + 96, 4, "Price peasant room"));
    list.add(new DecNumber(buffer, offset + 100, 4, "Price merchant room"));
    list.add(new DecNumber(buffer, offset + 104, 4, "Price noble room"));
    list.add(new DecNumber(buffer, offset + 108, 4, "Price royal room"));
    SectionOffset offset_cures = new SectionOffset(buffer, offset + 112, "Cures offset",
                                                   Cure.class);
    list.add(offset_cures);
    SectionCount count_cures = new SectionCount(buffer, offset + 116, 4, "# cures for sale",
                                                Cure.class);
    list.add(count_cures);
    list.add(new Unknown(buffer, offset + 120, 36));
    if (version.toString().equals("V9.0")) {
      list.add(new UnsignDecNumber(buffer, offset + 156, 2, "Storage capacity"));
      list.add(new Unknown(buffer, offset + 158, 82));
    }

    offset = offset_drinks.getValue();
    for (int i = 0; i < count_drinks.getValue(); i++) {
      Drink drink = new Drink(this, buffer, offset);
      offset = drink.getEndOffset();
      list.add(drink);
    }

    offset = offset_sale.getValue();
    if (version.toString().equals("V1.0") || version.toString().equals("V9.0")) {
      for (int i = 0; i < count_sale.getValue(); i++) {
        ItemSale sale = new ItemSale(this, buffer, offset);
        offset = sale.getEndOffset();
        list.add(sale);
      }
    }
    else if (version.toString().equals("V1.1")) {
      for (int i = 0; i < count_sale.getValue(); i++) {
        ItemSale11 sale = new ItemSale11(this, buffer, offset);
        offset = sale.getEndOffset();
        list.add(sale);
      }
    }

    offset = offset_cures.getValue();
    for (int i = 0; i < count_cures.getValue(); i++) {
      Cure cure = new Cure(this, buffer, offset);
      offset = cure.getEndOffset();
      list.add(cure);
    }

    offset = offset_purchased.getValue();
    for (int i = 0; i < count_purchased.getValue(); i++) {
      Purchases pur = new Purchases(buffer, offset);
      offset += pur.getSize();
      list.add(pur);
    }

    int endoffset = offset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }
}

