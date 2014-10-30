// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vef;

import javax.swing.JComponent;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;

public final class VefResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{

  private HexViewer hexViewer;

  public VefResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception

  {
    return new AddRemovable[]{new Component1(), new Component2()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

//--------------------- Begin Interface HasViewerTabs ---------------------

 @Override
 public int getViewerTabCount()
 {
   return 1;
 }

 @Override
 public String getViewerTabName(int index)
 {
   return StructViewer.TAB_RAW;
 }

 @Override
 public JComponent getViewerTab(int index)
 {
   if (hexViewer == null) {
     hexViewer = new HexViewer(this, new BasicColorMap(this, true));
   }
   return hexViewer;
 }

 @Override
 public boolean viewerTabAddedBefore(int index)
 {
   return false;
 }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    SectionOffset offset_component1 = new SectionOffset(buffer, offset + 8, "Component1 offset", Component1.class);
    list.add(offset_component1);
    SectionCount count_component1 = new SectionCount(buffer, offset + 12, 4, "Component1 count", Component1.class);
    list.add(count_component1);
    SectionOffset offset_component2 = new SectionOffset(buffer, offset + 16, "Component2 offset", Component2.class);
    list.add(offset_component2);
    SectionCount count_component2 = new SectionCount(buffer, offset + 20, 4, "Component2 count", Component2.class);
    list.add(count_component2);

    offset = offset_component1.getValue();
    for (int i = 0; i < count_component1.getValue(); i++) {
      Component1 comp1 = new Component1(this, buffer, offset);
      offset = comp1.getEndOffset();
      list.add(comp1);
    }

    offset = offset_component2.getValue();
    for (int i = 0; i < count_component2.getValue(); i++) {
      Component2 comp2 = new Component2(this, buffer, offset);
      offset = comp2.getEndOffset();
      list.add(comp2);
    }

    int endoffset = offset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    hexViewer.dataModified();
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    hexViewer.dataModified();
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    hexViewer.dataModified();
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    hexViewer.dataModified();
  }
}
