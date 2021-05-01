// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.chu;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.graphics.BamResource;
import org.infinity.resource.graphics.MosResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.tuples.Couple;

/**
 * This resource describes the layout of the GUI screens (the graphics for the
 * screens are held in {@link MosResource MOS} and {@link BamResource BAM} files).
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/chu_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/chu_v1.htm</a>
 */
public final class ChuResource extends AbstractStruct implements Resource, HasViewerTabs //, HasChildStructs
{
  // CHU-specific field labels
  public static final String CHU_NUM_PANELS       = "# panels";
  public static final String CHU_OFFSET_CONTROLS  = "Controls offset";
  public static final String CHU_OFFSET_PANELS    = "Panels offset";

  private List<Couple<Integer, Integer>> listControls;
  private int ofsPanels, numPanels, sizePanels, ofsControls, numControls;
  private Viewer detailViewer;
  private StructHexViewer hexViewer;

  public ChuResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.write(os);
    for (final StructEntry o : getFields()) {
      if (o instanceof Window) {
        ((Window)o).writeControlsTable(os);
      }
    }
    for (final StructEntry o : getFields()) {
      if (o instanceof Window) {
        ((Window)o).writeControls(os);
      }
    }
  }

// --------------------- End Interface Writeable ---------------------

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
        if (detailViewer == null) {
          detailViewer = new Viewer(this);
        }
        return detailViewer;
      }
      case 1:
      {
        if (hexViewer == null) {
          BasicColorMap colorMap = new BasicColorMap(this, false);
          colorMap.setColoredEntry(BasicColorMap.Coloring.BLUE, Window.class);
          colorMap.setColoredEntry(BasicColorMap.Coloring.GREEN, Control.class);
          hexViewer = new StructHexViewer(this, colorMap);
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

  /** Write 'size' number of zeros to the output stream. */
  void writeGap(OutputStream os, int startOfs, int endOfs) throws IOException
  {
    while (startOfs < endOfs) {
      os.write(0);
      startOfs++;
    }
  }

  /** Returns the starting offset of available panels. */
  public int getPanelsOffset()
  {
    return ofsPanels;
  }

  /** Returns the number of available panels. */
  public int getPanelCount()
  {
    return numPanels;
  }

  /** Returns the starting offset of the control table. */
  public int getControlsOffset()
  {
    return ofsControls;
  }

  /** Returns the number of available controls. */
  public int getControlCount()
  {
    return numControls;
  }

  /** Returns the absolute starting offset of the control at the given index. */
  public int getControlOffset(int index)
  {
    if (index >= 0 && index < listControls.size()) {
      return listControls.get(index).getValue0();
    } else {
      return 0;
    }
  }

  /** Returns the size of the control at the given index. */
  public int getControlSize(int index)
  {
    if (index >= 0 && index < listControls.size()) {
      return listControls.get(index).getValue1();
    } else {
      return 0;
    }
  }

  /** Returns the panel size in bytes. */
  public int getPanelSize()
  {
    return sizePanels;
  }

  /** Returns the given panel. */
  public Window getPanel(int index)
  {
    if (index >= 0 && index < getPanelCount()) {
      return (Window)getAttribute(Window.CHU_WINDOW_PANEL + " " + index);
    } else {
      return null;
    }
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    initData(buffer, offset);

    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new SectionCount(buffer, offset + 8, 4, CHU_NUM_PANELS, Window.class));
    addField(new SectionOffset(buffer, offset + 12, CHU_OFFSET_CONTROLS, Control.class));
    addField(new SectionOffset(buffer, offset + 16, CHU_OFFSET_PANELS, Window.class));
    offset += 20;

    // handling optional gap between header and panels section
    int endoffset = Math.min(getPanelsOffset(), getControlsOffset());
    if (offset < endoffset) {
      addField(new Unknown(buffer, offset, endoffset - offset, COMMON_UNUSED));
    }

    offset = endoffset;
    for (int i = 0; i < getPanelCount(); i++) {
      Window window = new Window(this, buffer, offset, i);
      offset = window.getEndOffset();
      endoffset = Math.max(endoffset, window.readControls(buffer));
      addField(window);
    }

    // handling optional gap between panels section and controls section
    if (offset < ofsControls) {
      addField(new Unknown(buffer, offset, ofsControls - offset, COMMON_UNUSED));
    }

    return Math.max(offset, endoffset);
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

  // initialize data required to reconstruct the original resource on save
  private void initData(ByteBuffer buffer, int offset)
  {
    // loading header data
    numPanels = buffer.getInt(offset + 8);
    ofsControls = buffer.getInt(offset + 12);
    ofsPanels = buffer.getInt(offset + 16);
    if (numPanels > 0) {
      sizePanels = Math.abs(ofsControls - ofsPanels) / numPanels;
    } else {
      sizePanels = 28;
    }
    if (sizePanels >= 36) sizePanels = 36; else if (sizePanels >= 28) sizePanels = 28;

    // loading controls data
    numControls = 0;
    int curOfs = ofsPanels;
    int skip = (sizePanels == 36) ? 8 : 0;
    for (int i = 0; i < numPanels; i++) {
      int numCtrl = buffer.getShort(curOfs + skip + 14);
      int startIdx = buffer.getShort(curOfs + skip + 24);
      numControls = Math.max(numControls, startIdx + numCtrl);
      curOfs += sizePanels;
    }

    // Adding offset/size pairs for each control
    curOfs = ofsControls;
    if (listControls == null) {
      listControls = new ArrayList<>();
    }
    int ofs = 0, len = 0;
    for (int i = 0; i < numControls; i++, curOfs += 8) {
      ofs = buffer.getInt(curOfs);
      len = buffer.getInt(curOfs + 4);
      listControls.add(Couple.with(Integer.valueOf(ofs), Integer.valueOf(len)));
    }

    // adding virtual entry for determining the true size of the last control entry
    ofs = Math.max(ofs + len, buffer.limit());
    listControls.add(Couple.with(Integer.valueOf(ofs), Integer.valueOf(0)));
  }
}
