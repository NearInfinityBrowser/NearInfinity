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
  // GAM/StoredLocation-specific field labels
  public static final String GAM_LOC            = "Stored location";
  public static final String GAM_LOC_AREA       = "Area";
  public static final String GAM_LOC_LOCATION_X = "Saved location: X";
  public static final String GAM_LOC_LOCATION_Y = "Saved location: Y";

  StoredLocation() throws Exception
  {
    super(null, GAM_LOC, new byte[12], 0);
  }

  StoredLocation(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    this(superStruct, GAM_LOC, buffer, offset, nr);
  }

  StoredLocation(AbstractStruct superStruct, String name, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, name + " " + nr, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, GAM_LOC_AREA, "ARE"));
    addField(new DecNumber(buffer, offset + 8, 2, GAM_LOC_LOCATION_X));
    addField(new DecNumber(buffer, offset + 10, 2, GAM_LOC_LOCATION_Y));
    return offset + 12;
  }
}

