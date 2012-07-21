// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff.field;

import infinity.util.Byteconvert;

import java.util.List;
import java.util.Collections;
import java.io.OutputStream;
import java.io.IOException;


public abstract class GffField
{
  private String label;

  GffField()
  {
    label = "";
  }

  GffField(byte buffer[], int fieldOffset, int labelOffset)
  {
    // type is already read
    int labelIndex = Byteconvert.convertInt(buffer, fieldOffset + 4);
    label = Byteconvert.convertString(buffer, labelOffset + labelIndex * 16, 16);
  }

  public void addNestedFields(List<GffStruct> structs, List fields, List<GffList> lists)
  {
    if (this instanceof GffStruct) {
      List fieldList = getChildren();
      fields.addAll(fieldList);
      for (int i = 0; i < fieldList.size(); i++) {
        Object o = fieldList.get(i);
        if (o instanceof GffStruct) {
          structs.add((GffStruct)o);
          ((GffField)o).addNestedFields(structs, fields, lists);
        }
        else if (o instanceof GffList) {
          lists.add((GffList)o);
          ((GffField)o).addNestedFields(structs, fields, lists);
        }
      }
    }
    else if (this instanceof GffList) {
      List fieldList = getChildren();
      structs.addAll(fieldList);
      for (int i = 0; i < fieldList.size(); i++)
        ((GffStruct)fieldList.get(i)).addNestedFields(structs, fields, lists);
    }
  }

  public List getChildren()
  {
    return Collections.EMPTY_LIST;
  }

  public int getFieldDataSize()
  {
    return 0;
  }

  public String getLabel()
  {
    return label;
  }

  public void setLabel(String label)
  {
    this.label = label;
  }

  protected abstract void compare(GffField field);

  public abstract Object getValue();

  public abstract int writeField(OutputStream os, List<String> labels, byte[] fieldData, int fieldDataIndex) throws IOException;
}

