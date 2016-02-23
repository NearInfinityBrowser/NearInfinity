// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.util.EventListener;

/**
 * Used in AbstractLayerItem
 * @author argent77
 *
 */
public interface LayerItemListener extends EventListener
{
  public void layerItemChanged(LayerItemEvent e);
}
