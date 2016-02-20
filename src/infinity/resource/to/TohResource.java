// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;

import infinity.datatype.DecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.ButtonPanel;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.StructHexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

public final class TohResource extends AbstractStruct implements Resource, HasViewerTabs
{
  // TOH-specific field labels
  public static final String TOH_NUM_ENTRIES    = "# strref entries";
  public static final String TOH_OFFSET_ENTRIES = "Strref entries offset";

  private StructHexViewer hexViewer;

  public TohResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

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
      colorMap.setColoredEntry(BasicColorMap.Coloring.BLUE, StrRefEntry.class);
      colorMap.setColoredEntry(BasicColorMap.Coloring.GREEN, StrRefEntry2.class);
      colorMap.setColoredEntry(BasicColorMap.Coloring.RED, StringEntry.class);
      colorMap.setColoredEntry(BasicColorMap.Coloring.CYAN, StringEntry2.class);
      hexViewer = new StructHexViewer(this, colorMap);
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
  public void close() throws Exception
  {
    // don't save changes
  }

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    int startOffset = offset;
    boolean isEnhanced = Profile.isEnhancedEdition() && (DynamicArray.getInt(buffer, offset + 4) == 2);
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new DecNumber(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new Unknown(buffer, offset + 8, 4));
    SectionCount scStrref = new SectionCount(buffer, offset + 12, 4, TOH_NUM_ENTRIES, StrRefEntry.class);
    addField(scStrref);
    SectionOffset soStrref = null;
    if (isEnhanced) {
      soStrref = new SectionOffset(buffer, offset + 16, TOH_OFFSET_ENTRIES, StrRefEntry.class);
      addField(soStrref);
    } else {
      addField(new Unknown(buffer, offset + 16, 4));
    }

    List<Integer> ofsList = null;
    offset = 20;
    if (isEnhanced) {
      offset = soStrref.getValue();
      ofsList = new ArrayList<Integer>(scStrref.getValue());
    }
    for (int i = 0; i < scStrref.getValue(); i++) {
      if (isEnhanced) {
        // storing string offset for later
        int ofs = soStrref.getValue() + DynamicArray.getInt(buffer, offset + 4);
        ofsList.add(ofs);
        // adding strref entries structure
        StrRefEntry2 entry = new StrRefEntry2(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      } else {
        StrRefEntry entry = new StrRefEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      }
    }

    if (isEnhanced) {
      for (int i = 0; i < scStrref.getValue(); i++) {
        StringEntry2 entry = new StringEntry2(this, buffer, startOffset + ofsList.get(i), i);
        addField(entry);
        offset += entry.getEndOffset();
      }
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

    // disabling 'Save' button
    JButton bSave = (JButton)viewer.getButtonPanel().getControlByType(ButtonPanel.Control.Save);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
  }
}
