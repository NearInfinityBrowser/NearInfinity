// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

/**
 * Manages virtual position objects (pins).
 */
public class LayerVirtualPosition extends BasicLayer<LayerObjectVirtualPosition, VirtualMap> {
  private static final String AVAILABLE_FMT = "Pins: %d";

  public LayerVirtualPosition(VirtualMap map, AreaViewer viewer) {
    super(map, ViewerConstants.LayerType.VIRTUAL_POSITION, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer() {
    loadLayerItems(VirtualMap.VIRTUAL_MAP_OFFSET_POSITIONS, VirtualMap.VIRTUAL_MAP_NUM_POSITIONS, VirtualPosition.class,
        p -> new LayerObjectVirtualPosition(parent, p));
  }

  @Override
  public String getAvailability() {
    int cnt = getLayerObjectCount();
    return String.format(AVAILABLE_FMT, cnt);
  }
}
