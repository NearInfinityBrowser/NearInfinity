// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.DecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

public final class WmpResource extends AbstractStruct implements Resource, HasViewerTabs
{
  private HexViewer hexViewer;

  public WmpResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 2;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return StructViewer.TAB_RAW;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
      {
        JTabbedPane tabbedPane = new JTabbedPane();
        int count = ((DecNumber)getAttribute("# maps")).getValue();
        for (int i = 0; i < count; i++) {
          MapEntry entry = (MapEntry)getAttribute("Map " + i);
          tabbedPane.addTab(entry.getName(), entry.getViewerTab(0));
        }
        return tabbedPane;
      }
      case 1:
      {
        if (hexViewer == null) {
          hexViewer = new HexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
      }
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

// --------------------- End Interface HasViewerTabs ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, "Signature"));
    addField(new TextString(buffer, offset + 4, 4, "Version"));
    SectionCount entry_count = new SectionCount(buffer, offset + 8, 4, "# maps", MapEntry.class);
    addField(entry_count);
    SectionOffset entry_offset = new SectionOffset(buffer, offset + 12, "Maps offset", MapEntry.class);
    addField(entry_offset);
    offset = entry_offset.getValue();
    for (int i = 0; i < entry_count.getValue(); i++) {
      MapEntry entry = new MapEntry(this, buffer, offset, i);
      offset = entry.getEndOffset();
      addField(entry);
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

