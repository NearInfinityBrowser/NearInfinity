// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.DynamicArray;
import infinity.util.Filewriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class GffExoString extends GffField
{
  private final String string;

  public GffExoString(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = DynamicArray.getInt(buffer, fieldOffset + 8);

    int size = DynamicArray.getInt(buffer, fieldDataOffset + dataOrDataOffset);
    string = DynamicArray.getString(buffer, fieldDataOffset + dataOrDataOffset + 4, size);
  }

  @Override
  public int getFieldDataSize()
  {
    return 4 + string.length();
  }

  @Override
  public String toString()
  {
    return getLabel() + " = " + string;
  }

  @Override
  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        !string.equals(((GffExoString)field).string))
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  @Override
  public Object getValue()
  {
    return string;
  }

  @Override
  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 10);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);

    System.arraycopy(DynamicArray.convertInt(string.length()), 0, fieldData, fieldDataIndex, 4);
    System.arraycopy(string.getBytes(), 0, fieldData, fieldDataIndex + 4, string.length());
    return fieldDataIndex + 4 + string.length();
  }
}

