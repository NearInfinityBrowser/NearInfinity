// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.NearInfinity;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.gui.BrowserMenuBar;
import infinity.gui.ViewerUtil;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.StringResource;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/** Show dialog content as tree structure. */
final class TreeViewer extends JPanel implements TreeSelectionListener
{
  private final DlgResource dlg;
  private final DlgTreeModel dlgModel;
  private final JTree dlgTree;
  private final ItemInfo dlgInfo;

  TreeViewer(DlgResource dlg)
  {
    super(new BorderLayout());
    this.dlg = dlg;
    dlgModel = new DlgTreeModel(this.dlg);
    dlgTree = new JTree();
    dlgTree.addTreeSelectionListener(this);
    dlgInfo = new ItemInfo();
    initControls();
  }

//--------------------- Begin Interface TreeSelectionListener ---------------------

  @Override
  public void valueChanged(TreeSelectionEvent e)
  {
    if (e.getSource() == dlgTree) {
      Object data = dlgTree.getLastSelectedPathComponent();
      if (data != null) {
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
      dlgInfo.updateControlBorder(ItemInfo.Type.RESPONSE, trans.getName());

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
    } else {
      dlgInfo.showPanel(ItemInfo.CARD_EMPTY);
    }
  }

  private void initControls()
  {
    // initializing info component
    JScrollPane spInfo = new JScrollPane(dlgInfo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
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
    JScrollPane spTree = new JScrollPane(pTree);
    spTree.setBorder(BorderFactory.createEmptyBorder());
    spTree.getHorizontalScrollBar().setUnitIncrement(16);
    spTree.getVerticalScrollBar().setUnitIncrement(16);

    dlgTree.setRootVisible(true);
    dlgTree.setEditable(false);
    DefaultTreeCellRenderer tcr = (DefaultTreeCellRenderer)dlgTree.getCellRenderer();
    tcr.setLeafIcon(null);
    tcr.setOpenIcon(null);
    tcr.setClosedIcon(null);
    dlgTree.setModel(dlgModel);   // set model AFTER customizing visual appearance of the tree control

    // putting components together
    JSplitPane splitv = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTree, spInfo);
    int height = NearInfinity.getInstance().getContentPane().getHeight() -
                 NearInfinity.getInstance().getStatusBar().getHeight() - 24;
    splitv.setDividerLocation(height / 2);
    add(splitv, BorderLayout.CENTER);
  }


//-------------------------- INNER CLASSES --------------------------

  // Meta class for identifying root node
  private final class RootItem
  {
    private final ArrayList<StateItem> states = new ArrayList<StateItem>();

    private int numStates, numTransitions, numStateTriggers, numResponseTriggers, numActions;
    private String dlgName, flags;

    public RootItem(DlgResource dlg)
    {
      if (dlg != null) {
        dlgName = dlg.getResourceEntry().getResourceName();

        StructEntry entry = dlg.getAttribute("# states");
        if (entry instanceof SectionCount) {
          numStates = ((SectionCount)entry).getValue();
        }
        entry = dlg.getAttribute("# responses");
        if (entry instanceof SectionCount) {
          numTransitions = ((SectionCount)entry).getValue();
        }
        entry = dlg.getAttribute("# state triggers");
        if (entry instanceof SectionCount) {
          numStateTriggers = ((SectionCount)entry).getValue();
        }
        entry = dlg.getAttribute("# response triggers");
        if (entry instanceof SectionCount) {
          numResponseTriggers = ((SectionCount)entry).getValue();
        }
        entry = dlg.getAttribute("# actions");
        if (entry instanceof SectionCount) {
          numActions = ((SectionCount)entry).getValue();
        }
        entry = dlg.getAttribute("Threat response");
        if (entry instanceof Flag) {
          flags = ((Flag)entry).toString();
        }

        // finding and storing initial states (sorted by trigger index in ascending order)
        for (int i = 0; i < numStates; i++) {
          entry = dlg.getAttribute(String.format(State.FMT_NAME, i));
          if (entry instanceof State) {
            int triggerIndex = ((State)entry).getTriggerIndex();
            if (triggerIndex >= 0) {
              int j = 0;
              for (; j < states.size(); j++) {
                if (states.get(j).getState().getTriggerIndex() > triggerIndex) {
                  break;
                }
              }
              states.add(j, new StateItem(dlg, (State)entry));
            }
          }
        }
      } else {
        dlgName = "(Invalid DLG resource)";
        flags = "";
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
    public String toString()
    {
      StringBuilder sb = new StringBuilder(dlgName);
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
  private final class StateItem
  {
    private static final int MAX_LENGTH = 100;    // max. string length to display

    private DlgResource dlg;
    private State state;

    public StateItem(DlgResource dlg, State state)
    {
      this.dlg = dlg;
      this.state = state;
    }

    public DlgResource getDialog()
    {
      return dlg;
    }

//    public void setDialog(DlgResource dlg)
//    {
//      this.dlg = dlg;
//    }

    public State getState()
    {
      return state;
    }

//    public void setState(State state)
//    {
//      this.state = state;
//    }

    @Override
    public String toString()
    {
      if (state != null) {
        String text = StringResource.getStringRef(state.getResponse().getValue());
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
  private final class TransitionItem
  {
    private static final int MAX_LENGTH = 100;    // max. string length to display

    private DlgResource dlg;
    private Transition trans;

    public TransitionItem(DlgResource dlg, Transition trans)
    {
      this.dlg = dlg;
      this.trans = trans;
    }

    public DlgResource getDialog()
    {
      return dlg;
    }

//    public void setDialog(DlgResource dlg)
//    {
//      this.dlg = dlg;
//    }

    public Transition getTransition()
    {
      return trans;
    }

//    public void setTransition(Transition trans)
//    {
//      this.trans = trans;
//    }

    @Override
    public String toString()
    {
      if (trans != null) {
        if (trans.getFlag().isFlagSet(0)) {
          // Transition contains text
          String text = StringResource.getStringRef(trans.getAssociatedText().getValue());
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
  private final class DlgTreeModel implements TreeModel
  {
    private final ArrayList<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
    // maps dialog resources to tables of state index/item pairs
    private final HashMap<String, HashMap<Integer, StateItem>> mapState = new HashMap<String, HashMap<Integer, StateItem>>();
    // maps dialog resources to tables of transition index/item pairs
    private final HashMap<String, HashMap<Integer, TransitionItem>> mapTransition = new HashMap<String, HashMap<Integer, TransitionItem>>();

    private DlgResource dlg;
    private RootItem root;

    public DlgTreeModel(DlgResource dlg)
    {
      reset(dlg);
    }

  //--------------------- Begin Interface TreeModel ---------------------

    @Override
    public Object getRoot()
    {
      return root;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
      if (parent instanceof StateItem) {
        // states allow multiple transitions as children
        StateItem state = (StateItem)parent;
        int count = state.getState().getTransCount();
        if (index >= 0 && index < count) {
          int transIndex = state.getState().getFirstTrans() + index;
          String dlgName = state.getDialog().getResourceEntry().getResourceName();
          return mapTransition.get(dlgName).get(Integer.valueOf(transIndex));
        }
      } else if (parent instanceof TransitionItem && index == 0) {
        // transitions only allow a single state as child
        TransitionItem trans = (TransitionItem)parent;

        // Handling local and external dialog references separately
        ResourceRef dlgRef = trans.getTransition().getNextDialog();
        int stateIndex = trans.getTransition().getNextDialogState();
        if (dlgRef.isEmpty() || dlgRef.getResourceName().equals(dlg.getResourceEntry().getResourceName())) {
          if (mapState.containsKey(dlg.getResourceEntry().getResourceName())) {
            return mapState.get(dlg.getResourceEntry().getResourceName()).get(Integer.valueOf(stateIndex));
          }
        } else {
          if (mapState.containsKey(dlgRef.getResourceName())) {
            return mapState.get(dlgRef.getResourceName()).get(Integer.valueOf(stateIndex));
          }
        }
      } else if (parent instanceof RootItem) {
        return ((RootItem)parent).getInitialState(index);
      }
      return null;
    }

    @Override
    public int getChildCount(Object parent)
    {
      if (parent instanceof StateItem) {
        StateItem state = (StateItem)parent;
        return state.getState().getTransCount();
      } else if (parent instanceof TransitionItem) {
        TransitionItem trans = (TransitionItem)parent;
        if (trans.getTransition().getFlag().isFlagSet(3)) {
          // dialog is terminated
          return 0;
        } else {
          // dialog continues
          return 1;
        }
      } else if (parent instanceof RootItem) {
        return ((RootItem)parent).getInitialStateCount();
      }
      return 0;
    }

    @Override
    public boolean isLeaf(Object node)
    {
      if (node instanceof TransitionItem) {
        TransitionItem trans = (TransitionItem)node;
        return trans.getTransition().getFlag().isFlagSet(3);
      }
      return false;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
      // tree is immutable
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
      if (child != null) {
        if (parent instanceof StateItem) {
          StateItem state = (StateItem)parent;
          int count = state.getState().getTransCount();
          int triggerIndex = state.getState().getTriggerIndex();
          for (int i = 0; i < count; i++) {
            if (mapTransition.get(state.getDialog()).get(Integer.valueOf(triggerIndex+i)) == child) {
              return i;
            }
          }
        } else if (parent instanceof TransitionItem) {
          TransitionItem trans = (TransitionItem)parent;

          // Handling local and external dialog references separately
          ResourceRef dlgRef = trans.getTransition().getNextDialog();
          if (dlgRef.isEmpty() || dlgRef.getResourceName().equals(dlg.getResourceEntry().getResourceName())) {
            HashMap<Integer, StateItem> mapItem = mapState.get(dlg.getResourceEntry().getResourceName());
            if (mapItem != null && mapItem.containsValue(child)) {
              return 0;
            }
          } else {
            HashMap<Integer, StateItem> mapItem = mapState.get(dlgRef.getResourceName());
            if (mapItem != null && mapItem.containsValue(child)) {
              return 0;
            }
          }
        } else if (parent instanceof RootItem) {
          RootItem root = (RootItem)parent;
          for (int i = 0; i < root.getInitialStateCount(); i++) {
            if (child == root.getInitialState(i)) {
              return i;
            }
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

      this.dlg = dlg;
      initialize();

      // notifying listeners
      TreeModelEvent event = new TreeModelEvent(getRoot(), new TreePath(getRoot()));
      for (final TreeModelListener tml: listeners) {
        tml.treeStructureChanged(event);
      }
    }

//    /** Attempts to find the state specified by the parameters. Returns null if not found. */
//    public StateItem getState(DlgResource dlg, int stateIndex)
//    {
//      if (dlg == null) dlg = this.dlg;
//      if (mapState.containsKey(dlg.getResourceEntry().getResourceName())) {
//        return mapState.get(dlg.getResourceEntry().getResourceName()).get(Integer.valueOf(stateIndex));
//      }
//      return null;
//    }

//    /** Attempts to find the transition specified by the parameters. Returns null if not found. */
//    public TransitionItem getTransition(DlgResource dlg, int transIndex)
//    {
//      if (dlg == null) dlg = this.dlg;
//      if (mapTransition.containsKey(dlg.getResourceEntry().getResourceName())) {
//        return mapTransition.get(dlg.getResourceEntry().getResourceName()).get(Integer.valueOf(transIndex));
//      }
//      return null;
//    }

    private void initState(StateItem state)
    {
      if (state != null) {
        DlgResource dlg = state.getDialog();
        HashMap<Integer, StateItem> map = mapState.get(dlg.getResourceEntry().getResourceName());
        if (map == null) {
          map = new HashMap<Integer, StateItem>();
          mapState.put(dlg.getResourceEntry().getResourceName(), map);
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
        HashMap<Integer, TransitionItem> map = mapTransition.get(dlg.getResourceEntry().getResourceName());
        if (map == null) {
          map = new HashMap<Integer, TransitionItem>();
          mapTransition.put(dlg.getResourceEntry().getResourceName(), map);
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
        if (mapState.containsKey(dlgName)) {
          HashMap<Integer, StateItem> map = mapState.get(dlgName);
          if (!map.keySet().isEmpty()) {
            return map.get(map.keySet().iterator().next()).getDialog();
          }
        } else if (mapTransition.containsKey(dlgName)) {
          HashMap<Integer, TransitionItem> map = mapTransition.get(dlgName);
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


    // Initializing dialog entries
    private void initialize()
    {
      root = new RootItem(dlg);
      for (int i = 0; i < root.getInitialStateCount(); i++) {
        initState(root.getInitialState(i));
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
      pState.setBorder(BorderFactory.createTitledBorder("State"));
      pMainPanel.add(pState, CARD_STATE);

      taStateText = createReadOnlyTextArea();
//      scroll = new JScrollPane(taStateText);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pStateText = new JPanel(new BorderLayout());
      pStateText.setBorder(BorderFactory.createTitledBorder("Associated text"));
      pStateText.add(taStateText, BorderLayout.CENTER);

      taStateTrigger = createReadOnlyTextArea();
//      scroll = new JScrollPane(taStateTrigger);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pStateTrigger = new JPanel(new BorderLayout());
      pStateTrigger.setBorder(BorderFactory.createTitledBorder("State trigger"));
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
      pResponse.setBorder(BorderFactory.createTitledBorder("Response"));
      pMainPanel.add(pResponse, CARD_RESPONSE);

      tfResponseFlags = createReadOnlyTextField();
//      scroll = new JScrollPane(tfResponseFlags, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
//                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pResponseFlags = new JPanel(new BorderLayout());
      pResponseFlags.setBorder(BorderFactory.createTitledBorder("Flags"));
      pResponseFlags.add(tfResponseFlags, BorderLayout.CENTER);

      taResponseText = createReadOnlyTextArea();
//      scroll = new JScrollPane(taResponseText);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pResponseText = new JPanel(new BorderLayout());
      pResponseText.setBorder(BorderFactory.createTitledBorder("Associated text"));
      pResponseText.add(taResponseText, BorderLayout.CENTER);

      taResponseJournal = createReadOnlyTextArea();
//      scroll = new JScrollPane(taResponseJournal);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pResponseJournal = new JPanel(new BorderLayout());
      pResponseJournal.setBorder(BorderFactory.createTitledBorder("Journal entry"));
      pResponseJournal.add(taResponseJournal, BorderLayout.CENTER);

      taResponseTrigger = createReadOnlyTextArea();
//      scroll = new JScrollPane(taResponseTrigger);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pResponseTrigger = new JPanel(new BorderLayout());
      pResponseTrigger.setBorder(BorderFactory.createTitledBorder("Response trigger"));
      pResponseTrigger.add(taResponseTrigger, BorderLayout.CENTER);

      taResponseAction = createReadOnlyTextArea();
//      scroll = new JScrollPane(taResponseAction);
//      scroll.setBorder(BorderFactory.createEmptyBorder());
      pResponseAction = new JPanel(new BorderLayout());
      pResponseAction.setBorder(BorderFactory.createTitledBorder("Action"));
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
      getControl(type).setBorder(BorderFactory.createTitledBorder(title));
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
      ta.setFont(l.getFont());
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
      tf.setFont(l.getFont());
      tf.setBackground(l.getBackground());
      l = null;

      return tf;
    }
  }
}
