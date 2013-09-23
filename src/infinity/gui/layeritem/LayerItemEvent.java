// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import java.util.EventObject;

/**
 * Used in AbstractLayerItem and subclasses.
 * @author argent77
 */
public class LayerItemEvent extends EventObject
{
  // represents the possible states of this component
  public enum ItemState { NORMAL, HIGHLIGHTED, SELECTED }
  private ItemState itemState;
  private String actionCommand;

  public LayerItemEvent(Object source, ItemState state, String cmd)
  {
    super(source);
    this.itemState = (state != null) ? state : ItemState.NORMAL;
    this.actionCommand = cmd;
  }

  public ItemState getItemState()
  {
    return itemState;
  }

  public String getActionCommand()
  {
    return actionCommand;
  }
}
