// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.NearInfinity;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.gui.BrowserMenuBar;
import infinity.gui.ViewFrame;
import infinity.gui.ViewerUtil;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.StringResource;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


/** Show dialog content as tree structure. */
final class TreeViewer extends JPanel implements ActionListener, TreeSelectionListener,
                                                 TableModelListener, PropertyChangeListener
{
  // Max. node depth allowed to search or expand the tree model
  private static final int MAX_DEPTH = 32;

  private final JPopupMenu pmTree = new JPopupMenu();
  private final JMenuItem miExpandAll = new JMenuItem("Expand all nodes");
  private final JMenuItem miExpand = new JMenuItem("Expand selected node");
  private final JMenuItem miCollapseAll = new JMenuItem("Collapse all nodes");
  private final JMenuItem miCollapse = new JMenuItem("Collapse selected nodes");
  private final JMenuItem miEditEntry = new JMenuItem("Edit selected entry");

  // caches ViewFrame instances used to display external dialog entries
  private final HashMap<String, ViewFrame> mapViewer = new HashMap<String, ViewFrame>();

  private final DlgResource dlg;
  private final DlgTreeModel dlgModel;
  private final JTree dlgTree;
  private final ItemInfo dlgInfo;

  private JScrollPane spInfo, spTree;
  private TreeWorker worker;
  private WindowBlocker blocker;


  TreeViewer(DlgResource dlg)
  {
    super(new BorderLayout());
    this.dlg = dlg;
    this.dlg.addTableModelListener(this);
    dlgModel = new DlgTreeModel(this.dlg);
    dlgTree = new JTree();
    dlgTree.addTreeSelectionListener(this);
    dlgInfo = new ItemInfo();
    initControls();
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == miEditEntry) {
      TreePath path = dlgTree.getSelectionPath();
      if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        if (node.getUserObject() instanceof ItemBase) {
          ItemBase item = (ItemBase)node.getUserObject();
          boolean isExtern = (!item.getDialogName().equals(dlg.getResourceEntry().getResourceName()));
          if (isExtern) {
            ViewFrame vf = mapViewer.get(item.getDialogName().toUpperCase(Locale.ENGLISH));
            // reuseing external dialog window if possible
            if (vf != null && vf.isVisible()) {
              vf.toFront();
            } else {
              vf = new ViewFrame(this, item.getDialog());
              mapViewer.put(item.getDialogName().toUpperCase(Locale.ENGLISH), vf);
            }
          }

          if (item.getDialog().getViewer() != null) {
            // selecting table entry
            Viewer viewer = (Viewer)item.getDialog().getViewerTab(0);
            if (node.getUserObject() instanceof RootItem) {
              item.getDialog().getViewer().selectEntry(0);
            } else if (node.getUserObject() instanceof StateItem) {
              int stateIdx = ((StateItem)item).getState().getNumber();
              item.getDialog().getViewer().selectEntry(String.format(State.FMT_NAME, stateIdx));
              viewer.showStateWithStructEntry(((StateItem)item).getState());
            } else if (node.getUserObject() instanceof TransitionItem) {
              int transIdx = ((TransitionItem)item).getTransition().getNumber();
              item.getDialog().getViewer().selectEntry(String.format(Transition.FMT_NAME, transIdx));
              viewer.showStateWithStructEntry(((TransitionItem)item).getTransition());
            }
            item.getDialog().selectEditTab();
          }
        }
      }
    } else if (e.getSource() == miExpandAll) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.Expand, new TreePath(dlgModel.getRoot()));
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    } else if (e.getSource() == miCollapseAll) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.Collapse, new TreePath(dlgModel.getRoot()));
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    } else if (e.getSource() == miExpand) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.Expand, dlgTree.getSelectionPath());
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    } else if (e.getSource() == miCollapse) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.Collapse, dlgTree.getSelectionPath());
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface TreeSelectionListener ---------------------

  @Override
  public void valueChanged(TreeSelectionEvent e)
  {
    if (e.getSource() == dlgTree) {
      Object node = dlgTree.getLastSelectedPathComponent();
      if (node instanceof DefaultMutableTreeNode) {
        Object data = ((DefaultMutableTreeNode)node).getUserObject();
        if (data instanceof StateItem) {
          // dialog state found
          updateStateInfo((StateItem)data);
        } else if (data instanceof TransitionItem) {
          // dialog response found
          updateTransitionInfo((TransitionItem)data);
        } else {
          // no valid type found
          dlgInfo.showPanel(ItemInfo.CARD_EMPTY);
        }
      } else {
        // no node selected
        dlgInfo.showPanel(ItemInfo.CARD_EMPTY);
      }
    }
  }

//--------------------- End Interface TreeSelectionListener ---------------------

//--------------------- Begin Interface TableModelListener ---------------------

 @Override
 public void tableChanged(TableModelEvent e)
 {
   // Insertion or removal of nodes not yet supported
   if (e.getType() == TableModelEvent.UPDATE) {
     if (e.getSource() instanceof State) {
       State state = (State)e.getSource();
       dlgModel.updateState(state);
     } else if (e.getSource() instanceof Transition) {
       Transition trans = (Transition)e.getSource();
       dlgModel.updateTransition(trans);
     } else if (e.getSource() instanceof DlgResource) {
       dlgModel.updateRoot();
     }
   }
 }

//--------------------- End Interface TableModelListener ---------------------

//--------------------- Begin Interface PropertyChangeListener ---------------------

 @Override
 public void propertyChange(PropertyChangeEvent event)
 {
   if (event.getSource() == worker) {
     if ("state".equals(event.getPropertyName()) &&
         TreeWorker.StateValue.DONE == event.getNewValue()) {
       if (blocker != null) {
         blocker.setBlocked(false);
         blocker = null;
       }
       worker = null;
     }
   }
 }

//--------------------- End Interface PropertyChangeListener ---------------------

  private void updateStateInfo(StateItem si)
  {
    if (si != null && si.getDialog() != null && si.getState() != null) {
      DlgResource curDlg = si.getDialog();
      State state = si.getState();
      boolean showStrrefs = BrowserMenuBar.getInstance().showStrrefs();

      // updating info box title
      StringBuilder sb = new StringBuilder(state.getName() + ", ");
      if (curDlg != dlg) {
        sb.append(String.format("Dialog: %1$s, ", curDlg.getResourceEntry().getResourceName()));
      }
      sb.append(String.format("Responses: %1$d", state.getTransCount()));
      if (state.getTriggerIndex() >= 0) {
        sb.append(String.format(", Weight: %1$d", state.getTriggerIndex()));
      }
      dlgInfo.updateControlBorder(ItemInfo.Type.STATE, sb.toString());

      // updating state text
      dlgInfo.showControl(ItemInfo.Type.STATE_TEXT, true);
      dlgInfo.updateControlText(ItemInfo.Type.STATE_TEXT,
                                StringResource.getStringRef(state.getResponse().getValue(),
                                                            showStrrefs));

      // updating state triggers
      if (state.getTriggerIndex() >= 0) {
        dlgInfo.showControl(ItemInfo.Type.STATE_TRIGGER, true);
        StructEntry entry = curDlg.getAttribute(String.format(StateTrigger.FMT_NAME, state.getTriggerIndex()));
        if (entry instanceof StateTrigger) {
          dlgInfo.updateControlText(ItemInfo.Type.STATE_TRIGGER, ((StateTrigger)entry).toString());
        } else {
          dlgInfo.updateControlText(ItemInfo.Type.STATE_TRIGGER, "");
        }
      } else {
        dlgInfo.showControl(ItemInfo.Type.STATE_TRIGGER, false);
      }

      dlgInfo.showPanel(ItemInfo.CARD_STATE);

      // jumping to top of scroll area
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() { spInfo.getVerticalScrollBar().setValue(0); }
      });
    } else {
      dlgInfo.showPanel(ItemInfo.CARD_EMPTY);
    }
  }

  private void updateTransitionInfo(TransitionItem ti)
  {
    if (ti != null && ti.getDialog() != null && ti.getTransition() != null) {
      DlgResource curDlg = ti.getDialog();
      Transition trans = ti.getTransition();
      boolean showStrrefs = BrowserMenuBar.getInstance().showStrrefs();
      StructEntry entry;

      // updating info box title
      StringBuilder sb = new StringBuilder(trans.getName());
      if (curDlg != dlg) {
        sb.append(String.format(", Dialog: %1$s", curDlg.getResourceEntry().getResourceName()));
      }
      dlgInfo.updateControlBorder(ItemInfo.Type.RESPONSE, sb.toString());

      // updating flags
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_FLAGS, true);
      dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_FLAGS, trans.getFlag().toString());

      // updating response text
      if (trans.getFlag().isFlagSet(0)) {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_TEXT, true);
        dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_TEXT,
                                  StringResource.getStringRef(trans.getAssociatedText().getValue(),
                                                              showStrrefs));
      } else {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_TEXT, false);
      }

      // updating journal entry
      if (trans.getFlag().isFlagSet(4)) {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_JOURNAL, true);
        dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_JOURNAL,
                                  StringResource.getStringRef(trans.getJournalEntry().getValue(),
                                                              showStrrefs));
      } else {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_JOURNAL, false);
      }

      // updating response trigger
      if (trans.getFlag().isFlagSet(1)) {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_TRIGGER, true);
        entry = curDlg.getAttribute(String.format(ResponseTrigger.FMT_NAME, trans.getTriggerIndex()));
        if (entry instanceof ResponseTrigger) {
          dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_TRIGGER, ((ResponseTrigger)entry).toString());
        } else {
          dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_TRIGGER, "");
        }
      } else {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_TRIGGER, false);
      }

      // updating action
      if (trans.getFlag().isFlagSet(2)) {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_ACTION, true);
        entry = curDlg.getAttribute(String.format(Action.FMT_NAME, trans.getActionIndex()));
        if (entry instanceof Action) {
          dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_ACTION, ((Action)entry).toString());
        } else {
          dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_ACTION, "");
        }
      } else {
        dlgInfo.showControl(ItemInfo.Type.RESPONSE_ACTION, false);
      }

      dlgInfo.showPanel(ItemInfo.CARD_RESPONSE);

      // jumping to top of scroll area
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() { spInfo.getVerticalScrollBar().setValue(0); }
      });
    } else {
      dlgInfo.showPanel(ItemInfo.CARD_EMPTY);
    }
  }

  private void initControls()
  {
    // initializing info component
    spInfo = new JScrollPane(dlgInfo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    spInfo.getViewport().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e)
      {
        // never scroll horizontally
        JViewport vp = (JViewport)e.getSource();
        if (vp != null) {
          Dimension d = vp.getExtentSize();
          if (d.width != vp.getView().getWidth()) {
            d.height = vp.getView().getHeight();
            vp.getView().setSize(d);
          }
        }
      }
    });
    spInfo.getVerticalScrollBar().setUnitIncrement(16);

    // initializing tree component
    JPanel pTree = new JPanel(new GridBagLayout());
    pTree.setBackground(dlgTree.getBackground());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pTree.add(dlgTree, gbc);
    spTree = new JScrollPane(pTree);
    spTree.setBorder(BorderFactory.createEmptyBorder());
    spTree.getHorizontalScrollBar().setUnitIncrement(16);
    spTree.getVerticalScrollBar().setUnitIncrement(16);

    dlgTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    dlgTree.setRootVisible(true);
    dlgTree.setEditable(false);
    DefaultTreeCellRenderer tcr = (DefaultTreeCellRenderer)dlgTree.getCellRenderer();
    tcr.setLeafIcon(null);
    tcr.setOpenIcon(null);
    tcr.setClosedIcon(null);

    // drawing custom icons for each node type
    dlgTree.setCellRenderer(new DefaultTreeCellRenderer() {
      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                    boolean expanded, boolean leaf, int row,
                                                    boolean focused)
      {
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
        if (value instanceof DefaultMutableTreeNode) {
          Object data = ((DefaultMutableTreeNode)value).getUserObject();
          if (data instanceof ItemBase) {
            setIcon(((ItemBase)data).getIcon());
          } else {
            setIcon(null);
          }
        }
        return c;
      }
    });

    // preventing root node from collapsing
    dlgTree.addTreeWillExpandListener(new TreeWillExpandListener() {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
      {
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
      {
        if (event.getPath().getLastPathComponent() == dlgModel.getRoot()) {
          throw new ExpandVetoException(event);
        }
      }
    });

    // setting model AFTER customizing visual appearance of the tree control
    dlgTree.setModel(dlgModel);

    // initializing popup menu
    miEditEntry.addActionListener(this);
    miEditEntry.setEnabled(!dlgTree.isSelectionEmpty());
    miExpand.addActionListener(this);
    miExpand.setEnabled(!dlgTree.isSelectionEmpty());
    miCollapse.addActionListener(this);
    miCollapse.setEnabled(!dlgTree.isSelectionEmpty());
    miExpandAll.addActionListener(this);
    miCollapseAll.addActionListener(this);
    pmTree.add(miEditEntry);
    pmTree.addSeparator();
    pmTree.add(miExpand);
    pmTree.add(miCollapse);
    pmTree.add(miExpandAll);
    pmTree.add(miCollapseAll);
    dlgTree.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mousePressed(MouseEvent e) { maybeShowPopup(e); }

      @Override
      public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

      private void maybeShowPopup(MouseEvent e)
      {
        if (e.getSource() == dlgTree && e.isPopupTrigger()) {
          miEditEntry.setEnabled(!dlgTree.isSelectionEmpty());
          miExpand.setEnabled(!dlgTree.isSelectionEmpty() &&
                              !isNodeExpanded(dlgTree.getSelectionPath()) &&
                              dlgTree.getSelectionPath().getPathCount() > 1);
          miCollapse.setEnabled(!dlgTree.isSelectionEmpty() &&
                                !isNodeCollapsed(dlgTree.getSelectionPath()) &&
                                dlgTree.getSelectionPath().getPathCount() > 1);

          pmTree.show(dlgTree, e.getX(), e.getY());
        }
      }
    });

    // putting components together
    JSplitPane splitv = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTree, spInfo);
    splitv.setDividerLocation(2 * NearInfinity.getInstance().getContentPane().getHeight() / 5);
    add(splitv, BorderLayout.CENTER);
  }

  // Expands all children and their children of the given path
  private void expandNode(TreePath path, int maxDepth)
  {
    final TreePath curPath = path;
    if (worker != null && worker.userCancelled()) return;
    if (path != null && maxDepth > path.getPathCount()) {
      TreeNode node = (TreeNode)path.getLastPathComponent();

      if (worker != null) { worker.advanceProgress(); }
      if (!dlgTree.isExpanded(path)) {
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() { dlgTree.expandPath(curPath); }
          });
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }

      for (int i = 0; i < node.getChildCount(); i++) {
        expandNode(curPath.pathByAddingChild(node.getChildAt(i)), maxDepth);
        if (worker != null && worker.userCancelled()) return;
      }
    }
  }

  // Collapses all children and their children of the given path
  private void collapseNode(TreePath path, int maxDepth)
  {
    final TreePath curPath = path;
    if (worker != null && worker.userCancelled()) return;
    if (path != null) {
      if (maxDepth > path.getPathCount()) {
        TreeNode node = (TreeNode)path.getLastPathComponent();

        for (int i = 0; i < node.getChildCount(); i++) {
          collapseNode(curPath.pathByAddingChild(node.getChildAt(i)), maxDepth);
          if (worker != null && worker.userCancelled()) return;
        }
      }

      if (worker != null) { worker.advanceProgress(); }
      if (!dlgTree.isCollapsed(path)) {
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() { dlgTree.collapsePath(curPath); }
          });
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
  }

  // Returns true if the given path contains expanded nodes
  private boolean isNodeExpanded(TreePath path)
  {
    boolean retVal = true;
    if (path != null) {
      // evaluating current node
      TreeNode node = (TreeNode)path.getLastPathComponent();
      if (!node.isLeaf()) {
        retVal = dlgTree.isExpanded(path);

        // traversing child nodes
        if (retVal) {
          for (int i = 0; i < node.getChildCount(); i++) {
            retVal = isNodeExpanded(path.pathByAddingChild(node.getChildAt(i)));
            if (!retVal) break;
          }
        }
      }
    }
    return retVal;
  }

  // Returns true if the given path contains collapsed nodes
  private boolean isNodeCollapsed(TreePath path)
  {
    boolean retVal = true;
    if (path != null) {
      // evaluating current node
      TreeNode node = (TreeNode)path.getLastPathComponent();
      if (!node.isLeaf()) {
        retVal = dlgTree.isCollapsed(path);

        // traversing child nodes
        if (retVal) {
          for (int i = 0; i < node.getChildCount(); i++) {
            retVal = isNodeCollapsed(path.pathByAddingChild(node.getChildAt(i)));
            if (retVal) break;
          }
        }
      }
    }
    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  // Applies expand or collapse operations on a set of dialog tree nodes in a background task
  private static class TreeWorker extends SwingWorker<Void, Void>
  {
    // Display short notice after expanding more than this number of nodes
    private static final int MAX_NODE_WAIT = 5000;

    // Supported operations
    public enum Type { Expand, Collapse }

    private final TreeViewer instance;
    private final Type type;
    private final TreePath path;

    private ProgressMonitor progress;

    public TreeWorker(TreeViewer instance, Type type, TreePath path)
    {
      this.instance = instance;
      this.type = type;
      this.path = path;

      String msg;
      switch (this.type) {
        case Expand:
          msg = "Expanding nodes";
          break;
        case Collapse:
          msg = "Collapsing nodes";
          break;
        default:
          msg = "";
      }
      progress = new ProgressMonitor(this.instance, msg, "This may take a while...", 0, 1);
      progress.setMillisToDecideToPopup(250);
      progress.setMillisToPopup(1000);
      progress.setProgress(0);
    }

    @Override
    protected Void doInBackground() throws Exception
    {
      try {
        switch (type) {
          case Expand:
            instance.expandNode(path, MAX_DEPTH);
            break;
          case Collapse:
            instance.collapseNode(path, MAX_DEPTH);
            break;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void done()
    {
      if (progress != null) {
        progress.close();
        progress = null;
      }
    }

    /** Current operation type. */
    public Type getType()
    {
      return type;
    }

    /** Advances the progress bar by one unit. May display a short notice after a while. */
    public void advanceProgress()
    {
      if (progress != null) {
        progress.setMaximum(progress.getMaximum() + 1);
        if ((progress.getMaximum() < MAX_NODE_WAIT) || getType() == Type.Collapse) {
          if ((progress.getMaximum() - 1) % 100 == 0) {
            progress.setNote(String.format("Processing node %1$d", progress.getMaximum() - 1));
          }
        } else if (progress.getMaximum() == MAX_NODE_WAIT && getType() == Type.Expand) {
          progress.setNote("You may cancel this operation.");
        }
        progress.setProgress(progress.getMaximum() - 1);
      }
    }

    /** Returns true if the user cancelled the operation. */
    public boolean userCancelled()
    {
      if (progress != null) {
        return progress.isCanceled();
      }
      return false;
    }
  }

  // Common base class for node type specific classes
  private static abstract class ItemBase
  {
     private final DlgResource dlg;
     private final boolean showStrrefs;

     public ItemBase(DlgResource dlg)
     {
       this.dlg = dlg;
       this.showStrrefs = BrowserMenuBar.getInstance().showStrrefs();
     }

     /** Returns the dialog resource object. */
     public DlgResource getDialog()
     {
       return dlg;
     }

     /** Returns the dialog resource name. */
     public String getDialogName()
     {
       if (dlg != null) {
         return dlg.getResourceEntry().getResourceName();
       } else {
         return "";
       }
     }

     /** Returns the icon associated with the item type. */
     public abstract Icon getIcon();

     /** Returns whether to show the Strref value next to the string. */
     protected boolean showStrrefs() { return showStrrefs; }
  }

  // Meta class for identifying root node
  private static final class RootItem extends ItemBase
  {
    private static final ImageIcon ICON = Icons.getIcon("RowInsertAfter16.gif");

    private final ArrayList<StateItem> states = new ArrayList<StateItem>();

    private int numStates, numTransitions, numStateTriggers, numResponseTriggers, numActions;
    private String flags;

    public RootItem(DlgResource dlg)
    {
      super(dlg);

      if (getDialog() != null) {
        StructEntry entry = getDialog().getAttribute("# states");
        if (entry instanceof SectionCount) {
          numStates = ((SectionCount)entry).getValue();
        }
        entry = getDialog().getAttribute("# responses");
        if (entry instanceof SectionCount) {
          numTransitions = ((SectionCount)entry).getValue();
        }
        entry = getDialog().getAttribute("# state triggers");
        if (entry instanceof SectionCount) {
          numStateTriggers = ((SectionCount)entry).getValue();
        }
        entry = getDialog().getAttribute("# response triggers");
        if (entry instanceof SectionCount) {
          numResponseTriggers = ((SectionCount)entry).getValue();
        }
        entry = getDialog().getAttribute("# actions");
        if (entry instanceof SectionCount) {
          numActions = ((SectionCount)entry).getValue();
        }
        entry = getDialog().getAttribute("Threat response");
        if (entry instanceof Flag) {
          flags = ((Flag)entry).toString();
        }

        // finding and storing initial states (sorted by trigger index in ascending order)
        for (int i = 0; i < numStates; i++) {
          entry = getDialog().getAttribute(String.format(State.FMT_NAME, i));
          if (entry instanceof State) {
            int triggerIndex = ((State)entry).getTriggerIndex();
            if (triggerIndex >= 0) {
              int j = 0;
              for (; j < states.size(); j++) {
                if (states.get(j).getState().getTriggerIndex() > triggerIndex) {
                  break;
                }
              }
              states.add(j, new StateItem(getDialog(), (State)entry));
            }
          }
        }
      }
    }

    /** Returns number of available initial states. */
    public int getInitialStateCount()
    {
      return states.size();
    }

    /** Returns the StateItem at the given index or null on error. */
    public StateItem getInitialState(int index)
    {
      if (index >= 0 && index < states.size()) {
        return states.get(index);
      }
      return null;
    }

    @Override
    public Icon getIcon()
    {
      return ICON;
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      if (!getDialogName().isEmpty()) {
        sb.append(getDialogName());
      } else {
        sb.append("(Invalid DLG resource)");
      }
      sb.append(" (states: ").append(Integer.toString(numStates));
      sb.append(", responses: ").append(Integer.toString(numTransitions));
      sb.append(", state triggers: ").append(Integer.toString(numStateTriggers));
      sb.append(", response triggers: ").append(Integer.toString(numResponseTriggers));
      sb.append(", actions: ").append(Integer.toString(numActions));
      if (flags != null) {
        sb.append(", flags: ").append(flags);
      }
      sb.append(")");

      return sb.toString();
    }
  }


  // Encapsulates a dialog state entry
  private static final class StateItem extends ItemBase
  {
    private static final ImageIcon ICON = Icons.getIcon("Stop16.gif");
    private static final int MAX_LENGTH = 100;    // max. string length to display

    private State state;

    public StateItem(DlgResource dlg, State state)
    {
      super(dlg);
      this.state = state;
    }

    public State getState()
    {
      return state;
    }

    public void setState(State state)
    {
      if (state != null) {
        this.state = state;
      }
    }

    @Override
    public Icon getIcon()
    {
      return ICON;
    }

    @Override
    public String toString()
    {
      if (state != null) {
        String text = StringResource.getStringRef(state.getResponse().getValue(), showStrrefs(), true);
        if (text.length() > MAX_LENGTH) {
          text = text.substring(0, MAX_LENGTH) + "...";
        }
        return String.format("%1$s: %2$s", state.getName(), text);
      } else {
        return "(Invalid state)";
      }
    }
  }


  // Encapsulates a dialog transition entry
  private static final class TransitionItem extends ItemBase
  {
    private static final ImageIcon ICON = Icons.getIcon("Play16.gif");
    private static final int MAX_LENGTH = 100;    // max. string length to display

    private Transition trans;

    public TransitionItem(DlgResource dlg, Transition trans)
    {
      super(dlg);
      this.trans = trans;
    }

    public Transition getTransition()
    {
      return trans;
    }

    public void setTransition(Transition trans)
    {
      if (trans != null) {
        this.trans = trans;
      }
    }

    @Override
    public Icon getIcon()
    {
      return ICON;
    }

    @Override
    public String toString()
    {
      if (trans != null) {
        if (trans.getFlag().isFlagSet(0)) {
          // Transition contains text
          String text = StringResource.getStringRef(trans.getAssociatedText().getValue(), showStrrefs(), true);
          if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH) + "...";
          }
          String dlg = getDialog().getResourceEntry().getResourceName();
          if (trans.getNextDialog().isEmpty() ||
              trans.getNextDialog().getResourceName().equalsIgnoreCase(dlg)) {
            return String.format("%1$s: %2$s", trans.getName(), text);
          } else {
            return String.format("%1$s: %2$s [%3$s]",
                                 trans.getName(), text, trans.getNextDialog().getResourceName());
          }
        } else {
          // Transition contains no text
          String dlg = getDialog().getResourceEntry().getResourceName();
          if (trans.getNextDialog().isEmpty() ||
              trans.getNextDialog().getResourceName().equalsIgnoreCase(dlg)) {
            return String.format("%1$s: (No text)", trans.getName());
          } else {
            return String.format("%1$s: (No text) [%2$s]",
                                 trans.getName(), trans.getNextDialog().getResourceName());
          }
        }
      } else {
        return "(Invalid response)";
      }
    }
  }


  // Creates and manages the dialog tree structure
  private static final class DlgTreeModel implements TreeModel
  {
    private final ArrayList<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
    // maps dialog resources to tables of state index/item pairs
    private final HashMap<String, HashMap<Integer, StateItem>> mapState = new HashMap<String, HashMap<Integer, StateItem>>();
    // maps dialog resources to tables of transition index/item pairs
    private final HashMap<String, HashMap<Integer, TransitionItem>> mapTransition = new HashMap<String, HashMap<Integer, TransitionItem>>();

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

    // Recursively parses the tree and triggers a nodeChanged event for each node containing data.
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

    // Generates an array of TreeNode objects from root to specified node
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

    // Determines the child index based on the specified node's parent
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

//    private void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices,
//                                       Object[] children)
//    {
//      if (!listeners.isEmpty()) {
//        TreeModelEvent event;
//        if (path == null || path.length == 0) {
//          event = new TreeModelEvent(source, (TreePath)null);
//        } else {
//          event = new TreeModelEvent(source, path, childIndices, children);
//        }
//        for (int i = listeners.size()-1; i >= 0; i--) {
//          TreeModelListener tml = listeners.get(i);
//          tml.treeNodesInserted(event);
//        }
//      }
//    }

//    private void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices,
//                                       Object[] children)
//    {
//      if (!listeners.isEmpty()) {
//        TreeModelEvent event;
//        if (path == null || path.length == 0) {
//          event = new TreeModelEvent(source, (TreePath)null);
//        } else {
//          event = new TreeModelEvent(source, path, childIndices, children);
//        }
//        for (int i = listeners.size()-1; i >= 0; i--) {
//          TreeModelListener tml = listeners.get(i);
//          tml.treeNodesRemoved(event);
//        }
//      }
//    }

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
          map = new HashMap<Integer, StateItem>();
          setStateTable(dlg.getResourceEntry().getResourceName(), map);
        }

        if (!map.containsKey(Integer.valueOf(state.getState().getNumber()))) {
          map.put(Integer.valueOf(state.getState().getNumber()), state);

          for (int i = 0; i < state.getState().getTransCount(); i++) {
            int transIdx = state.getState().getFirstTrans() + i;
            StructEntry entry = dlg.getAttribute(String.format(Transition.FMT_NAME, transIdx));
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
          map = new HashMap<Integer, TransitionItem>();
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
              StructEntry entry = dlg.getAttribute(String.format(State.FMT_NAME, stateIdx));
              if (entry instanceof State) {
                initState(new StateItem(dlg, (State)entry));
              }
            }
          }
        }
      }
    }

    // Returns a dialog resource object based on the specified resource name
    // Reuses exising DlgResource objects if available
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
        } else if (ResourceFactory.getInstance().resourceExists(dlgName)) {
          try {
            return new DlgResource(ResourceFactory.getInstance().getResourceEntry(dlgName));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      return null;
    }

    // Adds all available child nodes to the given parent node
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

    // Adds all available transition child nodes to the given parent state node
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

    // Adds all available state child nodes to the given parent transition node
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

    // Adds all available initial state child nodes to the given parent root node
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

    // Returns the state table of the specified dialog resource
    private HashMap<Integer, StateItem> getStateTable(String dlgName)
    {
      if (dlgName != null) {
        return mapState.get(dlgName.toUpperCase(Locale.ENGLISH));
      } else {
        return null;
      }
    }

    // Adds or replaces a dialog resource entry with its associated state table
    private void setStateTable(String dlgName, HashMap<Integer, StateItem> map)
    {
      if (dlgName != null) {
        mapState.put(dlgName.toUpperCase(Locale.ENGLISH), map);
      }
    }

    // Returns whether the specified dialog resource has been mapped
    private boolean containsStateTable(String dlgName)
    {
      if (dlgName != null) {
        return mapState.containsKey(dlgName.toUpperCase(Locale.ENGLISH));
      } else {
        return false;
      }
    }

    // Returns the transition table of the specified dialog resource
    private HashMap<Integer, TransitionItem> getTransitionTable(String dlgName)
    {
      if (dlgName != null) {
        return mapTransition.get(dlgName.toUpperCase(Locale.ENGLISH));
      } else {
        return null;
      }
    }

    // Adds or replaces a dialog resource entry with its associated transition table
    private void setTransitionTable(String dlgName, HashMap<Integer, TransitionItem> map)
    {
      if (dlgName != null) {
        mapTransition.put(dlgName.toUpperCase(Locale.ENGLISH), map);
      }
    }

    // Returns whether the specified dialog resource has been mapped
    private boolean containsTransitionTable(String dlgName)
    {
      if (dlgName != null) {
        return mapTransition.containsKey(dlgName.toUpperCase(Locale.ENGLISH));
      } else {
        return false;
      }
    }
  }


  // Panel for displaying information about the current dialog state or trigger
  private static final class ItemInfo extends JPanel
  {
    /** Identifies the respective controls for displaying information. */
    private enum Type {
      STATE, STATE_TEXT, STATE_TRIGGER,
      RESPONSE, RESPONSE_FLAGS, RESPONSE_TEXT, RESPONSE_JOURNAL, RESPONSE_TRIGGER, RESPONSE_ACTION
    }

    private static final String CARD_EMPTY    = "Empty";
    private static final String CARD_STATE    = "State";
    private static final String CARD_RESPONSE = "Response";

    private final CardLayout cardLayout;
    private final JPanel pMainPanel, pState, pResponse, pStateText, pStateTrigger, pResponseFlags,
                         pResponseText, pResponseJournal, pResponseTrigger, pResponseAction;
    private final JTextArea taStateText, taStateTrigger;
    private final JTextArea taResponseText, taResponseJournal, taResponseTrigger, taResponseAction;
    private final JTextField tfResponseFlags;


    public ItemInfo()
    {
      setLayout(new GridBagLayout());

      GridBagConstraints gbc = new GridBagConstraints();

      cardLayout = new CardLayout(0, 0);
      pMainPanel = new JPanel(cardLayout);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
      add(pMainPanel, gbc);

      // shown when no item has been selected
      pMainPanel.add(new JPanel(), CARD_EMPTY);

      // initializing state item info
      pState = new JPanel(new GridBagLayout());
      pState.setBorder(createTitledBorder("State", Font.BOLD, true));
      pMainPanel.add(pState, CARD_STATE);

      taStateText = createReadOnlyTextArea();
      taStateText.setMargin(new Insets(0, 4, 0, 4));
      pStateText = new JPanel(new BorderLayout());
      pStateText.setBorder(createTitledBorder("Associated text", Font.BOLD, false));
      pStateText.add(taStateText, BorderLayout.CENTER);

      taStateTrigger = createReadOnlyTextArea();
      taStateTrigger.setFont(BrowserMenuBar.getInstance().getScriptFont());
      taStateTrigger.setMargin(new Insets(0, 4, 0, 4));
      pStateTrigger = new JPanel(new BorderLayout());
      pStateTrigger.setBorder(createTitledBorder("State trigger", Font.BOLD, false));
      pStateTrigger.add(taStateTrigger, BorderLayout.CENTER);

      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pState.add(pStateText, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pState.add(pStateTrigger, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      pState.add(new JPanel(), gbc);


      // initializing response item info
      pResponse = new JPanel(new GridBagLayout());
      pResponse.setBorder(createTitledBorder("Response", Font.BOLD, true));
      pMainPanel.add(pResponse, CARD_RESPONSE);

      tfResponseFlags = createReadOnlyTextField();
      tfResponseFlags.setMargin(new Insets(0, 4, 0, 4));
      pResponseFlags = new JPanel(new BorderLayout());
      pResponseFlags.setBorder(createTitledBorder("Flags", Font.BOLD, false));
      pResponseFlags.add(tfResponseFlags, BorderLayout.CENTER);

      taResponseText = createReadOnlyTextArea();
      taResponseText.setMargin(new Insets(0, 4, 0, 4));
      pResponseText = new JPanel(new BorderLayout());
      pResponseText.setBorder(createTitledBorder("Associated text", Font.BOLD, false));
      pResponseText.add(taResponseText, BorderLayout.CENTER);

      taResponseJournal = createReadOnlyTextArea();
      taResponseJournal.setMargin(new Insets(0, 4, 0, 4));
      pResponseJournal = new JPanel(new BorderLayout());
      pResponseJournal.setBorder(createTitledBorder("Journal entry", Font.BOLD, false));
      pResponseJournal.add(taResponseJournal, BorderLayout.CENTER);

      taResponseTrigger = createReadOnlyTextArea();
      taResponseTrigger.setFont(BrowserMenuBar.getInstance().getScriptFont());
      taResponseTrigger.setMargin(new Insets(0, 4, 0, 4));
      pResponseTrigger = new JPanel(new BorderLayout());
      pResponseTrigger.setBorder(createTitledBorder("Response trigger", Font.BOLD, false));
      pResponseTrigger.add(taResponseTrigger, BorderLayout.CENTER);

      taResponseAction = createReadOnlyTextArea();
      taResponseAction.setFont(BrowserMenuBar.getInstance().getScriptFont());
      taResponseAction.setMargin(new Insets(0, 4, 0, 4));
      pResponseAction = new JPanel(new BorderLayout());
      pResponseAction.setBorder(createTitledBorder("Action", Font.BOLD, false));
      pResponseAction.add(taResponseAction, BorderLayout.CENTER);

      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pResponse.add(pResponseFlags, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pResponse.add(pResponseText, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pResponse.add(pResponseJournal, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pResponse.add(pResponseTrigger, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pResponse.add(pResponseAction, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      pResponse.add(new JPanel(), gbc);

      showPanel(CARD_EMPTY);
    }

    /** Shows the panel of given name. */
    public void showPanel(String cardName)
    {
      if (cardName != null) {
        if (cardName.equals(CARD_STATE)) {
          clearCard(CARD_RESPONSE);
        } else if (cardName.equals(CARD_RESPONSE)) {
          clearCard(CARD_STATE);
        } else {
          cardName = CARD_EMPTY;
          clearCard(CARD_STATE);
          clearCard(CARD_RESPONSE);
        }
        cardLayout.show(pMainPanel, cardName);
      }
    }

    /** Enable/disable visibility of the given control. */
    public void showControl(Type type, boolean b)
    {
      getControl(type).setVisible(b);
    }

    /** Update border title of the given control. */
    public void updateControlBorder(Type type, String title)
    {
      if (title == null) title = "";
      if (getControl(type).getBorder() instanceof TitledBorder) {
        TitledBorder b = (TitledBorder)getControl(type).getBorder();
        b.setTitle(" " + title + " ");
        getControl(type).repaint();
      }
    }

    /** Update content of the given control. */
    public void updateControlText(Type type, String text)
    {
      if (text == null) text = "";
      switch (type) {
        case STATE_TEXT:        taStateText.setText(text); break;
        case STATE_TRIGGER:     taStateTrigger.setText(text); break;
        case RESPONSE_FLAGS:    tfResponseFlags.setText(text); break;
        case RESPONSE_TEXT:     taResponseText.setText(text); break;
        case RESPONSE_JOURNAL:  taResponseJournal.setText(text); break;
        case RESPONSE_TRIGGER:  taResponseTrigger.setText(text); break;
        case RESPONSE_ACTION:   taResponseAction.setText(text); break;
        default:
      }
    }


    // Returns the given control
    private JPanel getControl(Type type)
    {
      switch (type) {
        case STATE:             return pState;
        case RESPONSE:          return pResponse;
        case STATE_TEXT:        return pStateText;
        case STATE_TRIGGER:     return pStateTrigger;
        case RESPONSE_FLAGS:    return pResponseFlags;
        case RESPONSE_TEXT:     return pResponseText;
        case RESPONSE_JOURNAL:  return pResponseJournal;
        case RESPONSE_TRIGGER:  return pResponseTrigger;
        case RESPONSE_ACTION:   return pResponseAction;
      }
      return new JPanel();
    }

    // Clears and disables controls in the specified panel
    private void clearCard(String cardName)
    {
      if (cardName != null) {
        if (cardName.equals(CARD_STATE)) {
          updateControlText(Type.STATE_TEXT, "");
          showControl(Type.STATE_TEXT, false);
          updateControlText(Type.STATE_TRIGGER, "");
          showControl(Type.STATE_TRIGGER, false);
        } else if (cardName.equals(CARD_RESPONSE)) {
          updateControlText(Type.RESPONSE_FLAGS, "");
          showControl(Type.RESPONSE_FLAGS, false);
          updateControlText(Type.RESPONSE_TEXT, "");
          showControl(Type.RESPONSE_TEXT, false);
          updateControlText(Type.RESPONSE_JOURNAL, "");
          showControl(Type.RESPONSE_JOURNAL, false);
          updateControlText(Type.RESPONSE_TRIGGER, "");
          showControl(Type.RESPONSE_TRIGGER, false);
          updateControlText(Type.RESPONSE_ACTION, "");
          showControl(Type.RESPONSE_ACTION, false);
        }
      }
    }

    // Helper method for creating a read-only textarea component
    private JTextArea createReadOnlyTextArea()
    {
      JLabel l = new JLabel();
      JTextArea ta = new JTextArea();
      ta.setEditable(false);
      ta.setFont(new Font(l.getFont().getFamily(), 0, l.getFont().getSize()));
      ta.setBackground(l.getBackground());
      ta.setWrapStyleWord(true);
      ta.setLineWrap(true);
      l = null;

      return ta;
    }

    // Helper method for creating a read-only textfield component
    private JTextField createReadOnlyTextField()
    {
      JLabel l = new JLabel();
      JTextField tf = new JTextField();
      tf.setBorder(BorderFactory.createEmptyBorder());
      tf.setEditable(false);
      tf.setFont(new Font(l.getFont().getFamily(), 0, l.getFont().getSize()));
      tf.setBackground(l.getBackground());
      l = null;

      return tf;
    }

    // Returns a modified TitledBorder object
    private TitledBorder createTitledBorder(String title, int fontStyle, boolean isTitle)
    {
      if(title == null) title = "";
      TitledBorder tb = BorderFactory.createTitledBorder(title);
      Font f = tb.getTitleFont();
      if (f == null) {
        f = (new JLabel()).getFont();
      }
      if (f != null) {
        tb.setTitleFont(new Font(f.getFamily(), fontStyle, isTitle ? (f.getSize() + 1) : f.getSize()));
      }
      return tb;
    }
  }
}
