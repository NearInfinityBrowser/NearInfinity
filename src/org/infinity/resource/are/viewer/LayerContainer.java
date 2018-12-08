// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_CONTAINERS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_CONTAINERS;
import org.infinity.resource.are.Container;

/**
 * Manages container layer objects.
 */
public class LayerContainer extends BasicLayer<LayerObjectContainer, AreResource>
{
  private static final String AvailableFmt = "Containers: %d";

  public LayerContainer(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.CONTAINER, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(ARE_OFFSET_CONTAINERS, ARE_NUM_CONTAINERS,
                   Container.class, c -> new LayerObjectContainer(parent, c));
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
