// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.DecNumber;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Drink extends AbstractStruct implements AddRemovable
{
  Drink() throws Exception
  {
    super(null, "Drink", new byte[20], 0);
  }

  Drink(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Drink " + number, buffer, offset);
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
    addField(new Unknown(buffer, offset, 8));
    addField(new StringRef(buffer, offset + 8, "Drink name"));
    addField(new DecNumber(buffer, offset + 12, 4, "Price"));
    addField(new DecNumber(buffer, offset + 16, 4, "Rumor rate"));
    return offset + 20;
  }
}

