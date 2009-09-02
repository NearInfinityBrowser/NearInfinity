// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

public final class GffExoString extends GffField
{
  private final String string;

  public GffExoString(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = Byteconvert.convertInt(buffer, fieldOffset + 8);

    int size = Byteconvert.convertInt(buffer, fieldDataOffset + dataOrDataOffset);
    string = Byteconvert.convertString(buffer, fieldDataOffset + dataOrDataOffset + 4, size);
  }

  public int getFieldDataSize()
  {
    return 4 + string.length();
  }

  public String toString()
  {
    return getLabel() + " = " + string;
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()) ||
        !string.equals(((GffExoString)field).string))
      throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return string;
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 10);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);

    System.arraycopy(Byteconvert.convertBack(string.length()), 0, fieldData, fieldDataIndex, 4);
    System.arraycopy(string.getBytes(), 0, fieldData, fieldDataIndex + 4, string.length());
    return fieldDataIndex + 4 + string.length();
  }
}

