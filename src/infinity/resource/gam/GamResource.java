// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public final class GamResource extends AbstractStruct implements Resource, HasAddRemovable, HasDetailViewer
{
  private static final String s_formation[] = {"Button 1", "Button 2", "Button 3", "Button 4", "Button 5"};
  private static final String s_weather[] = {"No weather", "Raining", "Snowing", "Light weather",
                                             "Medium weather", "Light wind", "Medium wind", "Rare lightning",
                                             "Regular lightning", "Storm increasing"};
  private static final String s_torment[] = {"Follow", "T", "Gather", "4 and 2", "3 by 2",
                                             "Protect", "2 by 3", "Rank", "V", "Wedge", "S",
                                             "Line", "None"};

  public GamResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      return new AddRemovable[]{new Variable(), new JournalEntry(), new KillVariable(),
                                new NonPartyNPC()};
    else
      return new AddRemovable[]{new Variable(), new JournalEntry(), new NonPartyNPC()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    JScrollPane scroll = new JScrollPane(new Viewer(this));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

// --------------------- End Interface HasDetailViewer ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  protected void datatypeAdded(AddRemovable datatype)
  {
    updateOffsets();
  }

  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateOffsets();
  }

  protected void datatypeRemoved(AddRemovable datatype)
  {
    updateOffsets();
  }

  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateOffsets();
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    list.add(version);
    list.add(new DecNumber(buffer, offset + 8, 4, "Game time (game seconds)"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      list.add(new Bitmap(buffer, offset + 12, 2, "Selected formation", s_torment));
    else
      list.add(new Bitmap(buffer, offset + 12, 2, "Selected formation", s_formation));
    list.add(new DecNumber(buffer, offset + 14, 2, "Formation button 1"));
    list.add(new DecNumber(buffer, offset + 16, 2, "Formation button 2"));
    list.add(new DecNumber(buffer, offset + 18, 2, "Formation button 3"));
    list.add(new DecNumber(buffer, offset + 20, 2, "Formation button 4"));
    list.add(new DecNumber(buffer, offset + 22, 2, "Formation button 5"));
    list.add(new DecNumber(buffer, offset + 24, 4, "Party gold"));
    list.add(new DecNumber(buffer, offset + 28, 2, "# NPCs in party"));
    list.add(new Flag(buffer, offset + 30, 2, "Weather", s_weather));
    SectionOffset offset_partynpc = new SectionOffset(buffer, offset + 32, "Party members offset",
                                                      PartyNPC.class);
    list.add(offset_partynpc);
    SectionCount count_partynpc = new SectionCount(buffer, offset + 36, 4, "# party members",
                                                   PartyNPC.class);
    list.add(count_partynpc);
    SectionOffset offset_unknown = new SectionOffset(buffer, offset + 40, "Party inventory offset",
                                                     UnknownSection2.class);
    list.add(offset_unknown);
    SectionCount count_unknown = new SectionCount(buffer, offset + 44, 4, "Party inventory count",
                                                  UnknownSection2.class);
    list.add(count_unknown);
    SectionOffset offset_nonpartynpc = new SectionOffset(buffer, offset + 48, "Non-party characters offset",
                                                         NonPartyNPC.class);
    list.add(offset_nonpartynpc);
    SectionCount count_nonpartynpc = new SectionCount(buffer, offset + 52, 4, "# non-party characters",
                                                      NonPartyNPC.class);
    list.add(count_nonpartynpc);
    SectionOffset offset_global = new SectionOffset(buffer, offset + 56, "Global variables offset",
                                                    Variable.class);
    list.add(offset_global);
    SectionCount count_global = new SectionCount(buffer, offset + 60, 4, "# global variables",
                                                 Variable.class);
    list.add(count_global);
    list.add(new ResourceRef(buffer, offset + 64, "Current area", "ARE"));
    list.add(new DecNumber(buffer, offset + 72, 4, "Current link"));
    SectionCount count_journal = new SectionCount(buffer, offset + 76, 4, "# journal entries",
                                                  JournalEntry.class);
    list.add(count_journal);
    SectionOffset offset_journal = new SectionOffset(buffer, offset + 80, "Journal entries offset",
                                                     JournalEntry.class);
    list.add(offset_journal);

    SectionOffset offKillvariable = null, offFamiliar = null, offIWD2 = null, offIWD = null;
    SectionOffset offLocation = null, offRubikon = null, offBestiary = null;
    SectionCount numKillVariable = null, numIWD = null, numLocation = null;

    int gameid = ResourceFactory.getGameID();
    if (gameid == ResourceFactory.ID_BG1 || gameid == ResourceFactory.ID_BG1TOTSC) { // V1.1
      list.add(new DecNumber(buffer, offset + 84, 4, "Reputation"));
      list.add(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      list.add(new Flag(buffer, offset + 96, 4, "Configuration",
                        new String[]{"Normal windows", "Party AI disabled", "Larger text window",
                                     "Largest text window"}));
      list.add(new DecNumber(buffer, offset + 100, 4, "Save version"));
      list.add(new Unknown(buffer, offset + 104, 76));
    }
    else if (gameid == ResourceFactory.ID_ICEWIND || gameid == ResourceFactory.ID_ICEWINDHOW ||
             gameid == ResourceFactory.ID_ICEWINDHOWTOT) { // V1.1
      list.add(new DecNumber(buffer, offset + 84, 4, "Reputation"));
      list.add(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      list.add(new Flag(buffer, offset + 96, 4, "Configuration",
                        new String[]{"Normal windows", "", "Larger text window",
                                     "Largest text window"}));
      numIWD = new SectionCount(buffer, offset + 100, 4, "Unknown section count", UnknownSection3.class);
      list.add(numIWD);
      offIWD = new SectionOffset(buffer, offset + 104, "Unknown section offset", UnknownSection3.class);
      list.add(offIWD);
      list.add(new Unknown(buffer, offset + 108, 72));
    }
    else if (gameid == ResourceFactory.ID_TORMENT) { // V1.1
      offRubikon = new SectionOffset(buffer, offset + 84, "Modron maze offset", Unknown.class);
      list.add(offRubikon);
      list.add(new DecNumber(buffer, offset + 88, 4, "Reputation"));
      list.add(new ResourceRef(buffer, offset + 92, "Master area", "ARE"));
      offKillvariable = new SectionOffset(buffer, offset + 100, "Kill variables offset", KillVariable.class);
      list.add(offKillvariable);
      numKillVariable = new SectionCount(buffer, offset + 104, 4, "# kill variables", KillVariable.class);
      list.add(numKillVariable);
      offBestiary = new SectionOffset(buffer, offset + 108, "Bestiary offset", Unknown.class);
      list.add(offBestiary);
      list.add(new ResourceRef(buffer, offset + 112, "Current area?", "ARE"));
      list.add(new Unknown(buffer, offset + 120, 64));
    }
    else if (gameid == ResourceFactory.ID_BG2 || gameid == ResourceFactory.ID_BG2TOB ||
             gameid == ResourceFactory.ID_TUTU) { // V2.0
      list.add(new DecNumber(buffer, offset + 84, 4, "Reputation"));
      list.add(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      list.add(new Flag(buffer, offset + 96, 4, "Configuration",
                        new String[]{"Normal windows", "Party AI disabled", "Larger text window",
                                     "Largest text window", "", "Fullscreen mode", "Left pane hidden",
                                     "Right pane hidden", "Automap notes hidden"}));
      list.add(new DecNumber(buffer, offset + 100, 4, "Save version"));
      offFamiliar = new SectionOffset(buffer, offset + 104, "Familiar info offset", Familiar.class);
      list.add(offFamiliar);
//      list.add(new DecNumber(buffer, offset + 108, 4, "File size"));
      offLocation = new SectionOffset(buffer, offset + 108, "Stored locations offset", StoredLocation.class);
      list.add(offLocation);
      numLocation = new SectionCount(buffer, offset + 112, 4, "# stored locations", StoredLocation.class);
      list.add(numLocation);
      list.add(new DecNumber(buffer, offset + 116, 4, "Game time (real seconds)"));
//      list.add(new DecNumber(buffer, offset + 120, 4, "File size"));
      list.add(new SectionOffset(buffer, offset + 120, "Pocket plane locations offset", StoredLocation.class));
      list.add(new SectionCount(buffer, offset + 124, 4, "# pocket plane locations", StoredLocation.class));
      list.add(new Unknown(buffer, offset + 128, 52));
    }
    else if (gameid == ResourceFactory.ID_ICEWIND2) { // V2.2 (V1.1 & V2.0 in BIFF)
      list.add(new Unknown(buffer, offset + 84, 4));
      list.add(new ResourceRef(buffer, offset + 88, "Master area", "ARE"));
      list.add(new Flag(buffer, offset + 96, 4, "Configuration",
                        new String[]{"Normal windows", "Party AI disabled", "",
                                     "", "", "Fullscreen mode", "Button bar hidden",
                                     "Console hidden", "Automap notes hidden"}));
      list.add(new Unknown(buffer, offset + 100, 4));
      if (version.toString().equalsIgnoreCase("V2.2")) {
        offIWD2 = new SectionOffset(buffer, offset + 104, "Unknown offset",
                                            UnknownSection.class);
        list.add(offIWD2);
      }
      else
        list.add(new Unknown(buffer, offset + 104, 4));
      list.add(new Unknown(buffer, offset + 108, 72));
    }

    offset = offset_partynpc.getValue();
    for (int i = 0; i < count_partynpc.getValue(); i++) {
      PartyNPC npc = new PartyNPC(this, buffer, offset, i);
      offset += npc.getSize();
      list.add(npc);
    }

    offset = offset_nonpartynpc.getValue();
    for (int i = 0; i < count_nonpartynpc.getValue(); i++) {
      NonPartyNPC npc = new NonPartyNPC(this, buffer, offset, i);
      offset += npc.getSize();
      list.add(npc);
    }

    offset = offset_unknown.getValue();
    if (offset > 0) {
      for (int i = 0; i < count_unknown.getValue(); i++)
        list.add(new UnknownSection2(this, buffer, offset + i * 20));
    }

    if (offRubikon != null) { // Torment
      offset = offRubikon.getValue();
      if (offset > 0) {
        list.add(new Unknown(buffer, offset, 1720, "Modron maze state"));
        offset += 1720;
      }
    }

    offset = offset_global.getValue();
    for (int i = 0; i < count_global.getValue(); i++) {
      Variable var = new Variable(this, buffer, offset);
      offset += var.getSize();
      list.add(var);
    }

    if (offKillvariable != null) { // Torment
      offset = offKillvariable.getValue();
      for (int i = 0; i < numKillVariable.getValue(); i++) {
        KillVariable kvar = new KillVariable(this, buffer, offset);
        offset += kvar.getSize();
        list.add(kvar);
      }
    }

    offset = offset_journal.getValue();
    for (int i = 0; i < count_journal.getValue(); i++) {
      JournalEntry ent = new JournalEntry(this, buffer, offset);
      offset += ent.getSize();
      list.add(ent);
    }

    if (offBestiary != null) { // Torment
      offset = offBestiary.getValue();
      if (offset > 0) {
        list.add(new Unknown(buffer, offset, 260, "Bestiary"));
        offset += 260;
      }
    }

    if (offFamiliar != null) { // BG2
      offset = offFamiliar.getValue();
      Familiar familiar = new Familiar(this, buffer, offset);
      offset += familiar.getSize();
      list.add(familiar);
    }

    if (offIWD2 != null) { // Icewind2
      offset = offIWD2.getValue();
      if (offset > 0) {
        UnknownSection unknown = new UnknownSection(this, buffer, offset);
        offset += unknown.getSize();
        list.add(unknown);
      }
    }

    if (numIWD != null && offIWD != null) { // Icewind
      offset = offIWD.getValue();
      if (offset > 0) {
        for (int i = 0; i < numIWD.getValue(); i++) {
          UnknownSection3 unknown = new UnknownSection3(this, buffer, offset);
          offset += unknown.getSize();
          list.add(unknown);
        }
        list.add(new DecNumber(buffer, offset, 4, "File size"));
        list.add(new Unknown(buffer, offset + 4, 324));
        offset += 328;
      }
    }

    if (offLocation != null && numLocation != null) { // BG2?
      offset = offLocation.getValue();
      if (offset > 0) {
        for (int i = 0; i < numLocation.getValue(); i++) {
          StoredLocation location = new StoredLocation(this, buffer, offset);
          offset += location.getSize();
          list.add(location);
        }
      }
    }

    if (offset == 0)
      offset = getStructEntryAt(list.size() - 1).getOffset() + getStructEntryAt(list.size() - 1).getSize();

    return offset;
  }

  private void updateOffsets()
  {
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof PartyNPC)
        ((PartyNPC)o).updateCREOffset();
//      if (o instanceof Familiar)
//        ((Familiar)o).updateFilesize((DecNumber)getAttribute("File size"));
    }
  }
}

