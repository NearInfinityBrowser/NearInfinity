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
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Profile;
import infinity.resource.StructEntry;
import infinity.resource.vertex.Vertex;

import javax.swing.JComponent;

public final class Container extends AbstractStruct implements AddRemovable, HasVertices, HasViewerTabs,
                                                               HasAddRemovable
{
  public static final String[] s_type = { "", "Bag", "Chest", "Drawer", "Pile", "Table", "Shelf",
                                          "Altar", "Non-visible", "Spellbook", "Body", "Barrel", "Crate"};
  public static final String[] s_noyes = {"No", "Yes"};
  public static final String[] s_flag = { "No flags set", "Locked", "", "Magical lock", "Trap resets",
                                          "", "Disabled" };
  public static final String[] s_flag_ee = { "No flags set", "Locked", "", "Magical lock", "Trap resets",
                                             "", "Disabled", "Don't clear" };

  public Container() throws Exception
  {
    super(null, "Container", new byte[192], 0);
  }

  public Container(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Container " + nr, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Vertex(), new Item()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    return new ViewerContainer(this);
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------


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
    for (int i = 0; i < numItems; i++) {
      addField(new Item(this, buffer, offset + 20 * i, i));
    }
//    return offset + numItems * 20;
  }

  public int updateItems(int offset, int number)
  {
    ((DecNumber)getAttribute("First item index")).setValue(number);
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
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

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    addField(new Bitmap(buffer, offset + 36, 2, "Type", s_type));
    addField(new DecNumber(buffer, offset + 38, 2, "Lock difficulty"));
    if (Profile.isEnhancedEdition()) {
      addField(new Flag(buffer, offset + 40, 4, "Flags", s_flag_ee));
    } else {
      addField(new Flag(buffer, offset + 40, 4, "Flags", s_flag));
    }
    addField(new DecNumber(buffer, offset + 44, 2, "Trap detection difficulty"));
    addField(new DecNumber(buffer, offset + 46, 2, "Trap removal difficulty"));
    addField(new Bitmap(buffer, offset + 48, 2, "Is trapped?", s_noyes));
    addField(new Bitmap(buffer, offset + 50, 2, "Is trap detected?", s_noyes));
    addField(new DecNumber(buffer, offset + 52, 2, "Launch point: X"));
    addField(new DecNumber(buffer, offset + 54, 2, "Launch point: Y"));
    addField(new DecNumber(buffer, offset + 56, 2, "Bounding box: Left"));
    addField(new DecNumber(buffer, offset + 58, 2, "Bounding box: Top"));
    addField(new DecNumber(buffer, offset + 60, 2, "Bounding box: Right"));
    addField(new DecNumber(buffer, offset + 62, 2, "Bounding box: Bottom"));
    addField(new DecNumber(buffer, offset + 64, 4, "First item index"));
    addField(new DecNumber(buffer, offset + 68, 4, "# items"));
    addField(new ResourceRef(buffer, offset + 72, "Trap script", "BCS"));
    addField(new DecNumber(buffer, offset + 80, 4, "First vertex index"));
    addField(new DecNumber(buffer, offset + 84, 2, "# vertices"));
    addField(new DecNumber(buffer, offset + 86, 2, "Activation range"));
    addField(new TextString(buffer, offset + 88, 32, "Owner name"));
//    addField(new ResourceRef(buffer, offset + 88, "Creature?", "CRE"));
//    addField(new Unknown(buffer, offset + 96, 24));
    addField(new ResourceRef(buffer, offset + 120, "Key", "ITM"));
    addField(new Unknown(buffer, offset + 128, 4));
    addField(new StringRef(buffer, offset + 132, "Lockpick string"));
    addField(new Unknown(buffer, offset + 136, 56));
    return offset + 192;
  }
}

