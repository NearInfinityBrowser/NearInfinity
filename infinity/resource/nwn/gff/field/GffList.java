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

public final class GffList extends GffField
{
  private final List<GffStruct> structs = new ArrayList<GffStruct>();
  private final int structIndices[]; // Temporary value

  public GffList(byte buffer[], int fieldOffset, int labelOffset, int listIndicesOffset)
  {
    super(buffer, fieldOffset, labelOffset);
    int dataOrDataOffset = Byteconvert.convertInt(buffer, fieldOffset + 8);

    int size = Byteconvert.convertInt(buffer, listIndicesOffset + dataOrDataOffset);
    structIndices = new int[size];
    for (int i = 0; i < size; i++)
      structIndices[i] = Byteconvert.convertInt(buffer, listIndicesOffset + dataOrDataOffset + 4 + i * 4);
  }

  public List getChildren()
  {
    return structs;
  }

  public String toString()
  {
    return getLabel();
  }

  public void addStruct(GffStruct struct)
  {
    structs.add(struct);
  }

  public void compare(GffField field)
  {
    if (!getLabel().equals(field.getLabel()))
      throw new IllegalStateException(toString() + " - " + field.toString());
    GffList other = (GffList)field;
    for (int i = 0; i < structs.size(); i++)
      structs.get(i).compare(other.structs.get(i));
  }

  public int getListIndiciesSize()
  {
    return 4 + 4 * structs.size();
  }

  public Object getValue()
  {
    return null;  
  }

  public void initStructs(GffStruct structArray[])
  {
    for (final int structIndex : structIndices)
      structs.add(structArray[structIndex]);
  }

  public void removeStruct(GffStruct struct)
  {
    structs.remove(struct);
  }

  public int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex)
  {
    throw new IllegalAccessError("List not regular field - writeField aborted");
  }

  public int writeField(OutputStream os, List labels, List structList, int listIndices[],
                        int listIndicesIndex) throws IOException
  {
    Filewriter.writeInt(os, 15);
    Filewriter.writeInt(os, labels.indexOf(getLabel()));
    Filewriter.writeInt(os, listIndicesIndex * 4);

    listIndices[listIndicesIndex++] = structs.size();
    for (int i = 0; i < structs.size(); i++)
      listIndices[listIndicesIndex++] = structList.indexOf(structs.get(i));
    return listIndicesIndex;
  }
}

