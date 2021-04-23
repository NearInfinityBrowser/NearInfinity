// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import static java.util.Collections.enumeration;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

/**
 * Represents node in the dialog tree that contains states, that not accessible
 * from any other dialog
 *
 * @author Mingun
 */
final class OrphanStates extends StateOwnerItem implements Iterable<StateItem>
{
  private final DlgTreeModel parent;
  final List<StateItem> states = new ArrayList<>();

  public OrphanStates(DlgTreeModel parent) { this.parent = parent; }

  @Override
  public String toString() { return "Orphan states"; }

  @Override
  public StateItem getChildAt(int childIndex) { return states.get(childIndex); }

  @Override
  public int getChildCount() { return states.size(); }

  @Override
  public TreeNode getParent() { return parent; }

  @Override
  public int getIndex(TreeNode node) { return states.indexOf(node); }

  @Override
  public boolean getAllowsChildren() { return true; }

  @Override
  public boolean isLeaf() { return states.isEmpty(); }

  @Override
  public Enumeration<? extends StateItem> children() { return enumeration(states); }

  @Override
  public Iterator<StateItem> iterator() { return states.iterator(); }

  @Override
  public TreeItemEntry getEntry() { return null; }

  @Override
  public ItemBase getMain() { return null; }

  @Override
  public DlgResource getDialog() { return parent.getDialog(); }

  @Override
  public Icon getIcon() { return null; }

  @Override
  public boolean removeChild(ItemBase child) { return states.remove(child); }

  @Override
  public void traverseChildren(Consumer<ItemBase> action) { states.forEach(action); }
}
