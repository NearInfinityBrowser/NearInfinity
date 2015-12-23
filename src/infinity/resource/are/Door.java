// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Profile;
import infinity.resource.StructEntry;
import infinity.resource.vertex.ClosedVertex;
import infinity.resource.vertex.ClosedVertexImpeded;
import infinity.resource.vertex.OpenVertex;
import infinity.resource.vertex.OpenVertexImpeded;
import infinity.resource.vertex.Vertex;

public final class Door extends AbstractStruct implements AddRemovable, HasVertices, HasAddRemovable
{
  public static final String[] s_noyes = {"No", "Yes"};
  public static final String[] s_flag = {"No flags set", "Door open", "Door locked", "Trap resets",
                                         "Detectable trap", "Door forced", "Cannot close", "Door located",
                                         "Door secret", "Secret door detected", "Can be looked through",
                                         "Uses key", "Sliding door"};
  public static final String[] s_flag_iwd2 = {"No flags set", "Door open", "Door locked", "Trap resets",
                                              "Detectable trap", "Door forced", "Cannot close", "Door located",
                                              "Door secret", "Secret door detected", "Alternate lock string",
                                              "Can be looked through", "Warn on activate", "Displayed warning",
                                              "Door hidden", "Uses key"};

  public Door() throws Exception
  {
    super(null, "Door", new byte[200], 0);
  }

  public Door(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Door " + nr, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new OpenVertex(), new ClosedVertex(), new ClosedVertexImpeded(),
                              new OpenVertexImpeded()};
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
    DecNumber firstVertex = (DecNumber)getAttribute("First vertex index (open)");
    DecNumber numVertices = (DecNumber)getAttribute("# vertices (open)");
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new OpenVertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }

    firstVertex = (DecNumber)getAttribute("First vertex index (closed)");
    numVertices = (DecNumber)getAttribute("# vertices (closed)");
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new ClosedVertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }

    firstVertex = (DecNumber)getAttribute("First vertex index (impeded, open)");
    numVertices = (DecNumber)getAttribute("# vertices (impeded, open)");
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new OpenVertexImpeded(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }

    firstVertex = (DecNumber)getAttribute("First vertex index (impeded, closed)");
    numVertices = (DecNumber)getAttribute("# vertices (impeded, closed)");
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new ClosedVertexImpeded(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }
  }

  @Override
  public int updateVertices(int offset, int number)
  {
    // Må anta at antallet er riktig
    ((DecNumber)getAttribute("First vertex index (open)")).setValue(number);
    int count = ((DecNumber)getAttribute("# vertices (open)")).getValue();
    ((DecNumber)getAttribute("First vertex index (closed)")).setValue(number + count);
    count += ((DecNumber)getAttribute("# vertices (closed)")).getValue();
    ((DecNumber)getAttribute("First vertex index (impeded, open)")).setValue(number + count);
    count += ((DecNumber)getAttribute("# vertices (impeded, open)")).getValue();
    ((DecNumber)getAttribute("First vertex index (impeded, closed)")).setValue(number + count);
    count += ((DecNumber)getAttribute("# vertices (impeded, closed)")).getValue();

    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry instanceof Vertex) {
        entry.setOffset(offset);
        ((Vertex)entry).realignStructOffsets();
        offset += 4;
      }
    }
    return count;
  }

// --------------------- End Interface HasVertices ---------------------

  @Override
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    int offset = ((HexNumber)getSuperStruct().getAttribute("Vertices offset")).getValue();
    if (datatype instanceof OpenVertex) {
      int index = ((DecNumber)getAttribute("First vertex index (open)")).getValue();
      index += ((DecNumber)getAttribute("# vertices (open)")).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
    else if (datatype instanceof ClosedVertex) {
      int index = ((DecNumber)getAttribute("First vertex index (closed)")).getValue();
      index += ((DecNumber)getAttribute("# vertices (closed)")).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
    else if (datatype instanceof OpenVertexImpeded) {
      int index = ((DecNumber)getAttribute("First vertex index (impeded, open)")).getValue();
      index += ((DecNumber)getAttribute("# vertices (impeded, open)")).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
    else if (datatype instanceof ClosedVertexImpeded) {
      int index = ((DecNumber)getAttribute("First vertex index (impeded, closed)")).getValue();
      index += ((DecNumber)getAttribute("# vertices (impeded, closed)")).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new TextString(buffer, offset + 32, 8, "Door ID"));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Flag(buffer, offset + 40, 4, "Flags", s_flag_iwd2));
    } else {
      addField(new Flag(buffer, offset + 40, 4, "Flags", s_flag));
    }
    addField(new DecNumber(buffer, offset + 44, 4, "First vertex index (open)"));
    addField(new SectionCount(buffer, offset + 48, 2, "# vertices (open)", OpenVertex.class));
    addField(new SectionCount(buffer, offset + 50, 2, "# vertices (closed)", ClosedVertex.class));
    addField(new DecNumber(buffer, offset + 52, 4, "First vertex index (closed)"));
    addField(new DecNumber(buffer, offset + 56, 2, "Bounding box (open): Left"));
    addField(new DecNumber(buffer, offset + 58, 2, "Bounding box (open): Top"));
    addField(new DecNumber(buffer, offset + 60, 2, "Bounding box (open): Right"));
    addField(new DecNumber(buffer, offset + 62, 2, "Bounding box (open): Bottom"));
    addField(new DecNumber(buffer, offset + 64, 2, "Bounding box (closed): Left"));
    addField(new DecNumber(buffer, offset + 66, 2, "Bounding box (closed): Top"));
    addField(new DecNumber(buffer, offset + 68, 2, "Bounding box (closed): Right"));
    addField(new DecNumber(buffer, offset + 70, 2, "Bounding box (closed): Bottom"));
    addField(new DecNumber(buffer, offset + 72, 4, "First vertex index (impeded, open)"));
    addField(new SectionCount(buffer, offset + 76, 2, "# vertices (impeded, open)",
                              OpenVertexImpeded.class));
    addField(new SectionCount(buffer, offset + 78, 2, "# vertices (impeded, closed)",
                              ClosedVertexImpeded.class));
    addField(new DecNumber(buffer, offset + 80, 4, "First vertex index (impeded, closed)"));
    addField(new DecNumber(buffer, offset + 84, 2, "Current HP"));
    addField(new DecNumber(buffer, offset + 86, 2, "Effective AC"));
    addField(new ResourceRef(buffer, offset + 88, "Opening sound", "WAV"));
    addField(new ResourceRef(buffer, offset + 96, "Closing sound", "WAV"));
    addField(new DecNumber(buffer, offset + 104, 4, "Cursor number"));
    addField(new DecNumber(buffer, offset + 108, 2, "Trap detection difficulty"));
    addField(new DecNumber(buffer, offset + 110, 2, "Trap removal difficulty"));
    addField(new Bitmap(buffer, offset + 112, 2, "Is trapped?", s_noyes));
    addField(new Bitmap(buffer, offset + 114, 2, "Is trap detected?", s_noyes));
    addField(new DecNumber(buffer, offset + 116, 2, "Launch point: X"));
    addField(new DecNumber(buffer, offset + 118, 2, "Launch point: Y"));
    addField(new ResourceRef(buffer, offset + 120, "Key", "ITM"));
    addField(new ResourceRef(buffer, offset + 128, "Script", "BCS"));
    addField(new DecNumber(buffer, offset + 136, 4, "Detection difficulty"));
    addField(new DecNumber(buffer, offset + 140, 4, "Lock difficulty"));
    addField(new DecNumber(buffer, offset + 144, 2, "Open location: X"));
    addField(new DecNumber(buffer, offset + 146, 2, "Open location: Y"));
    addField(new DecNumber(buffer, offset + 148, 2, "Close location: X"));
    addField(new DecNumber(buffer, offset + 150, 2, "Close location: Y"));
    addField(new StringRef(buffer, offset + 152, "Unlock message"));
    addField(new TextString(buffer, offset + 156, 24, "Travel trigger name"));
//    addField(new Unknown(buffer, offset + 172, 8));
    addField(new StringRef(buffer, offset + 180, "Speaker name"));
    addField(new ResourceRef(buffer, offset + 184, "Dialogue", "DLG"));
    addField(new Unknown(buffer, offset + 192, 8));
    return offset + 200;
  }
}

