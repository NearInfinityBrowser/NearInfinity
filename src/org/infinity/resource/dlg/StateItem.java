// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.infinity.icon.Icons;

/** Encapsulates a dialog state entry. */
final class StateItem extends ItemBase
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_STOP_16);

  private final ImageIcon icon;

  private State state;

  public StateItem(DlgResource dlg, State state)
  {
    super(dlg);
    this.icon = showIcons() ? ICON : null;
    this.state = state;
  }

  public State getState()
  {
    return state;
  }

  public void setState(State state)
  {
    this.state = state;
  }

  @Override
  public Icon getIcon()
  {
    return icon;
  }

  @Override
  public String toString()
  {
    final String text = getText(state.getResponse());
    return String.format("%s: %s", state.getName(), text);
  }
}
