// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

final class UnknownSection3 extends AbstractStruct
{
  UnknownSection3(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Unknown section", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new Unknown(buffer, offset, 24));
    return offset + 24;
  }
}

