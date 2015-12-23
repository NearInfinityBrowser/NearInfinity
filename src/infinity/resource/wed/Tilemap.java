// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

public final class Tilemap extends AbstractStruct // implements AddRemovable
{
  private static final String[] s_flags = {"Primary overlay only", "Unused", "Overlay 1",
                                           "Overlay 2", "Overlay 3", "Overlay 4", "Overlay 5",
                                           "Overlay 6", "Overlay 7"};

  public Tilemap(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Tilemap " + number, buffer, offset, 5);
  }

  public int getTileCount()
  {
    return ((DecNumber)getAttribute("Primary tile count")).getValue();
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 2, "Primary tile index"));
    addField(new DecNumber(buffer, offset + 2, 2, "Primary tile count"));
    addField(new DecNumber(buffer, offset + 4, 2, "Secondary tile index"));
    addField(new Flag(buffer, offset + 6, 1, "Draw Overlays", s_flags));
    addField(new DecNumber(buffer, offset + 7, 1, "Animation speed"));
    addField(new Unknown(buffer, offset + 8, 2));
    return offset + 10;
  }
}

