// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.src;

import infinity.datatype.SectionCount;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

public final class SrcResource extends AbstractStruct implements Resource, HasAddRemovable
{
  public SrcResource(ResourceEntry entry) throws Exception
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
    SectionCount entry_count = new SectionCount(buffer, offset, 4, "# entries", Entry.class);
    list.add(entry_count);
    offset += 4;
    for (int i = 0; i < entry_count.getValue(); i++) {
      Entry entry = new Entry(this, buffer, offset);
      list.add(entry);
      offset = entry.getEndOffset();
    }
    return offset;
  }
}

