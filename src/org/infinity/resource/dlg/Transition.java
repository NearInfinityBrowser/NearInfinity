// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class Transition extends AbstractStruct implements AddRemovable
{
  // DLG/Transition-specific field labels
  public static final String DLG_TRANS = "Response";
  public static final String DLG_TRANS_FLAGS              = "Flags";
  public static final String DLG_TRANS_TEXT               = "Associated text";
  public static final String DLG_TRANS_JOURNAL_ENTRY      = "Journal entry";
  public static final String DLG_TRANS_TRIGGER_INDEX      = "Trigger index";
  public static final String DLG_TRANS_ACTION_INDEX       = "Action index";
  public static final String DLG_TRANS_NEXT_DIALOG        = "Next dialogue";
  public static final String DLG_TRANS_NEXT_DIALOG_STATE  = "Next dialogue state";

  private static final String[] s_flag = {"No flags set", "Text associated", "Trigger", "Action",
                                          "Terminates dialogue", "Journal entry", "", "Add unsolved quest",
                                          "Add journal note", "Add solved quest"};
  private int nr;

  Transition() throws Exception
  {
    super(null, DLG_TRANS, StreamUtils.getByteBuffer(32), 0);
  }

  Transition(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, DLG_TRANS + " " + nr, buffer, offset);
    this.nr = nr;
  }

  public int getActionIndex()
  {
    if (getFlag().isFlagSet(2)) {
      return ((DecNumber)getAttribute(DLG_TRANS_ACTION_INDEX)).getValue();
    } else {
      return -1;
    }
  }

  public StringRef getAssociatedText()
  {
    return (StringRef)getAttribute(DLG_TRANS_TEXT);
  }

  public Flag getFlag()
  {
    return (Flag)getAttribute(DLG_TRANS_FLAGS);
  }

  public StringRef getJournalEntry()
  {
    return (StringRef)getAttribute(DLG_TRANS_JOURNAL_ENTRY);
  }

  public ResourceRef getNextDialog()
  {
    return (ResourceRef)getAttribute(DLG_TRANS_NEXT_DIALOG);
  }

  public int getNextDialogState()
  {
    return ((DecNumber)getAttribute(DLG_TRANS_NEXT_DIALOG_STATE)).getValue();
  }

  public int getNumber()
  {
    return nr;
  }

  public int getTriggerIndex()
  {
    if (getFlag().isFlagSet(1)) {
      return ((DecNumber)getAttribute(DLG_TRANS_TRIGGER_INDEX)).getValue();
    } else {
      return -1;
    }
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new Flag(buffer, offset, 4, DLG_TRANS_FLAGS, s_flag));
    addField(new StringRef(buffer, offset + 4, DLG_TRANS_TEXT));
    addField(new StringRef(buffer, offset + 8, DLG_TRANS_JOURNAL_ENTRY));
    addField(new DecNumber(buffer, offset + 12, 4, DLG_TRANS_TRIGGER_INDEX));
    addField(new DecNumber(buffer, offset + 16, 4, DLG_TRANS_ACTION_INDEX));
    addField(new ResourceRef(buffer, offset + 20, DLG_TRANS_NEXT_DIALOG, "DLG"));
    addField(new DecNumber(buffer, offset + 28, 4, DLG_TRANS_NEXT_DIALOG_STATE));
    return offset + 32;
  }
}

