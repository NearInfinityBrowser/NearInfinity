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
// --Recycle Bin START (27.09.03 16:47):
//  public Tilemap() throws Exception
//  {
//    super(null, "Tilemap", new byte[10], 0);
//  }
// --Recycle Bin STOP (27.09.03 16:47)
  private static final String s_flags[] = { "Primary overlay only", "Unused", "Overlay 1",
                                           "Overlay 2", "Overlay 3", "Overlay 4", "Overlay 5",
                                           "Overlay 6", "Overlay 7" };

  public Tilemap(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Tilemap " + number, buffer, offset, 5);
  }

  public int getTileCount()
  {
    return ((DecNumber)getAttribute("Primary tile count")).getValue();
  }

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new DecNumber(buffer, offset, 2, "Primary tile index"));
    list.add(new DecNumber(buffer, offset + 2, 2, "Primary tile count"));
    list.add(new DecNumber(buffer, offset + 4, 2, "Secondary tile index"));
    list.add(new Flag(buffer, offset + 6, 1, "Draw Overlays", s_flags));
    list.add(new Unknown(buffer, offset + 7, 3));
//    list.add(new Unknown(buffer, offset + 7, 3));
    return offset + 10;
  }
}

