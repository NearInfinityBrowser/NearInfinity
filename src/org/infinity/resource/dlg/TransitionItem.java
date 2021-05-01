// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import static java.util.Collections.emptyList;
import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;

/** Encapsulates a dialog transition entry. */
class TransitionItem extends StateOwnerItem
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_PLAY_16);

  private final Transition trans;

  /** Parent tree item from which this transition is available. */
  protected final TransitionOwnerItem parent;
  /**
   * Item to which need go to in break cycles tree view mode. This item contains
   * referense to the same transition as this one (i.e. {@code this.trans == main.ref.trans})
   */
  private final DlgElement elem;
  /** Tree item to which go this transition or {@code null}, if this transition terminates dialog. */
  StateItem nextState;

  /**
   * Constructor for broken references to transitions.
   *
   * @param parent State which contains broken reference
   */
  protected TransitionItem(StateItem parent)
  {
    this.trans  = null;
    this.parent = Objects.requireNonNull(parent, "Parent state of broken transition item must be not null");
    this.elem   = null;
  }
  public TransitionItem(Transition trans, TransitionOwnerItem parent, DlgElement elem)
  {
    this.trans  = Objects.requireNonNull(trans,  "Transition dialog entry must be not null");
    this.parent = Objects.requireNonNull(parent, "Parent state of transition item must be not null");
    this.elem   = elem;
  }

  @Override
  public Transition getEntry() { return trans; }

  @Override
  public ItemBase getMain() { return elem == null || elem.main == this ? null : elem.main; }

  @Override
  public DlgResource getDialog() { return trans.getParent(); }

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

  @Override
  public void traverseChildren(Consumer<ItemBase> action)
  {
    if (nextState != null) {
      action.accept(nextState);
    }
  }

  /** Returns technical name of transition item which uniquely identifying it within dialog. */
  public String getName() { return trans.getName(); }

  @Override
  public StateItem getChildAt(int childIndex) { return isMain() && childIndex == 0 ? nextState : null; }

  @Override
  public int getChildCount() { return isMain() && nextState != null ? 1 : 0; }

  @Override
  public TransitionOwnerItem getParent() { return parent; }

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
    return enumeration(isLeaf() ? emptyList() : singletonList(nextState));
  }

  @Override
  public String toString()
  {
    final String text = getText(trans);
    final String nextDlg = trans.getNextDialog().getResourceName();
    //TODO: When getResourceName() will return null, replace check `.isEmpty()` to `nextDlg == null`
    if (trans.getNextDialog().isEmpty() || nextDlg.equalsIgnoreCase(getDialogName())) {
      return text;
    }
    return String.format("%s [%s]", text, nextDlg);
  }

  private boolean isMain()
  {
    return getMain() == null || !BrowserMenuBar.getInstance().breakCyclesInDialogs();
  }
}
