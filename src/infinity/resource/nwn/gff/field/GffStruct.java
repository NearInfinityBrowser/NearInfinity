// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.Byteconvert;
import infinity.util.Filewriter;

import java.util.List;
import java.util.ArrayList;
import java.io.OutputStream;
import java.io.IOException;

public final class GffStruct extends GffField
{
  private final List<GffField> fields = new ArrayList<GffField>();
  private final int id;
  private final int dataOrDataOffset; // Temporary value
  private int fieldCount; // Temporary value

  public GffStruct(byte buffer[], int offset)
  {
    id = Byteconvert.convertInt(buffer, offset);
    dataOrDataOffset = Byteconvert.convertInt(buffer, offset + 4);
    fieldCount = Byteconvert.convertInt(buffer, offset + 8);
  }

  public List getChildren()
  {
    return fields;
  }

  public String toString()
  {
    return new StringBuffer("Struct (Type=").append(id).append(" Label =").append(getLabel()).append(')').toString();
  }

  public void addField(GffField field)
  {
    fields.add(field);
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()))
      throw new IllegalStateException(toString() + " - " + field.toString());
    GffStruct other = (GffStruct)field;
    if (id != other.id)
      throw new IllegalStateException(toString() + " - " + field.toString());
    for (int i = 0; i < fields.size(); i++) {
      GffField f = fields.get(i);
      f.compare(other.getField(f.getLabel()));
    }
  }

  public GffField getField(String label) // ToDo: HashMap?
  {
    for (int i = 0; i < fields.size(); i++) {
      GffField field = fields.get(i);
      if (label.equals(field.getLabel()))
        return field;
    }
    System.err.println(label + " not found.");
    return null;
  }

  public int getFieldIndicesSize()
  {
    if (fields.size() > 1)
      return 4 * fields.size();
    return 0;
  }

  public Object getValue()
  {
    return new Integer(id);
  }

  public void initFields(GffField fieldArray[], byte buffer[], int fieldIndicesOffset)
  {
    if (fieldCount == 1)
      fields.add(fieldArray[dataOrDataOffset]);
    else {
      for (int i = 0; i < fieldCount; i++)
        fields.add(fieldArray[Byteconvert.convertInt(buffer, fieldIndicesOffset + dataOrDataOffset + i * 4)]);
    }
  }

  public void removeField(GffField field)
  {
    fields.remove(field);
  }

  public void writeField(OutputStream os, List labels, List structs) throws IOException
  {
    Filewriter.writeInt(os, 14);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, structs.indexOf(this));
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex)
  {
    throw new IllegalAccessError("Struct not regular field - writeField aborted");
  }

  public int writeStruct(OutputStream os, List fieldList, int[] fieldIndices, int fieldIndicesIndex) throws IOException
  {
    Filewriter.writeInt(os, id);
    fieldCount = fields.size();
    if (fieldCount == 1)
      Filewriter.writeInt(os, fieldList.indexOf(fields.get(0)));
    else if (fieldCount > 1) {
      Filewriter.writeInt(os, fieldIndicesIndex * 4);
      int i = 0;
      int index = fieldList.indexOf(fields.get(0)) - 1;
      while (i < fieldCount) {
        index++;
        if (fieldList.get(index) == fields.get(i))
          fieldIndices[fieldIndicesIndex++] = index;
        else
          fieldIndices[fieldIndicesIndex++] = fieldList.indexOf(fields.get(i));
        index = fieldIndices[fieldIndicesIndex - 1];
        i++;
      }
//  Removed for optimization - indexOf was too slow
//      for (int i = 0; i < fieldCount; i++)
//        fieldIndices[fieldIndicesIndex++] = fieldList.indexOf(fields.get(i));
    }
    else
      Filewriter.writeInt(os, 0);
    Filewriter.writeInt(os, fieldCount);
    return fieldIndicesIndex;
  }
}

