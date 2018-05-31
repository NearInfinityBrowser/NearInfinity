// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;

import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;

public final class TotResource extends AbstractStruct implements Resource
{
  // TOT-specific field labels
  public static final String TOT_EMPTY = "(empty)";

  public TotResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    if (buffer != null && buffer.limit() > 0) {
      // TODO: fetch number of valid string entries from associated TOH resource
      for (int i = 0; offset + 524 <= buffer.limit(); i++) {
        StringEntry entry = new StringEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      }
    } else {
      addField(new Unknown(buffer, offset, 0, TOT_EMPTY));  // Placeholder for empty structure
    }

    int endoffset = offset;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }

    return endoffset;
  }
}
