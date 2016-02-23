// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IwdRef;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;

public final class Iwd2Spell extends AbstractStruct implements AddRemovable
{
  // CRE/Iwd2Spell-specific field labels
  public static final String CRE_SPELL                  = "Spell";
  public static final String CRE_SPELL_RESREF           = "ResRef";
  public static final String CRE_SPELL_NUM_MEMORIZABLE  = "# memorizable";
  public static final String CRE_SPELL_NUM_REMAINING    = "# remaining";

  public Iwd2Spell() throws Exception
  {
    super(null, CRE_SPELL, new byte[16], 0);
  }

  public Iwd2Spell(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, CRE_SPELL, buffer, offset);
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
    addField(new IwdRef(buffer, offset, CRE_SPELL_RESREF, "LISTSPLL.2DA"));
    addField(new DecNumber(buffer, offset + 4, 4, CRE_SPELL_NUM_MEMORIZABLE));
    addField(new DecNumber(buffer, offset + 8, 4, CRE_SPELL_NUM_REMAINING));
    addField(new Unknown(buffer, offset + 12, 4));
    return offset + 16;
  }
}

