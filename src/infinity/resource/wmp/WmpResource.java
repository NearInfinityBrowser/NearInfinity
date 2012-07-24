// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public final class WmpResource extends AbstractStruct implements Resource, HasDetailViewer
{
  public WmpResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    JTabbedPane tabbedPane = new JTabbedPane();
    int count = ((DecNumber)getAttribute("# maps")).getValue();
    for (int i = 0; i < count; i++) {
      MapEntry entry = (MapEntry)getAttribute("Map " + i);
      tabbedPane.addTab(entry.getName(), entry.getDetailViewer());
    }
    return tabbedPane;
  }

// --------------------- End Interface HasDetailViewer ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
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

