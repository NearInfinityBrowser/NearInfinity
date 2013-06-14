// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.tot;

import infinity.resource.*;
import infinity.datatype.Unknown;
import infinity.resource.key.ResourceEntry;

public final class TotResource extends AbstractStruct implements Resource/*, HasAddRemovable*/
{
  public TotResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  // --------------------- Begin Interface HasAddRemovable ---------------------
  /*
  public AddRemovable[] getAddRemovables() throws Exception
  {
  return new AddRemovable[]{new StringEntry()};
  }
   */
  // --------------------- End Interface HasAddRemovable ---------------------

  protected int read(byte[] buffer, int offset) throws Exception
  {
    if (buffer != null && buffer.length > 0) {
      // TODO: fetch number of valid string entries from associated TOH resource
      for (int i = 1; offset + 524 <= buffer.length; i++) {
        StringEntry entry = new StringEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        list.add(entry);
      }
    } else {
      // Placeholder for empty structure
      list.add(new Unknown(buffer, offset, 0, "(Empty)"));
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
