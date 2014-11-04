// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

final class Familiar extends AbstractStruct
{
  Familiar(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Familiar info", buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, "Lawful good", "CRE"));
    addField(new ResourceRef(buffer, offset + 8, "Lawful neutral", "CRE"));
    addField(new ResourceRef(buffer, offset + 16, "Lawful evil", "CRE"));
    addField(new ResourceRef(buffer, offset + 24, "Neutral good", "CRE"));
    addField(new ResourceRef(buffer, offset + 32, "True neutral", "CRE"));
    addField(new ResourceRef(buffer, offset + 40, "Neutral evil", "CRE"));
    addField(new ResourceRef(buffer, offset + 48, "Chaotic good", "CRE"));
    addField(new ResourceRef(buffer, offset + 56, "Chaotic neutral", "CRE"));
    addField(new ResourceRef(buffer, offset + 64, "Chaotic evil", "CRE"));
    HexNumber offEOS = new HexNumber(buffer, offset + 72, 4, "End of structure offset");
    addField(offEOS);
    offset += 76;
    int unknownOfs = offEOS.getValue();
    if (unknownOfs < offset) {
      // size of unknown block appears to be always 324 bytes in valid GAM files
      unknownOfs = Math.min(offset + 324, buffer.length);
    }
    int unknownSize = unknownOfs > buffer.length ? buffer.length - offset : unknownOfs - offset;
    addField(new Unknown(buffer, offset, unknownSize));
    offset += unknownSize;
    return offset;
  }


  void updateFilesize(DecNumber filesize)
  {
    DecNumber fs = (DecNumber)getAttribute("File size");
    fs.setValue(filesize.getValue());
  }
}

