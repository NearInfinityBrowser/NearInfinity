// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Iwd2Ability extends AbstractStruct implements AddRemovable
{
  public Iwd2Ability() throws Exception
  {
    super(null, "Ability", new byte[16], 0);
  }

  public Iwd2Ability(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Ability", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new IwdRef(buffer, offset, "ResRef", "LISTINNT.2DA"));
    list.add(new DecNumber(buffer, offset + 4, 4, "# memorizable"));
    list.add(new DecNumber(buffer, offset + 8, 4, "# remaining"));
    list.add(new Unknown(buffer, offset + 12, 4));
    return offset + 16;
  }
}

