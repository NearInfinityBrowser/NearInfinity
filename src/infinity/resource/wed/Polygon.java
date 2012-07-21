// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.vertex.Vertex;

abstract class Polygon extends AbstractStruct implements AddRemovable, HasAddRemovable
{
  Polygon(AbstractStruct superStruct, String name, byte buffer[], int offset) throws Exception
  {
    super(superStruct, name, buffer, offset, 8);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Vertex()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof Vertex) {
      int index = ((DecNumber)getAttribute("Vertex index")).getValue();
      index += ((DecNumber)getAttribute("# vertices")).getValue();
      AbstractStruct superStruct = getSuperStruct();
      while (superStruct.getSuperStruct() != null)
        superStruct = superStruct.getSuperStruct();
      int offset = ((HexNumber)superStruct.getAttribute("Vertices offset")).getValue();
      datatype.setOffset(offset + 4 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  public void readVertices(byte buffer[], int offset) throws Exception
  {
    DecNumber firstVertex = (DecNumber)getAttribute("Vertex index");
    DecNumber numVertices = (DecNumber)getAttribute("# vertices");
    for (int i = 0; i < numVertices.getValue(); i++)
      list.add(new Vertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
  }

  public int updateVertices(int offset, int startIndex)
  {
    ((DecNumber)getAttribute("Vertex index")).setValue(startIndex);
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry instanceof Vertex) {
        entry.setOffset(offset);
        ((AbstractStruct)entry).realignStructOffsets();
        offset += 4;
        count++;
      }
    }
    ((DecNumber)getAttribute("# vertices")).setValue(count);
    return count;
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new DecNumber(buffer, offset, 4, "Vertex index"));
    list.add(new SectionCount(buffer, offset + 4, 4, "# vertices", Vertex.class));
    list.add(new Unknown(buffer, offset + 8, 2));
//    list.add(new Unknown(buffer, offset + 9, 1));
    list.add(new DecNumber(buffer, offset + 10, 2, "Minimum coordinate: X"));
    list.add(new DecNumber(buffer, offset + 12, 2, "Maximum coordinate: X"));
    list.add(new DecNumber(buffer, offset + 14, 2, "Minimum coordinate: Y"));
    list.add(new DecNumber(buffer, offset + 16, 2, "Maximum coordinate: Y"));
    return offset + 18;
  }
}

