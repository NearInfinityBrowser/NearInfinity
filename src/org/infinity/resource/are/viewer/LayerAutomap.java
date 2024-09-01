// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import static org.infinity.resource.are.AreResource.ARE_NUM_AUTOMAP_NOTES;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_AUTOMAP_NOTES;

import java.util.Iterator;
import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.are.AutomapNotePST;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapCache;
import org.infinity.util.IniMapSection;
import org.tinylog.Logger;

/**
 * Manages automap notes layer objects (both PST-specific and generic types).
 */
public class LayerAutomap extends BasicLayer<LayerObject, AreResource> {
  private static final String AVAILABLE_FMT = "Automap notes: %d";

  public LayerAutomap(AreResource are, AreaViewer viewer) {
    super(are, ViewerConstants.LayerType.AUTOMAP, viewer);
    loadLayer();
  }

  /**
   * Returns whether torment-specific automap note structures have been loaded.
   */
  public boolean isTorment() {
    return (Profile.getEngine() == Profile.Engine.PST);
  }

  @Override
  protected void loadLayer() {
    if (isTorment()) {
      // loading predefined entries
      loadPredefinedAutoNotes(parent);

      // loading user-defined entries
      loadLayerItems(ARE_OFFSET_AUTOMAP_NOTES, ARE_NUM_AUTOMAP_NOTES, AutomapNotePST.class,
          n -> new LayerObjectAutomapPST(parent, n));
    } else {
      loadLayerItems(ARE_OFFSET_AUTOMAP_NOTES, ARE_NUM_AUTOMAP_NOTES, AutomapNote.class,
          n -> new LayerObjectAutomap(parent, n));
    }
  }

  @Override
  public String getAvailability() {
    int cnt = getLayerObjectCount();
    return String.format(AVAILABLE_FMT, cnt);
  }

  /**
   * (PST only) Loads predefined automap note definitions from "AUTONOTE.INI" for the specified map.
   *
   * @param are Area to load automap notes for.
   */
  private void loadPredefinedAutoNotes(AreResource are) {
    if (are == null) {
      return;
    }

    // loading autonote section for this area
    final IniMap ini = IniMapCache.get("autonote.ini");
    if (ini != null) {
      final String areResref = are.getResourceEntry().getResourceRef();
      IniMapSection iniSection = null;
      for (final Iterator<IniMapSection> iter = ini.iterator(); iter.hasNext(); ) {
        final IniMapSection s = iter.next();
        if (s.getName().equalsIgnoreCase(areResref)) {
          final int count = s.getAsInteger("count", 0);
          if (count > 0) {
            iniSection = s;
          }
          break;
        }
      }

      // loading automap notes
      if (iniSection != null) {
        final List<LayerObject> objectList = getLayerObjects();
        int count = 0;
        try {
          count = iniSection.getAsInteger("count", 0);
        } catch (IllegalArgumentException e) {
        }
        final double scale = 32.0 / 3.0;
        for (int i = 0; i < count; i++) {
          try {
            final String keyStrref = "text" + (i + 1);
            final String keyX = "xPos" + (i + 1);
            final String keyY = "yPos" + (i + 1);
            final int strref = iniSection.getAsInteger(keyStrref, -1);
            final int x = (int) (iniSection.getAsInteger(keyX, -1) * scale);
            final int y = (int) (iniSection.getAsInteger(keyY, -1) * scale);
            if (strref >= 0 && x >= 0 && y >= 0) {
              final LayerObjectAutomapPSTIni obj = new LayerObjectAutomapPSTIni(iniSection, i);
              setListeners(obj);
              objectList.add(obj);
            }
          } catch (IllegalArgumentException e) {
            Logger.error(e);
          }
        }
      }
    }
  }
}
