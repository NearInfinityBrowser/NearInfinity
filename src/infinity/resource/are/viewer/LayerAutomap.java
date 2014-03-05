// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNote;
import infinity.resource.are.AutomapNotePST;

/**
 * Manages automap notes layer objects (both PST-specific and generic types).
 * @author argent77
 */
public class LayerAutomap extends BasicLayer<LayerObject>
{
  private static final String AvailableFmt = "%1$d automap note%2$s available";

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
    return (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      clear();
      List<LayerObject> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute("Automap notes offset");
        SectionCount sc = (SectionCount)are.getAttribute("# automap notes");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          if (isTorment()) {
            List<StructEntry> listStruct = getStructures(ofs, count, AutomapNotePST.class);
            for (int i = 0; i < listStruct.size(); i++) {
              LayerObjectAutomapPST obj = new LayerObjectAutomapPST(are, (AutomapNotePST)listStruct.get(i));
              setListeners(obj);
              list.add(obj);
            }
          } else {
            List<StructEntry> listStruct = getStructures(ofs, count, AutomapNote.class);
            for (int i = 0; i < listStruct.size(); i++) {
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
    return String.format(AvailableFmt, cnt, (cnt == 1) ? "" : "s");
  }
}
