// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.*;
import infinity.resource.are.Actor;
import infinity.resource.spl.SplResource;
import infinity.util.LongIntegerHashMap;

import java.util.List;

public final class EffectFactory
{
  private static EffectFactory efactory;
  private final String s_poricon[];
  private String s_effname[];

  private static final LongIntegerHashMap<String> m_colorloc = new LongIntegerHashMap<String>();
  private static final String s_inctype[] = {"Increment", "Set", "Set % of"};
  private static final String s_yesno[] = {"Yes", "No"};
  private static final String s_noyes[] = {"No", "Yes"};

  private static final String s_visuals[] = {"None", "Hit abjuration",
    "Hit alteration", "Hit invocation", "Hit necromancy", "Hit conjuration",
    "Hit enchantment", "Hit illusion", "Hit divination", "Armor",
    "Spirit armor", "Ghost armor", "Strength", "Confusion", "Shroud of flame",
    "Death spell", "Disintegrate", "Power word, silence", "Power word, stun",
    "Finger of death", "Mordenkainen's sword", "Monster summoning 1",
    "Monster summoning 2", "Monster summoning 3", "Monster summoning 4",
    "Monster summoning 5", "Monster summoning 6", "Monster summoning 7",
    "Conjure fire elemental", "Conjure earth elemental",
    "Conjure water elemental", "Bless", "Curse", "Prayer", "Recitation",
    "Cure light wounds", "Cure moderate wounds", "Cure serious wounds",
    "Cure critical wounds", "Heal", "Animal summoning 1", "Animal summoning 2",
    "Animal summoning 3", "Slow poison", "Neutralize poison", "Call lightning",
    "Static charge", "Remove paralysis", "Free action", "Miscast magic",
    "Strength of one", "Champion's strength", "Flame strike", "Raise dead",
    "Resurrection", "Chaotic commands", "Righteous wrath of the faithful",
    "Sunray", "Spike stones", "Dimension door departure",
    "Dimension door arrival", "Cone of cold", "Sol's searing orb",
    "Hit fire", "Hit cold", "Hit electricity", "Hit acid", "Hit paralysis",
    "Malavon's rage", "Righteous wrath of the faithful ground",
    "Belhifet death", "Portal", "Sunscorch", "Blade barrier (front)",
    "Blade barrier (back)", "Circle of bones (front)", "Circle of bones (back)",
    "Cause light wounds", "Cause moderate wounds", "Cause serious wounds",
    "Cause critical wounds", "Cause disease", "Hit poison", "Slay living",
    "Harm", "Destruction", "Exaltation", "Cloudburst", "Mold touch",
    "Lower resistance", "Cat's grace", "Soul eater", "Smashing wave",
    "Suffocate", "Abi-dalzim's horrid wilting", "Mordenkainen's force missiles",
    "Vitriolic sphere", "Wailing virgin death", "Undead ward",
    "Wailing virgin hit", "Wylfden's death 1", "Wylfden's death 2",
    "Dragon's death 1", "Dragon's death 2", "Monster summoning circle",
    "Animal summoning circle", "Earth summoning circle",
    "Fire summoning circle", "Water summoning circle", "Gedlee's electric loop",
    "Darktree attack"};
  private static final String s_actype[] = {"All weapons", "Crushing weapons",
    "Missile weapons", "Piercing weapons", "Slashing weapons",
    "Set base AC to value"};
  private static final String s_button[] = {"Stealth", "Thieving",
    "Spell select", "Quick spell 1", "Quick spell 2", "Quick spell 3",
    "Turn undead", "Talk", "Use item", "Quick item 1", "", "Quick item 2",
    "Quick item 3", "Special abilities"};
  private static final String s_school[] = {"None", "Abjuration",
    "Conjuration", "Divination", "Enchantment", "Illusion", "Evocation",
    "Necromancy", "Alteration", "Generalist"};

  static {
    m_colorloc.put(0L, "Belt/Amulet");
    m_colorloc.put(1L, "Minor color");
    m_colorloc.put(2L, "Major color");
    m_colorloc.put(3L, "Skin color");
    m_colorloc.put(4L, "Strap/Leather");
    m_colorloc.put(5L, "Armor/Trimming");
    m_colorloc.put(6L, "Hair");
    m_colorloc.put(16L, "Weapon head/blade/staff major");
    m_colorloc.put(20L, "Weapon grip/staff minor");
    m_colorloc.put(21L, "Weapon head/blade minor");
    m_colorloc.put(32L, "Shield hub");
    m_colorloc.put(33L, "Shield interior");
    m_colorloc.put(34L, "Shield panel");
    m_colorloc.put(35L, "Helm");
    m_colorloc.put(36L, "Shield grip");
    m_colorloc.put(37L, "Shield body/trim");
    m_colorloc.put(48L, "Helm wings");
    m_colorloc.put(49L, "Helm detail");
    m_colorloc.put(50L, "Helm plume");
    m_colorloc.put(52L, "Helm face");
    m_colorloc.put(53L, "Helm exterior");
    m_colorloc.put(255L, "Character color");
  }

  public static EffectFactory getFactory()
  {
    if (efactory == null)
      efactory = new EffectFactory();
    return efactory;
  }

  public static void init()
  {
    efactory = null;
  }

  private EffectFactory()
  {
    s_effname = null;
    switch (ResourceFactory.getGameID()) {
      case ResourceFactory.ID_BG1:
      case ResourceFactory.ID_BG1TOTSC:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set color", "Set color glow solid",
          "Set color glow pulse", "Constitution bonus", "Cure poison", "Damage",
          "Kill target", "Defrost", "Dexterity bonus", "Haste",
          "Current HP bonus", "Maximum HP bonus", "Intelligence bonus",
          "Invisibility", "Lore bonus", "Luck bonus", "Reset morale", "Panic",
          "Poison", "Remove curse", "Acid resistance bonus",
          "Cold resistance bonus", "Electricity resistance bonus",
          "Fire resistance bonus", "Magic damage resistance bonus",
          "Raise dead", "Save vs. death bonus", "Save vs. wand bonus",
          "Save vs. polymorph bonus", "Save vs. breath bonus",
          "Save vs. spell bonus", "Silence", "Sleep", "Slow", "Sparkle",
          "Bonus wizard spells", "Stone to flesh", "Strength bonus", "Stun",
          "Cure stun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucency", "Summon creature", "Unsummon creature",
          "Nondetection", "Remove nondetection", "Change gender",
          "Change AI type", "Attack damage bonus", "Blindness",
          "Cure blindness", "Feeblemindedness", "Cure feeblemindedness",
          "Disease", "Cure disease", "Deafness", "Cure deafness",
          "Set AI script", "Immunity to projectile",
          "Magical fire resistance bonus", "Magical cold resistance bonus",
          "Slashing resistance bonus", "Crushing resistance bonus",
          "Piercing resistance bonus", "Missile resistance bonus",
          "Open locks bonus", "Find traps bonus", "Pick pockets bonus",
          "Fatigue bonus", "Intoxication bonus", "Tracking bonus",
          "Change level", "Exceptional strength bonus", "Regeneration",
          "Modify duration", "Protection from creature type",
          "Immunity to effect", "Immunity to spell level", "Change name",
          "XP bonus", "Remove gold", "Morale break", "Change portrait",
          "Reputation bonus", "Paralyze", "Retreat from", "Create weapon",
          "Remove item", "Equip weapon", "Dither", "Detect alignment",
          "Detect invisible", "Clairvoyance", "Show creatures", "Mirror image",
          "Immunity to weapons", "Visual animation effect",
          "Create inventory item", "Remove inventory item", "Teleport",
          "Unlock", "Movement rate bonus", "Summon monsters", "Confusion",
          "Aid (non-cumulative)", "Bless (non-cumulative)",
          "Chant (non-cumulative)", "Draw upon holy might (non-cumulative)",
          "Luck (non-cumulative)", "Petrification", "Polymorph",
          "Force visible", "Bad chant (non-cumulative)",
          "Set animation sequence", "Display string", "Casting glow",
          "Lighting effects", "Display portrait icon", "Create item in slot",
          "Disable button", "Disable spellcasting", "Cast spell", "Learn spell",
          "Cast spell at point", "Identify", "Find traps", "Replace self",
          "Play movie", "Sanctuary", "Entangle overlay", "Minor globe overlay",
          "Protection from normal missiles overlay", "Web effect",
          "Grease overlay", "Mirror image effect", "Remove sanctuary",
          "Remove fear", "Remove paralysis", "Free action",
          "Remove intoxication", "Pause caster", "Magic resistance bonus",
          "Missile THAC0 bonus", "Remove creature", "Prevent portrait icon",
          "Play damage animation", "Give innate ability", "Remove spell",
          "Poison resistance bonus", "Play sound", "Hold creature",
          "Movement rate bonus 2", "Use EFF file", "THAC0 vs. type bonus",
          "Damage vs. type bonus", "Disallow item", "Disallow item type",
          "Apply effect on equip item", "Apply effect on equip type",
          "No collision detection", "Hold creature 2", "Move creature",
          "Set local variable", "Increase spells cast per round",
          "Increase casting speed factor", "Increase attack speed factor",
          "Casting level bonus"};
        s_poricon = new String[]{"Charm", "Dire charm", "Rigid thinking",
          "Confused", "Berserk", "Intoxicated", "Poisoned", "Nauseated",
          "Blind", "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Magic armor", "Held", "Sleep",
          "Shielded", "Protection from fire", "Blessed", "Chant", "Free action",
          "Barkskin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Nondetection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Domination",
          "Hopelessness", "Greater malison", "Spirit armor", "Chaos",
          "Feeblemind", "Defensive harmony", "Champion's strength", "Dying",
          "Mind shield", "Energy drain", "Polymorph self", "Stun",
          "Regeneration", "Perception", "Master thievery"};
        break;

      case ResourceFactory.ID_TORMENT:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set color", "Set color glow solid",
          "Set color glow pulse", "Constitution bonus", "Cure poison", "Damage",
          "Kill target", "Defrost", "Dexterity bonus", "Haste",
          "Current HP bonus", "Maximum HP bonus", "Intelligence bonus",
          "Invisibility", "Lore bonus", "Luck bonus", "Reset morale", "Panic",
          "Poison", "Remove curse", "Acid resistance bonus",
          "Cold resistance bonus", "Electricity resistance bonus",
          "Fire resistance bonus", "Magic damage resistance bonus",
          "Raise dead", "Save vs. death bonus", "Save vs. wand bonus",
          "Save vs. polymorph bonus", "Save vs. breath bonus",
          "Save vs. spell bonus", "Silence", "Sleep", "Slow", "Sparkle",
          "Bonus wizard spells", "Stone to flesh", "Strength bonus", "Stun",
          "Cure stun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucency", "Summon creature", "Unsummon creature",
          "Nondetection", "Remove nondetection", "Change gender",
          "Change AI type", "Attack damage bonus", "Blindness",
          "Cure blindness", "Feeblemindedness", "Cure feeblemindedness",
          "Disease", "Cure disease", "Deafness", "Cure deafness",
          "Rush morte", "Immunity to projectile",
          "Magical fire resistance bonus", "Magical cold resistance bonus",
          "Slashing resistance bonus", "Crushing resistance bonus",
          "Piercing resistance bonus", "Missile resistance bonus",
          "Open locks bonus", "Find traps bonus", "Pick pockets bonus",
          "Fatigue bonus", "Intoxication bonus", "Tracking bonus",
          "Change level", "Exceptional strength bonus", "Regeneration",
          "Modify duration", "Protection from creature type",
          "Immunity to effect", "Immunity to spell level", "Change name",
          "XP bonus", "Remove gold", "Morale break", "Change portrait",
          "Reputation bonus", "Paralyze", "Retreat from", "Create weapon",
          "Remove item", "Equip weapon", "Dither", "Detect alignment",
          "Detect invisible", "Clairvoyance", "Show creatures", "Mirror image",
          "Immunity to weapons", "Visual animation effect",
          "Create inventory item", "Remove inventory item", "Teleport",
          "Unlock", "Movement rate bonus", "Summon monsters", "Confusion",
          "Aid (non-cumulative)", "Bless (non-cumulative)",
          "Chant (non-cumulative)", "Draw upon holy might (non-cumulative)",
          "Luck (non-cumulative)", "Petrification", "Polymorph",
          "Force visible", "Bad chant (non-cumulative)",
          "Set animation sequence", "Display string", "Casting glow",
          "Lighting effects", "Display portrait icon", "Create item in slot",
          "Disable button", "Disable spellcasting", "Cast spell", "Learn spell",
          "Cast spell at point", "Identify", "Find traps", "Replace self",
          "Play movie", "Sanctuary", "Entangle overlay", "Minor globe overlay",
          "Protection from normal missiles overlay", "Web effect",
          "Grease overlay", "Mirror image effect", "Remove sanctuary",
          "Remove fear", "Remove paralysis", "Free action",
          "Remove intoxication", "Pause caster", "Magic resistance bonus",
          "Missile THAC0 bonus", "Remove creature", "Prevent portrait icon",
          "Play damage animation", "Give innate ability", "Remove spell",
          "Poison resistance bonus", "Play sound", "Hold creature",
          "Movement rate bonus 2", "Use EFF file", "THAC0 vs. type bonus",
          "Damage vs. type bonus", "Disallow item", "Disallow item type",
          "Apply effect on equip item", "Apply effect on equip type",
          "No collision detection", "Hold creature 2", "Set status",
          "Play BAM file (single/dual)", "Play BAM file", "Play BAM file 2",
          "Play BAM file 3", "Play BAM file 4", "Hit point transfer",
          "Shake screen", "Flash screen", "Tint screen", "Special spell hit",
          "Unknown (C5)", "Unknown (C6)", "Unknown (C7)", "Unknown (C8)",
          "Play BAM with effects", "Unknown (CA)", "Curse", "Prayer",
          "Move view to target", "Embalm", "Stop all actions", "Fist of iron",
          "Soul exodus", "Detect evil", "Induce hiccups"};
        s_poricon = new String[]{"Charm", "Dire charm", "Rigid thinking",
          "Confused", "Berserk", "Intoxicated", "Poisoned", "Nauseated",
          "Blind", "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Magic armor", "Held", "Sleep",
          "Shielded", "Protection from fire", "Blessed", "Chant", "Free action",
          "Barkskin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Nondetection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Domination",
          "Hopelessness", "Greater malison", "Spirit armor", "Chaos",
          "Feeblemind", "Defensive harmony", "Champions strength", "Dying",
          "Mind shield", "Level drain", "Polymorph self", "Stun",
          "Regeneration", "Perception", "Master thievery"};
        break;

      case ResourceFactory.ID_ICEWIND:
      case ResourceFactory.ID_ICEWINDHOW:
      case ResourceFactory.ID_ICEWINDHOWTOT:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set color", "Set color glow solid",
          "Set color glow pulse", "Constitution bonus", "Cure poison", "Damage",
          "Kill target", "Defrost", "Dexterity bonus", "Haste",
          "Current HP bonus", "Maximum HP bonus", "Intelligence bonus",
          "Invisibility", "Lore bonus", "Luck bonus", "Morale bonus", "Panic",
          "Poison", "Remove curse", "Acid resistance bonus",
          "Cold resistance bonus", "Electricity resistance bonus",
          "Fire resistance bonus", "Magic damage resistance bonus",
          "Raise dead", "Save vs. death bonus", "Save vs. wand bonus",
          "Save vs. polymorph bonus", "Save vs. breath bonus",
          "Save vs. spell bonus", "Silence", "Sleep", "Slow", "Sparkle",
          "Bonus wizard spells", "Stone to flesh", "Strength bonus", "Stun",
          "Cure stun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucency", "Summon creature", "Unsummon creature",
          "Nondetection", "Remove nondetection", "Change gender",
          "Change AI type", "Attack damage bonus", "Blindness",
          "Cure blindness", "Feeblemindedness", "Cure feeblemindedness",
          "Disease", "Cure disease", "Deafness", "Cure deafness",
          "Set AI script", "Immunity to projectile",
          "Magical fire resistance bonus", "Magical cold resistance bonus",
          "Slashing resistance bonus", "Crushing resistance bonus",
          "Piercing resistance bonus", "Missile resistance bonus",
          "Open locks bonus", "Find traps bonus", "Pick pockets bonus",
          "Fatigue bonus", "Intoxication bonus", "Tracking bonus",
          "Change level", "Exceptional strength bonus", "Regeneration",
          "Modify duration", "Protection from creature type",
          "Immunity to effect", "Immunity to spell level", "Change name",
          "XP bonus", "Remove gold", "Morale break", "Change portrait",
          "Reputation bonus", "Paralyze", "Retreat from", "Create weapon",
          "Remove item", "Equip weapon", "Dither", "Detect alignment",
          "Detect invisible", "Clairvoyance", "Show creatures", "Mirror image",
          "Immunity to weapons", "Visual animation effect",
          "Create inventory item", "Remove inventory item", "Teleport",
          "Unlock", "Movement rate bonus", "Summon monsters", "Confusion",
          "Aid (non-cumulative)", "Bless (non-cumulative)",
          "Chant (non-cumulative)", "Draw upon holy might (non-cumulative)",
          "Luck (non-cumulative)", "Petrification", "Polymorph",
          "Force visible", "Bad chant (non-cumulative)",
          "Set animation sequence", "Display string", "Casting glow",
          "Lighting effects", "Display portrait icon", "Create item in slot",
          "Disable button", "Disable spellcasting", "Cast spell", "Learn spell",
          "Cast spell at point", "Identify", "Find traps", "Replace self",
          "Play movie", "Sanctuary", "Entangle overlay", "Minor globe overlay",
          "Protection from normal missiles overlay", "Web effect",
          "Grease overlay", "Mirror image effect", "Remove sanctuary",
          "Remove fear", "Remove paralysis", "Free action",
          "Remove intoxication", "Pause caster", "Magic resistance bonus",
          "Missile THAC0 bonus", "Remove creature", "Prevent portrait icon",
          "Play damage animation", "Give innate ability", "Remove spell",
          "Poison resistance bonus", "Play sound", "Hold creature",
          "Movement rate bonus 2", "Use EFF file", "THAC0 vs. type bonus",
          "Damage vs. type bonus", "Disallow item", "Disallow item type",
          "Apply effect on equip item", "Apply effect on equip type",
          "No collision detection", "Hold creature 2", "Move creature",
          "Set local variable", "Increase spells cast per round",
          "Increase casting speed factor", "Increase attack speed factor",
          "Casting level bonus", "Find familiar", "Invisibility detection",
          "Ignore dialogue pause", "Drain HP on death", "Familiar",
          "Physical mirror", "Reflect specified effect", "Reflect spell level",
          "Spell turning", "Spell deflection", "Reflect spell school",
          "Reflect spell type", "Protection from spell school",
          "Protection from spell type", "Protection from spell",
          "Reflect specified spell", "Minimum HP", "Power word, kill",
          "Power word, stun", "Imprisonment", "Freedom", "Maze", "Select spell",
          "Play visual effect", "Level drain", "Power word, sleep",
          "Stoneskin effect", "Attack roll penalty",
          "Remove spell school protections", "Remove spell type protections",
          "Teleport field", "Spell school deflection", "Restoration",
          "Detect magic", "Spell type deflection", "Spell school turning",
          "Spell type turning", "Remove protection by school",
          "Remove protection by type", "Time stop", "Creature RGB color fade",
          "Show visual effect", "Snilloc's snowball swarm", "Show casting glow",
          "Chill touch", "Magical stone", "All saving throws bonus",
          "Slow poison", "Summon creature 2", "Vampiric touch",
          "Show visual overlay", "Animate dead", "Prayer", "Bad prayer",
          "Summon creature 3", "Beltyn's burning blood", "Summon shadow",
          "Recitation", "Bad recitation", "Lich touch", "Sol's searing orb",
          "Bonus AC vs. weapons", "Dispel specific spell", "Salamander aura",
          "Umber hulk gaze", "Zombie lord aura",
          "Immunity to specific resource", "Summon creatures with cloud",
          "Hide creature", "Immunity to effect and string", "Pomab images",
          "Evil turn undead", "Static charge", "Cloak of fear",
          "Movement rate modifier", "Cure confusion", "Eye of the mind",
          "Eye of the sword", "Eye of the mage", "Eye of venom",
          "Eye of the spirit", "Eye of fortitude", "Eye of stone",
          "Remove seven eyes", "Remove effect by type", "Soul eater",
          "Shroud of flame", "Animal rage", "Turn undead", "Vitriolic sphere",
          "Hide hit points", "Float text", "Mace of disruption", "Force sleep",
          "Ranger tracking", "Immunity to sneak attack", "Set status",
          "Dragon gem cutscene", "Display spell immunity string",
          "Rod of smiting", "Rest", "Beholder dispel magic", "Harpy wail",
          "Jackalwere gaze", "Set global variable"};
        s_poricon = new String[]{"Charm", "Dire charm", "Rigid thinking",
          "Confused", "Berserk", "Intoxicated", "Poisoned", "Nauseated",
          "Blind", "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Magic armor", "Held", "Sleep",
          "Shielded", "Protection from fire", "Blessed", "Chant", "Free action",
          "Barkskin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Nondetection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Nauseous",
          "Stun", "Ghost armor", "Stoneskin", "Hopelessness", "Courage",
          "Friends", "Hope", "Malison", "Spirit armor", "Domination",
          "Feeblemind", "Tenser's transformation", "Mind blank", "Aid",
          "Find traps", "Draw upon holy might", "Miscast magic",
          "Strength of one", "Prayer", "Defensive harmony", "Recitation",
          "Champion's strength", "Chaotic commands",
          "Righteous wrath of the faithful", "Phased", "Pain",
          "Impervious sanctity of mind", "Petrified", "Iron body",
          "Animal rage", "Exaltation", "Cat's grace", "Blood rage",
          "Ballad of three heroes", "Tale of curran strongheart",
          "Tymora's melody", "Song of kaudies", "Siren's yearning",
          "War chant of sith", "Deaf", "Armor of faith"};
        break;

      case ResourceFactory.ID_BG2:
      case ResourceFactory.ID_BG2TOB:
      case ResourceFactory.ID_TUTU:
      case ResourceFactory.ID_UNKNOWNGAME: // Default list
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set color", "Set color glow solid",
          "Set color glow pulse", "Constitution bonus", "Cure poison", "Damage",
          "Kill target", "Defrost", "Dexterity bonus", "Haste",
          "Current HP bonus", "Maximum HP bonus", "Intelligence bonus",
          "Invisibility", "Lore bonus", "Luck bonus", "Reset morale", "Panic",
          "Poison", "Remove curse", "Acid resistance bonus",
          "Cold resistance bonus", "Electricity resistance bonus",
          "Fire resistance bonus", "Magic damage resistance bonus",
          "Raise dead", "Save vs. death bonus", "Save vs. wand bonus",
          "Save vs. polymorph bonus", "Save vs. breath bonus",
          "Save vs. spell bonus", "Silence", "Sleep", "Slow", "Sparkle",
          "Bonus wizard spells", "Stone to flesh", "Strength bonus", "Stun",
          "Cure stun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Move silently bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucency", "Summon creature", "Unsummon creature",
          "Nondetection", "Remove nondetection", "Change gender",
          "Change AI type", "Attack damage bonus", "Blindness",
          "Cure blindness", "Feeblemindedness", "Cure feeblemindedness",
          "Disease", "Cure disease", "Deafness", "Cure deafness",
          "Set AI script", "Immunity to projectile",
          "Magical fire resistance bonus", "Magical cold resistance bonus",
          "Slashing resistance bonus", "Crushing resistance bonus",
          "Piercing resistance bonus", "Missile resistance bonus",
          "Open locks bonus", "Find traps bonus", "Pick pockets bonus",
          "Fatigue bonus", "Intoxication bonus", "Tracking bonus",
          "Change level", "Exceptional strength bonus", "Regeneration",
          "Modify duration", "Protection from creature type",
          "Immunity to effect", "Immunity to spell level", "Change name",
          "XP bonus", "Remove gold", "Morale break", "Change portrait",
          "Reputation bonus", "Paralyze", "Retreat from", "Create weapon",
          "Remove item", "Equip weapon", "Dither", "Detect alignment",
          "Detect invisible", "Clairvoyance", "Show creatures", "Mirror image",
          "Immunity to weapons", "Visual animation effect",
          "Create inventory item", "Remove inventory item", "Teleport",
          "Unlock", "Movement rate bonus", "Summon monsters", "Confusion",
          "Aid (non-cumulative)", "Bless (non-cumulative)",
          "Chant (non-cumulative)", "Draw upon holy might (non-cumulative)",
          "Luck (non-cumulative)", "Petrification", "Polymorph",
          "Force visible", "Bad chant (non-cumulative)",
          "Set animation sequence", "Display string", "Casting glow",
          "Lighting effects", "Display portrait icon", "Create item in slot",
          "Disable button", "Disable spellcasting", "Cast spell", "Learn spell",
          "Cast spell at point", "Identify", "Find traps", "Replace self",
          "Play movie", "Sanctuary", "Entangle overlay", "Minor globe overlay",
          "Protection from normal missiles overlay", "Web effect",
          "Grease overlay", "Mirror image effect", "Remove sanctuary",
          "Remove fear", "Remove paralysis", "Free action",
          "Remove intoxication", "Pause caster", "Magic resistance bonus",
          "Missile THAC0 bonus", "Remove creature", "Prevent portrait icon",
          "Play damage animation", "Give innate ability", "Remove spell",
          "Poison resistance bonus", "Play sound", "Hold creature",
          "Movement rate bonus 2", "Use EFF file", "THAC0 vs. type bonus",
          "Damage vs. type bonus", "Disallow item", "Disallow item type",
          "Apply effect on equip item", "Apply effect on equip type",
          "No collision detection", "Hold creature 2", "Move creature",
          "Set local variable", "Increase spells cast per round",
          "Increase casting speed factor", "Increase attack speed factor",
          "Casting level bonus", "Find familiar",
          "Invisibility detection", "Ignore dialogue pause",
          "Drain HP on death", "Familiar", "Physical mirror",
          "Reflect specified effect", "Reflect spell level", "Spell turning",
          "Spell deflection", "Reflect spell school", "Reflect spell type",
          "Protection from spell school", "Protection from spell type",
          "Protection from spell", "Reflect specified spell", "Minimum HP",
          "Power word, kill", "Power word, stun", "Imprisonment", "Freedom",
          "Maze", "Select spell", "Play visual effect", "Level drain",
          "Power word, sleep", "Stoneskin effect", "Attack roll penalty",
          "Remove spell school protections", "Remove spell type protections",
          "Teleport field", "Spell school deflection", "Restoration",
          "Detect magic", "Spell type deflection", "Spell school turning",
          "Spell type turning", "Remove protection by school",
          "Remove protection by type", "Time stop", "Cast spell on condition",
          "Modify proficiencies", "Create contingency", "Wing buffet",
          "Project image", "Set image type", "Disintegrate", "Farsight",
          "Remove portrait icon", "Control creature", "Cure confusion",
          "Drain item charges", "Drain wizard spells", "Unknown (F5)",
          "Berserk effect", "Attack nearest creature", "Melee hit effect",
          "Ranged hit effect", "Maximum damage each hit", "Change bard song",
          "Set trap", "Set automap note", "Remove automap note",
          "Create item (days)", "Spell sequencer", "Create spell sequencer",
          "Activate spell sequencer", "Spell trap", "Unknown (104)",
          "Wondrous recall", "Visual range bonus", "Backstab bonus",
          "Drop item", "Set global variable", "Remove protection from spell",
          "Disable display string", "Clear fog of war", "Shake screen",
          "Unpause caster", "Disable creature", "Use EFF file on condition",
          "Zone of sweet air", "Phase", "Hide in shadows bonus",
          "Detect illusions bonus", "Set traps bonus", "THAC0 bonus",
          "Enable button", "Wild magic", "Wild surge bonus",
          "Modify script state", "Use EFF file as curse", "Melee THAC0 bonus",
          "Melee weapon damage bonus", "Missile weapon damage bonus",
          "Remove feet circle", "Fist THAC0 bonus", "Fist damage bonus",
          "Change title", "Disable visual effects", "Immunity to backstab",
          "Set persistent AI", "Set existence delay", "Disable permanent death",
          "Immunity to specific animation", "Immunity to turn undead",
          "Pocket plane", "Chaos shield effect", "Modify collision behavior",
          "Critical threat range bonus", "Can use any item",
          "Backstab every hit", "Mass raise dead", "Off-hand THAC0 bonus",
          "Main hand THAC0 bonus", "Tracking", "Immunity to tracking",
          "Set variable", "Immunity to time stop", "Wish",
          "Immunity to sequester", "High-level ability", "Stoneskin protection",
          "Remove animation", "Rest", "Haste 2"};
        s_poricon = new String[]{"Charm", "Dire charm", "Rigid thinking",
          "Confused", "Berserk", "Intoxicated", "Poisoned", "Nauseated",
          "Blind", "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Magic armor", "Held", "Sleep",
          "Shielded", "Protection from fire", "Blessed", "Chant", "Free action",
          "Barkskin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Nondetection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Domination",
          "Hopelessness", "Greater malison", "Spirit armor", "Chaos",
          "Feeblemind", "Defensive harmony", "Champion's strength", "Dying",
          "Mind shield", "Level drain", "Polymorph self", "Stun",
          "Regeneration", "Perception", "Master thievery", "Energy drain",
          "Holy power", "Cloak of fear", "Iron skins", "Magic resistance",
          "Righteous magic", "Spell turning", "Repulsing undead",
          "Spell deflection", "Fire shield (red)", "Fire shield (blue)",
          "Protection from normal weapons", "Protection from magic weapons",
          "Tenser's transformation", "Protection from magic energy",
          "Mislead", "Contingency", "Protection from the elements",
          "Projected image", "Maze", "Imprisonment", "Stoneskin", "Kai",
          "Called shot", "Spell failure", "Offensive stance",
          "Defensive stance", "Intelligence drained", "Regenerating", "Talking",
          "Shopping", "Negative plane protection", "Ability score drained",
          "Spell sequencer", "Protection from energy", "Magnetized",
          "Able to poison weapons", "Setting trap", "Glass dust",
          "Blade barrier", "Death ward", "Doom", "Decaying", "Acid", "Vocalize",
          "Mantle", "Miscast magic", "Lower resistance", "Spell immunity",
          "True seeing", "Detecting traps", "Improved haste", "Spell trigger",
          "Deaf", "Enfeebled", "Infravision", "Friends",
          "Shield of the archons", "Spell trap", "Absolute immunity",
          "Improved mantle", "Farsight", "Globe of invulnerability",
          "Minor globe of invulnerability", "Spell shield", "Polymorphed",
          "Otiluke's resilient sphere", "Nauseous", "Ghost armor",
          "Glitterdust", "Webbed", "Unconscious", "Mental combat",
          "Physical mirror", "Repulse undead", "Chaotic commands",
          "Draw upon holy might", "Strength of one", "Bleeding",
          "Barbarian rage", "Boon of lathander", "Storm shield", "Enraged",
          "Stunning blow", "Quivering palm", "Entangled", "Grease", "Smite",
          "Hardiness", "Power attack", "Whirlwind attack",
          "Greater whirlwind attack", "Magic flute", "Critical strike",
          "Greater deathblow", "Deathblow", "Avoid death", "Assassination",
          "Evasion", "Greater evasion", "Improved alacrity",
          "Aura of flaming death", "Globe of blades", "Improved chaos shield",
          "Chaos shield", "Fire elemental transformation",
          "Earth elemental transformation"};
        break;

      case ResourceFactory.ID_ICEWIND2:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set color", "Set color glow solid",
          "Set color glow pulse", "Constitution bonus", "Cure poison", "Damage",
          "Kill target", "Defrost", "Dexterity bonus", "Haste",
          "Current HP bonus", "Maximum HP bonus", "Intelligence bonus",
          "Invisibility", "Knowledge arcana", "Luck bonus", "Morale bonus",
          "Panic", "Poison", "Remove curse", "Acid resistance bonus",
          "Cold resistance bonus", "Electricity resistance bonus",
          "Fire resistance bonus", "Magic damage resistance bonus",
          "Raise dead", "Fortitude save bonus", "Reflex save bonus",
          "Will save bonus", "Unknown (24)", "Unknown (25)", "Silence", "Sleep",
          "Slow", "Sparkle", "Bonus wizard spells", "Stone to flesh",
          "Strength bonus", "Stun", "Cure stun", "Remove invisibility",
          "Vocalize", "Wisdom bonus", "Character color pulse",
          "Character tint solid", "Character tint bright", "Animation change",
          "Base attack bonus", "Slay", "Reverse alignment", "Change alignment",
          "Dispel effects", "Move silently bonus", "Casting failure", "Alchemy",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucency", "Summon creature", "Unsummon creature",
          "Nondetection", "Remove nondetection", "Change gender",
          "Change AI type", "Attack damage bonus", "Blindness",
          "Cure blindness", "Feeblemindedness", "Cure feeblemindedness",
          "Disease", "Cure disease", "Deafness", "Cure deafness",
          "Set AI script", "Immunity to projectile",
          "Magical fire resistance bonus", "Magical cold resistance bonus",
          "Slashing resistance bonus", "Crushing resistance bonus",
          "Piercing resistance bonus", "Missile resistance bonus",
          "Open locks bonus", "Find traps bonus", "Pick pockets bonus",
          "Fatigue bonus", "Intoxication bonus", "Tracking bonus",
          "Change level", "Exceptional strength bonus", "Regeneration",
          "Modify duration", "Protection from creature type",
          "Immunity to effect", "Immunity to spell level", "Change name",
          "XP bonus", "Remove gold", "Morale break", "Change portrait",
          "Reputation bonus", "Paralyze", "Retreat from", "Create weapon",
          "Remove item", "Equip weapon", "Dither", "Detect alignment",
          "Detect invisible", "Clairvoyance", "Show creatures", "Mirror image",
          "Immunity to weapons", "Visual animation effect",
          "Create inventory item", "Remove inventory item", "Teleport",
          "Unlock", "Movement rate bonus", "Summon monsters", "Confusion",
          "Aid (non-cumulative)", "Bless (non-cumulative)",
          "Chant (non-cumulative)", "Draw upon holy might (non-cumulative)",
          "Luck (non-cumulative)", "Petrification", "Polymorph",
          "Force visible", "Bad chant (non-cumulative)",
          "Set animation sequence", "Display string", "Casting glow",
          "Lighting effects", "Display portrait icon", "Create item in slot",
          "Disable button", "Disable spellcasting", "Cast spell", "Learn spell",
          "Cast spell at point", "Identify", "Find traps", "Replace self",
          "Play movie", "Sanctuary", "Entangle overlay", "Minor globe overlay",
          "Protection from normal missiles overlay", "Web effect",
          "Grease overlay", "Mirror image effect", "Remove sanctuary",
          "Remove fear", "Remove paralysis", "Reset movement rate",
          "Remove intoxication", "Pause caster", "Magic resistance bonus",
          "Missile attack bonus", "Remove creature", "Prevent portrait icon",
          "Play damage animation", "Give innate ability", "Remove spell",
          "Poison resistance bonus", "Play sound", "Hold creature",
          "Movement rate penalty", "Use EFF file", "THAC0 vs. type bonus",
          "Damage vs. type bonus", "Disallow item", "Disallow item type",
          "Apply effect on equip item", "Apply effect on equip type",
          "No collision detection", "Hold creature 2", "Move creature",
          "Set local variable", "Increase spells cast per round",
          "Increase casting speed factor", "Increase attack speed factor",
          "Casting level bonus", "Find familiar", "Invisibility detection",
          "Ignore dialogue pause", "Drain HP on death", "Familiar",
          "Physical mirror", "Reflect specified effect", "Reflect spell level",
          "Spell turning", "Spell deflection", "Reflect spell school",
          "Reflect spell type", "Protection from spell school",
          "Protection from spell type", "Protection from spell",
          "Reflect specified spell", "Minimum HP", "Power word, kill",
          "Power word, stun", "Imprisonment", "Freedom", "Maze", "Select spell",
          "Play visual effect", "Level drain", "Power word, sleep",
          "Stoneskin effect", "Attack roll penalty",
          "Remove spell school protections", "Remove spell type protections",
          "Teleport field", "Spell school deflection", "Cure level drain",
          "Detect magic", "Spell type deflection", "Spell school turning",
          "Spell type turning", "Remove protection by school",
          "Remove protection by type", "Time stop", "Creature RGB color fade",
          "Show visual effect", "Snilloc's snowball swarm", "Show casting glow",
          "Chill touch", "Magical stone", "All saving throws bonus",
          "Slow poison", "Summon creature 2", "Vampiric touch",
          "Show visual overlay", "Animate dead", "Prayer", "Bad prayer",
          "Summon creature 3", "Beltyn's burning blood", "Summon shadow",
          "Recitation", "Bad recitation", "Lich touch", "Sol's searing orb",
          "Bonus AC vs. weapons", "Dispel specific spell", "Salamander aura",
          "Umber hulk gaze", "Zombie lord aura",
          "Immunity to specific resource", "Summon creatures with cloud",
          "Hide creature", "Immunity to effect and resource", "Pomab images",
          "Evil turn undead", "Static charge", "Cloak of fear",
          "Movement rate modifier", "Cure confusion", "Eye of the mind",
          "Eye of the sword", "Eye of the mage", "Eye of venom",
          "Eye of the spirit", "Eye of fortitude", "Eye of stone",
          "Remove seven eyes", "Remove effect by type", "Soul eater",
          "Shroud of flame", "Animal rage", "Turn undead", "Vitriolic sphere",
          "Hide hit points", "Float text", "Mace of disruption", "Force sleep",
          "Ranger tracking", "Immunity to sneak attack", "Set status",
          "Dragon gem cutscene", "Display spell immunity string",
          "Rod of smiting", "Rest", "Beholder dispel magic", "Harpy wail",
          "Jackalwere gaze", "Set global variable", "Hide in shadows bonus",
          "Use magic device bonus", "Unknown (12B)", "Unknown (12C)",
          "Unknown (12D)", "Unknown (12E)", "Unknown (12F)", "Unknown (130)",
          "Unknown (131)", "Unknown (132)", "Unknown (133)", "Unknown (134)",
          "Unknown (135)", "Unknown (136)", "Unknown (137)", "Unknown (138)",
          "Unknown (139)", "Unknown (13A)", "Unknown (13B)", "Unknown (13C)",
          "Unknown (13D)", "Unknown (13E)", "Unknown (13F)", "Unknown (140)",
          "Unknown (141)", "Unknown (142)", "Unknown (143)", "Unknown (144)",
          "Unknown (145)", "Unknown (146)", "Unknown (147)", "Unknown (148)",
          "Unknown (149)", "Unknown (14A)", "Unknown (14B)", "Unknown (14C)",
          "Unknown (14D)", "Unknown (14E)", "Unknown (14F)", "Unknown (150)",
          "Unknown (151)", "Unknown (152)", "Unknown (153)", "Unknown (154)",
          "Unknown (155)", "Unknown (156)", "Unknown (157)", "Unknown (158)",
          "Unknown (159)", "Unknown (15A)", "Unknown (15B)", "Unknown (15C)",
          "Unknown (15D)", "Unknown (15E)", "Unknown (15F)", "Unknown (160)",
          "Unknown (161)", "Unknown (162)", "Unknown (163)", "Unknown (164)",
          "Unknown (165)", "Unknown (166)", "Unknown (167)", "Unknown (168)",
          "Unknown (169)", "Unknown (16A)", "Unknown (16B)", "Unknown (16C)",
          "Unknown (16D)", "Unknown (16E)", "Unknown (16F)", "Unknown (170)",
          "Unknown (171)", "Unknown (172)", "Unknown (173)", "Unknown (174)",
          "Unknown (175)", "Unknown (176)", "Unknown (177)", "Unknown (178)",
          "Unknown (179)", "Unknown (17A)", "Unknown (17B)", "Unknown (17C)",
          "Unknown (17D)", "Unknown (17E)", "Unknown (17F)", "Unknown (180)",
          "Unknown (181)", "Unknown (182)", "Unknown (183)", "Unknown (184)",
          "Unknown (185)", "Unknown (186)", "Unknown (187)", "Unknown (188)",
          "Unknown (189)", "Unknown (18A)", "Unknown (18B)", "Unknown (18C)",
          "Unknown (18D)", "Unknown (18E)", "Unknown (18F)", "Hopelessness",
          "Protection from evil", "Apply effects list", "Armor of faith",
          "Nausea", "Enfeeblement", "Fire shield", "Death ward", "Holy power",
          "Righteous wrath of the faithful", "Summon friendly creature",
          "Summon hostile creature", "Control creature", "Run visual effect",
          "Otiluke's resilient sphere", "Barkskin", "Bleeding wounds",
          "Area effect using effects list", "Free action", "Unconsciousness",
          "Death magic", "Entropy shield", "Storm shell",
          "Protection from the elements", "Hold undead", "Control undead",
          "Aegis", "Executioner's eyes", "Banish", "Apply effects list on hit",
          "Projectile type using effects list", "Energy drain",
          "Tortoise shell", "Blink", "Persistent using effects list",
          "Day blindness", "Damage reduction", "Disguise", "Heroic inspiration",
          "Prevent AI slowdown", "Barbarian rage", "Force slow", "Cleave",
          "Protection from arrows", "Tenser's transformation", "Unknown (1BD)",
          "Smite evil", "Restoration", "Alicorn lance", "Call lightning",
          "Globe of invulnerability", "Lower resistance", "Bane",
          "Power attack", "Expertise", "Arterial strike", "Hamstring",
          "Rapid shot", "Unknown (1CA)", "Unknown (1CB)"};
        s_poricon = new String[]{"Charmed", "", "", "Confused", "Berserk",
          "Intoxicated", "Poisoned", "Diseased", "Blind",
          "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Armor", "Held", "Asleep",
          "Shield", "Protection from fire", "Bless", "Chant", "Free action",
          "Barkskin", "Strength", "Heroism", "Spell invulnerability",
          "Protection from acid", "Protection from cold", "",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Undetectable",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Hasted", "Fatigued", "Bard song", "Slowed", "Regenerating",
          "Nauseous", "Stunned", "Ghost armor", "Stoneskin", "Hopelessness",
          "Courage", "Friends", "Hope", "Malison", "Spirit armor", "Dominated",
          "Feebleminded", "Tenser's transformation", "Mind blank", "Aid",
          "Find traps", "Draw upon holy might", "Miscast magic",
          "Strength of one", "Prayer", "Defensive harmony", "Recitation",
          "Champion's strength", "Chaotic commands",
          "Righteous wrath of the faithful", "Phased", "Pain",
          "Impervious sanctity of mind", "Petrified", "Iron body",
          "Animal rage", "Exaltation", "Cat's grace", "Blood rage",
          "Ballad of three heroes", "Tale of curran strongheart",
          "Tymora's melody", "Song of kaudies", "Siren's yearning",
          "War chant of sith", "Deaf", "Armor of faith", "Bleeding wound",
          "Holy power", "Death ward", "Unconscious", "Iron skins",
          "Enfeeblement", "Sanctuary", "Entangle",
          "Protection from the elements", "Grease", "Web",
          "Minor globe of invulnerability", "Globe of invulnerability",
          "Shroud of flame", "Antimagic shell", "Otiluke's resilient sphere",
          "Intelligence drained", "Cloak of fear", "Entropy shield",
          "Insect plague", "Storm shell", "Shield of lathander",
          "Greater shield of lathander", "Seven eyes", "Blur", "Invisibility",
          "Barbarian rage", "Called shot", "Defensive spin",
          "Maximized attacks", "Offensive spin", "Envenom weapon",
          "Unconscious", "Doom", "Aegis", "Executioner's eyes",
          "Fire shield (red)", "Fire shield (blue)", "Energy drained",
          "Faerie fire", "Tortoise shell", "Spell shield",
          "Negative energy protection", "Aura of vitality", "Death armor",
          "Blink", "Vipergout", "Mind fog", "", "Stunning fist",
          "Quivering palm", "Gram's paradox", "Blindness", "Heroic inspiration",
          "Vocalize", "Despair", "Ilmater's endurance", "Destructive blow",
          "Master thievery", "Improved invisibility"};
        break;

      default:
        s_effname = new String[0];
        s_poricon = new String[0];
    }
  }

  public String[] getEffectNameArray()
  {
    return s_effname;
  }

  public String makeEffectStruct(byte buffer[], int offset, List<StructEntry> s, int effectType)
  {
    String restype = null;
    final int initSize = s.size();
    final int gameid = ResourceFactory.getGameID();

    // -----------------------
    // Infinity Engine Effects
    // -----------------------
    switch (effectType) {
      case 0x5: // Charm creature (CGameEffectCharm)
        s.add(new IdsBitmap(buffer, offset, 4, "Creature type", "GENERAL.IDS"));
        s.add(new Flag(buffer, offset + 4, 4, "Charm flags",
          new String[]{"No flags set", "Turn hostile afterward", "Dire charm",
                       "Controlled by cleric"}));
        break;

      case 0x6: // Charisma bonus (CGameEffectCHR)
      case 0xA: // Constitution bonus (CGameEffectCON)
      case 0x13: // Intelligence bonus (CGameEffectINT)
      case 0x1B: // Acid resistance bonus (CGameEffectResistAcid)
      case 0x1C: // Cold resistance bonus (CGameEffectResistCold)
      case 0x1D: // Electricity resistance bonus (CGameEffectResistElectricity)
      case 0x1E: // Fire resistance bonus (CGameEffectResistFire)
      case 0x1F: // Magic damage resistance bonus (CGameEffectResistMagic)
      case 0x21: // Save vs. death bonus (CGameEffectSaveVsDeath) / Fortitude save bonus
      case 0x22: // Save vs. wand bonus (CGameEffectSaveVsWands) / Reflex save bonus
      case 0x23: // Save vs. polymorph bonus (CGameEffectSaveVsPoly) / Will save bonus
      case 0x24: // Save vs. breath bonus (CGameEffectSaveVsBreath)
      case 0x25: // Save vs. spell bonus (CGameEffectSaveVsSpell)
      case 0x31: // Wisdom bonus (CGameEffectWIS)
      case 0x36: // Base THAC0 bonus (CGameEffectThac0) / Base attack bonus
      case 0x3B: // Stealth bonus / Move silently bonus (CGameEffectSkillStealth)
      case 0x54: // Magical fire resistance bonus (CGameEffectResistMagicFire)
      case 0x55: // Magical cold resistance bonus (CGameEffectResistMagicCold)
      case 0x56: // Slashing resistance bonus (CGameEffectResistSlashing)
      case 0x57: // Crushing resistance bonus (CGameEffectResistCrushing)
      case 0x58: // Piercing resistance bonus (CGameEffectResistPiercing)
      case 0x59: // Missile resistance bonus (CGameEffectResistMissile)
      case 0x5A: // Open locks bonus (CGameEffectSkillLockPicking)
      case 0x5B: // Find traps bonus (CGameEffectSkillTraps)
      case 0x5C: // Pick pockets bonus (CGameEffectSkillPickPocket)
      case 0x5D: // Fatigue bonus (CGameEffectFatigue)
      case 0x5E: // Intoxication bonus (CGameEffectIntoxication)
      case 0x5F: // Tracking bonus (CGameEffectSkillTracking)
      case 0x60: // Change level (CGameEffectLevel)
      case 0x61: // Exceptional strength bonus (CGameEffectSTRExtra)
      case 0x68: // XP bonus (CGameEffectXP)
      case 0x69: // Remove gold (CGameEffectGold)
      case 0x6A: // Morale break (CGameEffectMoraleBreak)
      case 0x6C: // Reputation bonus (CGameEffectReputation)
      case 0x7E: // Movement rate bonus (CGameEffectMovementRate)
      case 0xA7: // Missile THAC0 bonus (CGameEffectMissileTHAC0Bonus) / Missile attack bonus
      case 0xB0: // Movement rate bonus 2 (CGameEffectMovementRate) / Movement rate penalty
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
        break;

      case 0x7: // Set color (CGameEffectColorChange)
        s.add(new ColorValue(buffer, offset, 4, "Color"));
        s.add(new HashBitmap(buffer, offset + 4, 4, "Location", m_colorloc));
        break;

      case 0x8: // Set color glow solid (CGameEffectColorGlowSolid)
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new HashBitmap(buffer, offset + 4, 4, "Location", m_colorloc));
        break;

      case 0x9: // Set color glow pulse (CGameEffectColorGlowPulse)
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new HashBitmap(buffer, offset + 4, 2, "Location", m_colorloc));
        s.add(new DecNumber(buffer, offset + 6, 2, "Cycle speed"));
        break;

      case 0xD: // Kill target (CGameEffectDeath)
        s.add(new Bitmap(buffer, offset, 4, "Display text?", s_yesno));
        s.add(new Flag(buffer, offset + 4, 4, "Death type",
          new String[]{"Acid", "Burning", "Crushed", "Normal", "Exploding",
                       "Stoned", "Freezing", "Exploding stoned",
                       "Exploding freezing", "Electrified", "Disintegration"}));
        break;

      case 0x10: // Haste (CGameEffectHaste)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Haste type",
          new String[]{"Normal", "Improved", "Movement rate only"}));
        break;

      case 0x12: // Maximum HP bonus (CGameEffectHitPoints)
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
          new String[]{"Increment", "Set", "Set % of",
                       "Increment, don't update current HP",
                       "Set, don't update current HP",
                       "Set % of, don't update current HP"}));
        break;

      case 0x14: // Invisibility (CGameEffectInvisible)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Invisibility type",
          new String[]{"Normal", "Improved"}));
        break;

      case 0x15: // Lore bonus (CGameEffectLore) / Knowledge arcana
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
          new String[]{"Increment", "Set", "Mastery"}));
        break;

      case 0x29: // Sparkle (CGameEffectSparkle)
        s.add(new Bitmap(buffer, offset, 4, "Color",
          new String[]{"", "Black", "Blue", "Chromatic", "Gold", "Green",
                       "Purple", "Red", "White", "Ice", "Stone", "Magenta",
                       "Orange"}));
        s.add(new Bitmap(buffer, offset + 4, 4, "Particle effect",
          new String[]{"", "Explosion", "Swirl", "Shower"}));
        break;

      case 0x2A: // Bonus wizard spells (CGameEffectSpellMemorizationMage)
        s.add(new DecNumber(buffer, offset, 4, "# spells to add"));
        s.add(new Flag(buffer, offset + 4, 4, "Spell levels",
          new String[]{"Double spells", "Level 1", "Level 2", "Level 3",
                       "Level 4", "Level 5", "Level 6", "Level 7", "Level 8",
                       "Level 9"}));
        break;

      case 0x32: // Character color pulse (CGameEffectSingleColorPulseAll)
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new Unknown(buffer, offset + 4, 2));
        s.add(new DecNumber(buffer, offset + 6, 2, "Cycle speed"));
        break;

      case 0x33: // Character tint solid (CGameEffectColorTintSolid)
      case 0x34: // Character tint bright (CGameEffectColorLightSolid)
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new HashBitmap(buffer, offset + 4, 2, "Location", m_colorloc));
        s.add(new Unknown(buffer, offset + 6, 2));
        break;

      case 0x35: // Animation change (CGameEffectAnimationChange)
        s.add(new IdsBitmap(buffer, offset, 4, "Morph into", "ANIMATE.IDS"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Morph type",
          new String[]{"Temporary change", "Remove temporary change",
                       "Permanent change"}));
        break;

      case 0x37: // Slay (CGameEffectSlay)
      case 0x64: // Protection from creature type (CGameEffectProtectionFromCreature)
      case 0x6D: // Paralyze (CGameEffectHoldCreature)
      case 0xAF: // Hold creature (CGameEffectHoldCreatureSpell)
      case 0xB2: // THAC0 vs. type bonus (CGameEffectSelectiveToHitBonus)
      case 0xB3: // Damage vs. type bonus (CGameEffectSelectiveDamageBonus)
      case 0xB9: // Hold creature 2 (CGameEffectHoldCreatureSpell)
        s.add(new IDSTargetEffect(buffer, offset));
        break;

      case 0x39: // Change alignment (CGameEffectAlignmentChange)
        s.add(new Unknown(buffer, offset, 4));
        if (gameid == ResourceFactory.ID_ICEWIND2)
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Alignment", "ALIGNMNT.IDS"));
        else
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Alignment", "ALIGN.IDS"));
        break;

      case 0x3A: // Dispel effects (CGameEffectDispelEffects)
        s.add(new DecNumber(buffer, offset, 4, "Level"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Dispel type",
          new String[]{"Always dispel", "Use caster level",
                       "Use specific level"}));
        break;

      case 0x3E: // Bonus priest spells (CGameEffectSpellMemorizationCleric)
        s.add(new DecNumber(buffer, offset, 4, "# spells to add"));
        s.add(new Flag(buffer, offset + 4, 4, "Spell levels",
          new String[]{"Double spells", "Level 1", "Level 2", "Level 3",
                       "Level 4", "Level 5", "Level 6", "Level 7"}));
        break;

      case 0x44: // Unsummon creature (CGameEffectUnsummon)
        s.add(new Bitmap(buffer, offset, 4, "Display text?", s_noyes));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x47: // Change gender (CGameEffectSexChange)
        s.add(new IdsBitmap(buffer, offset, 4, "Gender", "GENDER.IDS"));
        s.add(new Bitmap(buffer, offset + 4, 4, "How?",
          new String[]{"Reverse gender", "Set gender"}));
        break;

      case 0x48: // Change AI type (CGameEffectAIChange)
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "AI type",
          new String[]{"Allegiance", "General", "Race", "Class", "Specifics",
                       "Gender", "Alignment"}));
        break;

      case 0x53: // Immunity to projectile (CGameEffectImmunityToProjectile)
        s.add(new Unknown(buffer, offset, 4));
        if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Projectile", "PROJECTL.IDS"));
        else
          s.add(new DecNumber(buffer, offset + 4, 4, "Projectile"));
        break;

      case 0x63: // Modify duration (CGameEffectDurationCasting)
        s.add(new DecNumber(buffer, offset, 4, "Percentage"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
          new String[]{"Wizard", "Priest"}));
        break;

      case 0x65: // Immunity to effect (CGameEffectImmunityToEffect)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
        break;

      case 0x66: // Immunity to spell level (CGameEffectImmunityToSpellLevel)
        s.add(new DecNumber(buffer, offset, 4, "Spell level"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x67: // Change name (CGameEffectName)
        s.add(new StringRef(buffer, offset, "Name"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x6B: // Change portrait (CGameEffectPortrait)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Which portrait?",
          new String[]{"Small", "Large"}));
        restype = "BMP";
        break;

      case 0x6F: // Create weapon (CGameEffectCreateWeapon)
      case 0x7A: // Create inventory item (CGameEffectCreateItem)
        s.add(new DecNumber(buffer, offset, 4, "# to create"));
        s.add(new Unknown(buffer, offset + 4, 4));
        restype = "ITM";
        break;

      case 0x70: // Remove item (CGameEffectDestroyWeapon)
      case 0x7B: // Remove inventory item (CGameEffectDestroyItem)
        restype = "ITM";
        break;

      case 0x73: // Detect alignment (CGameEffectDetectAlignment)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Alignment mask",
          new String[]{"Evil", "Neutral", "Good"}));
        break;

      case 0x78: // Immunity to weapons (CGameEffectImmuneToWeapon)
        s.add(new DecNumber(buffer, offset, 4, "Maximum enchantment"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Weapon type",
          new String[]{"Enchanted", "Magical", "Non-magical", "Silver",
                       "Non-silver", "Non-silver, non-magical", "Two-handed",
                       "One-handed", "Cursed", "Non-cursed", "Cold iron",
                       "Non-cold-iron"}));
        break;

      case 0x7F: // Summon monsters (CGameEffectRandomSummon)
        s.add(new DecNumber(buffer, offset, 4, "Total XP"));
        s.add(new Bitmap(buffer, offset + 4, 4, "From 2DA file",
          new String[]{"Monsum01 (ally)", "Monsum02 (ally)", "Monsum03 (ally)",
                       "Anisum01 (ally)", "Anisum02 (ally)", "Monsum01 (enemy)",
                       "Monsum02 (enemy)", "Monsum03 (enemy)",
                       "Anisum01 (enemy)", "Anisum02 (enemy)"}));
        restype = "2DA";
        break;

      case 0x81: // Aid (non-cumulative) (CGameEffectNon_CumulativeAid)
        s.add(new DecNumber(buffer, offset, 4, "Amount"));
        s.add(new DecNumber(buffer, offset + 4, 4, "HP bonus"));
        break;

      case 0x82: // Bless (non-cumulative) (CGameEffectNon_CumulativeBless)
      case 0x84: // Draw upon holy might (non-cumulative) (CGameEffectNon_CumulativeDrawUponHolyMight)
      case 0x85: // Luck (non-cumulative) (CGameEffectNon_CumulativeLuck)
      case 0x89: // Bad chant (non-cumulative) (CGameEffectNon_CumulativeChantBad)
      case 0xAD: // Poison resistance bonus (CGameEffectResistanceToPoison)
        s.add(new DecNumber(buffer, offset, 4, "Amount"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x87: // Polymorph (CGameEffectPolymorph)
        s.add(new IdsBitmap(buffer, offset, 4, "Animation", "ANIMATE.IDS"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Polymorph type",
          new String[]{"Change into", "Appearance only", "Appearance only",
                       "Appearance only"}));
        restype = "CRE";
        break;

      case 0x8A: // Set animation sequence (CGameEffectSetSequence)
        s.add(new Unknown(buffer, offset, 4));
        if (ResourceFactory.getInstance().resourceExists("ANIMSTAT.IDS"))
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Sequence", "ANIMSTAT.IDS"));
        else if (ResourceFactory.getInstance().resourceExists("SEQ.IDS"))
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Sequence", "SEQ.IDS"));
        else {
          String s_seqtype[] = {"", "Lay down (short)", "Move hands (short)",
                                "Move hands (long)", "Move shoulder (short)",
                                "Move shoulder (long)", "Lay down (long)",
                                "Breathe rapidly (short)",
                                "Breathe rapidly (long)"};
          if (gameid == ResourceFactory.ID_ICEWIND2)
            s_seqtype = new String[]{"Attack", "Awake", "Cast", "Conjure",
                                     "Damage", "Die", "Turn head", "Ready",
                                     "Shoot", "Twitch", "Walk", "Attack slash",
                                     "Attack backslash", "Attack jab", "Emerge",
                                     "Hide", "Sleep"};
          s.add(new Bitmap(buffer, offset + 4, 4, "Sequence", s_seqtype));
        }
        break;

      case 0x8B: // Display string (CGameEffectDisplayString)
        s.add(new StringRef(buffer, offset, "String"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x8C: // Casting glow (CGameEffectCastingGlow)
        final LongIntegerHashMap<String> m_castglow = new LongIntegerHashMap<String>();
        m_castglow.put(9L, "Necromancy");
        m_castglow.put(10L, "Alteration");
        m_castglow.put(11L, "Enchantment");
        m_castglow.put(12L, "Abjuration");
        m_castglow.put(13L, "Illusion");
        m_castglow.put(14L, "Conjuration");
        m_castglow.put(15L, "Invocation");
        m_castglow.put(16L, "Divination");
        s.add(new Unknown(buffer, offset, 4));
        s.add(new HashBitmap(buffer, offset + 4, 4, "Glow", m_castglow));
        break;

      case 0x8D: // Lighting effects (CGameEffectVisualSpellHit) / Visual spell hit
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Effect",
          new String[]{"Necromancy air", "Necromancy earth", "Necromancy water",
                       "", "Alteration air", "Alteration earth",
                       "Alteration water", "", "Enchantment air",
                       "Enchantment earth", "Enchantment water", "",
                       "Abjuration air", "Abjuration earth", "Abjuration water",
                       "", "Illusion air", "Illusion earth", "Illusion water",
                       "", "Conjure air", "Conjure earth", "Conjure water", "",
                       "Invocation air", "Invocation earth", "Invocation water",
                       "", "Divination air", "Divination earth",
                       "Divination water", "", "Mushroom fire", "Mushroom gray",
                       "Mushroom green", "Shaft fire", "Shaft light",
                       "Shaft white", "Hit door", "Hit finger of death"}));
        break;

      case 0x8E: // Display portrait icon (CGameEffectPortraitIcon)
      case 0xA9: // Prevent portrait icon (CGameEffectImmunityToPortraitIcon)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Icon", s_poricon));
        break;

      case 0x8F: // Create item in slot (CGameEffectReplaceItem)
        s.add(new IdsBitmap(buffer, offset, 4, "Slot", "SLOTS.IDS"));
        s.add(new Unknown(buffer, offset + 4, 4));
        restype = "ITM";
        break;

      case 0x92: // Cast spell (CGameEffectCastSpell)
      case 0x94: // Cast spell at point (CGameEffectCastSpellPoint)
        s.add(new DecNumber(buffer, offset, 4, "Cast at level"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Cast instantly?", s_noyes));
        restype = "SPL";
        break;

      case 0x93: // Learn spell (CGameEffectLearnSpell)
      case 0xAB: // Give innate ability (CGameEffectAddInnateAbility)
      case 0xAC: // Remove spell (CGameEffectRemoveInnateAbility) / Remove innate ability
        restype = "SPL";
        break;

      case 0x97: // Replace self (CGameEffectReplaceSelf)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Replacement method",
          new String[]{"Remove silently", "Remove via chunked death",
                       "Remove via normal death", "Don't remove"}));
        restype = "CRE";
        break;

      case 0x98: // Play movie (CGameEffectPlayMovie)
        restype = "MVE";
        break;

      case 0x9F: // Mirror image effect (CGameEffectMirrorImageRun)
        s.add(new DecNumber(buffer, offset, 4, "# images"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0xA6: // Magic resistance bonus (CGameEffectResistanceToMagic)
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
          new String[]{"Increment", "Set"}));
        break;

      case 0xAA: // Play damage animation (CGameEffectDamageVisualEffect)
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Animation",
          new String[]{"Blood (behind)", "Blood (front)", "Blood (left)",
                       "Blood (right)", "Fire 1", "Fire 2", "Fire 3",
                       "Electricity 1", "Electricity 2", "Electricity 3"}));
        break;

      case 0xAE: // Play sound (CGameEffectPlaySound)
        restype = "WAV";
        break;

      case 0xB1: // Use EFF file (CGameEffectApplyEffect)
        s.add(new IDSTargetEffect(buffer, offset));
        restype = "EFF";
        break;

      case 0xB4: // Disallow item (CGameEffectRestrictEquipItem)
        s.add(new StringRef(buffer, offset, "String"));
        s.add(new Unknown(buffer, offset + 4, 4));
        restype = "ITM";
        break;
    }

    if (s.size() != initSize)
      return restype;

    // ---------------
    // Baldur's Gate I & TotSC Effects
    // ---------------
    if (gameid == ResourceFactory.ID_BG1 ||
        gameid == ResourceFactory.ID_BG1TOTSC ||
        gameid == ResourceFactory.ID_DEMO) {
      switch (effectType) {
        case 0x0: // AC bonus (CGameEffectAC)
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to", s_actype));
          break;

        case 0x1: // Modify attacks per round (CGameEffectAttackNo)
          s.add(new Bitmap(buffer, offset, 4, "Value",
            new String[]{"0 attacks per round", "1 attack per round",
                         "2 attacks per round", "3 attacks per round",
                         "4 attacks per round", "5 attacks per round",
                         "1 attack per 2 rounds", "3 attacks per 2 rounds",
                         "5 attacks per 2 rounds", "7 attacks per 2 rounds",
                         "9 attacks per 2 rounds"}));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0xC: // Damage (CGameEffectDamage)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Mode",
            new String[]{"Normal", "Set to value", "Set to %"}));
          s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
          break;

        case 0xF: // Dexterity bonus (CGameEffectDEX)
        case 0x16: // Luck bonus (CGameEffectLuck)
        case 0x2C: // Strength bonus (CGameEffectSTR)
        case 0x49: // Attack damage bonus (CGameEffectDamageMod)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0x11: // Current HP bonus (CGameEffectHeal)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Modifier type",
            new String[]{"Increment", "Set", "Increment % of"}));
          s.add(new Flag(buffer, offset + 6, 2, "Heal flags",
            new String[]{"No flags set", "Raise dead"}));
          break;

        case 0x19: // Poison (CGameEffectPoison)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Poison type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds",
                         "Variable per amount seconds"}));
          break;

        case 0x3C: // Casting failure (CGameEffectCastingFailure)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Failure type",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0x42: // Translucency (CGameEffectTranslucent)
          s.add(new DecNumber(buffer, offset, 4, "Fade amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x43: // Summon creature (CGameEffectSummon)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Allegiance",
            new String[]{"Match target", "Match target", "From CRE file",
                         "Match target", "From CRE file", "Hostile"}));
          restype = "CRE";
          break;

        case 0x4E: // Disease (CGameEffectDisease)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Disease type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds", "Strength", "Dexterity",
                         "Constitution", "Intelligence", "Wisdom", "Charisma",
                         "Slow target"}));
          break;

        case 0x52: // Set AI script (CGameEffectSetAIScript)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Script level", "SCRLEV.IDS"));
          restype = "BCS";
          break;

        case 0x62: // Regeneration (CGameEffectRegeneration)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Regeneration type",
            new String[]{"1 HP per second", "1 HP per second",
                         "Amount HP per second", "1 HP per amount seconds",
                         "Variable per amount seconds"}));
          break;

        case 0x83: // Chant (non-cumulative) (CGameEffectNon_CumulativeChant)
        case 0xBD: // Increase casting speed factor (CGameEffectMentalSpeed)
        case 0xBE: // Increase attack speed factor (CGameEffectPhysicalSpeed)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x90: // Disable button (CGameEffectDisableButton)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0x91: // Disable spellcasting (CGameEffectDisableSpellType)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0xBA: // Move creature (CGameEffectJumpToArea)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Orientation", Actor.s_orientation));
          restype = "ARE";
          break;

        case 0xBB: // Set local variable (CGameEffectSetLocalVariable)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "String";
          break;

        case 0xBC: // Increase spells cast per round (CGameEffectAuraCleansing)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Cleanse aura?", s_noyes));
          break;

        case 0xBF: // Casting level bonus (CGameEffectCastingLevelBonus)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest"}));
          break;
      }
    }

    // ---------------
    // Planescape: Torment Effects
    // ---------------
    else if (gameid == ResourceFactory.ID_TORMENT) {
      switch (effectType) {
        case 0x0: // AC bonus
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to", s_actype));
          break;

        case 0x1: // Modify attacks per round
          s.add(new Bitmap(buffer, offset, 4, "Value",
            new String[]{"0 attacks per round", "1 attack per round",
                         "2 attacks per round", "3 attacks per round",
                         "4 attacks per round", "5 attacks per round",
                         "1 attack per 2 rounds", "3 attacks per 2 rounds",
                         "5 attacks per 2 rounds", "7 attacks per 2 rounds",
                         "9 attacks per 2 rounds"}));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0xC: // Damage
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Mode",
            new String[]{"Normal", "Set to value", "Set to %"}));
          s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
          break;

        case 0xF: // Dexterity bonus
        case 0x16: // Luck bonus
        case 0x2C: // Strength bonus
        case 0x49: // Attack damage bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0x11: // Current HP bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Modifier type",
            new String[]{"Increment", "Set", "Increment % of"}));
          s.add(new Flag(buffer, offset + 6, 2, "Heal flags",
            new String[]{"No flags set", "Raise dead"}));
          break;

        case 0x19: // Poison
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Poison type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds",
                         "Variable per amount seconds"}));
          break;

        case 0x3C: // Casting failure
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Failure type",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0x42: // Translucency
          s.add(new DecNumber(buffer, offset, 4, "Fade amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x43: // Summon creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Allegiance",
            new String[]{"Match target", "Match target", "From CRE file",
                         "Match target", "From CRE file", "Hostile"}));
          restype = "CRE";
          break;

        case 0x4E: // Disease
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Disease type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds", "Strength", "Dexterity",
                         "Constitution", "Intelligence", "Wisdom", "Charisma",
                         "Slow target"}));
          break;

        case 0x62: // Regeneration
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Regeneration type",
            new String[]{"1 HP per second", "1 HP per second",
                         "Amount HP per second", "1 HP per amount seconds",
                         "Variable per amount seconds"}));
          break;

        case 0x83: // Chant (non-cumulative)
        case 0xCB: // Curse
        case 0xCC: // Prayer
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x90: // Disable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0x91: // Disable spellcasting
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0xBA: // Set status
          s.add(new Bitmap(buffer, offset, 4, "Action", new String[]{"Clear", "Set"}));
          s.add(new IdsFlag(buffer, offset + 4, 4, "State", "STATE.IDS"));
          break;

        case 0xBB: // Play BAM file (single/dual)
          s.add(new UnsignDecNumber(buffer, offset, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 3, 1));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "BAM";
          break;

        case 0xBC: // Play BAM file
        case 0xBD: // Play BAM file 2
        case 0xBE: // Play BAM file 3
        case 0xBF: // Play BAM file 4
          final LongIntegerHashMap<String> m_playbam = new LongIntegerHashMap<String>();
          m_playbam.put(0L, "Non-sticky, not 3D");
          m_playbam.put(1L, "Random placement, not 3D");
          m_playbam.put(528384L, "Sticky, 3D");
          m_playbam.put(1179648L, "RGB transparent");
          m_playbam.put(1183744L, "RGB transparent, 3D, sticky");
          m_playbam.put(3280896L, "RGB transparent, 3D");
          s.add(new UnsignDecNumber(buffer, offset, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 3, 1));
          s.add(new HashBitmap(buffer, offset + 4, 4, "Properties", m_playbam));
          restype = "BAM";
          break;

        case 0xC0: // Hit point transfer
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Direction",
            new String[]{"Source to target", "Target to source", "Swap HP",
                         "Caster to target"}));
          s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
          break;

        case 0xC1: // Shake screen
          s.add(new DecNumber(buffer, offset, 4, "Strength"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xC2: // Flash screen
          s.add(new UnsignDecNumber(buffer, offset, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 3, 1));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xC3: // Tint screen
          s.add(new UnsignDecNumber(buffer, offset, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 3, 1));
          s.add(new DecNumber(buffer, offset + 4, 4, "Method"));
          break;

        case 0xC4: // Special spell hit
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect",
            new String[]{"Adder's kiss", "Ball lightning", "Fizzle"}));
          break;

        case 0xC9: // Play BAM with effects
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect",
            new String[]{"Cloak of warding", "Shield", "Black-barbed shield",
                         "Pain mirror", "Guardian mantle", "",
                         "Enoll eva's duplication", "Armor", "Antimagic shell",
                         "", "", "Flame walk", "Protection from evil",
                         "Conflagration", "Infernal shield",
                         "Submerge the will", "Balance in all things"}));
          restype = "BAM";
          break;

        case 0xCD: // Move view to target
          s.add(new DecNumber(buffer, offset, 4, "Speed"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xCE: // Embalm
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Embalming type",
            new String[]{"Normal", "Greater"}));
          break;
      }
    }

    // ---------------
    // Icewind Dale & HoW Effects
    // ---------------
    else if (gameid == ResourceFactory.ID_ICEWIND ||
             gameid == ResourceFactory.ID_ICEWINDHOW ||
             gameid == ResourceFactory.ID_ICEWINDHOWTOT) {
      switch (effectType) {
        case 0x0: // AC bonus
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to", s_actype));
          break;

        case 0x1: // Modify attacks per round
        case 0x16: // Luck bonus
        case 0x17: // Morale bonus
        case 0xEE: // All saving throws bonus
        case 0x10A: // Movement rate modifier
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0x3: // Berserk
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Berserk type",
            new String[]{"Normal", "Blood rage"}));
          break;

        case 0xC: // Damage
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Mode",
            new String[]{"Normal", "Set to value", "Set to %", "Percentage"}));
          s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
          break;

        case 0xF: // Dexterity bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Set", "Set % of", "Cat's grace"}));
          break;

        case 0x11: // Current HP bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Modifier type",
            new String[]{"Increment", "Set", "Increment % of"}));
          s.add(new Flag(buffer, offset + 6, 2, "Heal flags",
            new String[]{"No flags set", "Raise dead"}));
          break;

        case 0x19: // Poison
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Poison type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds",
                         "Variable per amount seconds"}));
          break;

        case 0x2C: // Strength bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Set", "Set % of", "Wizard strength"}));
          break;

        case 0x27: // Sleep
        case 0x11D: // Force sleep
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Wake on damage?", s_yesno));
          break;

        case 0x3C: // Casting failure
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Failure type",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0x42: // Translucency
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Visual effect",
            new String[]{"Draw instantly", "Fade in", "Fade out"}));
          break;

        case 0x43: // Summon creature
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon animation",
            new String[]{"No animation", "Monster summoning circle",
                         "Animal summoning circle", "Earth summoning circle",
                         "Fire summoning circle", "Water summoning circle", "",
                         "Puff of smoke"}));
          restype = "CRE";
          break;

        case 0x49: // Attack damage bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Fire damage", "Cold damage",
                         "Electricity damage", "Acid damage", "Magic damage",
                         "Poison damage", "Slashing damage", "Piercing damage",
                         "Crushing damage", "Missile damage"}));
          break;

        case 0x4E: // Disease
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Disease type",
            new String[]{"1 damage per second", "Amount damage per round",
                         "Amount damage per second",
                         "1 damage per amount seconds", "Strength", "Dexterity",
                         "Constitution", "Intelligence", "Wisdom", "Charisma",
                         "Slow target", "Mold touch"}));
          break;

        case 0x52: // Set AI script
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Script level", "SCRLEV.IDS"));
          restype = "BCS";
          break;

        case 0x62: // Regeneration
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Regeneration type",
            new String[]{"1 HP per second", "1 HP per second",
                         "Amount HP per second", "1 HP per amount seconds",
                         "Amount HP per round"}));
          break;

        case 0x77: // Mirror image
          s.add(new DecNumber(buffer, offset, 4, "# images"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x7C: // Teleport
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Behavior",
            new String[]{"Normal", "Source to target", "Return to start",
                         "Exchange with target"}));
          break;

        case 0x83: // Chant (non-cumulative)
        case 0xF4: // Prayer
        case 0xF9: // Recitation
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Prayer type",
            new String[]{"Beneficial", "Detrimental"}));
          break;

        case 0x90: // Disable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0x91: // Disable spellcasting
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0xBA: // Move creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Orientation", "DIR.IDS"));
          restype = "ARE";
          break;

        case 0xBB: // Set local variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "String";
          break;

        case 0xBC: // Increase spells cast per round
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Cleanse aura?", s_noyes));
          break;

        case 0xBD: // Increase casting speed factor
        case 0xBE: // Increase attack speed factor
        case 0xEA: // Snilloc's snowball swarm
        case 0xED: // Magical stone
        case 0xEF: // Slow poison
        case 0xF5: // Bad prayer
        case 0xFA: // Bad recitation
        case 0xFC: // Sol's searing orb
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xCE: // Protection from spell
        case 0x122: // Display spell immunity string
          final String s_cretype[] = {"Anyone", "Undead", "Not undead",
            "Fire-dwelling", "Not fire-dwelling", "Humanoid", "Not humanoid",
            "Animal", "Not animal", "Elemental", "Not elemental", "Fungus",
            "Not fungus", "Huge creature", "Not huge creature", "Elf",
            "Not elf", "Umber hulk", "Not umber hulk", "Half-elf",
            "Not half-elf", "Humanoid or animal", "Not humanoid or animal",
            "Blind", "Not blind", "Cold-dwelling", "Not cold-dwelling", "Golem",
            "Not golem", "Minotaur", "Not minotaur", "Undead or fungus",
            "Not undead or fungus", "Good", "Not good", "Neutral",
            "Not neutral", "Evil", "Not evil", "Paladin", "Not paladin",
            "Same moral alignment as source",
            "Not same moral alignment as source", "Source", "Not source",
            "Water-dwelling", "Not water-dwelling", "Breathing",
            "Not breathing", "Allies", "Not allies", "Enemies", "Not enemies",
            "Fire or cold dwelling", "Not fire or cold dwelling", "Unnatural",
            "Not unnatural", "Male", "Not male", "Lawful", "Not lawful",
            "Chaotic", "Not chaotic", "Evasion check", "Orc", "Not orc", "Deaf",
            "Not deaf", "Summoned creature", "Not summoned creature",
            "Mind flayer", "Not mind flayer", "Silenced", "Not silenced",
            "Intelligence less than", "Intelligence greater than",
            "Intelligence less than or equal to",
            "Intelligence greater than or equal to", "Skald", "Not skald",
            "Near enemies", "Not near enemies", "Drow", "Not drow",
            "Gray dwarf", "Not gray dwarf", "Daytime", "Not daytime", "Outdoor",
            "Not outdoor", "Keg", "Not keg", "Outsider", "Not outsider"};
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Creature type", s_cretype));
          restype = "SPL";
          break;

        case 0xD0: // Minimum HP
          s.add(new DecNumber(buffer, offset, 4, "HP amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDA: // Stoneskin effect
          s.add(new DecNumber(buffer, offset, 4, "# skins"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xE8: // Creature RGB color fade
          s.add(new Unknown(buffer, offset, 1));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 4, 2));
          s.add(new DecNumber(buffer, offset + 6, 2, "Speed"));
          break;

        case 0xE9: // Show visual effect
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_visuals));
          break;

        case 0xEB: // Show casting glow
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Glow",
            new String[]{"None", "Abjuration", "Conjuration", "Divination",
                         "Enchantment", "Illusion", "Invocation", "Necromancy",
                         "Alteration"}));
          break;

        case 0xF0: // Summon creature 2
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon type",
            new String[]{"Monster summoning 1", "Monster summoning 2",
                         "Monster summoning 3", "Monster summoning 4",
                         "Monster summoning 5", "Monster summoning 6",
                         "Monster summoning 7", "Animal summoning 1",
                         "Animal summoning 2", "Animal summoning 3",
                         "Summon insects", "Creeping doom", "Malavon summon"}));
          break;

        case 0xF1: // Vampiric touch
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Direction",
            new String[]{"Target to source", "Source to target"}));
          break;

        case 0xF2: // Show visual overlay
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Overlay",
            new String[]{"Globe of invulnerability", "Shroud of flame",
                         "Antimagic shell", "Otiluke's resilient sphere",
                         "Protection from normal missiles", "Cloak of fear",
                         "Entropy shield", "Fire aura", "Frost aura",
                         "Insect plague", "Storm shell", "Shield of lathander",
                         "Greater shield of lathander", "Seven eyes"}));
          break;

        case 0xF3: // Animate dead
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Undead type",
            new String[]{"Normal", "Lich"}));
          break;

        case 0xF6: // Summon creature 3
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Creature type",
            new String[]{"Lizard man", "Troll", "Shadow", "Invisible stalker",
                         "Fire elemental (wizard)", "Earth elemental (wizard)",
                         "Water elemental (wizard)", "Fire elemental (priest)",
                         "Earth elemental (priest)", "Water elemental (priest)",
                         "Malavon earth elemental"}));
          break;

        case 0xF7: // Beltyn's burning blood
        case 0x108: // Static charge
        case 0x109: // Cloak of fear
        case 0x116: // Shroud of flame
          s.add(new DecNumber(buffer, offset, 4, "# hits"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF8: // Summon shadow
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Shadow type",
            new String[]{"Shadow", "Demishadow", "Shade"}));
          break;

        case 0xFB: // Lich touch
        case 0x100: // Umber hulk gaze
        case 0x117: // Animal rage
          s.add(new DecNumber(buffer, offset, 4, "# seconds"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xFD: // Bonus AC vs. weapons
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to",
            new String[]{"All weapons", "Blunt weapons", "Missile weapons",
                         "Piercing weapons", "Slashing weapons",
                         "Set base AC to value"}));
          break;

        case 0xFE: // Dispel specific spell
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Dispel type",
            new String[]{"All effects", "Equipped effects only",
                         "Limited effects only"}));
          restype = "SPL";
          break;

        case 0xFF: // Salamander aura
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Aura type",
            new String[]{"Fire", "Frost"}));
          break;

        case 0x103: // Summon creatures with cloud
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon type",
            new String[]{"Default", "Ally", "Hostile", "Forced", "Genie"}));
          restype = "CRE";
          break;

        case 0x105: // Immunity to effect and string
        case 0x114: // Remove effect by type
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          break;

        case 0x11B: // Float text
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Display type",
            new String[]{"String reference", "Cynicism"}));
          break;

        case 0x128: // Set global variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;
      }
    }

    // -----------------------
    // Baldur's Gate 2 & ToB Effects
    // -----------------------
    else if (gameid == ResourceFactory.ID_BG2 ||
             gameid == ResourceFactory.ID_BG2TOB ||
             gameid == ResourceFactory.ID_TUTU) {
      switch (effectType) {
        case 0x0: // AC bonus (CGameEffectAC)
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to", s_actype));
          break;

        case 0x1: // Modify attacks per round (CGameEffectAttackNo)
          s.add(new Bitmap(buffer, offset, 4, "Value",
            new String[]{"0 attacks per round", "1 attack per round",
                         "2 attacks per round", "3 attacks per round",
                         "4 attacks per round", "5 attacks per round",
                         "1 attack per 2 rounds", "3 attacks per 2 rounds",
                         "5 attacks per 2 rounds", "7 attacks per 2 rounds",
                         "9 attacks per 2 rounds"}));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0xC: // Damage (CGameEffectDamage)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Mode",
            new String[]{"Normal", "Set to value", "Set to %", "Percentage"}));
          s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
          break;

        case 0x11: // Current HP bonus (CGameEffectHeal)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Modifier type",
            new String[]{"Increment", "Set", "Increment % of"}));
          s.add(new Flag(buffer, offset + 6, 2, "Heal flags",
            new String[]{"No flags set", "Raise dead",
                         "Remove limited effects"}));
          break;

        case 0xF: // Dexterity bonus (CGameEffectDEX)
        case 0x16: // Luck bonus (CGameEffectLuck)
        case 0x2C: // Strength bonus (CGameEffectSTR)
        case 0x49: // Attack damage bonus (CGameEffectDamageMod)
        case 0x106: // Visual range bonus (CGameEffectVisualRange)
        case 0x107: // Backstab bonus (CGameEffectBackStabMod)
        case 0x113: // Hide in shadows bonus (CGameEffectHideInShadows)
        case 0x114: // Detect illusions bonus (CGameEffectDetectIllusion)
        case 0x115: // Set traps bonus (CGameEffectSetTrap)
        case 0x116: // THAC0 bonus (CGameEffectHitMod)
        case 0x119: // Wild surge bonus (CGameEffectSurgeMod)
        case 0x11C: // Melee THAC0 bonus (CGameEffectMeleeTHAC0Bonus)
        case 0x11D: // Melee weapon damage bonus (CGameEffectMeleeDamageBonus)
        case 0x11E: // Missile weapon damage bonus (CGameEffectMissileDamageBonus)
        case 0x120: // Fist THAC0 bonus (CGameEffectFistTHAC0Bonus)
        case 0x121: // Fist damage bonus (CGameEffectFistDamageBonus)
        case 0x131: // Off-hand THAC0 bonus (CGameEffectThac0Left)
        case 0x132: // Main hand THAC0 bonus (CGameEffectThac0Right)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0x3C: // Casting failure (CGameEffectCastingFailure)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Failure type",
            new String[]{"Wizard", "Priest", "Innate", "Wizard (dead magic)",
                         "Priest (dead magic)", "Innate (dead magic)"}));
          break;

        case 0x42: // Translucency (CGameEffectTranslucent)
          s.add(new DecNumber(buffer, offset, 4, "Fade amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x43: // Summon creature (CGameEffectSummon)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Allegiance",
            new String[]{"Match target", "Match target", "From CRE file",
                         "Match target", "From CRE file", "Hostile"}));
          restype = "CRE";
          break;

        case 0x4E: // Disease (CGameEffectDisease)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Disease type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds", "Strength", "Dexterity",
                         "Constitution", "Intelligence", "Wisdom", "Charisma",
                         "Slow target"}));
          break;

        case 0x52: // Set AI script (CGameEffectSetAIScript)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Script level", "SCRLEV.IDS"));
          restype = "BCS";
          break;

        case 0x62: // Regeneration (CGameEffectRegeneration)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Regeneration type",
            new String[]{"1 HP per second", "1 HP per second",
                         "Amount HP per second", "1 HP per amount seconds",
                         "Variable per amount seconds"}));
          break;

        case 0x83: // Chant (non-cumulative) (CGameEffectNon_CumulativeChant)
        case 0xBD: // Increase casting speed factor (CGameEffectMentalSpeed)
        case 0xBE: // Increase attack speed factor (CGameEffectPhysicalSpeed)
        case 0x12D: // Critical threat range bonus (CGameEffectCriticalHitBonus)
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x90: // Disable button (CGameEffectDisableButton)
        case 0x117: // Enable button (CGameEffectEnableButton)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0x91: // Disable spellcasting (CGameEffectDisableSpellType)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest", "Innate"}));
          break;

        case 0xB8: // No collision detection (CGameEffectSetDoNotJump)
        case 0xBC: // Increase spells cast per round (CGameEffectAuraCleansing)
        case 0xC1: // Invisibility detection (CGameEffectSeeInvisible)
        case 0xC2: // Ignore dialogue pause (CGameEffectIgnoreDialogPause)
        case 0xF5: // Unknown (F5) (CGameEffectCheckForBerserk)
        case 0xF6: // Berserk effect (CGameEffectBerserkStage1)
        case 0xF7: // Attack nearest creature (CGameEffectBerserkStage2)
        case 0x123: // Disable visual effects (CGameEffectPreventSpellProtectionEffects)
        case 0x124: // Immunity to backstab (CGameEffectImmunityToBackstab)
        case 0x125: // Set persistent AI (CGameEffectPreventAISlowDown)
        case 0x127: // Disable permanent death (CGameEffectNoPermanentDeath)
        case 0x129: // Immunity to turn undead (CGameEffectImmunityToTurnUndead)
        case 0x12C: // Modify collision behavior (CGameEffectNPCBump)
        case 0x12E: // Can use any item (CGameEffectUseAnyItem)
        case 0x12F: // Backstab every hit (CGameEffectAssassination)
        case 0x134: // Immunity to tracking (CGameEffectImmuneToTracking)
        case 0x136: // Immunity to time stop (CGameEffectImmunityToTimeStop)
        case 0x13B: // Remove animation (CGameEffectDoNotDraw)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new DecNumber(buffer, offset + 4, 4, "Stat value"));
          break;

        case 0xBA: // Move creature (CGameEffectJumpToArea)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Orientation", Actor.s_orientation));
          restype = "ARE";
          break;

        case 0xBB: // Set local variable (CGameEffectSetLocalVariable)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "String";
          break;

        case 0xBF: // Casting level bonus (CGameEffectCastingLevelBonus)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest"}));
          break;

        case 0xC3: // Drain HP on death (CGameEffectHitPointsOnDeath)
          s.add(new DecNumber(buffer, offset, 4, "HP amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xC5: // Physical mirror (CGameEffectBounceProjectile)
          s.add(new Unknown(buffer, offset, 4));
          if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
            s.add(new IdsBitmap(buffer, offset + 4, 4, "Projectile", "PROJECTL.IDS"));
          else
            s.add(new DecNumber(buffer, offset + 4, 4, "Projectile"));
          break;

        case 0xC6: // Reflect specified effect (CGameEffectBounceEffect)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          break;

        case 0xC7: // Reflect spell level (CGameEffectBounceLevel)
          s.add(new DecNumber(buffer, offset, 4, "Spell level"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xC8: // Spell turning (CGameEffectBounceLevelDecrement)
        case 0xC9: // Spell deflection (CGameEffectImmunityLevelDecrement)
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Spell level"));
          break;

        case 0xCA: // Reflect spell school (CGameEffectBounceSchool)
        case 0xCC: // Protection from spell school (CGameEffectImmunitySchool)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell school", s_school));
          break;

        case 0xCB: // Reflect spell type (CGameEffectBounceSecondaryType)
        case 0xCD: // Protection from spell type (CGameEffectImmunitySecondaryType)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell type", SplResource.s_category));
          break;

        case 0xCE: // Protection from spell (CGameEffectImmunitySpell)
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "SPL";
          break;

        case 0xCF: // Reflect specified spell (CGameEffectBounceSpell)
        case 0xFB: // Change bard song (CGameEffectBardSong)
        case 0xFC: // Set trap (CGameEffectSetSnare)
        case 0x100: // Spell sequencer (CGameEffectSequencerInstance)
        case 0x102: // Activate spell sequencer (CGameEffectSequencerFire)
        case 0x10A: // Remove protection from spell (CGameEffectRemoveSpellImmunity)
        case 0x139: // High-level ability (CGameEffectHighLevelAbility)
          restype = "SPL";
          break;

        case 0xD0: // Minimum HP (CGameEffectMinHitPoints)
          s.add(new DecNumber(buffer, offset, 4, "HP amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xD6: // Select spell (CGameEffectSecondaryCastList)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Show",
            new String[]{"All spells", "Known spells"}));
          restype = "2DA";
          break;

        case 0xD7: // Play visual effect (CGameEffectVisualEffect)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Play where?",
            new String[]{"Over target (unattached)", "Over target (attached)",
                         "At target point"}));
          restype = "VVC";
          break;

        case 0xD8: // Level drain (CGameEffectLevelDrain)
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDA: // Stoneskin effect (CGameEffectStoneSkins)
        case 0x13A: // Stoneskin protection (CGameEffectStoneSkinsGolem)
          s.add(new DecNumber(buffer, offset, 4, "# skins"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDB: // Attack roll penalty (CGameEffectProtectionCircle)
        case 0xEE: // Disintegrate (CGameEffectDisintegrate)
          s.add(new IDSTargetEffect(buffer, offset));
          break;

        case 0xDC: // Remove spell school protections (CGameEffectDispelSchool)
        case 0xE5: // Remove protection by school (CGameEffectDispelSchoolOne)
          s.add(new DecNumber(buffer, offset, 4, "Maximum level"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell school", s_school));
          break;

        case 0xDD: // Remove spell type protections (CGameEffectDispelSecondaryType)
        case 0xE6: // Remove protection by type (CGameEffectDispelSecondaryTypeOne)
          s.add(new DecNumber(buffer, offset, 4, "Maximum level"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell type", SplResource.s_category));
          break;

        case 0xDE: // Teleport field (CGameEffectRandomTeleport)
          s.add(new DecNumber(buffer, offset, 4, "Maximum range"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDF: // Spell school deflection (CGameEffectImmunitySchoolDecrement)
        case 0xE3: // Spell school turning (CGameEffectBounceSchoolDecrement)
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell school", s_school));
          break;

        case 0xE2: // Spell type deflection (CGameEffectImmunitySecondaryTypeDecrement)
        case 0xE4: // Spell type turning (CGameEffectBounceSecondaryTypeDecrement)
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell type", SplResource.s_category));
          break;

        case 0xE8: // Cast spell on condition (CGameEffectContingencyInstance)
          s.add(new Bitmap(buffer, offset, 4, "Target",
            new String[]{"Caster", "Last hit by", "Nearest enemy"}));
          s.add(new Bitmap(buffer, offset + 4, 4, "Condition",
            new String[]{"Target hit", "Enemy sighted", "HP below 50%",
                         "HP below 25%", "HP below 10%", "If helpless",
                         "If poisoned", "Every round when attacked",
                         "Every round when hit", "Every round"}));
          restype = "SPL";
          break;

        case 0xE9: // Modify proficiencies (CGameEffectProficiency)
          s.add(new DecNumber(buffer, offset, 4, "# stars"));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Proficiency", "STATS.IDS"));
          break;

        case 0xEA: // Create contingency (CGameEffectContingencyStart)
          s.add(new DecNumber(buffer, offset, 4, "Maximum spell level"));
          s.add(new DecNumber(buffer, offset + 4, 2, "# spells"));
          s.add(new Bitmap(buffer, offset + 6, 2, "Trigger type",
            new String[]{"Chain contingency", "Contingency",
                         "Spell sequencer"}));
          break;

        case 0xEB: // Wing buffet (CGameEffectPushPull)
          s.add(new DecNumber(buffer, offset, 4, "Strength"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Direction",
            new String[]{"", "Away from target point", "Away from source",
                         "Toward target point", "Toward source"}));
          break;

        case 0xEC: // Project image (CGameEffectCopySelf)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Image type",
            new String[]{"", "Mislead", "Project image", "Simulacrum"}));
          break;

        case 0xED: // Set image type (CGameEffectPuppetMaster)
          s.add(new DecNumber(buffer, offset, 4, "Puppet master"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Puppet type",
            new String[]{"", "Mislead", "Project image", "Simulacrum"}));
          break;

        case 0xEF: // Farsight (CGameEffectClairvoyance)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Can view unexplored?", s_noyes));
          break;

        case 0xF0: // Remove portrait icon (CGameEffectRemovePortraitIcon)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Icon", s_poricon));
          break;

        case 0xF1: // Control creature (CGameEffectCharm)
          s.add(new IdsBitmap(buffer, offset, 4, "Creature type", "GENERAL.IDS"));
          s.add(new Flag(buffer, offset + 4, 4, "Charm flags",
            new String[]{"No flags set", "Turn hostile afterward", "Dire charm",
                         "Controlled by cleric"}));
          break;

        case 0xF3: // Drain item charges (CGameEffectDrainChargeFromAllItems)
          s.add(new Bitmap(buffer, offset, 4, "Include weapons?", s_noyes));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF4: // Drain wizard spells (CGameEffectRemoveRandomSpell)
          s.add(new DecNumber(buffer, offset, 4, "# spells"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF8: // Melee hit effect (CGameEffectMeleeEffect)
        case 0xF9: // Ranged hit effect (CGameEffectRangeEffect)
          restype = "EFF";
          break;

        case 0xFA: // Maximum damage each hit (CGameEffectDamageLuck)
          s.add(new DecNumber(buffer, offset, 4, "Stat value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xFD: // Set automap note (CGameAddMapNote)
        case 0xFE: // Remove automap note (CGameRemoveMapNote)
        case 0x10B: // Disable display string (CGameEffectImmunityToDisplayString)
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xFF: // Create item (days) (CGameEffectCreateItem)
          s.add(new DecNumber(buffer, offset, 4, "# items in stack"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "ITM";
          break;

        case 0x101: // Create spell sequencer (CGameEffectSequencerStart)
          s.add(new DecNumber(buffer, offset, 4, "Maximum level"));
          s.add(new DecNumber(buffer, offset + 4, 4, "# spells"));
          break;

        case 0x103: // Spell trap (CGameEffectSpellTrapLevelDecrement)
          s.add(new DecNumber(buffer, offset, 4, "# spells"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Spell level"));
          break;

        case 0x105: // Wondrous recall (CGameEffectRememorizeSpell)
          s.add(new DecNumber(buffer, offset, 4, "Spell level"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest"}));
          break;

        case 0x108: // Drop item (CGameEffectRandomDrop)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Only quick weapons?", s_noyes));
          break;

        case 0x109: // Set global variable (CGameEffectSetGlobal)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Set", "Increment"}));
          restype = "String";
          break;

        case 0x10D: // Shake screen (CGameEffectScreenShake)
          s.add(new DecNumber(buffer, offset, 4, "Strength"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x110: // Use EFF file on condition (CGameEffectRepeatingApplyEffect)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Frequency",
            new String[]{"Once per second", "", "Value per second",
                         "Once per value seconds",
                         "Variable per value rounds"}));
          restype = "EFF";
          break;

        case 0x118: // Wild magic (CGameEffectForceSurge)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Affect",
            new String[]{"", "Next spell", "Every spell"}));
          break;

        case 0x11A: // Modify script state (CGameEffectScriptingState)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "State", "STATS.IDS", 156));
          break;

        case 0x11B: // Use EFF file as curse (CGameEffectCurseApplyEffect)
          s.add(new IDSTargetEffect(buffer, offset));
          restype = "EFF";
          break;

        case 0x122: // Change title (CGameEffectClassStringOverride)
          s.add(new StringRef(buffer, offset, "Title"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Change where?",
            new String[]{"Records screen", "Class name"}));
          break;

        case 0x126: // Set existence delay (CGameEffectExistanceDelayOverride)
          s.add(new DecNumber(buffer, offset, 4, "Interval override"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x128: // Immunity to specific animation (CGameEffectImmunityToVisualEffect)
          restype = "VVC";
          break;

        case 0x133: // Tracking (CGameEffectTracking)
          s.add(new DecNumber(buffer, offset, 4, "Range"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x135: // Set variable (CGameEffectSetLocalExternal)
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Global or local?",
            new String[]{"Both", "Local only"}));
          restype = "String";
          break;

        case 0x13D: // Haste 2 (CGameEffectHaste2)
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Haste type",
            new String[]{"Normal", "Improved", "Movement rate only"}));
          break;
      }
    }

    // -----------------------
    // Icewind Dale 2 Effects
    // -----------------------
    else if (gameid == ResourceFactory.ID_ICEWIND2) {
      switch (effectType) {
        case 0x0: // AC bonus
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Bonus to",
            new String[]{"Generic", "Armor", "Deflection", "Shield", "Crushing",
                         "Piercing", "Slashing", "Missile"}));
          break;

        case 0x1: // Modify attacks per round
        case 0x17: // Morale bonus
        case 0xEE: // All saving throws bonus
        case 0x10A: // Movement rate modifier
        case 0x129: // Hide in shadows bonus
        case 0x12A: // Use magic device bonus
        case 0x1B9: // Force slow
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0x3: // Berserk
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Berserk type",
            new String[]{"Normal", "Constant", "Blood rage"}));
          break;

        case 0xC: // Damage
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Mode",
            new String[]{"Normal", "Set to value", "Set to %", "Save for half"}));
          s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
          break;

        case 0xF: // Dexterity bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Set", "Set % of", "Cat's grace"}));
          break;

        case 0x11: // Current HP bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 2, "Modifier type",
            new String[]{"Increment", "Set", "Increment % of", "Lay on hands",
                         "Wholeness of body", "Lathander's renewal"}));
          s.add(new Flag(buffer, offset + 6, 2, "Heal flags",
            new String[]{"No flags set", "Raise dead"}));
          break;

        case 0x16: // Luck bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Lucky streak", "Fortune's favorite"}));
          break;

        case 0x18: // Panic
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Panic type",
            new String[]{"Normal", "Harpy wail"}));
          break;

        case 0x19: // Poison
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Poison type",
            new String[]{"1 damage per second", "1 damage per second",
                         "Amount damage per second",
                         "1 damage per amount seconds",
                         "Amount damage per round",
                         "Snake bite", "Envenomed weapon"}));
          break;

        case 0x27: // Sleep
        case 0x11D: // Force sleep
        case 0x1A3: // Unconsciousness
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Wake on damage?", s_yesno));
          break;

        case 0x2C: // Strength bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Set", "Set % of", "Wizard strength"}));
          break;

        case 0x2D: // Stun
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Stun type",
            new String[]{"Normal", "Unstun on damage", "Power word, stun"}));
          break;

        case 0x3C: // Casting failure
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Arcane", "Divine", "All spells"}));
          break;

        case 0x3D: // Alchemy
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Set", "Mastery"}));
          break;

        case 0x42: // Translucency
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Visual effect",
            new String[]{"Draw instantly", "Fade in", "Fade out"}));
          break;

        case 0x43: // Summon creature
        case 0x19A: // Summon friendly creature
        case 0x19B: // Summon hostile creature
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon animation",
            new String[]{"No animation", "Monster summoning circle",
                         "Animal summoning circle", "Earth summoning circle",
                         "Fire summoning circle", "Water summoning circle", "",
                         "Puff of smoke"}));
          restype = "CRE";
          break;

        case 0x49: // Attack damage bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Increment", "Fire damage", "Cold damage",
                         "Electricity damage", "Acid damage", "Magic damage",
                         "Poison damage", "Slashing damage", "Piercing damage",
                         "Crushing damage", "Missile damage"}));
          break;

        case 0x4E: // Disease
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Disease type",
            new String[]{"1 damage per second", "Amount damage per round",
                         "Amount damage per second",
                         "1 damage per amount seconds", "Strength", "Dexterity",
                         "Constitution", "Intelligence", "Wisdom", "Charisma",
                         "Slow target", "Mold touch", "", "Contagion",
                         "Cloud of pestilence", "Dolorous decay"}));
          break;

        case 0x52: // Set AI script
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Script level", "SCRLEV.IDS"));
          restype = "BCS";
          break;

        case 0x62: // Regeneration
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Regeneration type",
            new String[]{"1 HP per second", "1 HP per second",
                         "Amount HP per second", "1 HP per amount seconds",
                         "Amount HP per round"}));
          break;

        case 0x77: // Mirror image
          s.add(new DecNumber(buffer, offset, 4, "# images"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Image type",
            new String[]{"Normal", "Reflected image"}));
          break;

        case 0x7C: // Teleport
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Behavior",
            new String[]{"Normal", "Source to target", "Return to start",
                         "Exchange with target"}));
          break;

        case 0x83: // Chant (non-cumulative)
        case 0xF4: // Prayer
        case 0xF9: // Recitation
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Prayer type",
            new String[]{"Beneficial", "Detrimental"}));
          break;

        case 0x90: // Disable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button",
            new String[]{"Stealth", "Thieving", "Cast spell", "Quick spell 0",
                         "Quick spell 1", "Quick spell 2", "Quick spell 3",
                         "Quick spell 4", "Quick spell 5", "Quick spell 6",
                         "Quick spell 7", "Quick spell 8", "Bard song",
                         "Quick song 0", "Quick song 1", "Quick song 2",
                         "Quick song 3", "Quick song 4", "Quick song 5",
                         "Quick song 6", "Quick song 7", "Quick song 8",
                         "Quick skill 0", "Quick skill 1", "Quick skill 2",
                         "Quick skill 3", "Quick skill 4", "Quick skill 5",
                         "Quick skill 6", "Quick skill 7", "Quick skill 8"}));
          break;

        case 0x91: // Disable spellcasting
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"All spells", "Non-innate", "Arcane", "Divine",
                         "Innate"}));
          break;

        case 0xBA: // Move creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Orientation", "DIR.IDS"));
          restype = "ARE";
          break;

        case 0xBB: // Set local variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "String";
          break;

        case 0xBC: // Increase spells cast per round
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Cleanse aura?", s_noyes));
          break;

        case 0xBD: // Increase casting speed factor
        case 0xBE: // Increase attack speed factor
        case 0xEA: // Snilloc's snowball swarm
        case 0xED: // Magical stone
        case 0xEF: // Slow poison
        case 0xF5: // Bad prayer
        case 0xFA: // Bad recitation
        case 0xFC: // Sol's searing orb
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xBF: // Casting level bonus
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset, 4, "Spell class",
            new String[]{"Arcane", "Divine"}));
          break;

        case 0xC1: // Invisibility detection
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Ignore visibility?", s_noyes));
          break;

        case 0xCE: // Protection from spell
        case 0x122: // Display spell immunity string
          final String s_cretype[] = {"Anyone", "Undead", "Not undead",
            "Fire-dwelling", "Not fire-dwelling", "Humanoid", "Not humanoid",
            "Animal", "Not animal", "Elemental", "Not elemental", "Fungus",
            "Not fungus", "Huge creature", "Not huge creature", "Elf",
            "Not elf", "Umber hulk", "Not umber hulk", "Half-elf",
            "Not half-elf", "Humanoid or animal", "Not humanoid or animal",
            "Blind", "Not blind", "Cold-dwelling", "Not cold-dwelling", "Golem",
            "Not golem", "Minotaur", "Not minotaur", "Undead or fungus",
            "Not undead or fungus", "Good", "Not good", "Neutral",
            "Not neutral", "Evil", "Not evil", "Paladin", "Not paladin",
            "Same moral alignment as source",
            "Not same moral alignment as source", "Source", "Not source",
            "Water-dwelling", "Not water-dwelling", "Breathing",
            "Not breathing", "Allies", "Not allies", "Enemies", "Not enemies",
            "Fire or cold dwelling", "Not fire or cold dwelling", "Unnatural",
            "Not unnatural", "Male", "Not male", "Lawful", "Not lawful",
            "Chaotic", "Not chaotic", "Evasion check", "Orc", "Not orc", "Deaf",
            "Not deaf", "Summoned creature", "Not summoned creature",
            "Mind flayer", "Not mind flayer", "Silenced", "Not silenced",
            "Intelligence less than", "Intelligence greater than",
            "Intelligence less than or equal to",
            "Intelligence greater than or equal to", "Skald", "Not skald",
            "Near enemies", "Not near enemies", "Drow", "Not drow",
            "Gray dwarf", "Not gray dwarf", "Daytime", "Not daytime", "Outdoor",
            "Not outdoor", "Keg", "Not keg", "Outsider", "Not outsider"};
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Creature type", s_cretype));
          restype = "SPL";
          break;

        case 0xD0: // Minimum HP
        case 0x1B0: // Tortoise shell
          s.add(new DecNumber(buffer, offset, 4, "HP amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDA: // Stoneskin effect
          s.add(new DecNumber(buffer, offset, 4, "# skins"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Skin type",
            new String[]{"Stoneskin", "Iron skins"}));
          break;

        case 0xE8: // Creature RGB color fade
          s.add(new Unknown(buffer, offset, 1));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 4, 2));
          s.add(new DecNumber(buffer, offset + 6, 2, "Speed"));
          break;

        case 0xE9: // Show visual effect
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_visuals));
          break;

        case 0xEB: // Show casting glow
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Glow",
            new String[]{"None", "Abjuration", "Conjuration", "Divination",
                         "Enchantment", "Illusion", "Invocation", "Necromancy",
                         "Alteration"}));
          break;

        case 0xF0: // Summon creature 2
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon type",
            new String[]{"Monster summoning 1", "Monster summoning 2",
                         "Monster summoning 3", "Monster summoning 4",
                         "Monster summoning 5", "Monster summoning 6",
                         "Monster summoning 7", "Animal summoning 1",
                         "Animal summoning 2", "Animal summoning 3",
                         "Summon insects", "Creeping doom", "Malavon summon"}));
          break;

        case 0xF1: // Vampiric touch
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Direction",
            new String[]{"Target to source", "Source to target"}));
          break;

        case 0xF2: // Show visual overlay
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Overlay",
            new String[]{"Globe of invulnerability", "Shroud of flame",
                         "Antimagic shell", "Otiluke's resilient sphere",
                         "Protection from normal missiles", "Cloak of fear",
                         "Entropy shield", "Fire aura", "Frost aura",
                         "Insect plague", "Storm shell", "Shield of lathander",
                         "Greater shield of lathander", "Seven eyes"}));
          break;

        case 0xF3: // Animate dead
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Undead type",
            new String[]{"Normal", "Lich"}));
          break;

        case 0xF6: // Summon creature 3
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Creature type",
            new String[]{"Lizard man", "Troll", "Shadow", "Invisible stalker",
                         "Fire elemental (wizard)", "Earth elemental (wizard)",
                         "Water elemental (wizard)", "Fire elemental (priest)",
                         "Earth elemental (priest)", "Water elemental (priest)",
                         "Malavon earth elemental"}));
          break;

        case 0xF7: // Beltyn's burning blood
          s.add(new DecNumber(buffer, offset, 4, "# hits"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF8: // Summon shadow
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Shadow type",
            new String[]{"Shadow", "Demishadow", "Shade"}));
          break;

        case 0xFB: // Lich touch
        case 0x100: // Umber hulk gaze
        case 0x117: // Animal rage
          s.add(new DecNumber(buffer, offset, 4, "# seconds"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xFD: // Bonus AC vs. weapons
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to",
            new String[]{"All weapons", "Blunt weapons", "Missile weapons",
                         "Piercing weapons", "Slashing weapons",
                         "Set base AC to value"}));
          break;

        case 0xFE: // Dispel specific spell
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Dispel type",
            new String[]{"All effects", "Equipped effects only",
                         "Limited effects only"}));
          restype = "SPL";
          break;

        case 0xFF: // Salamander aura
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Aura type",
            new String[]{"Fire", "Frost"}));
          break;

        case 0x103: // Summon creatures with cloud
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon type",
            new String[]{"Default", "Ally", "Hostile", "Forced", "Genie"}));
          restype = "CRE";
          break;

        case 0x105: // Immunity to effect and resource
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          restype = "SPL";
          break;

        case 0x108: // Static charge
        case 0x109: // Cloak of fear
        case 0x1C1: // Call lightning
          s.add(new DecNumber(buffer, offset, 4, "# hits"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "SPL";
          break;

        case 0x114: // Remove effect by type
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          break;

        case 0x118: // Turn undead
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Turn type",
            new String[]{"Command", "Rebuke", "Destroy", "Panic",
                         "Depend on caster"}));
          break;

        case 0x11B: // Float text
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Display type",
            new String[]{"String reference", "Cynicism"}));
          break;

        case 0x120: // Set status
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "State", "SPLSTATE.IDS"));
          break;

        case 0x128: // Set global variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x192: // Apply effects list
          final String s_applytype[] = {"Anyone", "Undead", "Not undead",
            "Fire-dwelling", "Not fire-dwelling", "Humanoid", "Not humanoid",
            "Animal", "Not animal", "Elemental", "Not elemental", "Fungus",
            "Not fungus", "Huge creature", "Not huge creature", "Elf",
            "Not elf", "Umber hulk", "Not umber hulk", "Half-elf",
            "Not half-elf", "Humanoid or animal", "Not humanoid or animal",
            "Blind", "Not blind", "Cold-dwelling", "Not cold-dwelling", "Golem",
            "Not golem", "Minotaur", "Not minotaur", "Undead or fungus",
            "Not undead or fungus", "Good", "Not good", "Neutral",
            "Not neutral", "Evil", "Not evil", "Paladin", "Not paladin",
            "Same moral alignment as source",
            "Not same moral alignment as source", "Source", "Not source",
            "Water-dwelling", "Not water-dwelling", "Breathing",
            "Not breathing", "Allies", "Not allies", "Enemies", "Not enemies",
            "Fire or cold dwelling", "Not fire or cold dwelling", "Unnatural",
            "Not unnatural", "Male", "Not male", "Lawful", "Not lawful",
            "Chaotic", "Not chaotic", "Orc", "Not orc", "Deaf", "Not deaf",
            "Same alignment", "Not same alignment", "Allied and same alignment",
            "Not allied and same alignment", "Hit points greater than",
            "Hit points less than", "Drow", "Not drow", "Gray dwarf",
            "Not gray dwarf", "Summoned creature", "Not summoned creature",
            "Goblin", "Not goblin", "Giant", "Not giant", "Troll", "Not troll",
            "Keg", "Not keg", "Human", "Not human", "Yuan-ti", "Not yuan-ti",
            "Outsider", "Not outsider"};
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Creature type", s_applytype));
          restype = "SPL";
          break;

        case 0x194: // Nausea
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Nausea type",
            new String[]{"Stinking cloud", "Ghoul touch"}));
          break;

        case 0x196: // Fire shield
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Shield type",
            new String[]{"Red", "Blue"}));
          restype = "SPL";
          break;

        case 0x199: // Righteous wrath of the faithful
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Affect",
            new String[]{"Allies", "Allies and same alignment"}));
          break;

        case 0x19C: // Control creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Control type",
            new String[]{"", "Default", "Mental domination"}));
          break;

        case 0x19D: // Run visual effect
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Animation",
            new String[]{"Sanctuary", "Entangle", "Wisp", "Shield", "Grease",
                         "Web", "Minor globe of invulnerability",
                         "Globe of invulnerability", "Shroud of flame",
                         "Antimagic shell", "Otiluke's resilient sphere",
                         "Protection from normal missiles", "Cloak of fear",
                         "Entrophy shield", "Fire aura", "Frost aura",
                         "Insect plague", "Storm shell", "Shield of lathander",
                         "", "Greater shield of lathander", "", "Seven eyes",
                         "", "Blur", "Invisibility", "Fire shield (red)",
                         "Fire shield (blue)", "", "", "Tortoise shell",
                         "Death armor"}));
          break;

        case 0x1A0: // Bleeding wounds
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Damage type",
            new String[]{"Amount HP per round", "Amount HP per second",
                         "1 HP per amount seconds"}));
          break;

        case 0x1A1: // Area effect using effects list
          s.add(new DecNumber(buffer, offset, 4, "Radius"));
          s.add(new Flag(buffer, offset + 4, 4, "Area effect type",
            new String[]{"Instant", "Once per round", "Ignore target"}));
          restype = "SPL";
          break;

        case 0x1A4: // Death magic
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Flag(buffer, offset + 4, 4, "Death type",
            new String[]{"Acid", "Burning", "Crushing", "Normal", "Exploding",
                         "Stoned", "Freezing", "", "", "", "Permanent",
                         "Destruction"}));
          break;

        case 0x1AD: // Apply effects list on hit
          restype = "SPL";
          break;

        case 0x1AE: // Projectile type using effects list
          s.add(new Unknown(buffer, offset, 4));
          s.add(new DecNumber(buffer, offset + 4, 4, "Projectile"));
          restype = "SPL";
          break;

        case 0x1AF: // Energy drain
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x1B1: // Blink
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Blink type",
            new String[]{"Normal", "Empty body"}));
          break;

        case 0x1B2: // Persistent using effects list
          s.add(new DecNumber(buffer, offset, 4, "Interval"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "SPL";
          break;

        case 0x1B4: // Damage reduction
          s.add(new DecNumber(buffer, offset, 4, "Damage ignored"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Enchantment to overcome"));
          break;

        case 0x1B5: // Disguise
          s.add(new IdsBitmap(buffer, offset, 4, "Animation", "ANIMATE.IDS"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x1BB: // Protection from arrows
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Damage reduction",
            new String[]{"None", "10/+1", "10/+2", "10/+3", "10/+4", "10/+5"}));
          break;

        case 0x1C2: // Globe of invulnerability
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Globe type",
            new String[]{"Minor globe of invulnerability",
                         "Globe of invulnerability"}));
          break;

        case 0x1C4: // Bane
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;
      }
    }

    if (s.size() == initSize) {
      s.add(new Unknown(buffer, offset, 4));
      s.add(new Unknown(buffer, offset + 4, 4));
    }
    return restype;
  }
}

