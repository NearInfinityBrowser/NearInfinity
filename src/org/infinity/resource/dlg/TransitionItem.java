// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.infinity.icon.Icons;
import org.infinity.util.StringTable;

/** Encapsulates a dialog transition entry. */
final class TransitionItem extends ItemBase
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_PLAY_16);
  /** Maximum string length to display. */
  private static final int MAX_LENGTH = 100;

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
    if (trans != null) {
      this.trans = trans;
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
    if (trans != null) {
      if (trans.getFlag().isFlagSet(0)) {
        // Transition contains text
        String text = StringTable.getStringRef(trans.getAssociatedText().getValue(),
                                               showStrrefs() ? StringTable.Format.STRREF_PREFIX : StringTable.Format.NONE);
        if (text.length() > MAX_LENGTH) {
          text = text.substring(0, MAX_LENGTH) + "...";
        }
        String dlg = getDialog().getResourceEntry().getResourceName();
        if (trans.getNextDialog().isEmpty() ||
            trans.getNextDialog().getResourceName().equalsIgnoreCase(dlg)) {
          return String.format("%s: %s", trans.getName(), text);
        } else {
          return String.format("%s: %s [%s]",
                               trans.getName(), text, trans.getNextDialog().getResourceName());
        }
      } else {
        // Transition contains no text
        String dlg = getDialog().getResourceEntry().getResourceName();
        if (trans.getNextDialog().isEmpty() ||
            trans.getNextDialog().getResourceName().equalsIgnoreCase(dlg)) {
          return String.format("%s: (No text)", trans.getName());
        } else {
          return String.format("%s: (No text) [%s]",
                               trans.getName(), trans.getNextDialog().getResourceName());
        }
      }
    } else {
      return "(Invalid response)";
    }
  }
}
