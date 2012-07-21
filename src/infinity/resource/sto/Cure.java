// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Cure extends AbstractStruct implements AddRemovable
{
  Cure() throws Exception
  {
    super(null, "Cure", new byte[12], 0);
  }

  Cure(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Cure", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Spell", "SPL"));
    list.add(new DecNumber(buffer, offset + 8, 4, "Price"));
    return offset + 12;
  }
}

