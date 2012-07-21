// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffDword extends GffField
{
  private long value;

  public GffDword(byte buffer[], int fieldOffset, int labelOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    value = Byteconvert.convertUnsignedInt(buffer, fieldOffset + 8);
  }

  public String toString()
  {
    return getLabel() + " = " + value;
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        value != ((GffDword)field).value)
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return new Long(value);
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 4);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    if (value > 2147483647L)
      value -= 4294967296L;
    Filewriter.writeInt(os, (int)value);
    return fieldDataIndex;
  }
}

