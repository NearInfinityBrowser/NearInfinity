// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
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
import java.util.HashMap;

import javax.swing.BorderFactory;
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
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.LinkButton;
import org.infinity.gui.ScriptTextArea;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.StructEntry;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;


/** Show dialog content as tree structure. */
final class TreeViewer extends JPanel implements ActionListener, TreeSelectionListener,
                                                 TableModelListener, PropertyChangeListener
{
  /**
   * This array contains background colors for other dialogs to which viewed dialog
   * refers. Colors are assigned to other resources from this array on rotation basis.
   */
  private static final Color[] OTHER_DIALOG_COLORS = {
    new Color(0xd8d8ff),
    new Color(0xb8ffb8),
    new Color(0xffd0d0),
    new Color(0xffc8ff),
    new Color(0xffffa0),
    new Color(0xe0e0e0),
    new Color(0x85ffc2),
    new Color(0xffd3a6),
    new Color(0x99ccff),
    new Color(0xffa3d1),
  };
  private final JPopupMenu pmTree = new JPopupMenu();
  private final JMenuItem miExpandAll = new JMenuItem("Expand all nodes");
  private final JMenuItem miExpand = new JMenuItem("Expand selected node");
  private final JMenuItem miCollapseAll = new JMenuItem("Collapse all nodes");
  private final JMenuItem miCollapse = new JMenuItem("Collapse selected nodes");
  private final JMenuItem miEditEntry = new JMenuItem("Edit selected entry");

  /** Caches ViewFrame instances used to display external dialog entries. */
  private final HashMap<DlgResource, ViewFrame> mapViewer = new HashMap<>();

  private final DlgResource dlg;
  private final JTree dlgTree;
  private final ItemInfo dlgInfo;

  private DlgTreeModel dlgModel;
  private JScrollPane spInfo, spTree;
  private TreeWorker worker;
  private WindowBlocker blocker;

  /** Background colors for text in dialogs to that can refer main dialog. */
  private HashMap<DlgResource, Color> dialogColors = new HashMap<>();

  TreeViewer(DlgResource dlg)
  {
    super(new BorderLayout());
    this.dlg = dlg;
    this.dlg.addTableModelListener(this);
    dlgModel = null;
    dlgTree = new JTree((TreeModel)null);
    dlgTree.addTreeSelectionListener(this);
    dlgInfo = new ItemInfo();
    initControls();
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == miEditEntry) {
      final TreePath path = dlgTree.getSelectionPath();
      if (path != null) {
        final ItemBase item = (ItemBase)path.getLastPathComponent();
        final DlgResource curDlg = item.getDialog();
        if (curDlg != dlg) {
          ViewFrame vf = mapViewer.get(curDlg);
          // reuseing external dialog window if possible
          if (vf != null && vf.isVisible()) {
            vf.toFront();
          } else {
            vf = new ViewFrame(this, curDlg);
            mapViewer.put(curDlg, vf);
          }
        }

        final StructViewer viewer = curDlg.getViewer();
        if (viewer != null) {
          // selecting table entry
          final Viewer tab = (Viewer)curDlg.getViewerTab(0);
          if (item instanceof RootItem) {
            viewer.selectEntry(0);
          } else if (item instanceof StateItem) {
            final State s = ((StateItem)item).getState();
            viewer.selectEntry(s.getName());
            tab.showStateWithStructEntry(s);
          } else if (item instanceof TransitionItem) {
            final Transition t = ((TransitionItem)item).getTransition();
            viewer.selectEntry(t.getName());
            tab.showStateWithStructEntry(t);
          }
          curDlg.selectEditTab();
        }
      }
    } else if (e.getSource() == miExpandAll) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.EXPAND, new TreePath(dlgModel.getRoot()));
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    } else if (e.getSource() == miCollapseAll) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.COLLAPSE, new TreePath(dlgModel.getRoot()));
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    } else if (e.getSource() == miExpand) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.EXPAND, dlgTree.getSelectionPath());
        worker.addPropertyChangeListener(this);
        blocker = new WindowBlocker(NearInfinity.getInstance());
        blocker.setBlocked(true);
        worker.execute();
      }
    } else if (e.getSource() == miCollapse) {
      if (worker == null) {
        worker = new TreeWorker(this, TreeWorker.Type.COLLAPSE, dlgTree.getSelectionPath());
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
      final Object data = dlgTree.getLastSelectedPathComponent();
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
    }
  }

//--------------------- End Interface TreeSelectionListener ---------------------

//--------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent e)
  {
    // Insertion or removal of nodes not yet supported
    if (e.getType() == TableModelEvent.UPDATE) {
      if (dlgModel != null) {
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

  /** Initializes the actual dialog content. */
  public void init()
  {
    if (dlgModel == null) {
      dlgModel = new DlgTreeModel(this.dlg);
      dlgTree.setModel(dlgModel);
    }
  }

  private void updateStateInfo(StateItem si)
  {
    final DlgResource curDlg = si.getDialog();
    final State state = si.getState();

    // updating info box title
    final StringBuilder sb = new StringBuilder(state.getName() + ", ");
    if (curDlg != dlg) {
      sb.append(String.format("Dialog: %s, ", curDlg.getResourceEntry().getResourceName()));
    }
    sb.append(String.format("Responses: %d", state.getTransCount()));
    if (state.getTriggerIndex() >= 0) {
      sb.append(String.format(", Weight: %d", state.getTriggerIndex()));
    }
    dlgInfo.updateControlBorder(ItemInfo.Type.STATE, sb.toString());

    // updating state text
    dlgInfo.showControl(ItemInfo.Type.STATE_TEXT, true);
    dlgInfo.updateControlText(ItemInfo.Type.STATE_TEXT, StringTable.getStringRef(state.getResponse().getValue()));

    // updating state WAV Res
    final String responseText = StringTable.getSoundResource(state.getResponse().getValue());
    if (!responseText.isEmpty()) {
      dlgInfo.showControl(ItemInfo.Type.STATE_WAV, true);
      dlgInfo.updateControlText(ItemInfo.Type.STATE_WAV, responseText + ".WAV");
    } else {
      dlgInfo.showControl(ItemInfo.Type.STATE_WAV, false);
    }

    // updating state triggers
    if (state.getTriggerIndex() >= 0) {
      dlgInfo.showControl(ItemInfo.Type.STATE_TRIGGER, true);
      final StructEntry entry = curDlg.getAttribute(StateTrigger.DLG_STATETRIGGER + " " + state.getTriggerIndex());
      final String text = entry instanceof StateTrigger ? ((StateTrigger)entry).getText() : "";
      dlgInfo.updateControlText(ItemInfo.Type.STATE_TRIGGER, text);
    } else {
      dlgInfo.showControl(ItemInfo.Type.STATE_TRIGGER, false);
    }

    dlgInfo.showPanel(ItemInfo.CARD_STATE);

    // jumping to top of scroll area
    SwingUtilities.invokeLater(() -> spInfo.getVerticalScrollBar().setValue(0));
  }

  private void updateTransitionInfo(TransitionItem ti)
  {
    final DlgResource curDlg = ti.getDialog();
    final Transition trans = ti.getTransition();

    // updating info box title
    final StringBuilder sb = new StringBuilder(trans.getName());
    if (curDlg != dlg) {
      sb.append(String.format(", Dialog: %s", curDlg.getResourceEntry().getResourceName()));
    }
    dlgInfo.updateControlBorder(ItemInfo.Type.RESPONSE, sb.toString());

    // updating flags
    dlgInfo.showControl(ItemInfo.Type.RESPONSE_FLAGS, true);
    dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_FLAGS, trans.getFlag().toString());

    // updating response text
    if (trans.getFlag().isFlagSet(0)) {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_TEXT, true);
      dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_TEXT,
                                StringTable.getStringRef(trans.getAssociatedText().getValue()));
    } else {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_TEXT, false);
    }

    // updating journal entry
    if (trans.getFlag().isFlagSet(4)) {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_JOURNAL, true);
      dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_JOURNAL,
                                StringTable.getStringRef(trans.getJournalEntry().getValue()));
    } else {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_JOURNAL, false);
    }

    // updating response trigger
    if (trans.getFlag().isFlagSet(1)) {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_TRIGGER, true);
      final StructEntry entry = curDlg.getAttribute(ResponseTrigger.DLG_RESPONSETRIGGER + " " + trans.getTriggerIndex());
      final String text = entry instanceof ResponseTrigger ? ((ResponseTrigger)entry).getText() : "";
      dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_TRIGGER, text);
    } else {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_TRIGGER, false);
    }

    // updating action
    if (trans.getFlag().isFlagSet(2)) {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_ACTION, true);
      final StructEntry entry = curDlg.getAttribute(Action.DLG_ACTION + " " + trans.getActionIndex());
      final String text = entry instanceof Action ? ((Action)entry).getText() : "";
      dlgInfo.updateControlText(ItemInfo.Type.RESPONSE_ACTION, text);
    } else {
      dlgInfo.showControl(ItemInfo.Type.RESPONSE_ACTION, false);
    }

    dlgInfo.showPanel(ItemInfo.CARD_RESPONSE);

    // jumping to top of scroll area
    SwingUtilities.invokeLater(() -> spInfo.getVerticalScrollBar().setValue(0));
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
      private Color getColor(DlgResource dialog)
      {
        if (dlg == dialog) {
          return null;
        }
        return dialogColors.computeIfAbsent(dialog,
            d -> OTHER_DIALOG_COLORS[dialogColors.size() % OTHER_DIALOG_COLORS.length]
        );
      }
      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                    boolean expanded, boolean leaf, int row,
                                                    boolean focused)
      {
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
        final ItemBase data = (ItemBase)value;

        setIcon(data.getIcon());

        final BrowserMenuBar options = BrowserMenuBar.getInstance();
        setBackgroundNonSelectionColor(options.colorizeOtherDialogs() ? getColor(data.getDialog()) : null);

        if (value instanceof StateItem) {
          final StateItem state = (StateItem)value;
          final State s = state.getState();
          if (s.getNumber() == 0 && s.getTriggerIndex() < 0 && options.alwaysShowState0()) {
            setForeground(Color.GRAY);
          } else
          if (state.getMain() != null) {
            setForeground(Color.GRAY);
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
//    dlgTree.setModel(dlgModel);

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
      public void mouseClicked(MouseEvent e)
      {
        if (e.getSource() == dlgTree && e.getClickCount() == 2) {
          final TreePath path = dlgTree.getPathForLocation(e.getX(), e.getY());
          if (path == null) { return; }

          final Object node = path.getLastPathComponent();
          if (!(node instanceof StateItem)) { return; }

          final StateItem state = (StateItem)node;
          if (!state.getAllowsChildren()) {
            final TreePath target = state.getMain().getPath();
            dlgTree.setSelectionPath(target);
            dlgTree.scrollPathToVisible(target);
          }
        }
      }

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

  /** Checks whether given path contains a node with the specified item object. */
  private boolean nodeExists(TreePath path, Object item)
  {
    while (path != null) {
      if (item.equals(path.getLastPathComponent())) {
        return true;
      }
      path = path.getParentPath();
    }
    return false;
  }

  /** Expands all children and their children of the given path. */
  private void expandNode(TreePath path)
  {
    final TreePath curPath = path;
    if (worker != null && worker.userCancelled()) return;
    if (path != null) {
      final TreeNode item = (TreeNode)path.getLastPathComponent();

      if (item instanceof StateItem) {
        if (nodeExists(path.getParentPath(), item)) {
          return;
        }
      }

      if (worker != null) { worker.advanceProgress(); }
      if (!dlgTree.isExpanded(path)) {
        try {
          SwingUtilities.invokeAndWait(() -> dlgTree.expandPath(curPath));
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }

      for (int i = 0, count = item.getChildCount(); i < count; i++) {
        expandNode(curPath.pathByAddingChild(item.getChildAt(i)));
        if (worker != null && worker.userCancelled()) return;
      }
    }
  }

  /** Collapses all children and their children of the given path. */
  private void collapseNode(TreePath path)
  {
    final TreePath curPath = path;
    if (worker != null && worker.userCancelled()) return;
    if (path != null) {
      TreeNode node = (TreeNode)path.getLastPathComponent();

      for (int i = 0; i < node.getChildCount(); i++) {
        collapseNode(curPath.pathByAddingChild(node.getChildAt(i)));
        if (worker != null && worker.userCancelled()) return;
      }

      if (worker != null) { worker.advanceProgress(); }
      if (!dlgTree.isCollapsed(path)) {
        try {
          SwingUtilities.invokeAndWait(() -> dlgTree.collapsePath(curPath));
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** Returns true if the given path contains expanded nodes. */
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

  /** Returns true if the given path contains collapsed nodes. */
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

  /** Applies expand or collapse operations on a set of dialog tree nodes in a background task. */
  private static class TreeWorker extends SwingWorker<Void, Void>
  {
    /** Supported operations. */
    public enum Type { EXPAND, COLLAPSE }

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
      switch (getType()) {
        case EXPAND:
          msg = "Expanding nodes";
          break;
        case COLLAPSE:
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
        switch (getType()) {
          case EXPAND:
            instance.expandNode(path);
            break;
          case COLLAPSE:
            instance.collapseNode(path);
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
        if ((progress.getMaximum() - 1) % 100 == 0) {
          progress.setNote(String.format("Processing node %d", progress.getMaximum() - 1));
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

  /** Panel for displaying information about the current dialog state or trigger. */
  private static final class ItemInfo extends JPanel
  {
    /** Identifies the respective controls for displaying information. */
    private enum Type {
      STATE, STATE_TEXT, STATE_WAV, STATE_TRIGGER,
      RESPONSE, RESPONSE_FLAGS, RESPONSE_TEXT, RESPONSE_JOURNAL, RESPONSE_TRIGGER, RESPONSE_ACTION
    }

    private static final String CARD_EMPTY    = "Empty";
    private static final String CARD_STATE    = "State";
    private static final String CARD_RESPONSE = "Response";

    private static final Color  COLOR_BACKGROUND = UIManager.getColor("Panel.background");
    private static final Font   FONT_DEFAULT = UIManager.getFont("Label.font").deriveFont(0);

    private final CardLayout cardLayout;
    private final JPanel pMainPanel, pState, pResponse, pStateText, pStateWAV, pStateTrigger,
                         pResponseFlags, pResponseText, pResponseJournal, pResponseTrigger,
                         pResponseAction;
    private final ScriptTextArea taStateTrigger, taResponseTrigger, taResponseAction;
    private final LinkButton lbStateWAV;
    private final JTextArea taStateText, taResponseText, taResponseJournal;
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

      lbStateWAV = new LinkButton("");
      pStateWAV = new JPanel(new GridBagLayout());
      pStateWAV.setBorder(createTitledBorder("Sound Resource", Font.BOLD, false));
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.BOTH, new Insets(0, 4, 0, 4), 0, 0);
      pStateWAV.add(lbStateWAV, gbc);

      taStateTrigger = createScriptTextArea(true);
      taStateTrigger.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
      taStateTrigger.setMargin(new Insets(0, 4, 0, 4));
      pStateTrigger = new JPanel(new BorderLayout());
      pStateTrigger.setBorder(createTitledBorder("State trigger", Font.BOLD, false));
      pStateTrigger.add(taStateTrigger, BorderLayout.CENTER);

      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pState.add(pStateText, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pState.add(pStateWAV, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      pState.add(pStateTrigger, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
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

      taResponseTrigger = createScriptTextArea(true);
      taResponseTrigger.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
      taResponseTrigger.setMargin(new Insets(0, 4, 0, 4));
      pResponseTrigger = new JPanel(new BorderLayout());
      pResponseTrigger.setBorder(createTitledBorder("Response trigger", Font.BOLD, false));
      pResponseTrigger.add(taResponseTrigger, BorderLayout.CENTER);

      taResponseAction = createScriptTextArea(true);
      taResponseAction.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
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
        case STATE_WAV:         lbStateWAV.setResource(text); break;
        case STATE_TRIGGER:     taStateTrigger.setText(text); break;
        case RESPONSE_FLAGS:    tfResponseFlags.setText(text); break;
        case RESPONSE_TEXT:     taResponseText.setText(text); break;
        case RESPONSE_JOURNAL:  taResponseJournal.setText(text); break;
        case RESPONSE_TRIGGER:  taResponseTrigger.setText(text); break;
        case RESPONSE_ACTION:   taResponseAction.setText(text); break;
        default:
      }
    }


    /** Returns the given control. */
    private JPanel getControl(Type type)
    {
      switch (type) {
        case STATE:             return pState;
        case RESPONSE:          return pResponse;
        case STATE_TEXT:        return pStateText;
        case STATE_WAV:         return pStateWAV;
        case STATE_TRIGGER:     return pStateTrigger;
        case RESPONSE_FLAGS:    return pResponseFlags;
        case RESPONSE_TEXT:     return pResponseText;
        case RESPONSE_JOURNAL:  return pResponseJournal;
        case RESPONSE_TRIGGER:  return pResponseTrigger;
        case RESPONSE_ACTION:   return pResponseAction;
      }
      return new JPanel();
    }

    /** Clears and disables controls in the specified panel. */
    private void clearCard(String cardName)
    {
      if (cardName != null) {
        if (cardName.equals(CARD_STATE)) {
          updateControlText(Type.STATE_TEXT, "");
          showControl(Type.STATE_TEXT, false);
          updateControlText(Type.STATE_WAV, "");
          showControl(Type.STATE_WAV, false);
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

    /** Helper method for creating a read-only textarea component. */
    private JTextArea createReadOnlyTextArea()
    {
      JTextArea ta = new JTextArea();
      ta.setEditable(false);
      ta.setFont(FONT_DEFAULT);
      ta.setBackground(COLOR_BACKGROUND);
      ta.setWrapStyleWord(true);
      ta.setLineWrap(true);

      return ta;
    }

    /** Helper method for creating a ScriptTextArea component. */
    private ScriptTextArea createScriptTextArea(boolean readOnly)
    {
      ScriptTextArea ta = new ScriptTextArea();
      if (readOnly) {
        ta.setBackground(COLOR_BACKGROUND);
        ta.setHighlightCurrentLine(false);
      }
      ta.setEditable(!readOnly);
      ta.setWrapStyleWord(true);
      ta.setLineWrap(true);

      return ta;
    }

    /** Helper method for creating a read-only textfield component. */
    private JTextField createReadOnlyTextField()
    {
      JTextField tf = new JTextField();
      tf.setBorder(BorderFactory.createEmptyBorder());
      tf.setEditable(false);
      tf.setFont(FONT_DEFAULT);
      tf.setBackground(COLOR_BACKGROUND);

      return tf;
    }

    /** Returns a modified TitledBorder object. */
    private TitledBorder createTitledBorder(String title, int fontStyle, boolean isTitle)
    {
      if(title == null) title = "";
      TitledBorder tb = BorderFactory.createTitledBorder(title);
      Font f = tb.getTitleFont();
      if (f == null) {
        f = FONT_DEFAULT;
      }
      if (f != null) {
        tb.setTitleFont(new Font(f.getFamily(), fontStyle, isTitle ? (f.getSize() + 1) : f.getSize()));
      }
      return tb;
    }
  }
}
