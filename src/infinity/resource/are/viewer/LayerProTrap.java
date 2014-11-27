// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.StructEntry;
import infinity.resource.are.AreResource;
import infinity.resource.are.ProTrap;

/**
 * Manages projectile trap layer objects.
 * @author argent77
 */
public class LayerProTrap extends BasicLayer<LayerObjectProTrap>
{
  private static final String AvailableFmt = "Projectile traps: %1$d";

  public LayerProTrap(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.ProTrap, viewer);
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
        SectionOffset so = (SectionOffset)are.getAttribute("Projectile traps offset");
        SectionCount sc = (SectionCount)are.getAttribute("# projectile traps");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, ProTrap.class);
          for (int i = 0, size = listStruct.size(); i < size; i++) {
            LayerObjectProTrap obj = new LayerObjectProTrap(are, (ProTrap)listStruct.get(i));
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
