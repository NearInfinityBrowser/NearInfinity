// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.NearInfinity;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.key.ResourceEntry;
import infinity.search.DialogSearcher;
import infinity.util.StringResource;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

final class Viewer extends JPanel implements ActionListener, ItemListener, TableModelListener
{
  private final ButtonPopupMenu bfind;
  private final DlgPanel stateTextPanel, stateTriggerPanel, transTextPanel, transTriggerPanel, transActionPanel;
  private final DlgResource dlg;
  private final JButton bnextstate = new JButton(Icons.getIcon("Forward16.gif"));
  private final JButton bprevstate = new JButton(Icons.getIcon("Back16.gif"));
  private final JButton bnexttrans = new JButton(Icons.getIcon("Forward16.gif"));
  private final JButton bprevtrans = new JButton(Icons.getIcon("Back16.gif"));
  private final JButton bselect = new JButton("Select", Icons.getIcon("Redo16.gif"));
  private final JButton bundo = new JButton("Undo", Icons.getIcon("Undo16.gif"));
  private final JMenuItem ifindall = new JMenuItem("in all DLG files");
  private final JMenuItem ifindthis = new JMenuItem("in this file only");
  private final JPanel outerpanel;
  private final JTextField tfState = new JTextField(4);
  private final JTextField tfResponse = new JTextField(4);
  private final List<Action> actionList = new ArrayList<Action>();
  private final List<ResponseTrigger> transTriList = new ArrayList<ResponseTrigger>();
  private final List<State> stateList = new ArrayList<State>();
  private final List<StateTrigger> staTriList = new ArrayList<StateTrigger>();
  private final List<Transition> transList = new ArrayList<Transition>();
  private final Stack<State> lastStates = new Stack<State>();
  private final Stack<Transition> lastTransitions = new Stack<Transition>();
  private final TitledBorder bostate = new TitledBorder("State");
  private final TitledBorder botrans = new TitledBorder("Response");
  private State currentstate;
  private Transition currenttransition;
  private boolean alive = true;
  private DlgResource undoDlg;

  Viewer(DlgResource dlg)
  {
    this.dlg = dlg;
    bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindall, ifindthis});
    bfind.addItemListener(this);
    bfind.setIcon(Icons.getIcon("Find16.gif"));

    dlg.addTableModelListener(this);
    bnextstate.setMargin(new Insets(bnextstate.getMargin().top, 0, bnextstate.getMargin().bottom, 0));
    bprevstate.setMargin(bnextstate.getMargin());
    bnexttrans.setMargin(bnextstate.getMargin());
    bprevtrans.setMargin(bnextstate.getMargin());
    int width = (int)tfState.getPreferredSize().getWidth();
    int height = (int)bnextstate.getPreferredSize().getHeight();
    tfState.setPreferredSize(new Dimension(width, height));
    tfResponse.setPreferredSize(new Dimension(width, height));
    tfState.setHorizontalAlignment(JTextField.CENTER);
    tfResponse.setHorizontalAlignment(JTextField.CENTER);
    tfState.addActionListener(this);
    tfResponse.addActionListener(this);
    bnextstate.addActionListener(this);
    bprevstate.addActionListener(this);
    bnexttrans.addActionListener(this);
    bprevtrans.addActionListener(this);
    bselect.addActionListener(this);
    bundo.addActionListener(this);
    bfind.addActionListener(this);
    stateTextPanel = new DlgPanel("Text", true);
    stateTriggerPanel = new DlgPanel("Trigger", false);
    transTextPanel = new DlgPanel("Text", true);
    transTriggerPanel = new DlgPanel("Trigger", false);
    transActionPanel = new DlgPanel("Action", false);

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

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
    bpanel.add(new JLabel("State:"));
    bpanel.add(tfState);
    bpanel.add(bprevstate);
    bpanel.add(bnextstate);
    bpanel.add(new JLabel(" Response:"));
    bpanel.add(tfResponse);
    bpanel.add(bprevtrans);
    bpanel.add(bnexttrans);
    bpanel.add(bselect);
    bpanel.add(bundo);
    bpanel.add(bfind);

    setLayout(new BorderLayout());
    add(outerpanel, BorderLayout.CENTER);
    add(bpanel, BorderLayout.SOUTH);
    outerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    updateViewerLists();

    if (stateList.size() > 0) {
      showState(0);
      showTransition(currentstate.getFirstTrans());
    }
    else {
      bprevstate.setEnabled(false);
      bnextstate.setEnabled(false);
      bprevtrans.setEnabled(false);
      bnexttrans.setEnabled(false);
      bselect.setEnabled(false);
    }
    bundo.setEnabled(false);
  }

  public void setUndoDlg(DlgResource dlg) {
    this.undoDlg = dlg;
    bundo.setEnabled(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (!alive) return;
    if (event.getSource() == bundo) {
      if(lastStates.empty() && (undoDlg != null)) {
        showExternState(undoDlg, -1, true);
        return;
      }
      State oldstate = lastStates.pop();
      Transition oldtrans = lastTransitions.pop();
      if (lastStates.empty() && (undoDlg == null)) {
        bundo.setEnabled(false);
      }
      //bundo.setEnabled(lastStates.size() > 0);
      if (oldstate != currentstate)
        showState(oldstate.getNumber());
      if (oldtrans != currenttransition)
        showTransition(oldtrans.getNumber());
    }
    else {
      int newstate = currentstate.getNumber();
      int newtrans = currenttransition.getNumber();
      if (event.getSource() == bnextstate)
        newstate++;
      else if (event.getSource() == bprevstate)
        newstate--;
      else if (event.getSource() == bnexttrans)
        newtrans++;
      else if (event.getSource() == bprevtrans)
        newtrans--;
      else if (event.getSource() == tfState) {
        try {
          int number = Integer.parseInt(tfState.getText());
          if (number > 0 && number <= stateList.size())
            newstate = number - 1;
          else
            tfState.setText(String.valueOf(currentstate.getNumber() + 1));
        } catch (Exception e) {
          tfState.setText(String.valueOf(currentstate.getNumber() + 1));
        }
      }
      else if (event.getSource() == tfResponse) {
        try {
          int number = Integer.parseInt(tfResponse.getText());
          if (number > 0 && number <= currentstate.getTransCount())
            newtrans = currentstate.getFirstTrans() + number - 1;
          else
            tfResponse.setText(
                    String.valueOf(currenttransition.getNumber() - currentstate.getFirstTrans() + 1));
        } catch (Exception e) {
          tfResponse.setText(
                  String.valueOf(currenttransition.getNumber() - currentstate.getFirstTrans() + 1));
        }
      }
      else if (event.getSource() == bselect) {
        ResourceRef next_dlg = currenttransition.getNextDialog();
        if (dlg.getResourceEntry().toString().equalsIgnoreCase(next_dlg.toString())) {
          lastStates.push(currentstate);
          lastTransitions.push(currenttransition);
          bundo.setEnabled(true);
          newstate = currenttransition.getNextDialogState();
        }
        else {
          DlgResource newdlg = (DlgResource)ResourceFactory.getResource(
              ResourceFactory.getInstance().getResourceEntry(next_dlg.toString()));
          showExternState(newdlg, currenttransition.getNextDialogState(), false);
        }
      }
      if (alive) {
        if (newstate != currentstate.getNumber()) {
          showState(newstate);
          showTransition(stateList.get(newstate).getFirstTrans());
        }
        else if (newtrans != currenttransition.getNumber())
          showTransition(newtrans);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bfind) {
      if (bfind.getSelectedItem() == ifindall) {
        List<ResourceEntry> files = ResourceFactory.getInstance().getResources("DLG");
        new DialogSearcher(files, getTopLevelAncestor());
      }
      else if (bfind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<ResourceEntry>();
        files.add(dlg.getResourceEntry());
        new DialogSearcher(files, getTopLevelAncestor());
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  public void tableChanged(TableModelEvent e)
  {
    updateViewerLists();
    showState(currentstate.getNumber());
    showTransition(currenttransition.getNumber());
  }

// --------------------- End Interface TableModelListener ---------------------

  // for quickly jump to the corresponding state while only having a StructEntry
  public void showStateWithStructEntry(StructEntry entry)
  {
    int stateNrToShow = 0;
    int transNrToShow = 0;

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
      int triggerOffset = ((StateTrigger) entry).getOffset();
      int nr = 0;
      for (StateTrigger trig : staTriList) {
        if (trig.getOffset() == triggerOffset)
          break;
        nr++;
      }

      for (State state : stateList) {
        if (state.getTriggerIndex() == nr) {
          stateNrToShow = state.getNumber();
          transNrToShow = state.getFirstTrans();
          break;
        }
      }
    }
    else if (entry instanceof ResponseTrigger) {
      int triggerOffset = ((ResponseTrigger) entry).getOffset();
      int nr = 0;
      for (ResponseTrigger trig : transTriList) {
        if (trig.getOffset() == triggerOffset)
          break;
        nr++;
      }

      for (Transition trans : transList) {
        if (trans.getTriggerIndex() == nr) {
          transNrToShow = trans.getNumber();
          stateNrToShow = findStateForTrans(transNrToShow);
        }
      }
    }
    else if (entry instanceof Action) {
      int actionOffset = ((Action) entry).getOffset();
      int nr = 0;
      for (Action action : actionList) {
        if (action.getOffset() == actionOffset)
          break;
        nr++;
      }

      for (Transition trans : transList) {
        if (trans.getActionIndex() == nr) {
          transNrToShow = trans.getNumber();
          stateNrToShow = findStateForTrans(transNrToShow);
        }
      }
    }
    else if (entry instanceof StringRef) {
      // this can happen with the dlg search
      // check all states and transitions
      int strref = ((StringRef) entry).getValue();
      boolean found = false;
      for (State state : stateList) {
        if (state.getResponse().getValue() == strref) {
          stateNrToShow = state.getNumber();
          transNrToShow = state.getFirstTrans();
          found = true;
        }
      }
      if (!found) {
        for (Transition trans : transList) {
          if (trans.getAssociatedText().getValue() == strref) {
            transNrToShow = trans.getNumber();
            stateNrToShow = findStateForTrans(transNrToShow);
          }
        }
      }
    }

    showState(stateNrToShow);
    showTransition(transNrToShow);
  }

  private int findStateForTrans(int transnr) {
    for (State state : stateList) {
      if ((transnr >= state.getFirstTrans())
          && (transnr < (state.getFirstTrans() + state.getTransCount()))) {
        return state.getNumber();
      }
    }
    // default
    return 0;
  }

  private void showState(int nr)
  {
    if (currentstate != null)
      currentstate.removeTableModelListener(this);
    currentstate = stateList.get(nr);
    currentstate.addTableModelListener(this);
    bostate.setTitle("State " + (nr + 1) + '/' + stateList.size());
    stateTextPanel.display(currentstate, nr);
    tfState.setText(String.valueOf(nr + 1));
    outerpanel.repaint();
    if (currentstate.getTriggerIndex() != 0xffffffff)
      stateTriggerPanel.display(staTriList.get(currentstate.getTriggerIndex()),
                                currentstate.getTriggerIndex());
    else
      stateTriggerPanel.clearDisplay();
    bprevstate.setEnabled(nr > 0);
    bnextstate.setEnabled(nr + 1 < stateList.size());
  }

  private void showTransition(int nr)
  {
    if (currenttransition != null)
      currenttransition.removeTableModelListener(this);
    currenttransition = transList.get(nr);
    currenttransition.addTableModelListener(this);
    botrans.setTitle("Response " + (nr - currentstate.getFirstTrans() + 1) +
                     '/' + currentstate.getTransCount());
    tfResponse.setText(String.valueOf(nr - currentstate.getFirstTrans() + 1));
    outerpanel.repaint();
    transTextPanel.display(currenttransition, nr);
    if (currenttransition.getFlag().isFlagSet(1))
      transTriggerPanel.display(transTriList.get(currenttransition.getTriggerIndex()),
                                currenttransition.getTriggerIndex());
    else
      transTriggerPanel.clearDisplay();
    if (currenttransition.getFlag().isFlagSet(2))
      transActionPanel.display(actionList.get(currenttransition.getActionIndex()),
                               currenttransition.getActionIndex());
    else
      transActionPanel.clearDisplay();
    bselect.setEnabled(!currenttransition.getFlag().isFlagSet(3));
    bprevtrans.setEnabled(nr > currentstate.getFirstTrans());
    bnexttrans.setEnabled(nr - currentstate.getFirstTrans() + 1 < currentstate.getTransCount());
  }

  private void updateViewerLists()
  {
    stateList.clear();
    transList.clear();
    staTriList.clear();
    transTriList.clear();
    actionList.clear();
    for (int i = 0; i < dlg.getRowCount(); i++) {
      StructEntry entry = dlg.getStructEntryAt(i);
      if (entry instanceof State)
        stateList.add((State)entry);
      else if (entry instanceof Transition)
        transList.add((Transition)entry);
      else if (entry instanceof StateTrigger)
        staTriList.add((StateTrigger)entry);
      else if (entry instanceof ResponseTrigger)
        transTriList.add((ResponseTrigger)entry);
      else if (entry instanceof Action)
        actionList.add((Action)entry);
    }
  }

  private void showExternState(DlgResource newdlg, int state, boolean isUndo) {

    alive = false;
    Container window = getTopLevelAncestor();
    if (window instanceof ViewFrame && window.isVisible())
      ((ViewFrame) window).setViewable(newdlg);
    else
      NearInfinity.getInstance().setViewable(newdlg);

    Viewer newdlg_viewer = (Viewer) newdlg.getDetailViewer();
    if (isUndo) {
      newdlg_viewer.alive = true;
      newdlg_viewer.repaint(); // only necessary when dlg is in extra window
    }
    else {
      newdlg_viewer.setUndoDlg(this.dlg);
      newdlg_viewer.showState(state);
      newdlg_viewer.showTransition(newdlg_viewer.currentstate.getFirstTrans());
    }

    // make sure the viewer tab is selected
    JTabbedPane parent = (JTabbedPane) newdlg_viewer.getParent();
    parent.getModel().setSelectedIndex(parent.indexOfComponent(newdlg_viewer));
  }

// -------------------------- INNER CLASSES --------------------------

  private final class DlgPanel extends JPanel implements ActionListener
  {
    private final JButton bView = new JButton(Icons.getIcon("Zoom16.gif"));
    private final JButton bGoto = new JButton(Icons.getIcon("RowInsertAfter16.gif"));
    private final JButton bPlay = new JButton(Icons.getIcon("Volume16.gif"));
    private final ScriptTextArea textArea = new ScriptTextArea();
    private final JLabel label = new JLabel();
    private final String title;
    private AbstractStruct struct;
    private StructEntry structEntry;

    private DlgPanel(String title, boolean viewable)
    {
      this.title = title;
      bView.setMargin(new Insets(0, 0, 0, 0));
      bView.addActionListener(this);
      bGoto.setMargin(bView.getMargin());
      bGoto.addActionListener(this);
      bPlay.setMargin(bView.getMargin());
      bPlay.addActionListener(this);
      bView.setToolTipText("View/Edit");
      bGoto.setToolTipText("Select attribute");
      bPlay.setToolTipText("Open associated sound");
      textArea.setEditable(false);
      if (viewable) {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
      }
      textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      textArea.setFont(BrowserMenuBar.getInstance().getScriptFont());
      JScrollPane scroll = new JScrollPane(textArea);

      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      setLayout(gbl);

      gbc.insets = new Insets(0, 3, 0, 0);
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      gbc.weighty = 0.0;
      gbc.anchor = GridBagConstraints.WEST;
      gbl.setConstraints(bGoto, gbc);
      add(bGoto);
      if (viewable) {
        gbl.setConstraints(bView, gbc);
        add(bView);
        gbl.setConstraints(bPlay, gbc);
        add(bPlay);
      }

      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.insets.right = 3;
      gbl.setConstraints(label, gbc);
      add(label);

      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      gbl.setConstraints(scroll, gbc);
      add(scroll);
    }

    private void display(State state, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(true);
      bGoto.setEnabled(true);
      struct = state;
      structEntry = state;
      StringRef response = state.getResponse();
      textArea.setText(response.toString() + "\n(StrRef: " + response.getValue() + ')');
      bPlay.setEnabled(StringResource.getResource(response.getValue()) != null);
      textArea.setCaretPosition(0);
    }

    private void display(Transition trans, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(true);
      bGoto.setEnabled(true);
      struct = trans;
      structEntry = trans;
      StringRef assText = trans.getAssociatedText();
      StringRef jouText = trans.getJournalEntry();
      String text = "";
      if (trans.getFlag().isFlagSet(0))
        text = assText.toString() + "\n(StrRef: " + assText.getValue() + ")\n";
      if (trans.getFlag().isFlagSet(4))
        text += "\nJournal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
      bPlay.setEnabled(StringResource.getResource(assText.getValue()) != null);
      textArea.setText(text);
      textArea.setCaretPosition(0);
    }

    private void display(AbstractCode trigger, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(false);
      bPlay.setEnabled(false);
      bGoto.setEnabled(true);
      structEntry = trigger;
      String code = Compiler.getInstance().compileDialogCode(trigger.toString(), trigger instanceof Action);
      try {
        if (Compiler.getInstance().getErrors().size() == 0) {
          if (trigger instanceof Action)
            textArea.setText(Decompiler.decompileDialogAction(code, true));
          else
            textArea.setText(Decompiler.decompileDialogTrigger(code, true));
        }
        else
          textArea.setText(trigger.toString());
      } catch (Exception e) {
        textArea.setText(trigger.toString());
      }
      textArea.setCaretPosition(0);
    }

    private void clearDisplay()
    {
      label.setText(title + " (-)");
      textArea.setText("");
      bView.setEnabled(false);
      bGoto.setEnabled(false);
      struct = null;
      structEntry = null;
    }

    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == bView)
        new ViewFrame(getTopLevelAncestor(), struct);
      else if (event.getSource() == bGoto)
        dlg.getViewer().selectEntry(structEntry.getName());
      else if (event.getSource() == bPlay) {
        StringRef text = null;
        if (struct instanceof State)
          text = ((State)struct).getResponse();
        else if (struct instanceof Transition)
          text = ((Transition)struct).getAssociatedText();
        if (text != null) {
          String resourceName = StringResource.getResource(text.getValue()) + ".WAV";
          if (resourceName != null) {
            ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(resourceName);
            new ViewFrame(getTopLevelAncestor(), ResourceFactory.getResource(entry));
          }
        }
      }
    }
  }
}

