// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class MemorizedSpells extends AbstractStruct implements AddRemovable
{
  private static final String[] s_mem = {"Spell already cast", "Spell memorized", "Spell disabled"};

  MemorizedSpells() throws Exception
  {
    super(null, "Memorized spell", new byte[12], 0);
  }

  MemorizedSpells(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Memorized spell", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Spell", "SPL"));
    list.add(new Bitmap(buffer, offset + 8, 2, "Memorization", s_mem));
    list.add(new Unknown(buffer, offset + 10, 2));
    return offset + 12;
  }
}

