// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Profile;
import infinity.resource.StructEntry;
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

  public ITEPoint(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Trigger " + number, buffer, offset);
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


// --------------------- Begin Interface HasVertices ---------------------

  @Override
  public void readVertices(byte buffer[], int offset) throws Exception
  {
    int firstVertex = ((DecNumber)getAttribute("First vertex index")).getValue();
    int numVertices = ((DecNumber)getAttribute("# vertices")).getValue();
    offset += firstVertex << 2;
    for (int i = 0; i < numVertices; i++) {
      addField(new Vertex(this, buffer, offset + 4 * i, i));
    }
  }

  @Override
  public int updateVertices(int offset, int number)
  {
    ((DecNumber)getAttribute("First vertex index")).setValue(number);
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
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

  @Override
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

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new Bitmap(buffer, offset + 32, 2, "Type", s_type));
    addField(new DecNumber(buffer, offset + 34, 2, "Bounding box: Left"));
    addField(new DecNumber(buffer, offset + 36, 2, "Bounding box: Top"));
    addField(new DecNumber(buffer, offset + 38, 2, "Bounding box: Right"));
    addField(new DecNumber(buffer, offset + 40, 2, "Bounding box: Bottom"));
    addField(new DecNumber(buffer, offset + 42, 2, "# vertices"));
    addField(new DecNumber(buffer, offset + 44, 4, "First vertex index"));
    addField(new Unknown(buffer, offset + 48, 4));
    addField(new DecNumber(buffer, offset + 52, 4, "Cursor number"));
    addField(new ResourceRef(buffer, offset + 56, "Destination area", "ARE"));
    addField(new TextString(buffer, offset + 64, 32, "Entrance name"));
    addField(new Flag(buffer, offset + 96, 4, "Flags", s_flag));
    addField(new StringRef(buffer, offset + 100, "Info point text"));
    addField(new DecNumber(buffer, offset + 104, 2, "Trap detection difficulty"));
    addField(new DecNumber(buffer, offset + 106, 2, "Trap removal difficulty"));
    addField(new Bitmap(buffer, offset + 108, 2, "Is trapped?", s_yesno));
    addField(new Bitmap(buffer, offset + 110, 2, "Is trap detected?", s_yesno));
    addField(new DecNumber(buffer, offset + 112, 2, "Launch point: X"));
    addField(new DecNumber(buffer, offset + 114, 2, "Launch point: Y"));
    addField(new ResourceRef(buffer, offset + 116, "Key", "ITM")); // Key type?
    addField(new ResourceRef(buffer, offset + 124, "Script", "BCS"));
//    addField(new DecNumber(buffer, offset + 132, 2, "Override box: Left"));
//    addField(new DecNumber(buffer, offset + 134, 2, "Override box: Top"));
//    addField(new DecNumber(buffer, offset + 136, 2, "Override box: Right"));
//    addField(new DecNumber(buffer, offset + 138, 2, "Override box: Bottom"));
    if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Unknown(buffer, offset + 132, 48));
      addField(new DecNumber(buffer, offset + 180, 2, "Speaker point: X"));
      addField(new DecNumber(buffer, offset + 182, 2, "Speaker point: Y"));
      addField(new StringRef(buffer, offset + 184, "Speaker name"));
      addField(new ResourceRef(buffer, offset + 188, "Dialogue", "DLG"));
    }
    else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new DecNumber(buffer, offset + 132, 2, "Override point: X"));
      addField(new DecNumber(buffer, offset + 134, 2, "Override point: Y"));
      addField(new DecNumber(buffer, offset + 136, 4, "Alternate point: X"));
      addField(new DecNumber(buffer, offset + 140, 4, "Alternate point: Y"));
      addField(new Unknown(buffer, offset + 144, 52));
    }
    else {
      addField(new DecNumber(buffer, offset + 132, 2, "Activation point: X"));
      addField(new DecNumber(buffer, offset + 134, 2, "Activation point: Y"));
      addField(new Unknown(buffer, offset + 136, 60));
    }
//      list.add(new ResourceRef(buffer, offset + 188, "Proximity trigger dialog", "DLG"));
    return offset + 196;
  }
}

