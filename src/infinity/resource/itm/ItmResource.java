// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.itm;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public final class ItmResource extends AbstractStruct implements Resource, HasAddRemovable, HasDetailViewer
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
  private static final String[] s_flags =
          {"None", "Unsellable", "Two-handed", "Droppable", "Displayable",
           "Cursed", "Not copyable", "Magical", "Left-handed", "Silver", "Cold iron", "Off-handed", "Conversable"};
  private static final String[] s_flags11 =
          {"None", "Unsellable", "Two-handed", "Droppable", "Displayable",
           "Cursed", "Not copyable", "Magical", "Left-handed", "Silver", "Cold iron", "Steel", "Conversable",
           "Pulsating"};
  private static final String[] s_usability =
          {"None", "Chaotic", "Evil", "Good", "... Neutral", "Lawful",
           "Neutral ...", "Bard", "Cleric", "Cleric-Mage",
           "Cleric-Thief", "Cleric-Ranger", "Fighter", "Fighter-Druid",
           "Fighter-Mage", "Fighter-Cleric", "Fighter-Mage-Cleric",
           "Fighter-Mage-Thief", "Fighter-Thief", "Mage", "Mage-Thief",
           "Paladin", "Ranger", "Thief", "Elf", "Dwarf", "Half-Elf",
           "Halfling", "Human", "Gnome", "Monk", "Druid"};
  private static final String[] s_usability11 =
          {"None", "Chaotic", "Evil", "Good",
           "... Neutral", "Lawful", "Neutral ...", "Sensate",
           "Priest", "Godsman", "Anarchist", "Xaositect",
           "Fighter", "Non-aligned", "Fighter-Mage", "Dustman",
           "Mercykiller", "Indep", "Figher-Thief", "Mage",
           "Mage-Thief", "Dak'kon", "Fall-From-Grace", "Thief",
           "Vhailor", "Ignus", "Morte", "Nordom",
           "Human", "Annah", "", "Nameless One", ""
          };
  private static final String[] s_usability20 =
          {"None", "Barbarian", "Bard", "Cleric", "Druid",
           "Fighter", "Monk", "Paladin", "Ranger",
           "Rogue", "Sorcerer", "Wizard", "",
           "Chaotic", "Evil", "Good", "... Neutral",
           "Lawful", "Neutral ...", "", "",
           "", "", "", "Elf",
           "Dwarf", "Half-elf", "Halfling", "Human", "Gnome"
          };
  private static final String[] s_kituse1 =
          {"None", "Cleric of talos", "Cleric of helm", "Cleric of lathander",
           "Totemic druid", "Shapeshifter", "Avenger", "Barbarian", "Wild mage"};
  private static final String[] s_kituse2 =
          {"None", "Stalker", "Beastmaster", "Assassin", "Bounty hunter",
           "Swashbuckler", "Blade", "Jester", "Skald"};
  private static final String[] s_kituse3 =
          {"None", "Diviner", "Enchanter", "Illusionist", "Invoker", "Necromancer", "Transmuter",
           "Generalist", "Archer"};
  private static final String[] s_kituse4 =
          {"None", "Berserker", "Wizard slayer", "Kensai", "Cavalier", "Inquisitor",
           "Undead hunter", "Abjurer", "Conjurer"};
  private static final String[] s_tag = {"  ", "2A", "3A", "4A", "2W", "3W", "4W", "AX", "BW",
                                         "CB", "CL", "D1", "D2", "D3", "D4", "DD", "FL", "FS",
                                         "H0", "H1", "H2", "H3", "H4", "H5", "H6", "HB", "MC",
                                         "MS", "QS", "S1", "S2", "S3", "SC", "SL", "SP", "SS", "WH"};
  private static final String[] s_anim =
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

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Ability(), new Effect()};
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
      list.add(new TextBitmap(buffer, 34, 2, "Equipped appearance",
                              new String[]{"  ", "AX", "CB", "CL", "DD", "S1", "WH"},
                              new String[]{"None", "Axe", "Crossbow", "Club", "Dagger",
                                           "Sword", "Hammer"}));
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
      list.add(new TextBitmap(buffer, 34, 2, "Equipped appearance", s_tag, s_anim));
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
    if (version.toString().equalsIgnoreCase("V1.1"))
      list.add(new ResourceRef(buffer, 88, "Pick up sound", "WAV"));
    else
      list.add(new ResourceRef(buffer, 88, "Description image", "BAM"));
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
      abilities[i] = new Ability(this, buffer, offset);
      offset = abilities[i].getEndOffset();
      list.add(abilities[i]);
    }

    int offset2 = global_offset.getValue();
    for (int i = 0; i < global_count.getValue(); i++) {
      Effect eff = new Effect(this, buffer, offset2);
      offset2 = eff.getEndOffset();
      list.add(eff);
    }

    for (final Ability ability : abilities)
      offset2 = ability.readEffects(buffer, offset2);

    return Math.max(offset, offset2);
  }
}

