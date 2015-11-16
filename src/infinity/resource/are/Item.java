// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Item extends AbstractStruct implements AddRemovable
{
  public static final String[] s_flags = {"No flags set", "Identified", "Not stealable", "Stolen",
                                          "Undroppable"};
  private int nr = -1;

  Item() throws Exception
  {
    super(null, "Item", new byte[20], 0);
  }

  Item(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Item", buffer, offset);
    this.nr = nr;
  }

// --------------------- Begin Interface StructEntry ---------------------

  @Override
  public String getName()
  {
    if (nr == -1)
      return "Item";
    return "Item " + nr;
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
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, "Item", "ITM"));
    addField(new Unknown(buffer, offset + 8, 2));
    addField(new DecNumber(buffer, offset + 10, 2, "Quantity/Charges 1"));
    addField(new DecNumber(buffer, offset + 12, 2, "Quantity/Charges 2"));
    addField(new DecNumber(buffer, offset + 14, 2, "Quantity/Charges 3"));
    addField(new Flag(buffer, offset + 16, 4, "Flags", s_flags));
    return offset + 20;
  }
}

