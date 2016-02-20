// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.DecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Wallgroup extends AbstractStruct implements AddRemovable
{
  // WED/Wallgroup-specific field labels
  public static final String WED_WALLGROUP                = "Wall group";
  public static final String WED_WALLGROUP_POLYGON_INDEX  = "Polygon index";
  public static final String WED_WALLGROUP_NUM_POLYGONS   = "# polygons";

  public Wallgroup() throws Exception
  {
    super(null, WED_WALLGROUP, new byte[4], 0);
  }

  public Wallgroup(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, WED_WALLGROUP + " " + nr, buffer, offset, 2);
  }

  public int getNextPolygonIndex()
  {
    int count = ((DecNumber)getAttribute(WED_WALLGROUP_NUM_POLYGONS)).getValue();
    int index = ((DecNumber)getAttribute(WED_WALLGROUP_POLYGON_INDEX)).getValue();
    return count + index;
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset)
  {
    addField(new DecNumber(buffer, offset, 2, WED_WALLGROUP_POLYGON_INDEX));
    addField(new DecNumber(buffer, offset + 2, 2, WED_WALLGROUP_NUM_POLYGONS));
    return offset + 4;
  }
}

