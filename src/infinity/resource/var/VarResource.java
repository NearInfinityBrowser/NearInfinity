// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.var;

import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class VarResource extends AbstractStruct implements Resource, HasAddRemovable
{
  public VarResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Entry()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    int count = buffer.length / 44;
    for (int i = 0; i < count; i++)
      list.add(new Entry(this, buffer, offset + i * 44, i));
    return offset + count * 44;
  }
}

