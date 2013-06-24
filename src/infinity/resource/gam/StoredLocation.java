// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

class StoredLocation extends AbstractStruct implements AddRemovable
{
  StoredLocation() throws Exception
  {
    super(null, "Stored location", new byte[12], 0);
  }

  StoredLocation(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Stored location", buffer, offset);
  }

  StoredLocation(AbstractStruct superStruct, String s, byte b[], int o) throws Exception
  {
    super(superStruct, s, b, o);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Area", "ARE"));
    list.add(new DecNumber(buffer, offset + 8, 2, "Saved location: X"));
    list.add(new DecNumber(buffer, offset + 10, 2, "Saved location: Y"));
    return offset + 12;
  }
}

