// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;

public final class Item extends AbstractStruct implements AddRemovable
{
  // CRE/Item-specific field labels
  public static final String CRE_ITEM             = "Item";
  public static final String CRE_ITEM_RESREF      = "Item";
  public static final String CRE_ITEM_DURATION    = "Duration";
  public static final String CRE_ITEM_QUANTITY_1  = "Quantity/Charges 1";
  public static final String CRE_ITEM_QUANTITY_2  = "Quantity/Charges 2";
  public static final String CRE_ITEM_QUANTITY_3  = "Quantity/Charges 3";
  public static final String CRE_ITEM_FLAGS       = "Flags";

  private static final String[] s_itemflag = {"No flags set", "Identified", "Not stealable", "Stolen",
                                              "Undroppable"};

  public Item() throws Exception
  {
    super(null, CRE_ITEM, new byte[20], 0);
  }

  public Item(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, CRE_ITEM + " " + nr, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, CRE_ITEM_RESREF, "ITM"));
    if (Profile.isEnhancedEdition()) {
      addField(new UnsignDecNumber(buffer, offset + 8, 2, CRE_ITEM_DURATION));
    } else {
      addField(new Unknown(buffer, offset + 8, 2));
    }
    addField(new DecNumber(buffer, offset + 10, 2, CRE_ITEM_QUANTITY_1));
    addField(new DecNumber(buffer, offset + 12, 2, CRE_ITEM_QUANTITY_2));
    addField(new DecNumber(buffer, offset + 14, 2, CRE_ITEM_QUANTITY_3));
    addField(new Flag(buffer, offset + 16, 4, CRE_ITEM_FLAGS, s_itemflag));
    return offset + 20;
  }
}

