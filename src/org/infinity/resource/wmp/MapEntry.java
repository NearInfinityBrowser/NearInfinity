// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.Unknown;
import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;

final public class MapEntry extends AbstractStruct implements HasViewerTabs
{
  // WMP/MapEntry-specific field labels
  public static final String WMP_MAP                    = "Map";
  public static final String WMP_MAP_RESREF             = "Map";
  public static final String WMP_MAP_WIDTH              = "Width";
  public static final String WMP_MAP_HEIGHT             = "Height";
  public static final String WMP_MAP_ID                 = "Map ID";
  public static final String WMP_MAP_NAME               = "Name";
  public static final String WMP_MAP_CENTER_X           = "Center location: X";
  public static final String WMP_MAP_CENTER_Y           = "Center location: Y";
  public static final String WMP_MAP_NUM_AREAS          = "# areas";
  public static final String WMP_MAP_OFFSET_AREAS       = "Areas offset";
  public static final String WMP_MAP_OFFSET_AREA_LINKS  = "Area links offset";
  public static final String WMP_MAP_NUM_AREA_LINKS     = "# area links";
  public static final String WMP_MAP_ICONS              = "Map icons";

  private static final String[] s_flag = {"No flags set", "Colored icon", "Ignore palette"};

  MapEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, WMP_MAP + " " + nr, buffer, offset);
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
    return new ViewerMap(this);
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new ResourceRef(buffer, offset, WMP_MAP_RESREF, "MOS"));
    addField(new DecNumber(buffer, offset + 8, 4, WMP_MAP_WIDTH));
    addField(new DecNumber(buffer, offset + 12, 4, WMP_MAP_HEIGHT));
    addField(new DecNumber(buffer, offset + 16, 4, WMP_MAP_ID));
    addField(new StringRef(buffer, offset + 20, WMP_MAP_NAME));
    addField(new DecNumber(buffer, offset + 24, 4, WMP_MAP_CENTER_X));
    addField(new DecNumber(buffer, offset + 28, 4, WMP_MAP_CENTER_Y));
    SectionCount area_count = new SectionCount(buffer, offset + 32, 4, WMP_MAP_NUM_AREAS, AreaEntry.class);
    addField(area_count);
    SectionOffset area_offset = new SectionOffset(buffer, offset + 36, WMP_MAP_OFFSET_AREAS, AreaEntry.class);
    addField(area_offset);
    SectionOffset link_offset = new SectionOffset(buffer, offset + 40, WMP_MAP_OFFSET_AREA_LINKS, AreaLink.class);
    addField(link_offset);
    SectionCount link_count = new SectionCount(buffer, offset + 44, 4, WMP_MAP_NUM_AREA_LINKS, AreaLink.class);
    addField(link_count);
    addField(new ResourceRef(buffer, offset + 48, WMP_MAP_ICONS, "BAM"));
    if (Profile.isEnhancedEdition()) {
      addField(new Flag(buffer, offset + 56, 4, "Flags", s_flag));
      addField(new Unknown(buffer, offset + 60, 124));
    } else {
      addField(new Unknown(buffer, offset + 56, 128));
    }

    int curOfs = area_offset.getValue();
    for (int i = 0; i < area_count.getValue(); i++) {
      AreaEntry areaEntry = new AreaEntry(this, buffer, curOfs, i);
      curOfs = areaEntry.getEndOffset();
      addField(areaEntry);
      areaEntry.readLinks(buffer, link_offset);
    }

    return offset + 128 + 56;
  }
}

