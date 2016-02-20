// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import java.util.ArrayList;
import java.util.List;

import infinity.datatype.EffectType;

public final class Effect extends AbstractStruct implements AddRemovable
{
  // Effect-specific field labels
  public static final String EFFECT = "Effect";

  public Effect() throws Exception
  {
    super(null, EFFECT, new byte[48], 0);
  }

  public Effect(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, EFFECT + " " + number, buffer, offset);
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
    EffectType type = new EffectType(buffer, offset, 2);
    addField(type);
    List<StructEntry> list = new ArrayList<StructEntry>();
    offset = type.readAttributes(buffer, offset + 2, list);
    addToList(getList().size() - 1, list);
    return offset;
  }
}

