// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_ENTRANCES;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_ENTRANCES;
import org.infinity.resource.are.Entrance;

/**
 * Manages entrance layer objects.
 */
public class LayerEntrance extends BasicLayer<LayerObjectEntrance, AreResource>
{
  private static final String AvailableFmt = "Entrances: %d";

  public LayerEntrance(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.ENTRANCE, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(ARE_OFFSET_ENTRANCES, ARE_NUM_ENTRANCES,
                   Entrance.class, e -> new LayerObjectEntrance(parent, e));
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
