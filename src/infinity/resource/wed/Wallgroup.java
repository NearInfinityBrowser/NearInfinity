// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.DecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Wallgroup extends AbstractStruct implements AddRemovable
{
  Wallgroup() throws Exception
  {
    super(null, "Wall group", new byte[4], 0);
  }

  Wallgroup(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Wall group " + nr, buffer, offset, 2);
  }

  public int getNextPolygonIndex()
  {
    int count = ((DecNumber)getAttribute("# polygons")).getValue();
    int index = ((DecNumber)getAttribute("Polygon index")).getValue();
    return count + index;
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  public int read(byte buffer[], int offset)
  {
    list.add(new DecNumber(buffer, offset, 2, "Polygon index"));
    list.add(new DecNumber(buffer, offset + 2, 2, "# polygons"));
    return offset + 4;
  }
}

