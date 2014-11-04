// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.src;

import javax.swing.JComponent;

import infinity.datatype.SectionCount;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class SrcResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
  private HexViewer hexViewer;

  public SrcResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Entry()};
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
      BasicColorMap colorMap = new BasicColorMap(this, false);
      colorMap.setColoredEntry(BasicColorMap.Coloring.BLUE, Entry.class);
      hexViewer = new HexViewer(this, colorMap);
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
    SectionCount entry_count = new SectionCount(buffer, offset, 4, "# entries", Entry.class);
    addField(entry_count);
    offset += 4;
    for (int i = 0; i < entry_count.getValue(); i++) {
      Entry entry = new Entry(this, buffer, offset, i);
      addField(entry);
      offset = entry.getEndOffset();
    }
    return offset;
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

