// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import static org.infinity.resource.are.AreResource.ARE_NUM_CONTAINERS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_CONTAINERS;

import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Container;

/**
 * Manages container layer objects.
 */
public class LayerContainer extends BasicCompositeLayer<LayerObjectContainer, AreResource> {
  /** Identifier for the target icons sublayer. */
  public static final int LAYER_ICONS_TARGET = 1;

  private static final String AVAILABLE_FMT = "Containers: %d";

  public LayerContainer(AreResource are, AreaViewer viewer) {
    super(are, ViewerConstants.LayerType.CONTAINER, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer() {
    loadLayerItems(ARE_OFFSET_CONTAINERS, ARE_NUM_CONTAINERS, Container.class, c -> new LayerObjectContainer(parent, c));
  }

  @Override
  public String getAvailability() {
    int cnt = getLayerObjectCount();
    return String.format(AVAILABLE_FMT, cnt);
  }
}
