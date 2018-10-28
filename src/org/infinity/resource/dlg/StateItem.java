// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.infinity.icon.Icons;
import org.infinity.util.StringTable;

/** Encapsulates a dialog state entry. */
final class StateItem extends ItemBase
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_STOP_16);
  /** Maximum string length to display. */
  private static final int MAX_LENGTH = 100;

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
    if (state != null) {
      this.state = state;
    }
  }

  @Override
  public Icon getIcon()
  {
    return icon;
  }

  @Override
  public String toString()
  {
    if (state != null) {
      String text = StringTable.getStringRef(state.getResponse().getValue(),
                                             showStrrefs() ? StringTable.Format.STRREF_PREFIX : StringTable.Format.NONE);
      if (text.length() > MAX_LENGTH) {
        text = text.substring(0, MAX_LENGTH) + "...";
      }
      return String.format("%s: %s", state.getName(), text);
    } else {
      return "(Invalid state)";
    }
  }
}
