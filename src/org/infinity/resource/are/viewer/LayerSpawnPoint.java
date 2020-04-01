// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_SPAWN_POINTS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_SPAWN_POINTS;
import org.infinity.resource.are.SpawnPoint;

/**
 * Manages spawn point layer objects.
 */
public class LayerSpawnPoint extends BasicLayer<LayerObjectSpawnPoint, AreResource>
{
  private static final String AvailableFmt = "Spawn points: %d";

  public LayerSpawnPoint(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.SPAWN_POINT, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(ARE_OFFSET_SPAWN_POINTS, ARE_NUM_SPAWN_POINTS,
                   SpawnPoint.class, p -> new LayerObjectSpawnPoint(parent, p));
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
