// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.Bitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.Unknown;
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

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, "Spell", "SPL"));
    addField(new Bitmap(buffer, offset + 8, 2, "Memorization", s_mem));
    addField(new Unknown(buffer, offset + 10, 2));
    return offset + 12;
  }
}

