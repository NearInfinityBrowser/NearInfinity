// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.StructEntry;
import infinity.resource.are.Actor;
import infinity.resource.are.AreResource;

/**
 * Manages actor layer objects.
 * @author argent77
 */
public class LayerActor extends BasicLayer<LayerObjectActor>
{
  private static final String AvailableFmt = "%1$d actor%2$s available";

  public LayerActor(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.Actor, viewer);
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectActor> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute("Actors offset");
        SectionCount sc = (SectionCount)are.getAttribute("# actors");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, Actor.class);
          for (int i = 0; i < listStruct.size(); i++) {
            LayerObjectActor obj = new LayerObjectActor(are, (Actor)listStruct.get(i));
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
    return String.format(AvailableFmt, cnt, (cnt == 1) ? "" : "s");
  }
}
