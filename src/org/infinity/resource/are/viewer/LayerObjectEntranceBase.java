// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Point;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.AbstractStruct;

/**
 *
 */
public abstract class LayerObjectEntranceBase extends LayerObject {
  protected LayerObjectEntranceBase(AbstractStruct parent) {
    super("Entrance", parent);
  }

  /** Returns the {@link IconLayerItem} instance of the entrance icon. */
  protected abstract IconLayerItem getLayerItem();

  /** Returns the location of the Entrance on the map. */
  protected abstract Point getLocation();

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    if (type == 0 && getLayerItem() != null) {
      return new AbstractLayerItem[] { getLayerItem() };
    }
    return new AbstractLayerItem[0];
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    return new AbstractLayerItem[] { getLayerItem() };
  }

  @Override
  public void update(double zoomFactor) {
    if (getLayerItem() != null) {
      getLayerItem().setItemLocation((int) (getLocation().x * zoomFactor + (zoomFactor / 2.0)),
          (int) (getLocation().y * zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
