// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.SpawnPoint;

/**
 * Manages spawn point layer objects.
 * @author argent77
 */
public class LayerSpawnPoint extends BasicLayer<LayerObjectSpawnPoint>
{
  private static final String AvailableFmt = "Spawn points: %1$d";

  public LayerSpawnPoint(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.SPAWN_POINT, viewer);
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectSpawnPoint> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_SPAWN_POINTS);
        SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_SPAWN_POINTS);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, SpawnPoint.class);
          for (int i = 0, size = listStruct.size(); i < size; i++) {
            LayerObjectSpawnPoint obj = new LayerObjectSpawnPoint(are, (SpawnPoint)listStruct.get(i));
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
