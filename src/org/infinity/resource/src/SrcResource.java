// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.src;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.SectionCount;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.key.ResourceEntry;

/**
 * This resource contain strrefs for out-of-dialog text (e.g text displayed over
 * the chracter on the game screen). Text is usually displayed over the creatures
 * head, and the associated sound is played.
 * <p>
 * SRC files have a very simple header, simply containing the count of strref entries.
 * Each strref entry is eight bytes and consists of a strref and an unknown dword
 * (always set to 1).
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/src.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/src.htm</a>
 */
public final class SrcResource extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs
{
  // SRC-specific field labels
  public static final String SRC_NUM_ENTRIES  = "# entries";

  private StructHexViewer hexViewer;

  public SrcResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    return new AddRemovable[]{new Entry()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

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
      hexViewer = new StructHexViewer(this, colorMap);
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    SectionCount entry_count = new SectionCount(buffer, offset, 4, SRC_NUM_ENTRIES, Entry.class);
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
