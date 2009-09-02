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

  private static final String s_projectile[] = {"None", "Arrow",
    "Arrow exploding", "Arrow flaming", "Arrow heavy", "Arrow shocking", "Axe",
    "Axe exploding", "Axe flaming", "Axe heavy", "Axe shocking", "Bolt",
    "Bolt exploding", "Bolt flaming", "Bolt heavy", "Bolt shocking", "Bullet",
    "Bullet exploding", "Bullet flaming", "Bullet heavy", "Bullet shocking",
    "Burning hands", "Call lightning BG", "Chromiatic orb", "Cone of cold BG",
    "Cone of fire", "Dagger", "Dagger exploding", "Dagger flaming",
    "Dagger heavy", "Dagger shocking", "Dart", "Dart exploding", "Dart flaming",
    "Dart heavy", "Dart shocking", "Magic missile BG", "Fireball",
    "Ice fragments", "Chain lightning", "Skipping stone", "Sleep",
    "Skeleton animation", "Smoke ball", "Smoke large", "Smoke small",
    "Sparkle blue", "Sparkle gold", "Sparkle purple", "Sparkle ice",
    "Sparkle stone", "Sparkle black", "Sparkle chromatic", "Sparkle red",
    "Sparkle green", "Spear", "Spear exploding", "Spear flaming", "Spear heavy",
    "Spear shocking", "Star sprite", "Stoned", "Web travel", "Web ground",
    "Gaze", "Holy might", "Flame strike", "Magic missiles BG 1",
    "Magic missiles BG 2", "Magic missiles BG 3", "Magic missiles BG 4",
    "Magic missiles BG 5", "Magic missiles BG 6", "Magic missiles BG 7",
    "Magic missiles BG 8", "Magic missiles BG 9", "Magic missiles BG 10",
    "Magic missiles BG 11", "Invisible traveling", "Fire bolt",
    "Call lightning chain 1", "Call lightning chain 2",
    "Call lightning chain 3", "Call lightning chain 4",
    "Call lightning chain 5", "Call lightning chain 6",
    "Call lightning chain 7", "Call lightning chain 8",
    "Call lightning chain 9", "Call lightning chain 10",
    "Call lightning chain 11", "Fire storm", "Call lighting storm",
    "Instant area effect", "Cloud", "Skull trap", "Color spray", "Ice storm",
    "Fire wall", "Glyph", "Grease", "Flame arrow green", "Flame arrow blue",
    "Fireball green", "Fireball blue", "Potion", "Potion exploding",
    "Acid blob", "Agannazar's scorcher", "Travel door", "Glow necromancy",
    "Glow alteration", "Glow enchantment", "Glow abjuration", "Glow illusion",
    "Glow conjuration", "Glow invocation", "Glow divination",
    "Hit necromancy air", "Hit necromancy earth", "Hit necromancy water",
    "Hit alteration air", "Hit alteration earth", "Hit alteration water",
    "Hit enchantment air", "Hit enchantment earth", "Hit enchantment water",
    "Hit abjuration air", "Hit abjuration earth", "Hit abjuration water",
    "Hit illusion air", "Hit illusion earth", "Hit illusion water",
    "Hit conjuration air", "Hit conjuration earth", "Hit conjuration water",
    "Hit invocation air", "Hit invocation earth", "Hit invocation water",
    "Hit divination air", "Hit divination earth", "Hit divination water",
    "Hit mushroom fire", "Hit mushroom gray", "Hit mushroom green",
    "Hit shaft fire", "Hit shaft light", "Hit shaft white", "Sparkle area blue",
    "Sparkle area gold", "Sparkle area purple", "Sparkle area ice",
    "Sparkle area stone", "Sparkle area black", "Sparkle area chromatic",
    "Sparkle area red", "Sparkle area green", "Instant area (party only)",
    "Instant area (not party)", "Sparkle area blue (party only)",
    "Sparkle area gold (party only)", "Sparkle area purple (party only)",
    "Sparkle area ice (party only)", "Sparkle area stone (party only)",
    "Sparkle area black (party only)", "Sparkle area chromatic (party only)",
    "Sparkle area red (party only)", "Sparkle area green (party only)",
    "Sparkle area blue (not party)", "Sparkle area gold (not party)",
    "Sparkle area purple (not party)", "Sparkle area ice (not party)",
    "Sparkle area stone (not party)", "Sparkle area black (not party)",
    "Sparkle area chromatic (not party)", "Sparkle area red (not party)",
    "Sparkle area green (not party)", "Sparkle area magenta (not party)",
    "Sparkle area orange (not party)", "Sparkle area magenta (party only)",
    "Sparkle area orange (party only)", "Sparkle area magenta",
    "Sparkle area orange", "Sparkle magneta", "Sparkle orange",
    "Non-sprite area", "Cloudkill BG", "Flame arrow ice", "Cow", "Hold",
    "Scorcher ice", "Acid blob mustard", "Acid blob gray", "Acid blob ochre",
    "Red holy might", "Hit necromancy area", "Hit alteration area",
    "Hit enchantment area", "Hit abjuration area", "Hit illusion area",
    "Hit conjuration area", "Hit invocation area", "Litany of curses",
    "Stories-bones-tell", "Magic missles 1", "Magic missles 2",
    "Magic missles 3", "Magic missles 4", "Magic missles 5", "Magic missles 6",
    "Magic missles 7", "Magic missles 8", "Magic missles 9", "Magic missles 10",
    "Skull mob", "One skull at the target", "Swarm curse",
    "Bees fly to target, no damage", "Adder's kiss", "Ice knife", "Pacify",
    "Strength", "Ball lightning", "Ball lightning 2", "Blood bridge",
    "Force missles", "Improved strength", "Shroud of shadows", "Cloudkill",
    "Howl of pandemonium", "Bladestorm", "Elysium's fires", "Abyssal fury",
    "Horror", "Knock", "Hold undead", "Missle of patience", "Elysium's tears",
    "Tasha's unbearable derisive laughter", "Axe of torment", "Blacksphere",
    "Cone of cold", "Desert hell", "Fire and ice", "Chain lightning storm",
    "Acid storm", "Stygian ice storm", "Meteor storm bombardment", "Deathbolt",
    "Ignus' fury", "Power word, blind", "Mechanus' cannon", "Celestial host",
    "Rune of torment", "Blessing, scripture of steel", "Curse, seeking flames",
    "", "Halo of lesser revelation", "Spiritual hammer", "Call lightning", "",
    "Vampiric touch", "Confusion", "Power word, kill",
    "Globe of invulnerability", "Nordom's crossbow bolts", "Raise dead",
    "Innate fear ability", "Conflagration", "Traps projectile",
    "Ignus' fireball", "Tongues of flame 1", "Tongues of flame 2",
    "Tongues of flame 3", "Tongues of flame 4", "Tongues of flame 5",
    "Ignus' terror", "Infernal orb", "Fiery rain", "Elemental strike",
    "Reign of anger 1", "Reign of anger 2", "Reign of anger 3",
    "Reign of anger 4", "Reign of anger 5", "Power of one", "Kiss",
    "Embalming"};
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
    m_colorloc.put(4L, "Strap/Trimming");
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
      case ResourceFactory.ID_DEMO:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set item color", "Set color glow solid",
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
          "Unstun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucent", "Summon creature", "Unsummon creature",
          "Non-detection", "Remove non-detection", "Change gender",
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
          "Bark skin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Non-detection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Domination",
          "Hopelessness", "Greater malison", "Spirit armor", "Chaos",
          "Feebleminded", "Defensive harmony", "Champion's strength", "Dying",
          "Mind shield", "Level drain", "Polymorph self", "Stun",
          "Regeneration", "Perception", "Master thievery"};
        break;

      case ResourceFactory.ID_TORMENT:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set item color", "Set color glow solid",
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
          "Unstun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucent", "Summon creature", "Unsummon creature",
          "Non-detection", "Remove non-detection", "Change gender",
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
          "Bark skin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Non-detection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Domination",
          "Hopelessness", "Greater malison", "Spirit armor", "Chaos",
          "Feebleminded", "Defensive harmony", "Champions strength", "Dying",
          "Mind shield", "Level drain", "Polymorph self", "Stun",
          "Regeneration", "Perception", "Master thievery"};
        break;

      case ResourceFactory.ID_ICEWIND:
      case ResourceFactory.ID_ICEWINDHOW:
      case ResourceFactory.ID_ICEWINDHOWTOT:
        s_effname = new String[]{"AC bonus", "Modify attacks per round",
          "Cure sleep", "Berserk", "Cure berserk", "Charm creature",
          "Charisma bonus", "Set item color", "Set color glow solid",
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
          "Unstun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucent", "Summon creature", "Unsummon creature",
          "Non-detection", "Remove non-detection", "Change gender",
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
          "Casting level bonus", "Unknown (C0)", "Unknown (C1)", "Unknown (C2)",
          "Unknown (C3)", "Unknown (C4)", "Unknown (C5)", "Unknown (C6)",
          "Unknown (C7)", "Unknown (C8)", "Unknown (C9)", "Unknown (CA)",
          "Unknown (CB)", "Unknown (CC)", "Unknown (CD)",
          "Protection from spell", "Unknown (CF)", "Minimum HP", "Unknown (D1)",
          "Power word, stun", "Unknown (D3)", "Unknown (D4)", "Unknown (D5)",
          "Unknown (D6)", "Unknown (D7)", "Unknown (D8)", "Unknown (D9)",
          "Stoneskin effect", "Unknown (DB)", "Unknown (DC)", "Unknown (DD)",
          "Unknown (DE)", "Unknown (DF)", "Unknown (E0)", "Unknown (E1)",
          "Unknown (E2)", "Unknown (E3)", "Unknown (E4)", "Unknown (E5)",
          "Unknown (E6)", "Unknown (E7)", "Creature RGB color fade",
          "Show visual effect", "Cold damage on hit", "Unknown (EB)",
          "Unknown (EC)", "Double damage to undead", "All saving throws bonus",
          "Unknown (EF)", "Summon creature 2", "Hit point transfer",
          "Show visual overlay", "Animate dead", "Unknown (F4)", "Unknown (F5)",
          "Summon creature 3", "Beltyn's burning blood", "Summon shadow",
          "Unknown (F9)", "Unknown (FA)", "Unknown (FB)", "Blinding",
          "Bonus AC vs. weapons", "Dispel specific spell", "Salamander aura",
          "Umber hulk gaze", "Unknown (101)", "Unknown (102)",
          "Summon creatures with cloud", "Animation removal",
          "Immunity to effect and string", "Unknown (106)", "Unknown (107)",
          "Unknown (108)", "Unknown (109)", "Movement rate modifier",
          "Unknown (10B)", "Eye of the mind", "Eye of the sword",
          "Eye of the mage", "Eye of venom", "Eye of the spirit",
          "Eye of fortitude", "Eye of stone", "Unknown (113)", "Unknown (114)",
          "Unknown (115)", "Unknown (116)", "Unknown (117)", "Unknown (118)",
          "Unknown (119)", "Hide hit points", "Display string over head",
          "Double damage vs. undead/outer planar", "Unknown (11D)",
          "Ranger tracking", "Unknown (11F)", "Unknown (120)", "Unknown (121)",
          "Display spell immunity string",
          "Double damage vs. golem/outer planar"};
        s_poricon = new String[]{"Charm", "Dire charm", "Rigid thinking",
          "Confused", "Berserk", "Intoxicated", "Poisoned", "Nauseated",
          "Blind", "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Magic armor", "Held", "Sleep",
          "Shielded", "Protection from fire", "Blessed", "Chant", "Free action",
          "Bark skin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Non-detection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Nauseous",
          "Stun", "Ghost armor", "Stoneskin", "Hopelessness", "Courage",
          "Friends", "Hope", "Malison", "Spirit armor", "Domination",
          "Feebleminded", "Tensor's transformation", "Mind blank", "Aid",
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
          "Charisma bonus", "Set item color", "Set color glow solid",
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
          "Unstun", "Remove invisibility", "Vocalize", "Wisdom bonus",
          "Character color pulse", "Character tint solid",
          "Character tint bright", "Animation change", "Base THAC0 bonus",
          "Slay", "Reverse alignment", "Change alignment", "Dispel effects",
          "Stealth bonus", "Casting failure", "Unknown (3D)",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucent", "Summon creature", "Unsummon creature",
          "Non-detection", "Remove non-detection", "Change gender",
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
          "Invisible detection", "Ignore dialogue pause", "Drain HP on death",
          "Familiar", "Physical mirror", "Reflect specified effect",
          "Reflect spell level", "Spell turning", "Spell deflection",
          "Reflect spell school", "Reflect spell type",
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
          "Set trap", "Set automap note", "Remove automap note", "Create item",
          "Spell sequencer", "Create spell sequencer",
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
          "Set persistent AI", "Unknown (126)", "Disable permanent death",
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
          "Bark skin", "Strength", "Heroism", "Invulnerable",
          "Protection from acid", "Protection from cold", "Resist fire/cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Non-detection",
          "Good luck", "Bad luck", "Silenced", "Cursed", "Panic", "Resist fear",
          "Haste", "Fatigue", "Bard song", "Slow", "Regenerate", "Domination",
          "Hopelessness", "Greater malison", "Spirit armor", "Chaos",
          "Feebleminded", "Defensive harmony", "Champion's strength", "Dying",
          "Mind shield", "Level drain", "Polymorph self", "Stun",
          "Regeneration", "Perception", "Master thievery", "Energy drain",
          "Holy power", "Cloak of fear", "Iron skins", "Magic resistance",
          "Righteous magic", "Spell turning", "Repulsing undead",
          "Spell deflection", "Fire shield (red)", "Fire shield (blue)",
          "Protection from normal weapons", "Protection from magic weapons",
          "Tensor's transformation", "Protection from magical energy",
          "Mislead", "Contingency", "Protection from the elements",
          "Projected image", "Maze", "Imprisonment", "Stoneskin", "Kai",
          "Called shot", "Spell failure", "Offensive stance",
          "Defensive stance", "Intelligence drained", "Regenerating", "Talking",
          "At store", "Negative plane protection", "Ability score drained",
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
          "Awaken", "Berserk", "Calm", "Charm creature", "Charisma bonus",
          "Set item color", "Set color glow solid", "Set color glow pulse",
          "Constitution bonus", "Cure poison", "Damage", "Kill target",
          "Defrost", "Dexterity bonus", "Haste", "Current HP bonus",
          "Maximum HP bonus", "Intelligence bonus", "Invisibility",
          "Knowledge arcana", "Luck bonus", "Reset morale", "Panic", "Poison",
          "Remove curse", "Acid resistance bonus", "Cold resistance bonus",
          "Electricity resistance bonus", "Fire resistance bonus",
          "Magic damage resistance bonus", "Raise dead", "Fortitude save bonus",
          "Reflex save bonus", "Will save bonus", "Unknown (24)",
          "Unknown (25)", "Silence", "Sleep", "Slow", "Sparkle",
          "Bonus wizard spells", "Stone to flesh", "Strength bonus", "Stun",
          "Unstun", "Visible", "Vocalize", "Wisdom bonus", "RGB glow",
          "RGB hue dark", "RGB hue light", "Animation change",
          "Base attack bonus", "Slay", "Reverse alignment", "Change alignment",
          "Dispel effects", "Move silently bonus", "Casting failure", "Alchemy",
          "Bonus priest spells", "Infravision", "Remove infravision", "Blur",
          "Translucent", "Summon creature", "Unsummon creature",
          "Non-detection", "Remove non-detection", "Change gender", "AI change",
          "Attack damage bonus", "Blindness", "Cure blindness", "Feeblemind",
          "Cure feeblemind", "Disease", "Cure disease", "Deafness",
          "Cure deafness", "Set AI script", "Immunity to projectile",
          "Unknown (54)", "Unknown (55)", "Slashing resistance bonus",
          "Crushing resistance bonus", "Piercing resistance bonus",
          "Missile resistance bonus", "Open locks bonus", "Find traps bonus",
          "Pick pockets bonus", "Fatigue bonus", "Intoxication bonus",
          "Tracking bonus", "Change level", "Unknown (61)", "Regeneration",
          "Modify duration", "Unknown (64)", "Immunity to effect",
          "Immunity to spell level", "Change name", "XP bonus", "Remove gold",
          "Morale break", "Change portrait", "Reputation bonus", "Paralyze",
          "Retreat from", "Create weapon", "Remove item", "Equip weapon",
          "Dither", "Unknown (73)", "Detect invisible", "Clairvoyance",
          "Show creatures", "Mirror image", "Immunity to weapon",
          "Unknown (79)", "Create inventory item", "Remove inventory item",
          "Teleport", "Unlock", "Movement rate bonus", "Unknown (7E)",
          "Confusion", "Aid (non-cumulative)", "Bless (non-cumulative)",
          "Chant (non-cumulative)", "Draw upon holy might (non-cumulative)",
          "Luck (non-cumulative)", "Petrification", "Polymorph",
          "Force visible", "Unknown (89)", "Set animation sequence",
          "Display string", "Casting glow", "Visual spell hit",
          "Display portrait icon", "Create item in slot", "Disable button",
          "Disable spellcasting", "Cast spell", "Learn spell",
          "Cast spell (point)", "Identify", "Detect traps", "Replace self",
          "Play movie", "Sanctuary", "Entangle", "Unknown (9B)", "Unknown (9C)",
          "Web effect", "Grease overlay", "Mirror image effect", "Unknown (A0)",
          "Remove panic", "Remove hold", "Free action", "Remove intoxication",
          "Pause caster", "Magic resistance bonus", "Missile attack bonus",
          "Destroy self", "Prevent portrait icon", "Play damage animation",
          "Give innate ability", "Remove innate ability",
          "Poison resistance bonus", "Play sound", "Unknown (AF)",
          "Movement rate penalty", "Unknown (B1)", "Unknown (B2)",
          "Unknown (B3)", "Unknown (B4)", "Unknown (B5)", "Unknown (B6)",
          "Unknown (B7)", "Unknown (B8)", "Hold creature (forced)",
          "Unknown (BA)", "Unknown (BB)", "Aura cleansing", "Mental speed",
          "Physical speed", "Casting level bonus", "Unknown (C0)",
          "See invisible", "Unknown (C2)", "Unknown (C3)", "Unknown (C4)",
          "Unknown (C5)", "Unknown (C6)", "Unknown (C7)", "Unknown (C8)",
          "Unknown (C9)", "Unknown (CA)", "Unknown (CB)", "Unknown (CC)",
          "Unknown (CD)", "Immunity spell", "Unknown (CF)", "Minimum HP",
          "Unknown (D1)", "Unknown (D2)", "Unknown (D3)", "Unknown (D4)",
          "Unknown (D5)", "Unknown (D6)", "Unknown (D7)", "Unknown (D8)",
          "Unknown (D9)", "Stoneskin effect", "Unknown (DB)", "Unknown (DC)",
          "Unknown (DD)", "Unknown (DE)", "Unknown (DF)", "Unknown (E0)",
          "Unknown (E1)", "Unknown (E2)", "Unknown (E3)", "Unknown (E4)",
          "Unknown (E5)", "Unknown (E6)", "Unknown (E7)",
          "Color glow dissipate", "Visual spell hit", "Unknown (EA)",
          "Casting glow", "Panic undead", "Unknown (ED)", "Saving throw change",
          "Slow poison", "Unknown (F0)", "Vampiric touch", "Unknown (F2)",
          "Unknown (F3)", "Prayer", "Unknown (F5)", "Unknown (F6)",
          "Beltyn's burning blood", "Unknown (F8)", "Recitation",
          "Unknown (FA)", "Unknown (FB)", "Unknown (FC)", "Unknown (FD)",
          "Remove specific effects", "Salamander aura", "Umber hulk gaze",
          "Zombie lord aura", "Unknown (102)", "Unknown (103)", "Hide creature",
          "Immunity effect and resource", "Unknown (106)", "Unknown (107)",
          "Static charge", "Cloak of fear", "Movement rate", "Remove confusion",
          "Eye of the mind", "Eye of the sword", "Eye of the mage",
          "Eye of venom", "Eye of the spirit", "Eye of fortitude",
          "Eye of stone", "Remove seven eyes", "Remove effects of type",
          "Soul eater", "Shroud of flame", "Animal rage", "Turn undead",
          "Vitriolic sphere", "Suppress HP info", "Float text",
          "Mace of disruption", "Sleep", "Tracking", "Sneak attack immunity",
          "Set state", "Unknown (121)", "Immunity spell (message)",
          "Unknown (123)", "Rest", "Beholder dispel magic", "Harpy wail",
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
          "Protection from evil", "Add effects list", "Armor of faith",
          "Nausea", "Enfeeblement", "Fire shield", "Death ward", "Holy power",
          "Righteous wrath of the faithful", "Summon (as ally)",
          "Summon (as enemy)", "Control", "Visual effect",
          "Otiluke's resilient sphere", "Bark skin", "Bleeding wounds",
          "Area effect using effects list", "Free action",
          "Knocked unconscious", "Death magic", "Entropy shield", "Storm shell",
          "Protection from the elements", "Hold undead", "Control undead",
          "Aegis", "Executioner's eyes", "Banish",
          "When struck using effects list",
          "Projectile type using effects list", "Energy drain",
          "Tortoise shell", "Blink", "Persistant using effects list",
          "Day blindness", "Damage reduction", "Disguise", "Heroic inspiration",
          "Prevent AI slowdown", "Barbarian rage", "Slowed", "Unknown (1BA)",
          "Protection from arrows", "Tenser's transformation", "Unknown (1BD)",
          "Smite evil", "Restoration", "Alicorn lance glow", "Call lightning",
          "Globe of invulnerability", "Lower resistance", "Bane",
          "Power attack", "Expertise", "Arterial strike", "Hamstring",
          "Rapdid shot", "Unknown (1CA)", "Unknown (1CB)"};
        s_poricon = new String[]{"Charmed", "", "", "Confused", "Berserk",
          "Intoxicated", "Poisoned", "Diseased", "Blind",
          "Protection from evil", "Protection from petrification",
          "Protection from normal missiles", "Armor", "Held", "Asleep",
          "Shield", "Protection from fire", "Bless", "Chant", "Free action",
          "Bark skin", "Strength", "Heroism", "Spell invulnerability",
          "Protection from acid", "Protection from cold",
          "Protection from electricity", "Protection from magic",
          "Protection from undead", "Protection from poison", "Non-detectable",
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
          "Intelligence drained", "Cloak of fear", "Entrophy shield",
          "Insect plague", "Storm shell", "Shield of lathander",
          "Greater shield of lathander", "Seven eyes", "Blur", "Invisibility",
          "Barbarian rage", "Called shot", "Defensive spin",
          "Maximized attacks", "Offensive spin", "Envenom weapon",
          "Unconscious 2", "Doom", "Aegis", "Executioner's eyes",
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

      case 0x5: // Charm creature
        s.add(new IdsBitmap(buffer, offset, 4, "Creature type", "GENERAL.IDS"));
        s.add(new Flag(buffer, offset + 4, 4, "Charm flags",
          new String[]{"No flags set", "Turn hostile afterward", "Dire charm",
                       "Controlled by cleric"}));
        break;

      case 0x6: // Charisma bonus
      case 0xA: // Constitution bonus
      case 0xF: // Dexterity bonus
      case 0x13: // Intelligence bonus
      case 0x15: // Lore bonus
      case 0x16: // Luck bonus
      case 0x1B: // Acid resistance bonus
      case 0x1C: // Cold resistance bonus
      case 0x1D: // Electricity resistance bonus
      case 0x1E: // Fire resistance bonus
      case 0x1F: // Magic damage resistance bonus
      case 0x21: // Save vs. death bonus / Fortitude save bonus
      case 0x22: // Save vs. wand bonus / Reflex save bonus
      case 0x23: // Save vs. polymorph bonus / Will save bonus
      case 0x24: // Save vs. breath bonus
      case 0x25: // Save vs. spell bonus
      case 0x2C: // Strength bonus
      case 0x31: // Wisdom bonus
      case 0x36: // Base THAC0 bonus / Base attack bonus
      case 0x3B: // Stealth bonus / Move silently bonus
      case 0x49: // Attack damage bonus
      case 0x54: // Magical fire resistance bonus
      case 0x55: // Magical cold resistance bonus
      case 0x56: // Slashing resistance bonus
      case 0x57: // Crushing resistance bonus
      case 0x58: // Piercing resistance bonus
      case 0x59: // Missile resistance bonus
      case 0x5A: // Open locks bonus
      case 0x5B: // Find traps bonus
      case 0x5C: // Pick pockets bonus
      case 0x5D: // Fatigue bonus
      case 0x5E: // Intoxication bonus
      case 0x5F: // Tracking bonus
      case 0x60: // Change level
      case 0x61: // Exceptional strength bonus
      case 0x68: // XP bonus
      case 0x69: // Remove gold
      case 0x6A: // Morale break
      case 0x6C: // Reputation bonus
      case 0x7E: // Movement rate bonus
      case 0xA7: // Missile THAC0 bonus
      case 0xB0: // Movement rate bonus 2
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
        break;

      case 0x7: // Set item color
        s.add(new ColorValue(buffer, offset, 4, "Color"));
        s.add(new HashBitmap(buffer, offset + 4, 4, "Location", m_colorloc));
        break;

      case 0x8: // Set color glow solid
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new HashBitmap(buffer, offset + 4, 4, "Location", m_colorloc));
        break;

      case 0x9: // Set color glow pulse
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new HashBitmap(buffer, offset + 4, 2, "Location", m_colorloc));
        s.add(new DecNumber(buffer, offset + 6, 2, "Cycle speed"));
        break;

      case 0xC: // Damage
        s.add(new DecNumber(buffer, offset, 4, "Amount"));
        s.add(new Bitmap(buffer, offset + 4, 2, "Mode",
          new String[]{"Normal", "Set to value", "Set to %", "Percentage"}));
        s.add(new IdsBitmap(buffer, offset + 6, 2, "Damage type", "DAMAGES.IDS"));
        break;

      case 0xD: // Kill target
        s.add(new Bitmap(buffer, offset, 4, "Display text?", s_yesno));
        s.add(new Flag(buffer, offset + 4, 4, "Death type",
          new String[]{"Acid", "Burning", "Crushed", "Normal", "Chunked",
                       "Stoned", "Freezing", "Exploding stoned",
                       "Exploding freezing", "Electrified", "Disintegration"}));
        break;

      case 0x10: // Haste
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Haste type",
          new String[]{"Normal", "Improved", "Movement rate only"}));
        break;

      case 0x11: // Current HP bonus
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 2, "Modifier type",
          new String[]{"Increment", "Set", "Increment % of"}));
        s.add(new Flag(buffer, offset + 6, 2, "Heal flags",
          new String[]{"No flags set", "Raise dead",
                       "Remove limited effects"}));
        break;

      case 0x12: // Maximum HP bonus
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
          new String[]{"Increment", "Set", "Set % of",
                       "Increment, don't update current HP",
                       "Set, don't update current HP",
                       "Set % of, don't update current HP"}));
        break;

      case 0x14: // Invisibility
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Invisibility type",
          new String[]{"Normal", "Improved"}));
        break;

      case 0x19: // Poison
        s.add(new DecNumber(buffer, offset, 4, "Amount"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Poison type",
          new String[]{"1 damage per second", "1 damage per second",
                       "Amount damage per second",
                       "1 damage per amount seconds",
                       "Amount damage per amount seconds"}));
        break;

      case 0x29: // Sparkle
        s.add(new Bitmap(buffer, offset, 4, "Color",
          new String[]{"", "Black", "Blue", "Chromatic", "Gold", "Green",
                       "Purple", "Red", "White", "Ice", "Stone", "Magenta",
                       "Orange"}));
        s.add(new Bitmap(buffer, offset + 4, 4, "Particle effect",
          new String[]{"", "Explosion", "", "Shower"}));
        break;

      case 0x2A: // Bonus wizard spells
      case 0x3E: // Bonus priest spells
        s.add(new DecNumber(buffer, offset, 4, "# spells to add"));
        s.add(new Flag(buffer, offset + 4, 4, "Spell levels",
          new String[]{"Double spells", "Level 1", "Level 2", "Level 3",
                       "Level 4", "Level 5", "Level 6", "Level 7", "Level 8",
                       "Level 9"}));
        break;

      case 0x32: // Character color pulse
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new Unknown(buffer, offset + 4, 2));
        s.add(new DecNumber(buffer, offset + 6, 2, "Cycle speed"));
        break;

      case 0x33: // Character tint solid
      case 0x34: // Character tint bright
        s.add(new Unknown(buffer, offset, 1));
        s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Red"));
        s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Green"));
        s.add(new UnsignDecNumber(buffer, offset + 3, 1, "Blue"));
        s.add(new HashBitmap(buffer, offset + 4, 2, "Location", m_colorloc));
        s.add(new Unknown(buffer, offset + 6, 2));
        break;

      case 0x35: // Animation change
        s.add(new IdsBitmap(buffer, offset, 4, "Morph into", "ANIMATE.IDS"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Morph type",
          new String[]{"Temporary change", "Remove temporary change",
                       "Permanent change"}));
        break;

      case 0x37: // Slay
      case 0x64: // Protection from creature type
      case 0x6D: // Paralyze
      case 0xAF: // Hold creature
      case 0xB2: // THAC0 vs. type bonus
      case 0xB3: // Damage vs. type bonus
      case 0xB9: // Hold creature 2 / Hold creature (forced)
        s.add(new IDSTargetEffect(buffer, offset));
        break;

      case 0x39: // Change alignment
        s.add(new Unknown(buffer, offset, 4));
        if (gameid == ResourceFactory.ID_ICEWIND2)
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Alignment", "ALIGNMNT.IDS"));
        else
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Alignment", "ALIGNMEN.IDS"));
        break;

      case 0x3A: // Dispel effects
        s.add(new DecNumber(buffer, offset, 4, "Level"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Dispel type",
          new String[]{"Always dispel", "Use caster level",
                       "Use specific level"}));
        break;

      case 0x3C: // Casting failure
        s.add(new DecNumber(buffer, offset, 4, "Amount"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Failure type",
          new String[]{"Wizard", "Priest", "Innate", "Wizard (dead magic)",
                       "Priest (dead magic)", "Innate (dead magic)"}));
        break;

      case 0x42: // Translucent
        s.add(new DecNumber(buffer, offset, 4, "Fade amount"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x43: // Summon creature
        if (gameid == ResourceFactory.ID_ICEWIND ||
            gameid == ResourceFactory.ID_ICEWINDHOW ||
            gameid == ResourceFactory.ID_ICEWINDHOWTOT)
          s.add(new Bitmap(buffer, offset, 4, "Animation",
            new String[]{"No animation", "Monster summoning circle",
                         "Animal summoning circle", "Earth summoning circle",
                         "Fire summoning circle", "Water summoning circle", "",
                         "Puff of smoke"}));
        else
          s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Allegiance",
          new String[]{"Match target", "Match target", "From CRE file",
                       "Match target", "From CRE file", "Hostile",
                       "From CRE file"}));
        restype = "CRE";
        break;

      case 0x44: // Unsummon creature
        s.add(new Bitmap(buffer, offset, 4, "Display text?", s_noyes));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x47: // Change gender
        s.add(new IdsBitmap(buffer, offset, 4, "Gender", "GENDER.IDS"));
        s.add(new Bitmap(buffer, offset + 4, 4, "How?",
          new String[]{"Reverse gender", "Set gender"}));
        break;

      case 0x48: // Change AI type
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "From IDS file",
          new String[]{"Ea.ids", "General.ids", "Race.ids", "Class.ids",
                       "Specific.ids", "Gender.ids", "Align.ids"}));
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

      case 0x52: // Set AI script
        s.add(new Unknown(buffer, offset, 4));
        s.add(new IdsBitmap(buffer, offset + 4, 4, "Script level", "SCRLEV.IDS"));
        restype = "BCS";
        break;

      case 0x53: // Immunity to projectile
        s.add(new Unknown(buffer, offset, 4));
        if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Projectile", "PROJECTL.IDS"));
        else
          s.add(new Bitmap(buffer, offset + 4, 4, "Projectile", s_projectile));
        break;

      case 0x62: // Regeneration
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Regeneration type",
          new String[]{"1 HP per second", "1 HP per second",
                       "Amount HP per second", "1 HP per amount seconds",
                       "Amount HP per amount seconds"}));
        break;

      case 0x63: // Modify duration
        s.add(new DecNumber(buffer, offset, 4, "Percentage"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
          new String[]{"Wizard", "Priest"}));
        break;

      case 0x65: // Immunity to effect
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
        break;

      case 0x66: // Immunity to spell level
        s.add(new DecNumber(buffer, offset, 4, "Spell level"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x67: // Change name
        s.add(new StringRef(buffer, offset, "Name"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x6B: // Portrait
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Which portrait?",
          new String[]{"Small", "Large"}));
        restype = "BMP";
        break;

      case 0x6F: // Create weapon
      case 0x7A: // Create inventory item
        s.add(new DecNumber(buffer, offset, 4, "# to create"));
        s.add(new Unknown(buffer, offset + 4, 4));
        restype = "ITM";
        break;

      case 0x70: // Remove item
      case 0x7B: // Remove inventory item
        restype = "ITM";
        break;

      case 0x73: // Detect alignment
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Alignment mask",
          new String[]{"Evil", "Neutral", "Good"}));
        break;

//      case 0x77: // Mirror image
//        s.add(new DecNumber(buffer, offset, 4, "Maximum # images"));
//        s.add(new Unknown(buffer, offset + 4, 4));
//        break;

      case 0x78: // Immunity to weapons
        s.add(new DecNumber(buffer, offset, 4, "Enchantment"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Weapon type",
          new String[]{"Enchanted", "Magical", "Non-magical", "Silver",
                       "Non-silver", "Non-magical, non-silver", "Two-handed",
                       "One-handed", "Cursed", "Non-cursed", "Cold iron",
                       "Non-cold iron", "Magical two-handed"}));
        break;

      case 0x7F: // Summon monsters
        s.add(new DecNumber(buffer, offset, 4, "Total XP"));
        s.add(new Bitmap(buffer, offset + 4, 4, "From 2DA file",
          new String[]{"Monsum01 (ally)", "Monsum02 (ally)", "Monsum03 (ally)",
                       "Anisum01 (ally)", "Anisum02 (ally)", "Monsum01 (enemy)",
                       "Monsum02 (enemy)", "Monsum03 (enemy)",
                       "Anisum01 (enemy)", "Anisum02 (enemy)"}));
        restype = "2DA";
        break;

      case 0x81: // Aid (non-cumulative)
      case 0x82: // Bless (non-cumulative)
      case 0x83: // Chant (non-cumulative)
      case 0x84: // Draw upon holy might (non-cumulative)
      case 0x85: // Luck (non-cumulative)
      case 0x89: // Bad chant (non-cumulative)
      case 0xAD: // Poison resistance bonus
        s.add(new DecNumber(buffer, offset, 4, "Amount"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x87: // Polymorph
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Polymorph type",
          new String[]{"Change into", "Appearance only", "Appearance only",
                       "Appearance only"}));
        restype = "CRE";
        break;

      case 0x8A: // Set animation sequence
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

      case 0x8B: // Display string
        s.add(new StringRef(buffer, offset, "String"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0x8C: // Casting glow
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

      case 0x8D: // Lighting effects / Visual spell hit
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

      case 0x8E: // Display portrait icon
      case 0xA9: // Prevent portrait icon
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Icon", s_poricon));
        break;

      case 0x8F: // Create item in slot
        s.add(new IdsBitmap(buffer, offset, 4, "Slot", "SLOTS.IDS"));
        s.add(new Unknown(buffer, offset + 4, 4));
        restype = "ITM";
        break;

      case 0x91: // Disable spellcasting
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
          new String[]{"Wizard", "Priest", "Innate"}));
        break;

      case 0x92: // Cast spell
      case 0x94: // Cast spell at point
        s.add(new DecNumber(buffer, offset, 4, "Cast at level"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Cast instantly?", s_noyes));
        restype = "SPL";
        break;

      case 0x93: // Learn spell
      case 0xAB: // Give innate ability
      case 0xAC: // Remove spell / Remove innate ability
      case 0x10A: // Remove protection from spell
        restype = "SPL";
        break;

      case 0x97: // Replace self
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Replacement method",
          new String[]{"Remove silently", "Remove via chunked death",
                       "Remove via normal death", "Don't remove"}));
        restype = "CRE";
        break;

      case 0x98: // Play movie
        restype = "MVE";
        break;

      case 0x9F: // Mirror image effect
        s.add(new DecNumber(buffer, offset, 4, "# images"));
        s.add(new Unknown(buffer, offset + 4, 4));
        break;

      case 0xA6: // Magic resistance bonus
        s.add(new DecNumber(buffer, offset, 4, "Value"));
        s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
          new String[]{"Increment", "Set"}));
        break;

      case 0xAA: // Play damage animation
        s.add(new Unknown(buffer, offset, 4));
        s.add(new Bitmap(buffer, offset + 4, 4, "Animation",
          new String[]{"Blood (behind)", "Blood (front)", "Blood (left)",
                       "Blood (right)", "Fire 1", "Fire 2", "Fire 3",
                       "Electricity 1", "Electricity 2", "Electricity 3"}));
        break;

      case 0xAE: // Play sound
        restype = "WAV";
        break;

      case 0xB1: // Use EFF file
        s.add(new IDSTargetEffect(buffer, offset));
        restype = "EFF";
        break;

      case 0xB4: // Disallow item
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
        case 0x0: // AC bonus
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to", s_actype));
          break;

        case 0x90: // Disable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0xBA: // Move creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Orientation", Actor.s_orientation));
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
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xBF: // Casting level bonus
          s.add(new Unknown(buffer, offset, 4));
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

        case 0x90: // Disable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0xBA: // Set status
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsFlag(buffer, offset + 4, 4, "Status", "STATE.IDS"));
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
            new String[]{"Caster to target", "Target to caster", "Swap HP",
                         "Caster to target 2"}));
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
          s.add(new Bitmap(buffer, offset + 4, 4, "Flash type",
            new String[]{"Delayed", "Instant"}));
          break;

        case 0xC3: // Tint screen
          s.add(new UnsignDecNumber(buffer, offset, 1, "Red"));
          s.add(new UnsignDecNumber(buffer, offset + 1, 1, "Green"));
          s.add(new UnsignDecNumber(buffer, offset + 2, 1, "Blue"));
          s.add(new Unknown(buffer, offset + 3, 1));
          s.add(new Bitmap(buffer, offset + 4, 4, "Method",
            new String[]{"Quick to dark then back", "",
                         "Quick to dark then instant light", "",
                         "Light to dark for duration", "", "", "", "No effect",
                         "Very fast light to dark then back",
                         "Instant dark duration then instant light"}));
          break;

        case 0xC4: // Special spell hit
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect",
            new String[]{"Adder's kiss", "Ball lightning", "Fizzle"}));
          break;

        case 0xC9: // Play BAM with effects
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect",
            new String[]{"Cloak of warding", "Shield", "Black barbed shield",
                         "Pain mirror", "Guardian mantle", "",
                         "Enoll eva's duplication", "Armor", "Antimagic shield",
                         "", "", "Flame walk", "Protection from evil",
                         "Conflagration", "Infernal shield",
                         "Submerge the will", "Balance in all things"}));
          restype = "BAM";
          break;

        case 0xCB: // Curse
        case 0xCC: // Prayer
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
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

        case 0x90: // Disable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0xBA: // Move creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Orientation", Actor.s_orientation));
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
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xCE: // Protection from spell
        case 0x122: // Display spell immunity string
          s.add(new Unknown(buffer, offset, 4));
          s.add(new HexNumber(buffer, offset + 4, 4, "Creature type"));
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
          s.add(new HexNumber(buffer, offset + 4, 4, "Effect"));
          break;

        case 0xEE: // All saving throws bonus
        case 0x10A: // Movement rate modifier
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0xF0: // Summon creature 2
          s.add(new DecNumber(buffer, offset, 4, "Creature"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon type",
            new String[]{"Monster summoning 1", "Monster summoning 2",
                         "Monster summoning 3", "Monster summoning 4",
                         "Monster summoning 5", "Monster summoning 6",
                         "Monster summoning 7", "Animal summoning 1",
                         "Animal summoning 2", "Animal summoning 3",
                         "Summon insects", "Creeping doom", "Malavon summon"}));
          break;

        case 0xF1: // Hit point transfer
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Direction",
            new String[]{"Target to caster", "Caster to target"}));
          break;

        case 0xF2: // Show visual overlay
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Overlay",
            new String[]{"Globe of invulnerability", "Shroud of flame",
                         "Antimagic shell", "Otiluke's resilient sphere",
                         "Protection from normal missiles", "Cloak of fear",
                         "Entropy shield", "Fire aura", "Frost aura",
                         "Insect plague", "Storm shield", "Shield of lathander",
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
                         "Earth elemental"}));
          break;

        case 0xF8: // Summon shadow
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Shadow type",
            new String[]{"Shadow", "Demi-shadow", "Shade"}));
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
          s.add(new DecNumber(buffer, offset, 4, "Damage amount"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Aura type",
            new String[]{"Fire", "Frost"}));
          break;

        case 0x100: // Umber hulk gaze
          s.add(new DecNumber(buffer, offset, 4, "# seconds"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x103: // Summon creatures with cloud
          s.add(new DecNumber(buffer, offset, 4, "Creature"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon type",
            new String[]{"Default", "Ally", "Hostile", "Forced", "Genie"}));
          restype = "CRE";
          break;

        case 0x105: // Immunity to effect and string
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          break;

        case 0x11B: // Display string over head
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Display type",
            new String[]{"Default", "Cynicism"}));
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
        case 0x0: // AC bonus
          s.add(new DecNumber(buffer, offset, 4, "AC value"));
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to", s_actype));
          break;

        case 0x90: // Disable button
        case 0x117: // Reenable button
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Button", s_button));
          break;

        case 0xB8: // No collision detection
        case 0xBC: // Increase spells cast per round
        case 0xC1: // Invisible detection
        case 0xC2: // Ignore dialogue pause
        case 0xF5: // Unknown (F5)
        case 0xF6: // Berserk effect
        case 0xF7: // Attack nearest creature
        case 0x123: // Disable visual effects
        case 0x124: // Immunity to backstab
        case 0x125: // Set persistent AI
        case 0x126: // Unknown (126)
        case 0x127: // Disable permanent death
        case 0x129: // Immunity to turn undead
        case 0x12C: // Modify collision behavior
        case 0x12E: // Can use any item
        case 0x12F: // Backstab every hit
        case 0x134: // Immunity to tracking
        case 0x136: // Immunity to time stop
        case 0x13B: // Full animation removal
          s.add(new Unknown(buffer, offset, 4));
          s.add(new DecNumber(buffer, offset + 4, 4, "Stat value"));
          break;

        case 0xBA: // Move creature
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Orientation", Actor.s_orientation));
          restype = "ARE";
          break;

        case 0xBB: // Set local variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "String";
          break;

        case 0xBD: // Increase casting speed factor
        case 0xBE: // Increase attack speed factor
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xBF: // Casting level bonus
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest"}));
          break;

        case 0xC3: // Familiar death
          s.add(new DecNumber(buffer, offset, 4, "HP amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xC5: // Physical mirror
          s.add(new Unknown(buffer, offset, 4));
          if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
            s.add(new IdsBitmap(buffer, offset + 4, 4, "Projectile", "PROJECTL.IDS"));
          else
            s.add(new HexNumber(buffer, offset + 4, 4, "Projectile"));
          break;

        case 0xC6: // Reflect specified effect
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          break;

        case 0xC7: // Reflect spell level
          s.add(new DecNumber(buffer, offset, 4, "Spell level"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xC8: // Spell turning
        case 0xC9: // Spell deflection
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Maximum level"));
          break;

        case 0xCA: // Reflect spell school
        case 0xCC: // Protection from spell school
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell school", s_school));
          break;

        case 0xCB: // Reflect spell type
        case 0xCD: // Protection from spell type
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell type", SplResource.s_category));
          break;

        case 0xCE: // Protection from spell
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "SPL";
          break;

        case 0xCF: // Reflect specified spell
        case 0xFB: // Change bard song
        case 0xFC: // Set trap
        case 0x100: // Spell sequencer
        case 0x102: // Activate spell sequencer
        case 0x139: // High-level ability
          restype = "SPL";
          break;

        case 0xD0: // Minimum HP
          s.add(new DecNumber(buffer, offset, 4, "HP amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xD6: // Select spell
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Show",
            new String[]{"All spells", "Known spells"}));
          restype = "2DA";
          break;

        case 0xD7: // Play 3D effect
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Play where?",
            new String[]{"Over target (unattached)", "Over target (attached)",
                         "On point cast"}));
          restype = "VVC";
          break;

        case 0xD8: // Level drain
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDA: // Stoneskin effect
        case 0x13A: // Stoneskin protection
          s.add(new DecNumber(buffer, offset, 4, "# skins"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDB: // Attack roll penalty
        case 0xEE: // Disintegrate
          s.add(new IDSTargetEffect(buffer, offset));
          break;

        case 0xDC: // Remove spell school protections
        case 0xE5: // Remove protection by school
          s.add(new DecNumber(buffer, offset, 4, "Maximum level"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell school", s_school));
          break;

        case 0xDD: // Remove spell type protections
        case 0xE6: // Remove protection by type
          s.add(new DecNumber(buffer, offset, 4, "Maximum level"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell type", SplResource.s_category));
          break;

        case 0xDE: // Teleport field
          s.add(new DecNumber(buffer, offset, 4, "Maximum range"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xDF: // Spell school deflection
        case 0xE3: // Spell school turning
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell school", s_school));
          break;

        case 0xE2: // Spell type deflection
        case 0xE4: // Spell type turning
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell type", SplResource.s_category));
          break;

        case 0xE8: // Cast spell on condition
          s.add(new Bitmap(buffer, offset, 4, "Target",
            new String[]{"Caster", "Last hit by", "Nearest enemy"}));
          s.add(new Bitmap(buffer, offset + 4, 4, "Condition",
            new String[]{"Target hit", "Enemy sighted", "HP below 50%",
                         "HP below 25%", "HP below 10%", "If helpless",
                         "If poisoned", "Every round when attacked",
                         "Every round when hit", "Every round"}));
          restype = "SPL";
          break;

        case 0xE9: // Modify proficiencies
          s.add(new DecNumber(buffer, offset, 4, "# stars"));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "Proficiency", "STATS.IDS"));
          break;

        case 0xEA: // Contingency
          s.add(new DecNumber(buffer, offset, 4, "Maximum spell level"));
          s.add(new DecNumber(buffer, offset + 4, 2, "# spells"));
          s.add(new Bitmap(buffer, offset + 6, 2, "Trigger type",
            new String[]{"Chain contingency", "Contingency",
                         "Spell sequencer"}));
          break;

        case 0xEB: // Wing buffet
          s.add(new DecNumber(buffer, offset, 4, "Distance"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Direction",
            new String[]{"", "", "Away from source", "", "Toward source"}));
          break;

        case 0xEC: // Project image
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Image type",
            new String[]{"", "Mislead", "Project image", "Simulacrum"}));
          break;

        case 0xED: // Set image type
          s.add(new DecNumber(buffer, offset, 4, "Puppet master"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Puppet type",
            new String[]{"", "Mislead", "Project image", "Simulacrum"}));
          break;

        case 0xEF: // Farsight
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Can view unexplored?", s_noyes));
          break;

        case 0xF0: // Remove portrait icon
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Icon", s_poricon));
          break;

        case 0xF1: // Control creature
          s.add(new IdsBitmap(buffer, offset, 4, "Creature type", "GENERAL.IDS"));
          s.add(new Flag(buffer, offset + 4, 4, "Control flags",
            new String[]{"No flags set", "Turn hostile afterward", "Dire charm",
                         "Controlled by cleric"}));
          break;

        case 0xF3: // Drain item charges
          s.add(new Bitmap(buffer, offset, 4, "Include weapons?", s_noyes));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF4: // Drain wizard spells
          s.add(new DecNumber(buffer, offset, 4, "# spells"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF8: // Melee hit effect
        case 0xF9: // Ranged hit effect
        case 0x11B: // Use EFF file as curse
          s.add(new IDSTargetEffect(buffer, offset));
          restype = "EFF";
          break;

        case 0xFA: // Maximum damage each hit
        case 0x12D: // Critical threat range bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xFD: // Set automap note
        case 0xFE: // Remove automap note
        case 0x10B: // Disable display string
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xFF: // Create item
          s.add(new DecNumber(buffer, offset, 4, "# items in stack"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "ITM";
          break;

        case 0x101: // Create spell sequencer
          s.add(new DecNumber(buffer, offset, 4, "Maximum level"));
          s.add(new DecNumber(buffer, offset + 4, 4, "# spells"));
          break;

        case 0x103: // Spell trap
          s.add(new DecNumber(buffer, offset, 4, "# spells"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Spell level"));
          break;

        case 0x105: // Wondrous recall
          s.add(new DecNumber(buffer, offset, 4, "Spell level"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Spell class",
            new String[]{"Wizard", "Priest"}));
          break;

        case 0x106: // Visual range bonus
        case 0x107: // Backstab bonus
        case 0x113: // Hide in shadows bonus
        case 0x114: // Detect illusions bonus
        case 0x115: // Set traps bonus
        case 0x116: // THAC0 bonus
        case 0x119: // Wild surge bonus
        case 0x11C: // Melee THAC0 bonus
        case 0x11D: // Melee weapon damage bonus
        case 0x11E: // Missile weapon damage bonus
        case 0x120: // Fist THAC0 bonus
        case 0x121: // Fist damage bonus
        case 0x131: // Off-hand THAC0 bonus
        case 0x132: // Main hand THAC0 bonus
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
          break;

        case 0x108: // Drop item
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Only quick weapons?", s_noyes));
          break;

        case 0x109: // Set global variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type",
            new String[]{"Set", "Increment"}));
          restype = "String";
          break;

        case 0x10D: // Shake screen
          s.add(new DecNumber(buffer, offset, 4, "Strength"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x110: // Use EFF File on condition
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Frequency",
            new String[]{"Once per second", "", "Value per second",
                         "Once per value seconds",
                         "Variable per value rounds"}));
          restype = "EFF";
          break;

        case 0x118: // Wild magic
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Affect",
            new String[]{"", "Next spell", "Every spell"}));
          break;

        case 0x11A: // Modify script state
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "State", "STATS.IDS", 156));
          break;

        case 0x122: // Change title
          s.add(new StringRef(buffer, offset, "Title"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Change where?",
            new String[]{"Records screen", "Class name"}));
          break;

        case 0x128: // Immunity to specific animation
          restype = "VVC";
          break;

        case 0x12A: // Pocket plane
          restype = "2DA";
          break;

        case 0x133: // Tracking
          s.add(new DecNumber(buffer, offset, 4, "Range"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x135: // Set variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Global or local?",
            new String[]{"Both", "Local only"}));
          restype = "String";
          break;

        case 0x13D: // Haste 2
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
          s.add(new Flag(buffer, offset + 4, 4, "Bonus to",
            new String[]{"Generic", "Armor", "Deflection", "Shield", "Crushing",
                         "Piercing", "Slashing", "Missile"}));
          break;

        case 0x3D: // Alchemy
        case 0xEE: // Saving throw change
        case 0x10A: // Movement rate
        case 0x129: // Hide in shadows bonus
        case 0x12A: // Use magic device bonus
        case 0x1B9: // Slowed
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Modifier type", s_inctype));
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

        case 0xBB: // Set local variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "String";
          break;

        case 0xBC: // Aura cleansing
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Cleanse aura?", s_noyes));
          break;

        case 0xBD: // Increase casting speed factor
        case 0xBE: // Increase attack speed factor
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xBF: // Casting level bonus
          s.add(new DecNumber(buffer, offset, 4, "Amount"));
          s.add(new Bitmap(buffer, offset, 4, "Spell class",
            new String[]{"Wizard", "Priest"}));
          break;

        case 0xCE: // Immunity spell
        case 0x122: // Immunity spell (message)
          final String s_cretype[] = {"Default", "Undead", "Not undead",
            "Fire dwelling", "Not fire dwelling", "Humanoid", "Not humanoid",
            "Animal", "Not animal", "Elemental", "Not elemental", "Fungus",
            "Not fungus", "Huge creature", "Not huge creature", "Elf",
            "Not elf", "Umber hulk", "Not umber hulk", "Half-elf",
            "Not half-elf", "Humanoid or animal", "Not humanoid or animal",
            "Blind", "Not blind", "Cold dwelling", "Not cold dwelling", "Golem",
            "Not golem", "Minotaur", "Not minotaur", "Undead or fungus",
            "Not undead or fungus", "Good", "Not good", "Neutral",
            "Not neutral", "Evil", "Not evil", "Paladin", "Not paladin",
            "Same moral alignment as source",
            "Not same moral alignment as source", "Source", "Not source",
            "Water dwelling", "Not water dwelling", "Breathing",
            "Not breathing", "Allies", "Not allies", "Enemies", "Not enemies",
            "Fire or cold dwelling", "Not fire or cold dwelling", "Unnatural",
            "Not unnatural", "Male", "Not male", "Lawful", "Not lawful",
            "Chaotic", "Not chaotic", "Evasion check", "Orc", "Not orc", "Deaf",
            "Not deaf", "Summoned creature", "Not summoned creature",
            "Mind flayer", "Not mind flayer", "Silenced", "Not silenced",
            "Intelligence less than amount", "Intelligence greater than amount",
            "Intelligence less than or equal amount",
            "Intelligence greater than or equal amount", "Skald", "Not skald",
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
          s.add(new Bitmap(buffer, offset + 4, 4, "Skin type",
            new String[]{"Stoneskin", "Iron skins"}));
          break;

        case 0xE9: // Visual spell hit
          final String s_visuals[] = {"None", "Abjuration", "Alteration",
            "Invocation", "Necromancy", "Conjuration", "Enchantment",
            "Illusion", "Divination", "Armor", "Spirit armor", "Ghost armor",
            "Strength", "Confusion", "Shroud of flame", "Death spell",
            "Disintegrate", "Power word, silence", "Power word, stun",
            "Finger of death", "Mordenkainen's sword", "Monster summoning 1",
            "Monster summoning 2", "Monster summoning 3", "Monster summoning 4",
            "Monster summoning 5", "Monster summoning 6", "Monster summoning 7",
            "Conjure fire elemental", "Conjure earth elemental",
            "Conjure water elemental", "Bless", "Curse", "Prayer", "Recitation",
            "Cure light wounds", "Cure moderate wounds", "Cure serious wounds",
            "Cure critical wounds", "Heal", "Animal summoning 1",
            "Animal summoning 2", "Animal summoning 3", "Slow posion",
            "Neutralize posion", "Call lightning", "Static charge",
            "Remove paralysis", "Free action", "Miscast magic",
            "Strength of one", "Champion's strength", "Flame strike",
            "Raise dead", "Resurrection", "Chaotic commands",
            "Righteous wrath of the faithful", "Sunray", "Spike stones",
            "Dimension door origin", "Dimension door destination",
            "Cone of cold", "Sol's searing orb", "Fire hit", "Cold hit",
            "Electricity hit", "Acid hit", "Paralysis", "Malavon's rage",
            "Righteous wrath of the faithful ground", "Belhifet death",
            "Portal", "Sunscorch", "Blade barrier (front)",
            "Blade barrier (back)", "Circle of bones (front)",
            "Circle of bones (back)", "Cause light wounds",
            "Cause moderate wounds", "Cause serious wounds",
            "Cause critical wounds", "Cause disease", "Poison", "Slay living",
            "Harm", "Destruction", "Exaltation", "Cloudburst", "Mold touch",
            "Lower resistance", "Cat's grace", "Soul eater", "Smashing wave",
            "Suffocate", "Abi-dalzim's horrid wilting",
            "Mordenkainen's force missile", "Vitriolic sphere",
            "Wailing virgin death", "Undead ward", "Wailing virgin hit",
            "Wylfden's death 1", "Wylfden's death 2", "Dragon's death 1",
            "Dragon's death 2", "Monster summoning circle",
            "Animal summoning circle", "Earth summoning circle",
            "Fire summoning circle", "Water summoning circle",
            "Dedlee's electric loop", "Darktree attack"};
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_visuals));
          break;

        case 0xEF: // Slow poison
          s.add(new Unknown(buffer, offset, 4));
          s.add(new DecNumber(buffer, offset + 4, 4, "Factor"));
          break;

        case 0xF1: // Vampiric touch
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Direction",
            new String[]{"Target to caster", "Caster to target"}));
          break;

        case 0xF4: // Prayer
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Prayer type",
            new String[]{"Beneficial", "Detrimental"}));
          break;

        case 0xF7: // Beltyn's burning blood
        case 0x108: // Static charge
        case 0x109: // Cloak of fear
          s.add(new DecNumber(buffer, offset, 4, "# strikes"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0xF9: // Recitation
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Recitation type",
            new String[]{"Beneficial", "Detrimental"}));
          break;

        case 0xFE: // Remove effects with res equals
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Removal type",
            new String[]{"Default", "Equipped effects only",
                         "Limited effects only"}));
          restype = "SPL";
          break;

        case 0xFF: // Salamander aura
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Aura type",
            new String[]{"Fire", "Frost"}));
          break;

        case 0x100: // Umber hulk gaze
          s.add(new DecNumber(buffer, offset, 4, "# seconds"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x105: // Immunity effect and resource
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          restype = "SPL";
          break;

        case 0x114: // Remove effects of type
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Effect", s_effname));
          break;

        case 0x117: // Animal rage
          s.add(new DecNumber(buffer, offset, 4, "# seconds"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x118: // Turn undead
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Turning type",
            new String[]{"Command", "Rebuke", "Destroy", "Panic",
                         "Depend on caster"}));
          break;

        case 0x11B: // Float text
          s.add(new StringRef(buffer, offset, "String"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Text type",
            new String[]{"Default", "Cynicim (while equipped)"}));
          break;

        case 0x11D: // Sleep
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Awaken on damage?", s_yesno));
          break;

        case 0x120: // Set state
          s.add(new Unknown(buffer, offset, 4));
          s.add(new IdsBitmap(buffer, offset + 4, 4, "State", "SPLSTATE.IDS"));
          break;

        case 0x128: // Set global variable
          s.add(new DecNumber(buffer, offset, 4, "Value"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Increase by"));
          restype = "String";
          break;

        case 0x192: // Add effects list
          s.add(new Unknown(buffer, offset, 4));
          s.add(new DecNumber(buffer, offset + 4, 4, "Apply to type"));
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
          break;

        case 0x199: // Righteous wrath of the faithful
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Affect",
            new String[]{"Allies", "Allies and same alignment"}));
          break;

        case 0x19A: // Summon (as ally)
        case 0x19B: // Summon (as enemy)
          s.add(new DecNumber(buffer, offset, 4, "# creatures"));
          s.add(new Bitmap(buffer, offset + 4, 4, "Summon animation",
            new String[]{"No animation", "Monster summoning circle",
                         "Animal summoning circle", "Earth summoning circle",
                         "Fire summoning circle", "Water summoning circle", "",
                         "Puff of smoke"}));
          restype = "CRE";
          break;

        case 0x19C: // Control
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Control type",
            new String[]{"", "Default", "Mental domination"}));
          break;

        case 0x19D: // Visual effect
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
                         "Amount seconds per HP"}));
          break;

        case 0x1A1: // Area effect using effects list
          s.add(new DecNumber(buffer, offset, 4, "Radius"));
          s.add(new Flag(buffer, offset + 4, 4, "Area effect type",
            new String[]{"Instant", "Once per round", "Ignore target"}));
          restype = "SPL";
          break;

        case 0x1A3: // Knocked unconscious
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Awaken on damage?", s_yesno));
          break;

        case 0x1A4: // Death magic
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Flag(buffer, offset + 4, 4, "Death type",
            new String[]{"Acid", "Burning", "Crushing", "Normal", "Exploding",
                         "Stoned", "Freezing", "", "", "", "Permanent",
                         "Destruction"}));
          break;

        case 0x1AD: // When struck using effects list
          restype = "SPL";
          break;

        case 0x1AE: // Projectile type using effects list
          final LongIntegerHashMap<String> m_projmap = new LongIntegerHashMap<String>();
          m_projmap.put(0L, "Default");
          m_projmap.put(243L, "Find traps");
          s.add(new Unknown(buffer, offset, 4));
          s.add(new HashBitmap(buffer, offset + 4, 4, "Projectile type", m_projmap));
          restype = "SPL";
          break;

        case 0x1AF: // Energy drain
          s.add(new DecNumber(buffer, offset, 4, "# levels"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x1B0: // Tortoise shell
          s.add(new DecNumber(buffer, offset, 4, "# HP"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x1B1: // Blink
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Blink type",
            new String[]{"Default", "Empty body"}));
          break;

        case 0x1B2: // Persistent using effects list
          s.add(new DecNumber(buffer, offset, 4, "Interval in seconds"));
          s.add(new Unknown(buffer, offset + 4, 4));
          restype = "SPL";
          break;

        case 0x1B4: // Damage reduction
          s.add(new DecNumber(buffer, offset, 4, "Damage ignored"));
          s.add(new DecNumber(buffer, offset + 4, 4, "Enchantment to overcome"));
          break;

        case 0x1BB: // Protection from arrows
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Damage reduction",
            new String[]{"None", "10/+1", "10/+2", "10/+3", "10/+4", "10/+5"}));
          break;

        case 0x1C1: // Call lightning
          s.add(new DecNumber(buffer, offset, 4, "# strikes"));
          s.add(new Unknown(buffer, offset + 4, 4));
          break;

        case 0x1C2: // Globe of invulnerability
          s.add(new Unknown(buffer, offset, 4));
          s.add(new Bitmap(buffer, offset + 4, 4, "Globe type",
            new String[]{"Minor globe of invulnerability",
                         "Globe of invulnerability"}));
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

