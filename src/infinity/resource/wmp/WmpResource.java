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
import infinity.gui.hexview.StructHexViewer;
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
  // WMP-specific field labels
  public static final String WMP_NUM_MAPS     = "# maps";
  public static final String WMP_OFFSET_MAPS  = "Maps offset";

  private StructHexViewer hexViewer;

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
        int count = ((DecNumber)getAttribute(WMP_NUM_MAPS)).getValue();
        for (int i = 0; i < count; i++) {
          MapEntry entry = (MapEntry)getAttribute(MapEntry.WMP_MAP + " " + i);
          tabbedPane.addTab(entry.getName(), entry.getViewerTab(0));
        }
        return tabbedPane;
      }
      case 1:
      {
        if (hexViewer == null) {
          hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
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
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    SectionCount entry_count = new SectionCount(buffer, offset + 8, 4, WMP_NUM_MAPS, MapEntry.class);
    addField(entry_count);
    SectionOffset entry_offset = new SectionOffset(buffer, offset + 12, WMP_OFFSET_MAPS, MapEntry.class);
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

