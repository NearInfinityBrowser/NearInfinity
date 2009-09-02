// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Entrance extends AbstractStruct implements AddRemovable
{
  Entrance() throws Exception
  {
    super(null, "Entrance", new byte[104], 0);
  }

  Entrance(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Entrance", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    list.add(new Bitmap(buffer, offset + 36, 4, "Orientation", Actor.s_orientation));
    list.add(new Unknown(buffer, offset + 40, 64));
    return offset + 104;
  }
}

