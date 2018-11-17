// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.SpawnPoint;

/**
 * Manages spawn point layer objects.
 */
public class LayerSpawnPoint extends BasicLayer<LayerObjectSpawnPoint>
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
    List<LayerObjectSpawnPoint> list = getLayerObjects();
    if (hasAre()) {
      AreResource are = getAre();
      SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_SPAWN_POINTS);
      SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_SPAWN_POINTS);
      if (so != null && sc != null) {
        int ofs = so.getValue();
        int count = sc.getValue();
        for (final SpawnPoint entry : getStructures(ofs, count, SpawnPoint.class)) {
          final LayerObjectSpawnPoint obj = new LayerObjectSpawnPoint(are, entry);
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
