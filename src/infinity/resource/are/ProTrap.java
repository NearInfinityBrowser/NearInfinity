// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.DecNumber;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionOffset;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class ProTrap extends AbstractStruct implements AddRemovable
{
  ProTrap() throws Exception
  {
    super(null, "Projectile trap", new byte[28], 0);
  }

  ProTrap(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Projectile trap " + number, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, "Trap", "PRO"));
    SectionOffset ofsEffects = new SectionOffset(buffer, offset + 8, "Effects list offset", null);
    addField(ofsEffects);
    // Mac ToB doesn't save these right, so EFFs not handled
    DecNumber sizeEffects = new DecNumber(buffer, offset + 12, 2, "Effects list size");
    addField(sizeEffects);
    addField(new DecNumber(buffer, offset + 14, 2, "Projectile"));
    addField(new DecNumber(buffer, offset + 16, 2, "Explosion frequency (frames)"));
    addField(new DecNumber(buffer, offset + 18, 2, "Duration"));
    addField(new DecNumber(buffer, offset + 20, 2, "Location: X"));
    addField(new DecNumber(buffer, offset + 22, 2, "Location: Y"));
    addField(new DecNumber(buffer, offset + 24, 2, "Location: Z"));
    addField(new IdsBitmap(buffer, offset + 26, 1, "Target", "EA.IDS"));
    addField(new DecNumber(buffer, offset + 27, 1, "Portrait"));

    if (ofsEffects.getValue() > 0 && sizeEffects.getValue() > 0) {
      int curOffset = ofsEffects.getValue();
      int endOffset = curOffset + sizeEffects.getValue();
      int number = 0;
      while (curOffset < endOffset) {
        ProEffect pe = new ProEffect(this, buffer, curOffset, number++);
        curOffset = pe.getEndOffset();
        addField(pe);
      }
    }

    return offset + 28;
  }
}

