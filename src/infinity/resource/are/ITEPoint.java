// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.vertex.Vertex;

public final class ITEPoint extends AbstractStruct implements AddRemovable, HasVertices, HasAddRemovable
{
  private static final String[] s_type = {"Proximity trigger", "Info trigger", "Travel trigger"};
  private static final String[] s_flag = {"No flags set", "Locked", "Trap resets", "Party required", "Trap detectable",
                                          "Trap set off by enemy", "Tutorial trigger", "Trap set off by NPC", "Trigger silent",
                                          "Trigger deactivated", "Cannot be passed by NPC", "Use activation point",
                                          "Connected to door"};
  private static final String[] s_yesno = {"No", "Yes"};

  public ITEPoint() throws Exception
  {
    super(null, "Trigger", new byte[196], 0);
  }

  public ITEPoint(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Trigger", buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Vertex()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasVertices ---------------------

  public void readVertices(byte buffer[], int offset) throws Exception
  {
    int firstVertex = ((DecNumber)getAttribute("First vertex index")).getValue();
    int numVertices = ((DecNumber)getAttribute("# vertices")).getValue();
    offset += firstVertex << 2;
    for (int i = 0; i < numVertices; i++)
      list.add(new Vertex(this, buffer, offset + 4 * i, i));
  }

  public int updateVertices(int offset, int number)
  {
    ((DecNumber)getAttribute("First vertex index")).setValue(number);
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry instanceof Vertex) {
        entry.setOffset(offset);
        ((Vertex)entry).realignStructOffsets();
        offset += 4;
        count++;
      }
    }
    ((DecNumber)getAttribute("# vertices")).setValue(count);
    return count;
  }

// --------------------- End Interface HasVertices ---------------------

  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof Vertex) {
      int index = ((DecNumber)getAttribute("First vertex index")).getValue();
      index += ((DecNumber)getAttribute("# vertices")).getValue();
      int offset = ((HexNumber)getSuperStruct().getAttribute("Vertices offset")).getValue();
      datatype.setOffset(offset + 4 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new Bitmap(buffer, offset + 32, 2, "Type", s_type));
    list.add(new DecNumber(buffer, offset + 34, 2, "Bounding box: Left"));
    list.add(new DecNumber(buffer, offset + 36, 2, "Bounding box: Top"));
    list.add(new DecNumber(buffer, offset + 38, 2, "Bounding box: Right"));
    list.add(new DecNumber(buffer, offset + 40, 2, "Bounding box: Bottom"));
    list.add(new DecNumber(buffer, offset + 42, 2, "# vertices"));
    list.add(new DecNumber(buffer, offset + 44, 4, "First vertex index"));
    list.add(new Unknown(buffer, offset + 48, 4));
    list.add(new DecNumber(buffer, offset + 52, 4, "Cursor number"));
    list.add(new ResourceRef(buffer, offset + 56, "Destination area", "ARE"));
    list.add(new TextString(buffer, offset + 64, 32, "Entrance name"));
    list.add(new Flag(buffer, offset + 96, 4, "Flags", s_flag));
    list.add(new StringRef(buffer, offset + 100, "Info point text"));
    list.add(new DecNumber(buffer, offset + 104, 2, "Trap detection difficulty"));
    list.add(new DecNumber(buffer, offset + 106, 2, "Trap removal difficulty"));
    list.add(new Bitmap(buffer, offset + 108, 2, "Is trapped?", s_yesno));
    list.add(new Bitmap(buffer, offset + 110, 2, "Is trap detected?", s_yesno));
    list.add(new DecNumber(buffer, offset + 112, 2, "Launch point: X"));
    list.add(new DecNumber(buffer, offset + 114, 2, "Launch point: Y"));
    list.add(new ResourceRef(buffer, offset + 116, "Key", "ITM")); // Key type?
    list.add(new ResourceRef(buffer, offset + 124, "Script", "BCS"));
//    list.add(new DecNumber(buffer, offset + 132, 2, "Override box: Left"));
//    list.add(new DecNumber(buffer, offset + 134, 2, "Override box: Top"));
//    list.add(new DecNumber(buffer, offset + 136, 2, "Override box: Right"));
//    list.add(new DecNumber(buffer, offset + 138, 2, "Override box: Bottom"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
      list.add(new Unknown(buffer, offset + 132, 48));
      list.add(new DecNumber(buffer, offset + 180, 2, "Speaker point: X"));
      list.add(new DecNumber(buffer, offset + 182, 2, "Speaker point: Y"));
      list.add(new StringRef(buffer, offset + 184, "Speaker name"));
      list.add(new ResourceRef(buffer, offset + 188, "Dialogue", "DLG"));
    }
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      list.add(new DecNumber(buffer, offset + 132, 2, "Override point: X"));
      list.add(new DecNumber(buffer, offset + 134, 2, "Override point: Y"));
      list.add(new DecNumber(buffer, offset + 136, 4, "Alternate point: X"));
      list.add(new DecNumber(buffer, offset + 140, 4, "Alternate point: Y"));
      list.add(new Unknown(buffer, offset + 144, 52));
    }
    else {
      list.add(new DecNumber(buffer, offset + 132, 2, "Activation point: X"));
      list.add(new DecNumber(buffer, offset + 134, 2, "Activation point: Y"));
      list.add(new Unknown(buffer, offset + 136, 60));
    }
//      list.add(new ResourceRef(buffer, offset + 188, "Proximity trigger dialog", "DLG"));
    return offset + 196;
  }
}

