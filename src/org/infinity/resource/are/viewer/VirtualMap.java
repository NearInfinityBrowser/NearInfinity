// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.StreamUtils;

/**
 * Creates a root {@link AbstractStruct} instance that can hold an arbitrary amount of {@link VirtualPosition}
 * substructures.
 * <p>
 * This class is intended to be used as a virtual resource structure for storing map-related data that is not present in
 * the ARE or WED resources of a map.
 * </p>
 */
public class VirtualMap extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs {
  public static final String VIRTUAL_MAP                  = "Virtual Map";
  public static final String VIRTUAL_MAP_OFFSET_POSITIONS = "Map Positions offset";
  public static final String VIRTUAL_MAP_NUM_POSITIONS    = "Map Positions count";

  private StructHexViewer hexViewer;

  public VirtualMap() throws Exception {
    super(null, VIRTUAL_MAP, StreamUtils.getByteBuffer(createBufferData()), 0);
  }

  public VirtualMap(ResourceEntry entry) throws Exception {
    super(entry);
  }

  /** Returns the number of available {@link VirtualPosition} structures for this map instance. */
  public int getPositionCount() {
    final StructEntry entry = getAttribute(VIRTUAL_MAP_NUM_POSITIONS);
    if (entry instanceof IsNumeric) {
      return ((IsNumeric)entry).getValue();
    }
    return 0;
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    SectionOffset soPositions = new SectionOffset(buffer, offset + 8, VIRTUAL_MAP_OFFSET_POSITIONS, VirtualPosition.class);
    addField(soPositions);
    SectionCount scPositions = new SectionCount(buffer, offset + 12, 4, VIRTUAL_MAP_NUM_POSITIONS, VirtualPosition.class);
    addField(scPositions);

    offset = soPositions.getValue();
    for (int i = 0; i < scPositions.getValue(); i++) {
      final VirtualPosition mp = new VirtualPosition(this, buffer, offset, i);
      offset = mp.getEndOffset();
      addField(mp);
    }

    return offset;
  }

  // --------------------- Begin Interface HasChildStructs ---------------------

  @Override
  public AddRemovable[] getPrototypes() throws Exception {
    return new AddRemovable[] { new VirtualPosition() };
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception {
    return entry;
  }

  // --------------------- Begin Interface HasChildStructs ---------------------

  // --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount() {
    return 1;
  }

  @Override
  public String getViewerTabName(int index) {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index) {
    if (hexViewer == null) {
      hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index) {
    return false;
  }

  // --------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer) {
    final ButtonPanel panel = viewer.getButtonPanel();
    panel.removeControl(panel.getControlByType(ButtonPanel.Control.FIND_MENU));
    panel.removeControl(panel.getControlByType(ButtonPanel.Control.SYNC_VIEW));

    viewer.addTabChangeListener(hexViewer);
  }

  /** Used internally to create an empty VirtualMap byte array. */
  private static byte[] createBufferData() {
    final byte[] buffer = {
        // "VMAP", "V1.0", (int)0x10, (int)0x00
        0x56, 0x4d, 0x41, 0x50, 0x56, 0x31, 0x2e, 0x30, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    return buffer;
  }
}