// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;

final class RestSpawn extends AbstractStruct // implements AddRemovable
{
  RestSpawn(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Rest encounters", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    for (int i = 0; i < 10; i++)
      list.add(new StringRef(buffer, offset + 32 + i * 4, "Creature " + (i + 1) + " string"));
    for (int i = 0; i < 10; i++)
      list.add(new SpawnResourceRef(buffer, offset + 72 + i * 8, "Creature " + (i + 1)));
    list.add(new DecNumber(buffer, offset + 152, 2, "# creatures"));
    list.add(new DecNumber(buffer, offset + 154, 2, "Encounter difficulty"));
    list.add(new DecNumber(buffer, offset + 156, 4, "Creature duration"));
    list.add(new DecNumber(buffer, offset + 160, 2, "Creature wander distance"));
    list.add(new DecNumber(buffer, offset + 162, 2, "Creature follow distance"));
    list.add(new DecNumber(buffer, offset + 164, 2, "Maximum spawned creatures"));
    list.add(new Bitmap(buffer, offset + 166, 2, "Is active?", new String[]{"No", "Yes"}));
    list.add(new DecNumber(buffer, offset + 168, 2, "Probability (day)"));
    list.add(new DecNumber(buffer, offset + 170, 2, "Probability (night)"));
    list.add(new Unknown(buffer, offset + 172, 56));
    return offset + 228;
  }
}

