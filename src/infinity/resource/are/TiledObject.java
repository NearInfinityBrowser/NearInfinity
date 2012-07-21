// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class TiledObject extends AbstractStruct implements AddRemovable
{
  TiledObject() throws Exception
  {
    super(null, "Tiled object", new byte[108], 0);
  }

  TiledObject(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Tiled object", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new TextString(buffer, offset + 32, 8, "Resource?"));
    list.add(new Unknown(buffer, offset + 40, 4));
    list.add(new DecNumber(buffer, offset + 44, 4, "Primary search squares offset"));
    list.add(new DecNumber(buffer, offset + 48, 4, "# primary search squares"));
    list.add(new DecNumber(buffer, offset + 52, 4, "Secondary search squares offset"));
    list.add(new DecNumber(buffer, offset + 56, 4, "# secondary search squares"));
    list.add(new Unknown(buffer, offset + 60, 48));
    return offset + 108;
  }
}

