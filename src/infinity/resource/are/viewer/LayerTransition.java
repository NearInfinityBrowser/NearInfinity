// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.ResourceRef;
import infinity.resource.ResourceFactory;
import infinity.resource.are.AreResource;

/**
 * Manages map transition layer objects.
 * @author argent77
 */
public class LayerTransition extends BasicLayer<LayerObjectTransition>
{
  private static final String AvailableFmt = "%1$d map transition%2$s available";

  public LayerTransition(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.Transition, viewer);
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
        for (int i = 0; i < LayerObjectTransition.FieldName.length; i++) {
          ResourceRef ref = (ResourceRef)are.getAttribute(LayerObjectTransition.FieldName[i]);
          if (ref != null && !ref.getResourceName().isEmpty() && !"None".equalsIgnoreCase(ref.getResourceName())) {
            AreResource destAre = null;
            try {
              destAre = new AreResource(ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
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
    return String.format(AvailableFmt, cnt, (cnt == 1) ? "" : "s");
  }
}
