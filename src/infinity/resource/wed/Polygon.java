// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.StructEntry;
import infinity.resource.vertex.Vertex;

public abstract class Polygon extends AbstractStruct implements AddRemovable, HasAddRemovable
{
  // WED/Polygon-specific field labels
  public static final String WED_POLY_VERTEX_INDEX  = "Vertex index";
  public static final String WED_POLY_NUM_VERTICES  = "# vertices";
  public static final String WED_POLY_FLAGS         = "Polygon flags";
  public static final String WED_POLY_MIN_COORD_X   = "Minimum coordinate: X";
  public static final String WED_POLY_MAX_COORD_X   = "Maximum coordinate: X";
  public static final String WED_POLY_MIN_COORD_Y   = "Minimum coordinate: Y";
  public static final String WED_POLY_MAX_COORD_Y   = "Maximum coordinate: Y";

  public static final String[] s_flags = { "No flags set", "Shade wall", "Semi transparent",
                                            "Hovering wall", "Cover animations", "Unknown",
                                            "Unknown", "Unknown", "Is door" };

  public Polygon(AbstractStruct superStruct, String name, byte buffer[], int offset) throws Exception
  {
    super(superStruct, name, buffer, offset, 8);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Vertex()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof Vertex) {
      int index = ((DecNumber)getAttribute(WED_POLY_VERTEX_INDEX)).getValue();
      index += ((DecNumber)getAttribute(WED_POLY_NUM_VERTICES)).getValue();
      AbstractStruct superStruct = getSuperStruct();
      while (superStruct.getSuperStruct() != null)
        superStruct = superStruct.getSuperStruct();
      int offset = ((HexNumber)superStruct.getAttribute(WedResource.WED_OFFSET_VERTICES)).getValue();
      datatype.setOffset(offset + 4 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  public void readVertices(byte buffer[], int offset) throws Exception
  {
    DecNumber firstVertex = (DecNumber)getAttribute(WED_POLY_VERTEX_INDEX);
    DecNumber numVertices = (DecNumber)getAttribute(WED_POLY_NUM_VERTICES);
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new Vertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }
  }

  public int updateVertices(int offset, int startIndex)
  {
    ((DecNumber)getAttribute(WED_POLY_VERTEX_INDEX)).setValue(startIndex);
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry instanceof Vertex) {
        entry.setOffset(offset);
        ((AbstractStruct)entry).realignStructOffsets();
        offset += 4;
        count++;
      }
    }
    ((DecNumber)getAttribute(WED_POLY_NUM_VERTICES)).setValue(count);
    return count;
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 4, WED_POLY_VERTEX_INDEX));
    addField(new SectionCount(buffer, offset + 4, 4, WED_POLY_NUM_VERTICES, Vertex.class));
    addField(new Flag(buffer, offset + 8, 1, WED_POLY_FLAGS, s_flags));
    addField(new Unknown(buffer, offset + 9, 1));
    addField(new DecNumber(buffer, offset + 10, 2, WED_POLY_MIN_COORD_X));
    addField(new DecNumber(buffer, offset + 12, 2, WED_POLY_MAX_COORD_X));
    addField(new DecNumber(buffer, offset + 14, 2, WED_POLY_MIN_COORD_Y));
    addField(new DecNumber(buffer, offset + 16, 2, WED_POLY_MAX_COORD_Y));
    return offset + 18;
  }
}

