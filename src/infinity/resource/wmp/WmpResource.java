// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.DecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

public final class WmpResource extends AbstractStruct implements Resource, HasViewerTabs
{
  public WmpResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

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
    JTabbedPane tabbedPane = new JTabbedPane();
    int count = ((DecNumber)getAttribute("# maps")).getValue();
    for (int i = 0; i < count; i++) {
      MapEntry entry = (MapEntry)getAttribute("Map " + i);
      tabbedPane.addTab(entry.getName(), entry.getViewerTab(0));
    }
    return tabbedPane;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
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
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    SectionCount entry_count = new SectionCount(buffer, offset + 8, 4, "# maps", MapEntry.class);
    list.add(entry_count);
    SectionOffset entry_offset = new SectionOffset(buffer, offset + 12, "Maps offset", MapEntry.class);
    list.add(entry_offset);
    offset = entry_offset.getValue();
    for (int i = 0; i < entry_count.getValue(); i++) {
      MapEntry entry = new MapEntry(this, buffer, offset, i);
      offset = entry.getEndOffset();
      list.add(entry);
    }
    return offset;
  }
}

