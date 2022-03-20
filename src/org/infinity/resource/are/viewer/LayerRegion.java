// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import static org.infinity.resource.are.AreResource.ARE_NUM_TRIGGERS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_TRIGGERS;

import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.ITEPoint;

/**
 * Manages region layer objects.
 */
public class LayerRegion extends BasicLayer<LayerObjectRegion, AreResource> {
  private static final String AVAILABLE_FMT = "Regions: %d";

  public LayerRegion(AreResource are, AreaViewer viewer) {
    super(are, ViewerConstants.LayerType.REGION, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer() {
    loadLayerItems(ARE_OFFSET_TRIGGERS, ARE_NUM_TRIGGERS, ITEPoint.class, p -> new LayerObjectRegion(parent, p));
  }

  @Override
  public String getAvailability() {
    int cnt = getLayerObjectCount();
    return String.format(AVAILABLE_FMT, cnt);
  }
}
