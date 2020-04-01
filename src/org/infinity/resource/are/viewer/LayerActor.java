// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;
import java.util.Locale;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_ACTORS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_ACTORS;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapCache;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;

/**
 * Manages actor layer objects.
 */
public class LayerActor extends BasicLayer<LayerObjectActor, AreResource>
{
  private static final String AvailableFmt = "Actors: %d";

  public LayerActor(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.ACTOR, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    // loading actors from ARE
    loadLayerItems(ARE_OFFSET_ACTORS, ARE_NUM_ACTORS,
                   Actor.class, a -> new LayerObjectAreActor(parent, a));

    final List<LayerObjectActor> list = getLayerObjects();
    // loading actors from associated INI
    final String iniFile = parent.getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH).replace(".ARE", ".INI");
    IniMap ini = ResourceFactory.resourceExists(iniFile) ? IniMapCache.get(iniFile) : null;
    if (ini != null) {
      for (final IniMapSection section : ini) {
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

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
