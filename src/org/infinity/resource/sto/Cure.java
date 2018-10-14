// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasIcon;
import org.infinity.util.io.StreamUtils;

/** Represents healing service in the store. */
public final class Cure extends AbstractStruct implements AddRemovable, HasIcon
{
  // STO/Cure-specific field labels
  public static final String STO_CURE       = "Cure";
  /** Name of the field with ResRef to the spell for apply when purchased. */
  public static final String STO_CURE_SPELL = "Spell";
  public static final String STO_CURE_PRICE = "Price";

  Cure() throws Exception
  {
    super(null, STO_CURE, StreamUtils.getByteBuffer(12), 0);
  }

  Cure(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, STO_CURE + " " + number, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, STO_CURE_SPELL, "SPL"));
    addField(new DecNumber(buffer, offset + 8, 4, STO_CURE_PRICE));
    return offset + 12;
  }

  @Override
  public ResourceRef getIcon() { return getIndirectIcon(STO_CURE_SPELL); }
}
