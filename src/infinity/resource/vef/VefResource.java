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
  // VEF-specific field labels
  public static final String VEF_OFFSET_COMPONENT_PRI = "Primary component offset";
  public static final String VEF_NUM_COMPONENT_PRI    = "Primary component count";
  public static final String VEF_OFFSET_COMPONENT_SEC = "Secondary component offset";
  public static final String VEF_NUM_COMPONENT_SEC    = "Secondary component count";

  private HexViewer hexViewer;

  public VefResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception

  {
    return new AddRemovable[]{new PrimaryComponent(), new SecondaryComponent()};
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
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    SectionOffset offset_component1 = new SectionOffset(buffer, offset + 8, VEF_OFFSET_COMPONENT_PRI,
                                                        PrimaryComponent.class);
    addField(offset_component1);
    SectionCount count_component1 = new SectionCount(buffer, offset + 12, 4, VEF_NUM_COMPONENT_PRI,
                                                     PrimaryComponent.class);
    addField(count_component1);
    SectionOffset offset_component2 = new SectionOffset(buffer, offset + 16, VEF_OFFSET_COMPONENT_SEC,
                                                        SecondaryComponent.class);
    addField(offset_component2);
    SectionCount count_component2 = new SectionCount(buffer, offset + 20, 4, VEF_NUM_COMPONENT_SEC,
                                                     SecondaryComponent.class);
    addField(count_component2);

    offset = offset_component1.getValue();
    for (int i = 0; i < count_component1.getValue(); i++) {
      PrimaryComponent comp1 = new PrimaryComponent(this, buffer, offset);
      offset = comp1.getEndOffset();
      addField(comp1);
    }

    offset = offset_component2.getValue();
    for (int i = 0; i < count_component2.getValue(); i++) {
      SecondaryComponent comp2 = new SecondaryComponent(this, buffer, offset);
      offset = comp2.getEndOffset();
      addField(comp2);
    }

    int endoffset = offset;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
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
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }
}
