// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.resource.AbstractStruct;

public final class Familiar extends AbstractStruct
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
    HexNumber offEOS = new HexNumber(buffer, offset + 72, 4, "Familiar resources offset");
    addField(offEOS);
    offset += 76;
    // To be confirmed: I've never seen these fields in use
    final String[] alignLabels = { "LG", "LN", "CG", "NG", "TN", "NE", "LE", "CN", "CE" };
    int numFamiliarExtra = 0;
    for (final String align: alignLabels) {
      for (int i = 1; i < 10; i++) {
        DecNumber familiarCount = new DecNumber(buffer, offset, 4,
                                                String.format("%1$s level %2$d familiar count", align, i));
        numFamiliarExtra += familiarCount.getValue();
        addField(familiarCount);
        offset += 4;
      }
    }
    if (numFamiliarExtra > 0) {
      int curOffset = offEOS.getValue();
      for (int i = 0; i < numFamiliarExtra; i++) {
        addField(new ResourceRef(buffer, curOffset, "Familiar resource " + i, "CRE"));
        curOffset += 8;
      }
    }
    return offset;
  }


  void updateFilesize(DecNumber filesize)
  {
    DecNumber fs = (DecNumber)getAttribute("File size");
    fs.setValue(filesize.getValue());
  }
}

