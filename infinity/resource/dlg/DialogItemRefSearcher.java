package infinity.resource.dlg;

import infinity.resource.StructEntry;
import infinity.search.ReferenceHitFrame;

import java.awt.Component;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;


public class DialogItemRefSearcher implements Runnable {

  private final DlgResource dlg;
  private final Object item;
  private final Component parent;
  private final ReferenceHitFrame hitFrame;

  public DialogItemRefSearcher(DlgResource dlg, Object item, Component parent) {
    this.dlg = dlg;
    this.parent = parent;
    this.item = item;
    hitFrame = new ReferenceHitFrame(item, parent);
    new Thread(this).start();
  }

  public void run() {
    List<StructEntry> searchItems = dlg.getList();
    ProgressMonitor progress = new ProgressMonitor(parent, "Searching...", null, 0, searchItems.size());
    progress.setMillisToDecideToPopup(100);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < searchItems.size(); i++) {
      StructEntry entry = searchItems.get(i);
      if (entry instanceof State || entry instanceof Transition || entry instanceof AbstractCode) {
        search(entry);
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(parent, "Search canceled", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
      }
    }
    System.out.println("Search completed: " + (System.currentTimeMillis() - startTime) + "ms.");
    hitFrame.setVisible(true);
  }

  void search(StructEntry entry) {
    boolean found = false;

    // ugly dispatching, depending on what we have and what we look for
    if (item instanceof State) {
      // check transitions
      if (entry instanceof Transition) {
        State state = (State) item;
        int stateNumber = state.getNumber();
        Transition trans = (Transition) entry;
        String nextDialog = trans.getNextDialog().getResourceName();
        if ((trans.getNextDialogState() == stateNumber)
            && (nextDialog.equalsIgnoreCase(dlg.getName()))) {
          found = true;
        }
      }
    }
    else if (item instanceof Transition) {
      // check states
      if (entry instanceof State) {
        Transition trans = (Transition) item;
        State state = (State) entry;
        int transNumber = trans.getNumber();
        int firstTrans = state.getFirstTrans();
        int transCount = state.getTransCount();
        if ((transNumber >= firstTrans)
            && (transNumber < (firstTrans + transCount))) {
          found = true;
        }
      }
    }
    else if (item instanceof StateTrigger) {
      // check states
      if (entry instanceof State) {
        StateTrigger trigger = (StateTrigger) item;
        State state = (State) entry;
        int triggerNumber = getIndexFromName(trigger.getName());
        if (triggerNumber == state.getTriggerIndex()) {
          found = true;
        }
      }
    }
    else if (item instanceof ResponseTrigger) {
      // check transitions
      if (entry instanceof Transition) {
        ResponseTrigger trigger = (ResponseTrigger) item;
        Transition trans = (Transition) entry;
        int triggerNumber = getIndexFromName(trigger.getName());
        if (triggerNumber == trans.getTriggerIndex()) {
          found = true;
        }
      }
    }
    else if (item instanceof Action) {
      // check transitions
      if (entry instanceof Transition) {
        Action action = (Action) item;
        Transition trans = (Transition) entry;
        int actionNumber = getIndexFromName(action.getName());
        if (actionNumber == trans.getActionIndex()) {
          found = true;
        }
      }
    }

    if (found) {
      hitFrame.addHit(dlg.getResourceEntry(), entry.getName(), entry);
    }
  }

  // tries to return the last number in the name
  private int getIndexFromName(String name) {
    int posSpace = name.lastIndexOf(' ');
    if (posSpace != -1) {
      try {
        return Integer.parseInt(name.substring(posSpace + 1));
      } catch (NumberFormatException nfe) {}
    }
    return -1;
  }
}