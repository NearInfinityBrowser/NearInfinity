// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;
import java.util.Locale;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.Actor;
import infinity.resource.are.AreResource;
import infinity.resource.text.PlainTextResource;
import infinity.util.IniMap;
import infinity.util.IniMapCache;
import infinity.util.IniMapEntry;
import infinity.util.IniMapSection;

/**
 * Manages actor layer objects.
 * @author argent77
 */
public class LayerActor extends BasicLayer<LayerObjectActor>
{
  private static final String AvailableFmt = "Actors: %1$d";

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
        // loading actors from ARE
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_ACTORS);
        SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_ACTORS);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, Actor.class);
          for (int i = 0, size = listStruct.size(); i < size; i++) {
            LayerObjectActor obj = new LayerObjectAreActor(are, (Actor)listStruct.get(i));
            setListeners(obj);
            list.add(obj);
          }
          setInitialized(true);
        }

        // loading actors from associated INI
        String iniFile = are.getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH).replace(".ARE", ".INI");
        IniMap ini = ResourceFactory.resourceExists(iniFile) ? IniMapCache.get(iniFile) : null;
        if (ini != null) {
          for (int i = 0, count = ini.getSectionCount(); i < count; i++) {
            IniMapSection section = ini.getSection(i);
            IniMapEntry creFile = section.getEntry("cre_file");
            IniMapEntry spawnPoint = section.getEntry("spawn_point");
            if (creFile != null && spawnPoint != null) {
              String[] position = IniMapEntry.splitValues(spawnPoint.getValue(), IniMapEntry.REGEX_POSITION);
              for (int j = 0; j < position.length; j++) {
                try {
                  PlainTextResource iniRes = new PlainTextResource(ResourceFactory.getResourceEntry(iniFile));
                  LayerObjectActor obj = new LayerObjectIniActor(iniRes, section, j);
                  setListeners(obj);
                  list.add(obj);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }
          }
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
