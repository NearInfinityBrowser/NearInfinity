// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.var;

import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

public final class VarResource extends AbstractStruct implements Resource, HasAddRemovable
{
  public VarResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Entry()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    int count = buffer.length / 44;
    for (int i = 0; i < count; i++)
      list.add(new Entry(this, buffer, offset + i * 44, i));
    return offset + count * 44;
  }
}

