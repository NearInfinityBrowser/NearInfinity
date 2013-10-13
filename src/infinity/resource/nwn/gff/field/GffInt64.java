// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.DynamicArray;
import infinity.util.Filewriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class GffInt64 extends GffField
{
  private final long value;

  public GffInt64(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = DynamicArray.getInt(buffer, fieldOffset + 8);

    value = DynamicArray.getLong(buffer, fieldDataOffset + dataOrDataOffset);
  }

  @Override
  public int getFieldDataSize()
  {
    return 8;
  }

  @Override
  public String toString()
  {
    return getLabel() + " = " + value;
  }

  @Override
  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        value != ((GffInt64)field).value)
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  @Override
  public Object getValue()
  {
    return new Long(value);
  }

  @Override
  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 7);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);
    byte b[] = DynamicArray.convertLong(value);
    System.arraycopy(b, 0, fieldData, fieldDataIndex, b.length);
    return fieldDataIndex + b.length;
  }
}

