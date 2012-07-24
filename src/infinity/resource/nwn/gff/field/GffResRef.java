// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffResRef extends GffField
{
  private final String resRef;

  public GffResRef(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = Byteconvert.convertInt(buffer, fieldOffset + 8);

    int size = (int)buffer[fieldDataOffset + dataOrDataOffset];
    resRef = Byteconvert.convertString(buffer, fieldDataOffset + dataOrDataOffset + 1, size);
  }

  public int getFieldDataSize()
  {
    return 1 + resRef.length();
  }

  public String toString()
  {
    return getLabel() + " = " + resRef;
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        !resRef.equals(((GffResRef)field).resRef))
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return resRef;
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 11);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);

    fieldData[fieldDataIndex++] = (byte)resRef.length();
    System.arraycopy(resRef.getBytes(), 0, fieldData, fieldDataIndex, resRef.length());
    return fieldDataIndex + resRef.length();
  }
}

