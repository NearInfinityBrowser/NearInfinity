// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasIcon;
import org.infinity.util.io.StreamUtils;

/**
 * Structure for the item for sale in the store, used by games:
 * <ul>
 * <li>Baldur's Gate: Tales of the Swoard Coast</li>
 * <li>Baldur's Gate II: Throne of Baal</li>
 * <li>Icewind Dale: Heart of Winter</li>
 * <li>Icewind Dale: Enhanced Edition</li>
 * <li>Icewind Dale II</li>
 * </ul>
 */
public final class ItemSale extends AbstractStruct implements AddRemovable, HasIcon
{
  // STO/ItemSale-specific field labels
  public static final String STO_SALE                 = "Item for sale";
  /** Name of the field with ResRef to the item to sale. */
  public static final String STO_SALE_ITEM            = "Item";
  public static final String STO_SALE_EXPIRATION      = "Expiration time";
  public static final String STO_SALE_QUANTITY_FMT    = "Quantity/Charges %d";
  public static final String STO_SALE_FLAGS           = "Flags";
  public static final String STO_SALE_NUM_IN_STOCK    = "# in stock";
  public static final String STO_SALE_INFINITE_SUPPLY = "Infinite supply?";

  public static final String[] s_itemflag = {"No flags set", "Identified", "Not stealable", "Stolen",
                                             "Undroppable"};
  public static final String[] s_noyes = { "No", "Yes" };

  ItemSale() throws Exception
  {
    super(null, STO_SALE, StreamUtils.getByteBuffer(28), 0);
  }

  ItemSale(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
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
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, STO_SALE_ITEM, "ITM"));
    addField(new DecNumber(buffer, offset + 8, 2, STO_SALE_EXPIRATION));
    for (int i = 0; i < 3; i++) {
      addField(new DecNumber(buffer, offset + 10 + (i * 2), 2, String.format(STO_SALE_QUANTITY_FMT, i+1)));
    }
    addField(new Flag(buffer, offset + 16, 4, STO_SALE_FLAGS, s_itemflag));
    addField(new DecNumber(buffer, offset + 20, 4, STO_SALE_NUM_IN_STOCK));
    addField(new Bitmap(buffer, offset + 24, 4, STO_SALE_INFINITE_SUPPLY, s_noyes));
    return offset + 28;
  }

  @Override
  public ResourceRef getIcon() { return getIndirectIcon(STO_SALE_ITEM); }
}
