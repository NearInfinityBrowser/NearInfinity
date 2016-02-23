// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;

public final class KnownSpells extends AbstractStruct implements AddRemovable
{
  // CRE/KnownSpells-specific field labels
  public static final String CRE_KNOWN        = "Known spell";
  public static final String CRE_KNOWN_RESREF = "Spell";
  public static final String CRE_KNOWN_LEVEL  = "Level";
  public static final String CRE_KNOWN_TYPE   = "Type";

  private static final String[] s_spelltype = {"Priest", "Wizard", "Innate"};

  KnownSpells() throws Exception
  {
    super(null, CRE_KNOWN, new byte[12], 0);
  }

  KnownSpells(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, CRE_KNOWN + " " + number, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, CRE_KNOWN_RESREF, "SPL"));
    addField(new DecNumber(buffer, offset + 8, 2, CRE_KNOWN_LEVEL));
    addField(new Bitmap(buffer, offset + 10, 2, CRE_KNOWN_TYPE, s_spelltype));
    return offset + 12;
  }
}

