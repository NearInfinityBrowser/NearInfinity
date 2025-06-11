// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import static org.infinity.resource.are.AreResource.ARE_NUM_ENTRANCES;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_ENTRANCES;

import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Entrance;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.tinylog.Logger;

/**
 * Manages entrance layer objects.
 */
public class LayerEntrance extends BasicLayer<LayerObjectEntranceBase, AreResource> {
  private static final String AVAILABLE_FMT = "Entrances: %d";

  public LayerEntrance(AreResource are, AreaViewer viewer) {
    super(are, ViewerConstants.LayerType.ENTRANCE, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer() {
    // loading entrances from ARE
    loadLayerItems(ARE_OFFSET_ENTRANCES, ARE_NUM_ENTRANCES, Entrance.class, e -> new LayerObjectEntrance(parent, e));

    // loading entrances from 2DA table
    final boolean tableSupported = Profile.getEngine() == Profile.Engine.BG2 || Profile.getEngine() == Profile.Engine.EE;
    if (tableSupported) {
      final Table2da table = Table2daCache.get("ENTRIES.2DA");
      if (table != null) {
        final List<LayerObjectEntranceBase> objectList = getLayerObjects();
        final String areaName = parent.getResourceEntry().getResourceRef();
        for (int row = 0; row < table.getRowCount(); row++) {
          final String label = table.get(row, 0);
          if (areaName.equalsIgnoreCase(label)) {
            for (int col = 1; col < table.getColCount(row); col++) {
              try {
                final LayerObjectTableEntrance obj = new LayerObjectTableEntrance(table, row, col);
                setListeners(obj);
                objectList.add(obj);
              } catch (UnsupportedOperationException e) {
                // ignored
              } catch (Exception e) {
                Logger.error(e);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public String getAvailability() {
    int cnt = getLayerObjectCount();
    return String.format(AVAILABLE_FMT, cnt);
  }
}
