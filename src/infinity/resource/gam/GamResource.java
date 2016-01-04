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
  // GAM-specific field labels
  public static final String GAM_GAME_TIME                        = "Game time (game seconds)";
  public static final String GAM_SELECTED_FORMATION               = "Selected formation";
  public static final String GAM_FORMATION_BUTTON_FMT             = "Formation button %d";
  public static final String GAM_PARTY_GOLD                       = "Party gold";
  public static final String GAM_NUM_NPCS_IN_PARTY                = "# NPCs in party";
  public static final String GAM_WEATHER                          = "Weather";
  public static final String GAM_OFFSET_PARTY_MEMBERS             = "Party members offset";
  public static final String GAM_NUM_PARTY_MEMBERS                = "# party members";
  public static final String GAM_OFFSET_UNUSED                    = "Unused offset";
  public static final String GAM_NUM_UNUSED                       = "Unused count";
  public static final String GAM_OFFSET_NON_PARTY_MEMBERS         = "Non-party characters offset";
  public static final String GAM_NUM_NON_PARTY_MEMBERS            = "# non-party characters";
  public static final String GAM_OFFSET_GLOBAL_VARIABLES          = "Global variables offset";
  public static final String GAM_NUM_GLOBAL_VARIABLES             = "# global variables";
  public static final String GAM_MASTER_AREA                      = "Master area";
  public static final String GAM_CURRENT_LINK                     = "Current link";
  public static final String GAM_NUM_JOURNAL_ENTRIES              = "# journal entries";
  public static final String GAM_OFFSET_JOURNAL_ENTRIES           = "Journal entries offset";
  public static final String GAM_REPUTATION                       = "Reputation";
  public static final String GAM_CURRENT_AREA                     = "Current area";
  public static final String GAM_CURRENT_AREA_2                   = "Current area 2";
  public static final String GAM_CONFIGURATION                    = "Configuration";
  public static final String GAM_SAVE_VERSION                     = "Save version";
  public static final String GAM_NUM_UNKNOWN                      = "Unknown section count";
  public static final String GAM_OFFSET_UNKNOWN                   = "Unknown section offset";
  public static final String GAM_OFFSET_MODRON_MAZE               = "Modron maze offset";
  public static final String GAM_OFFSET_KILL_VARIABLES            = "Kill variables offset";
  public static final String GAM_NUM_KILL_VARIABLES               = "# kill variables";
  public static final String GAM_OFFSET_BESTIARY                  = "Bestiary offset";
  public static final String GAM_OFFSET_FAMILIAR_INFO             = "Familiar info offset";
  public static final String GAM_OFFSET_STORED_LOCATIONS          = "Stored locations offset";
  public static final String GAM_NUM_STORED_LOCATIONS             = "# stored locations";
  public static final String GAM_REAL_TIME                        = "Game time (real seconds)";
  public static final String GAM_OFFSET_POCKET_PLANE_LOCATIONS    = "Pocket plane locations offset";
  public static final String GAM_NUM_POCKET_PLANE_LOCATIONS       = "# pocket plane locations";
  public static final String GAM_ZOOM_LEVEL                       = "Zoom level";
  public static final String GAM_RANDOM_ENCOUNTER_AREA            = "Random encounter area";
  public static final String GAM_WORLDMAP                         = "Worldmap";
  public static final String GAM_FAMILIAR_OWNER                   = "Familiar owner";
  public static final String GAM_BESTIARY                         = "Bestiary";
  public static final String GAM_OFFSET_END_OF_UNKNOWN_STRUCTURE  = "End of unknown structure offset";
  public static final String GAM_UNKNOWN_STRUCTURE                = "Unknown structure";
  public static final String GAM_POCKET_PLANE                     = "Pocket plane";

  public static final String[] s_formation = {"Button 1", "Button 2", "Button 3", "Button 4", "Button 5"};
  public static final String[] s_weather = {"No weather", "Raining", "Snowing", "Light weather",
                                            "Medium weather", "Light wind", "Medium wind", "Rare lightning",
                                            "Regular lightning", "Storm increasing"};
  public static final String[] s_torment = {"Follow", "T", "Gather", "4 and 2", "3 by 2",
                                            "Protect", "2 by 3", "Rank", "V", "Wedge", "S",
                                            "Line", "None"};
  public static final String[] s_configuration = {
      "Normal windows", "Party AI disabled", "Larger text window", "Largest text window", "",
      "Fullscreen mode", "Left pane hidden", "Right pane hidden", "Automap notes hidden"};
  public static final String[] s_configuration_bg1 = {
      "Normal windows", "Party AI disabled", "Larger text window", "Largest text window"};
  public static final String[] s_configuration_iwd = {
      "Normal windows", "Party AI disabled", "Larger text window", "Largest text window", "",
      "Fullscreen mode", "Left pane hidden", "Right pane hidden", "Unsupported"};
  public static final String[] s_configuration_iwd2 = {
      "Normal windows", "Party AI disabled", "", "", "", "Fullscreen mode", "",
      "Console hidden", "Automap notes hidden"};
  public static final String[] s_version_bg1 = {"Restrict XP to BG1 limit", "Restrict XP to TotSC limit"};
  public static final String[] s_version = {
      "Restrict XP to BG1 limit", "Restrict XP to TotSC limit", "Restrict XP to SoA limit",
      "Unknown", "SoA active", "ToB active"};
  public static final String[] s_version_iwdee = {
      "Restrict XP to BG1 limit", "Restrict XP to TotSC limit", "Restrict XP to SoA limit",
      "Icewind Dale", "Icewind Dale: HoW only", "ToB active"};
  public static final String[] s_familiar_owner = {
      "Party member 0", "Party member 1", "Party member 2", "Party member 3",
      "Party member 4", "Party member 5"};

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
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    TextString version = new TextString(buffer, offset + 4, 4, COMMON_VERSION);
    addField(version);
    addField(new DecNumber(buffer, offset + 8, 4, GAM_GAME_TIME));
    if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 12, 2, GAM_SELECTED_FORMATION, s_torment));
    } else {
      addField(new Bitmap(buffer, offset + 12, 2, GAM_SELECTED_FORMATION, s_formation));
    }
    for (int i = 0; i < 5; i++) {
      addField(new DecNumber(buffer, offset + 14 + (i * 2), 2, String.format(GAM_FORMATION_BUTTON_FMT, i+1)));
    }
    addField(new DecNumber(buffer, offset + 24, 4, GAM_PARTY_GOLD));
    addField(new DecNumber(buffer, offset + 28, 2, GAM_NUM_NPCS_IN_PARTY));
    addField(new Flag(buffer, offset + 30, 2, GAM_WEATHER, s_weather));
    SectionOffset offset_partynpc = new SectionOffset(buffer, offset + 32, GAM_OFFSET_PARTY_MEMBERS,
                                                      PartyNPC.class);
    addField(offset_partynpc);
    SectionCount count_partynpc = new SectionCount(buffer, offset + 36, 4, GAM_NUM_PARTY_MEMBERS,
                                                   PartyNPC.class);
    addField(count_partynpc);
    SectionOffset offset_unknown = new SectionOffset(buffer, offset + 40, GAM_OFFSET_UNUSED,
                                                     UnknownSection2.class);
    addField(offset_unknown);
    SectionCount count_unknown = new SectionCount(buffer, offset + 44, 4, GAM_NUM_UNUSED,
                                                  UnknownSection2.class);
    addField(count_unknown);
    SectionOffset offset_nonpartynpc = new SectionOffset(buffer, offset + 48, GAM_OFFSET_NON_PARTY_MEMBERS,
                                                         NonPartyNPC.class);
    addField(offset_nonpartynpc);
    SectionCount count_nonpartynpc = new SectionCount(buffer, offset + 52, 4, GAM_NUM_NON_PARTY_MEMBERS,
                                                      NonPartyNPC.class);
    addField(count_nonpartynpc);
    SectionOffset offset_global = new SectionOffset(buffer, offset + 56, GAM_OFFSET_GLOBAL_VARIABLES,
                                                    Variable.class);
    addField(offset_global);
    SectionCount count_global = new SectionCount(buffer, offset + 60, 4, GAM_NUM_GLOBAL_VARIABLES,
                                                 Variable.class);
    addField(count_global);
    addField(new ResourceRef(buffer, offset + 64, GAM_MASTER_AREA, "ARE"));
    addField(new DecNumber(buffer, offset + 72, 4, GAM_CURRENT_LINK));
    SectionCount count_journal = new SectionCount(buffer, offset + 76, 4, GAM_NUM_JOURNAL_ENTRIES,
                                                  JournalEntry.class);
    addField(count_journal);
    SectionOffset offset_journal = new SectionOffset(buffer, offset + 80, GAM_OFFSET_JOURNAL_ENTRIES,
                                                     JournalEntry.class);
    addField(offset_journal);

    SectionOffset offKillvariable = null, offFamiliar = null, offIWD2 = null, offIWD = null;
    SectionOffset offLocation = null, offRubikon = null, offBestiary = null, offPocket = null;
    SectionCount numKillVariable = null, numIWD2 = null, numIWD = null, numLocation = null, numPocket = null;

    if (Profile.getEngine() == Profile.Engine.BG1) { // V1.1
      addField(new DecNumber(buffer, offset + 84, 4, GAM_REPUTATION));
      addField(new ResourceRef(buffer, offset + 88, GAM_CURRENT_AREA, "ARE"));
      addField(new Flag(buffer, offset + 96, 4, GAM_CONFIGURATION, s_configuration_bg1));
      addField(new Bitmap(buffer, offset + 100, 4, GAM_SAVE_VERSION, s_version_bg1));
      addField(new Unknown(buffer, offset + 104, 76));
    }
    else if (Profile.getEngine() == Profile.Engine.IWD) { // V1.1
      addField(new DecNumber(buffer, offset + 84, 4, GAM_REPUTATION));
      addField(new ResourceRef(buffer, offset + 88, GAM_CURRENT_AREA, "ARE"));
      addField(new Flag(buffer, offset + 96, 4, GAM_CONFIGURATION, s_configuration_iwd));
      numIWD = new SectionCount(buffer, offset + 100, 4, GAM_NUM_UNKNOWN, UnknownSection3.class);
      addField(numIWD);
      offIWD = new SectionOffset(buffer, offset + 104, GAM_OFFSET_UNKNOWN, UnknownSection3.class);
      addField(offIWD);
      addField(new Unknown(buffer, offset + 108, 72));
    }
    else if (Profile.getEngine() == Profile.Engine.PST) { // V1.1
      offRubikon = new SectionOffset(buffer, offset + 84, GAM_OFFSET_MODRON_MAZE, Unknown.class);
      addField(offRubikon);
      addField(new DecNumber(buffer, offset + 88, 4, GAM_REPUTATION));
      addField(new ResourceRef(buffer, offset + 92, GAM_CURRENT_AREA, "ARE"));
      offKillvariable = new SectionOffset(buffer, offset + 100, GAM_OFFSET_KILL_VARIABLES, KillVariable.class);
      addField(offKillvariable);
      numKillVariable = new SectionCount(buffer, offset + 104, 4, GAM_NUM_KILL_VARIABLES, KillVariable.class);
      addField(numKillVariable);
      offBestiary = new SectionOffset(buffer, offset + 108, GAM_OFFSET_BESTIARY, Unknown.class);
      addField(offBestiary);
      addField(new ResourceRef(buffer, offset + 112, GAM_CURRENT_AREA_2, "ARE"));
      addField(new Unknown(buffer, offset + 120, 64));
    }
    else if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) { // V2.0
      addField(new DecNumber(buffer, offset + 84, 4, GAM_REPUTATION));
      addField(new ResourceRef(buffer, offset + 88, GAM_CURRENT_AREA, "ARE"));
      addField(new Flag(buffer, offset + 96, 4, GAM_CONFIGURATION, s_configuration));

      if (Profile.getGame() == Profile.Game.IWDEE) {
        addField(new Bitmap(buffer, offset + 100, 4, GAM_SAVE_VERSION, s_version_iwdee));   // to be confirmed
      } else {
        addField(new Bitmap(buffer, offset + 100, 4, GAM_SAVE_VERSION, s_version));
      }

      offFamiliar = new SectionOffset(buffer, offset + 104, GAM_OFFSET_FAMILIAR_INFO, Familiar.class);
      addField(offFamiliar);
      offLocation = new SectionOffset(buffer, offset + 108, GAM_OFFSET_STORED_LOCATIONS, StoredLocation.class);
      addField(offLocation);
      numLocation = new SectionCount(buffer, offset + 112, 4, GAM_NUM_STORED_LOCATIONS, StoredLocation.class);
      addField(numLocation);
      addField(new DecNumber(buffer, offset + 116, 4, GAM_REAL_TIME));
      offPocket = new SectionOffset(buffer, offset + 120, GAM_OFFSET_POCKET_PLANE_LOCATIONS, StoredLocation.class);
      addField(offPocket);
      numPocket = new SectionCount(buffer, offset + 124, 4, GAM_NUM_POCKET_PLANE_LOCATIONS, StoredLocation.class);
      addField(numPocket);
      if (Profile.isEnhancedEdition()) {
        addField(new DecNumber(buffer, offset + 128, 4, GAM_ZOOM_LEVEL));
        addField(new ResourceRef(buffer, offset + 132, GAM_RANDOM_ENCOUNTER_AREA, "ARE"));
        addField(new ResourceRef(buffer, offset + 140, GAM_WORLDMAP, "WMP"));
        if (Profile.getGame() == Profile.Game.IWDEE) {
          addField(new Unknown(buffer, offset + 148, 8));
          addField(new Bitmap(buffer, offset + 156, 4, GAM_FAMILIAR_OWNER, s_familiar_owner));
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
      addField(new ResourceRef(buffer, offset + 88, GAM_CURRENT_AREA, "ARE"));
      addField(new Flag(buffer, offset + 96, 4, GAM_CONFIGURATION, s_configuration_iwd2));
      numIWD2 = new SectionCount(buffer, offset + 100, 4, GAM_NUM_UNKNOWN, UnknownSection3.class);
      addField(numIWD2);
      offIWD2 = new SectionOffset(buffer, offset + 104, GAM_OFFSET_UNKNOWN, UnknownSection3.class);
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
        addField(new Unknown(buffer, offset, 260, GAM_BESTIARY));
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
        HexNumber offEOS = new HexNumber(buffer, offset, 4, GAM_OFFSET_END_OF_UNKNOWN_STRUCTURE);
        addField(offEOS);
        offset += 4;
        int unknownSize = (offEOS.getValue() > buffer.length - 4) ?
                              buffer.length - offset - 4 : offEOS.getValue() - offset;
        addField(new Unknown(buffer, offset, unknownSize, GAM_UNKNOWN_STRUCTURE));
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
        HexNumber offEOS = new HexNumber(buffer, offset, 4, GAM_OFFSET_END_OF_UNKNOWN_STRUCTURE);
        addField(offEOS);
        offset += 4;
        int unknownSize = offEOS.getValue() > buffer.length ? buffer.length - offset : offEOS.getValue() - offset;
        addField(new Unknown(buffer, offset, unknownSize, GAM_UNKNOWN_STRUCTURE));
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
          StoredLocation location = new StoredLocation(this, GAM_POCKET_PLANE, buffer, offset, i);
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

