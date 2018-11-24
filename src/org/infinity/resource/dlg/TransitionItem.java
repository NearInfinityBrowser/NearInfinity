// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import static java.util.Collections.emptyList;
import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;

/** Encapsulates a dialog transition entry. */
final class TransitionItem extends StateOwnerItem
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_PLAY_16);

  /** Parent tree item from which this transition is available. */
  private final StateItem parent;
  /**
   * Item to which need go to in break cycles tree view mode. This item contains
   * referense to the same transition as this one (i.e. {@code this.trans == main.trans})
   */
  private final TransitionItem main;
  /** Tree item to which go this transition or {@code null}, if this transition terminates dialog. */
  StateItem nextState;

  private final Transition trans;

  public TransitionItem(StateItem parent, Transition trans, TransitionItem main)
  {
    this.parent = parent;
    this.main = main;
    this.trans = trans;
  }

  public Transition getTransition()
  {
    return trans;
  }

  @Override
  public TransitionItem getMain() { return main; }

  @Override
  public DlgResource getDialog() { return (DlgResource)trans.getParent(); }

  @Override
  public Icon getIcon() { return ICON; }

  @Override
  public boolean removeChild(ItemBase child)
  {
    if (child != null && child == nextState) {
      nextState = null;
      return true;
    }
    return false;
  }

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public StateItem getChildAt(int childIndex) { return isMain() && childIndex == 0 ? nextState : null; }

  @Override
  public int getChildCount() { return isMain() && nextState != null ? 1 : 0; }

  @Override
  public StateItem getParent() { return parent; }

  @Override
  public int getIndex(TreeNode node) { return isMain() && node != null && node == nextState ? 0 : -1; }

  // Flag 3: Terminates dialogue
  @Override
  public boolean getAllowsChildren() { return isMain() && !trans.getFlag().isFlagSet(3); }

  @Override
  public boolean isLeaf() { return isMain() ? nextState == null : true; }

  @Override
  public Enumeration<? extends StateItem> children()
  {
    return enumeration(isLeaf() ?  emptyList(): singletonList(nextState));
  }
  //</editor-fold>

  @Override
  public String toString()
  {
    String text = "(No text)";
    if (trans.getFlag().isFlagSet(0)) {
      // Flag 0: Transition contains text
      text = getText(trans);
    }
    final String nextDlg = trans.getNextDialog().getResourceName();
    //TODO: When getResourceName() will return null, replace check `.isEmpty()` to `nextDlg == null`
    if (trans.getNextDialog().isEmpty() || nextDlg.equalsIgnoreCase(getDialogName())) {
      return text;
    }
    return String.format("%s [%s]", text, nextDlg);
  }

  private boolean isMain()
  {
    return main == null || !BrowserMenuBar.getInstance().breakCyclesInDialogs();
  }
}
