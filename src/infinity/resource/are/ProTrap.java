// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class ProTrap extends AbstractStruct implements AddRemovable
{
  ProTrap() throws Exception
  {
    super(null, "Projectile trap", new byte[28], 0);
  }

  ProTrap(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Projectile trap", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Trap", "PRO"));
    list.add(new SectionOffset(buffer, offset + 8, "Effects list offset", null));
    // Mac ToB doesn't save these right, so EFFs not handled
    list.add(new DecNumber(buffer, offset + 12, 2, "Effects list size"));
    list.add(new DecNumber(buffer, offset + 14, 2, "Projectile"));
    list.add(new DecNumber(buffer, offset + 16, 2, "Explosion frequency (frames)"));
    list.add(new DecNumber(buffer, offset + 18, 2, "Duration"));
    list.add(new DecNumber(buffer, offset + 20, 2, "Location: X"));
    list.add(new DecNumber(buffer, offset + 22, 2, "Location: Y"));
    list.add(new DecNumber(buffer, offset + 24, 2, "Location: Z"));
    list.add(new DecNumber(buffer, offset + 26, 1, "Target"));
    list.add(new DecNumber(buffer, offset + 27, 1, "Portrait"));
    return offset + 28;
  }
}

