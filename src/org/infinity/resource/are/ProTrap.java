// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.ProRef;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class ProTrap extends AbstractStruct implements AddRemovable
{
  // ARE/Projectile Trap-specific field labels
  public static final String ARE_PROTRAP                      = "Projectile trap";
  public static final String ARE_PROTRAP_TRAP                 = "Trap";
  public static final String ARE_PROTRAP_OFFSET_EFFECTS       = "Effects list offset";
  public static final String ARE_PROTRAP_EFFECTS_SIZE         = "Effects list size";
  public static final String ARE_PROTRAP_PROJECTILE           = "Projectile";
  public static final String ARE_PROTRAP_EXPLOSION_FREQUENCY  = "Explosion frequency (frames)";
  public static final String ARE_PROTRAP_DURATION             = "Duration";
  public static final String ARE_PROTRAP_LOCATION_X           = "Location: X";
  public static final String ARE_PROTRAP_LOCATION_Y           = "Location: Y";
  public static final String ARE_PROTRAP_LOCATION_Z           = "Location: Z";
  public static final String ARE_PROTRAP_TARGET               = "Target";
  public static final String ARE_PROTRAP_PORTRAIT             = "Portrait";

  ProTrap() throws Exception
  {
    super(null, ARE_PROTRAP, StreamUtils.getByteBuffer(28), 0);
  }

  ProTrap(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, ARE_PROTRAP + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, ARE_PROTRAP_TRAP, "PRO"));
    SectionOffset ofsEffects = new SectionOffset(buffer, offset + 8, ARE_PROTRAP_OFFSET_EFFECTS, ProEffect.class);
    addField(ofsEffects);
    // Mac ToB doesn't save these right, so EFFs not handled
    DecNumber sizeEffects = new DecNumber(buffer, offset + 12, 2, ARE_PROTRAP_EFFECTS_SIZE);
    addField(sizeEffects);
    addField(new ProRef(buffer, offset + 14, 2, ARE_PROTRAP_PROJECTILE));
    addField(new DecNumber(buffer, offset + 16, 2, ARE_PROTRAP_EXPLOSION_FREQUENCY));
    addField(new DecNumber(buffer, offset + 18, 2, ARE_PROTRAP_DURATION));
    addField(new DecNumber(buffer, offset + 20, 2, ARE_PROTRAP_LOCATION_X));
    addField(new DecNumber(buffer, offset + 22, 2, ARE_PROTRAP_LOCATION_Y));
    addField(new DecNumber(buffer, offset + 24, 2, ARE_PROTRAP_LOCATION_Z));
    addField(new IdsBitmap(buffer, offset + 26, 1, ARE_PROTRAP_TARGET, "EA.IDS"));
    addField(new DecNumber(buffer, offset + 27, 1, ARE_PROTRAP_PORTRAIT));

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
