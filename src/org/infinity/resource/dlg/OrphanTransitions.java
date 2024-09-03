// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import static java.util.Collections.enumeration;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

/**
 * Represents node in the dialog tree that contains transitions, that not accessible from any other dialog
 *
 * @author Mingun
 */
public class OrphanTransitions extends TransitionOwnerItem {
  private final DlgTreeModel parent;

  protected final List<TransitionItem> trans = new ArrayList<>();

  public OrphanTransitions(DlgTreeModel parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return "Orphan responses";
  }

  @Override
  public TransitionItem getChildAt(int childIndex) {
    return trans.get(childIndex);
  }

  @Override
  public int getChildCount() {
    return trans.size();
  }

  @Override
  public TreeNode getParent() {
    return parent;
  }

  @Override
  public int getIndex(TreeNode node) {
    if (node instanceof TransitionItem) {
      return trans.indexOf(node);
    }
    return -1;
  }

  @Override
  public boolean getAllowsChildren() {
    return true;
  }

  @Override
  public boolean isLeaf() {
    return trans.isEmpty();
  }

  @Override
  public Enumeration<? extends TransitionItem> children() {
    return enumeration(trans);
  }

  @Override
  public Iterator<TransitionItem> iterator() {
    return trans.iterator();
  }

  @Override
  public TreeItemEntry getEntry() {
    return null;
  }

  @Override
  public ItemBase getMain() {
    return null;
  }

  @Override
  public DlgResource getDialog() {
    return parent.getDialog();
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public boolean removeChild(ItemBase child) {
    if (child instanceof TransitionItem) {
      return trans.remove(child);
    }
    return false;
  }

  @Override
  public void traverseChildren(Consumer<ItemBase> action) {
    trans.forEach(action);
  }
}
