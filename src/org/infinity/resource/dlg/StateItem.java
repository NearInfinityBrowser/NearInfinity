// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import static java.util.Collections.enumeration;
import java.util.Enumeration;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import org.infinity.icon.Icons;

/** Encapsulates a dialog state entry. */
final class StateItem extends ItemBase implements Iterable<TransitionItem>
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_STOP_16);

  /** Tree item that represent visual parent of this state in the tree. */
  private final StateOwnerItem parent;
  /** Item to which need go to in break cycles tree view mode. */
  private final StateItem main;
  /** Items that represents transition tree nodes from this state. */
  private ArrayList<TransitionItem> trans;

  private final ImageIcon icon;

  private State state;

  public StateItem(State state, StateOwnerItem parent, StateItem main)
  {
    super((DlgResource)state.getParent());
    this.parent = parent;
    this.main = main;
    this.icon = showIcons() ? ICON : null;
    setState(state);
  }

  public State getState()
  {
    return state;
  }

  public void setState(State state)
  {
    this.state = state;
    this.trans = new ArrayList<>(state.getTransCount());

    final int start = state.getFirstTrans();
    final int count = state.getTransCount();
    for (int i = start; i < start + count; ++i) {
      trans.add(new TransitionItem(this, dlg.getTransition(i)));
    }
  }

  @Override
  public Icon getIcon()
  {
    return icon;
  }

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public TransitionItem getChildAt(int childIndex) { return trans.get(childIndex); }

  @Override
  public int getChildCount() { return trans.size(); }

  @Override
  public ItemBase getParent() { return parent; }

  @Override
  public int getIndex(TreeNode node) { return trans.indexOf(node); }

  @Override
  public boolean getAllowsChildren() { return true; }

  @Override
  public boolean isLeaf() { return trans.isEmpty(); }

  @Override
  public Enumeration<? extends TransitionItem> children() { return enumeration(trans); }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Iterable">
  @Override
  public Iterator<TransitionItem> iterator() { return trans.iterator(); }
  //</editor-fold>

  @Override
  public String toString()
  {
    final String text = getText(state.getResponse());
    return String.format("%s: %s", state.getName(), text);
  }
}
