// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.resource.AbstractStruct;

class AreaLinkWest extends AreaLink
{
  AreaLinkWest() throws Exception
  {
    super("West link");
  }

  AreaLinkWest(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, buffer, offset, "West link");
  }
}
