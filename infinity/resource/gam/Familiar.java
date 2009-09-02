// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;

final class Familiar extends AbstractStruct
{
  Familiar(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Familiar info", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Lawful good", "CRE"));
    list.add(new ResourceRef(buffer, offset + 8, "Lawful neutral", "CRE"));
    list.add(new ResourceRef(buffer, offset + 16, "Lawful evil", "CRE"));
    list.add(new ResourceRef(buffer, offset + 24, "Neutral good", "CRE"));
    list.add(new ResourceRef(buffer, offset + 32, "True neutral", "CRE"));
    list.add(new ResourceRef(buffer, offset + 40, "Neutral evil", "CRE"));
    list.add(new ResourceRef(buffer, offset + 48, "Chaotic good", "CRE"));
    list.add(new ResourceRef(buffer, offset + 56, "Chaotic neutral", "CRE"));
    list.add(new ResourceRef(buffer, offset + 64, "Chaotic evil", "CRE"));
    list.add(new DecNumber(buffer, offset + 72, 4, "File size"));
    list.add(new Unknown(buffer, offset + 76, 324));
    return offset + 400;
  }


  void updateFilesize(DecNumber filesize)
  {
    DecNumber fs = (DecNumber)getAttribute("File size");
    fs.setValue(filesize.getValue());
  }
}

