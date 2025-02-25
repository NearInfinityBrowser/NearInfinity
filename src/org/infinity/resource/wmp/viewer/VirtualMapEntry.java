// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.util.HashMap;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/**
 * Parser for table-based worldmap definitions.
 */
public class VirtualMapEntry extends VirtualStructEntry {
  private final HashMap<Integer, VirtualAreaEntry> areaCache = new HashMap<>();

  private final Table2da mapTable;

  public VirtualMapEntry(ResourceEntry wmpResource) throws Exception {
    super(getMapResourceEntry(wmpResource));
    mapTable = Table2daCache.get(getResourceEntry());
    validate();
  }

  public Table2da getMap() {
    return mapTable;
  }

  public int getAreaCount() {
    return mapTable.getRowCount();
  }

  public VirtualAreaEntry getAreaEntry(int index) throws IndexOutOfBoundsException {
    if (index < 0 || index >= getAreaCount()) {
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);
    }

    return areaCache.computeIfAbsent(index, idx -> {
      VirtualAreaEntry retVal = null;
      try {
        retVal = new VirtualAreaEntry(this, idx);
      } catch (Exception e) {
      }
      return retVal;
    });
  }

  @Override
  public String toString() {
    return "VirtualMapEntry [mapTable=" + mapTable + "]";
  }

  /** Validates the table content. */
  private void validate() throws Exception {
    final int colCount = 15;
    for (int row = 0, rowCount = mapTable.getRowCount(); row < rowCount; row++) {
      if (mapTable.getColCount(row) < colCount) {
        throw new Exception("Incomplete definition at row " + row);
      }

      for (int col = 0; col < colCount; col++) {
        final String value = mapTable.get(row, col);
        switch (col) {
          case 1:
            // validate ARE resref
            if (!ResourceFactory.resourceExists(value + ".ARE")) {
              throw new Exception("Resource does not exist: " + value);
            }
            break;
          case 2:
            // ignored
            break;
          case 9:
            // validate 2DA resref
            if (!ResourceFactory.resourceExists(value + ".2DA")) {
              throw new Exception("Resource does not exist: " + value);
            }
            break;
          default:
            // validate number
            Integer.parseInt(value);
        }
      }
    }
  }

  /**
   * Returns the worldmap definition table if available.
   * <p>
   * Note: Map definitions are only available for the default worldmap on BG2:SoA.
   * </p>
   *
   * @param wmpResource The worldmap resource.
   * @return {@link ResourceEntry} instance of the map definition table if applicable, {@code null} otherwise.
   */
  private static ResourceEntry getMapResourceEntry(ResourceEntry wmpResource) {
    ResourceEntry retVal = null;
    if ("WORLDMAP.WMP".equalsIgnoreCase(wmpResource.getResourceName())) {
      switch (Profile.getGame()) {
        case BG2ToB:
        case BG2EE:
        case BGT:
        case EET:
          retVal = ResourceFactory.getResourceEntry("XNEWAREA.2DA");
          break;
        default:
      }
    }
    return retVal;
  }
}