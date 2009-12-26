// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.are.Actor;
import infinity.resource.cre.CreResource;
import infinity.util.LongIntegerHashMap;

import javax.swing.*;

class PartyNPC extends AbstractStruct implements HasDetailViewer, HasAddRemovable, AddRemovable
{
  private static final LongIntegerHashMap<String> partyOrder = new LongIntegerHashMap<String>();
  private static final LongIntegerHashMap<String> m_selected = new LongIntegerHashMap<String>();
  private static final LongIntegerHashMap<String> m_partyslot = new LongIntegerHashMap<String>();
  private static final String s_noyes[] = {"No", "Yes"};

  static {
    partyOrder.put(0L, "Slot 1");
    partyOrder.put(1L, "Slot 2");
    partyOrder.put(2L, "Slot 3");
    partyOrder.put(3L, "Slot 4");
    partyOrder.put(4L, "Slot 5");
    partyOrder.put(5L, "Slot 6");
//    partyOrder.put(0x8000L, "In party, dead");
    partyOrder.put(0xffff, "Not in party");

    m_selected.put(0L, "Not selected");
    m_selected.put(1L, "Selected");
    m_selected.put(32768L, "Dead");

    m_partyslot.put(0L, "Yes");
    m_partyslot.put(65535L, "No");
  }

  PartyNPC() throws Exception
  {
    super(null, "Party character",
          ResourceFactory.getGameID() == ResourceFactory.ID_BG1 ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG1TOTSC ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
          ResourceFactory.getGameID() == ResourceFactory.ID_TUTU ? new byte[352] :
          ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ? new byte[360] :
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2 ? new byte[832] : new byte[384],
          0);
  }

  PartyNPC(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Party member " + nr, buffer, offset);
  }

  PartyNPC(AbstractStruct superStruct, String name, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    return new ViewerNPC(this);
  }

// --------------------- End Interface HasDetailViewer ---------------------

  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    ((DecNumber)getAttribute("CRE structure size")).setValue(getStructEntryAt(getRowCount() - 1).getSize());
    super.datatypeAddedInChild(child, datatype);
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
    ((DecNumber)getAttribute("CRE structure size")).setValue(getStructEntryAt(getRowCount() - 1).getSize());
    super.datatypeRemovedInChild(child, datatype);
  }

  void updateCREOffset()
  {
    StructEntry entry = getStructEntryAt(getRowCount() - 1);
    if (entry instanceof CreResource)
      ((HexNumber)getAttribute("CRE structure offset")).setValue(entry.getOffset());
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new HashBitmap(buffer, offset, 2, "Selection state", m_selected));
    list.add(new HashBitmap(buffer, offset + 2, 2, "Party position", partyOrder));
    HexNumber creOffset = new HexNumber(buffer, offset + 4, 4, "CRE structure offset");
    list.add(creOffset);
    list.add(new DecNumber(buffer, offset + 8, 4, "CRE structure size"));
    if (buffer[offset + 12] == 0x2A)
      list.add(new TextString(buffer, offset + 12, 8, "Character"));
    else
      list.add(new ResourceRef(buffer, offset + 12, "Character", "CRE"));
    list.add(new Bitmap(buffer, offset + 20, 4, "Orientation", Actor.s_orientation));
    list.add(new ResourceRef(buffer, offset + 24, "Current area", "ARE"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    list.add(new DecNumber(buffer, offset + 36, 2, "Viewport location: X"));
    list.add(new DecNumber(buffer, offset + 38, 2, "Viewport location: Y"));

    int gameid = ResourceFactory.getGameID();
    if (gameid == ResourceFactory.ID_BG1 || gameid == ResourceFactory.ID_BG1TOTSC) {
      list.add(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      list.add(new DecNumber(buffer, offset + 42, 2, "Happiness"));
      list.add(new Unknown(buffer, offset + 44, 96));
      list.add(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 142, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 146, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 148, 2, "Show quick weapon 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 150, 2, "Show quick weapon 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 152, 2, "Show quick weapon 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 154, 2, "Show quick weapon 4?", m_partyslot));
      list.add(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      list.add(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      list.add(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      list.add(new IdsBitmap(buffer, offset + 180, 2, "Quick item slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 182, 2, "Quick item slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 184, 2, "Quick item slot 3", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 186, 2, "Show quick item 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 188, 2, "Show quick item 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 190, 2, "Show quick item 3?", m_partyslot));
      list.add(new TextString(buffer, offset + 192, 32, "Name"));
      list.add(new DecNumber(buffer, offset + 224, 4, "# times talked to"));
      offset = readCharStats(buffer, offset + 228);
      list.add(new TextString(buffer, offset, 8, "Voice set"));
      offset += 8;
    }
    else if (gameid == ResourceFactory.ID_BG2 || gameid == ResourceFactory.ID_BG2TOB ||
             gameid == ResourceFactory.ID_TUTU) {
      list.add(new IdsBitmap(buffer, offset + 40, 2, "Modal state", "MODAL.IDS"));
      list.add(new DecNumber(buffer, offset + 42, 2, "Happiness"));
      list.add(new Unknown(buffer, offset + 44, 96));
      list.add(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 142, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 146, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 148, 2, "Show quick weapon 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 150, 2, "Show quick weapon 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 152, 2, "Show quick weapon 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 154, 2, "Show quick weapon 4?", m_partyslot));
      list.add(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      list.add(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      list.add(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      list.add(new IdsBitmap(buffer, offset + 180, 2, "Quick item slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 182, 2, "Quick item slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 184, 2, "Quick item slot 3", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 186, 2, "Show quick item 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 188, 2, "Show quick item 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 190, 2, "Show quick item 3?", m_partyslot));
      list.add(new TextString(buffer, offset + 192, 32, "Name"));
      list.add(new DecNumber(buffer, offset + 224, 4, "# times talked to"));
      offset = readCharStats(buffer, offset + 228);
      list.add(new TextString(buffer, offset, 8, "Voice set"));
      offset += 8;
    }
    else if (gameid == ResourceFactory.ID_TORMENT) {
      list.add(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      list.add(new DecNumber(buffer, offset + 42, 2, "Happiness"));
      list.add(new Unknown(buffer, offset + 44, 96));
      list.add(new DecNumber(buffer, offset + 140, 2, "Quick weapon slot 1"));
      list.add(new DecNumber(buffer, offset + 142, 2, "Quick weapon slot 2"));
      list.add(new DecNumber(buffer, offset + 144, 2, "Quick weapon slot 3"));
      list.add(new DecNumber(buffer, offset + 146, 2, "Quick weapon slot 4"));
      list.add(new HashBitmap(buffer, offset + 148, 2, "Show quick weapon 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 150, 2, "Show quick weapon 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 152, 2, "Show quick weapon 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 154, 2, "Show quick weapon 4?", m_partyslot));
      list.add(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      list.add(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      list.add(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      list.add(new DecNumber(buffer, offset + 180, 2, "Quick item slot 1"));
      list.add(new DecNumber(buffer, offset + 182, 2, "Quick item slot 2"));
      list.add(new DecNumber(buffer, offset + 184, 2, "Quick item slot 3"));
      list.add(new DecNumber(buffer, offset + 186, 2, "Quick item slot 4"));
      list.add(new DecNumber(buffer, offset + 188, 2, "Quick item slot 5"));
      list.add(new HashBitmap(buffer, offset + 190, 2, "Show quick item 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 192, 2, "Show quick item 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 194, 2, "Show quick item 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 196, 2, "Show quick item 4?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 198, 2, "Show quick item 5?", m_partyslot));
      list.add(new TextString(buffer, offset + 200, 32, "Name"));
      list.add(new DecNumber(buffer, offset + 232, 4, "# times talked to"));
      offset = readCharStats(buffer, offset + 236);
      list.add(new Unknown(buffer, offset, 8));
      offset += 8;
    }
    else if (gameid == ResourceFactory.ID_ICEWIND || gameid == ResourceFactory.ID_ICEWINDHOW ||
             gameid == ResourceFactory.ID_ICEWINDHOWTOT) {
      list.add(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      list.add(new Unknown(buffer, offset + 42, 98));
      list.add(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 142, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 146, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 148, 2, "Show quick weapon 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 150, 2, "Show quick weapon 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 152, 2, "Show quick weapon 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 154, 2, "Show quick weapon 4?", m_partyslot));
      list.add(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      list.add(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      list.add(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      list.add(new IdsBitmap(buffer, offset + 180, 2, "Quick item slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 182, 2, "Quick item slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 184, 2, "Quick item slot 3", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 186, 2, "Show quick item 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 188, 2, "Show quick item 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 190, 2, "Show quick item 3?", m_partyslot));
      list.add(new TextString(buffer, offset + 192, 32, "Name"));
      list.add(new Unknown(buffer, offset + 224, 4));
      offset = readCharStats(buffer, offset + 228);
      list.add(new TextString(buffer, offset, 8, "Voice set prefix"));
      list.add(new TextString(buffer, offset + 8, 32, "Voice set"));
      offset += 40;
    }
    else if (gameid == ResourceFactory.ID_ICEWIND2) {
      list.add(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      list.add(new Unknown(buffer, offset + 42, 98));
      list.add(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 142, 2, "Quick shield slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 146, 2, "Quick shield slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 148, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 150, 2, "Quick shield slot 3", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 152, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 154, 2, "Quick shield slot 4", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 156, 2, "Show quick weapon 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 158, 2, "Show quick shield 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 160, 2, "Show quick weapon 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 162, 2, "Show quick shield 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 164, 2, "Show quick weapon 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 166, 2, "Show quick shield 3?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 168, 2, "Show quick weapon 4?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 170, 2, "Show quick shield 4?", m_partyslot));
      list.add(new TextString(buffer, offset + 172, 8, "Quick spell 1"));
      list.add(new TextString(buffer, offset + 180, 8, "Quick spell 2"));
      list.add(new TextString(buffer, offset + 188, 8, "Quick spell 3"));
      list.add(new TextString(buffer, offset + 196, 8, "Quick spell 4"));
      list.add(new TextString(buffer, offset + 204, 8, "Quick spell 5"));
      list.add(new TextString(buffer, offset + 212, 8, "Quick spell 6"));
      list.add(new TextString(buffer, offset + 220, 8, "Quick spell 7"));
      list.add(new TextString(buffer, offset + 228, 8, "Quick spell 8"));
      list.add(new TextString(buffer, offset + 236, 8, "Quick spell 9"));
      list.add(new DecNumber(buffer, offset + 244, 1, "Quick spell 1 class"));
      list.add(new DecNumber(buffer, offset + 245, 1, "Quick spell 2 class"));
      list.add(new DecNumber(buffer, offset + 246, 1, "Quick spell 3 class"));
      list.add(new DecNumber(buffer, offset + 247, 1, "Quick spell 4 class"));
      list.add(new DecNumber(buffer, offset + 248, 1, "Quick spell 5 class"));
      list.add(new DecNumber(buffer, offset + 249, 1, "Quick spell 6 class"));
      list.add(new DecNumber(buffer, offset + 250, 1, "Quick spell 7 class"));
      list.add(new DecNumber(buffer, offset + 251, 1, "Quick spell 8 class"));
      list.add(new DecNumber(buffer, offset + 252, 1, "Quick spell 9 class"));
      list.add(new Unknown(buffer, offset + 253, 1));
      list.add(new IdsBitmap(buffer, offset + 254, 2, "Quick item slot 1", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 256, 2, "Quick item slot 2", "SLOTS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 258, 2, "Quick item slot 3", "SLOTS.IDS"));
      list.add(new HashBitmap(buffer, offset + 260, 2, "Show quick item 1?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 262, 2, "Show quick item 2?", m_partyslot));
      list.add(new HashBitmap(buffer, offset + 264, 2, "Show quick item 3?", m_partyslot));
      list.add(new TextString(buffer, offset + 266, 8, "Quick ability 1"));
      list.add(new TextString(buffer, offset + 274, 8, "Quick ability 2"));
      list.add(new TextString(buffer, offset + 282, 8, "Quick ability 3"));
      list.add(new TextString(buffer, offset + 290, 8, "Quick ability 4"));
      list.add(new TextString(buffer, offset + 298, 8, "Quick ability 5"));
      list.add(new TextString(buffer, offset + 306, 8, "Quick ability 6"));
      list.add(new TextString(buffer, offset + 314, 8, "Quick ability 7"));
      list.add(new TextString(buffer, offset + 322, 8, "Quick ability 8"));
      list.add(new TextString(buffer, offset + 330, 8, "Quick ability 9"));
      list.add(new TextString(buffer, offset + 338, 8, "Quick song 1"));
      list.add(new TextString(buffer, offset + 346, 8, "Quick song 2"));
      list.add(new TextString(buffer, offset + 354, 8, "Quick song 3"));
      list.add(new TextString(buffer, offset + 362, 8, "Quick song 4"));
      list.add(new TextString(buffer, offset + 370, 8, "Quick song 5"));
      list.add(new TextString(buffer, offset + 378, 8, "Quick song 6"));
      list.add(new TextString(buffer, offset + 386, 8, "Quick song 7"));
      list.add(new TextString(buffer, offset + 394, 8, "Quick song 8"));
      list.add(new TextString(buffer, offset + 402, 8, "Quick song 9"));
      list.add(new DecNumber(buffer, offset + 410, 4, "Quick slot 1"));
      list.add(new DecNumber(buffer, offset + 414, 4, "Quick slot 2"));
      list.add(new DecNumber(buffer, offset + 418, 4, "Quick slot 3"));
      list.add(new DecNumber(buffer, offset + 422, 4, "Quick slot 4"));
      list.add(new DecNumber(buffer, offset + 426, 4, "Quick slot 5"));
      list.add(new DecNumber(buffer, offset + 430, 4, "Quick slot 6"));
      list.add(new DecNumber(buffer, offset + 434, 4, "Quick slot 7"));
      list.add(new DecNumber(buffer, offset + 438, 4, "Quick slot 8"));
      list.add(new DecNumber(buffer, offset + 442, 4, "Quick slot 9"));
      list.add(new TextString(buffer, offset + 446, 32, "Name"));
      list.add(new Unknown(buffer, offset + 478, 4));
      offset = readCharStats(buffer, offset + 482);
      list.add(new TextString(buffer, offset, 8, "Voice set prefix"));
      list.add(new TextString(buffer, offset + 8, 32, "Voice set"));
      list.add(new Unknown(buffer, offset + 40, 12));
      list.add(new DecNumber(buffer, offset + 52, 4, "Expertise"));
      list.add(new DecNumber(buffer, offset + 56, 4, "Power attack"));
      list.add(new DecNumber(buffer, offset + 60, 4, "Arterial strike"));
      list.add(new DecNumber(buffer, offset + 64, 4, "Hamstring"));
      list.add(new DecNumber(buffer, offset + 68, 4, "Rapid shot"));
      list.add(new Unknown(buffer, offset + 72, 162));
      offset += 234;
    }

    if (creOffset.getValue() != 0)
      list.add(new CreResource(this, "CRE file", buffer, creOffset.getValue()));

    return offset;
  }

  private int readCharStats(byte buffer[], int offset)
  {
    list.add(new StringRef(buffer, offset, "Most powerful foe vanquished"));
    list.add(new DecNumber(buffer, offset + 4, 4, "XP for most powerful foe"));
    list.add(new DecNumber(buffer, offset + 8, 4, "Time in party (ticks)"));
    list.add(new DecNumber(buffer, offset + 12, 4, "Join time (ticks)"));
    list.add(new Bitmap(buffer, offset + 16, 1, "Currently in party?", s_noyes));
    list.add(new Unknown(buffer, offset + 17, 2));
    list.add(new TextString(buffer, offset + 19, 1, "Initial character"));
    list.add(new DecNumber(buffer, offset + 20, 4, "Kill XP (chapter)"));
    list.add(new DecNumber(buffer, offset + 24, 4, "# kills (chapter)"));
    list.add(new DecNumber(buffer, offset + 28, 4, "Kill XP (game)"));
    list.add(new DecNumber(buffer, offset + 32, 4, "# kills (game)"));
    list.add(new ResourceRef(buffer, offset + 36, "Favorite spell 1", "SPL"));
    list.add(new ResourceRef(buffer, offset + 44, "Favorite spell 2", "SPL"));
    list.add(new ResourceRef(buffer, offset + 52, "Favorite spell 3", "SPL"));
    list.add(new ResourceRef(buffer, offset + 60, "Favorite spell 4", "SPL"));
    list.add(new UnsignDecNumber(buffer, offset + 68, 2, "Favorite spell count 1"));
    list.add(new UnsignDecNumber(buffer, offset + 70, 2, "Favorite spell count 2"));
    list.add(new UnsignDecNumber(buffer, offset + 72, 2, "Favorite spell count 3"));
    list.add(new UnsignDecNumber(buffer, offset + 74, 2, "Favorite spell count 4"));
    list.add(new ResourceRef(buffer, offset + 76, "Favorite weapon 1", "ITM"));
    list.add(new ResourceRef(buffer, offset + 84, "Favorite weapon 2", "ITM"));
    list.add(new ResourceRef(buffer, offset + 92, "Favorite weapon 3", "ITM"));
    list.add(new ResourceRef(buffer, offset + 100, "Favorite weapon 4", "ITM"));
    list.add(new UnsignDecNumber(buffer, offset + 108, 2, "Favorite weapon counter 1"));
    list.add(new UnsignDecNumber(buffer, offset + 110, 2, "Favorite weapon counter 2"));
    list.add(new UnsignDecNumber(buffer, offset + 112, 2, "Favorite weapon counter 3"));
    list.add(new UnsignDecNumber(buffer, offset + 114, 2, "Favorite weapon counter 4"));
    return offset + 116;
  }
}

