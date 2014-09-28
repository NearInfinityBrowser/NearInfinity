// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.itm;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextBitmap;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Effect;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public final class ItmResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
  public static final String s_categories[] =
          {"Miscellaneous", "Amulets and necklaces", "Armor", "Belts and girdles",
           "Boots", "Arrows", "Bracers and gauntlets", "Headgear",
           "Keys", "Potions", "Rings", "Scrolls", "Shields", "Food",
           "Bullets", "Bows", "Daggers", "Maces", "Slings", "Small swords",
           "Large swords", "Hammers", "Morning stars", "Flails", "Darts",
           "Axes", "Quarterstaves", "Crossbows", "Hand-to-hand weapons",
           "Spears", "Halberds", "Bolts", "Cloaks and robes",
           "Gold pieces", "Gems", "Wands", "Containers", "Books",
           "Familiars", "Tattoos", "Lenses", "Bucklers",
           "Candles", "Child bodies", "Clubs", "Female bodies", "Keys (old)",
           "Large shields", "Male bodies", "Medium shields", "Notes",
           "Rods", "Skulls", "Small shields", "Spider bodies",
           "Telescopes", "Bottles", "Greatswords", "Bags",
           "Furs and pelts", "Leather armor", "Studded leather", "Chain mail",
           "Splint mail", "Plate mail", "Full plate", "Hide armor", "Robes",
           "Scale mail", "Bastard swords", "Scarves", "Rations", "Hats", "Gloves"};
  public static final String s_categories11[] =
          {"Miscellaneous", "Amulets and necklaces", "Armor", "Belts and girdles",
           "Boots", "Arrows", "Bracers and gauntlets", "Headgear",
           "Keys", "Potions", "Rings", "Scrolls", "Shields", "Spells",
           "Bullets", "Bows", "Daggers", "Maces", "Slings", "Small swords",
           "Large swords", "Hammers", "Morning stars", "Flails", "Darts",
           "Axes", "Quarterstaves", "Crossbows", "Hand-to-hand weapons",
           "Greatswords", "Halberds", "Bolts", "Cloaks and robes",
           "Copper commons", "Gems", "Wands", "Eyeballs", "Bracelets",
           "Earrings", "Tattoos", "Lenses", "Teeth"};
  public static final String[] s_flags =
          {"None", "Unsellable", "Two-handed", "Droppable", "Displayable",
           "Cursed", "Not copyable", "Magical", "Left-handed", "Silver", "Cold iron", "Off-handed", "Conversable", "", "", "", "",
           "", "", "", "", "", "", "", "", "Ex: undispellable", "Ex: toggle critical hits"};
  public static final String[] s_flags11 =
          {"None", "Unsellable", "Two-handed", "Droppable", "Displayable",
           "Cursed", "Not copyable", "Magical", "Left-handed", "Silver", "Cold iron", "Steel", "Conversable",
           "Pulsating"};
  public static final String[] s_usability =
          {"None", "Chaotic", "Evil", "Good", "... Neutral", "Lawful",
           "Neutral ...", "Bard", "Cleric", "Cleric-Mage",
           "Cleric-Thief", "Cleric-Ranger", "Fighter", "Fighter-Druid",
           "Fighter-Mage", "Fighter-Cleric", "Fighter-Mage-Cleric",
           "Fighter-Mage-Thief", "Fighter-Thief", "Mage", "Mage-Thief",
           "Paladin", "Ranger", "Thief", "Elf", "Dwarf", "Half-Elf",
           "Halfling", "Human", "Gnome", "Monk", "Druid"};
  public static final String[] s_usability11 =
          {"None", "Chaotic", "Evil", "Good",
           "... Neutral", "Lawful", "Neutral ...", "Sensate",
           "Priest", "Godsman", "Anarchist", "Xaositect",
           "Fighter", "Non-aligned", "Fighter-Mage", "Dustman",
           "Mercykiller", "Indep", "Figher-Thief", "Mage",
           "Mage-Thief", "Dak'kon", "Fall-From-Grace", "Thief",
           "Vhailor", "Ignus", "Morte", "Nordom",
           "Human", "Annah", "", "Nameless One", ""
          };
  public static final String[] s_usability20 =
          {"None", "Barbarian", "Bard", "Cleric", "Druid",
           "Fighter", "Monk", "Paladin", "Ranger",
           "Rogue", "Sorcerer", "Wizard", "",
           "Chaotic", "Evil", "Good", "... Neutral",
           "Lawful", "Neutral ...", "", "",
           "", "", "", "Elf",
           "Dwarf", "Half-elf", "Halfling", "Human", "Gnome"
          };
  public static final String[] s_kituse1 =
          {"None", "Cleric of talos", "Cleric of helm", "Cleric of lathander",
           "Totemic druid", "Shapeshifter", "Avenger", "Barbarian", "Wild mage"};
  public static final String[] s_kituse2 =
          {"None", "Stalker", "Beastmaster", "Assassin", "Bounty hunter",
           "Swashbuckler", "Blade", "Jester", "Skald"};
  public static final String[] s_kituse3 =
          {"None", "Diviner", "Enchanter", "Illusionist", "Invoker", "Necromancer", "Transmuter",
           "Generalist", "Archer"};
  public static final String[] s_kituse4 =
          {"None", "Berserker", "Wizard slayer", "Kensai", "Cavalier", "Inquisitor",
           "Undead hunter", "Abjurer", "Conjurer"};
  public static final String[] s_tag = {"  ", "2A", "3A", "4A", "2W", "3W", "4W", "AX", "BW",
                                        "CB", "CL", "D1", "D2", "D3", "D4", "DD", "FL", "FS",
                                        "H0", "H1", "H2", "H3", "H4", "H5", "H6", "HB", "MC",
                                        "MS", "QS", "S1", "S2", "S3", "SC", "SL", "SP", "SS", "WH"};
  public static final String[] s_anim =
          {"None", "Leather armor", "Chain mail", "Plate mail",
           "Mage robe 1", "Mage robe 2", "Mage robe 3",
           "Battle axe", "Bow", "Crossbow", "Club",
           "Buckler", "Small shield", "Medium shield", "Large shield",
           "Dagger", "Flail", "Flaming sword",
           "Helmet 1", "Helmet 2", "Helmet 3", "Helmet 4", "Helmet 5", "Helmet 6", "Helmet 7",
           "Halberd", "Mace", "Morning star", "Quarterstaff",
           "Long sword", "Two-handed sword", "Katana", "Scimitar",
           "Sling", "Spear", "Short sword", "War hammer"
          };
  public static final String[] s_tag11 = {"  ", "AX", "CB", "CL", "DD", "S1", "WH"};
  public static final String[] s_anim11 = {"None", "Axe", "Crossbow", "Club", "Dagger",
                                           "Sword", "Hammer"};
  public static final String[] s_tag_1pp = {"  ", "2A", "3A", "4A", "2W", "3W", "4W", "AX",
                                            "BS", "BW", "C0", "C1", "C2", "C3", "C4", "C5",
                                            "C6", "C7", "CB", "CL", "D1", "D2", "D3", "D4",
                                            "DD", "F0", "F1", "F2", "F3",
                                            "FL", "FS", "GS",
                                            "H0", "H1", "H2", "H3", "H4", "H5", "H6", "HB",
                                            "J0", "J1", "J2", "J3", "J4", "J5", "J6", "J7",
                                            "J8", "J9", "JA", "JB", "JC", "M2", "MC",
                                            "MS", "Q2", "Q3", "Q4",
                                            "QS", "S0", "S1", "S2", "S3", "SC", "SL", "SP",
                                            "SS", "WH", "YW", "ZW"};
  public static final String[] s_anim_1pp =
          {"None", "Leather armor", "Chain mail", "Plate mail",
           "Mage robe 1", "Mage robe 2", "Mage robe 3",
           "Battle axe", "Bow?", "Bow",
           "Small shield (alternate 1)", "Medium shield (alternate 1)", "Large shield (alternate 1)",
           "Medium shield (alternate 2)", "Small shield (alternate 2)", "Large shield (alternate 2)",
           "Large shield (alternate 3)", "Medium shield (alternate 3)",
           "Crossbow", "Club",
           "Buckler", "Small shield", "Medium shield", "Large shield",
           "Dagger",
           "Flail (alternate 1)", "Flail (alternate 2)", "Flaming sword (blue)", "Flail (alternate 3",
           "Flail", "Flaming sword", "Glowing staff",
           "Helmet 1", "Helmet 2", "Helmet 3", "Helmet 4", "Helmet 5", "Helmet 6", "Helmet 7",
           "Halberd",
           "Helmet 8", "Helmet 9", "Helmet 10", "Helmet 11", "Helmet 12", "Helmet 13", "Helmet 14",
           "Helmet 15", "Helmet 16", "Helmet 17", "Helmet 18", "Circlet", "Helmet 20",
           "Mace (alternate)", "Mace", "Morning star",
           "Quarterstaff (alternate 1)", "Quarterstaff (alternate 2)", "Quarterstaff (alternate 3)",
           "Quarterstaff", "Bastard sword", "Long sword", "Two-handed sword", "Katana", "Scimitar",
           "Sling", "Spear", "Short sword", "War hammer", "Wings?", "Feathered wings"
          };

  public static String getSearchString(byte buffer[])
  {
//    return new StringRef(buffer, 12, "").toString();
    String name = new StringRef(buffer, 12, "").toString().trim();
    if (name.equals("") || name.equalsIgnoreCase("No such index"))
      return new StringRef(buffer, 8, "").toString().trim();
    return name;
  }

  public ItmResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Ability(), new Effect()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewTabCount()
  {
    return 1;
  }

  @Override
  public String getViewTabName(int index)
  {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    JScrollPane scroll = new JScrollPane(new Viewer(this));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

  @Override
  public boolean viewTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.write(os);
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Ability) {
        Ability a = (Ability)o;
        a.writeEffects(os);
      }
    }
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    if (datatype instanceof Effect) {
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability)
          ((Ability)o).incEffectsIndex(1);
      }
    }
    else if (datatype instanceof Ability) {
      int effect_count = ((SectionCount)getAttribute("# global effects")).getValue();
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability) {
          Ability ability = (Ability)o;
          ability.setEffectsIndex(effect_count);
          effect_count += ability.getEffectsCount();
        }
      }
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (child instanceof Ability && datatype instanceof Effect) {
      int index = getIndexOf(child) + 1;
      while (index < getRowCount()) {
        StructEntry se = getStructEntryAt(index++);
        if (se instanceof Ability)
          ((Ability)se).incEffectsIndex(1);
      }
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (datatype instanceof Effect) {
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability)
          ((Ability)o).incEffectsIndex(-1);
      }
    }
    else if (datatype instanceof Ability) {
      int effect_count = ((SectionCount)getAttribute("# global effects")).getValue();
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability) {
          Ability ability = (Ability)o;
          ability.setEffectsIndex(effect_count);
          effect_count += ability.getEffectsCount();
        }
      }
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (child instanceof Ability && datatype instanceof Effect) {
      int index = getIndexOf(child) + 1;
      while (index < getRowCount()) {
        StructEntry se = getStructEntryAt(index++);
        if (se instanceof Ability)
          ((Ability)se).incEffectsIndex(-1);
      }
    }
  }

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, 0, 4, "Signature"));
    TextString version = new TextString(buffer, 4, 4, "Version");
    list.add(version);
    list.add(new StringRef(buffer, 8, "General name"));
    list.add(new StringRef(buffer, 12, "Identified name"));
    if (version.toString().equalsIgnoreCase("V1.1")) {
      list.add(new ResourceRef(buffer, 16, "Drop sound", "WAV"));
      list.add(new Flag(buffer, 24, 4, "Flags", s_flags11));
      list.add(new Bitmap(buffer, 28, 2, "Category", s_categories11));
      list.add(new Flag(buffer, 30, 4, "Unusable by", s_usability11));
      list.add(new TextBitmap(buffer, 34, 2, "Equipped appearance", s_tag11, s_anim11));
//      list.add(new Bitmap(buffer, 36, 1, "Disable paper doll?", new String[]{"No", "Yes"}));
//      list.add(new Unknown(buffer, 37, 15));
    }
    else {
      list.add(new ResourceRef(buffer, 16, "Used up item", "ITM"));
      list.add(new Flag(buffer, 24, 4, "Flags", s_flags));
      list.add(new Bitmap(buffer, 28, 2, "Category", s_categories));
      if (version.toString().equalsIgnoreCase("V2.0"))
        list.add(new Flag(buffer, 30, 4, "Unusable by", s_usability20));
      else
        list.add(new Flag(buffer, 30, 4, "Unusable by", s_usability));
      if (ResourceFactory.isEnhancedEdition()) {
        list.add(new TextBitmap(buffer, 34, 2, "Equipped appearance", s_tag_1pp, s_anim_1pp));
      }
      else {
        list.add(new TextBitmap(buffer, 34, 2, "Equipped appearance", s_tag, s_anim));
      }
    }
    list.add(new DecNumber(buffer, 36, 2, "Minimum level"));
    list.add(new DecNumber(buffer, 38, 2, "Minimum strength"));
//    list.add(new Unknown(buffer, 39, 1));
    if (ResourceFactory.getInstance().resourceExists("KIT.IDS")) {
      list.add(new DecNumber(buffer, 40, 1, "Minimum strength bonus"));
      list.add(new Flag(buffer, 41, 1, "Unusable by (1/4)", s_kituse1));
      list.add(new DecNumber(buffer, 42, 1, "Minimum intelligence"));
      list.add(new Flag(buffer, 43, 1, "Unusable by (2/4)", s_kituse2));
      list.add(new DecNumber(buffer, 44, 1, "Minimum dexterity"));
      list.add(new Flag(buffer, 45, 1, "Unusable by (3/4)", s_kituse3));
      list.add(new DecNumber(buffer, 46, 1, "Minimum wisdom"));
      list.add(new Flag(buffer, 47, 1, "Unusable by (4/4)", s_kituse4));
      list.add(new DecNumber(buffer, 48, 1, "Minimum constitution"));
      if (ResourceFactory.getInstance().resourceExists("PROFTYPE.IDS"))
        list.add(new IdsBitmap(buffer, 49, 1, "Weapon proficiency", "PROFTYPE.IDS"));
      else
        list.add(new IdsBitmap(buffer, 49, 1, "Weapon proficiency", "STATS.IDS"));
    }
    else {
      list.add(new DecNumber(buffer, 40, 2, "Minimum strength bonus"));
      list.add(new DecNumber(buffer, 42, 2, "Minimum intelligence"));
      list.add(new DecNumber(buffer, 44, 2, "Minimum dexterity"));
      list.add(new DecNumber(buffer, 46, 2, "Minimum wisdom"));
      list.add(new DecNumber(buffer, 48, 2, "Minimum constitution"));
//      list.add(new Unknown(buffer, 41, 1));
//      list.add(new Unknown(buffer, 43, 1));
//      list.add(new Unknown(buffer, 45, 1));
//      list.add(new Unknown(buffer, 47, 1));
//      list.add(new Unknown(buffer, 49, 1));
    }
    list.add(new DecNumber(buffer, 50, 2, "Minimum charisma"));
//    list.add(new Unknown(buffer, 51, 1));
    list.add(new DecNumber(buffer, 52, 4, "Price"));
    list.add(new DecNumber(buffer, 56, 2, "Maximum in stack"));
    list.add(new ResourceRef(buffer, 58, "Icon", "BAM"));
    list.add(new DecNumber(buffer, 66, 2, "Lore to identify"));
    list.add(new ResourceRef(buffer, 68, "Ground icon", "BAM"));
    list.add(new DecNumber(buffer, 76, 4, "Weight"));
    list.add(new StringRef(buffer, 80, "General description"));
    list.add(new StringRef(buffer, 84, "Identified description"));
    if (version.toString().equalsIgnoreCase("V1.1")) {
      list.add(new ResourceRef(buffer, 88, "Pick up sound", "WAV"));
    } else {
      if (ResourceFactory.isEnhancedEdition()) {
        list.add(new ResourceRef(buffer, 88, "Description image", new String[]{"BAM", "BMP"}));
      } else {
        list.add(new ResourceRef(buffer, 88, "Description image", "BAM"));
      }
    }
    list.add(new DecNumber(buffer, 96, 4, "Enchantment"));
    SectionOffset abil_offset = new SectionOffset(buffer, 100, "Abilities offset",
                                                  Ability.class);
    list.add(abil_offset);
    SectionCount abil_count = new SectionCount(buffer, 104, 2, "# abilities",
                                               Ability.class);
    list.add(abil_count);
    SectionOffset global_offset = new SectionOffset(buffer, 106, "Effects offset",
                                                    Effect.class);
    list.add(global_offset);
    list.add(new DecNumber(buffer, 110, 2, "First effect index"));
    SectionCount global_count = new SectionCount(buffer, 112, 2, "# global effects",
                                                 Effect.class);
    list.add(global_count);

    if (version.toString().equalsIgnoreCase("V1.1")) {
      list.add(new ResourceRef(buffer, 114, "Dialogue", "DLG"));
      list.add(new StringRef(buffer, 122, "Speaker name"));
      list.add(new IdsBitmap(buffer, 126, 2, "Weapon color", "CLOWNCLR.IDS"));
      list.add(new Unknown(buffer, 128, 26));
    }
    else if (version.toString().equalsIgnoreCase("V2.0")) {
      list.add(new Unknown(buffer, 114, 16));
    }

    offset = abil_offset.getValue();
    Ability abilities[] = new Ability[abil_count.getValue()];
    for (int i = 0; i < abilities.length; i++) {
      abilities[i] = new Ability(this, buffer, offset, i);
      offset = abilities[i].getEndOffset();
      list.add(abilities[i]);
    }

    int offset2 = global_offset.getValue();
    for (int i = 0; i < global_count.getValue(); i++) {
      Effect eff = new Effect(this, buffer, offset2, i);
      offset2 = eff.getEndOffset();
      list.add(eff);
    }

    for (final Ability ability : abilities)
      offset2 = ability.readEffects(buffer, offset2);

    return Math.max(offset, offset2);
  }


  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        ItmResource itm = new ItmResource(entry);
        Ability[] abilities;
        Effect[][] abilityEffects;
        Effect[] effects;
        boolean retVal = true;
        String key;
        Object o;

        // preparing substructures
        DecNumber ofs = (DecNumber)itm.getAttribute("Effects offset");
        DecNumber cnt = (DecNumber)itm.getAttribute("# global effects");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          effects = new Effect[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.ITM_Effect), idx);
            effects[idx] = (Effect)itm.getAttribute(label);
          }
        } else {
          effects = new Effect[0];
        }

        ofs = (DecNumber)itm.getAttribute("Abilities offset");
        cnt = (DecNumber)itm.getAttribute("# abilities");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          abilities = new Ability[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.ITM_Ability), idx);
            abilities[idx] = (Ability)itm.getAttribute(label);
          }
        } else {
          abilities = new Ability[0];
        }

        abilityEffects = new Effect[abilities.length][];
        for (int idx = 0; idx < abilities.length; idx++) {
          if (abilities[idx] != null) {
            cnt = (DecNumber)abilities[idx].getAttribute("# effects");
            if (cnt != null && cnt.getValue() > 0) {
              abilityEffects[idx] = new Effect[cnt.getValue()];
              for (int idx2 = 0; idx2 < cnt.getValue(); idx2++) {
                String label = String.format(SearchOptions.getResourceName(SearchOptions.ITM_Ability_Effect), idx2);
                abilityEffects[idx][idx2] = (Effect)abilities[idx].getAttribute(label);
              }
            } else {
              abilityEffects[idx] = new Effect[0];
            }
          } else {
            abilityEffects[idx] = new Effect[0];
          }
        }

        // checking options
        if (retVal) {
          key = SearchOptions.ITM_Name;
          o = searchOptions.getOption(key);
          StructEntry struct = itm.getAttribute(SearchOptions.getResourceName(key));
          retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
        }

        if (retVal) {
          key = SearchOptions.ITM_Appearance;
          o = searchOptions.getOption(key);
          StructEntry struct = itm.getAttribute(SearchOptions.getResourceName(key));
          retVal &= SearchOptions.Utils.matchString(struct, o, true, true);
        }

        String[] keyList = new String[]{SearchOptions.ITM_Flags, SearchOptions.ITM_Unusable,
                                        SearchOptions.ITM_KitsUnusable1, SearchOptions.ITM_KitsUnusable2,
                                        SearchOptions.ITM_KitsUnusable3, SearchOptions.ITM_KitsUnusable4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = itm.getAttribute(SearchOptions.getResourceName(key));
            retVal &= SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.ITM_Category, SearchOptions.ITM_Price,
                               SearchOptions.ITM_Enchantment, SearchOptions.ITM_MinLevel,
                               SearchOptions.ITM_MinSTR, SearchOptions.ITM_MinSTRExtra,
                               SearchOptions.ITM_MinCON, SearchOptions.ITM_MinDEX,
                               SearchOptions.ITM_MinINT, SearchOptions.ITM_MinWIS,
                               SearchOptions.ITM_MinCHA};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = itm.getAttribute(SearchOptions.getResourceName(key));
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.ITM_Effect_Type1, SearchOptions.ITM_Effect_Type2,
                               SearchOptions.ITM_Effect_Type3};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            boolean found = false;
            key = keyList[idx];
            o = searchOptions.getOption(key);
            for (int idx2 = 0; idx2 < effects.length; idx2++) {
              if (!found) {
                if (effects[idx2] != null) {
                  StructEntry struct = effects[idx2].getAttribute(SearchOptions.getResourceName(key));
                  found |= SearchOptions.Utils.matchNumber(struct, o);
                }
              } else {
                break;
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        SearchOptions abilityOption = (SearchOptions)searchOptions.getOption(SearchOptions.ITM_Ability);
        if (retVal && abilityOption != null) {
          // indicates whether any ability options have been selected
          boolean hasAbilityOptions = false;
          keyList = new String[]{SearchOptions.ITM_Ability_Type, SearchOptions.ITM_Ability_Target,
                                 SearchOptions.ITM_Ability_Range, SearchOptions.ITM_Ability_Launcher,
                                 SearchOptions.ITM_Ability_Speed, SearchOptions.ITM_Ability_DiceCount,
                                 SearchOptions.ITM_Ability_DiceSize, SearchOptions.ITM_Ability_Charges,
                                 SearchOptions.ITM_Ability_DamageType,
                                 SearchOptions.ITM_Ability_Projectile,
                                 SearchOptions.ITM_Ability_Flags,
                                 SearchOptions.ITM_Ability_Effect_Type1, SearchOptions.ITM_Ability_Effect_Type2,
                                 SearchOptions.ITM_Ability_Effect_Type3};
          for (int i = 0; i < keyList.length; i++) {
            hasAbilityOptions |= (abilityOption.getOption(keyList[i]) != null);
          }

          // tracks matches for each option in every available ability
          final int abilityOptions = keyList.length;    // number of supported spell ability options
          boolean[][] abilityMatches = new boolean[abilities.length][abilityOptions];
          for (int i = 0; i < abilities.length; i++) {
            for (int j = 0; j < abilityMatches[i].length; j++) {
              abilityMatches[i][j] = false;
            }
          }

          for (int i = 0; i < abilities.length; i++) {
            if (abilities[i] != null) {
              for (int j = 0; j < 10; j++) {
                key = keyList[j];
                o = abilityOption.getOption(key);
                StructEntry struct = abilities[i].getAttribute(SearchOptions.getResourceName(key));
                abilityMatches[i][j] = SearchOptions.Utils.matchNumber(struct, o);
              }

              {
                key = keyList[10];
                o = abilityOption.getOption(key);
                StructEntry struct = abilities[i].getAttribute(SearchOptions.getResourceName(key));
                abilityMatches[i][10] = SearchOptions.Utils.matchFlags(struct, o);
              }

              for (int j = 11; j < keyList.length; j++) {
                key = keyList[j];
                o = abilityOption.getOption(key);
                for (int k = 0; k < abilityEffects[i].length; k++) {
                  if (abilityEffects[i][k] != null) {
                    StructEntry struct = abilityEffects[i][k].getAttribute(SearchOptions.getResourceName(key));
                    abilityMatches[i][j] |= SearchOptions.Utils.matchNumber(struct, o);
                  }
                }
              }
            }
          }

          // evaluating collected results
          boolean[] foundSingle = new boolean[abilityMatches.length];    // for single ability option
          for (int i = 0; i < foundSingle.length; i++)
          {
            foundSingle[i] = false;
          }
          boolean[] foundMulti = new boolean[abilityOptions];           // for multiple abilities option
          for (int i = 0; i < foundMulti.length; i++)
          {
            foundMulti[i] = (abilityOption.getOption(keyList[i]) == null);
          }

          for (int i = 0; i < abilityMatches.length; i++) {
            if (abilities[i] != null) {
              foundSingle[i] = true;
              for (int j = 0; j < abilityMatches[i].length; j++) {
                foundSingle[i] &= abilityMatches[i][j];
                foundMulti[j] |= abilityMatches[i][j];
              }
            }
          }

          boolean resultSingle = false;
          for (int i = 0; i < foundSingle.length; i++) { resultSingle |= foundSingle[i]; }
          resultSingle |= !hasAbilityOptions;

          boolean resultMulti = true;
          for (int i = 0; i < foundMulti.length; i++) { resultMulti &= foundMulti[i]; }
          resultMulti |= !hasAbilityOptions;

          Boolean isAbilitySingle;
          o = abilityOption.getOption(SearchOptions.ITM_Ability_MatchSingle);
          if (o != null && o instanceof Boolean) {
            isAbilitySingle = (Boolean)o;
          } else {
            isAbilitySingle = false;
          }

          if (isAbilitySingle) {
            retVal &= resultSingle;
          } else {
            retVal &= resultMulti;
          }
        }

        keyList = new String[]{SearchOptions.ITM_Custom1, SearchOptions.ITM_Custom2,
                               SearchOptions.ITM_Custom3, SearchOptions.ITM_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(itm, o);
          } else {
            break;
          }
        }


        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}

