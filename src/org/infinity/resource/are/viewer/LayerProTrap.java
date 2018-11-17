// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.ProTrap;

/**
 * Manages projectile trap layer objects.
 */
public class LayerProTrap extends BasicLayer<LayerObjectProTrap>
{
  private static final String AvailableFmt = "Projectile traps: %d";

  public LayerProTrap(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.PRO_TRAP, viewer);
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectProTrap> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_PROJECTILE_TRAPS);
        SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_PROJECTILE_TRAPS);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          for (final ProTrap entry : getStructures(ofs, count, ProTrap.class)) {
            final LayerObjectProTrap obj = new LayerObjectProTrap(are, entry);
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
