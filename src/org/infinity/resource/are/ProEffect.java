// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.TextString;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Effect2;
import org.infinity.resource.StructEntry;

public class ProEffect extends AbstractStruct implements AddRemovable
{
  // ARE/Projectile Effect-specific field labels
  public static final String ARE_PROEFFECT  = "Effect";

  ProEffect(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
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
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    EffectType type = new EffectType(buffer, offset + 8, 4);
    addField(type);
    final List<StructEntry> list = new ArrayList<>();
    offset = type.readAttributes(buffer, offset + 12, list);
    addFields(getFields().size() - 1, list);

    list.clear();
    offset = Effect2.readCommon(list, buffer, offset);
    addFields(getFields().size() - 1, list);

    return offset;
  }
}
