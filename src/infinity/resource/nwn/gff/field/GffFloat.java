// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffFloat extends GffField
{
  private final float value;

  public GffFloat(byte buffer[], int fieldOffset, int labelOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    value = Float.intBitsToFloat(Byteconvert.convertInt(buffer, fieldOffset + 8));
  }

  public String toString()
  {
    return getLabel() + " = " + value;
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        value != ((GffFloat)field).value)
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return new Float(value);
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 8);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, Float.floatToIntBits(value));
    return fieldDataIndex;
  }
}

