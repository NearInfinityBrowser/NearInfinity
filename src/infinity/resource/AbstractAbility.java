// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.*;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractAbility extends AbstractStruct
{
  protected static final String[] s_type = {"", "Melee", "Ranged", "Magical", "Launcher"};
  protected static final String[] s_targettype = {"", "Living actor", "Inventory", "Dead actor",
                                                  "Any point within range", "Caster", "",
                                                  "Caster (keep spell, no animation)"};
  protected static final String[] s_dmgtype = {"None", "Piercing", "Crushing", "Slashing", "Missile",
                                               "Fist", "Piercing or crushing", "Piercing or slashing",
                                               "Crushing or slashing", "Blunt missile"};

  // ToDo: these are pretty nasty in here?
  protected static final String[] s_proj_iwd = {"", "None", "Arrow",
    "Arrow exploding", "Arrow flaming", "Arrow heavy", "Arrow non-magical",
    "Axe", "Axe exploding", "Axe flaming", "Axe heavy", "Axe non-magical",
    "Bolt", "Bolt exploding", "Bolt flaming", "Bolt heavy", "Bolt non-magical",
    "Bullet", "Bullet exploding", "Bullet flaming", "Bullet heavy",
    "Bullet non-magical", "Burning hands", "Call lightning", "Chromatic orb",
    "Cone of cold", "Cone of fire", "Dagger", "Dagger exploding",
    "Dagger flaming", "Dagger heavy", "Dagger non-magical", "Dart",
    "Dart exploding", "Dart flaming", "Dart heavy", "Dart non-magical",
    "BG magic missile", "Fireball", "Ice fragments", "Lightning bolt",
    "Skipping stone", "Sleep", "Skeleton animation", "Smoke ball",
    "Smoke large", "Smoke small", "Sparkle blue", "Sparkle gold",
    "Sparkle purple", "Sparkle ice", "Sparkle stone", "Sparkle black",
    "Sparkle chromatic", "Sparkle red", "Sparkle green", "Spear",
    "Spear exploding", "Spear flaming", "Spear heavy", "Spear non-magical",
    "Star sprite", "Stoned", "Web travel", "Web ground", "Gaze", "Holy might",
    "Flame strike", "Magic missiles 1", "Magic missiles 2", "Magic missiles 3",
    "Magic missiles 4", "Magic missiles 5", "Magic missiles 6",
    "Magic missiles 7", "Magic missiles 8", "Magic missiles 9",
    "Magic missiles 10", "Magic missiles 11", "Invisible traveling",
    "Fire bolt", "Call lightning chain 1", "Call lightning chain 2",
    "Call lightning chain 3", "Call lightning chain 4",
    "Call lightning chain 5", "Call lightning chain 6",
    "Call lightning chain 7", "Call lightning chain 8",
    "Call lightning chain 9", "Call lightning chain 10",
    "Call lightning chain 11", "Fire storm", "Call lightning storm",
    "Instant area effect", "Stinking cloud", "Skulltrap", "Color spray",
    "Ice storm", "Fire wall", "Glyph", "Grease", "Flame arrow green",
    "Flame arrow blue", "Fireball green", "Fireball blue", "Potion",
    "Potion exploding", "Acid blob", "Agannazar's scorcher", "Travel door",
    "Glow necromancy", "Glow alteration", "Glow enchantment", "Glow abjuration",
    "Glow illusion", "Glow conjure", "Glow invocation", "Glow divination",
    "Hit necromancy air", "Hit necromancy earth", "Hit necromancy water",
    "Hit alteration air", "Hit alteration earth", "Hit alteration water",
    "Hit enchantment air", "Hit enchantment earth", "Hit enchantment water",
    "Hit abjuration air", "Hit abjuration earth", "Hit abjuration water",
    "Hit illusion air", "Hit illusion earth", "Hit illusion water",
    "Hit conjure air", "Hit conjure earth", "Hit conjure water",
    "Hit invocation air", "Hit invocation earth", "Hit invocation water",
    "Hit divination air", "Hit divination earth", "Hit divination water",
    "Hit mushroom fire", "Hit mushroom grey", "Hit mushroom green",
    "Hit shaft fire", "Hit shaft light", "Hit swirl white", "Sparkle area blue",
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
    "Sparkle area orange", "Sparkle magenta", "Sparkle orange",
    "Non-sprite area", "Cloudkill", "Flame arrow ice", "Cow", "Hold person",
    "Scorcher ice", "Acid blob mustard", "Acid blob grey", "Acid blob ochre",
    "Red holy might", "Hit necromancy area", "Hit alteration area",
    "Hit enchantment area", "Hit abjuration area", "Hit illusion area",
    "Hit conjure area", "Hit invocation area", "Hit divination area",
    "Fireball white", "Instant area effect small", "Lightning bolt ground",
    "Lightning no bounce", "Hit finger of death", "Malavon's rage",
    "Chain lightning", "Acid storm", "Death fog", "Spike stones",
    "Incendiary cloud", "Produce fire", "Insect plague",
    "Snilloc's snowball swarm", "Magic missile", "Hit abjuration",
    "Hit alteration", "Hit invocation", "Hit necromancy", "Hit conjuration",
    "Hit enchantment", "Hit illusion", "Hit divination", "Travel abjuration",
    "Travel alteration", "Travel invocation", "Travel necromancy",
    "Travel conjuration", "Travel enchantment", "Travel illusion",
    "Travel divination", "Entangle", "Bless", "Curse", "Remove fear",
    "Detect evil", "Detect invisibility", "Horror", "Resist fear", "Chant",
    "Find traps", "Silence 15' radius", "Dispel magic", "Haste", "Slow",
    "Hold animal", "Remove paralysis", "Icelance", "Strength of one", "Prayer",
    "Confusion (wizard)", "Emotion", "Malison", "Defensive harmony",
    "Protection from evil 10' radius", "Cloak of fear", "Recitation",
    "Otiluke's resilient sphere", "Static charge", "Hold monster", "Chaos",
    "Shroud of flame", "Righteous wrath of the faithful", "Death spell",
    "Disintegrate", "Otiluke's freezing sphere", "Fire seed",
    "Sol's searing orb", "Prismatic spray", "Mass invisibility", "Sunray",
    "Confusion (priest)", "Symbol of pain", "Symbol of hopelessness",
    "Power word, kill", "Malavon's corrosive fog", "Salamander aura",
    "Umber hulk gaze", "Bombardier beetle cloud", "Zombie lord aura",
    "Iron golem cloud", "Myconid spores", "Incendiary cloud explosion",
    "Incendiary cloud idling", "Heavenly inferno", "Area hit monster summoning",
    "Area hit animal summoning", "Area hit conjure earth elemental",
    "Area hit conjure fire elemental", "Area hit conjure water elemental",
    "Portal animation open", "Horror trap", "Winter wolf breath",
    "Portal animation close", "Alicorn lance", "Soul eater", "Spike growth",
    "Cloudburst", "Smashing wave", "Thorn spray", "Wall of moonlight",
    "Whirlwind", "Earthquake", "Mist of eldath", "Circle of bones",
    "Cloud of pestilence", "Undead ward", "Blade barrier", "Spiritual wrath",
    "Lance of disruption", "Mordenkainen's force missiles", "Shout",
    "Vitriolic sphere", "Suffocate", "Abi-dalzim's horrid wilting",
    "Great shout", "Mournful wail", "Death knell", "War cry", "Undying lament",
    "Mordenkainen's force missiles 1", "Mordenkainen's force missiles 2",
    "Mordenkainen's force missiles 3", "Mordenkainen's force missiles 4",
    "Mordenkainen's force missiles 5", "Mordenkainen's force missiles 6",
    "Mordenkainen's force missiles 7", "Mordenkainen's force missiles 8",
    "Mordenkainen's force missiles 9", "Mordenkainen's force missiles 10",
    "Mordenkainen's force missiles 11", "Sunfire", "Power word, blind",
    "Holy smite", "Unholy blight", "Greater command", "Holy word",
    "Unholy word", "Great roar", "Will-o-wisp spray",
    "Retribution (single projectile)", "Retribution", "Sekolah's fire",
    "Blue glow", "Dragon gem cutscene", "Dragon breath",
    "Crypt thing teleport", "Mustard jelly vapor", "Summon cornugons",
    "Container glow", "Container glow bad", "Crypt thing teleport (fighter)",
    "Crypt thing teleport (thief)", "Hold undead", "Invisibility 10' radius",
    "Mass cure", "Delayed blast fireball", "Area hit gate",
    "Wail of the banshee", "Symbol, fear", "Symbol, stun", "Symbol, death",
    "Meteor swarm", "Improved haste", "Frost fingers", "Gedlee's electric loop",
    "Wall of fire", "Aura of vitality", "Banishment", "Mass dominate",
    "Mind flayer psionic blast", "Boulder", "Turn undead", "Mind fog",
    "Half-dragon acid breath", "Half-dragon fire breath",
    "Half-dragon lightning breath", "Guardian acid breath", "Ultrablast",
    "Boulder big (trap)", "Fire trap", "Acid trap", "Chimera fire breath"};
  protected static final String[] s_proj_pst = {"", "None", "Arrow",
    "Arrow exploding", "Arrow flaming", "Arrow heavy", "Arrow shocking", "Axe",
    "Axe exploding", "Axe flaming", "Axe heavy", "Axe shocking", "Bolt",
    "Bolt exploding", "Bolt flaming", "Bolt heavy", "Bolt shocking", "Bullet",
    "Bullet exploding", "Bullet flaming", "Bullet heavy", "Bullet shocking",
    "Burning hands", "Call lightning old", "Chromatic orb",
    "BG cone of cold", "Cone of fire", "Dagger", "Dagger exploding",
    "Dagger flaming", "Dagger heavy", "Dagger shocking", "Dart",
    "Dart exploding", "Dart flaming", "Dart heavy", "Dart shocking",
    "Magic missile", "Fireball", "Ice fragments", "Lightning bolt",
    "Skipping stone", "Sleep", "Skeleton animation", "Smoke ball",
    "Smoke large", "Smoke small", "Sparkle blue", "Sparkle gold",
    "Sparkle purple", "Sparkle ice", "Sparkle stone", "Sparkle black",
    "Sparkle chromatic", "Sparkle red", "Sparkle green", "Spear",
    "Spear exploding", "Spear flaming", "Spear heavy", "Spear shocking",
    "Star sprite", "Stoned", "Web travel", "Web ground", "Gaze", "Holy might",
    "Flame strike", "BG magic missiles 1", "BG magic missiles 2",
    "BG magic missiles 3", "BG magic missiles 4", "BG magic missiles 5",
    "BG magic missiles 6", "BG magic missiles 7", "BG magic missiles 8",
    "BG magic missiles 9", "BG magic missiles 10", "BG magic missiles 11",
    "Invisible traveling", "Fire bolt", "Call lightning chain 1",
    "Call lightning chain 2", "Call lightning chain 3",
    "Call lightning chain 4", "Call lightning chain 5",
    "Call lightning chain 6", "Call lightning chain 7",
    "Call lightning chain 8", "Call lightning chain 9",
    "Call lightning chain 10", "Call lightning chain 11", "Fire storm",
    "Call lighting storm", "Instant area effect", "Stinking cloud", "Skulltrap",
    "Color spray", "Ice storm", "Fire wall", "Glyph", "Grease",
    "Flame arrow green", "Flame arrow blue", "Fireball green", "Fireball blue",
    "Potion", "Potion exploding", "Acid blob", "Agannazar's scorcher",
    "Travel door", "Glow necromancy", "Glow alteration", "Glow enchantment",
    "Glow abjuration", "Glow illusion", "Glow conjuration", "Glow invocation",
    "Glow divination", "Hit necromancy air", "Hit necromancy earth",
    "Hit necromancy water", "Hit alteration air", "Hit alteration earth",
    "Hit alteration water", "Hit enchantment air", "Hit enchantment earth",
    "Hit enchantment water", "Hit abjuration air", "Hit abjuration earth",
    "Hit abjuration water", "Hit illusion air", "Hit illusion earth",
    "Hit illusion water", "Hit conjuration air", "Hit conjuration earth",
    "Hit conjuration water", "Hit invocation air", "Hit invocation earth",
    "Hit invocation water", "Hit divination air", "Hit divination earth",
    "Hit divination water", "Hit mushroom fire", "Hit mushroom gray",
    "Hit mushroom green", "Hit shaft fire", "Hit shaft light",
    "Hit shaft white", "Sparkle area blue", "Sparkle area gold",
    "Sparkle area purple", "Sparkle area ice", "Sparkle area stone",
    "Sparkle area black", "Sparkle area chromatic", "Sparkle area red",
    "Sparkle area green", "Instant area (party only)",
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
    "Sparkle area orange", "Sparkle magenta", "Sparkle orange",
    "Non-sprite area", "BG cloudkill", "Flame arrow ice", "Cow",
    "Hold person", "Scorcher ice", "Acid blob mustard", "Acid blob gray",
    "Acid blob ochre", "Red holy might", "Hit necromancy area",
    "Hit alteration area", "Hit enchantment area", "Hit abjuration area",
    "Hit illusion area", "Hit conjuration area", "Hit invocation area",
    "Hit divination area", "Litany of curses", "Stories-bones-tell",
    "Magic missiles 1", "Magic missiles 2", "Magic missiles 3",
    "Magic missiles 4", "Magic missiles 5", "Magic missiles 6",
    "Magic missiles 7", "Magic missiles 8", "Magic missiles 9",
    "Magic missiles 10", "Skull mob", "Skull mob 2", "Swarm curse",
    "Swarm curse 2", "Adder's kiss", "Ice knife", "Pacify", "Strength",
    "Ball lightning", "Ball lightning 2", "Blood bridge", "Force missiles",
    "Improved strength", "Shroud of shadows", "Cloudkill",
    "Howl of pandemonium", "Bladestorm", "Elysium's fires", "Abyssal fury",
    "Horror", "Knock", "Hold undead", "Missile of patience", "Elysium's tears",
    "Tasha's unbearable derisive laughter", "Axe of torment", "Blacksphere",
    "Cone of cold", "Desert hell", "Fire and ice", "Chain lightning storm",
    "Acid storm", "Stygian ice storm", "Meteor storm bombardment", "Deathbolt",
    "Ignus' fury", "Power word, blind", "Mechanus' cannon", "Celestial host",
    "Rune of torment", "Blessing", "Curse", "", "Halo of lesser revelation",
    "Spiritual hammer", "Call lightning", "", "Vampiric touch", "Confusion",
    "Power word, kill", "Globe of invulnerability", "Nordom's crossbow bolts",
    "Raise dead", "Aura of fear", "Conflagration", "Special trap",
    "Ignus' fireball", "Tongues of flame 1", "Tongues of flame 2",
    "Tongues of flame 3", "Tongues of flame 4", "Tongues of flame 5",
    "Ignus' terror", "Infernal orb", "Fiery rain", "Elemental strike",
    "Reign of anger 1", "Reign of anger 2", "Reign of anger 3",
    "Reign of anger 4", "Reign of anger 5", "Power of one", "Succubus kiss",
    "Embalming"};
  protected static final String[] s_projectile = {"", "None", "Arrow",
    "Arrow exploding", "Arrow flaming", "Arrow heavy", "Arrow shocking", "Axe",
    "Axe exploding", "Axe flaming", "Axe heavy", "Axe shocking", "Bolt",
    "Bolt exploding", "Bolt flaming", "Bolt heavy", "Bolt shocking", "Bullet",
    "Bullet exploding", "Bullet flaming", "Bullet heavy", "Bullet shocking",
    "Burning hands", "Call lightning", "Chromatic orb", "Cone of cold",
    "Cone of fire", "Dagger", "Dagger exploding", "Dagger flaming",
    "Dagger heavy", "Dagger shocking", "Dart", "Dart exploding", "Dart flaming",
    "Dart heavy", "Dart shocking", "Magic missile", "Fireball", "Ice fragments",
    "Lightning bolt", "Skipping stone", "Sleep", "Skeleton animation",
    "Smoke ball", "Smoke large", "Smoke small", "Sparkle blue", "Sparkle gold",
    "Sparkle purple", "Sparkle ice", "Sparkle stone", "Sparkle black",
    "Sparkle chromatic", "Sparkle red", "Sparkle green", "Spear",
    "Spear exploding", "Spear flaming", "Spear heavy", "Spear shocking",
    "Star sprite", "Stoned", "Web travel", "Web ground", "Gaze", "Holy might",
    "Flame strike", "Magic missiles 1", "Magic missiles 2", "Magic missiles 3",
    "Magic missiles 4", "Magic missiles 5", "Magic missiles 6",
    "Magic missiles 7", "Magic missiles 8", "Magic missiles 9",
    "Magic missiles 10", "Magic missiles 11", "Invisible traveling",
    "Fire bolt", "Call lightning chain 1", "Call lightning chain 2",
    "Call lightning chain 3", "Call lightning chain 4",
    "Call lightning chain 5", "Call lightning chain 6",
    "Call lightning chain 7", "Call lightning chain 8",
    "Call lightning chain 9", "Call lightning chain 10",
    "Call lightning chain 11", "Fire storm", "Call lightning storm",
    "Instant area effect", "Cloud", "Skulltrap", "Color spray", "Ice storm",
    "Fire wall", "Glyph", "Grease", "Flame arrow green", "Flame arrow blue",
    "Fireball green", "Fireball blue", "Potion", "Potion exploding",
    "Acid blob", "Agannazar's scorcher", "Travel door", "Glow necromancy",
    "Glow alteration", "Glow enchantment", "Glow abjuration", "Glow illusion",
    "Glow conjure", "Glow invocation", "Glow divination", "Hit necromancy air",
    "Hit necromancy earth", "Hit necromancy water", "Hit alteration air",
    "Hit alteration earth", "Hit alteration water", "Hit enchantment air",
    "Hit enchantment earth", "Hit enchantment water", "Hit abjuration air",
    "Hit abjuration earth", "Hit abjuration water", "Hit illusion air",
    "Hit illusion earth", "Hit illusion water", "Hit conjure air",
    "Hit conjure earth", "Hit conjure water", "Hit invocation air",
    "Hit invocation earth", "Hit invocation water", "Hit divination air",
    "Hit divination earth", "Hit divination water", "Hit mushroom fire",
    "Hit mushroom grey", "Hit mushroom green", "Hit shaft fire",
    "Hit shaft light", "Hit swirl white", "Sparkle area blue",
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
    "Sparkle area orange", "Sparkle magenta", "Sparkle orange",
    "Non-sprite area", "Cloudkill", "Flame arrow ice", "Cow", "Hold",
    "Scorcher ice", "Acid blob mustard", "Acid blob grey", "Acid blob ochre",
    "Red holy might", "Hit necromancy area", "Hit alteration area",
    "Hit enchantment area", "Hit abjuration area", "Hit illusion area",
    "Hit conjure area", "Hit invocation area", "Hit divination area",
    "Fireball white", "Instant area effect small", "Lightning bolt ground",
    "Lightning no bounce", "Hit finger of death", "Beholder blast",
    "Hold necromancy", "Fireball ignore center", "Area ignore center",
    "Chain lightning", "Trap snare", "Small instant area",
    "Small area ignore center", "Glyph trap (not party)",
    "Small area (not party)", "Disintegrate", "Lightning bolt no bounce",
    "Spell attack", "Medium instant area", "Cloud (not party)", "Golem cloud",
    "Icewind glyph", "Fire seed", "Icewind insect", "Icewind chain insect",
    "Meteor swarm", "Chain insect", "Icewind glyph hit", "Insect hit",
    "Energy spear", "Fireball 3d", "Screaming skull", "Small comet", "Wilting",
    "Weird field (not party)", "Anti-fireball", "Skulltrap (not party)",
    "New lightning bolt (no bounce)", "Pillar of light", "Entangle area effect",
    "Delayed blast fireball", "Trap dart", "Red dragon breath",
    "Black dragon breath", "Silver dragon breath", "New color spray",
    "New cone of cold", "Holy blight", "Unholy blight", "Prismatic spray",
    "Instant area large (not party)", "Holy light pillar", "Red dragon hit",
    "Red dragon middle", "Fireball (just projectile)", "New hold necromancy",
    "Web (one person)", "Holy word (not party)", "Unholy word (not party)",
    "Power word, sleep", "MDK bullet", "Storm of vengeance", "Comet"};
  
  protected AbstractAbility(AbstractStruct superStruct, String name, byte buffer[], int offset)
          throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      Writeable w = list.get(i);
      if (w instanceof Effect)
        return;
      w.write(os);
    }
  }

// --------------------- End Interface Writeable ---------------------

  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof Effect && getEffectsCount() >= 1) {
      SectionOffset effectOffset = (SectionOffset)getSuperStruct().getAttribute("Effects offset");
      int effectIndex = ((DecNumber)getAttribute("First effect index")).getValue() + getEffectsCount() - 1;
      datatype.setOffset(effectOffset.getValue() + effectIndex * 48);
    }
  }

  public int getEffectsCount()
  {
    return ((SectionCount)getAttribute("# effects")).getValue();
  }

  public void incEffectsIndex(int value)
  {
    ((DecNumber)getAttribute("First effect index")).incValue(value);
  }

  public int readEffects(byte buffer[], int off) throws Exception
  {
    int effect_count = ((SectionCount)getAttribute("# effects")).getValue();
    for (int i = 0; i < effect_count; i++) {
      Effect eff = new Effect(this, buffer, off);
      off = eff.getEndOffset();
      list.add(eff);
    }
    return off;
  }

  public void setEffectsIndex(int value)
  {
    ((DecNumber)getAttribute("First effect index")).setValue(value);
  }

  public void writeEffects(OutputStream os) throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      Writeable w = list.get(i);
      if (w instanceof Effect)
        w.write(os);
    }
  }
}

