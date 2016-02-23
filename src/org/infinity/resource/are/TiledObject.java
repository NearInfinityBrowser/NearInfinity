// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;

public final class TiledObject extends AbstractStruct implements AddRemovable
{
  // ARE/Tiled Object-specific field labels
  public static final String ARE_TILED = "Tiled object";
  public static final String ARE_TILED_NAME = "Name";
  public static final String ARE_TILED_ID = "Tile ID";
  public static final String ARE_TILED_FLAGS = "Tile flags";
  public static final String ARE_TILED_FIRST_VERTEX_INDEX_PRI = "First vertex index (primary)";
  public static final String ARE_TILED_FIRST_VERTEX_INDEX_SEC = "First vertex index (secondary)";
  public static final String ARE_TILED_NUM_VERTICES_PRI = "# vertices (primary)";
  public static final String ARE_TILED_NUM_VERTICES_SEC = "# vertices (secondary)";

  public static final String[] s_flag = { "No flags set", "Secondary tile", "Can be looked through" };

  TiledObject() throws Exception
  {
    super(null, ARE_TILED, new byte[108], 0);
  }

  TiledObject(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, ARE_TILED + " " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, ARE_TILED_NAME));
    addField(new TextString(buffer, offset + 32, 8, ARE_TILED_ID));
    addField(new Flag(buffer, offset + 40, 4, ARE_TILED_FLAGS, s_flag));
    addField(new DecNumber(buffer, offset + 44, 4, ARE_TILED_FIRST_VERTEX_INDEX_PRI));
    addField(new DecNumber(buffer, offset + 48, 2, ARE_TILED_NUM_VERTICES_PRI));
    addField(new DecNumber(buffer, offset + 50, 2, ARE_TILED_NUM_VERTICES_SEC));
    addField(new DecNumber(buffer, offset + 52, 4, ARE_TILED_FIRST_VERTEX_INDEX_SEC));
    addField(new Unknown(buffer, offset + 60, 48));
    return offset + 108;
  }
}

