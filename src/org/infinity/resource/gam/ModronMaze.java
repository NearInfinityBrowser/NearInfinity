// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;

public final class ModronMaze extends AbstractStruct
{
  // GAM/ModronMaze-specific field labels
  public static final String GAM_MAZE                     = "Modron maze state";
  public static final String GAM_MAZE_SIZE_X              = "Size: X";
  public static final String GAM_MAZE_SIZE_Y              = "Size: Y";
  public static final String GAM_MAZE_NORDOM_X            = "Nordom position: X";
  public static final String GAM_MAZE_NORDOM_Y            = "Nordom position: Y";
  public static final String GAM_MAZE_WIZARD_ROOM_X       = "Wizard room position: X";
  public static final String GAM_MAZE_WIZARD_ROOM_Y       = "Wizard room position: Y";
  public static final String GAM_MAZE_FOYER_X             = "Foyer position: X";
  public static final String GAM_MAZE_FOYER_Y             = "Foyer position: Y";
  public static final String GAM_MAZE_ENGINE_ROOM_X       = "Engine room position: X";
  public static final String GAM_MAZE_ENGINE_ROOM_Y       = "Engine room position: Y";
  public static final String GAM_MAZE_NUM_TRAPS           = "# traps";
  public static final String GAM_MAZE_INITIALIZED         = "Initialized";
  public static final String GAM_MAZE_MAZE_BLOCKER_MADE   = "Foyer maze blocker made";
  public static final String GAM_MAZE_ENGINE_BLOCKER_MADE = "Foyer engine blocker made";

  public ModronMaze(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception
  {
    super(superStruct, GAM_MAZE, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
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
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_WIZARD_ROOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_WIZARD_ROOM_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_NORDOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, GAM_MAZE_NORDOM_Y));
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
    addField(new Bitmap(buffer, curOfs, 4, GAM_MAZE_INITIALIZED, OPTION_NOYES));
    curOfs += 4;
    addField(new Bitmap(buffer, curOfs, 4, GAM_MAZE_MAZE_BLOCKER_MADE, OPTION_NOYES));
    curOfs += 4;
    addField(new Bitmap(buffer, curOfs, 4, GAM_MAZE_ENGINE_BLOCKER_MADE, OPTION_NOYES));
    curOfs += 4;

    return curOfs;
  }
}
