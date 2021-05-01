// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.io.StreamUtils;

public class PartyNPC extends AbstractStruct implements HasViewerTabs, AddRemovable
{
  // GAM/PartyNPC-specific field labels
  public static final String GAM_NPC                            = "Party member";
  public static final String GAM_NPC_SELECTION_STATE            = "Selection state";
  public static final String GAM_NPC_PARTY_POSITION             = "Party position";
  public static final String GAM_NPC_OFFSET_CRE                 = CreResource.CHR_OFFSET_CRE;
  public static final String GAM_NPC_CRE_SIZE                   = CreResource.CHR_CRE_SIZE;
  public static final String GAM_NPC_CHARACTER                  = "Character";
  public static final String GAM_NPC_ORIENTATION                = "Orientation";
  public static final String GAM_NPC_CURRENT_AREA               = "Current area";
  public static final String GAM_NPC_LOCATION_X                 = "Location: X";
  public static final String GAM_NPC_LOCATION_Y                 = "Location: Y";
  public static final String GAM_NPC_VIEWPORT_X                 = "Viewport location: X";
  public static final String GAM_NPC_VIEWPORT_Y                 = "Viewport location: Y";
  public static final String GAM_NPC_MODAL_STATE                = "Modal state";
  public static final String GAM_NPC_HAPPINESS                  = "Happiness";
  public static final String GAM_NPC_QUICK_ITEMS                = "Quick items";
  public static final String GAM_NPC_ITEM_ABILITIES             = "Item abilities";
  public static final String GAM_NPC_QUICK_WEAPON_SLOT_FMT      = CreResource.CHR_QUICK_WEAPON_SLOT_FMT;
  public static final String GAM_NPC_QUICK_SHIELD_SLOT_FMT      = CreResource.CHR_QUICK_SHIELD_SLOT_FMT;
  public static final String GAM_NPC_QUICK_WEAPON_ABILITY_FMT   = CreResource.CHR_QUICK_WEAPON_ABILITY_FMT;
  public static final String GAM_NPC_QUICK_SHIELD_ABILITY_FMT   = CreResource.CHR_QUICK_SHIELD_ABILITY_FMT;
  public static final String GAM_NPC_QUICK_SPELL_FMT            = CreResource.CHR_QUICK_SPELL_FMT;
  public static final String GAM_NPC_QUICK_SPELL_CLASS_FMT      = CreResource.CHR_QUICK_SPELL_CLASS_FMT;
  public static final String GAM_NPC_QUICK_ITEM_SLOT_FMT        = CreResource.CHR_QUICK_ITEM_SLOT_FMT;
  public static final String GAM_NPC_QUICK_ITEM_ABILITY_FMT     = CreResource.CHR_QUICK_ITEM_ABILITY_FMT;
  public static final String GAM_NPC_QUICK_ABILITY_FMT          = CreResource.CHR_QUICK_ABILITY_FMT;
  public static final String GAM_NPC_QUICK_SONG_FMT             = CreResource.CHR_QUICK_SONG_FMT;
  public static final String GAM_NPC_QUICK_BUTTON_FMT           = CreResource.CHR_QUICK_BUTTON_FMT;
  public static final String GAM_NPC_NAME                       = CreResource.CHR_NAME;
  public static final String GAM_NPC_VOICE_SET                  = CreResource.CHR_VOICE_SET;
  public static final String GAM_NPC_VOICE_SET_PREFIX           = CreResource.CHR_VOICE_SET_PREFIX;
  public static final String GAM_NPC_NUM_TIMES_TALKED_TO        = "# times talked to";
  public static final String GAM_NPC_EXPERTISE                  = "Expertise";
  public static final String GAM_NPC_POWER_ATTACK               = "Power attack";
  public static final String GAM_NPC_ARTERIAL_STRIKE            = "Arterial strike";
  public static final String GAM_NPC_HAMSTRING                  = "Hamstring";
  public static final String GAM_NPC_RAPID_SHOT                 = "Rapid shot";
  public static final String GAM_NPC_CRE_RESOURCE               = "CRE resource";
  public static final String GAM_NPC_STAT_FOE_VANQUISHED        = "Most powerful foe vanquished";
  public static final String GAM_NPC_STAT_XP_FOE_VANQUISHED     = "XP for most powerful foe";
  public static final String GAM_NPC_STAT_TIME_IN_PARTY         = "Time in party (ticks)";
  public static final String GAM_NPC_STAT_JOIN_TIME             = "Join time (ticks)";
  public static final String GAM_NPC_STAT_IN_PARTY              = "Currently in party?";
  public static final String GAM_NPC_STAT_INITIAL_CHAR          = "Initial character";
  public static final String GAM_NPC_STAT_KILLS_XP_CHAPTER      = "Kill XP (chapter)";
  public static final String GAM_NPC_STAT_NUM_KILLS_CHAPTER     = "# kills (chapter)";
  public static final String GAM_NPC_STAT_KILLS_XP_GAME         = "Kill XP (game)";
  public static final String GAM_NPC_STAT_NUM_KILLS_GAME        = "# kills (game)";
  public static final String GAM_NPC_STAT_FAV_SPELL_FMT         = "Favorite spell %d";
  public static final String GAM_NPC_STAT_FAV_SPELL_COUNT_FMT   = "Favorite spell count %d";
  public static final String GAM_NPC_STAT_FAV_WEAPON_FMT        = "Favorite weapon %d";
  public static final String GAM_NPC_STAT_FAV_WEAPON_COUNT_FMT  = "Favorite weapon counter %d";

  public static final TreeMap<Long, String> m_partyOrder = new TreeMap<>();
//  private static final TreeMap<Long, String> m_selected = new TreeMap<>();

  private static final String[] s_selected = {"Not selected", "Selected", null, null, null, null, null, null, null,
                                              null, null, null, null, null, null, null, "Dead" };

  static {
    m_partyOrder.put(0L, "Slot 1");
    m_partyOrder.put(1L, "Slot 2");
    m_partyOrder.put(2L, "Slot 3");
    m_partyOrder.put(3L, "Slot 4");
    m_partyOrder.put(4L, "Slot 5");
    m_partyOrder.put(5L, "Slot 6");
    m_partyOrder.put(-1L, "Not in party");

//    m_selected.put(0L, "Not selected");
//    m_selected.put(1L, "Selected");
//    m_selected.put(0x8000L, "Dead");
  }

  PartyNPC() throws Exception
  {
    super(null, GAM_NPC, createEmptyBuffer(), 0);
  }

  PartyNPC(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, GAM_NPC + " " + nr, buffer, offset);
  }

  PartyNPC(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

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
    final StructEntry last = getFields().get(getFields().size() - 1);
    ((DecNumber)getAttribute(GAM_NPC_CRE_SIZE)).setValue(last.getSize());
    super.datatypeAddedInChild(child, datatype);
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (datatype instanceof CreResource) {
      ((DecNumber)getAttribute(GAM_NPC_CRE_SIZE)).setValue(0);
      ((HexNumber)getAttribute(GAM_NPC_OFFSET_CRE)).setValue(0);
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    final StructEntry last = getFields().get(getFields().size() - 1);
    ((DecNumber)getAttribute(GAM_NPC_CRE_SIZE)).setValue(last.getSize());
    super.datatypeRemovedInChild(child, datatype);
  }

  void updateCREOffset()
  {
    final StructEntry entry = getFields().get(getFields().size() - 1);
    if (entry instanceof CreResource)
      ((HexNumber)getAttribute(GAM_NPC_OFFSET_CRE)).setValue(entry.getOffset());
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new Flag(buffer, offset, 2, GAM_NPC_SELECTION_STATE, s_selected));
    addField(new HashBitmap(buffer, offset + 2, 2, GAM_NPC_PARTY_POSITION, m_partyOrder, true, true));
    HexNumber creOffset = new HexNumber(buffer, offset + 4, 4, GAM_NPC_OFFSET_CRE);
    addField(creOffset);
    addField(new DecNumber(buffer, offset + 8, 4, GAM_NPC_CRE_SIZE));
    if (buffer.get(offset + 12) == 0x2A) {
      addField(new TextString(buffer, offset + 12, 8, GAM_NPC_CHARACTER));
    } else {
      addField(new ResourceRef(buffer, offset + 12, GAM_NPC_CHARACTER, "CRE"));
    }
    addField(new Bitmap(buffer, offset + 20, 4, GAM_NPC_ORIENTATION, OPTION_ORIENTATION));
    addField(new ResourceRef(buffer, offset + 24, GAM_NPC_CURRENT_AREA, "ARE"));
    addField(new DecNumber(buffer, offset + 32, 2, GAM_NPC_LOCATION_X));
    addField(new DecNumber(buffer, offset + 34, 2, GAM_NPC_LOCATION_Y));
    addField(new DecNumber(buffer, offset + 36, 2, GAM_NPC_VIEWPORT_X));
    addField(new DecNumber(buffer, offset + 38, 2, GAM_NPC_VIEWPORT_Y));

    IdsBitmap bitmap;
    final IdsMapEntry entryNone = new IdsMapEntry(-1L, "NONE");
    if (Profile.getEngine() == Profile.Engine.BG1) {
      addField(new DecNumber(buffer, offset + 40, 2, GAM_NPC_MODAL_STATE));
      addField(new DecNumber(buffer, offset + 42, 2, GAM_NPC_HAPPINESS));
      addField(new Unknown(buffer, offset + 44, 96, COMMON_UNUSED));
      for (int i = 0; i < 4; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 140 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_WEAPON_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 4; i++) {
        addField(new DecNumber(buffer, offset + 148 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_WEAPON_ABILITY_FMT, i+1)));
      }
      for (int i = 0; i < 3; i++) {
        addField(new ResourceRef(buffer, offset + 156 + (i * 8),
                                 String.format(GAM_NPC_QUICK_SPELL_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 3; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 180 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_ITEM_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 3; i++) {
        addField(new DecNumber(buffer, offset + 186 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_ITEM_ABILITY_FMT, i+1)));
      }
      addField(new TextString(buffer, offset + 192, 32, GAM_NPC_NAME));
      addField(new DecNumber(buffer, offset + 224, 4, GAM_NPC_NUM_TIMES_TALKED_TO));
      offset = readCharStats(buffer, offset + 228);
      addField(new TextString(buffer, offset, 8, GAM_NPC_VOICE_SET));
      offset += 8;
    }
    else if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new IdsBitmap(buffer, offset + 40, 2, GAM_NPC_MODAL_STATE, "MODAL.IDS"));
      addField(new DecNumber(buffer, offset + 42, 2, GAM_NPC_HAPPINESS));
      int size = (Profile.getGame() == Profile.Game.PSTEE) ? 88 : 96;
      addField(new Unknown(buffer, offset + 44, size, COMMON_UNUSED));
      if (size == 88) {  // PSTEE
        // TODO: confirm fields
        addField(new DecNumber(buffer, offset + 132, 2, GAM_NPC_QUICK_ITEMS));
        addField(new DecNumber(buffer, offset + 134, 2, GAM_NPC_ITEM_ABILITIES));
        addField(new DecNumber(buffer, offset + 136, 2, COMMON_UNKNOWN));
        addField(new DecNumber(buffer, offset + 138, 2, COMMON_UNKNOWN));
      }
      for (int i = 0; i < 4; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 140 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_WEAPON_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 4; i++) {
        addField(new DecNumber(buffer, offset + 148 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_WEAPON_ABILITY_FMT, i+1)));
      }
      for (int i = 0; i < 3; i++) {
        addField(new ResourceRef(buffer, offset + 156 + (i * 8),
                                 String.format(GAM_NPC_QUICK_SPELL_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 3; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 180 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_ITEM_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 3; i++) {
        addField(new DecNumber(buffer, offset + 186 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_ITEM_ABILITY_FMT, i+1)));
      }
      addField(new TextString(buffer, offset + 192, 32, GAM_NPC_NAME));
      addField(new DecNumber(buffer, offset + 224, 4, GAM_NPC_NUM_TIMES_TALKED_TO));
      offset = readCharStats(buffer, offset + 228);
      addField(new TextString(buffer, offset, 8, GAM_NPC_VOICE_SET));
      offset += 8;
    }
    else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new DecNumber(buffer, offset + 40, 2, GAM_NPC_MODAL_STATE));
      addField(new DecNumber(buffer, offset + 42, 2, GAM_NPC_HAPPINESS));
      addField(new Unknown(buffer, offset + 44, 96, COMMON_UNUSED));
      for (int i = 0; i < 4; i++) {
        addField(new DecNumber(buffer, offset + 140 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_WEAPON_SLOT_FMT, i+1)));
      }
      for (int i = 0; i < 4; i++) {
        addField(new DecNumber(buffer, offset + 148 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_WEAPON_ABILITY_FMT, i+1)));
      }
      for (int i = 0; i < 3; i++) {
        addField(new ResourceRef(buffer, offset + 156 + (i * 8),
                                 String.format(GAM_NPC_QUICK_SPELL_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 5; i++) {
        addField(new DecNumber(buffer, offset + 180 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_ITEM_SLOT_FMT, i+1)));
      }
      for (int i = 0; i < 5; i++) {
        addField(new DecNumber(buffer, offset + 190 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_ITEM_ABILITY_FMT, i+1)));
      }
      addField(new TextString(buffer, offset + 200, 32, GAM_NPC_NAME));
      addField(new DecNumber(buffer, offset + 232, 4, GAM_NPC_NUM_TIMES_TALKED_TO));
      offset = readCharStats(buffer, offset + 236);
      addField(new Unknown(buffer, offset, 8));
      offset += 8;
    }
    else if (Profile.getEngine() == Profile.Engine.IWD) {
      addField(new DecNumber(buffer, offset + 40, 2, GAM_NPC_MODAL_STATE));
      addField(new Unknown(buffer, offset + 42, 98));
      for (int i = 0; i < 4; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 140 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_WEAPON_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 4; i++) {
        addField(new DecNumber(buffer, offset + 148 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_WEAPON_ABILITY_FMT, i+1)));
      }
      for (int i = 0; i < 3; i++) {
        addField(new ResourceRef(buffer, offset + 156 + (i * 8),
                                 String.format(GAM_NPC_QUICK_SPELL_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 3; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 180 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_ITEM_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 3; i++) {
        addField(new DecNumber(buffer, offset + 186 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_ITEM_ABILITY_FMT, i+1)));
      }
      addField(new TextString(buffer, offset + 192, 32, GAM_NPC_NAME));
      addField(new Unknown(buffer, offset + 224, 4));
      offset = readCharStats(buffer, offset + 228);
      addField(new TextString(buffer, offset, 8, GAM_NPC_VOICE_SET_PREFIX));
      addField(new TextString(buffer, offset + 8, 32, GAM_NPC_VOICE_SET));
      offset += 40;
    }
    else if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new DecNumber(buffer, offset + 40, 2, GAM_NPC_MODAL_STATE));
      addField(new Unknown(buffer, offset + 42, 98));
      for (int i = 0; i < 4; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 140 + (i * 4), 2,
                                        String.format(GAM_NPC_QUICK_WEAPON_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
        bitmap = addField(new IdsBitmap(buffer, offset + 142 + (i * 4), 2,
                                        String.format(GAM_NPC_QUICK_SHIELD_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 4; i++) {
        addField(new DecNumber(buffer, offset + 156 + (i * 4), 2,
                               String.format(GAM_NPC_QUICK_WEAPON_ABILITY_FMT, i+1)));
        addField(new DecNumber(buffer, offset + 158 + (i * 4), 2,
                               String.format(GAM_NPC_QUICK_SHIELD_ABILITY_FMT, i+1)));
      }
      for (int i = 0; i < 9; i++) {
        addField(new ResourceRef(buffer, offset + 172 + (i * 8),
                                 String.format(GAM_NPC_QUICK_SPELL_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 9; i++) {
        addField(new IdsBitmap(buffer, offset + 244 + i, 1,
                               String.format(GAM_NPC_QUICK_SPELL_CLASS_FMT, i+1), "CLASS.IDS"));
      }
      addField(new Unknown(buffer, offset + 253, 1));
      for (int i = 0; i < 3; i++) {
        bitmap = addField(new IdsBitmap(buffer, offset + 254 + (i * 2), 2,
                                        String.format(GAM_NPC_QUICK_ITEM_SLOT_FMT, i+1), "SLOTS.IDS", true, false, true));
        bitmap.addIdsMapEntry(entryNone);
      }
      for (int i = 0; i < 3; i++) {
        addField(new DecNumber(buffer, offset + 260 + (i * 2), 2,
                               String.format(GAM_NPC_QUICK_ITEM_ABILITY_FMT, i+1)));
      }
      for (int i = 0; i < 9; i++) {
        addField(new ResourceRef(buffer, offset + 266 + (i * 8),
                                 String.format(GAM_NPC_QUICK_ABILITY_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 9; i++) {
        addField(new ResourceRef(buffer, offset + 338 + (i * 8),
                                 String.format(GAM_NPC_QUICK_SONG_FMT, i+1), "SPL"));
      }
      for (int i = 0; i < 9; i++) {
        addField(new DecNumber(buffer, offset + 410 + (i * 4), 4,
                               String.format(GAM_NPC_QUICK_BUTTON_FMT, i+1)));
      }
      addField(new TextString(buffer, offset + 446, 32, GAM_NPC_NAME));
      addField(new Unknown(buffer, offset + 478, 4));
      offset = readCharStats(buffer, offset + 482);
      addField(new TextString(buffer, offset, 8, GAM_NPC_VOICE_SET_PREFIX));
      addField(new TextString(buffer, offset + 8, 32, GAM_NPC_VOICE_SET));
      addField(new Unknown(buffer, offset + 40, 12));
      addField(new DecNumber(buffer, offset + 52, 4, GAM_NPC_EXPERTISE));
      addField(new DecNumber(buffer, offset + 56, 4, GAM_NPC_POWER_ATTACK));
      addField(new DecNumber(buffer, offset + 60, 4, GAM_NPC_ARTERIAL_STRIKE));
      addField(new DecNumber(buffer, offset + 64, 4, GAM_NPC_HAMSTRING));
      addField(new DecNumber(buffer, offset + 68, 4, GAM_NPC_RAPID_SHOT));
      addField(new Unknown(buffer, offset + 72, 162));
      offset += 234;
    }

    if (creOffset.getValue() != 0) {
      addField(new CreResource(this, GAM_NPC_CRE_RESOURCE, buffer, creOffset.getValue()));
    }

    return offset;
  }

  private int readCharStats(ByteBuffer buffer, int offset)
  {
    addField(new StringRef(buffer, offset, GAM_NPC_STAT_FOE_VANQUISHED));
    addField(new DecNumber(buffer, offset + 4, 4, GAM_NPC_STAT_XP_FOE_VANQUISHED));
    addField(new DecNumber(buffer, offset + 8, 4, GAM_NPC_STAT_TIME_IN_PARTY));
    addField(new DecNumber(buffer, offset + 12, 4, GAM_NPC_STAT_JOIN_TIME));
    addField(new Bitmap(buffer, offset + 16, 1, GAM_NPC_STAT_IN_PARTY, OPTION_NOYES));
    addField(new Unknown(buffer, offset + 17, 2));
    addField(new TextString(buffer, offset + 19, 1, GAM_NPC_STAT_INITIAL_CHAR));
    addField(new DecNumber(buffer, offset + 20, 4, GAM_NPC_STAT_KILLS_XP_CHAPTER));
    addField(new DecNumber(buffer, offset + 24, 4, GAM_NPC_STAT_NUM_KILLS_CHAPTER));
    addField(new DecNumber(buffer, offset + 28, 4, GAM_NPC_STAT_KILLS_XP_GAME));
    addField(new DecNumber(buffer, offset + 32, 4, GAM_NPC_STAT_NUM_KILLS_GAME));
    for (int i = 0; i < 4; i++) {
      addField(new ResourceRef(buffer, offset + 36 + (i * 8),
                               String.format(GAM_NPC_STAT_FAV_SPELL_FMT, i+1), "SPL"));
    }
    for (int i = 0; i < 4; i++) {
      addField(new UnsignDecNumber(buffer, offset + 68 + (i * 2), 2,
                                   String.format(GAM_NPC_STAT_FAV_SPELL_COUNT_FMT, i+1)));
    }
    for (int i = 0; i < 4; i++) {
      addField(new ResourceRef(buffer, offset + 76 + (i * 8),
                               String.format(GAM_NPC_STAT_FAV_WEAPON_FMT, i+1), "ITM"));
    }
    for (int i = 0; i < 4; i++) {
      addField(new UnsignDecNumber(buffer, offset + 108 + (i * 2), 2,
                                   String.format(GAM_NPC_STAT_FAV_WEAPON_COUNT_FMT, i+1)));
    }
    return offset + 116;
  }

  protected static ByteBuffer createEmptyBuffer()
  {
    int size = 0;
    if (Profile.getEngine() == Profile.Engine.BG1 ||
        Profile.getEngine() == Profile.Engine.BG2 ||
        Profile.isEnhancedEdition()) {
      size = 352;
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      size = 360;
    } else if (Profile.getEngine() == Profile.Engine.IWD2) {
      size = 832;
    } else {
      size = 384;
    }
    return StreamUtils.getByteBuffer(size);
  }
}
