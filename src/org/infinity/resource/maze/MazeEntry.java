// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.maze;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.gam.ModronMazeEntry;

public class MazeEntry extends AbstractStruct
{
  private static final String[] s_traps = {"TrapA", "TrapB", "TrapC"};
  private static final String[] s_walls = {"None", "East", "West", "North", "South"};

  public MazeEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, ModronMazeEntry.GAM_MAZE_ENTRY + " " + nr, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new Bitmap(buffer, offset, 4, ModronMazeEntry.GAM_MAZE_ENTRY_USED, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 4, 4, ModronMazeEntry.GAM_MAZE_ENTRY_ACCESSIBLE, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 8, 4, ModronMazeEntry.GAM_MAZE_ENTRY_IS_VALID, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 12, 4, ModronMazeEntry.GAM_MAZE_ENTRY_IS_TRAPPED, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 16, 4, ModronMazeEntry.GAM_MAZE_ENTRY_TRAP_TYPE, s_traps));
    addField(new Flag(buffer, offset + 20, 4, ModronMazeEntry.GAM_MAZE_ENTRY_EXITS, s_walls));
    addField(new Bitmap(buffer, offset + 24, 4, ModronMazeEntry.GAM_MAZE_ENTRY_POLULATED, OPTION_NOYES));
    return offset + 28;
  }
}
