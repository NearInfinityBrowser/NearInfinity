// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_PROJECTILE_TRAPS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_PROJECTILE_TRAPS;
import org.infinity.resource.are.ProTrap;

/**
 * Manages projectile trap layer objects.
 */
public class LayerProTrap extends BasicLayer<LayerObjectProTrap, AreResource>
{
  private static final String AvailableFmt = "Projectile traps: %d";

  public LayerProTrap(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.PRO_TRAP, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(ARE_OFFSET_PROJECTILE_TRAPS, ARE_NUM_PROJECTILE_TRAPS,
                   ProTrap.class, p -> new LayerObjectProTrap(parent, p));
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
