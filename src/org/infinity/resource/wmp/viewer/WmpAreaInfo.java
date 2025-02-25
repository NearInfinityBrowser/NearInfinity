// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.IsTextual;
import org.infinity.resource.Closeable;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wmp.AreaEntry;
import org.infinity.resource.wmp.AreaLink;
import org.infinity.resource.wmp.AreaLinkEast;
import org.infinity.resource.wmp.AreaLinkNorth;
import org.infinity.resource.wmp.AreaLinkSouth;
import org.infinity.resource.wmp.AreaLinkWest;
import org.infinity.resource.wmp.viewer.ViewerMap.Direction;

public class WmpAreaInfo implements Closeable {
  private final List<WmpLinkInfo> linksList = new ArrayList<>();

  private final WmpMapInfo mapInfo;
  private final AreaEntry areaEntry;
  private final VirtualAreaEntry virtualAreaEntry;

  private ResourceEntry currentArea;
  private String originalArea;
  private String scriptName;
  private int flags;
  private int iconIndex;
  private int locationX;
  private int locationY;
  private int nameStrref;
  private int tooltipStrref;
  private String loadingImage;

  public WmpAreaInfo(WmpMapInfo parent, AreaEntry areaEntry) throws Exception {
    this.mapInfo = Objects.requireNonNull(parent);
    this.areaEntry = Objects.requireNonNull(areaEntry);
    this.virtualAreaEntry = null;
    init();
  }

  public WmpAreaInfo(WmpMapInfo parent, VirtualAreaEntry areaEntry) throws Exception {
    this.mapInfo = Objects.requireNonNull(parent);
    this.virtualAreaEntry = areaEntry;
    this.areaEntry = null;
    init();
  }

  @Override
  public void close() throws Exception {
    while (!linksList.isEmpty()) {
      linksList.remove(linksList.size() - 1).close();
    }
  }

  /** Discards existing and reinitializes new map data. */
  public void reset() throws Exception {
    close();
    init();
  }

  public WmpMapInfo getParent() {
    return mapInfo;
  }

  /** Returns the index of this area in the area list of the parent map structure. */
  public int getAreaIndex() {
    for (int i = 0, count = mapInfo.getAreaList().size(); i < count; i++) {
      if (mapInfo.getAreaList().get(i) == this) {
        return i;
      }
    }
    return mapInfo.getAreaList().size();
  }

  public AreaEntry getAreaEntry() {
    return areaEntry;
  }

  public VirtualAreaEntry getVirtualAreaEntry() {
    return virtualAreaEntry;
  }

  public ResourceEntry getCurrentArea() {
    return currentArea;
  }

  public String getOriginalArea() {
    return originalArea;
  }

  public String getScriptName() {
    return scriptName;
  }

  public int getFlags() {
    return flags;
  }

  public int getIconIndex() {
    return iconIndex;
  }

  public int getLocationX() {
    return locationX;
  }

  public int getLocationY() {
    return locationY;
  }

  public int getAreaNameStrref() {
    return nameStrref;
  }

  public int getAreaTooltipStrref() {
    return tooltipStrref;
  }

  public String getLoadImage() {
    return loadingImage;
  }

  public List<WmpLinkInfo> getLinksList() {
    return Collections.unmodifiableList(linksList);
  }

  @Override
  public String toString() {
    return "WmpAreaInfo [currentArea=" + currentArea + ", originalArea=" + originalArea + ", scriptName=" + scriptName
        + ", flags=" + flags + ", iconIndex=" + iconIndex + ", locationX=" + locationX + ", locationY=" + locationY
        + ", nameStrref=" + nameStrref + ", tooltipStrref=" + tooltipStrref + ", loadingImage=" + loadingImage
        + ", areaEntry=" + areaEntry + ", virtualAreaEntry=" + virtualAreaEntry + "]";
  }

  private void init() throws Exception {
    if (areaEntry != null) {
      initStruct();
    } else if (virtualAreaEntry != null) {
      initTable();
    } else {
      throw new Exception("No area definitions available");
    }
  }

  private void initStruct() throws Exception {
    final String resName = ((IsReference)areaEntry.getAttribute(AreaEntry.WMP_AREA_CURRENT)).getResourceName();
    currentArea = ResourceFactory.getResourceEntry(resName);
    originalArea = ((IsTextual)areaEntry.getAttribute(AreaEntry.WMP_AREA_ORIGINAL)).getText();
    scriptName = ((IsTextual)areaEntry.getAttribute(AreaEntry.WMP_AREA_SCRIPT_NAME)).getText();
    flags = ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_FLAGS)).getValue();
    iconIndex = ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_ICON_INDEX)).getValue();
    locationX = ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
    locationY = ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
    nameStrref = ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_NAME)).getValue();
    tooltipStrref = ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_TOOLTIP)).getValue();
    loadingImage = ((IsTextual)areaEntry.getAttribute(AreaEntry.WMP_AREA_LOADING_IMAGE)).getText();

    for (final Direction dir : Direction.values()) {
      Class<? extends StructEntry> clsLink = null;
      switch (dir) {
        case NORTH:
          clsLink = AreaLinkNorth.class;
          break;
        case WEST:
          clsLink = AreaLinkWest.class;
          break;
        case SOUTH:
          clsLink = AreaLinkSouth.class;
          break;
        case EAST:
          clsLink = AreaLinkEast.class;
          break;
        default:
      }
      for (final StructEntry se : areaEntry.getFields(clsLink)) {
        if (se instanceof AreaLink) {
          final WmpLinkInfo wli = new WmpLinkInfo(this, dir, (AreaLink)se);
          linksList.add(wli);
        }
      }
    }
  }

  private void initTable() throws Exception {
    currentArea = virtualAreaEntry.getAreaResource();
    originalArea = currentArea.getResourceRef();
    scriptName = virtualAreaEntry.getAreaScript();
    flags = virtualAreaEntry.getAreaFlags();
    iconIndex = virtualAreaEntry.getAreaIconIndex();
    locationX = virtualAreaEntry.getAreaLocationX();
    locationY = virtualAreaEntry.getAreaLocationY();
    nameStrref = virtualAreaEntry.getAreaLabelStrref();
    tooltipStrref = virtualAreaEntry.getAreaNameStrref();

    // initializing travel links
    int startIndex = 0;
    for (final Direction dir : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
      int linksCount = 0;
      switch (dir) {
        case NORTH:
          linksCount = virtualAreaEntry.getNumAreaLinksNorth();
          break;
        case EAST:
          linksCount = virtualAreaEntry.getNumAreaLinksEast();
          break;
        case SOUTH:
          linksCount = virtualAreaEntry.getNumAreaLinksSouth();
          break;
        case WEST:
          linksCount = virtualAreaEntry.getNumAreaLinksWest();
          break;
        default:
      }

      for (int i = 0; i < linksCount; i++) {
        final WmpLinkInfo wli = new WmpLinkInfo(this, dir, virtualAreaEntry, startIndex + i);
        linksList.add(wli);
      }

      startIndex += linksCount;
    }

    // initializing return links
    final int linksTo = virtualAreaEntry.getNumAreaLinksTo();
    final List<WmpAreaInfo> areas = mapInfo.getAreaList();
    for (int i = 0; i < linksTo; i++) {
      final int rowIdx = startIndex + i;

      final String srcArea = virtualAreaEntry.getLinkArea(rowIdx).getResourceRef();
      final WmpAreaInfo srcAreaInfo = areas
          .stream()
          .filter(wai -> srcArea.equalsIgnoreCase(wai.currentArea.getResourceRef()))
          .findFirst()
          .orElse(null);
      if (srcAreaInfo != null) {
        final int dirValueTo = virtualAreaEntry.getLinkEntrance(rowIdx);
        Direction dirTo = null;
        switch (dirValueTo) {
          case 0:
            dirTo = Direction.NORTH;
            break;
          case 1:
            dirTo = Direction.EAST;
            break;
          case 2:
            dirTo = Direction.SOUTH;
            break;
          case 3:
            dirTo = Direction.WEST;
            break;
          default:
        }

        if (dirTo != null) {
          final WmpLinkInfo wli = new WmpLinkInfo(srcAreaInfo, dirTo, virtualAreaEntry, rowIdx);
          srcAreaInfo.linksList.add(wli);
        }
      }
    }
  }
}