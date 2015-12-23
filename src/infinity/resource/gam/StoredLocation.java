// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public class StoredLocation extends AbstractStruct implements AddRemovable
{
  StoredLocation() throws Exception
  {
    super(null, "Stored location", new byte[12], 0);
  }

  StoredLocation(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Stored location " + nr, buffer, offset);
  }

  StoredLocation(AbstractStruct superStruct, String s, byte b[], int o, int nr) throws Exception
  {
    super(superStruct, s + " " + nr, b, o);
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
    addField(new ResourceRef(buffer, offset, "Area", "ARE"));
    addField(new DecNumber(buffer, offset + 8, 2, "Saved location: X"));
    addField(new DecNumber(buffer, offset + 10, 2, "Saved location: Y"));
    return offset + 12;
  }
}

