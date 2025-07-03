// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.tinylog.Logger;

/**
 * Handles the virtual layer type: VirtualMap/VirtualPosition
 */
public class LayerObjectVirtualPosition extends LayerObject {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_PUSH_PIN_1.getIcon().getImage(),
      ViewerIcons.ICON_ITM_PUSH_PIN_2.getIcon().getImage() };

  private static final Point CENTER = ViewerIcons.ICON_ITM_PUSH_PIN_1.getCenter();

  private final VirtualPosition position;
  private final Point location = new Point();

  private final IconLayerItem item;

  public LayerObjectVirtualPosition(VirtualMap parent, VirtualPosition position) {
    super("Pin", parent);
    this.position = position;
    String msg = "Pin";
    try {
      location.x = ((IsNumeric)this.position.getAttribute(VirtualPosition.POSITION_X)).getValue();
      location.y = ((IsNumeric)this.position.getAttribute(VirtualPosition.POSITION_Y)).getValue();
      int idx = parent.getFields(VirtualPosition.class).indexOf(this.position);
      if (idx >= 0) {
        msg = msg + ' ' + idx;
      }
      msg += " (" + location.x + ',' + location.y + ')';
    } catch (Exception e) {
      Logger.error(e);
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(position, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelPins);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable() {
    return position;
  }

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    if (type == 0 && item != null) {
      return new AbstractLayerItem[] { item };
    }
    return new AbstractLayerItem[0];
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    return new AbstractLayerItem[] { item };
  }

  @Override
  public void update(double zoomFactor) {
    if (item != null) {
      item.setItemLocation((int) (location.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (location.y * zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
