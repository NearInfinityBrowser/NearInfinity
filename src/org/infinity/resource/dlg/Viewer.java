// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.StringRef;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.ScriptTextArea;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.DialogSearcher;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

final class Viewer extends JPanel implements ActionListener, ItemListener, TableModelListener
{
  private static final ButtonPanel.Control CtrlNextState      = ButtonPanel.Control.CUSTOM_1;
  private static final ButtonPanel.Control CtrlPrevState      = ButtonPanel.Control.CUSTOM_2;
  private static final ButtonPanel.Control CtrlNextTrans      = ButtonPanel.Control.CUSTOM_3;
  private static final ButtonPanel.Control CtrlPrevTrans      = ButtonPanel.Control.CUSTOM_4;
  /** Button that allow move to next state, specified for the response. */
  private static final ButtonPanel.Control CtrlSelect         = ButtonPanel.Control.CUSTOM_5;
  /**
   * Button that allow return to previous state, before current was selected by
   * the {@link #CtrlSelect} button.
   */
  private static final ButtonPanel.Control CtrlReturn         = ButtonPanel.Control.CUSTOM_6;
  private static final ButtonPanel.Control CtrlStateField     = ButtonPanel.Control.CUSTOM_7;
  private static final ButtonPanel.Control CtrlResponseField  = ButtonPanel.Control.CUSTOM_8;

  private static final Color NORMAL_COLOR = Color.BLACK;
  private static final Color ERROR_COLOR  = Color.RED;

  private final DlgResource dlg;
  /** List of all states, found in {@link #dlg}. */
  private final List<State> stateList = new ArrayList<>();
  /** List of all transitions, found in {@link #dlg}. */
  private final List<Transition> transList = new ArrayList<>();
  /**
   * List of all state triggers, found in {@link #dlg}. Trigger determines conditions
   * when state will be visible in the dialogue.
   */
  private final List<StateTrigger> staTriList = new ArrayList<>();
  /**
   * List of all transition triggers, found in {@link #dlg}. Trigger determines
   * conditions when transition will be available for selection in the dialogue.
   */
  private final List<ResponseTrigger> transTriList = new ArrayList<>();
  /**
   * List of all state actions, found in {@link #dlg}. Action determines what
   * will be do when game process entering to related state.
   */
  private final List<Action> actionList = new ArrayList<>();

  /** State that editor shows right now. */
  private State currentState;
  /** Transition that editor shows right now. */
  private Transition currentTrans;

  /**
   * Stack of states, that were selected by the {@link #CtrlSelect} button. The
   * {@link #CtrlReturn} button allows return to one of this states together with
   * transition from {@link #lastTransitions}
   */
  private final ArrayDeque<State> lastStates = new ArrayDeque<>();
  /**
   * Stack of transitions, that were current at moment when next state selected
   * by the {@link #CtrlSelect} button. The {@link #CtrlReturn} button allows return
   * to one of this transitions together with state from {@link #lastStates}
   */
  private final ArrayDeque<Transition> lastTransitions = new ArrayDeque<>();
  private DlgResource undoDlg;
  private boolean alive = true;

  private final ButtonPanel buttonPanel = new ButtonPanel();
  private final DlgPanel stateTextPanel, stateTriggerPanel, transTextPanel, transTriggerPanel, transActionPanel;
  private final JMenuItem ifindall = new JMenuItem("in all DLG files");
  private final JMenuItem ifindthis = new JMenuItem("in this file only");
  private final JPanel outerpanel;
  private final JTextField tfState = new JTextField(4);
  private final JTextField tfResponse = new JTextField(4);
  private final TitledBorder bostate = new TitledBorder("State");
  private final TitledBorder botrans = new TitledBorder("Response");

  Viewer(DlgResource dlg)
  {
    this.dlg = dlg;
    this.dlg.addTableModelListener(this);

    ButtonPopupMenu bpmFind = (ButtonPopupMenu)ButtonPanel.createControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis});
    bpmFind.addItemListener(this);
    bpmFind.addActionListener(this);

    JButton bNextState = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextState.setMargin(new Insets(bNextState.getMargin().top, 0, bNextState.getMargin().bottom, 0));
    bNextState.addActionListener(this);

    JButton bPrevState = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevState.setMargin(bNextState.getMargin());
    bPrevState.addActionListener(this);

    JButton bNextTrans = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextTrans.setMargin(bNextState.getMargin());
    bNextTrans.addActionListener(this);

    JButton bPrevTrans = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevTrans.setMargin(bNextState.getMargin());
    bPrevTrans.addActionListener(this);

    JButton bSelect = new JButton("Select", Icons.getIcon(Icons.ICON_REDO_16));
    bSelect.addActionListener(this);

    JButton bReturn = new JButton("Return", Icons.getIcon(Icons.ICON_UNDO_16));
    bReturn.addActionListener(this);

    int width = (int)tfState.getPreferredSize().getWidth();
    int height = (int)bNextState.getPreferredSize().getHeight();
    tfState.setPreferredSize(new Dimension(width, height));
    tfResponse.setPreferredSize(new Dimension(width, height));
    tfState.setHorizontalAlignment(JTextField.CENTER);
    tfResponse.setHorizontalAlignment(JTextField.CENTER);
    tfState.addActionListener(this);
    tfResponse.addActionListener(this);
    stateTextPanel = new DlgPanel("Text", true);
    stateTriggerPanel = new DlgPanel("Trigger", false, true);
    transTextPanel = new DlgPanel("Text", true);
    transTriggerPanel = new DlgPanel("Trigger", false, true);
    transActionPanel = new DlgPanel("Action", false, true);

    JPanel statepanel = new JPanel();
    statepanel.setLayout(new GridLayout(2, 1, 6, 6));
    statepanel.add(stateTextPanel);
    statepanel.add(stateTriggerPanel);
    statepanel.setBorder(bostate);

    JPanel transpanel2 = new JPanel();
    transpanel2.setLayout(new GridLayout(1, 2, 6, 6));
    transpanel2.add(transTriggerPanel);
    transpanel2.add(transActionPanel);
    JPanel transpanel = new JPanel();
    transpanel.setLayout(new GridLayout(2, 1, 6, 6));
    transpanel.add(transTextPanel);
    transpanel.add(transpanel2);
    transpanel.setBorder(botrans);

    outerpanel = new JPanel();
    outerpanel.setLayout(new GridLayout(2, 1, 6, 6));
    outerpanel.add(statepanel);
    outerpanel.add(transpanel);

    buttonPanel.addControl(new JLabel("State:"));
    buttonPanel.addControl(tfState, CtrlStateField);
    buttonPanel.addControl(bPrevState, CtrlPrevState);
    buttonPanel.addControl(bNextState, CtrlNextState);
    buttonPanel.addControl(new JLabel(" Response:"));
    buttonPanel.addControl(tfResponse, CtrlResponseField);
    buttonPanel.addControl(bPrevTrans, CtrlPrevTrans);
    buttonPanel.addControl(bNextTrans, CtrlNextTrans);
    buttonPanel.addControl(bSelect, CtrlSelect);
    buttonPanel.addControl(bReturn, CtrlReturn);
    buttonPanel.addControl(bpmFind, ButtonPanel.Control.FIND_MENU);

    setLayout(new BorderLayout());
    add(outerpanel, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
    outerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    updateViewerLists();
    showState(stateList.isEmpty() ? -1 : 0);
    showTransition(currentState == null ? -1 : currentState.getFirstTrans());
  }

  public void setUndoDlg(DlgResource dlg)
  {
    this.undoDlg = dlg;
    buttonPanel.getControlByType(CtrlReturn).setEnabled(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (!alive) return;
    if (buttonPanel.getControlByType(CtrlReturn) == event.getSource()) {
      JButton bUndo = (JButton)event.getSource();
      if(lastStates.isEmpty() && (undoDlg != null)) {
        showExternState(undoDlg, -1, true);
        return;
      }
      State oldstate = lastStates.pop();
      Transition oldtrans = lastTransitions.pop();
      if (lastStates.isEmpty() && (undoDlg == null)) {
        bUndo.setEnabled(false);
      }
      if (oldstate != currentState) {
        showState(oldstate.getNumber());
      }
      if (oldtrans != currentTrans) {
        showTransition(oldtrans.getNumber());
      }
    } else {
      int newstate = currentState == null ? -1 : currentState.getNumber();
      int newtrans = currentTrans == null ? -1 : currentTrans.getNumber();
      if (buttonPanel.getControlByType(CtrlNextState) == event.getSource()) {
        newstate++;
      } else if (buttonPanel.getControlByType(CtrlPrevState) == event.getSource()) {
        newstate--;
      } else if (buttonPanel.getControlByType(CtrlNextTrans) == event.getSource()) {
        newtrans++;
      } else if (buttonPanel.getControlByType(CtrlPrevTrans) == event.getSource()) {
        newtrans--;
      } else if (event.getSource() == tfState) {
        try {
          int number = Integer.parseInt(tfState.getText());
          if (number >= 0 && number <= stateList.size()) {
            newstate = number;
          } else {
            tfState.setText(String.valueOf(currentState.getNumber()));
          }
        } catch (Exception e) {
          tfState.setText(String.valueOf(currentState.getNumber()));
        }
      } else if (event.getSource() == tfResponse) {
        try {
          int number = Integer.parseInt(tfResponse.getText());
          if (number >= 0 && number <= currentState.getTransCount()) {
            newtrans = currentState.getFirstTrans() + number;
          } else {
            tfResponse.setText(String.valueOf(currentTrans.getNumber() - currentState.getFirstTrans()));
          }
        } catch (Exception e) {
          tfResponse.setText(String.valueOf(currentTrans.getNumber() - currentState.getFirstTrans()));
        }
      } else if (buttonPanel.getControlByType(CtrlSelect) == event.getSource()) {
        final String nextDlgName = currentTrans.getNextDialog().getResourceName();
        if (dlg.getResourceEntry().getResourceName().equalsIgnoreCase(nextDlgName)) {
          lastStates.push(currentState);
          lastTransitions.push(currentTrans);
          buttonPanel.getControlByType(CtrlReturn).setEnabled(true);
          newstate = currentTrans.getNextDialogState();
        } else {
          DlgResource newdlg =
              (DlgResource)ResourceFactory.getResource(ResourceFactory.getResourceEntry(nextDlgName));
          showExternState(newdlg, currentTrans.getNextDialogState(), false);
        }
      }
      if (currentState != null && newstate != currentState.getNumber()) {
        showState(newstate);
        showTransition(currentState.getFirstTrans());
      } else
      if (currentTrans != null && newtrans != currentTrans.getNumber()) {
        showTransition(newtrans);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU) == event.getSource()) {
      ButtonPopupMenu bpmFind = (ButtonPopupMenu)event.getSource();
      if (bpmFind.getSelectedItem() == ifindall) {
        List<ResourceEntry> files = ResourceFactory.getResources("DLG");
        new DialogSearcher(files, getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<>();
        files.add(dlg.getResourceEntry());
        new DialogSearcher(files, getTopLevelAncestor());
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent e)
  {
    if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
      updateViewerLists();
    }
    showState(currentState == null ? -1 : currentState.getNumber());
    showTransition(currentTrans == null ? -1 : currentTrans.getNumber());
  }

// --------------------- End Interface TableModelListener ---------------------

  /** For quickly jump to the corresponding state while only having a StructEntry. */
  public void select(StructEntry entry)
  {
    try {
      int stateNrToShow = -1;
      int transNrToShow = -1;

      StructEntry dlgEntry = null;
      for (StructEntry ref = entry; ref != null; ref = ref.getParent())
        if (ref instanceof DlgResource)
          dlgEntry = ref;
      if (dlgEntry == null || !dlg.getResourceEntry().equals(((DlgResource)dlgEntry).getResourceEntry())) {
        throw new Exception("Source and target resource don't match.");
      }

      // we can have states, triggers, transitions and actions
      if (entry instanceof State) {
        stateNrToShow = ((State) entry).getNumber();
        transNrToShow = ((State) entry).getFirstTrans();
      }
      else if (entry instanceof Transition) {
        int transnr = ((Transition) entry).getNumber();
        stateNrToShow = findStateForTrans(transnr);
        transNrToShow = transnr;
      }
      else if (entry instanceof StateTrigger) {
        int triggerIndex = ((StateTrigger)entry).getNumber();
        State state = stateList
                      .stream()
                      .filter(s -> ((IsNumeric)s.getAttribute(State.DLG_STATE_TRIGGER_INDEX)).getValue() == triggerIndex)
                      .findFirst()
                      .orElse(null);
        if (state != null) {
          stateNrToShow = state.getNumber();
          transNrToShow = state.getFirstTrans();
        }
      }
      else if (entry instanceof ResponseTrigger) {
        int triggerIndex = ((ResponseTrigger)entry).getNumber();
        Transition trans = transList
                           .stream()
                           .filter(t -> ((Flag)t.getAttribute(Transition.DLG_TRANS_FLAGS)).isFlagSet(1) &&
                                        ((IsNumeric)t.getAttribute(Transition.DLG_TRANS_TRIGGER_INDEX)).getValue() == triggerIndex)
                           .findFirst()
                           .orElse(null);
        if (trans != null) {
          transNrToShow = trans.getNumber();
          stateNrToShow = findStateForTrans(transNrToShow);
        }
      }
      else if (entry instanceof Action) {
        int actionIndex = ((Action)entry).getNumber();
        Transition trans = transList
                           .stream()
                           .filter(t -> ((Flag)t.getAttribute(Transition.DLG_TRANS_FLAGS)).isFlagSet(2) &&
                                        ((IsNumeric)t.getAttribute(Transition.DLG_TRANS_ACTION_INDEX)).getValue() == actionIndex)
                           .findFirst()
                           .orElse(null);
        if (trans != null) {
          transNrToShow = trans.getNumber();
          stateNrToShow = findStateForTrans(transNrToShow);
        }
      }
      else if (entry instanceof StringRef) {
        // can be a child element of a state or transition
        for (StructEntry ref = entry; ref != null; ref = ref.getParent()) {
          if (ref instanceof State) {
            stateNrToShow = ((State)ref).getNumber();
            transNrToShow = ((State)ref).getFirstTrans();
            break;
          } else if (ref instanceof Transition) {
            transNrToShow = ((Transition)ref).getNumber();
            stateNrToShow = findStateForTrans(transNrToShow);
            break;
          }
        }
      }

      showState(stateNrToShow);
      showTransition(transNrToShow);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private int findStateForTrans(int transnr)
  {
    for (State state : stateList) {
      if ((transnr >= state.getFirstTrans()) &&
          (transnr < (state.getFirstTrans() + state.getTransCount()))) {
        return state.getNumber();
      }
    }
    // default
    return -1;
  }

  /**
   * Shows information about specified state of this dialog
   *
   * @param nr Number of state within {@link #dlg}
   */
  private void showState(int nr)
  {
    if (currentState != null) {
      currentState.removeTableModelListener(this);
      currentState = null;
    }
    final int cnt = stateList.size() - 1;

    final boolean isBroken = nr > cnt;
    final String cur = toString(nr);
    bostate.setTitle("State " + cur + '/' + toString(cnt) + (isBroken ? " (broken reference to state " + nr + ")" : ""));
    bostate.setTitleColor(isBroken ? ERROR_COLOR : NORMAL_COLOR);
    outerpanel.repaint();// Force repaint border
    tfState.setText(cur);
    // Do both range checks, just in case...
    buttonPanel.getControlByType(CtrlPrevState).setEnabled(nr > 0 && nr <= cnt);
    buttonPanel.getControlByType(CtrlNextState).setEnabled(nr >= 0 && nr < cnt);

    final boolean isValid = nr >= 0 && nr <= cnt;
    tfState.setEnabled(isValid);
    if (!isValid) {
      if (nr >= 0) {
        // Print warning about not correct resource
        System.err.println(dlg.getName() + ": state " + nr + " is not exist");
      }
      stateTextPanel.clearDisplay();
      stateTriggerPanel.clearDisplay();
      return;
    }

    currentState = stateList.get(nr);
    currentState.addTableModelListener(this);

    stateTextPanel.display(currentState, nr);

    final int trigger = currentState.getTriggerIndex();
    if (trigger != 0xffffffff) {
      stateTriggerPanel.display(staTriList, trigger);
    } else {
      stateTriggerPanel.clearDisplay();
    }
  }

  /**
   * Shows information about specified transition of this dialog
   *
   * @param nr Global number of transition within {@link #dlg}
   */
  private void showTransition(int nr)
  {
    if (currentTrans != null) {
      currentTrans.removeTableModelListener(this);
      currentTrans = null;
    }
    // Relative number of transition in the state
    final int num = currentState == null ? -1 : nr - currentState.getFirstTrans();
    final int cnt = currentState == null ? -1 : currentState.getTransCount() - 1;

    final boolean isBroken = nr >= transList.size() && currentState.getTransCount() > 0;
    final String cur = toString(num);
    botrans.setTitle("Response " + cur + '/' + toString(cnt) + (isBroken ? " (broken reference to response " + nr + ")" : ""));
    botrans.setTitleColor(isBroken ? ERROR_COLOR : NORMAL_COLOR);
    outerpanel.repaint();// Force repaint border
    tfResponse.setText(cur);
    // Do both range checks, just in case...
    buttonPanel.getControlByType(CtrlPrevTrans).setEnabled(num > 0 && num <= cnt);
    buttonPanel.getControlByType(CtrlNextTrans).setEnabled(num >= 0 && num < cnt);

    final boolean isValid = nr >= 0 && nr < transList.size();
    final boolean isCorrect = isValid && num >= 0 && num <= cnt;
    tfResponse.setEnabled(isCorrect);
    if (!isCorrect) {
      if (nr >= 0 && currentState != null) {
        // Print warning about not correct resource
        if (isValid) {
          System.err.println(dlg.getName() + ": transition " + nr + " is not transition from state " + currentState.getNumber());
        } else
        if (isBroken) {
          System.err.println(dlg.getName() + ": transition " + nr + " is not exist");
        }
      }
      transTextPanel.clearDisplay();
      transTriggerPanel.clearDisplay();
      transActionPanel.clearDisplay();
      buttonPanel.getControlByType(CtrlSelect).setEnabled(false);
      return;
    }

    currentTrans = transList.get(nr);
    currentTrans.addTableModelListener(this);

    transTextPanel.display(currentTrans, nr);

    final Flag flags = currentTrans.getFlag();
    if (flags.isFlagSet(1)) {// Bit 1: has trigger
      final int trigger = currentTrans.getTriggerIndex();
      transTriggerPanel.display(transTriList, trigger);
    } else {
      transTriggerPanel.clearDisplay();
    }
    if (flags.isFlagSet(2)) {// Bit 2: has action
      final int action = currentTrans.getActionIndex();
      transActionPanel.display(actionList, action);
    } else {
      transActionPanel.clearDisplay();
    }
    buttonPanel.getControlByType(CtrlSelect).setEnabled(!flags.isFlagSet(3));// Bit 3: terminate dialog
  }

  /**
   * Retrieves all kinds of its child structures from dialog and places them to
   * separate lists for fast access.
   */
  private void updateViewerLists()
  {
    stateList.clear();
    transList.clear();
    staTriList.clear();
    transTriList.clear();
    actionList.clear();
    for (final StructEntry entry : dlg.getFields()) {
      if (entry instanceof State) {
        stateList.add((State)entry);
      } else if (entry instanceof Transition) {
        transList.add((Transition)entry);
      } else if (entry instanceof StateTrigger) {
        staTriList.add((StateTrigger)entry);
      } else if (entry instanceof ResponseTrigger) {
        transTriList.add((ResponseTrigger)entry);
      } else if (entry instanceof Action) {
        actionList.add((Action)entry);
      }
    }
    if (currentState != null && !stateList.contains(currentState)) {
      currentState.removeTableModelListener(this);
      currentState = null;
    }
    if (currentTrans != null && !transList.contains(currentTrans)) {
      currentTrans.removeTableModelListener(this);
      currentTrans = null;
    }

    lastStates.retainAll(stateList);
    lastTransitions.retainAll(transList);
    if (lastStates.isEmpty() && (undoDlg == null)) {
      buttonPanel.getControlByType(CtrlReturn).setEnabled(false);
    }
  }

  private void showExternState(DlgResource newdlg, int state, boolean isUndo) {

    alive = false;
    Container window = getTopLevelAncestor();
    if (window instanceof ViewFrame && window.isVisible()) {
      ((ViewFrame) window).setViewable(newdlg);
    } else {
      NearInfinity.getInstance().setViewable(newdlg);
    }

    Viewer newdlg_viewer = (Viewer)newdlg.getViewerTab(0);
    if (isUndo) {
      newdlg_viewer.alive = true;
      newdlg_viewer.repaint(); // only necessary when dlg is in extra window
    } else {
      newdlg_viewer.setUndoDlg(this.dlg);
      newdlg_viewer.showState(state);
      newdlg_viewer.showTransition(newdlg_viewer.currentState == null ? -1 : newdlg_viewer.currentState.getFirstTrans());
    }

    // make sure the viewer tab is selected
    JTabbedPane parent = (JTabbedPane) newdlg_viewer.getParent();
    parent.getModel().setSelectedIndex(parent.indexOfComponent(newdlg_viewer));
  }

  /** Renders negative numbers as dash ({@code "-"}). */
  private static String toString(int value)
  {
    return value < 0 ? "-" : Integer.toString(value);
  }

// -------------------------- INNER CLASSES --------------------------

  private final class DlgPanel extends JPanel implements ActionListener
  {
    /** Button used to view {@link #struct} in the new {@link StructViewer} instance.  */
    private final JButton bView = new JButton(Icons.getIcon(Icons.ICON_ZOOM_16));
    /** Button used to open {@link #structEntry} in the table viewer. */
    private final JButton bGoto = new JButton(Icons.getIcon(Icons.ICON_ROW_INSERT_AFTER_16));
    /** Button used to open {@link #structEntry} in the tree viewer. */
    private final JButton bTree = new JButton(Icons.getIcon(Icons.ICON_SELECT_IN_TREE_16));
    private final JButton bPlay = new JButton(Icons.getIcon(Icons.ICON_VOLUME_16));
    private final ScriptTextArea textArea = new ScriptTextArea();
    private final JLabel label = new JLabel();
    private final String title;
    private AbstractStruct struct;
    private StructEntry structEntry;

    private DlgPanel(String title, boolean viewable)
    {
      this(title, viewable, false);
    }

    private DlgPanel(String title, boolean viewable, boolean useHighlighting)
    {
      this.title = title;
      final Insets insets = new Insets(0, 0, 0, 0);
      initButton(bGoto, insets, "Select attribute");
      initButton(bTree, insets, "Select in tree");
      initButton(bView, insets, "View/Edit");
      initButton(bPlay, insets, "Open associated sound");
      bTree.setVisible(false);

      if (!useHighlighting) {
        textArea.applyExtendedSettings(null, null);
        textArea.setFont(Misc.getScaledFont(textArea.getFont()));
      }
      textArea.setEditable(false);
      textArea.setHighlightCurrentLine(false);
      if (viewable) {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
      }
      textArea.setMargin(new Insets(3, 3, 3, 3));
      InfinityScrollPane scroll = new InfinityScrollPane(textArea, true);
      if (!useHighlighting) {
        scroll.setLineNumbersEnabled(false);
      }

      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();

      gbc.insets = new Insets(0, 3, 0, 0);
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      gbc.weighty = 0.0;
      gbc.anchor = GridBagConstraints.WEST;
      add(bGoto, gbc);
      add(bTree, gbc);
      if (viewable) {
        add(bView, gbc);
        add(bPlay, gbc);
      }

      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.insets.right = 3;
      add(label, gbc);

      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      add(scroll, gbc);
    }

    private void initButton(JButton button, Insets insets, String tooltip)
    {
      button.setMargin(insets);
      button.setToolTipText(tooltip);
      button.addActionListener(this);
    }

    private void display(State state, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(true);
      bGoto.setEnabled(true);
      bTree.setVisible(true);
      bTree.setEnabled(true);
      struct = state;
      structEntry = state;
      StringRef response = state.getAssociatedText();
      textArea.setText(response.toString() + "\n(StrRef: " + response.getValue() + ')');
      bPlay.setEnabled(!StringTable.getSoundResource(response.getValue()).isEmpty());
      textArea.setCaretPosition(0);
    }

    private void display(Transition trans, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(true);
      bGoto.setEnabled(true);
      bTree.setVisible(true);
      bTree.setEnabled(true);
      struct = trans;
      structEntry = trans;
      StringRef assText = trans.getAssociatedText();
      StringRef jouText = trans.getJournalEntry();
      String text = "";
      if (trans.getFlag().isFlagSet(0)) {
        text = assText.toString() + "\n(StrRef: " + assText.getValue() + ")\n";
      }
      if (trans.getFlag().isFlagSet(4)) {
        if (trans.getFlag().isFlagSet(6)) {
          text += "\nUnsolved journal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
        } else if (trans.getFlag().isFlagSet(7)) {
          text += "\nInfo journal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
        } else if (trans.getFlag().isFlagSet(8)) {
          text += "\nSolved journal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
        } else {
          text += "\nJournal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
        }
      }
      bPlay.setEnabled(!StringTable.getSoundResource(assText.getValue()).isEmpty());
      textArea.setText(text);
      textArea.setCaretPosition(0);
    }

    private void display(List<? extends AbstractCode> codes, int number)
    {
      final boolean isValid = number >= 0 && number < codes.size();

      label.setText(title + " (" + number + ')' + (isValid ? "" : " (broken reference)"));
      bView.setEnabled(false);
      bPlay.setEnabled(false);
      bGoto.setEnabled(true);
      bTree.setVisible(false);

      if (isValid) {// For example, Dialog DILQUIX.DLG in PST has broken format
        label.setForeground(NORMAL_COLOR);
        final AbstractCode code = codes.get(number);
        structEntry = code;
        final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
        final String text = code.getText();
        final Compiler compiler = new Compiler(text, type);
        final String compiled = compiler.getCode();
        try {
          if (compiler.getErrors().isEmpty()) {
            Decompiler decompiler = new Decompiler(compiled, type, true);
            textArea.setText(decompiler.getSource());
          } else {
            textArea.setText(text);
          }
        } catch (Exception e) {
          textArea.setText(text);
        }
        textArea.setCaretPosition(0);
      } else {
        label.setForeground(ERROR_COLOR);
        textArea.setText("");
      }
    }

    private void clearDisplay()
    {
      label.setText(title + " (-)");
      label.setForeground(NORMAL_COLOR);
      textArea.setText("");
      bView.setEnabled(false);
      bGoto.setEnabled(false);
      bTree.setEnabled(false);
      bPlay.setEnabled(false);
      struct = null;
      structEntry = null;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == bView) {
        new ViewFrame(getTopLevelAncestor(), struct);
      } else if (event.getSource() == bGoto) {
        dlg.getViewer().selectEntry(structEntry.getName());
      } else if (event.getSource() == bTree) {
        if (struct instanceof TreeItemEntry) {
          dlg.selectInTree((TreeItemEntry)struct);
        }
      } else if (event.getSource() == bPlay) {
        if (struct instanceof TreeItemEntry) {
          final int strRef = ((TreeItemEntry)struct).getAssociatedText().getValue();
          final String resourceName = StringTable.getSoundResource(strRef);
          if (!resourceName.isEmpty()) {
            ResourceEntry entry = ResourceFactory.getResourceEntry(resourceName + ".WAV");
            new ViewFrame(getTopLevelAncestor(), ResourceFactory.getResource(entry));
          }
        }
      }
    }
  }
}
