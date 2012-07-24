// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class KillVariable extends Variable implements AddRemovable
{
  KillVariable() throws Exception
  {
    super(null, "Kill variable", new byte[84], 0);
  }

  KillVariable(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Kill variable", buffer, offset);
  }
}

