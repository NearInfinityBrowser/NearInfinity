// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;

/** Creates and manages the dialog tree structure. */
final class DlgTreeModel implements TreeModel
{
  private final ArrayList<TreeModelListener> listeners = new ArrayList<>();
  /** Maps dialog resources to tables of state index/item pairs. */
  private final HashMap<State, StateItem> mainStates = new HashMap<>();
  /** Maps used dialog names to dialog resources. */
  private final HashMap<String, DlgResource> linkedDialogs = new HashMap<>();

  /** Maps state to tree items that represents it. Used for update tree when state changed. */
  private final HashMap<State, List<StateItem>> allStates = new HashMap<>();
  /** Maps transition to tree item that represents it. Used for update tree when transition changed. */
  private final HashMap<Transition, TransitionItem> allTransitions = new HashMap<>();

  private final RootItem root;

  public DlgTreeModel(DlgResource dlg)
  {
    linkedDialogs.put(key(dlg.getName()), dlg);

    root = new RootItem(dlg);
    for (StateItem state : root) {
      putState(state);
      mainStates.put(state.getState(), state);
    }
  }

  //<editor-fold defaultstate="collapsed" desc="TreeModel">
  @Override
  public RootItem getRoot() { return root; }

  @Override
  public ItemBase getChild(Object parent, int index)
  {
    if (parent instanceof ItemBase) {
      final ItemBase child = ((ItemBase)parent).getChildAt(index);
      if (child instanceof TransitionItem) {
        initTransition((TransitionItem)child);
      }
      return child;
    }
    return null;
  }

  @Override
  public int getChildCount(Object parent)
  {
    if (parent instanceof TreeNode) {
      return ((TreeNode)parent).getChildCount();
    }
    return 0;
  }

  @Override
  public boolean isLeaf(Object node)
  {
    if (node instanceof TreeNode) {
      return ((TreeNode)node).isLeaf();
    }
    return false;
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue)
  {
    // immutable
  }

  @Override
  public int getIndexOfChild(Object parent, Object child)
  {
    if (parent instanceof TreeNode && child instanceof TreeNode) {
      final TreeNode nodeParent = (TreeNode)parent;
      for (int i = 0; i < nodeParent.getChildCount(); i++) {
        TreeNode nodeChild = nodeParent.getChildAt(i);
        if (nodeChild == child) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public void addTreeModelListener(TreeModelListener l)
  {
    if (l != null && !listeners.contains(l)) {
      listeners.add(l);
    }
  }

  @Override
  public void removeTreeModelListener(TreeModelListener l)
  {
    if (l != null) {
      listeners.remove(l);
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Events">
  /** Updates tree when specified state entry changed. */
  public void updateState(State state)
  {
    final List<StateItem> states = allStates.get(state);
    if (states != null) {
      for (StateItem item : states) {
        nodeChanged(item);
      }
    }
  }

  /** Updates tree when specified transition entry changed. */
  public void updateTransition(Transition trans)
  {
    final TransitionItem item = allTransitions.get(trans);
    if (item != null) {
      nodeChanged(item);
    }
  }

  public void updateRoot()
  {
    nodeChanged(root);
  }

  private void putState(StateItem state)
  {
    allStates.computeIfAbsent(state.getState(), s -> new ArrayList<>()).add(state);
    for (TransitionItem trans : state) {
      allTransitions.put(trans.getTransition(), trans);
    }
  }

  //<editor-fold defaultstate="collapsed" desc="Event emitting">
  private void nodeChanged(ItemBase node)
  {
    final TreeNode parent = node.getParent();
    final Object[] children = {node};
    if (parent == null) {
      fireTreeNodesChanged(null, null, children);
    } else {
      fireTreeNodesChanged(node.getPath(), new int[]{parent.getIndex(node)}, children);
    }
  }

  private void fireTreeNodesChanged(TreePath path, int[] childIndices, Object[] children)
  {
    if (!listeners.isEmpty()) {
      final TreeModelEvent event = new TreeModelEvent(this, path, childIndices, children);
      for (int i = listeners.size()-1; i >= 0; i--) {
        TreeModelListener tml = listeners.get(i);
        tml.treeNodesChanged(event);
      }
    }
  }
  //</editor-fold>
  //</editor-fold>

  /**
   * Returns a dialog resource object based on the specified resource name.
   * Reuses exising DlgResource objects if available.
   */
  private DlgResource getDialogResource(ResourceRef dlgRef)
  {
    return dlgRef.isEmpty() ? null : linkedDialogs.computeIfAbsent(
            key(dlgRef.getResourceName()),
            name -> {
              try {
                return new DlgResource(ResourceFactory.getResourceEntry(name));
              } catch (Exception e) {
                e.printStackTrace();
              }
              return null;
            }
    );
  }

  /** Returns key for cache of {@link DlgResource}'s. */
  private static String key(String dlgName) { return dlgName.toUpperCase(Locale.ENGLISH); }

  /** Adds all available child nodes to the given parent node. */
  private void initTransition(TransitionItem trans)
  {
    if (trans.nextState == null) {
      final Transition t = trans.getTransition();
      final DlgResource nextDlg = getDialogResource(t.getNextDialog());

      if (nextDlg != null) {
        final State state = nextDlg.getState(t.getNextDialogState());
        final StateItem main = mainStates.get(state);

        trans.nextState = new StateItem(state, trans, main);
        putState(trans.nextState);
        if (main == null) {
          mainStates.put(state, trans.nextState);
        }
      }
    }
  }
}
