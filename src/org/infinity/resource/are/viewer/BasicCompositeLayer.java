// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.HashMap;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.are.viewer.ViewerConstants.LayerType;

/**
 * Specialized base class for layer-specific managers that consists of multiple sublayers.
 *
 * @param <E> Type of the layer item in the manager
 * @param <R> Type of the resource that contains layer items
 */
public abstract class BasicCompositeLayer<E extends LayerObject, R extends AbstractStruct> extends BasicLayer<E, R> {
  /** Predefined identifier for use with the primary sublayer. */
  public static final int LAYER_PRIMARY = 0;

  private final HashMap<Integer, Boolean> layerEnabled = new HashMap<>();

  public BasicCompositeLayer(R parent, LayerType type, AreaViewer viewer) {
    super(parent, type, viewer);
    setLayerEnabled(LAYER_PRIMARY, true);
  }

  public boolean isLayerVisible(int id) {
    return isLayerVisible() && layerEnabled.getOrDefault(id, false);
  }

  @Override
  public void setLayerVisible(boolean visible) {
    setVisibilityState(visible);

    getLayerObjects().stream().forEach(obj -> {
      AbstractLayerItem[] items = obj.getLayerItems();
      for (final AbstractLayerItem item : items) {
        final int id = item.getId();
        item.setVisible(isLayerVisible(id) && isLayerEnabled(id));
      }
    });
  }

  public boolean isLayerEnabled(int id) {
    return layerEnabled.getOrDefault(id, false);
  }

  public void setLayerEnabled(int id, boolean enable) {
    if (isLayerEnabled(id) != enable) {
      layerEnabled.put(id, enable);
      setLayerVisible(isLayerVisible(LAYER_PRIMARY) || isLayerVisible(id));
    }
  }
}
