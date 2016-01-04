// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import java.util.ArrayList;
import java.util.List;

import infinity.datatype.EffectType;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Effect2;
import infinity.resource.StructEntry;

public class ProEffect extends AbstractStruct implements AddRemovable
{
  // ARE/Projectile Effect-specific field labels
  public static final String ARE_PROEFFECT  = "Effect";

  ProEffect(AbstractStruct superStruct, byte[] buffer, int offset, int number) throws Exception
  {
    super(superStruct, ARE_PROEFFECT + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return false;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    EffectType type = new EffectType(buffer, offset + 8, 4);
    addField(type);
    List<StructEntry> list = new ArrayList<StructEntry>();
    offset = type.readAttributes(buffer, offset + 12, list);
    addToList(getList().size() - 1, list);

    list.clear();
    offset = Effect2.readCommon(list, buffer, offset);
    addToList(getList().size() - 1, list);

    return offset;
  }
}
