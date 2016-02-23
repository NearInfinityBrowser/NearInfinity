// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.are.AutomapNotePST;

/**
 * Manages automap notes layer objects (both PST-specific and generic types).
 * @author argent77
 */
public class LayerAutomap extends BasicLayer<LayerObject>
{
  private static final String AvailableFmt = "Automap notes: %1$d";

  public LayerAutomap(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.Automap, viewer);
    loadLayer(false);
  }

  /**
   * Returns whether torment-specific automap note structures have been loaded.
   */
  public boolean isTorment()
  {
    return (Profile.getEngine() == Profile.Engine.PST);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObject> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_AUTOMAP_NOTES);
        SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_AUTOMAP_NOTES);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          if (isTorment()) {
            List<StructEntry> listStruct = getStructures(ofs, count, AutomapNotePST.class);
            for (int i = 0, size = listStruct.size(); i < size; i++) {
              LayerObjectAutomapPST obj = new LayerObjectAutomapPST(are, (AutomapNotePST)listStruct.get(i));
              setListeners(obj);
              list.add(obj);
            }
          } else {
            List<StructEntry> listStruct = getStructures(ofs, count, AutomapNote.class);
            for (int i = 0, size = listStruct.size(); i < size; i++) {
              LayerObjectAutomap obj = new LayerObjectAutomap(are, (AutomapNote)listStruct.get(i));
              setListeners(obj);
              list.add(obj);
            }
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
