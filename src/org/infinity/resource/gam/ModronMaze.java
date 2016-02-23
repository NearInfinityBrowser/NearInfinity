// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;

public final class ModronMaze extends AbstractStruct
{
  // GAM/ModronMaze-specific field labels
  public static final String GAM_MAZE               = "Modron maze state";
  public static final String GAM_MAZE_SIZE_X        = "Size: X";
  public static final String GAM_MAZE_SIZE_Y        = "Size: Y";
  public static final String GAM_MAZE_NORDOM_X      = "Nordom position: X";
  public static final String GAM_MAZE_NORDOM_Y      = "Nordom position: Y";
  public static final String GAM_MAZE_MAIN_HALL_X   = "Main hall position: X";
  public static final String GAM_MAZE_MAIN_HALL_Y   = "Main hall position: Y";
  public static final String GAM_MAZE_FOYER_X       = "Foyer position: X";
  public static final String GAM_MAZE_FOYER_Y       = "Foyer position: Y";
  public static final String GAM_MAZE_ENGINE_ROOM_X = "Engine room position: X";
  public static final String GAM_MAZE_ENGINE_ROOM_Y = "Engine room position: Y";
  public static final String GAM_MAZE_NUM_TRAPS     = "# traps";
  public static final String GAM_MAZE_INITIALIZED   = "Initialized";

  private static final String[] s_noyes = {"No", "Yes"};

  public ModronMaze(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, GAM_MAZE, buffer, offset);
  }

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    int curOfs = offset;

    // adding room data
    for (int i = 0; i < 64; i++) {
      ModronMazeEntry entry = new ModronMazeEntry(this, buffer, curOfs, i);
      addField(entry);
      curOfs += entry.getSize();
    }

    // adding header data
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_SIZE_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_SIZE_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_NORDOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_NORDOM_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_MAIN_HALL_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_MAIN_HALL_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_FOYER_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_FOYER_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_ENGINE_ROOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_ENGINE_ROOM_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_NUM_TRAPS));
    curOfs += 4;
    addField(new Bitmap(buffer, curOfs, 4, GAM_MAZE_INITIALIZED, s_noyes));
    curOfs += 4;
    addField(new Unknown(buffer, curOfs, 8));
    curOfs += 8;

    return curOfs;
  }
}
