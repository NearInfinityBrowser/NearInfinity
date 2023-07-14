// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.are.viewer.ViewerConstants.LayerType;

/**
 *
 */
public abstract class BasicTargetLayer<E extends LayerObject, R extends AbstractStruct> extends BasicLayer<E, R> {
  private boolean polyEnabled = true;
  private boolean iconsEnabled;

  public BasicTargetLayer(R parent, LayerType type, AreaViewer viewer) {
    super(parent, type, viewer);
  }

  @Override
  public void setLayerVisible(boolean visible) {
    setVisibilityState(visible);

    getLayerObjects().stream().forEach(obj -> {
      AbstractLayerItem[] items = obj.getLayerItems(ViewerConstants.LAYER_ITEM_POLY);
      for (final AbstractLayerItem item : items) {
        item.setVisible(isPolyLayerVisible() && isPolyLayerEnabled());
      }

      items = obj.getLayerItems(ViewerConstants.LAYER_ITEM_ICON);
      for (final AbstractLayerItem item : items) {
        item.setVisible(isIconsLayerVisible() && isIconsLayerEnabled());
      }
    });
  }

  public boolean isPolyLayerVisible() {
    return isLayerVisible() && polyEnabled;
  }

  public boolean isIconsLayerVisible() {
    return isLayerVisible() && iconsEnabled;
  }

  public boolean isPolyLayerEnabled() {
    return polyEnabled;
  }

  public void setPolyLayerEnabled(boolean enable) {
    if (enable != polyEnabled) {
      polyEnabled = enable;
      setLayerVisible(isPolyLayerVisible());
    }
  }

  public boolean isIconsLayerEnabled() {
    return iconsEnabled;
  }

  public void setIconLayerEnabled(boolean enable) {
    if (enable != iconsEnabled) {
      iconsEnabled = enable;
      setLayerVisible(isPolyLayerVisible() || isIconsLayerVisible());
    }
  }
}
