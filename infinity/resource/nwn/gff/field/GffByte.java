// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffByte extends GffField
{
  private final byte value;

  public GffByte(byte buffer[], int fieldOffset, int labelOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    value = buffer[fieldOffset + 8];
  }

  public String toString()
  {
    return getLabel() + " = " + value;
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        value != ((GffByte)field).value)
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return new Byte(value);
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 0);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    os.write(new byte[] { value, 0, 0, 0 });
    return fieldDataIndex;
  }
}

