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
import org.infinity.resource.are.ProTrap;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Projectile Trap
 */
public class LayerObjectProTrap extends LayerObject
{
  private static final Image[] ICONS = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_PRO_TRAP_1),
                                        Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_PRO_TRAP_2)};
  private static final Point CENTER = new Point(14, 14);

  private final ProTrap trap;
  private final Point location = new Point();

  private final IconLayerItem item;

  public LayerObjectProTrap(AreResource parent, ProTrap trap)
  {
    super("Trap", ProTrap.class, parent);
    this.trap = trap;
    String msg = null;
    try {
      msg = trap.getAttribute(ProTrap.ARE_PROTRAP_TRAP).toString();
      location.x = ((IsNumeric)trap.getAttribute(ProTrap.ARE_PROTRAP_LOCATION_X)).getValue();
      location.y = ((IsNumeric)trap.getAttribute(ProTrap.ARE_PROTRAP_LOCATION_Y)).getValue();
      int target = ((IsNumeric)trap.getAttribute(ProTrap.ARE_PROTRAP_TARGET)).getValue();
      if (target < 0) target = 0; else if (target > 255) target = 255;
      if (target >= 2 && target <= 30) {
        msg += " (hostile)";
      } else if (target >= 200) {
        msg += " (friendly)";
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(trap, msg, icons[0], CENTER);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, ICONS[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable()
  {
    return trap;
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
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
