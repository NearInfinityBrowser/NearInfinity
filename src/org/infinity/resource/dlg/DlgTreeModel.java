// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;

/** Creates and manages the dialog tree structure. */
final class DlgTreeModel implements TreeModel, TreeNode, TableModelListener
{
  private final ArrayList<TreeModelListener> listeners = new ArrayList<>();
  /**
   * List of all dialogs, that contains at tree root. Each of these dialogs also
   * stored in the {@link #linkedDialogs} map, but the opposite is incorrect
   */
  private final List<DlgItem> rootDialogs = new ArrayList<>();
  /** Maps dialog names of dialogs, presented in the tree, to dialog itself. */
  private final HashMap<String, DlgResource> linkedDialogs = new HashMap<>();
  /** Maps dialog entries to main tree items - items wrom which the tree grows. */
  private final HashMap<TreeItemEntry, ItemBase> mainItems = new HashMap<>();
  /** Maps dialog entries to tree items that represents it. Used for update tree when entry changes. */
  private final HashMap<TreeItemEntry, List<ItemBase>> allItems = new HashMap<>();
  private final OrphanStates orphanStates = new OrphanStates(this);
  private final OrphanTransitions orphanTrans = new OrphanTransitions(this);

  public DlgTreeModel(DlgResource dlg) { addToRoot(dlg); }

  @Override
  public String toString() { return "Dialogues"; }

  //<editor-fold defaultstate="collapsed" desc="TreeModel">
  @Override
  public DlgTreeModel getRoot() { return this; }

  @Override
  public ItemBase getChild(Object parent, int index)
  {
    if (parent == this) {
      return getChildAt(index);
    }
    if (parent instanceof ItemBase) {
      final ItemBase child = ((ItemBase)parent).getChildAt(index);
      initNode(child);
      return child;
    }
    return null;
  }

  @Override
  public int getChildCount(Object parent)
  {
    if (parent instanceof TreeNode) {
      return initNode((TreeNode)parent).getChildCount();
    }
    return 0;
  }

  @Override
  public boolean isLeaf(Object node)
  {
    if (node instanceof TreeNode) {
      return initNode((TreeNode)node).isLeaf();
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
      final TreeNode nodeParent = initNode((TreeNode)parent);
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

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public ItemBase getChildAt(int childIndex)
  {
    if (childIndex == rootDialogs.size()) return orphanStates;
    if (childIndex == rootDialogs.size() + 1) return orphanTrans;
    return rootDialogs.get(childIndex);
  }

  @Override
  public int getChildCount() { return rootDialogs.size() + 2; }

  @Override
  public TreeNode getParent() { return null; }

  @Override
  public int getIndex(TreeNode node)
  {
    if (orphanStates == node) return rootDialogs.size();
    if (orphanTrans == node) return rootDialogs.size() + 1;
    return rootDialogs.indexOf(node);
  }

  @Override
  public boolean getAllowsChildren() { return true; }

  @Override
  public boolean isLeaf() { return false; }

  @Override
  public Enumeration<? extends ItemBase> children()
  {
    return new Enumeration<ItemBase>()
    {
      private final Iterator<? extends ItemBase> it1 = rootDialogs.iterator();
      private final Iterator<? extends ItemBase> it2 = asList(orphanStates, orphanTrans).iterator();

      @Override
      public boolean hasMoreElements()
      {
        return it1.hasNext() || it2.hasNext();
      }

      @Override
      public ItemBase nextElement()
      {
        return it1.hasNext() ? it1.next() : it2.next();
      }
    };
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
          nodeChanged(map((DlgResource)src));
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
        final TreeNode node = item.getParent();
        if (!(node instanceof ItemBase)) continue;

        final ItemBase parent = (ItemBase)node;
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
    final int[] childIndices = {parent.getIndex(node)};
    final Object[] children = {node};
    final TreePath path = parent instanceof ItemBase
                        ? ((ItemBase)parent).getPath()
                        : new TreePath(this);

    fireTreeNodesChanged(path, childIndices, children);
  }

  private void fireTreeNodesChanged(TreePath path, int[] childIndices, Object[] children)
  {
    if (!listeners.isEmpty()) {
      final TreeModelEvent event = new TreeModelEvent(this, path, childIndices, children);
      for (final TreeModelListener tml : listeners) {
        tml.treeNodesChanged(event);
      }
    }
  }

  private void fireTreeNodesInserted(TreePath path, int[] childIndices, Object[] children)
  {
    if (!listeners.isEmpty()) {
      final TreeModelEvent event = new TreeModelEvent(this, path, childIndices, children);
      for (final TreeModelListener tml : listeners) {
        tml.treeNodesInserted(event);
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
        for (final DlgItem dlg : rootDialogs) {
          final StateItem state = slowFindState(dlg, (State)entry);
          if (state != null) return state;
        }
        return null;
      }
      for (final DlgItem dlg : rootDialogs) {
        final TransitionItem trans = slowFindTransition(dlg, (Transition)entry);
        if (trans != null) return trans;
      }
    }
    return item;
  }

  /**
   * @return Path the identifying first (main) dialog in model. Never {@code null}
   */
  public TreePath getMainDlgPath()
  {
    return new TreePath(this).pathByAddingChild(rootDialogs.get(0));
  }

  public DlgResource getDialog() { return rootDialogs.get(0).getDialog(); }

  /**
   * Add specified entry to visual tree together with dialog(s), from which it accessible.
   *
   * @param entry Child struct of the dialog to add
   * @return GUI item of new added node. This is always main item
   */
  public ItemBase addToRoot(TreeItemEntry entry)
  {
    final DlgResource dlg = entry.getParent();
    if (entry instanceof State) {
      return addToRoot((State)entry);
    }
    if (entry instanceof Transition) {
      final Transition trans = (Transition)entry;
      final ArrayDeque<State> queue = new ArrayDeque<>();
      if (dlg.findUsages(trans, queue::add)) {
        // First add to the tree first state, that refers to this
        // transition, and then allow map do their work
        addToRoot(queue.pop());
        return map(trans);
      }
      // If transition is not accessible from any dialogue and has not parents,
      // add it under orphaned responses node
      return addOrphaned(trans);
    }
    return null;
  }

  /**
   * Add specified dialog to the tree, if it not yet present there.
   *
   * @param dlg Dialog to add. Can not be {@code null}
   */
  private void addToRoot(DlgResource dlg)
  {
    // If dialog already added as root, return
    if (map(dlg) != null) return;

    final DlgItem item       = new DlgItem(this, dlg);
    final int[] childIndices = {rootDialogs.size()};
    final Object[] children  = {item};

    rootDialogs.add(item);
    linkedDialogs.put(key(dlg.getName()), dlg);
    for (final StateItem state : item) {
      initState(state);
      putItem(state, null);
    }
    dlg.addTableModelListener(this);

    fireTreeNodesInserted(new TreePath(this), childIndices, children);
  }

  /**
   * Add new state to the tree.
   *
   * @param state Reference to the state, that is not accessible from the root of
   *        any dialogue. Can not be {@code null}
   * @return GUI item representing state in the tree
   */
  private StateItem addToRoot(State state)
  {
    final DlgResource dlg = state.getParent();
    final StateItem item = addState(dlg, findParents(state));
    // If state is not accessible from any dialogue, item is null, so add
    // state under orphaned states node
    return item == null ? addOrphaned(state) : item;
  }

  /**
   * Add new state node under {@link #orphanStates "Orphaned states"} node.
   *
   * @param state Reference to the state, that is not accessible from the root of
   *        any dialogue. Can not be {@code null}
   * @return GUI item representing state in the tree
   */
  private StateItem addOrphaned(State state)
  {
    final StateItem item     = new StateItem(state, orphanStates, null);
    final int[] childIndices = {orphanStates.getChildCount()};
    final Object[] children  = {item};

    orphanStates.states.add(item);
    putItem(item, null);
    initState(item);

    fireTreeNodesInserted(orphanStates.getPath(), childIndices, children);
    return item;
  }

  /**
   * Add new response node under {@link #orphanTrans "Orphaned responses"} node.
   *
   * @param trans Reference to the transition, that is not accessible from the
   *        root of any dialogue. Can not be {@code null}
   * @return GUI item representing transition in the tree
   */
  private TransitionItem addOrphaned(Transition trans)
  {
    final TransitionItem item= new TransitionItem(trans, orphanTrans, null);
    final int[] childIndices = {orphanTrans.getChildCount()};
    final Object[] children  = {item};

    orphanTrans.trans.add(item);
    putItem(item, null);
    initTransition(item);

    fireTreeNodesInserted(orphanTrans.getPath(), childIndices, children);
    return item;
  }

  /**
   * Returns all states from the {@code state} dialog from which specified state
   * is accessible and the state itself.
   *
   * @param state Status for which parent statuses are looked for
   * @return Collection of unique states, which forms queue of parent states
   */
  private Set<State> findParents(State state)
  {
    final DlgResource dlg = state.getParent();
    // Use LinkedHashSet to return states in child-parent order
    final LinkedHashSet<State> result = new LinkedHashSet<>();
    final ArrayDeque<State> queue = new ArrayDeque<>();
    result.add(state);
    queue.add(state);
    do {
      dlg.findUsages(queue.pop(), t -> dlg.findUsages(t, queue::add));
      // Stop when no changes was made in result. If result changed, `queue`
      // contains at least one value, so `pop()` on next iteration will not throw
    } while (result.addAll(queue));

    return result;
  }

  /**
   * Looks for all specified states in all dialogs and adds all dialogs in which
   * the reference to a state is found to a tree. The method looks for all specified
   * state for one pass. It allows to process a situation when directly from other
   * dialogs nobody refers to a required state, but refer to one of its parents
   * (for example, a state 32 and 33 in DMorte1.dlg in PS:T).
   *
   * @param dialog Dialog, that owns of all {@code states}. Must not be {@code null}
   * @param states List of states to search. Must not be {@code null}
   * @return GUI item representing state in first dialog or {@code null}, if state
   *         is not found in any dialogue
   */
  private StateItem addState(DlgResource dialog, Set<State> states)
  {
    final HashSet<String> checked = new HashSet<>();
    // First look at linked dialogs, for optimization
    for (int i = 0; ; ++i) {
      final Transition t = dialog.getTransition(i);
      if (t == null) break;

      final DlgResource dlg = getDialogResource(t.getNextDialog());
      // Transition target within this dialog
      if (dlg == null) continue;

      final StateItem item = map(dlg, states);
      if (item != null) return item;
      // This dialog not contains references to any state
      checked.add(key(dlg.getName()));
    }

    // If not found in linked dialogs, search in all dialogs
    for (final ResourceEntry e : ResourceFactory.getResources("DLG")) {
      final String name = key(e.getResourceName());
      // If this dialog in linkedDialogs, it already checked
      if (checked.contains(name)) continue;

      final Resource resource = ResourceFactory.getResource(e);
      // If resource has DLG extension but not DLG resource
      if (!(resource instanceof DlgResource)) continue;

      final StateItem item = map((DlgResource)resource, states);
      if (item != null) return item;
    }
    return null;
  }

  private StateItem map(DlgResource dlg, Set<State> states)
  {
    for (final State state : states) {
      if (dlg.findUsages(state, __ -> addToRoot(dlg))) {
        // For some reason just
        // return map(state);
        // not works, but this code work as expected
        for (final State s : states) {
          final StateItem item = (StateItem)map(s);
          if (item != null) return item;
        }
      }
    }
    return null;
  }

  /**
   * Translates dialog to GUI item, that presents at tree root.
   *
   * @param dlg Dialog to find in the tree
   * @return GUI item or {@code null} if dialog not exists in the tree
   */
  private DlgItem map(DlgResource dlg)
  {
    for (final DlgItem item : rootDialogs) {
      if (item.getDialog() == dlg) {
        return item;
      }
    }
    return null;
  }

  /**
   * Checks that specified GUI item corresponds searched state entry. This method
   * returns {@code true} only for {@link StateItem#getMain main} GUI items.
   *
   * @param queue The queue of transitions containing references to states for check.
   *        If specified {@code state} does not correspond to searched {@code entry}
   *        it will be filled with next candidates for check
   * @param state Checked GUI item
   * @param entry Searched entry
   *
   * @return {@code true} if specified GUI item contains reference to searchd entry,
   *         {@code false} otherwise
   */
  private boolean checkState(ArrayDeque<TransitionItem> queue, StateItem state, State entry)
  {
    if (state.getMain() != null) return false;
    initState(state);

    if (state.getEntry() == entry) {
      return true;
    }
    for (TransitionItem trans : state) {
      if (trans.getMain() == null) {
        queue.add(trans);
      }
    }
    return false;
  }

  /**
   * Finds GUI item that corresponds specified state. Creates non-existent
   * tree items when necessary.
   *
   * @param dlg Dialogue in which to perform search
   * @param entry Child struct of the dialog for search
   * @return Tree item that represents state or {@code null} if such state
   *         did not exist in the dialog
   */
  private StateItem slowFindState(DlgItem dlg, State entry)
  {
    final ArrayDeque<TransitionItem> queue = new ArrayDeque<>();
    for (final StateItem state : dlg) {
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
   * @param dlg Dialogue in which to perform search
   * @param entry Child struct of the dialog for search
   * @return Tree item that represents transition or {@code null} if such transition
   *         did not exist in the dialog
   */
  private TransitionItem slowFindTransition(DlgItem dlg, Transition entry)
  {
    final ArrayDeque<StateItem> queue = new ArrayDeque<>();
    for (final StateItem state : dlg) {
      if (state.getMain() == null) {
        queue.add(state);
      }
    }

    StateItem state;
    while (true) {
      state = queue.poll();
      if (state == null) break;

      initState(state);
      for (TransitionItem trans : state) {
        if (trans.getMain() != null) continue;
        initTransition(trans);

        if (trans.getEntry() == entry) {
          return trans;
        }
        if (trans.nextState != null && trans.nextState.getMain() == null) {
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
      final int start = state.getEntry().getFirstTrans();
      final int count = state.getEntry().getTransCount();

      state.trans = new ArrayList<>(count);
      for (int i = start; i < start + count; ++i) {
        final Transition trans = dlg.getTransition(i);
        if (trans != null) {
          final TransitionItem main = (TransitionItem)mainItems.get(trans);
          final TransitionItem item = new TransitionItem(trans, state, main);

          state.trans.add(item);
          putItem(item, main);
        }
      }
    }
  }

  /** Adds all available child nodes to the given parent node. */
  private void initTransition(TransitionItem trans)
  {
    if (trans.nextState == null) {
      final Transition t = trans.getEntry();
      final DlgResource nextDlg = getDialogResource(t.getNextDialog());

      if (nextDlg != null) {
        final State state = nextDlg.getState(t.getNextDialogState());
        if (state != null) {
          final StateItem main = (StateItem)mainItems.get(state);

          trans.nextState = new StateItem(state, trans, main);
          putItem(trans.nextState, main);
        }
      }
    }
  }

  /** Adds all available child nodes to the given parent node. */
  private TreeNode initNode(TreeNode node)
  {
    if (node.getAllowsChildren()) {
      if (node instanceof StateItem) {
        initState((StateItem)node);
      } else
      if (node instanceof TransitionItem) {
        initTransition((TransitionItem)node);
      }
    }
    return node;
  }

  /**
   * Registers visual tree node in internal maps.
   *
   * @param item The item to register
   * @param main The reference to an item which can have childrens in the break cycles mode
   */
  private <T extends ItemBase> void putItem(T item, T main)
  {
    final TreeItemEntry entry = item.getEntry();
    allItems.computeIfAbsent(entry, i -> new ArrayList<>()).add(item);
    if (main == null) {
      mainItems.put(entry, item);
    }
  }
}
