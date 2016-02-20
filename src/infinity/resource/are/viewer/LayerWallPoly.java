// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.StructEntry;
import infinity.resource.wed.WallPolygon;
import infinity.resource.wed.WedResource;

/**
 * Manages wall polygon layer objects.
 * @author argent77
 */
public class LayerWallPoly extends BasicLayer<LayerObjectWallPoly>
{
  private static final String AvailableFmt = "Wall polygons: %1$d";

  public LayerWallPoly(WedResource wed, AreaViewer viewer)
  {
    super(wed, ViewerConstants.LayerType.WallPoly, viewer);
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
          List<StructEntry> listStruct = getStructures(ofs, count, WallPolygon.class);
          for (int i = 0, size = listStruct.size(); i < size; i++) {
            LayerObjectWallPoly obj = new LayerObjectWallPoly(wed, (WallPolygon)listStruct.get(i));
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
