// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

public final class ModronMaze extends AbstractStruct
{
  private static final String[] s_noyes = {"No", "Yes"};

  public ModronMaze(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, "Modron maze state", buffer, offset);
  }

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    int curOfs = offset;

    // adding room data
    for (int i = 0; i < 64; i++) {
      ModronMazeEntry entry = new ModronMazeEntry(this, buffer, curOfs, i);
      list.add(entry);
      curOfs += entry.getSize();
    }

    // adding header data
    list.add(new DecNumber(buffer, curOfs, 4, "Size: X"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Size: Y"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Nordom position: X"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Nordom position: Y"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Main hall position: X"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Main hall position: Y"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Foyer position: X"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Foyer position: Y"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Engine room position: X"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "Engine room position: Y"));
    curOfs += 4;
    list.add(new DecNumber(buffer, curOfs, 4, "# traps"));
    curOfs += 4;
    list.add(new Bitmap(buffer, curOfs, 4, "Initialized", s_noyes));
    curOfs += 4;
    list.add(new Unknown(buffer, curOfs, 8));
    curOfs += 8;

    return curOfs;
  }
}
