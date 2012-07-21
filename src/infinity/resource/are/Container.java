// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.vertex.Vertex;

import javax.swing.*;

public final class Container extends AbstractStruct implements AddRemovable, HasVertices, HasDetailViewer,
                                                               HasAddRemovable
{
  private static final String s_type[] = {
    "", "Bag", "Chest", "Drawer", "Pile", "Table", "Shelf",
    "Altar", "Non-visible", "Spellbook", "Body", "Barrel", "Crate"};
  private static final String s_yesno[] = {"No", "Yes"};
  private static final String s_flag[] = { "No flags set", "Locked", "", "Magical lock", "Trap resets",
                                           "", "Disabled" };

  public Container() throws Exception
  {
    super(null, "Container", new byte[192], 0);
  }

  public Container(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Container " + nr, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Vertex(), new Item()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    return new ViewerContainer(this);
  }

// --------------------- End Interface HasDetailViewer ---------------------


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
    else if (datatype instanceof Item) {
      int index = ((DecNumber)getAttribute("First item index")).getValue();
      index += ((DecNumber)getAttribute("# items")).getValue();
      int offset = ((HexNumber)getSuperStruct().getAttribute("Items offset")).getValue();
      datatype.setOffset(offset + 20 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  public void readItems(byte buffer[], int offset) throws Exception
  {
    int firstIndex = ((DecNumber)getAttribute("First item index")).getValue();
    int numItems = ((DecNumber)getAttribute("# items")).getValue();
    offset += firstIndex * 20;
    for (int i = 0; i < numItems; i++)
      list.add(new Item(this, buffer, offset + 20 * i, i));
//    return offset + numItems * 20;
  }

  public int updateItems(int offset, int number)
  {
    ((DecNumber)getAttribute("First item index")).setValue(number);
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry instanceof Item) {
        entry.setOffset(offset);
        ((Item)entry).realignStructOffsets();
        offset += 20;
        count++;
      }
    }
    ((DecNumber)getAttribute("# items")).setValue(count);
    return count;
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    list.add(new Bitmap(buffer, offset + 36, 2, "Type", s_type));
    list.add(new DecNumber(buffer, offset + 38, 2, "Lock difficulty"));
    list.add(new Flag(buffer, offset + 40, 4, "Flags", s_flag));
    list.add(new DecNumber(buffer, offset + 44, 2, "Trap detection difficulty"));
    list.add(new DecNumber(buffer, offset + 46, 2, "Trap removal difficulty"));
    list.add(new Bitmap(buffer, offset + 48, 2, "Is trapped?", s_yesno));
    list.add(new Bitmap(buffer, offset + 50, 2, "Is trap detected?", s_yesno));
    list.add(new DecNumber(buffer, offset + 52, 2, "Launch point: X"));
    list.add(new DecNumber(buffer, offset + 54, 2, "Launch point: Y"));
    list.add(new DecNumber(buffer, offset + 56, 2, "Bounding box: Left"));
    list.add(new DecNumber(buffer, offset + 58, 2, "Bounding box: Top"));
    list.add(new DecNumber(buffer, offset + 60, 2, "Bounding box: Right"));
    list.add(new DecNumber(buffer, offset + 62, 2, "Bounding box: Bottom"));
    list.add(new DecNumber(buffer, offset + 64, 4, "First item index"));
    list.add(new DecNumber(buffer, offset + 68, 4, "# items"));
    list.add(new ResourceRef(buffer, offset + 72, "Trap script", "BCS"));
    list.add(new DecNumber(buffer, offset + 80, 4, "First vertex index"));
    list.add(new DecNumber(buffer, offset + 84, 2, "# vertices"));
    list.add(new DecNumber(buffer, offset + 86, 2, "Activation range"));
    list.add(new TextString(buffer, offset + 88, 32, "Owner name"));
//    list.add(new ResourceRef(buffer, offset + 88, "Creature?", "CRE"));
//    list.add(new Unknown(buffer, offset + 96, 24));
    list.add(new ResourceRef(buffer, offset + 120, "Key", "ITM"));
    list.add(new Unknown(buffer, offset + 128, 4));
    list.add(new StringRef(buffer, offset + 132, "Lockpick string"));
    list.add(new Unknown(buffer, offset + 136, 56));
    return offset + 192;
  }
}

