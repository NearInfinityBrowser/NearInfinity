// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffVoid extends GffField
{
  private final byte data[];

  public GffVoid(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = Byteconvert.convertInt(buffer, fieldOffset + 8);

    int size = Byteconvert.convertInt(buffer, fieldDataOffset + dataOrDataOffset);
    data = ArrayUtil.getSubArray(buffer, fieldDataOffset + dataOrDataOffset + 4, size);
  }

  public int getFieldDataSize()
  {
    return 4 + data.length;
  }

  public String toString()
  {
    return getLabel() + " (Len=" + data.length + ')';
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()))
      throw new IllegalStateException(toString() + " - " + field.toString());
    GffVoid other = (GffVoid)field;
    for (int i = 0; i < data.length; i++)
      if (data[i] != other.data[i])
        throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return data;
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex)
          throws IOException
  {
    Filewriter.writeInt(os, 13);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);

    System.arraycopy(Byteconvert.convertBack(data.length), 0, fieldData, fieldDataIndex, 4);
    System.arraycopy(data, 0, fieldData, fieldDataIndex + 4, data.length);
    return fieldDataIndex + 4 + data.length;
  }
}

