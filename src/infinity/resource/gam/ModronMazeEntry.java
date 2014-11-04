// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.resource.AbstractStruct;

public final class ModronMazeEntry extends AbstractStruct
{
  public static final String FMT_NAME = "Maze entry %1$d";

  private static final String[] s_noyes = {"No", "Yes"};

  public ModronMazeEntry(AbstractStruct superStruct, byte[] buffer, int offset, int nr) throws Exception
  {
    super(superStruct, String.format(FMT_NAME, nr), buffer, offset);
  }

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 4, "Override"));
    addField(new Bitmap(buffer, offset + 4, 4, "Accessible", s_noyes));
    addField(new Bitmap(buffer, offset + 8, 4, "Is valid", s_noyes));
    addField(new Bitmap(buffer, offset + 12, 4, "Is trapped", s_noyes));
    addField(new Bitmap(buffer, offset + 16, 4, "Trap type",
                        new String[]{"TrapA", "TrapB", "TrapC"}));
    addField(new Flag(buffer, offset + 20, 2, "Walls",
                      new String[]{"None", "East", "West", "North", "South"}));
    addField(new Bitmap(buffer, offset + 22, 4, "Visited", s_noyes));
    return offset + 26;
  }
}
