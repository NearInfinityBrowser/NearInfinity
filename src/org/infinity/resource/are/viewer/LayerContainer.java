// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Container;

/**
 * Manages container layer objects.
 */
public class LayerContainer extends BasicLayer<LayerObjectContainer>
{
  private static final String AvailableFmt = "Containers: %d";

  public LayerContainer(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.CONTAINER, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    List<LayerObjectContainer> list = getLayerObjects();
    if (hasAre()) {
      AreResource are = getAre();
      SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_CONTAINERS);
      SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_CONTAINERS);
      if (so != null && sc != null) {
        int ofs = so.getValue();
        int count = sc.getValue();
        for (final Container entry : getStructures(ofs, count, Container.class)) {
          final LayerObjectContainer obj = new LayerObjectContainer(are, entry);
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
