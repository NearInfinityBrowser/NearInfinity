// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.AutomapNotePST;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Automap Note (PST-specific)
 */
public class LayerObjectAutomapPST extends LayerObject
{
  private static final Image[] ICONS = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AUTOMAP_1),
                                        Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AUTOMAP_2)};
  private static final Point CENTER = new Point(26, 26);
  private static final double MAP_SCALE = 32.0 / 3.0;    // scaling factor for MOS to TIS coordinates

  private final AutomapNotePST note;
  private final Point location = new Point();

  private final IconLayerItem item;


  public LayerObjectAutomapPST(AreResource parent, AutomapNotePST note)
  {
    super("Automap", AutomapNotePST.class, parent);
    this.note = note;
    String msg = null;
    try {
      final IsNumeric x = (IsNumeric)note.getAttribute(AutomapNotePST.ARE_AUTOMAP_LOCATION_X);
      final IsNumeric y = (IsNumeric)note.getAttribute(AutomapNotePST.ARE_AUTOMAP_LOCATION_Y);
      location.x = (int)(x.getValue() * MAP_SCALE);
      location.y = (int)(y.getValue() * MAP_SCALE);
      msg = note.getAttribute(AutomapNotePST.ARE_AUTOMAP_TEXT).toString();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(note, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelMapNotes);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable()
  {
    return note;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void update(double zoomFactor)
  {
    if (note != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
