// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

public final class EffResource extends AbstractStruct implements Resource
{
  public EffResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new TextString(buffer, offset + 8, 4, "Signature 2"));
    list.add(new TextString(buffer, offset + 12, 4, "Version 2"));
    EffectType type = new EffectType(buffer, offset + 16, 4);
    list.add(type);
    offset = type.readAttributes(buffer, offset + 20, list);

    Effect2.readCommon(list, buffer, offset);

    return offset + 216;
  }
}

