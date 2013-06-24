// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.var;

import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Entry extends AbstractStruct implements AddRemovable
{
  Entry() throws Exception
  {
    super(null, "Variable", new byte[44], 0);
  }

  Entry(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Variable " + nr, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 8, "Type"));
    list.add(new TextString(buffer, offset + 8, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 40, 4, "Value"));
    return offset + 44;
  }
}

