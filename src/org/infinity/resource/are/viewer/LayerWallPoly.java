// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.wed.WallPolygon;
import org.infinity.resource.wed.WedResource;
import static org.infinity.resource.wed.WedResource.WED_NUM_WALL_POLYGONS;
import static org.infinity.resource.wed.WedResource.WED_OFFSET_WALL_POLYGONS;

/**
 * Manages wall polygon layer objects.
 */
public class LayerWallPoly extends BasicLayer<LayerObjectWallPoly, WedResource>
{
  private static final String AvailableFmt = "Wall polygons: %d";

  public LayerWallPoly(WedResource wed, AreaViewer viewer)
  {
    super(wed, ViewerConstants.LayerType.WALL_POLY, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(WED_OFFSET_WALL_POLYGONS, WED_NUM_WALL_POLYGONS,
                   WallPolygon.class, w -> new LayerObjectWallPoly(parent, w));
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
