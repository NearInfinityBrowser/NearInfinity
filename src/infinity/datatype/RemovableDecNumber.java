// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.AddRemovable;

public final class RemovableDecNumber extends DecNumber implements AddRemovable
{
  public RemovableDecNumber(byte buffer[], int offset, int length, String name)
  {
    super(buffer, offset, length, name);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------
}

