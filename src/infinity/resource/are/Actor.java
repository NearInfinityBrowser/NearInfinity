// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Profile;
import infinity.resource.StructEntry;
import infinity.resource.cre.CreResource;

import javax.swing.JComponent;

public final class Actor extends AbstractStruct implements AddRemovable, HasViewerTabs, HasAddRemovable
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

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables()
  {
    return new AddRemovable[]{};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    return new ViewerActor(this);
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (child instanceof CreResource)
      ((DecNumber)getAttribute("CRE structure size")).setValue(child.getSize());
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (datatype instanceof CreResource) {
      ((DecNumber)getAttribute("CRE structure size")).setValue(0);
      ((HexNumber)getAttribute("CRE structure offset")).setValue(0);
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (child instanceof CreResource)
      ((DecNumber)getAttribute("CRE structure size")).setValue(child.getSize());
  }

  void updateCREOffset()
  {
    StructEntry entry = getField(getFieldCount() - 1);
    if (entry instanceof CreResource)
      ((HexNumber)getAttribute("CRE structure offset")).setValue(entry.getOffset());
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new DecNumber(buffer, offset + 32, 2, "Position: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Position: Y"));
    addField(new DecNumber(buffer, offset + 36, 2, "Destination: X"));
    addField(new DecNumber(buffer, offset + 38, 2, "Destination: Y"));
//    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
//        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB)
    addField(new Flag(buffer, offset + 40, 4, "Loading", s_yesno));
//    else
//      addField(new Bitmap(buffer, offset + 40, 4, "Is visible?", s_noyes));
    addField(new Bitmap(buffer, offset + 44, 2, "Is spawned?", s_noyes));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Unknown(buffer, offset + 46, 1));
      addField(new Flag(buffer, offset + 47, 1, "Difficulty", s_diff));
    }
    else {
      addField(new Unknown(buffer, offset + 46, 2));
    }
    addField(new IdsBitmap(buffer, offset + 48, 4, "Animation", "ANIMATE.IDS"));
    addField(new Bitmap(buffer, offset + 52, 2, "Orientation", s_orientation));
    addField(new Unknown(buffer, offset + 54, 2));
    addField(new DecNumber(buffer, offset + 56, 4, "Expiry time"));
    addField(new DecNumber(buffer, offset + 60, 2, "Wander distance"));
    addField(new DecNumber(buffer, offset + 62, 2, "Follow distance"));
    addField(new Flag(buffer, offset + 64, 4, "Present at", s_schedule));
    addField(new DecNumber(buffer, offset + 68, 4, "# times talked to"));
    addField(new ResourceRef(buffer, offset + 72, "Dialogue", "DLG"));
    addField(new ResourceRef(buffer, offset + 80, "Override script", "BCS"));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new ResourceRef(buffer, offset + 88, "Special 3 script", "BCS"));
      addField(new ResourceRef(buffer, offset + 96, "Special 2 script", "BCS"));
      addField(new ResourceRef(buffer, offset + 104, "Combat script", "BCS"));
      addField(new ResourceRef(buffer, offset + 112, "Movement script", "BCS"));
      addField(new ResourceRef(buffer, offset + 120, "Team script", "BCS"));
    }
    else {
      addField(new ResourceRef(buffer, offset + 88, "General script", "BCS"));
      addField(new ResourceRef(buffer, offset + 96, "Class script", "BCS"));
      addField(new ResourceRef(buffer, offset + 104, "Race script", "BCS"));
      addField(new ResourceRef(buffer, offset + 112, "Default script", "BCS"));
      addField(new ResourceRef(buffer, offset + 120, "Specifics script", "BCS"));
    }
    if (buffer[offset + 128] == 0x2a) { // *
      addField(new TextString(buffer, offset + 128, 8, "Character"));
    }
    else {
      addField(new ResourceRef(buffer, offset + 128, "Character", "CRE"));
    }
    HexNumber creOffset = new HexNumber(buffer, offset + 136, 4, "CRE structure offset");
    addField(creOffset);
    addField(new DecNumber(buffer, offset + 140, 4, "CRE structure size"));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new ResourceRef(buffer, offset + 144, "Special 1 script", "BCS"));
      addField(new Unknown(buffer, offset + 152, 120));
    }
    else {
      addField(new Unknown(buffer, offset + 144, 128));
    }

    if (creOffset.getValue() != 0) {
      addField(new CreResource(this, "CRE file", buffer, creOffset.getValue()));
    }

    return offset + 272;
  }
}

