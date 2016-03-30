// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.gui.StructViewer;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasAddRemovable;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;


public final class DlgResource extends AbstractStruct
    implements Resource, HasAddRemovable, HasViewerTabs, ChangeListener
{
  // DLG-specific field labels
  public static final String DLG_OFFSET_STATES            = "States offset";
  public static final String DLG_OFFSET_RESPONSES         = "Responses offset";
  public static final String DLG_OFFSET_STATE_TRIGGERS    = "State triggers offset";
  public static final String DLG_OFFSET_RESPONSE_TRIGGERS = "Response triggers offset";
  public static final String DLG_OFFSET_ACTIONS           = "Actions offset";
  public static final String DLG_NUM_STATES               = "# states";
  public static final String DLG_NUM_RESPONSES            = "# responses";
  public static final String DLG_NUM_STATE_TRIGGERS       = "# state triggers";
  public static final String DLG_NUM_RESPONSE_TRIGGERS    = "# response triggers";
  public static final String DLG_NUM_ACTIONS              = "# actions";
  public static final String DLG_THREAT_RESPONSE          = "Threat response";

  private static final String TAB_TREE  = "Tree";
  public static final String s_NonInt[] = {"Pausing dialogue", "Turn hostile",
                                           "Escape area", "Ignore attack"};
  private SectionCount countState, countTrans, countStaTri, countTranTri, countAction;
  private SectionOffset offsetState, offsetTrans, offsetStaTri, offsetTranTri, offsetAction;
  private Viewer detailViewer;
  private TreeViewer treeViewer;

  public DlgResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new State(), new Transition(), new StateTrigger(),
                              new ResponseTrigger(), new Action()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public boolean confirmRemoveEntry(AddRemovable entry) throws Exception
  {
    return true;
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 2;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0: return StructViewer.TAB_VIEW;
      case 1: return TAB_TREE;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
        if (detailViewer == null)
          detailViewer = new Viewer(this);
        return detailViewer;
      case 1:
        if (treeViewer == null)
          treeViewer = new TreeViewer(this);
        return treeViewer;
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

// --------------------- End Interface HasViewerTabs ---------------------

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    offsetState.setValue(0x30);
    if (getFieldCount() >= 13 && getField(12).getName().equalsIgnoreCase(DLG_THREAT_RESPONSE))
      offsetState.setValue(0x34);
    offsetTrans.setValue(offsetState.getValue() + 0x10 * countState.getValue());
    offsetStaTri.setValue(offsetTrans.getValue() + 0x20 * countTrans.getValue());
    offsetTranTri.setValue(offsetStaTri.getValue() + 0x8 * countStaTri.getValue());
    offsetAction.setValue(offsetTranTri.getValue() + 0x8 * countTranTri.getValue());
    int stringoff = offsetAction.getValue() + 0x8 * countAction.getValue();
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof AbstractCode) {
        stringoff += ((AbstractCode)o).updateOffset(stringoff);
      }
    }
    super.write(os);

    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof AbstractCode) {
        ((AbstractCode)o).writeString(os);
      }
    }
  }

// --------------------- End Interface Writeable ---------------------

// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (getViewer() != null) {
      if (getViewer().isTabSelected(getViewer().getTabIndex(TAB_TREE))) {
        initTreeView();
      }
    }
  }

// --------------------- End Interface ChangeListener ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    if (viewer.isTabSelected(getViewer().getTabIndex(TAB_TREE))) {
      initTreeView();
    }
    viewer.addTabChangeListener(this);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    updateReferences(datatype, true);
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    updateReferences(datatype, false);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    TextString version = new TextString(buffer, offset + 4, 4, COMMON_VERSION);
    addField(version);
    if (!version.toString().equalsIgnoreCase("V1.0")) {
      clearFields();
      throw new Exception("Unsupported version: " + version);
    }

    countState = new SectionCount(buffer, offset + 8, 4, DLG_NUM_STATES, State.class);
    addField(countState);
    offsetState = new SectionOffset(buffer, offset + 12, DLG_OFFSET_STATES, State.class);
    addField(offsetState);
    countTrans = new SectionCount(buffer, offset + 16, 4, DLG_NUM_RESPONSES, Transition.class);
    addField(countTrans);
    offsetTrans = new SectionOffset(buffer, offset + 20, DLG_OFFSET_RESPONSES, Transition.class);
    addField(offsetTrans);
    offsetStaTri = new SectionOffset(buffer, offset + 24, DLG_OFFSET_STATE_TRIGGERS, StateTrigger.class);
    addField(offsetStaTri);
    countStaTri = new SectionCount(buffer, offset + 28, 4, DLG_NUM_STATE_TRIGGERS, StateTrigger.class);
    addField(countStaTri);
    offsetTranTri = new SectionOffset(buffer, offset + 32, DLG_OFFSET_RESPONSE_TRIGGERS, ResponseTrigger.class);
    addField(offsetTranTri);
    countTranTri = new SectionCount(buffer, offset + 36, 4, DLG_NUM_RESPONSE_TRIGGERS, ResponseTrigger.class);
    addField(countTranTri);
    offsetAction = new SectionOffset(buffer, offset + 40, DLG_OFFSET_ACTIONS, org.infinity.resource.dlg.Action.class);
    addField(offsetAction);
    countAction = new SectionCount(buffer, offset + 44, 4, DLG_NUM_ACTIONS, org.infinity.resource.dlg.Action.class);
    addField(countAction);

    if (offsetState.getValue() > 0x30) {
      addField(new Flag(buffer, offset + 48, 4, DLG_THREAT_RESPONSE, s_NonInt));
    }

    offset = offsetState.getValue();
    for (int i = 0; i < countState.getValue(); i++) {
      State state = new State(this, buffer, offset, i);
      offset = state.getEndOffset();
      addField(state);
    }

    offset = offsetTrans.getValue();
    for (int i = 0; i < countTrans.getValue(); i++) {
      Transition transition = new Transition(this, buffer, offset, i);
      offset = transition.getEndOffset();
      addField(transition);
    }

    int textSize = 0;
    offset = offsetStaTri.getValue();
    for (int i = 0; i < countStaTri.getValue(); i++) {
      StateTrigger statri = new StateTrigger(buffer, offset, i);
      offset += statri.getSize();
      textSize += statri.getTextLength();
      addField(statri);
    }

    offset = offsetTranTri.getValue();
    for (int i = 0; i < countTranTri.getValue(); i++) {
      ResponseTrigger trantri = new ResponseTrigger(buffer, offset, i);
      offset += trantri.getSize();
      textSize += trantri.getTextLength();
      addField(trantri);
    }

    offset = offsetAction.getValue();
    for (int i = 0; i < countAction.getValue(); i++) {
      Action action = new Action(buffer, offset, i);
      offset += action.getSize();
      textSize += action.getTextLength();
      addField(action);
    }
    return offset + textSize;
  }

  // sorry for this (visibility)
  public void showStateWithStructEntry(StructEntry entry) {
    if (detailViewer == null) {
      getViewerTab(0);
    }
    detailViewer.showStateWithStructEntry(entry);
  }

  private void initTreeView()
  {
    WindowBlocker.blockWindow(NearInfinity.getInstance(), true);
    try {
      treeViewer.init();
    } finally {
      WindowBlocker.blockWindow(NearInfinity.getInstance(), false);
    }
  }

  // Updates trigger/action references in states and responses
  private void updateReferences(AddRemovable datatype, boolean added)
  {
    if (datatype instanceof StateTrigger) {
      StateTrigger trigger = (StateTrigger)datatype;
      int ofsStates = ((SectionOffset)getAttribute(DLG_OFFSET_STATES)).getValue();
      int numStates = ((SectionCount)getAttribute(DLG_NUM_STATES)).getValue();
      int ofsTriggers = ((SectionOffset)getAttribute(DLG_OFFSET_STATE_TRIGGERS)).getValue();
      int idxTrigger = (trigger.getOffset() - ofsTriggers) / trigger.getSize();

      // adjusting state trigger references
      while (numStates > 0) {
        State state = (State)getAttribute(ofsStates, false);
        if (state != null) {
          DecNumber dec = (DecNumber)state.getAttribute(State.DLG_STATE_TRIGGER_INDEX);
          if (dec.getValue() == idxTrigger) {
            if (added) {
              dec.incValue(1);
            } else {
              dec.setValue(-1);
            }
          } else if (dec.getValue() > idxTrigger) {
            if (added) {
              dec.incValue(1);
            } else {
              dec.incValue(-1);
            }
          }
          ofsStates += state.getSize();
        }
        numStates--;
      }
    } else if (datatype instanceof ResponseTrigger) {
      ResponseTrigger trigger = (ResponseTrigger)datatype;
      int ofsTrans = ((SectionOffset)getAttribute(DLG_OFFSET_RESPONSES)).getValue();
      int numTrans = ((SectionCount)getAttribute(DLG_NUM_RESPONSES)).getValue();
      int ofsTriggers = ((SectionOffset)getAttribute(DLG_OFFSET_RESPONSE_TRIGGERS)).getValue();
      int idxTrigger = (trigger.getOffset() - ofsTriggers) / trigger.getSize();

      // adjusting response trigger references
      while (numTrans > 0) {
        Transition trans = (Transition)getAttribute(ofsTrans, false);
        if (trans != null) {
          Flag flags = (Flag)trans.getAttribute(Transition.DLG_TRANS_FLAGS);
          if (flags.isFlagSet(1)) {
            DecNumber dec = (DecNumber)trans.getAttribute(Transition.DLG_TRANS_TRIGGER_INDEX);
            if (dec.getValue() == idxTrigger) {
              if (added) {
                dec.incValue(1);
              } else {
                flags.setValue(flags.getValue() & ~2);
                dec.setValue(0);
              }
            } else if (dec.getValue() > idxTrigger) {
              if (added) {
                dec.incValue(1);
              } else {
                dec.incValue(-1);
              }
            }
          }
          ofsTrans += trans.getSize();
        }
        numTrans--;
      }
    } else if (datatype instanceof Action) {
      Action action = (Action)datatype;
      int ofsTrans = ((SectionOffset)getAttribute(DLG_OFFSET_RESPONSES)).getValue();
      int numTrans = ((SectionCount)getAttribute(DLG_NUM_RESPONSES)).getValue();
      int ofsActions = ((SectionOffset)getAttribute(DLG_OFFSET_ACTIONS)).getValue();
      int idxAction = (action.getOffset() - ofsActions) / action.getSize();

      // adjusting action references
      while (numTrans > 0) {
        Transition trans = (Transition)getAttribute(ofsTrans, false);
        if (trans != null) {
          Flag flags = (Flag)trans.getAttribute(Transition.DLG_TRANS_FLAGS);
          if (flags.isFlagSet(2)) {
            DecNumber dec = (DecNumber)trans.getAttribute(Transition.DLG_TRANS_ACTION_INDEX);
            if (dec.getValue() == idxAction) {
              if (added) {
                dec.incValue(1);
              } else {
                flags.setValue(flags.getValue() & ~4);
                dec.setValue(0);
              }
            } else if (dec.getValue() > idxAction) {
              if (added) {
                dec.incValue(1);
              } else {
                dec.incValue(-1);
              }
            }
          }
          ofsTrans += trans.getSize();
        }
        numTrans--;
      }
    }
  }
}

