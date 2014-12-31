// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

abstract class AreaLink extends AbstractStruct
{
  AreaLink(String name) throws Exception
  {
    super(null, name, new byte[216], 0);
  }

  AreaLink(AbstractStruct superStruct, byte buffer[], int offset, String name) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 4, "Target area"));
    addField(new TextString(buffer, offset + 4, 32, "Target entrance"));
    addField(new DecNumber(buffer, offset + 36, 4, "Distance scale"));
    addField(new Flag(buffer, offset + 40, 4, "Default entrance",
                      new String[]{"No default set", "North", "East", "South", "West"}));
    addField(new ResourceRef(buffer, offset + 44, "Random encounter area 1", "ARE"));
    addField(new ResourceRef(buffer, offset + 52, "Random encounter area 2", "ARE"));
    addField(new ResourceRef(buffer, offset + 60, "Random encounter area 3", "ARE"));
    addField(new ResourceRef(buffer, offset + 68, "Random encounter area 4", "ARE"));
    addField(new ResourceRef(buffer, offset + 76, "Random encounter area 5", "ARE"));
    addField(new DecNumber(buffer, offset + 84, 4, "Random encounter probability"));
    addField(new Unknown(buffer, offset + 88, 128));
    return offset + 216;
  }
}

