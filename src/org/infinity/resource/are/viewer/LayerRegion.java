// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.ITEPoint;

/**
 * Manages region layer objects.
 */
public class LayerRegion extends BasicLayer<LayerObjectRegion>
{
  private static final String AvailableFmt = "Regions: %d";

  public LayerRegion(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.REGION, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    List<LayerObjectRegion> list = getLayerObjects();
    if (hasAre()) {
      AreResource are = getAre();
      SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_TRIGGERS);
      SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_TRIGGERS);
      if (so != null && sc != null) {
        int ofs = so.getValue();
        int count = sc.getValue();
        for (final ITEPoint entry : getStructures(ofs, count, ITEPoint.class)) {
          final LayerObjectRegion obj = new LayerObjectRegion(are, entry);
          setListeners(obj);
          list.add(obj);
        }
        setInitialized(true);
      }
    }
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
