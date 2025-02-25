// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.infinity.resource.wmp.viewer.ViewerMap;
import org.tinylog.Logger;

public class MapEntry extends AbstractStruct implements HasViewerTabs {
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

  private static final String[] FLAGS_ARRAY = { "No flags set", "Colored icon", "Ignore palette" };

  private List<AreaEntry> areaCache;

  public MapEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, WMP_MAP + " " + nr, buffer, offset);
  }

  // --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount() {
    return 1;
  }

  @Override
  public String getViewerTabName(int index) {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index) {
    try {
      return new ViewerMap(this);
    } catch (Exception e) {
      Logger.error(e);
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index) {
    return true;
  }

  // --------------------- End Interface HasViewerTabs ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new ResourceRef(buffer, offset, WMP_MAP_RESREF, "MOS"));
    addField(new DecNumber(buffer, offset + 8, 4, WMP_MAP_WIDTH));
    addField(new DecNumber(buffer, offset + 12, 4, WMP_MAP_HEIGHT));
    addField(new DecNumber(buffer, offset + 16, 4, WMP_MAP_ID));
    addField(new StringRef(buffer, offset + 20, WMP_MAP_NAME));
    addField(new DecNumber(buffer, offset + 24, 4, WMP_MAP_CENTER_X));
    addField(new DecNumber(buffer, offset + 28, 4, WMP_MAP_CENTER_Y));
    SectionCount areaCount = new SectionCount(buffer, offset + 32, 4, WMP_MAP_NUM_AREAS, AreaEntry.class);
    addField(areaCount);
    SectionOffset areaOffset = new SectionOffset(buffer, offset + 36, WMP_MAP_OFFSET_AREAS, AreaEntry.class);
    addField(areaOffset);
    SectionOffset linkOffset = new SectionOffset(buffer, offset + 40, WMP_MAP_OFFSET_AREA_LINKS, AreaLink.class);
    addField(linkOffset);
    SectionCount linkCount = new SectionCount(buffer, offset + 44, 4, WMP_MAP_NUM_AREA_LINKS, AreaLink.class);
    addField(linkCount);
    addField(new ResourceRef(buffer, offset + 48, WMP_MAP_ICONS, "BAM"));
    if (Profile.isEnhancedEdition()) {
      addField(new Flag(buffer, offset + 56, 4, "Flags", FLAGS_ARRAY));
      addField(new Unknown(buffer, offset + 60, 124));
    } else {
      addField(new Unknown(buffer, offset + 56, 128));
    }

    int curOfs = areaOffset.getValue();
    for (int i = 0; i < areaCount.getValue(); i++) {
      AreaEntry areaEntry = new AreaEntry(this, buffer, curOfs, i);
      curOfs = areaEntry.getEndOffset();
      addField(areaEntry);
      addCachedArea(areaEntry);
      areaEntry.readLinks(buffer, linkOffset);
    }

    return offset + 128 + 56;
  }

  /** Provides quick read access to available {@link AreaEntry} instances. */
  public List<AreaEntry> getCachedAreas() {
    ensureCachedArea();
    return Collections.unmodifiableList(areaCache);
  }

  private void addCachedArea(AreaEntry areaEntry) {
    ensureCachedArea();
    if (areaEntry != null) {
      areaCache.add(areaEntry);
    }
  }

  private void ensureCachedArea() {
    if (areaCache == null) {
      areaCache = new ArrayList<>();
    }
  }
}
