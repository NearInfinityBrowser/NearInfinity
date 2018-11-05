// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import java.util.Enumeration;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import org.infinity.icon.Icons;

/** Encapsulates a dialog transition entry. */
final class TransitionItem extends StateOwnerItem
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_PLAY_16);

  /** Parent tree item from which this transition is available. */
  private final StateItem parent;
  /** Tree item to which go this transition or {@code null}, if this transition terminates dialog. */
  StateItem nextState;

  private final ImageIcon icon;

  private Transition trans;

  public TransitionItem(StateItem parent, Transition trans)
  {
    super(parent.dlg);
    this.parent = parent;
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

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public StateItem getChildAt(int childIndex) { return childIndex == 0 ? nextState : null; }

  @Override
  public int getChildCount() { return nextState == null ? 0 : 1; }

  @Override
  public StateItem getParent() { return parent; }

  @Override
  public int getIndex(TreeNode node) { return node != null && node == nextState ? 0 : -1; }

  // Flag 3: Terminates dialogue
  @Override
  public boolean getAllowsChildren() { return !trans.getFlag().isFlagSet(3); }

  @Override
  public boolean isLeaf() { return nextState == null; }

  @Override
  public Enumeration<? extends StateItem> children() { return enumeration(singletonList(nextState)); }
  //</editor-fold>

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
