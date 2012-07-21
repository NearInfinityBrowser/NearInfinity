// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

abstract class AreaLink extends AbstractStruct implements AddRemovable
{
  AreaLink(String name) throws Exception
  {
    super(null, name, new byte[216], 0);
  }

  AreaLink(AbstractStruct superStruct, byte buffer[], int offset, String name) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new DecNumber(buffer, offset, 4, "Target area"));
    list.add(new TextString(buffer, offset + 4, 32, "Target entrance"));
    list.add(new DecNumber(buffer, offset + 36, 4, "Distance scale"));
    list.add(new Flag(buffer, offset + 40, 4, "Default entrance",
                      new String[]{"No default set", "North", "East", "South", "West"}));
    list.add(new ResourceRef(buffer, offset + 44, "Random encounter area 1", "ARE"));
    list.add(new ResourceRef(buffer, offset + 52, "Random encounter area 2", "ARE"));
    list.add(new ResourceRef(buffer, offset + 60, "Random encounter area 3", "ARE"));
    list.add(new ResourceRef(buffer, offset + 68, "Random encounter area 4", "ARE"));
    list.add(new ResourceRef(buffer, offset + 76, "Random encounter area 5", "ARE"));
    list.add(new DecNumber(buffer, offset + 84, 4, "Random encounter probability"));
    list.add(new Unknown(buffer, offset + 88, 128));
    return offset + 216;
  }
}

