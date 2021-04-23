// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vef;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
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

/**
 * Visual effects allow you to group other visual effects, animations, and sounds
 * together and specify the timing with which each component plays. This is powerful
 * in the sense that you can create a whole visual effect display with these files.
 * VEF files can be used with the actions {@code CreateVisualEffect(S:Object*,P:Location*)}
 * and {@code CreateVisualEffectObject(S:Object*,O:Target*)}, and in effect opcode
 * 215 (Graphics: Play 3D Effect).
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/vef_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/vef_v1.htm</a>
 */
public final class VefResource extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs
{
  // VEF-specific field labels
  public static final String VEF_OFFSET_COMPONENT_PRI = "Primary component offset";
  public static final String VEF_NUM_COMPONENT_PRI    = "Primary component count";
  public static final String VEF_OFFSET_COMPONENT_SEC = "Secondary component offset";
  public static final String VEF_NUM_COMPONENT_SEC    = "Secondary component count";

  private StructHexViewer hexViewer;

  public VefResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    return new AddRemovable[]{new PrimaryComponent(), new SecondaryComponent()};
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
      hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
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
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    SectionOffset offset_component1 = new SectionOffset(buffer, offset + 8, VEF_OFFSET_COMPONENT_PRI,
                                                        PrimaryComponent.class);
    addField(offset_component1);
    SectionCount count_component1 = new SectionCount(buffer, offset + 12, 4, VEF_NUM_COMPONENT_PRI,
                                                     PrimaryComponent.class);
    addField(count_component1);
    SectionOffset offset_component2 = new SectionOffset(buffer, offset + 16, VEF_OFFSET_COMPONENT_SEC,
                                                        SecondaryComponent.class);
    addField(offset_component2);
    SectionCount count_component2 = new SectionCount(buffer, offset + 20, 4, VEF_NUM_COMPONENT_SEC,
                                                     SecondaryComponent.class);
    addField(count_component2);

    offset = offset_component1.getValue();
    for (int i = 0; i < count_component1.getValue(); i++) {
      PrimaryComponent comp1 = new PrimaryComponent(this, buffer, offset, i);
      offset = comp1.getEndOffset();
      addField(comp1);
    }

    offset = offset_component2.getValue();
    for (int i = 0; i < count_component2.getValue(); i++) {
      SecondaryComponent comp2 = new SecondaryComponent(this, buffer, offset, i);
      offset = comp2.getEndOffset();
      addField(comp2);
    }

    int endoffset = offset;
    for (final StructEntry entry : getFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
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
