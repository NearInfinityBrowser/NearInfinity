// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.var;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

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
 * This resource is used to declare variables for use in scripting - it is the
 * only way new variables can be added. Variables should be alphabetically ordered
 * (within their appropriate scope section). Globals and Locals work as with other
 * engines, the {@code KAPUTZ} scope acts as the {@code SPRITE_ID_DEAD} scope of
 * the BG series. The {@code var.var} file is located the root folder of the game
 * directory.
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/var.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/var.htm</a>
 */
public final class VarResource extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs
{
  private StructHexViewer hexViewer;

  public VarResource(ResourceEntry entry) throws Exception
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
    int count = buffer.limit() / 44;
    for (int i = 0; i < count; i++)
      addField(new Entry(this, buffer, offset + i * 44, i));
    return offset + count * 44;
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
