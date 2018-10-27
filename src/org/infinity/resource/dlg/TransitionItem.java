// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.infinity.icon.Icons;

/** Encapsulates a dialog transition entry. */
final class TransitionItem extends ItemBase
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_PLAY_16);

  private final ImageIcon icon;

  private Transition trans;

  public TransitionItem(DlgResource dlg, Transition trans)
  {
    super(dlg);
    this.icon = showIcons() ? ICON : null;
    this.trans = trans;
  }

  public Transition getTransition()
  {
    return trans;
  }

  public void setTransition(Transition trans)
  {
    this.trans = trans;
  }

  @Override
  public Icon getIcon()
  {
    return icon;
  }

  @Override
  public String toString()
  {
    String text = "(No text)";
    if (trans.getFlag().isFlagSet(0)) {
      // Flag 0: Transition contains text
      text = getText(trans.getAssociatedText());
    }
    final String nextDlg = trans.getNextDialog().getResourceName();
    //TODO: When getResourceName() will return null, replace check `.isEmpty()` to `nextDlg == null`
    if (trans.getNextDialog().isEmpty() || nextDlg.equalsIgnoreCase(getDialogName())) {
      return String.format("%s: %s", trans.getName(), text);
    }
    return String.format("%s: %s [%s]", trans.getName(), text, nextDlg);
  }
}
