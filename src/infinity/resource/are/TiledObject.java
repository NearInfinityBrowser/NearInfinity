// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class TiledObject extends AbstractStruct implements AddRemovable
{
  public static final String[] s_flag = { "No flags set", "Secondary tile", "Can be looked through" };

  TiledObject() throws Exception
  {
    super(null, "Tiled object", new byte[108], 0);
  }

  TiledObject(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Tiled object " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new TextString(buffer, offset + 32, 8, "Tile ID"));
    addField(new Flag(buffer, offset + 40, 4, "Tile flags", s_flag));
    addField(new DecNumber(buffer, offset + 44, 4, "First vertex index (primary)"));
    addField(new DecNumber(buffer, offset + 48, 2, "# vertices (primary)"));
    addField(new DecNumber(buffer, offset + 50, 2, "# vertices (secondary)"));
    addField(new DecNumber(buffer, offset + 52, 4, "First vertex index (secondary)"));
    addField(new Unknown(buffer, offset + 60, 48));
    return offset + 108;
  }
}

