// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;

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
    DefaultMutableTreeNode node = null;
    if (parent instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode)parent;
      nodeParent = updateNodeChildren(nodeParent);
      if (index >= 0 && index < nodeParent.getChildCount()) {
        node = (DefaultMutableTreeNode)nodeParent.getChildAt(index);
      }
    }
    return updateNodeChildren(node);
  }

  @Override
  public int getChildCount(Object parent)
  {
    if (parent instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode)parent;
      nodeParent = updateNodeChildren(nodeParent);
      return nodeParent.getChildCount();
    }
    return 0;
  }

  @Override
  public boolean isLeaf(Object node)
  {
    if (node instanceof DefaultMutableTreeNode) {
      return ((DefaultMutableTreeNode)node).isLeaf();
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
    if (parent instanceof DefaultMutableTreeNode && child instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode)parent;
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
    // clearing maps
    Iterator<String> iter = mapState.keySet().iterator();
    while (iter.hasNext()) {
      HashMap<Integer, StateItem> map = mapState.get(iter.next());
      if (map != null) {
        map.clear();
      }
    }
    mapState.clear();

    iter = mapTransition.keySet().iterator();
    while (iter.hasNext()) {
      HashMap<Integer, TransitionItem> map = mapTransition.get(iter.next());
      if (map != null) {
        map.clear();
      }
    }
    mapTransition.clear();

    root = null;
    nodeRoot = null;

    this.dlg = dlg;

    root = new RootItem(dlg);
    for (int i = 0; i < root.getInitialStateCount(); i++) {
      initState(root.getInitialState(i));
    }
    nodeRoot = new DefaultMutableTreeNode(root, true);

    // notifying listeners
    nodeStructureChanged((DefaultMutableTreeNode)getRoot());
  }

  public void updateState(State state)
  {
    if (state != null) {
      int stateIdx = state.getNumber();
      HashMap<Integer, StateItem> map = getStateTable(dlg.getResourceEntry().getResourceName());
      if (map != null) {
        Iterator<Integer> iter = map.keySet().iterator();
        while (iter.hasNext()) {
          StateItem item = map.get(iter.next());
          if (item != null && item.getState().getNumber() == stateIdx) {
            item.setState(state);
            triggerNodeChanged((DefaultMutableTreeNode)getRoot(), item);
            break;
          }
        }
      }
    }
  }

  public void updateTransition(Transition trans)
  {
    if (trans != null) {
      int transIdx = trans.getNumber();
      HashMap<Integer, TransitionItem> map = getTransitionTable(dlg.getResourceEntry().getResourceName());
      if (map != null) {
        Iterator<Integer> iter = map.keySet().iterator();
        while (iter.hasNext()) {
          TransitionItem item = map.get(iter.next());
          if (item != null && item.getTransition().getNumber() == transIdx) {
            item.setTransition(trans);
            triggerNodeChanged((DefaultMutableTreeNode)getRoot(), item);
            break;
          }
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
    Object[] retVal;
    if (node != null) {
      Stack<TreeNode> stack = new Stack<TreeNode>();
      while (node != null) {
        stack.push(node);
        node = node.getParent();
      }
      retVal = new Object[stack.size()];
      for (int i = 0; i < retVal.length; i++) {
        retVal[i] = stack.pop();
      }
      return retVal;
    } else {
      retVal = new Object[0];
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
    if (state != null) {
      DlgResource dlg = state.getDialog();
      HashMap<Integer, StateItem> map = getStateTable(dlg.getResourceEntry().getResourceName());
      if (map == null) {
        map = new HashMap<>();
        setStateTable(dlg.getResourceEntry().getResourceName(), map);
      }

      if (!map.containsKey(Integer.valueOf(state.getState().getNumber()))) {
        map.put(Integer.valueOf(state.getState().getNumber()), state);

        for (int i = 0; i < state.getState().getTransCount(); i++) {
          int transIdx = state.getState().getFirstTrans() + i;
          StructEntry entry = dlg.getAttribute(Transition.DLG_TRANS + " " + transIdx);
          if (entry instanceof Transition) {
            initTransition(new TransitionItem(dlg, (Transition)entry));
          }
        }
      }
    }
  }

  private void initTransition(TransitionItem trans)
  {
    if (trans != null) {
      DlgResource dlg = trans.getDialog();
      HashMap<Integer, TransitionItem> map = getTransitionTable(dlg.getResourceEntry().getResourceName());
      if (map == null) {
        map = new HashMap<>();
        setTransitionTable(dlg.getResourceEntry().getResourceName(), map);
      }

      if (!map.containsKey(Integer.valueOf(trans.getTransition().getNumber()))) {
        map.put(Integer.valueOf(trans.getTransition().getNumber()), trans);

        if (!trans.getTransition().getFlag().isFlagSet(3)) {
          // dialog continues
          ResourceRef dlgRef = trans.getTransition().getNextDialog();
          int stateIdx = trans.getTransition().getNextDialogState();
          dlg = getDialogResource(dlgRef.getResourceName());
          if (dlg != null && stateIdx >= 0) {
            StructEntry entry = dlg.getAttribute(State.DLG_STATE + " " + stateIdx);
            if (entry instanceof State) {
              initState(new StateItem(dlg, (State)entry));
            }
          }
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
      if (parent.getUserObject() instanceof StateItem) {
        return updateStateNodeChildren(parent);
      } else if (parent.getUserObject() instanceof TransitionItem) {
        return updateTransitionNodeChildren(parent);
      } else if (parent.getUserObject() instanceof RootItem) {
        return updateRootNodeChildren(parent);
      }
    }
    return parent;
  }

  /** Adds all available transition child nodes to the given parent state node. */
  private DefaultMutableTreeNode updateStateNodeChildren(DefaultMutableTreeNode parent)
  {
    if (parent != null && parent.getUserObject() instanceof StateItem) {
      StateItem state = (StateItem)parent.getUserObject();
      String dlgName = state.getDialog().getResourceEntry().getResourceName();
      int count = state.getState().getTransCount();
      while (parent.getChildCount() < count) {
        int transIdx = state.getState().getFirstTrans() + parent.getChildCount();
        TransitionItem child = getTransitionTable(dlgName).get(Integer.valueOf(transIdx));
        boolean allowChildren = !child.getTransition().getFlag().isFlagSet(3);
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode(child, allowChildren);
        parent.add(nodeChild);
      }
    }
    return parent;
  }

  /** Adds all available state child nodes to the given parent transition node. */
  private DefaultMutableTreeNode updateTransitionNodeChildren(DefaultMutableTreeNode parent)
  {
    if (parent != null && parent.getUserObject() instanceof TransitionItem) {
      // transitions only allow a single state as child
      if (parent.getChildCount() < 1) {
        TransitionItem trans = (TransitionItem)parent.getUserObject();
        ResourceRef dlgRef = trans.getTransition().getNextDialog();
        if (!dlgRef.isEmpty()) {
          String dlgName = dlgRef.getResourceName();
          int stateIdx = trans.getTransition().getNextDialogState();
          StateItem child = getStateTable(dlgName).get(Integer.valueOf(stateIdx));
          DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode(child, true);
          parent.add(nodeChild);
        }
      }
    }
    return parent;
  }

  /** Adds all available initial state child nodes to the given parent root node. */
  private DefaultMutableTreeNode updateRootNodeChildren(DefaultMutableTreeNode parent)
  {
    if (parent != null && parent.getUserObject() instanceof RootItem) {
      RootItem root = (RootItem)parent.getUserObject();
      while (parent.getChildCount() < root.getInitialStateCount()) {
        int stateIdx = parent.getChildCount();
        StateItem child = root.getInitialState(stateIdx);
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode(child, true);
        parent.add(nodeChild);
      }
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
