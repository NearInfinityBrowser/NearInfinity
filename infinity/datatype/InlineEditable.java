// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;

public interface InlineEditable extends StructEntry
{
  boolean update(Object value);
}

