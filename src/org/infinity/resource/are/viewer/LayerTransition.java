// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.AreResource;

/**
 * Manages map transition layer objects.
 */
public class LayerTransition extends BasicLayer<LayerObjectTransition>
{
  private static final String AvailableFmt = "Map transitions: %1$d";

  public LayerTransition(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.TRANSITION, viewer);
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectTransition> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        for (int i = 0; i < LayerObjectTransition.FIELD_NAME.length; i++) {
          ResourceRef ref = (ResourceRef)are.getAttribute(LayerObjectTransition.FIELD_NAME[i]);
          if (ref != null && !ref.getResourceName().isEmpty() && !"None".equalsIgnoreCase(ref.getResourceName())) {
            AreResource destAre = null;
            try {
              destAre = new AreResource(ResourceFactory.getResourceEntry(ref.getResourceName()));
            } catch (Exception e) {
              e.printStackTrace();
            }
            if (destAre != null) {
              LayerObjectTransition obj = new LayerObjectTransition(are, destAre, i, getViewer().getRenderer());
              setListeners(obj);
              list.add(obj);
            }
          }
        }
        setInitialized(true);
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
