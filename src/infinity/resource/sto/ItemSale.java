// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class ItemSale extends AbstractStruct implements AddRemovable
{
  private static final String[] s_itemflag = {"No flags set", "Identified", "Not stealable", "Stolen",
                                              "Undroppable"};
  private static final String[] s_noyes = { "No", "Yes" };

  ItemSale() throws Exception
  {
    super(null, "Item for sale", new byte[28], 0);
  }

  ItemSale(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Item for sale", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Item", "ITM"));
    list.add(new Unknown(buffer, offset + 8, 2));
    list.add(new DecNumber(buffer, offset + 10, 2, "Quantity/Charges 1"));
    list.add(new DecNumber(buffer, offset + 12, 2, "Quantity/Charges 2"));
    list.add(new DecNumber(buffer, offset + 14, 2, "Quantity/Charges 3"));
    list.add(new Flag(buffer, offset + 16, 4, "Flags", s_itemflag));
    list.add(new DecNumber(buffer, offset + 20, 4, "# in stock"));
    list.add(new Bitmap(buffer, offset + 24, 4, "Infinite supply?", s_noyes));
    return offset + 28;
  }
}

