// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/**
 * Parser for a single table-based worldmap area and associated travel links.
 */
public class VirtualAreaEntry extends VirtualStructEntry {
  // Cached row from XNEWAREA.2DA
  // Area entries are parsed ResourceEntry, String or Integer objects, depending on the column value
  private final Object[] cachedArea = new Object[15];

  // Cached rows from the link table of the area
  // Link entries are parsed ResourceEntry, String or Integer objects, depending on the column value
  // May contain "null" for undefined entries
  private final List<Object[]> cachedLinks = new ArrayList<>();

  private final VirtualMapEntry parent;
  private final int areaIndex;
  private final Table2da areaTable;

  public VirtualAreaEntry(VirtualMapEntry parentMap, int index) throws Exception {
    super(getAreaResourceEntry(parentMap, index));
    parent = parentMap;
    areaIndex = index;
    areaTable = Table2daCache.get(getResourceEntry());
    validate();
  }

  /** Returns the unprocessed parent map table with area definitions for the worldmap. */
  public VirtualMapEntry getParentMap() {
    return parent;
  }

  /** Returns the row index of the area definition in the parent map table. */
  public int getAreaIndex() {
    return areaIndex;
  }

  /** Returns the unprocessed 2DA table with link definitions for the current area. */
  public Table2da getArea() {
    return areaTable;
  }

  /** Returns the area resource linked to the worldmap location. */
  public ResourceEntry getAreaResource() {
    return (ResourceEntry)cachedArea[1];
  }

  /** Returns the script name of the area. */
  public String getAreaScript() {
    return (String)cachedArea[2];
  }

  /** Returns visibility flags of the worldmap location. */
  public int getAreaFlags() {
    return (Integer)cachedArea[3];
  }

  /** Returns the index of the worldmap icon. */
  public int getAreaIconIndex() {
    return (Integer)cachedArea[4];
  }

  /** Returns the x coordinate of the worldmap location. */
  public int getAreaLocationX() {
    return (Integer)cachedArea[5];
  }

  /** Returns the y coordinate of the worldmap location. */
  public int getAreaLocationY() {
    return (Integer)cachedArea[6];
  }

  /** Returns the strref of the location label. Returns -1 if not available. */
  public int getAreaLabelStrref() {
    return (Integer)cachedArea[7];
  }

  /** Returns the strref of the location name. Returns -1 if not available. */
  public int getAreaNameStrref() {
    return (Integer)cachedArea[8];
  }

  /** Returns the number of link definitions for travelling to a target area from the northern edge. */
  public int getNumAreaLinksNorth() {
    return (Integer)cachedArea[10];
  }

  /** Returns the first link definition index for travelling to a target area from the northern edge. */
  public int getFirstAreaLinkNorth() {
    return 0;
  }

  /** Returns the number of link definitions for travelling to a target area from the eastern edge. */
  public int getNumAreaLinksEast() {
    return (Integer)cachedArea[11];
  }

  /** Returns the first link definition index for travelling to a target area from the eastern edge. */
  public int getFirstAreaLinkEast() {
    return getNumAreaLinksNorth();
  }

  /** Returns the number of link definitions for travelling to a target area from the southern edge. */
  public int getNumAreaLinksSouth() {
    return (Integer)cachedArea[12];
  }

  /** Returns the first link definition index for travelling to a target area from the southern edge. */
  public int getFirstAreaLinkSouth() {
    return getNumAreaLinksNorth() + getNumAreaLinksEast();
  }

  /** Returns the number of link definitions for travelling to a target area from the western edge. */
  public int getNumAreaLinksWest() {
    return (Integer)cachedArea[13];
  }

  /** Returns the first link definition index for travelling to a target area from the western edge. */
  public int getFirstAreaLinkWest() {
    return getNumAreaLinksNorth() + getNumAreaLinksEast() + getNumAreaLinksSouth();
  }

  /** Returns the number of link definitions for travelling towards the current area. */
  public int getNumAreaLinksTo() {
    return (Integer)cachedArea[14];
  }

  /** Returns the first link definition index for travelling towards the current area. */
  public int getFirstAreaLinkTo() {
    return getNumAreaLinksNorth() + getNumAreaLinksEast() + getNumAreaLinksSouth() + getNumAreaLinksWest();
  }

  /**
   * Returns the target or source area for the specified link definition.
   *
   * @param index Travel link index.
   * @return Target or source area depending on the travel direction, as {@link ResourceEntry} instance.
   */
  public ResourceEntry getLinkArea(int index) {
    return (ResourceEntry)cachedLinks.get(index)[1];
  }

  /**
   * Returns the location flags for the specified link definition.
   *
   * @param index Travel link index.
   * @return Location flags.
   */
  public int getLinkFlags(int index) {
    return (Integer)cachedLinks.get(index)[2];
  }

  /**
   * Returns the entry point name for the specified link definition.
   *
   * @param index Travel link index.
   * @return Entry point name.
   */
  public String getLinkEntryPoint(int index) {
    return (String)cachedLinks.get(index)[3];
  }

  /**
   * Returns the distance in units of 4 hours for the specified link definition.
   *
   * @param index Travel link index.
   * @return Distance between source and target location, in 4 hours units.
   */
  public int getLinkDistanceScale(int index) {
    return (Integer)cachedLinks.get(index)[4];
  }

  /**
   * Returns the encounter probability for the specified link definition.
   *
   * @param index Travel link index.
   * @return Probability in percent.
   */
  public int getLinkEncounterProbability(int index) {
    return (Integer)cachedLinks.get(index)[5];
  }

  /**
   * Returns the encounter area for the specified link definition.
   *
   * @param index Travel link index.
   * @param num   The encounter area index in range 0 to 4.
   * @return {@link ResourceEntry} of the encounter area if available, {@code null} otherwise.
   */
  public ResourceEntry getLinkEncounter(int index, int num) {
    switch (num) {
      case 0:
        return (ResourceEntry)cachedLinks.get(index)[6];
      case 1:
        return (ResourceEntry)cachedLinks.get(index)[7];
      case 2:
        return (ResourceEntry)cachedLinks.get(index)[8];
      case 3:
        return (ResourceEntry)cachedLinks.get(index)[9];
      case 4:
        return (ResourceEntry)cachedLinks.get(index)[10];
    }
    return null;
  }

  /**
   * Returns the default edge of the entrance for the specified link definition.
   *
   * @param index Travel link index.
   * @return Entrance type for links towards the source location: 0=north, 1=east, 2=south, 3=west). Returns -1 for
   *         links towards the target location.
   */
  public int getLinkEntrance(int index) {
    final Object retVal = cachedLinks.get(index)[11];
    if (retVal instanceof Integer) {
      return (Integer)retVal;
    }
    return -1;
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, areaIndex);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj instanceof VirtualAreaEntry) {
      final VirtualAreaEntry vae = (VirtualAreaEntry)obj;
      return areaIndex == vae.areaIndex && Objects.equals(parent, vae.parent);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "VirtualAreaEntry [areaTable=" + areaTable + ", areaIndex=" + areaIndex + ", parent=" + parent + "]";
  }

  /** Validates and caches the table content. */
  private void validate() throws Exception {
    // caching area definitions
    cachedArea[0] = Misc.toNumber(parent.getMap().get(areaIndex, 0), areaIndex + 1);
    cachedArea[1] = ResourceFactory.getResourceEntry(parent.getMap().get(areaIndex, 1) + ".ARE");
    cachedArea[2] = parent.getMap().get(areaIndex, 2);
    cachedArea[3] = Misc.toNumber(parent.getMap().get(areaIndex, 3), 0);
    cachedArea[4] = Misc.toNumber(parent.getMap().get(areaIndex, 4), -1);
    cachedArea[5] = Misc.toNumber(parent.getMap().get(areaIndex, 5), -1);
    cachedArea[6] = Misc.toNumber(parent.getMap().get(areaIndex, 6), -1);
    cachedArea[7] = Misc.toNumber(parent.getMap().get(areaIndex, 7), -1);
    cachedArea[8] = Misc.toNumber(parent.getMap().get(areaIndex, 8), -1);
    cachedArea[9] = parent.getMap().get(areaIndex, 9);
    cachedArea[10] = Misc.toNumber(parent.getMap().get(areaIndex, 10), 0);
    cachedArea[11] = Misc.toNumber(parent.getMap().get(areaIndex, 11), 0);
    cachedArea[12] = Misc.toNumber(parent.getMap().get(areaIndex, 12), 0);
    cachedArea[13] = Misc.toNumber(parent.getMap().get(areaIndex, 13), 0);
    cachedArea[14] = Misc.toNumber(parent.getMap().get(areaIndex, 14), 0);
    final int numLinks = (Integer)cachedArea[10] + (Integer)cachedArea[11] + (Integer)cachedArea[12]
        + (Integer)cachedArea[13] + (Integer)cachedArea[14];
    if (areaTable.getRowCount() < numLinks) {
      throw new Exception("Unexpected number of links (expected: " + numLinks + ", found: " + areaTable.getRowCount() + ")");
    }

    // validating and caching link definitions
    final int colCount = 12;
    final String defValue = areaTable.getDefaultValue();
    for (int row = 0; row < numLinks; row++) {
      if (areaTable.getColCount(row) < colCount) {
        throw new Exception("Incomplete link definition at row " + row);
      }

      final Object[] cachedRow = new Object[colCount];
      for (int col = 0; col < colCount; col++) {
        final String value = areaTable.get(row, col);
        switch (col) {
          case 0:
          case 2:
          case 4:
          case 5:
            // valid number required
            cachedRow[col] = Integer.parseInt(value);
            break;
          case 1:
          {
            // validate ARE resref
            final ResourceEntry entry = ResourceFactory.getResourceEntry(value + ".ARE");
            if (entry != null) {
              cachedRow[col] = entry;
            } else {
              throw new Exception("Resource does not exist: " + value);
            }
            break;
          }
          case 3:
            // any string
            if (value.length() <= 32) {
              cachedRow[col] = value;
            } else {
              throw new Exception("Row=3: string is too long (expected: length <= 32, found: length = " + value.length() + ")");
            }
            break;
          case 6:
          case 7:
          case 8:
          case 9:
          case 10:
            // random encounter area
            if (!defValue.equals(value)) {
              cachedRow[col] = ResourceFactory.getResourceEntry(value + ".ARE");
            }
            break;
          default:
            // number or default value
            if (!defValue.equals(value)) {
              final int number = Integer.parseInt(value);
              cachedRow[col] = number;
            }
        }
      }
      cachedLinks.add(cachedRow);
    }
  }

  private static ResourceEntry getAreaResourceEntry(VirtualMapEntry map, int index) throws Exception {
    if (map != null && index >= 0 && index < map.getAreaCount()) {
      final String linkName = map.getMap().get(index, 9);
      if (!map.getMap().getDefaultValue().equals(linkName)) {
        return ResourceFactory.getResourceEntry(linkName + ".2DA");
      }
    }
    throw new Exception("Area entry does not exist at index=" + index);
  }
}