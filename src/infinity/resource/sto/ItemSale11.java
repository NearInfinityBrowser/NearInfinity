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

final class ItemSale11 extends AbstractStruct implements AddRemovable
{
  private static final String[] s_itemflag = {"No flags set", "Identified", "Not stealable", "Stolen"};
  private static final String[] s_noyes = { "No", "Yes" };

  ItemSale11() throws Exception
  {
    super(null, "Item for sale", new byte[88], 0);
  }

  ItemSale11(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Item for sale " + number, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, "Item", "ITM"));
    addField(new Unknown(buffer, offset + 8, 2));
    addField(new DecNumber(buffer, offset + 10, 2, "Quantity/Charges 1"));
    addField(new DecNumber(buffer, offset + 12, 2, "Quantity/Charges 2"));
    addField(new DecNumber(buffer, offset + 14, 2, "Quantity/Charges 3"));
    addField(new Flag(buffer, offset + 16, 4, "Flags", s_itemflag));
    addField(new DecNumber(buffer, offset + 20, 4, "# in stock"));
    addField(new Bitmap(buffer, offset + 24, 4, "Infinite supply?", s_noyes));
    addField(new StringRef(buffer, offset + 28, "Sale trigger"));
    addField(new Unknown(buffer, offset + 32, 56));
//    addField(new Unknown(buffer, offset + 76, 12));
    return offset + 88;
  }
}

