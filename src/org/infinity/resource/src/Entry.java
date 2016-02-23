// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.src;

import org.infinity.datatype.StringRef;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;

final class Entry extends AbstractStruct implements AddRemovable
{
  // SRC/Entry-specific field labels
  public static final String SRC_ENTRY      = "Entry";
  public static final String SRC_ENTRY_TEXT = "Text";

  Entry() throws Exception
  {
    super(null, SRC_ENTRY, new byte[8], 0);
  }

  Entry(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, SRC_ENTRY + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, SRC_ENTRY_TEXT));
    addField(new Unknown(buffer, offset + 4, 4));
    return offset + 8;
  }
}

