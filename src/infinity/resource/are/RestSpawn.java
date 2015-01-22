// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.SpawnResourceRef;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

public final class RestSpawn extends AbstractStruct // implements AddRemovable
{
  RestSpawn(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Rest encounters", buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, "Name"));
    for (int i = 0; i < 10; i++) {
      addField(new StringRef(buffer, offset + 32 + i * 4, "Creature " + (i + 1) + " string"));
    }
    for (int i = 0; i < 10; i++) {
      addField(new SpawnResourceRef(buffer, offset + 72 + i * 8, "Creature " + (i + 1)));
    }
    addField(new DecNumber(buffer, offset + 152, 2, "# creatures"));
    addField(new DecNumber(buffer, offset + 154, 2, "Encounter difficulty"));
    addField(new DecNumber(buffer, offset + 156, 4, "Creature duration"));
    addField(new DecNumber(buffer, offset + 160, 2, "Creature wander distance"));
    addField(new DecNumber(buffer, offset + 162, 2, "Creature follow distance"));
    addField(new DecNumber(buffer, offset + 164, 2, "Maximum spawned creatures"));
    addField(new Bitmap(buffer, offset + 166, 2, "Is active?", new String[]{"No", "Yes"}));
    addField(new DecNumber(buffer, offset + 168, 2, "Probability (day)"));
    addField(new DecNumber(buffer, offset + 170, 2, "Probability (night)"));
    addField(new Unknown(buffer, offset + 172, 56));
    return offset + 228;
  }
}

