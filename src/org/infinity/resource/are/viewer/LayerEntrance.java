// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Entrance;

/**
 * Manages entrance layer objects.
 */
public class LayerEntrance extends BasicLayer<LayerObjectEntrance>
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
    List<LayerObjectEntrance> list = getLayerObjects();
    if (hasAre()) {
      AreResource are = getAre();
      SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_ENTRANCES);
      SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_ENTRANCES);
      if (so != null && sc != null) {
        int ofs = so.getValue();
        int count = sc.getValue();
        for (final Entrance entry : getStructures(ofs, count, Entrance.class)) {
          final LayerObjectEntrance obj = new LayerObjectEntrance(are, entry);
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
