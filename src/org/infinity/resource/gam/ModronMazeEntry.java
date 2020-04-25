// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;

public final class ModronMazeEntry extends AbstractStruct
{
  // GAM/ModronMazeEntry-specific field labels
  public static final String GAM_MAZE_ENTRY             = "Maze entry";
  public static final String GAM_MAZE_ENTRY_USED        = "Used";
  public static final String GAM_MAZE_ENTRY_ACCESSIBLE  = "Accessible";
  public static final String GAM_MAZE_ENTRY_IS_VALID    = "Is valid";
  public static final String GAM_MAZE_ENTRY_IS_TRAPPED  = "Is trapped";
  public static final String GAM_MAZE_ENTRY_TRAP_TYPE   = "Trap type";
  public static final String GAM_MAZE_ENTRY_EXITS       = "Exits";
  public static final String GAM_MAZE_ENTRY_POLULATED   = "Populated";

  private static final String[] s_traps = {"TrapA", "TrapB", "TrapC"};
  private static final String[] s_walls = {"None", "East", "West", "North", "South"};

  public ModronMazeEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, GAM_MAZE_ENTRY + " " + nr, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new Bitmap(buffer, offset, 4, GAM_MAZE_ENTRY_USED, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 4, 4, GAM_MAZE_ENTRY_ACCESSIBLE, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 8, 4, GAM_MAZE_ENTRY_IS_VALID, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 12, 4, GAM_MAZE_ENTRY_IS_TRAPPED, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 16, 4, GAM_MAZE_ENTRY_TRAP_TYPE, s_traps));
    addField(new Flag(buffer, offset + 20, 2, GAM_MAZE_ENTRY_EXITS, s_walls));
    addField(new Bitmap(buffer, offset + 22, 4, GAM_MAZE_ENTRY_POLULATED, OPTION_NOYES));
    return offset + 26;
  }
}
