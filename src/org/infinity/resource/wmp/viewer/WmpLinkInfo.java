// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.resource.Closeable;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wmp.AreaLink;
import org.infinity.resource.wmp.viewer.ViewerMap.Direction;

public class WmpLinkInfo implements Closeable {
  private final List<ResourceEntry> randomEncounterAreaList = new ArrayList<>();

  private final WmpAreaInfo areaInfo;
  private final Direction dir;
  private final AreaLink link;
  private final VirtualAreaEntry virtualAreaEntry;
  private final int virtualLinkIndex;

  private int targetAreaIndex;
  private String targetEntrance;
  private int distanceScale;
  private int defaultEntrance;
  private int randomEncounterProbability;

  public WmpLinkInfo(WmpAreaInfo parent, Direction dir, AreaLink link) throws Exception {
    this.areaInfo = Objects.requireNonNull(parent);
    this.dir = Objects.requireNonNull(dir);
    this.link = Objects.requireNonNull(link);
    this.virtualAreaEntry = null;
    this.virtualLinkIndex = -1;
    init();
  }

  public WmpLinkInfo(WmpAreaInfo parent, Direction dir, VirtualAreaEntry areaEntry, int linkIndex) throws Exception {
    this.areaInfo = Objects.requireNonNull(parent);
    this.dir = Objects.requireNonNull(dir);
    this.virtualAreaEntry = Objects.requireNonNull(areaEntry);
    this.virtualLinkIndex = linkIndex;
    this.link = null;
    init();
  }

  @Override
  public void close() throws Exception {
    randomEncounterAreaList.clear();
  }

  /** Discards existing and reinitializes new map data. */
  public void reset() throws Exception {
    close();
    init();
  }

  public WmpAreaInfo getParent() {
    return areaInfo;
  }

  public Direction getDirection() {
    return dir;
  }

  public AreaLink getLink() {
    return link;
  }

  public int getTargetAreaIndex() {
    return targetAreaIndex;
  }

  public String getTargetEntrance() {
    return targetEntrance;
  }

  public int getDistanceScale() {
    return distanceScale;
  }

  public int getDefaultEntrance() {
    return defaultEntrance;
  }

  public int getRandomEncounterProbability() {
    return randomEncounterProbability;
  }

  public int getRandomEncounterAreaCount() {
    return randomEncounterAreaList.size();
  }

  public ResourceEntry getRandomEncounterArea(int index) throws IndexOutOfBoundsException {
    return randomEncounterAreaList.get(index);
  }

  @Override
  public String toString() {
    return "WmpLinkInfo [dir=" + dir + ", link=" + link + ", virtualAreaEntry=" + virtualAreaEntry
        + ", virtualLinkIndex=" + virtualLinkIndex + ", targetAreaIndex=" + targetAreaIndex + ", targetEntrance="
        + targetEntrance + ", distanceScale=" + distanceScale + ", defaultEntrance=" + defaultEntrance
        + ", randomEncounterProbability=" + randomEncounterProbability + ", randomEncounterAreaList="
        + randomEncounterAreaList + "]";
  }

  private void init() throws Exception {
    if (link != null) {
      initStruct();
    } else if (virtualAreaEntry != null) {
      initTable();
    } else {
      throw new Exception("No link definitions available");
    }
  }

  private void initStruct() throws Exception {
    targetAreaIndex = ((IsNumeric)link.getAttribute(AreaLink.WMP_LINK_TARGET_AREA)).getValue();
    targetEntrance = ((IsTextual)link.getAttribute(AreaLink.WMP_LINK_TARGET_ENTRANCE)).getText();
    distanceScale = ((IsNumeric)link.getAttribute(AreaLink.WMP_LINK_DISTANCE_SCALE)).getValue();
    defaultEntrance = ((IsNumeric)link.getAttribute(AreaLink.WMP_LINK_DEFAULT_ENTRANCE)).getValue();

    randomEncounterProbability =
        ((IsNumeric)link.getAttribute(AreaLink.WMP_LINK_RANDOM_ENCOUNTER_PROBABILITY)).getValue();
    for (int i = 0; i < 5; i++) {
      final String resref =
          ((IsTextual)link.getAttribute(String.format(AreaLink.WMP_LINK_RANDOM_ENCOUNTER_AREA_FMT, i + 1))).getText();
      if (!resref.isEmpty()) {
        final ResourceEntry entry = ResourceFactory.getResourceEntry(resref + ".ARE");
        if (entry != null) {
          randomEncounterAreaList.add(entry);
        }
      }
    }
  }

  private void initTable() throws Exception {
    final boolean isLinkTo = virtualLinkIndex >= virtualAreaEntry.getFirstAreaLinkTo();

    final ResourceEntry areaResource =
        isLinkTo ? virtualAreaEntry.getAreaResource() : virtualAreaEntry.getLinkArea(virtualLinkIndex);
    int areaIndex = getParent().getParent().indexOfArea(areaResource.getResourceRef());
    if (areaIndex < 0) {
      // about to be added later
      areaIndex = getParent().getParent().getAreaList().size();
    }
    targetAreaIndex = areaIndex;
    targetEntrance = virtualAreaEntry.getLinkEntryPoint(virtualLinkIndex);
    distanceScale = virtualAreaEntry.getLinkDistanceScale(virtualLinkIndex);
    defaultEntrance = virtualAreaEntry.getLinkFlags(virtualLinkIndex);

    randomEncounterProbability = virtualAreaEntry.getLinkEncounterProbability(virtualLinkIndex);
    for (int i = 0; i < 5; i++) {
      final ResourceEntry entry = virtualAreaEntry.getLinkEncounter(virtualLinkIndex, 0);
      if (entry != null) {
        randomEncounterAreaList.add(entry);
      }
    }
  }
}