// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.EffectType;

public final class Effect extends AbstractStruct implements AddRemovable
{
  public Effect() throws Exception
  {
    super(null, "Effect", new byte[48], 0);
  }

  public Effect(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Effect " + number, buffer, offset);
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
    list.add(type);
    offset = type.readAttributes(buffer, offset + 2, list);
    return offset;
  }
}

