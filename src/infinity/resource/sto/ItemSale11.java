// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class ItemSale11 extends AbstractStruct implements AddRemovable
{
  // STO/ItemSale-specific field labels
  public static final String STO_SALE                 = "Item for sale";
  public static final String STO_SALE_ITEM            = "Item";
  public static final String STO_SALE_QUANTITY_FMT    = "Quantity/Charges %d";
  public static final String STO_SALE_FLAGS           = "Flags";
  public static final String STO_SALE_NUM_IN_STOCK    = "# in stock";
  public static final String STO_SALE_INFINITE_SUPPLY = "Infinite supply?";
  public static final String STO_SALE_TRIGGER         = "Sale trigger";

  public static final String[] s_itemflag = {"No flags set", "Identified", "Not stealable", "Stolen"};
  public static final String[] s_noyes = { "No", "Yes" };

  ItemSale11() throws Exception
  {
    super(null, STO_SALE, new byte[88], 0);
  }

  ItemSale11(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, STO_SALE + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, STO_SALE_ITEM, "ITM"));
    addField(new Unknown(buffer, offset + 8, 2));
    for (int i = 0; i < 3; i++) {
      addField(new DecNumber(buffer, offset + 10 + (i * 2), 2, String.format(STO_SALE_QUANTITY_FMT, i+1)));
    }
    addField(new Flag(buffer, offset + 16, 4, STO_SALE_FLAGS, s_itemflag));
    addField(new DecNumber(buffer, offset + 20, 4, STO_SALE_NUM_IN_STOCK));
    addField(new Bitmap(buffer, offset + 24, 4, STO_SALE_INFINITE_SUPPLY, s_noyes));
    addField(new StringRef(buffer, offset + 28, STO_SALE_TRIGGER));
    addField(new Unknown(buffer, offset + 32, 56));
    return offset + 88;
  }
}

