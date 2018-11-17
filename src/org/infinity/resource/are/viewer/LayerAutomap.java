// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.Profile;
import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_AUTOMAP_NOTES;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_AUTOMAP_NOTES;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.are.AutomapNotePST;

/**
 * Manages automap notes layer objects (both PST-specific and generic types).
 */
public class LayerAutomap extends BasicLayer<LayerObject, AreResource>
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
    if (isTorment()) {
      loadLayerItems(ARE_OFFSET_AUTOMAP_NOTES, ARE_NUM_AUTOMAP_NOTES,
                     AutomapNotePST.class, n -> new LayerObjectAutomapPST(parent, n));
    } else {
      loadLayerItems(ARE_OFFSET_AUTOMAP_NOTES, ARE_NUM_AUTOMAP_NOTES,
                     AutomapNote.class, n -> new LayerObjectAutomap(parent, n));
    }
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}
