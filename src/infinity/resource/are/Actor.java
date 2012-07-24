// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.cre.CreResource;

import javax.swing.*;

public final class Actor extends AbstractStruct implements AddRemovable, HasDetailViewer, HasAddRemovable
{
  public static final String[] s_orientation = { "South", "SSW", "SW", "WSW", "West", "WNW", "NW", "NNW",
                                                 "North", "NNE", "NE", "ENE", "East", "ESE", "SE", "SSE" };
  private static final String[] s_noyes = {"No", "Yes"};
  private static final String[] s_yesno = {"CRE attached", "CRE not attached", "Has seen party",
                                           "Toggle invulnerability", "Override script name"};
  static final String[] s_schedule = {"Not active", "00:30-01:29", "01:30-02:29", "02:30-03:29",
                                      "03:30-04:29", "04:30-05:29", "05:30-06:29", "06:30-07:29",
                                      "07:30-08:29", "08:30-09:29", "09:30-10:29", "10:30-11:29",
                                      "11:30-12:29", "12:30-13:29", "13:30-14:29", "14:30-15:29",
                                      "15:30-16:29", "16:30-17:29", "17:30-18:29", "18:30-19:29",
                                      "19:30-20:29", "20:30-21:29", "21:30-22:29", "22:30-23:29",
                                      "23:30-00:29"};
  private static final String[] s_diff = {"None", "Level 1", "Level 2", "Level 3"};

  public Actor() throws Exception
  {
    super(null, "Actor", new byte[272], 0);
  }

  public Actor(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Actor " + nr, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables()
  {
    return new AddRemovable[]{};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    return new ViewerActor(this);
  }

// --------------------- End Interface HasDetailViewer ---------------------

  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (child instanceof CreResource)
      ((DecNumber)getAttribute("CRE structure size")).setValue(child.getSize());
  }

  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (datatype instanceof CreResource) {
      ((DecNumber)getAttribute("CRE structure size")).setValue(0);
      ((HexNumber)getAttribute("CRE structure offset")).setValue(0);
    }
  }

  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (child instanceof CreResource)
      ((DecNumber)getAttribute("CRE structure size")).setValue(child.getSize());
  }

  void updateCREOffset()
  {
    StructEntry entry = getStructEntryAt(getRowCount() - 1);
    if (entry instanceof CreResource)
      ((HexNumber)getAttribute("CRE structure offset")).setValue(entry.getOffset());
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Position: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Position: Y"));
    list.add(new DecNumber(buffer, offset + 36, 2, "Destination: X"));
    list.add(new DecNumber(buffer, offset + 38, 2, "Destination: Y"));
//    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
//        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB)
      list.add(new Flag(buffer, offset + 40, 4, "Loading", s_yesno));
//    else
//      list.add(new Bitmap(buffer, offset + 40, 4, "Is visible?", s_noyes));
    list.add(new Bitmap(buffer, offset + 44, 2, "Is spawned?", s_noyes));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      list.add(new Unknown(buffer, offset + 46, 1));
      list.add(new Flag(buffer, offset + 47, 1, "Difficulty", s_diff));
    }
    else
      list.add(new Unknown(buffer, offset + 46, 2));
    list.add(new IdsBitmap(buffer, offset + 48, 4, "Animation", "ANIMATE.IDS"));
    list.add(new Bitmap(buffer, offset + 52, 2, "Orientation", s_orientation));
    list.add(new Unknown(buffer, offset + 54, 2));
    list.add(new DecNumber(buffer, offset + 56, 4, "Expiry time"));
    list.add(new DecNumber(buffer, offset + 60, 2, "Wander distance"));
    list.add(new DecNumber(buffer, offset + 62, 2, "Follow distance"));
    list.add(new Flag(buffer, offset + 64, 4, "Present at", s_schedule));
    list.add(new DecNumber(buffer, offset + 68, 4, "# times talked to"));
    list.add(new ResourceRef(buffer, offset + 72, "Dialogue", "DLG"));
    list.add(new ResourceRef(buffer, offset + 80, "Override script", "BCS"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      list.add(new ResourceRef(buffer, offset + 88, "Special 3 script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 96, "Special 2 script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 104, "Combat script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 112, "Movement script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 120, "Team script", "BCS"));
    }
    else {
      list.add(new ResourceRef(buffer, offset + 88, "General script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 96, "Class script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 104, "Race script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 112, "Default script", "BCS"));
      list.add(new ResourceRef(buffer, offset + 120, "Specifics script", "BCS"));
    }
    if (buffer[offset + 128] == 0x2a)  // *
      list.add(new TextString(buffer, offset + 128, 8, "Character"));
    else
      list.add(new ResourceRef(buffer, offset + 128, "Character", "CRE"));
    HexNumber creOffset = new HexNumber(buffer, offset + 136, 4, "CRE structure offset");
    list.add(creOffset);
    list.add(new DecNumber(buffer, offset + 140, 4, "CRE structure size"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      list.add(new ResourceRef(buffer, offset + 144, "Special 1 script", "BCS"));
      list.add(new Unknown(buffer, offset + 152, 120));
    }
    else {
      list.add(new Unknown(buffer, offset + 144, 128));
    }

    if (creOffset.getValue() != 0)
      list.add(new CreResource(this, "CRE file", buffer, creOffset.getValue()));

    return offset + 272;
  }
}

