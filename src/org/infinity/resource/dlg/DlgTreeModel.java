// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;

/** Creates and manages the dialog tree structure. */
final class DlgTreeModel implements TreeModel, TableModelListener
{
  private final ArrayList<TreeModelListener> listeners = new ArrayList<>();
  /** Maps used dialog names to dialog resources. */
  private final HashMap<String, DlgResource> linkedDialogs = new HashMap<>();
  /** Maps dialog entries to main tree items - items wrom which the tree grows. */
  private final HashMap<TreeItemEntry, ItemBase> mainItems = new HashMap<>();
  /** Maps dialog entries  to tree items that represents it. Used for update tree when entry changes. */
  private final HashMap<TreeItemEntry, List<ItemBase>> allItems = new HashMap<>();

  private final RootItem root;

  public DlgTreeModel(DlgResource dlg)
  {
    linkedDialogs.put(key(dlg.getName()), dlg);

    root = new RootItem(dlg);
    for (StateItem state : root) {
      initState(state);
      allItems.computeIfAbsent(state.getState(), s -> new ArrayList<>()).add(state);
      mainItems.put(state.getState(), state);
    }
    dlg.addTableModelListener(this);
  }

  //<editor-fold defaultstate="collapsed" desc="TreeModel">
  @Override
  public RootItem getRoot() { return root; }

  @Override
  public ItemBase getChild(Object parent, int index)
  {
    if (parent instanceof ItemBase) {
      final ItemBase child = ((ItemBase)parent).getChildAt(index);
      if (child instanceof StateItem) {
        initState((StateItem)child);
      } else
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

  //<editor-fold defaultstate="collapsed" desc="TableModelListener">
  @Override
  public void tableChanged(TableModelEvent e)
  {
    final Object src = e.getSource();
    // TODO: Insertion or removal of nodes not yet fully supported
    switch (e.getType()) {
      case TableModelEvent.UPDATE: {
        if (src instanceof TreeItemEntry) {
          updateTreeItemEntry((TreeItemEntry)src);
        } else
        if (src instanceof DlgResource) {
          nodeChanged(root);
        }
        break;
      }
      case DlgResource.WILL_BE_DELETE: {
        final DlgResource dlg = (DlgResource)src;
        for (int i = e.getLastRow(); i >= e.getFirstRow(); --i) {
          final StructEntry field = dlg.getField(i);
          if (field instanceof TreeItemEntry) {
            //TODO: Can optimize algorithm and generate fewer events
            removeTreeItemEntry((TreeItemEntry)field);
          }
          //TODO: update nodes when trigger is deleted
        }
        break;
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Events">
  /**
   * Updates tree when specified state or transition entry changed.
   *
   * @param entry Tree entry for which all visual nodes must be updated
   */
  private void updateTreeItemEntry(TreeItemEntry entry)
  {
    final List<ItemBase> items = allItems.get(entry);
    if (items != null) {
      for (ItemBase item : items) {
        nodeChanged(item);
      }
    }
  }

  /**
   * Updates tree when specified state or transition is removed.
   *
   * @param entry Tree entry for which all visual nodes must be removed from the tree
   */
  private void removeTreeItemEntry(TreeItemEntry entry)
  {
    mainItems.remove(entry);
    final List<ItemBase> items = allItems.remove(entry);
    if (items != null) {
      for (final ItemBase item : items) {
        final ItemBase parent = item.getParent();
        final int index = parent.getIndex(item);

        parent.removeChild(item);
        if (index >= 0) {
          // In break cycles mode items that begins cycle is invisible, but exists.
          // For such items index is < 0
          fireTreeNodesRemoved(parent.getPath(), new int[]{index}, new Object[]{item});
        }
      }
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
        final TreeModelListener tml = listeners.get(i);
        tml.treeNodesChanged(event);
      }
    }
  }

  private void fireTreeNodesRemoved(TreePath path, int[] childIndices, Object[] children)
  {
    if (!listeners.isEmpty()) {
      final TreeModelEvent event = new TreeModelEvent(this, path, childIndices, children);
      for (int i = listeners.size()-1; i >= 0; i--) {
        final TreeModelListener tml = listeners.get(i);
        tml.treeNodesRemoved(event);
      }
    }
  }
  //</editor-fold>
  //</editor-fold>

  /**
   * Translates child struct of the dialog that this tree represents, to GUI item.
   * For {@link State states} returns main state.
   *
   * @param entry Child struct of the dialog for search
   * @return GUI item or {@code null} if such item not have GUI element
   */
  public ItemBase map(TreeItemEntry entry)
  {
    final ItemBase item = mainItems.get(entry);
    if (item == null) {
      if (entry instanceof State) {
        return slowFindState((State)entry);
      }
      return slowFindTransition((Transition)entry);
    }
    return item;
  }

  private boolean checkState(ArrayDeque<TransitionItem> queue, StateItem state, State entry)
  {
    if (state.getState() == entry) {
      return true;
    }
    for (TransitionItem trans : state) {
      queue.add(trans);
    }
    return false;
  }

  /**
   * Finds GUI item that corresponds specified state. Creates non-existent
   * tree items when necessary.
   *
   * @param entry Child struct of the dialog for search
   * @return Tree item that represents state or {@code null} if such state
   *         did not exist in the dialog
   */
  private StateItem slowFindState(State entry)
  {
    final ArrayDeque<TransitionItem> queue = new ArrayDeque<>();
    for (StateItem state : root) {
      if (checkState(queue, state, entry)) {
        return state;
      }
    }

    TransitionItem trans;
    while (true) {
      trans = queue.poll();
      if (trans == null) break;

      initTransition(trans);
      if (trans.nextState != null && checkState(queue, trans.nextState, entry)) {
        return trans.nextState;
      }
    }
    return null;
  }

  /**
   * Finds GUI item that corresponds specified transition. Creates non-existent
   * tree items when necessary.
   *
   * @param entry Child struct of the dialog for search
   * @return Tree item that represents transition or {@code null} if such transition
   *         did not exist in the dialog
   */
  private TransitionItem slowFindTransition(Transition entry)
  {
    final ArrayDeque<StateItem> queue = new ArrayDeque<>();
    for (StateItem state : root) {
      queue.add(state);
    }

    StateItem state;
    while (true) {
      state = queue.poll();
      if (state == null) break;

      for (TransitionItem trans : state) {
        if (trans.getTransition() == entry) {
          return trans;
        }
        initTransition(trans);
        if (trans.nextState != null) {
          queue.add(trans.nextState);
        }
      }
    }
    return null;
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
  private void initState(StateItem state)
  {
    if (state.trans == null) {
      final DlgResource dlg = state.getDialog();
      final int start = state.getState().getFirstTrans();
      final int count = state.getState().getTransCount();

      state.trans = new ArrayList<>(count);
      for (int i = start; i < start + count; ++i) {
        final Transition trans = dlg.getTransition(i);
        final TransitionItem main = (TransitionItem)mainItems.get(trans);
        final TransitionItem item = new TransitionItem(state, trans, main);

        state.trans.add(item);
        allItems.computeIfAbsent(trans, t -> new ArrayList<>()).add(item);
        if (main == null) {
          mainItems.put(trans, item);
        }
      }
    }
  }

  /** Adds all available child nodes to the given parent node. */
  private void initTransition(TransitionItem trans)
  {
    if (trans.nextState == null) {
      final Transition t = trans.getTransition();
      final DlgResource nextDlg = getDialogResource(t.getNextDialog());

      if (nextDlg != null) {
        final State state = nextDlg.getState(t.getNextDialogState());
        final StateItem main = (StateItem)mainItems.get(state);

        trans.nextState = new StateItem(state, trans, main);
        allItems.computeIfAbsent(state, s -> new ArrayList<>()).add(trans.nextState);
        if (main == null) {
          mainItems.put(state, trans.nextState);
        }
      }
    }
  }
}
