// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.SpawnResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class SpawnPoint extends AbstractStruct implements AddRemovable
{
  private static final String[] s_noyes = { "No", "Yes" };

  SpawnPoint() throws Exception
  {
    super(null, "Spawn point", new byte[200], 0);
  }

  SpawnPoint(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Spawn point " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    for (int i = 0; i < 10; i++) {
      addField(new SpawnResourceRef(buffer, offset + 36 + (i << 3), "Creature " + (i + 1)));
    }
    addField(new DecNumber(buffer, offset + 116, 2, "# creatures"));
    addField(new DecNumber(buffer, offset + 118, 2, "Encounter difficulty"));
    addField(new DecNumber(buffer, offset + 120, 2, "Spawn rate"));
    addField(new Flag(buffer, offset + 122, 2, "Spawn method",
                      new String[]{"No flags set", "Spawn until paused", "Disable after spawn",
                                   "Spawn paused"}));
    addField(new DecNumber(buffer, offset + 124, 4, "Creature duration"));
    addField(new DecNumber(buffer, offset + 128, 2, "Creature wander distance"));
    addField(new DecNumber(buffer, offset + 130, 2, "Creature follow distance"));
    addField(new DecNumber(buffer, offset + 132, 2, "Maximum spawned creatures"));
    addField(new Bitmap(buffer, offset + 134, 2, "Is active?", s_noyes));
    addField(new Flag(buffer, offset + 136, 4, "Active at", Actor.s_schedule));
    addField(new DecNumber(buffer, offset + 140, 2, "Probability (day)"));
    addField(new DecNumber(buffer, offset + 142, 2, "Probability (night)"));
    addField(new Unknown(buffer, offset + 144, 56));
    return offset + 200;
  }
}

