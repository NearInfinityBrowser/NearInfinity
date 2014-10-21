// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class KnownSpells extends AbstractStruct implements AddRemovable
{
  private static final String[] s_spelltype = {"Priest", "Wizard", "Innate"};

  KnownSpells() throws Exception
  {
    super(null, "Known spell", new byte[12], 0);
  }

  KnownSpells(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Known spell " + number, buffer, offset);
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
    list.add(new ResourceRef(buffer, offset, "Spell", "SPL"));
    list.add(new DecNumber(buffer, offset + 8, 2, "Level"));
    list.add(new Bitmap(buffer, offset + 10, 2, "Type", s_spelltype));
    return offset + 12;
  }
}

