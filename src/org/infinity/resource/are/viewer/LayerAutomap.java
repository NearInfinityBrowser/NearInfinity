// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.Profile;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.are.AutomapNotePST;

/**
 * Manages automap notes layer objects (both PST-specific and generic types).
 */
public class LayerAutomap extends BasicLayer<LayerObject>
{
  private static final String AvailableFmt = "Automap notes: %d";

  public LayerAutomap(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.AUTOMAP, viewer);
    loadLayer();
  }

  /**
   * Returns whether torment-specific automap note structures have been loaded.
   */
  public boolean isTorment()
  {
    return (Profile.getEngine() == Profile.Engine.PST);
  }

  @Override
  protected void loadLayer()
  {
    List<LayerObject> list = getLayerObjects();
    if (hasAre()) {
      AreResource are = getAre();
      SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_AUTOMAP_NOTES);
      SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_AUTOMAP_NOTES);
      if (so != null && sc != null) {
        int ofs = so.getValue();
        int count = sc.getValue();
        if (isTorment()) {
          for (final AutomapNotePST entry : getStructures(ofs, count, AutomapNotePST.class)) {
            final LayerObjectAutomapPST obj = new LayerObjectAutomapPST(are, entry);
            setListeners(obj);
            list.add(obj);
          }
        } else {
          for (final AutomapNote entry : getStructures(ofs, count, AutomapNote.class)) {
            final LayerObjectAutomap obj = new LayerObjectAutomap(are, entry);
            setListeners(obj);
            list.add(obj);
          }
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
