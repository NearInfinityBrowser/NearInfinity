// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.wed.WallPolygon;
import org.infinity.resource.wed.WedResource;

/**
 * Manages wall polygon layer objects.
 */
public class LayerWallPoly extends BasicLayer<LayerObjectWallPoly>
{
  private static final String AvailableFmt = "Wall polygons: %d";

  public LayerWallPoly(WedResource wed, AreaViewer viewer)
  {
    super(wed, ViewerConstants.LayerType.WALL_POLY, viewer);
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectWallPoly> list = getLayerObjects();
      if (hasWed()) {
        WedResource wed = getWed();
        SectionOffset so = (SectionOffset)wed.getAttribute(WedResource.WED_OFFSET_WALL_POLYGONS);
        SectionCount sc = (SectionCount)wed.getAttribute(WedResource.WED_NUM_WALL_POLYGONS);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          for (final WallPolygon entry : getStructures(ofs, count, WallPolygon.class)) {
            final LayerObjectWallPoly obj = new LayerObjectWallPoly(wed, entry);
            setListeners(obj);
            list.add(obj);
          }
          setInitialized(true);
        }
      }
      return list.size();
    }
    return 0;
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
