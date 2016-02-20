// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;

import javax.swing.JComponent;

final class MapEntry extends AbstractStruct implements HasViewerTabs
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

  MapEntry(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
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
  public int read(byte buffer[], int offset) throws Exception
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
    addField(new Unknown(buffer, offset + 56, 128));

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

