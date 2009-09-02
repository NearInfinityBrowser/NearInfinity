// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffDouble extends GffField
{
  private final double value;

  public GffDouble(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = Byteconvert.convertInt(buffer, fieldOffset + 8);

    long l = Byteconvert.convertLong(buffer, fieldDataOffset + dataOrDataOffset);
    value = Double.longBitsToDouble(l);
  }

  public int getFieldDataSize()
  {
    return 8;
  }

  public String toString()
  {
    return getLabel() + " = " + value;
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        value != ((GffDouble)field).value)
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return new Double(value);
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 9);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);
    byte b[] = Byteconvert.convertBack(Double.doubleToLongBits(value));
    System.arraycopy(b, 0, fieldData, fieldDataIndex, b.length);
    return fieldDataIndex + b.length;
  }
}

