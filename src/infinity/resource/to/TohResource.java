// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import infinity.datatype.SectionCount;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;

public final class TohResource extends AbstractStruct implements Resource
{
  public TohResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new Unknown(buffer, offset + 8, 4));
    SectionCount count_strref = new SectionCount(buffer, offset + 12, 4, "# strref entries", StrRefEntry.class);
    list.add(count_strref);
    list.add(new Unknown(buffer, offset + 16, 4));

    offset = 20;
    for (int i = 0; i < count_strref.getValue(); i++) {
      StrRefEntry entry = new StrRefEntry(this, buffer, offset, i);
      offset = entry.getEndOffset();
      list.add(entry);
    }

    int endoffset = offset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }
}
