// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public final class GamResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
  private static final String s_formation[] = {"Button 1", "Button 2", "Button 3", "Button 4", "Button 5"};
  private static final String s_weather[] = {"No weather", "Raining", "Snowing", "Light weather",
                                             "Medium weather", "Light wind", "Medium wind", "Rare lightning",
                                             "Regular lightning", "Storm increasing"};
  private static final String s_torment[] = {"Follow", "T", "Gather", "4 and 2", "3 by 2",
                                             "Protect", "2 by 3", "Rank", "V", "Wedge", "S",
                                             "Line", "None"};

  private HexViewer hexViewer;

  public GamResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    if (Profile.getEngine() == Profile.Engine.PST)
      return new AddRemovable[]{new Variable(), new JournalEntry(), new KillVariable(),
        new NonPartyNPC()};
    else
      return new AddRemovable[]{new Variable(), new JournalEntry(), new NonPartyNPC()};
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
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return StructViewer.TAB_RAW;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
      {
        JScrollPane scroll = new JScrollPane(new Viewer(this));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
      }
      case 1:
      {
        if (hexViewer == null) {
          hexViewer = new HexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
      }
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
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    updateOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    updateOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateOffsets();
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    addField(version);
    addField(new DecNumber(buffer, offset + 8, 4, "Game time (game seconds)"));
    if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 12, 2, "Selected formation", s_torment));
    } else {
      addField(new Bitmap(buffer, offset + 12, 2, "Selected formation", s_formation));
    }
    addField(new DecNumber(buffer, offset + 14, 2, "Formation button 1"));
    addField(new DecNumber(buffer, offset + 16, 2, "Formation button 2"));
    addField(new DecNumber(buffer, offset + 18, 2, "Formation button 3"));
    addField(new DecNumber(buffer, offset + 20, 2, "Formation button 4"));
    addField(new DecNumber(buffer, offset + 22, 2, "Formation button 5"));
    addField(new DecNumber(buffer, offset + 24, 4, "Party gold"));
    addField(new DecNumber(buffer, offset + 28, 2, "# NPCs in party"));
    addField(new Flag(buffer, offset + 30, 2, "Weather", s_weather));
    SectionOffset offset_partynpc = new SectionOffset(buffer, offset + 32, "Party members offset",
                                                      PartyNPC.class);
    addField(offset_partynpc);
    SectionCount count_partynpc = new SectionCount(buffer, offset + 36, 4, "# party members",
                                                   PartyNPC.class);
    addField(count_partynpc);
    SectionOffset offset_unknown = new SectionOffset(buffer, offset + 40, "Party inventory offset",
                                                     UnknownSection2.class);
    addField(offset_unknown);
    SectionCount count_unknown = new SectionCount(buffer, offset + 44, 4, "Party inventory count",
                                                  UnknownSection2.class);
    addField(count_unknown);
    SectionOffset offset_nonpartynpc = new SectionOffset(buffer, offset + 48, "Non-party characters offset",
                                                         NonPartyNPC.class);
    addField(offset_nonpartynpc);
    SectionCount count_nonpartynpc = new SectionCount(buffer, offset + 52, 4, "# non-party characters",
                                                      NonPartyNPC.class);
    addField(count_nonpartynpc);
    SectionOffset offset_global = new SectionOffset(buffer, offset + 56, "Global variables offset",
                                                    Variable.class);
    addField(offset_global);
    SectionCount count_global = new SectionCount(buffer, offset + 60, 4, "# global variables",
                                                 Variable.class);
    addField(count_global);
    addField(new ResourceRef(buffer, offset + 64, "Current area", "ARE"));
    addField(new DecNumber(buffer, offset + 72, 4, "Current link"));
    SectionCount count_journal = new SectionCount(buffer, offset + 76, 4, "# journal entries",
                                                  JournalEntry.class);
    addField(count_journal);
    SectionOffset offset_journal = new SectionOffset(buffer, offset + 80, "Journal entries offset",
                                                     JournalEntry.class);
    addField(offset_journal);

    SectionOffset offKillvariable = null, offFamiliar = null, offIWD2 = null, offIWD = null;
    SectionOffset offLocation = null, offRubikon = null, offBestiary = null, offPocket = null;
    SectionCount numKillVariable = null, numIWD2 = null, numIWD = null, numLocation = null, numPocket = null;

    if (Profile.getEngine() == Profile.Engine.BG1) { // V1.1
      addField(new DecNumber(buffer, offset + 84, 4, "Reputation"));
      addField(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      addField(new Flag(buffer, offset + 96, 4, "Configuration",
               new String[]{"Normal windows", "Party AI disabled", "Larger text window",
                            "Largest text window"}));
      addField(new DecNumber(buffer, offset + 100, 4, "Save version"));
      addField(new Unknown(buffer, offset + 104, 76));
    }
    else if (Profile.getEngine() == Profile.Engine.IWD) { // V1.1
      addField(new DecNumber(buffer, offset + 84, 4, "Reputation"));
      addField(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      addField(new Flag(buffer, offset + 96, 4, "Configuration",
               new String[]{"Normal windows", "Party AI disabled", "Larger text window",
                            "Largest text window", "", "Fullscreen mode", "Left pane hidden",
                            "Right pane hidden", "Unsupported"}));
      numIWD = new SectionCount(buffer, offset + 100, 4, "Unknown section count", UnknownSection3.class);
      addField(numIWD);
      offIWD = new SectionOffset(buffer, offset + 104, "Unknown section offset", UnknownSection3.class);
      addField(offIWD);
      addField(new Unknown(buffer, offset + 108, 72));
    }
    else if (Profile.getEngine() == Profile.Engine.PST) { // V1.1
      offRubikon = new SectionOffset(buffer, offset + 84, "Modron maze offset", Unknown.class);
      addField(offRubikon);
      addField(new DecNumber(buffer, offset + 88, 4, "Reputation"));
      addField(new ResourceRef(buffer, offset + 92, "Master area", "ARE"));
      offKillvariable = new SectionOffset(buffer, offset + 100, "Kill variables offset", KillVariable.class);
      addField(offKillvariable);
      numKillVariable = new SectionCount(buffer, offset + 104, 4, "# kill variables", KillVariable.class);
      addField(numKillVariable);
      offBestiary = new SectionOffset(buffer, offset + 108, "Bestiary offset", Unknown.class);
      addField(offBestiary);
      addField(new ResourceRef(buffer, offset + 112, "Current area?", "ARE"));
      addField(new Unknown(buffer, offset + 120, 64));
    }
    else if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) { // V2.0
      addField(new DecNumber(buffer, offset + 84, 4, "Reputation"));
      addField(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      addField(new Flag(buffer, offset + 96, 4, "Configuration",
               new String[]{"Normal windows", "Party AI disabled", "Larger text window",
                            "Largest text window", "", "Fullscreen mode", "Left pane hidden",
                            "Right pane hidden", "Automap notes hidden"}));
      addField(new DecNumber(buffer, offset + 100, 4, "Save version"));
      offFamiliar = new SectionOffset(buffer, offset + 104, "Familiar info offset", Familiar.class);
      addField(offFamiliar);
      offLocation = new SectionOffset(buffer, offset + 108, "Stored locations offset", StoredLocation.class);
      addField(offLocation);
      numLocation = new SectionCount(buffer, offset + 112, 4, "# stored locations", StoredLocation.class);
      addField(numLocation);
      addField(new DecNumber(buffer, offset + 116, 4, "Game time (real seconds)"));
      offPocket = new SectionOffset(buffer, offset + 120, "Pocket plane locations offset", StoredLocation.class);
      addField(offPocket);
      numPocket = new SectionCount(buffer, offset + 124, 4, "# pocket plane locations", StoredLocation.class);
      addField(numPocket);
      if (Profile.isEnhancedEdition()) {
        addField(new DecNumber(buffer, offset + 128, 4, "Zoom level"));
        addField(new ResourceRef(buffer, offset + 132, "Random encounter area", "ARE"));
        addField(new ResourceRef(buffer, offset + 140, "Worldmap", "WMP"));
        if (Profile.getGame() == Profile.Game.IWDEE) {
          addField(new Unknown(buffer, offset + 148, 8));
          addField(new Bitmap(buffer, offset + 156, 4, "Familiar owner",
                              new String[]{"Party member 0", "Party member 1", "Party member 2",
                                           "Party member 3", "Party member 4", "Party member 5"}));
          addField(new Unknown(buffer, offset + 160, 20));
        } else {
          addField(new Unknown(buffer, offset + 148, 32));
        }
      } else {
        addField(new Unknown(buffer, offset + 128, 52));
      }
    }
    else if (Profile.getEngine() == Profile.Engine.IWD2) { // V2.2 (V1.1 & V2.0 in BIFF)
      addField(new Unknown(buffer, offset + 84, 4));
      addField(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      addField(new Flag(buffer, offset + 96, 4, "Configuration",
                        new String[]{"Normal windows", "Party AI disabled", "",
                                     "", "", "Fullscreen mode", "",
                                     "Console hidden", "Automap notes hidden"}));
      numIWD2 = new SectionCount(buffer, offset + 100, 4, "Unknown section count", UnknownSection3.class);
      addField(numIWD2);
      offIWD2 = new SectionOffset(buffer, offset + 104, "Unknown section offset", UnknownSection3.class);
      addField(offIWD2);
      addField(new Unknown(buffer, offset + 108, 72));
    }

    offset = offset_partynpc.getValue();
    for (int i = 0; i < count_partynpc.getValue(); i++) {
      PartyNPC npc = new PartyNPC(this, buffer, offset, i);
      offset += npc.getSize();
      addField(npc);
    }

    offset = offset_nonpartynpc.getValue();
    for (int i = 0; i < count_nonpartynpc.getValue(); i++) {
      NonPartyNPC npc = new NonPartyNPC(this, buffer, offset, i);
      offset += npc.getSize();
      addField(npc);
    }

    offset = offset_unknown.getValue();
    if (offset > 0) {
      for (int i = 0; i < count_unknown.getValue(); i++) {
        addField(new UnknownSection2(this, buffer, offset + i * 20));
      }
    }

    if (offRubikon != null) { // Torment
      offset = offRubikon.getValue();
      if (offset > 0) {
        addField(new ModronMaze(this, buffer, offset));
        offset += 1720;
      }
    }

    offset = offset_global.getValue();
    for (int i = 0; i < count_global.getValue(); i++) {
      Variable var = new Variable(this, buffer, offset, i);
      offset += var.getSize();
      addField(var);
    }

    if (offKillvariable != null) { // Torment
      offset = offKillvariable.getValue();
      for (int i = 0; i < numKillVariable.getValue(); i++) {
        KillVariable kvar = new KillVariable(this, buffer, offset, i);
        offset += kvar.getSize();
        addField(kvar);
      }
    }

    offset = offset_journal.getValue();
    for (int i = 0; i < count_journal.getValue(); i++) {
      JournalEntry ent = new JournalEntry(this, buffer, offset, i);
      offset += ent.getSize();
      addField(ent);
    }

    if (offBestiary != null) { // Torment
      offset = offBestiary.getValue();
      if (offset > 0) {
        addField(new Unknown(buffer, offset, 260, "Bestiary"));
        offset += 260;
      }
    }

    if (offFamiliar != null) { // BG2
      offset = offFamiliar.getValue();
      if (offset > 0) {
        Familiar familiar = new Familiar(this, buffer, offset);
        offset += familiar.getSize();
        addField(familiar);
      }
    }

    if (offIWD2 != null && numIWD2 != null) { // Icewind2
      // a leftover from BG2 Familiar Info structure?
      if (numIWD2.getValue() > 0) {
        offset = offIWD2.getValue();
        for (int i = 0; i < numIWD2.getValue(); i++) {
          UnknownSection3 unknown = new UnknownSection3(this, buffer, offset);
          offset += unknown.getSize();
          addField(unknown);
        }
        HexNumber offEOS = new HexNumber(buffer, offset, 4, "End of unknown structure offset");
        addField(offEOS);
        offset += 4;
        int unknownSize = (offEOS.getValue() > buffer.length - 4) ?
                              buffer.length - offset - 4 : offEOS.getValue() - offset;
        addField(new Unknown(buffer, offset, unknownSize, "Unknown structure"));
        offset += unknownSize;
        addField(new Unknown(buffer, offset, 4));
        offset += 4;
      }
    }

    if (numIWD != null && offIWD != null) { // Icewind
      // a leftover from BG2 Familiar Info structure?
      if (numIWD.getValue() > 0) {
        offset = offIWD.getValue();
        for (int i = 0; i < numIWD.getValue(); i++) {
          UnknownSection3 unknown = new UnknownSection3(this, buffer, offset);
          offset += unknown.getSize();
          addField(unknown);
        }
        HexNumber offEOS = new HexNumber(buffer, offset, 4, "End of unknown structure offset");
        addField(offEOS);
        offset += 4;
        int unknownSize = offEOS.getValue() > buffer.length ? buffer.length - offset : offEOS.getValue() - offset;
        addField(new Unknown(buffer, offset, unknownSize, "Unknown structure"));
        offset += unknownSize;
      }
    }

    if (offLocation != null && numLocation != null) { // BG2?
      offset = offLocation.getValue();
      if (offset > 0) {
        for (int i = 0; i < numLocation.getValue(); i++) {
          StoredLocation location = new StoredLocation(this, buffer, offset, i);
          offset += location.getSize();
          addField(location);
        }
      }
    }

    if (offPocket != null && numPocket != null) {  // BG2
      offset = offPocket.getValue();
      if (offset > 0) {
        for (int i = 0; i < numPocket.getValue(); i++) {
          StoredLocation location = new StoredLocation(this, "Pocket plane", buffer, offset, i);
          offset += location.getSize();
          addField(location);
        }
      }
    }

    if (offset == 0) {
      offset = getField(getFieldCount() - 1).getOffset() + getField(getFieldCount() - 1).getSize();
    }

    return offset;
  }

  private void updateOffsets()
  {
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof PartyNPC) {
        ((PartyNPC)o).updateCREOffset();
      }
//      if (o instanceof Familiar) {
//        ((Familiar)o).updateFilesize((DecNumber)getAttribute("File size"));
//      }
    }
  }
}

