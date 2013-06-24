// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.src;

import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Entry extends AbstractStruct implements AddRemovable
{
  Entry() throws Exception
  {
    super(null, "Entry", new byte[8], 0);
  }

  Entry(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Entry", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new StringRef(buffer, offset, "Text"));
    list.add(new Unknown(buffer, offset + 4, 4));
    return offset + 8;
  }
}

