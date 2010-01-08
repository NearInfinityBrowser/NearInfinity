// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public final class DlgResource extends AbstractStruct implements Resource, HasAddRemovable, HasDetailViewer
{
  private static final String sNonInt[] = {"Pausing dialogue", "Turn hostile",
                                           "Escape area", "Ignore attack"};
  private SectionCount countState, countTrans, countStaTri, countTranTri, countAction;
  private SectionOffset offsetState, offsetTrans, offsetStaTri, offsetTranTri, offsetAction;
  private Viewer detailViewer;

  public DlgResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new State(), new Transition(), new StateTrigger(),
                              new ResponseTrigger(), new Action()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    if (detailViewer == null)
      detailViewer = new Viewer(this);
    return detailViewer;
  }

// --------------------- End Interface HasDetailViewer ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    offsetState.setValue(0x30);
    if (list.size() >= 13 && getStructEntryAt(12).getName().equalsIgnoreCase("Threat response"))
      offsetState.setValue(0x34);
    offsetTrans.setValue(offsetState.getValue() + 0x10 * countState.getValue());
    offsetStaTri.setValue(offsetTrans.getValue() + 0x20 * countTrans.getValue());
    offsetTranTri.setValue(offsetStaTri.getValue() + 0x8 * countStaTri.getValue());
    offsetAction.setValue(offsetTranTri.getValue() + 0x8 * countTranTri.getValue());
    int stringoff = offsetAction.getValue() + 0x8 * countAction.getValue();
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof AbstractCode)
        stringoff += ((AbstractCode)o).updateOffset(stringoff);
    }
    super.write(os);

    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof AbstractCode)
        ((AbstractCode)o).writeString(os);
    }
  }

// --------------------- End Interface Writeable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    list.add(version);
    if (!version.toString().equalsIgnoreCase("V1.0")) {
      list.clear();
      throw new Exception("Unsupported version: " + version);
    }

    countState = new SectionCount(buffer, offset + 8, 4, "# states",
                                  State.class);
    list.add(countState);
    offsetState = new SectionOffset(buffer, offset + 12, "States offset",
                                    State.class);
    list.add(offsetState);
    countTrans = new SectionCount(buffer, offset + 16, 4, "# responses",
                                  Transition.class);
    list.add(countTrans);
    offsetTrans = new SectionOffset(buffer, offset + 20, "Responses offset",
                                    Transition.class);
    list.add(offsetTrans);
    offsetStaTri = new SectionOffset(buffer, offset + 24, "State triggers offset",
                                     StateTrigger.class);
    list.add(offsetStaTri);
    countStaTri = new SectionCount(buffer, offset + 28, 4, "# state triggers",
                                   StateTrigger.class);
    list.add(countStaTri);
    offsetTranTri = new SectionOffset(buffer, offset + 32, "Response triggers offset",
                                      ResponseTrigger.class);
    list.add(offsetTranTri);
    countTranTri = new SectionCount(buffer, offset + 36, 4, "# response triggers",
                                    ResponseTrigger.class);
    list.add(countTranTri);
    offsetAction = new SectionOffset(buffer, offset + 40, "Actions offset",
                                     infinity.resource.dlg.Action.class);
    list.add(offsetAction);
    countAction = new SectionCount(buffer, offset + 44, 4, "# actions",
                                   infinity.resource.dlg.Action.class);
    list.add(countAction);

    if (offsetState.getValue() > 0x30)
      list.add(new Flag(buffer, offset + 48, 4, "Threat response", sNonInt));

    offset = offsetState.getValue();
    for (int i = 0; i < countState.getValue(); i++) {
      State state = new State(this, buffer, offset, i);
      offset = state.getEndOffset();
      list.add(state);
    }

    offset = offsetTrans.getValue();
    for (int i = 0; i < countTrans.getValue(); i++) {
      Transition transition = new Transition(this, buffer, offset, i);
      offset = transition.getEndOffset();
      list.add(transition);
    }

    int textSize = 0;
    offset = offsetStaTri.getValue();
    for (int i = 0; i < countStaTri.getValue(); i++) {
      StateTrigger statri = new StateTrigger(buffer, offset, i);
      offset += statri.getSize();
      textSize += statri.getTextLength();
      list.add(statri);
    }

    offset = offsetTranTri.getValue();
    for (int i = 0; i < countTranTri.getValue(); i++) {
      ResponseTrigger trantri = new ResponseTrigger(buffer, offset, i);
      offset += trantri.getSize();
      textSize += trantri.getTextLength();
      list.add(trantri);
    }

    offset = offsetAction.getValue();
    for (int i = 0; i < countAction.getValue(); i++) {
      Action action = new Action(buffer, offset, i);
      offset += action.getSize();
      textSize += action.getTextLength();
      list.add(action);
    }
    return offset + textSize;
  }

  // sorry for this (visibility)
  public void showStateWithStructEntry(StructEntry entry) {
    if (detailViewer == null) {
      getDetailViewer();
    }
    detailViewer.showStateWithStructEntry(entry);
  }
}

