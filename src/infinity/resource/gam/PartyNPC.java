// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.HashBitmap;
import infinity.datatype.HexNumber;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.Actor;
import infinity.resource.cre.CreResource;
import infinity.util.LongIntegerHashMap;

import javax.swing.JComponent;

class PartyNPC extends AbstractStruct implements HasViewerTabs, HasAddRemovable, AddRemovable
{
  private static final LongIntegerHashMap<String> partyOrder = new LongIntegerHashMap<String>();
  private static final LongIntegerHashMap<String> m_selected = new LongIntegerHashMap<String>();
  private static final String s_noyes[] = {"No", "Yes"};

  static {
    partyOrder.put(0L, "Slot 1");
    partyOrder.put(1L, "Slot 2");
    partyOrder.put(2L, "Slot 3");
    partyOrder.put(3L, "Slot 4");
    partyOrder.put(4L, "Slot 5");
    partyOrder.put(5L, "Slot 6");
//    partyOrder.put(0x8000L, "In party, dead");
    partyOrder.put(new Long(0xffff), "Not in party");

    m_selected.put(0L, "Not selected");
    m_selected.put(1L, "Selected");
    m_selected.put(32768L, "Dead");
  }

  PartyNPC() throws Exception
  {
    super(null, "Party character",
          ResourceFactory.getGameID() == ResourceFactory.ID_BG1 ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG1TOTSC ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
          ResourceFactory.isEnhancedEdition() ? new byte[352] :
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

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{};
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


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
    return new ViewerNPC(this);
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
    ((DecNumber)getAttribute("CRE structure size")).setValue(getField(getFieldCount() - 1).getSize());
    super.datatypeAddedInChild(child, datatype);
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
    ((DecNumber)getAttribute("CRE structure size")).setValue(getField(getFieldCount() - 1).getSize());
    super.datatypeRemovedInChild(child, datatype);
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
    addField(new HashBitmap(buffer, offset, 2, "Selection state", m_selected));
    addField(new HashBitmap(buffer, offset + 2, 2, "Party position", partyOrder));
    HexNumber creOffset = new HexNumber(buffer, offset + 4, 4, "CRE structure offset");
    addField(creOffset);
    addField(new DecNumber(buffer, offset + 8, 4, "CRE structure size"));
    if (buffer[offset + 12] == 0x2A) {
      addField(new TextString(buffer, offset + 12, 8, "Character"));
    } else {
      addField(new ResourceRef(buffer, offset + 12, "Character", "CRE"));
    }
    addField(new Bitmap(buffer, offset + 20, 4, "Orientation", Actor.s_orientation));
    addField(new ResourceRef(buffer, offset + 24, "Current area", "ARE"));
    addField(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    addField(new DecNumber(buffer, offset + 36, 2, "Viewport location: X"));
    addField(new DecNumber(buffer, offset + 38, 2, "Viewport location: Y"));

    int gameid = ResourceFactory.getGameID();
    if (gameid == ResourceFactory.ID_BG1 || gameid == ResourceFactory.ID_BG1TOTSC) {
      addField(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      addField(new DecNumber(buffer, offset + 42, 2, "Happiness"));
      addField(new Unknown(buffer, offset + 44, 96));
      addField(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 142, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 146, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 148, 2, "Quick weapon 1 ability"));
      addField(new DecNumber(buffer, offset + 150, 2, "Quick weapon 2 ability"));
      addField(new DecNumber(buffer, offset + 152, 2, "Quick weapon 3 ability"));
      addField(new DecNumber(buffer, offset + 154, 2, "Quick weapon 4 ability"));
      addField(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      addField(new IdsBitmap(buffer, offset + 180, 2, "Quick item slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 182, 2, "Quick item slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 184, 2, "Quick item slot 3", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 186, 2, "Quick item 1 ability"));
      addField(new DecNumber(buffer, offset + 188, 2, "Quick item 2 ability"));
      addField(new DecNumber(buffer, offset + 190, 2, "Quick item 3 ability"));
      addField(new TextString(buffer, offset + 192, 32, "Name"));
      addField(new DecNumber(buffer, offset + 224, 4, "# times talked to"));
      offset = readCharStats(buffer, offset + 228);
      addField(new TextString(buffer, offset, 8, "Voice set"));
      offset += 8;
    }
    else if (gameid == ResourceFactory.ID_BG2 || gameid == ResourceFactory.ID_BG2TOB ||
             ResourceFactory.isEnhancedEdition()) {
      addField(new IdsBitmap(buffer, offset + 40, 2, "Modal state", "MODAL.IDS"));
      addField(new DecNumber(buffer, offset + 42, 2, "Happiness"));
      addField(new Unknown(buffer, offset + 44, 96));
      addField(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 142, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 146, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 148, 2, "Quick weapon 1 ability"));
      addField(new DecNumber(buffer, offset + 150, 2, "Quick weapon 2 ability"));
      addField(new DecNumber(buffer, offset + 152, 2, "Quick weapon 3 ability"));
      addField(new DecNumber(buffer, offset + 154, 2, "Quick weapon 4 ability"));
      addField(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      addField(new IdsBitmap(buffer, offset + 180, 2, "Quick item slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 182, 2, "Quick item slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 184, 2, "Quick item slot 3", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 186, 2, "Quick item 1 ability"));
      addField(new DecNumber(buffer, offset + 188, 2, "Quick item 2 ability"));
      addField(new DecNumber(buffer, offset + 190, 2, "Quick item 3 ability"));
      addField(new TextString(buffer, offset + 192, 32, "Name"));
      addField(new DecNumber(buffer, offset + 224, 4, "# times talked to"));
      offset = readCharStats(buffer, offset + 228);
      addField(new TextString(buffer, offset, 8, "Voice set"));
      offset += 8;
    }
    else if (gameid == ResourceFactory.ID_TORMENT) {
      addField(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      addField(new DecNumber(buffer, offset + 42, 2, "Happiness"));
      addField(new Unknown(buffer, offset + 44, 96));
      addField(new DecNumber(buffer, offset + 140, 2, "Quick weapon slot 1"));
      addField(new DecNumber(buffer, offset + 142, 2, "Quick weapon slot 2"));
      addField(new DecNumber(buffer, offset + 144, 2, "Quick weapon slot 3"));
      addField(new DecNumber(buffer, offset + 146, 2, "Quick weapon slot 4"));
      addField(new DecNumber(buffer, offset + 148, 2, "Quick weapon 1 ability"));
      addField(new DecNumber(buffer, offset + 150, 2, "Quick weapon 2 ability"));
      addField(new DecNumber(buffer, offset + 152, 2, "Quick weapon 3 ability"));
      addField(new DecNumber(buffer, offset + 154, 2, "Quick weapon 4 ability"));
      addField(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      addField(new DecNumber(buffer, offset + 180, 2, "Quick item slot 1"));
      addField(new DecNumber(buffer, offset + 182, 2, "Quick item slot 2"));
      addField(new DecNumber(buffer, offset + 184, 2, "Quick item slot 3"));
      addField(new DecNumber(buffer, offset + 186, 2, "Quick item slot 4"));
      addField(new DecNumber(buffer, offset + 188, 2, "Quick item slot 5"));
      addField(new DecNumber(buffer, offset + 190, 2, "Quick item 1 ability"));
      addField(new DecNumber(buffer, offset + 192, 2, "Quick item 2 ability"));
      addField(new DecNumber(buffer, offset + 194, 2, "Quick item 3 ability"));
      addField(new DecNumber(buffer, offset + 196, 2, "Quick item 4 ability"));
      addField(new DecNumber(buffer, offset + 198, 2, "Quick item 5 ability"));
      addField(new TextString(buffer, offset + 200, 32, "Name"));
      addField(new DecNumber(buffer, offset + 232, 4, "# times talked to"));
      offset = readCharStats(buffer, offset + 236);
      addField(new Unknown(buffer, offset, 8));
      offset += 8;
    }
    else if (gameid == ResourceFactory.ID_ICEWIND || gameid == ResourceFactory.ID_ICEWINDHOW ||
             gameid == ResourceFactory.ID_ICEWINDHOWTOT) {
      addField(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      addField(new Unknown(buffer, offset + 42, 98));
      addField(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 142, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 146, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 148, 2, "Quick weapon 1 ability"));
      addField(new DecNumber(buffer, offset + 150, 2, "Quick weapon 2 ability"));
      addField(new DecNumber(buffer, offset + 152, 2, "Quick weapon 3 ability"));
      addField(new DecNumber(buffer, offset + 154, 2, "Quick weapon 4 ability"));
      addField(new ResourceRef(buffer, offset + 156, "Quick spell 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 164, "Quick spell 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 172, "Quick spell 3", "SPL"));
      addField(new IdsBitmap(buffer, offset + 180, 2, "Quick item slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 182, 2, "Quick item slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 184, 2, "Quick item slot 3", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 186, 2, "Quick item 1 ability"));
      addField(new DecNumber(buffer, offset + 188, 2, "Quick item 2 ability"));
      addField(new DecNumber(buffer, offset + 190, 2, "Quick item 3 ability"));
      addField(new TextString(buffer, offset + 192, 32, "Name"));
      addField(new Unknown(buffer, offset + 224, 4));
      offset = readCharStats(buffer, offset + 228);
      addField(new TextString(buffer, offset, 8, "Voice set prefix"));
      addField(new TextString(buffer, offset + 8, 32, "Voice set"));
      offset += 40;
    }
    else if (gameid == ResourceFactory.ID_ICEWIND2) {
      addField(new DecNumber(buffer, offset + 40, 2, "Modal state"));
      addField(new Unknown(buffer, offset + 42, 98));
      addField(new IdsBitmap(buffer, offset + 140, 2, "Quick weapon slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 142, 2, "Quick shield slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 144, 2, "Quick weapon slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 146, 2, "Quick shield slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 148, 2, "Quick weapon slot 3", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 150, 2, "Quick shield slot 3", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 152, 2, "Quick weapon slot 4", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 154, 2, "Quick shield slot 4", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 156, 2, "Quick weapon 1 ability"));
      addField(new DecNumber(buffer, offset + 158, 2, "Quick shield 1 ability"));
      addField(new DecNumber(buffer, offset + 160, 2, "Quick weapon 2 ability"));
      addField(new DecNumber(buffer, offset + 162, 2, "Quick shield 2 ability"));
      addField(new DecNumber(buffer, offset + 164, 2, "Quick weapon 3 ability"));
      addField(new DecNumber(buffer, offset + 166, 2, "Quick shield 3 ability"));
      addField(new DecNumber(buffer, offset + 168, 2, "Quick weapon 4 ability"));
      addField(new DecNumber(buffer, offset + 170, 2, "Quick shield 4 ability"));
      addField(new ResourceRef(buffer, offset + 172, "Quick spell 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 180, "Quick spell 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 188, "Quick spell 3", "SPL"));
      addField(new ResourceRef(buffer, offset + 196, "Quick spell 4", "SPL"));
      addField(new ResourceRef(buffer, offset + 204, "Quick spell 5", "SPL"));
      addField(new ResourceRef(buffer, offset + 212, "Quick spell 6", "SPL"));
      addField(new ResourceRef(buffer, offset + 220, "Quick spell 7", "SPL"));
      addField(new ResourceRef(buffer, offset + 228, "Quick spell 8", "SPL"));
      addField(new ResourceRef(buffer, offset + 236, "Quick spell 9", "SPL"));
      addField(new IdsBitmap(buffer, offset + 244, 1, "Quick spell 1 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 245, 1, "Quick spell 2 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 246, 1, "Quick spell 3 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 247, 1, "Quick spell 4 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 248, 1, "Quick spell 5 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 249, 1, "Quick spell 6 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 250, 1, "Quick spell 7 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 251, 1, "Quick spell 8 class", "CLASS.IDS"));
      addField(new IdsBitmap(buffer, offset + 252, 1, "Quick spell 9 class", "CLASS.IDS"));
      addField(new Unknown(buffer, offset + 253, 1));
      addField(new IdsBitmap(buffer, offset + 254, 2, "Quick item slot 1", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 256, 2, "Quick item slot 2", "SLOTS.IDS"));
      addField(new IdsBitmap(buffer, offset + 258, 2, "Quick item slot 3", "SLOTS.IDS"));
      addField(new DecNumber(buffer, offset + 260, 2, "Quick item 1 ability"));
      addField(new DecNumber(buffer, offset + 262, 2, "Quick item 2 ability"));
      addField(new DecNumber(buffer, offset + 264, 2, "Quick item 3 ability"));
      addField(new ResourceRef(buffer, offset + 266, "Quick ability 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 274, "Quick ability 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 282, "Quick ability 3", "SPL"));
      addField(new ResourceRef(buffer, offset + 290, "Quick ability 4", "SPL"));
      addField(new ResourceRef(buffer, offset + 298, "Quick ability 5", "SPL"));
      addField(new ResourceRef(buffer, offset + 306, "Quick ability 6", "SPL"));
      addField(new ResourceRef(buffer, offset + 314, "Quick ability 7", "SPL"));
      addField(new ResourceRef(buffer, offset + 322, "Quick ability 8", "SPL"));
      addField(new ResourceRef(buffer, offset + 330, "Quick ability 9", "SPL"));
      addField(new ResourceRef(buffer, offset + 338, "Quick song 1", "SPL"));
      addField(new ResourceRef(buffer, offset + 346, "Quick song 2", "SPL"));
      addField(new ResourceRef(buffer, offset + 354, "Quick song 3", "SPL"));
      addField(new ResourceRef(buffer, offset + 362, "Quick song 4", "SPL"));
      addField(new ResourceRef(buffer, offset + 370, "Quick song 5", "SPL"));
      addField(new ResourceRef(buffer, offset + 378, "Quick song 6", "SPL"));
      addField(new ResourceRef(buffer, offset + 386, "Quick song 7", "SPL"));
      addField(new ResourceRef(buffer, offset + 394, "Quick song 8", "SPL"));
      addField(new ResourceRef(buffer, offset + 402, "Quick song 9", "SPL"));
      addField(new DecNumber(buffer, offset + 410, 4, "Quick button 1"));
      addField(new DecNumber(buffer, offset + 414, 4, "Quick button 2"));
      addField(new DecNumber(buffer, offset + 418, 4, "Quick button 3"));
      addField(new DecNumber(buffer, offset + 422, 4, "Quick button 4"));
      addField(new DecNumber(buffer, offset + 426, 4, "Quick button 5"));
      addField(new DecNumber(buffer, offset + 430, 4, "Quick button 6"));
      addField(new DecNumber(buffer, offset + 434, 4, "Quick button 7"));
      addField(new DecNumber(buffer, offset + 438, 4, "Quick button 8"));
      addField(new DecNumber(buffer, offset + 442, 4, "Quick button 9"));
      addField(new TextString(buffer, offset + 446, 32, "Name"));
      addField(new Unknown(buffer, offset + 478, 4));
      offset = readCharStats(buffer, offset + 482);
      addField(new TextString(buffer, offset, 8, "Voice set prefix"));
      addField(new TextString(buffer, offset + 8, 32, "Voice set"));
      addField(new Unknown(buffer, offset + 40, 12));
      addField(new DecNumber(buffer, offset + 52, 4, "Expertise"));
      addField(new DecNumber(buffer, offset + 56, 4, "Power attack"));
      addField(new DecNumber(buffer, offset + 60, 4, "Arterial strike"));
      addField(new DecNumber(buffer, offset + 64, 4, "Hamstring"));
      addField(new DecNumber(buffer, offset + 68, 4, "Rapid shot"));
      addField(new Unknown(buffer, offset + 72, 162));
      offset += 234;
    }

    if (creOffset.getValue() != 0) {
      addField(new CreResource(this, "CRE file", buffer, creOffset.getValue()));
    }

    return offset;
  }

  private int readCharStats(byte buffer[], int offset)
  {
    addField(new StringRef(buffer, offset, "Most powerful foe vanquished"));
    addField(new DecNumber(buffer, offset + 4, 4, "XP for most powerful foe"));
    addField(new DecNumber(buffer, offset + 8, 4, "Time in party (ticks)"));
    addField(new DecNumber(buffer, offset + 12, 4, "Join time (ticks)"));
    addField(new Bitmap(buffer, offset + 16, 1, "Currently in party?", s_noyes));
    addField(new Unknown(buffer, offset + 17, 2));
    addField(new TextString(buffer, offset + 19, 1, "Initial character"));
    addField(new DecNumber(buffer, offset + 20, 4, "Kill XP (chapter)"));
    addField(new DecNumber(buffer, offset + 24, 4, "# kills (chapter)"));
    addField(new DecNumber(buffer, offset + 28, 4, "Kill XP (game)"));
    addField(new DecNumber(buffer, offset + 32, 4, "# kills (game)"));
    addField(new ResourceRef(buffer, offset + 36, "Favorite spell 1", "SPL"));
    addField(new ResourceRef(buffer, offset + 44, "Favorite spell 2", "SPL"));
    addField(new ResourceRef(buffer, offset + 52, "Favorite spell 3", "SPL"));
    addField(new ResourceRef(buffer, offset + 60, "Favorite spell 4", "SPL"));
    addField(new UnsignDecNumber(buffer, offset + 68, 2, "Favorite spell count 1"));
    addField(new UnsignDecNumber(buffer, offset + 70, 2, "Favorite spell count 2"));
    addField(new UnsignDecNumber(buffer, offset + 72, 2, "Favorite spell count 3"));
    addField(new UnsignDecNumber(buffer, offset + 74, 2, "Favorite spell count 4"));
    addField(new ResourceRef(buffer, offset + 76, "Favorite weapon 1", "ITM"));
    addField(new ResourceRef(buffer, offset + 84, "Favorite weapon 2", "ITM"));
    addField(new ResourceRef(buffer, offset + 92, "Favorite weapon 3", "ITM"));
    addField(new ResourceRef(buffer, offset + 100, "Favorite weapon 4", "ITM"));
    addField(new UnsignDecNumber(buffer, offset + 108, 2, "Favorite weapon counter 1"));
    addField(new UnsignDecNumber(buffer, offset + 110, 2, "Favorite weapon counter 2"));
    addField(new UnsignDecNumber(buffer, offset + 112, 2, "Favorite weapon counter 3"));
    addField(new UnsignDecNumber(buffer, offset + 114, 2, "Favorite weapon counter 4"));
    return offset + 116;
  }
}

