// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.AddRemovable;
import infinity.resource.StructEntry;

public final class RemovableDecNumber extends DecNumber implements AddRemovable
{
  public RemovableDecNumber(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public RemovableDecNumber(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------
}

