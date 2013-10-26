// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.Filewriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Deprecated
public final class GffChar extends GffField
{
  private final byte value; // ToDo: Char?

  public GffChar(byte buffer[], int fieldOffset, int labelOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    value = buffer[fieldOffset + 8];
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
        value != ((GffChar)field).value)
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  @Override
  public Object getValue()
  {
    return new Byte(value);
  }

  @Override
  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 1);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    os.write(new byte[] { value, 0, 0, 0});
    return fieldDataIndex;
  }
}

