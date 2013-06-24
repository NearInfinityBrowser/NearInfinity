// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class SpawnPoint extends AbstractStruct implements AddRemovable
{
  private static final String[] s_active = { "No", "Yes" };

  SpawnPoint() throws Exception
  {
    super(null, "Spawn point", new byte[200], 0);
  }

  SpawnPoint(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Spawn point", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    for (int i = 0; i < 10; i++)
      list.add(new SpawnResourceRef(buffer, offset + 36 + (i << 3), "Creature " + (i + 1)));
    list.add(new DecNumber(buffer, offset + 116, 2, "# creatures"));
    list.add(new DecNumber(buffer, offset + 118, 2, "Encounter difficulty"));
    list.add(new DecNumber(buffer, offset + 120, 2, "Spawn rate"));
    list.add(new Flag(buffer, offset + 122, 2, "Spawn method",
                      new String[]{"No flags set", "Spawn until paused", "Disable after spawn",
                                   "Spawn paused"}));
    list.add(new DecNumber(buffer, offset + 124, 4, "Creature duration"));
    list.add(new DecNumber(buffer, offset + 128, 2, "Creature wander distance"));
    list.add(new DecNumber(buffer, offset + 130, 2, "Creature follow distance"));
    list.add(new DecNumber(buffer, offset + 132, 2, "Maximum spawned creatures"));
    list.add(new Bitmap(buffer, offset + 134, 2, "Is active?", s_active));
    list.add(new Flag(buffer, offset + 136, 4, "Active at", Actor.s_schedule));
    list.add(new DecNumber(buffer, offset + 140, 2, "Probability (day)"));
    list.add(new DecNumber(buffer, offset + 142, 2, "Probability (night)"));
    list.add(new Unknown(buffer, offset + 144, 56));
    return offset + 200;
  }
}

