// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.util.EventObject;

/**
 * Used in AbstractLayerItem and subclasses.
 */
public class LayerItemEvent extends EventObject {
  private String actionCommand;

  public LayerItemEvent(Object source, String cmd) {
    super(source);
    this.actionCommand = cmd;
  }

  public boolean isHighlighted() {
    if (source instanceof AbstractLayerItem) {
      return (((AbstractLayerItem) source).getItemState() == AbstractLayerItem.ItemState.HIGHLIGHTED);
    } else {
      return false;
    }
  }

  public String getActionCommand() {
    return actionCommand;
  }
}
