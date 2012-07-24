// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.*;

import java.util.*;
import java.io.OutputStream;
import java.io.IOException;

public final class GffExoLocString extends GffField
{
  private final IntegerHashMap<String> strings = new IntegerHashMap<String>();
  private final int stringRef;

  public GffExoLocString(byte buffer[], int fieldOffset, int labelOffset, int fieldDataOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = Byteconvert.convertInt(buffer, fieldOffset + 8);

    int size = Byteconvert.convertInt(buffer, fieldDataOffset + dataOrDataOffset);
    stringRef = Byteconvert.convertInt(buffer, fieldDataOffset + dataOrDataOffset + 4);
    int stringCount = Byteconvert.convertInt(buffer, fieldDataOffset + dataOrDataOffset + 8);
    int offset = fieldDataOffset + dataOrDataOffset + 12;
    for (int i = 0; i < stringCount; i++) {
      int stringID = Byteconvert.convertInt(buffer, offset);
      int stringLength = Byteconvert.convertInt(buffer, offset + 4);
      String string = new String(buffer, offset + 8, stringLength);
      strings.put(stringID, string);
      offset += 8 + stringLength;
    }
  }

  public List getChildren()
  {
    List<String> children = new ArrayList<String>();
    int keys[] = strings.keys();
    for (final int key : keys)
      children.add(key + " = " + strings.get(key));
    return children;
  }

  public int getFieldDataSize()
  {
    int size = 12;
    int keys[] = strings.keys();
    for (final int key : keys)
      size += 8 + strings.get(key).length();
    return size;
  }

  public String toString()
  {
    return getLabel() + " = " + StringResource.getStringRef(stringRef);
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()))
      throw new IllegalStateException(toString() + " - " + field.toString());
    GffExoLocString other = (GffExoLocString)field;
    if (stringRef != other.stringRef)
      throw new IllegalStateException(toString() + " - " + field.toString());
    int keys[] = strings.keys();
    for (final int key : keys)
      if (!strings.get(key).equals(other.strings.get(key)))
        throw new IllegalStateException(toString() + " - " + field.toString());
  }

  public Object getValue()
  {
    return StringResource.getStringRef(stringRef);
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException
  {
    Filewriter.writeInt(os, 12);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, fieldDataIndex);

    // Size doesn't include the first 4 bytes 
    System.arraycopy(Byteconvert.convertBack(getFieldDataSize() - 4), 0, fieldData, fieldDataIndex, 4);
    System.arraycopy(Byteconvert.convertBack(stringRef), 0, fieldData, fieldDataIndex + 4, 4);
    System.arraycopy(Byteconvert.convertBack(strings.keys().length), 0, fieldData, fieldDataIndex + 8, 4);
    fieldDataIndex += 12;
    int keys[] = strings.keys();
    for (final int key : keys) {
      String string = strings.get(key);
      System.arraycopy(Byteconvert.convertBack(key), 0, fieldData, fieldDataIndex, 4);
      System.arraycopy(Byteconvert.convertBack(string.length()), 0, fieldData, fieldDataIndex + 4, 4);
      System.arraycopy(string.getBytes(), 0, fieldData, fieldDataIndex + 8, string.length());
      fieldDataIndex += 8 + string.length();
    }
    return fieldDataIndex;
  }
}

