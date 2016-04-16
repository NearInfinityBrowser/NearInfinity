// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class Item extends AbstractStruct implements AddRemovable
{
  // ARE/Item-specific field labels
  public static final String ARE_ITEM               = "Item";
  public static final String ARE_ITEM_RESREF        = "Item";
  public static final String ARE_ITEM_EXPIRY_TIME   = "Expiry time";
  public static final String ARE_ITEM_QUANTITY_FMT  = "Quantity/Charges %d";
  public static final String ARE_ITEM_FLAGS         = "Flags";

  public static final String[] s_flags = {"No flags set", "Identified", "Not stealable", "Stolen",
                                          "Undroppable"};
  private int nr = -1;

  Item() throws Exception
  {
    super(null, ARE_ITEM, StreamUtils.getByteBuffer(20), 0);
  }

  Item(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, ARE_ITEM, buffer, offset);
    this.nr = nr;
  }

// --------------------- Begin Interface StructEntry ---------------------

  @Override
  public String getName()
  {
    if (nr == -1)
      return ARE_ITEM;
    return ARE_ITEM + " " + nr;
  }

// --------------------- End Interface StructEntry ---------------------


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
    addField(new ResourceRef(buffer, offset, ARE_ITEM_RESREF, "ITM"));
    addField(new DecNumber(buffer, offset + 8, 2, ARE_ITEM_EXPIRY_TIME));
    for (int i = 0; i < 3; i++) {
      addField(new DecNumber(buffer, offset + 10 + (i * 2), 2, String.format(ARE_ITEM_QUANTITY_FMT, i+1)));
    }
    addField(new Flag(buffer, offset + 16, 4, ARE_ITEM_FLAGS, s_flags));
    return offset + 20;
  }
}

