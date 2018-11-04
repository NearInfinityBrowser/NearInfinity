// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
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
  private final HashMap<String, HashMap<Integer, StateItem>> mapState = new HashMap<>();
  /** Maps dialog resources to tables of transition index/item pairs. */
  private final HashMap<String, HashMap<Integer, TransitionItem>> mapTransition = new HashMap<>();

  private RootItem root;
  private DlgResource dlg;
  private DefaultMutableTreeNode nodeRoot;

  public DlgTreeModel(DlgResource dlg)
  {
    reset(dlg);
  }

//--------------------- Begin Interface TreeModel ---------------------

  @Override
  public Object getRoot()
  {
    return updateNodeChildren(nodeRoot);
  }

  @Override
  public Object getChild(Object parent, int index)
  {
    if (parent instanceof DefaultMutableTreeNode) {
      final DefaultMutableTreeNode nodeParent = updateNodeChildren((DefaultMutableTreeNode)parent);
      if (index >= 0 && index < nodeParent.getChildCount()) {
        return updateNodeChildren((DefaultMutableTreeNode)nodeParent.getChildAt(index));
      }
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
    if (node.getParent() == null) {
      fireTreeStructureChanged(this, null, null, null);
    } else {
      fireTreeStructureChanged(this, createNodePath(node.getParent()),
                               new int[getChildNodeIndex(node)], new Object[]{node});
    }
  }

  /** Removes any old content and re-initializes the model with the data from the given dialog resource. */
  public void reset(DlgResource dlg)
  {
    mapState.clear();
    mapTransition.clear();

    this.dlg = dlg;

    root = new RootItem(dlg);
    nodeRoot = new DefaultMutableTreeNode(root, true);

    for (StateItem state : root) {
      initState(state);
    }

    // notifying listeners
    nodeStructureChanged((DefaultMutableTreeNode)getRoot());
  }

  public void updateState(State state)
  {
    if (state != null) {
      final HashMap<Integer, StateItem> map = getStateTable(dlg.getResourceEntry().getResourceName());
      if (map != null) {
        final StateItem item = map.get(state.getNumber());
        if (item != null) {
          item.setState(state);
          triggerNodeChanged((DefaultMutableTreeNode)getRoot(), item);
        }
      }
    }
  }

  public void updateTransition(Transition trans)
  {
    if (trans != null) {
      final HashMap<Integer, TransitionItem> map = getTransitionTable(dlg.getResourceEntry().getResourceName());
      if (map != null) {
        final TransitionItem item = map.get(trans.getNumber());
        if (item != null) {
          item.setTransition(trans);
          triggerNodeChanged((DefaultMutableTreeNode)getRoot(), item);
        }
      }
    }
  }

  public void updateRoot()
  {
    root = new RootItem(dlg);
    nodeRoot.setUserObject(root);
    nodeChanged(nodeRoot);
  }

  /** Recursively parses the tree and triggers a nodeChanged event for each node containing data. */
  private void triggerNodeChanged(DefaultMutableTreeNode node, Object data)
  {
    if (node != null && data != null) {
      if (node.getUserObject() == data) {
        nodeChanged(node);
      }
      for (int i = 0; i < node.getChildCount(); i++) {
        triggerNodeChanged((DefaultMutableTreeNode)node.getChildAt(i), data);
      }
    }
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

//  private void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices,
//                                     Object[] children)
//  {
//    if (!listeners.isEmpty()) {
//      TreeModelEvent event;
//      if (path == null || path.length == 0) {
//        event = new TreeModelEvent(source, (TreePath)null);
//      } else {
//        event = new TreeModelEvent(source, path, childIndices, children);
//      }
//      for (int i = listeners.size()-1; i >= 0; i--) {
//        TreeModelListener tml = listeners.get(i);
//        tml.treeNodesInserted(event);
//      }
//    }
//  }

//  private void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices,
//                                    Object[] children)
//  {
//    if (!listeners.isEmpty()) {
//      TreeModelEvent event;
//      if (path == null || path.length == 0) {
//        event = new TreeModelEvent(source, (TreePath)null);
//      } else {
//        event = new TreeModelEvent(source, path, childIndices, children);
//      }
//      for (int i = listeners.size()-1; i >= 0; i--) {
//        TreeModelListener tml = listeners.get(i);
//        tml.treeNodesRemoved(event);
//      }
//    }
//  }

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

  private void initState(StateItem state)
  {
    final DlgResource dlg = state.getDialog();
    final String dlgName = dlg.getResourceEntry().getResourceName();
    HashMap<Integer, StateItem> map = getStateTable(dlgName);
    if (map == null) {
      map = new HashMap<>();
      setStateTable(dlgName, map);
    }

    final Integer number = state.getState().getNumber();
    if (!map.containsKey(number)) {
      map.put(number, state);

      final int start = state.getState().getFirstTrans();
      final int count = state.getState().getTransCount();
      for (int i = start; i < start + count; ++i) {
        initTransition(new TransitionItem(state, dlg.getTransition(i)));
      }
    }
  }

  private void initTransition(TransitionItem trans)
  {
    final DlgResource dlg = trans.getDialog();
    final String dlgName  = dlg.getResourceEntry().getResourceName();
    HashMap<Integer, TransitionItem> map = getTransitionTable(dlgName);
    if (map == null) {
      map = new HashMap<>();
      setTransitionTable(dlgName, map);
    }

    final Transition t = trans.getTransition();
    final Integer number = t.getNumber();
    if (!map.containsKey(number)) {
      map.put(number, trans);

      if (!t.getFlag().isFlagSet(3)) {
        // dialog continues
        final String nextDlgName  = t.getNextDialog().getResourceName();
        final DlgResource nextDlg = getDialogResource(nextDlgName);
        final int stateIdx = t.getNextDialogState();
        if (nextDlg != null && stateIdx >= 0) {
          initState(new StateItem(nextDlg, trans, null, nextDlg.getState(stateIdx)));
        }
      }
    }
  }

  /**
   * Returns a dialog resource object based on the specified resource name.
   * Reuses exising DlgResource objects if available.
   */
  private DlgResource getDialogResource(String dlgName)
  {
    if (dlgName != null) {
      if (containsStateTable(dlgName)) {
        HashMap<Integer, StateItem> map = getStateTable(dlgName);
        if (!map.keySet().isEmpty()) {
          return map.get(map.keySet().iterator().next()).getDialog();
        }
      } else if (containsTransitionTable(dlgName)) {
        HashMap<Integer, TransitionItem> map = getTransitionTable(dlgName);
        if (!map.keySet().isEmpty()) {
          return map.get(map.keySet().iterator().next()).getDialog();
        }
      } else if (ResourceFactory.resourceExists(dlgName)) {
        try {
          return new DlgResource(ResourceFactory.getResourceEntry(dlgName));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  /** Adds all available child nodes to the given parent node. */
  private DefaultMutableTreeNode updateNodeChildren(DefaultMutableTreeNode parent)
  {
    if (parent != null) {
      final Object obj = parent.getUserObject();
      if (obj instanceof StateItem) {
        return updateStateNodeChildren(parent, (StateItem)obj);
      } else if (obj instanceof TransitionItem) {
        return updateTransitionNodeChildren(parent, (TransitionItem)obj);
      } else if (obj instanceof RootItem) {
        return updateRootNodeChildren(parent, (RootItem)obj);
      }
    }
    return parent;
  }

  /** Adds all available transition child nodes to the given parent state node. */
  private DefaultMutableTreeNode updateStateNodeChildren(DefaultMutableTreeNode parent, StateItem state)
  {
    final String dlgName = state.getDialog().getResourceEntry().getResourceName();
    final HashMap<Integer, TransitionItem> transitions = getTransitionTable(dlgName);
    final int start = state.getState().getFirstTrans();
    final int count = state.getState().getTransCount();

    for (int i = parent.getChildCount(); i < count; ++i) {
      final TransitionItem child = transitions.get(start + i);
      // Flag 3: Terminates dialogue
      final boolean allowChildren = !child.getTransition().getFlag().isFlagSet(3);

      parent.add(new DefaultMutableTreeNode(child, allowChildren));
    }
    return parent;
  }

  /** Adds all available state child nodes to the given parent transition node. */
  private DefaultMutableTreeNode updateTransitionNodeChildren(DefaultMutableTreeNode parent, TransitionItem trans)
  {
    // transitions only allow a single state as child
    if (parent.getChildCount() < 1) {
      final ResourceRef dlgRef = trans.getTransition().getNextDialog();
      if (!dlgRef.isEmpty()) {
        final String dlgName = dlgRef.getResourceName();
        final int stateIdx = trans.getTransition().getNextDialogState();
        final StateItem child = getStateTable(dlgName).get(stateIdx);

        parent.add(new DefaultMutableTreeNode(child, true));
      }
    }
    return parent;
  }

  /** Adds all available initial state child nodes to the given parent root node. */
  private DefaultMutableTreeNode updateRootNodeChildren(DefaultMutableTreeNode parent, RootItem root)
  {
    for (int i = parent.getChildCount(); i < root.getInitialStateCount(); ++i) {
      final StateItem child = root.getInitialState(i);

      parent.add(new DefaultMutableTreeNode(child, true));
    }
    return parent;
  }

  /** Returns the state table of the specified dialog resource. */
  private HashMap<Integer, StateItem> getStateTable(String dlgName)
  {
    if (dlgName != null) {
      return mapState.get(dlgName.toUpperCase(Locale.ENGLISH));
    } else {
      return null;
    }
  }

  /** Adds or replaces a dialog resource entry with its associated state table. */
  private void setStateTable(String dlgName, HashMap<Integer, StateItem> map)
  {
    if (dlgName != null) {
      mapState.put(dlgName.toUpperCase(Locale.ENGLISH), map);
    }
  }

  /** Returns whether the specified dialog resource has been mapped. */
  private boolean containsStateTable(String dlgName)
  {
    if (dlgName != null) {
      return mapState.containsKey(dlgName.toUpperCase(Locale.ENGLISH));
    } else {
      return false;
    }
  }

  /** Returns the transition table of the specified dialog resource. */
  private HashMap<Integer, TransitionItem> getTransitionTable(String dlgName)
  {
    if (dlgName != null) {
      return mapTransition.get(dlgName.toUpperCase(Locale.ENGLISH));
    } else {
      return null;
    }
  }

  /** Adds or replaces a dialog resource entry with its associated transition table. */
  private void setTransitionTable(String dlgName, HashMap<Integer, TransitionItem> map)
  {
    if (dlgName != null) {
      mapTransition.put(dlgName.toUpperCase(Locale.ENGLISH), map);
    }
  }

  /** Returns whether the specified dialog resource has been mapped. */
  private boolean containsTransitionTable(String dlgName)
  {
    if (dlgName != null) {
      return mapTransition.containsKey(dlgName.toUpperCase(Locale.ENGLISH));
    } else {
      return false;
    }
  }
}
