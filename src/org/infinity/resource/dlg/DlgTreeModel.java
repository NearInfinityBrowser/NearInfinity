// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import java.util.HashMap;
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

  private RootItem root;

  public DlgTreeModel(DlgResource dlg)
  {
    reset(dlg);
  }

//--------------------- Begin Interface TreeModel ---------------------

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
      int idx = listeners.indexOf(l);
      if (idx >= 0) {
        listeners.remove(idx);
      }
    }
  }

//--------------------- End Interface TreeModel ---------------------

  public void nodeChanged(TreeNode node)
  {
    if (node != null) {
      if (node.getParent() == null) {
        fireTreeNodesChanged(this, null, null, null);
      } else {
        fireTreeNodesChanged(this, createNodePath(node.getParent()),
                             new int[]{getChildNodeIndex(node)}, new Object[]{node});
      }
    }
  }

  public void nodeStructureChanged(TreeNode node)
  {
    if (node.getParent() == null) {//Root
      fireTreeStructureChanged(this, null, null, null);
    } else {
      fireTreeStructureChanged(this, createNodePath(node.getParent()),
                               new int[getChildNodeIndex(node)], new Object[]{node});
    }
  }

  /** Removes any old content and re-initializes the model with the data from the given dialog resource. */
  public void reset(DlgResource dlg)
  {
    mainStates.clear();
    linkedDialogs.clear();
    linkedDialogs.put(key(dlg.getName()), dlg);

    root = new RootItem(dlg);
    for (StateItem state : root) {
      mainStates.put(state.getState(), state);
    }

    // notifying listeners
    nodeStructureChanged(root);
  }

  /** Updates tree when specified state entry changed. */
  public void updateState(State state)
  {
    //TODO: fire nodeChanged for linked StateItem
  }

  /** Updates tree when specified transition entry changed. */
  public void updateTransition(Transition trans)
  {
    //TODO: fire nodeChanged for linked TransitionItem
  }

  public void updateRoot()
  {
    nodeChanged(root);
  }

  /** Generates an array of TreeNode objects from root to specified node. */
  private Object[] createNodePath(TreeNode node)
  {
    final TreeNode leaf = node;
    int count = 0;
    while (node != null) {
      ++count;
      node = node.getParent();
    }

    final Object[] retVal = new Object[count];
    node = leaf;
    while (node != null) {
      retVal[--count] = node;
      node = node.getParent();
    }
    return retVal;
  }

  /** Determines the child index based on the specified node's parent. */
  private int getChildNodeIndex(TreeNode node)
  {
    int retVal = 0;
    if (node != null && node.getParent() != null) {
      TreeNode parent = node.getParent();
      for (int i = 0; i < parent.getChildCount(); i++) {
        if (parent.getChildAt(i) == node) {
          retVal = i;
          break;
        }
      }
    }
    return retVal;
  }

  private void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices,
                                    Object[] children)
  {
    if (!listeners.isEmpty()) {
      TreeModelEvent event;
      if (path == null || path.length == 0) {
        event = new TreeModelEvent(source, (TreePath)null);
      } else {
        event = new TreeModelEvent(source, path, childIndices, children);
      }
      for (int i = listeners.size()-1; i >= 0; i--) {
        TreeModelListener tml = listeners.get(i);
        tml.treeNodesChanged(event);
      }
    }
  }

  private void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices,
                                        Object[] children)
  {
    if (!listeners.isEmpty()) {
      TreeModelEvent event;
      if (path == null || path.length == 0) {
        event = new TreeModelEvent(source, (TreePath)null);
      } else {
        event = new TreeModelEvent(source, path, childIndices, children);
      }
      for (int i = listeners.size()-1; i >= 0; i--) {
        TreeModelListener tml = listeners.get(i);
        tml.treeStructureChanged(event);
      }
    }
  }

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
        if (main == null) {
          mainStates.put(state, trans.nextState);
        }
      }
    }
  }
}
