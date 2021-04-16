// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.DirDef;
import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.tuples.Couple;

/**
 * Creature animation decoder for processing type 5000/6000 (character) animations.
 * Available ranges: [5000,53ff], [5500,55ff], [6000,63ff], [6500,65ff]
 */
public class CharacterDecoder extends CharacterBaseDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.CHARACTER;

  public static final DecoderAttribute KEY_SPLIT_BAMS             = DecoderAttribute.with("split_bams", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_HEIGHT_CODE_SHIELD     = DecoderAttribute.with("height_code_shield", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF_PAPERDOLL       = DecoderAttribute.with("resref_paperdoll", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF_ARMOR_BASE      = DecoderAttribute.with("resref_armor_base", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF_ARMOR_SPECIFIC  = DecoderAttribute.with("resref_armor_specific", DecoderAttribute.DataType.STRING);

  /** Assigns BAM suffix and cycle index to a specific animation sequence (unsplit version). */
  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapUnsplit =
      new HashMap<Sequence, Couple<String, Integer>>() {{
        put(Sequence.ATTACK_SLASH_1H, Couple.with("A1", 0));
        put(Sequence.ATTACK_SLASH_2H, Couple.with("A2", 0));
        put(Sequence.ATTACK_BACKSLASH_1H, Couple.with("A3", 0));
        put(Sequence.ATTACK_BACKSLASH_2H, Couple.with("A4", 0));
        put(Sequence.ATTACK_JAB_1H, Couple.with("A5", 0));
        put(Sequence.ATTACK_JAB_2H, Couple.with("A6", 0));
        put(Sequence.ATTACK_2WEAPONS1, Couple.with("A7", 0));
        put(Sequence.ATTACK_OVERHEAD, Couple.with("A8", 0));
        put(Sequence.ATTACK_2WEAPONS2, Couple.with("A9", 0));
        put(Sequence.ATTACK_BOW, Couple.with("SA", 0));
        put(Sequence.ATTACK_SLING, Couple.with("SS", 0));
        put(Sequence.ATTACK_CROSSBOW, Couple.with("SX", 0));
        put(Sequence.SPELL, Couple.with("CA", 0));
        put(Sequence.SPELL1, get(Sequence.SPELL));
        put(Sequence.SPELL2, Couple.with("CA", 18));
        put(Sequence.SPELL3, Couple.with("CA", 36));
        put(Sequence.SPELL4, Couple.with("CA", 54));
        put(Sequence.CAST, Couple.with("CA", 9));
        put(Sequence.CAST1, get(Sequence.CAST));
        put(Sequence.CAST2, Couple.with("CA", 27));
        put(Sequence.CAST3, Couple.with("CA", 45));
        put(Sequence.CAST4, Couple.with("CA", 63));
        put(Sequence.WALK, Couple.with("G1", 0));
        put(Sequence.STANCE, Couple.with("G1", 9));
        put(Sequence.STANCE2, Couple.with("G1", 27));
        put(Sequence.STAND, Couple.with("G1", 18));
        put(Sequence.STAND2, Couple.with("G1", 63));
        put(Sequence.STAND3, Couple.with("G1", 72));
        put(Sequence.GET_HIT, Couple.with("G1", 36));
        put(Sequence.DIE, Couple.with("G1", 45));
        put(Sequence.TWITCH, Couple.with("G1", 54));
        put(Sequence.SLEEP, Couple.with("G1", 81));
        put(Sequence.GET_UP, Couple.with("!G1", 81));
        put(Sequence.SLEEP2, Couple.with("G1", 90));
        put(Sequence.GET_UP2, Couple.with("!G1", 90));
      }};

  /** Assigns BAM suffix and cycle index to a specific animation sequence (split version). */
  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapSplit =
      new HashMap<Sequence, Couple<String, Integer>>() {{
        put(Sequence.ATTACK_SLASH_1H, Couple.with("A1", 0));
        put(Sequence.ATTACK_SLASH_2H, Couple.with("A2", 0));
        put(Sequence.ATTACK_BACKSLASH_1H, Couple.with("A3", 0));
        put(Sequence.ATTACK_BACKSLASH_2H, Couple.with("A4", 0));
        put(Sequence.ATTACK_JAB_1H, Couple.with("A5", 0));
        put(Sequence.ATTACK_JAB_2H, Couple.with("A6", 0));
        put(Sequence.ATTACK_2WEAPONS1, Couple.with("A7", 0));
        put(Sequence.ATTACK_OVERHEAD, Couple.with("A8", 0));
        put(Sequence.ATTACK_2WEAPONS2, Couple.with("A9", 0));
        put(Sequence.ATTACK_BOW, Couple.with("SA", 0));
        put(Sequence.ATTACK_SLING, Couple.with("SS", 0));
        put(Sequence.ATTACK_CROSSBOW, Couple.with("SX", 0));
        put(Sequence.SPELL, Couple.with("CA", 0));
        put(Sequence.SPELL1, get(Sequence.SPELL));
        put(Sequence.SPELL2, Couple.with("CA", 18));
        put(Sequence.SPELL3, Couple.with("CA", 36));
        put(Sequence.SPELL4, Couple.with("CA", 54));
        put(Sequence.CAST, Couple.with("CA", 9));
        put(Sequence.CAST1, get(Sequence.CAST));
        put(Sequence.CAST2, Couple.with("CA", 27));
        put(Sequence.CAST3, Couple.with("CA", 45));
        put(Sequence.CAST4, Couple.with("CA", 63));
        put(Sequence.WALK, Couple.with("G11", 0));
        put(Sequence.STANCE, Couple.with("G1", 9));
        put(Sequence.STANCE2, Couple.with("G13", 27));
        put(Sequence.STAND, Couple.with("G12", 18));
        put(Sequence.STAND2, Couple.with("G17", 63));
        put(Sequence.STAND3, Couple.with("G18", 72));
        put(Sequence.GET_HIT, Couple.with("G15", 36));
        put(Sequence.DIE, Couple.with("G15", 45));
        put(Sequence.TWITCH, Couple.with("G16", 54));
        put(Sequence.SLEEP, Couple.with("G19", 81));
        put(Sequence.GET_UP, Couple.with("!G19", 81));
        put(Sequence.SLEEP2, Couple.with("G19", 90));
        put(Sequence.GET_UP2, Couple.with("!G19", 90));
      }};

  /** Set of invalid attack type / animation sequence combinations. */
  private static final EnumMap<AttackType, EnumSet<Sequence>> forbiddenSequences =
      new EnumMap<AttackType, EnumSet<Sequence>>(AttackType.class) {{
        put(AttackType.ONE_HANDED, EnumSet.of(Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                              Sequence.ATTACK_2WEAPONS1, Sequence.ATTACK_2WEAPONS2, Sequence.ATTACK_OVERHEAD,
                                              Sequence.ATTACK_BOW, Sequence.ATTACK_SLING, Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.TWO_HANDED, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                              Sequence.ATTACK_2WEAPONS1, Sequence.ATTACK_2WEAPONS2, Sequence.ATTACK_OVERHEAD,
                                              Sequence.ATTACK_BOW, Sequence.ATTACK_SLING, Sequence.ATTACK_CROSSBOW, Sequence.STANCE));
        put(AttackType.TWO_WEAPON, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                              Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                              Sequence.ATTACK_OVERHEAD, Sequence.ATTACK_BOW, Sequence.ATTACK_SLING, Sequence.ATTACK_CROSSBOW,
                                              Sequence.STANCE2));
        put(AttackType.THROWING, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                            Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                            Sequence.ATTACK_2WEAPONS1, Sequence.ATTACK_2WEAPONS2, Sequence.ATTACK_BOW,
                                            Sequence.ATTACK_SLING, Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.BOW, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                       Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                       Sequence.ATTACK_2WEAPONS1, Sequence.ATTACK_2WEAPONS2, Sequence.ATTACK_OVERHEAD,
                                       Sequence.ATTACK_SLING, Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.SLING, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                         Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                         Sequence.ATTACK_2WEAPONS1, Sequence.ATTACK_2WEAPONS2, Sequence.ATTACK_OVERHEAD,
                                         Sequence.ATTACK_BOW, Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.CROSSBOW, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                            Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                            Sequence.ATTACK_2WEAPONS1, Sequence.ATTACK_2WEAPONS2, Sequence.ATTACK_OVERHEAD,
                                            Sequence.ATTACK_BOW, Sequence.ATTACK_SLING, Sequence.STANCE2));
      }};

  /**
   * A helper method that parses the specified data array and generates a {@link IniMap} instance out of it.
   * @param data a String array containing table values for a specific table entry.
   * @return a {@code IniMap} instance with the value derived from the specified data array.
   *         Returns {@code null} if no data could be derived.
   */
  public static IniMap processTableData(String[] data)
  {
    IniMap retVal = null;
    if (data == null || data.length < 16) {
      return retVal;
    }

    String resref = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF, "");
    if (resref.isEmpty()) {
      return retVal;
    }
    int equipHelmet = SpriteTables.valueToInt(data, SpriteTables.COLUMN_HELMET, 0);
    int splitBams = SpriteTables.valueToInt(data, SpriteTables.COLUMN_SPLIT, 0);
    int falseColor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_CLOWN, 0);
    String heightCode = SpriteTables.valueToString(data, SpriteTables.COLUMN_HEIGHT, "");
    String heightCodeHelmet = heightCode;
    String heightCodeShield = SpriteTables.valueToString(data, SpriteTables.COLUMN_HEIGHT_SHIELD, "");
    String resrefSpecific = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF2, "");

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[character]");
    lines.add("equip_helmet=" + equipHelmet);
    lines.add("split_bams=" + splitBams);
    lines.add("false_color=" + falseColor);
    lines.add("resref=" + resref);
    if (!heightCode.isEmpty()) {
      lines.add("height_code=" + heightCode);
    }
    if (!heightCodeHelmet.isEmpty()) {
      lines.add("height_code_helmet=" + heightCodeHelmet);
    }
    if (!heightCodeShield.isEmpty()) {
      lines.add("height_code_shield=" + heightCodeShield);
    }
    lines.add("resref_armor_base=" + resref.charAt(resref.length() - 1));
    if (!resrefSpecific.isEmpty()) {
      lines.add("resref_armor_specific=" + resrefSpecific.charAt(resrefSpecific.length() - 1));
    }

    retVal = IniMap.from(lines);

    return retVal;
  }

  public CharacterDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public CharacterDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns the correct sequence map for the current settings. */
  private HashMap<Sequence, Couple<String, Integer>> getSuffixMap()
  {
    return isSplittedBams() ? suffixMapSplit : suffixMapUnsplit;
  }

  /** Returns whether animations are spread over various subfiles. */
  public boolean isSplittedBams() { return getAttribute(KEY_SPLIT_BAMS); }
  protected void setSplittedBams(boolean b) { setAttribute(KEY_SPLIT_BAMS, b); }

  /** Returns the height code prefix for shield overlay sprites. Falls back to generic height code if needed. */
  public String getShieldHeightCode()
  {
    String retVal = getAttribute(KEY_HEIGHT_CODE_SHIELD);
    if (retVal.isEmpty()) {
      retVal = getHeightCode();
    }
    return retVal;
  }

  protected void setShieldHeightCode(String s)
  {
    if (s != null && !s.isEmpty()) {
      // Discard if shield height code refers to non-existing overlays
      List<ResourceEntry> resList = ResourceFactory.getResources(s + "..G1\\.BAM");
      if (resList.isEmpty()) {
        s = "";
      }
    }
    setAttribute(KEY_HEIGHT_CODE_SHIELD, s);
    }

  /** Returns the paperdoll resref. */
  public String getPaperdollResref() { return getAttribute(KEY_RESREF_PAPERDOLL); }
  protected void setPaperdollResref(String s) { setAttribute(KEY_RESREF_PAPERDOLL, s); }

  /**
   * Returns the animation resref for lesser armor types.
   * Returns the same value as {@link #getAnimationResref()} if no base armor code is available.
   */
  public String getArmorBaseResref() { return getAttribute(KEY_RESREF_ARMOR_BASE); }
  protected void setArmorBaseResref(String s)
  {
    if (s.isEmpty()) {
      s = getAnimationResref();
    } else {
      s = getAnimationResref().substring(0, 3) + s.substring(0, 1);
    }
    setAttribute(KEY_RESREF_ARMOR_BASE, s);
  }

  /**
   * Returns the animation resref for greater armor types.
   * Returns the same value as {@link #getAnimationResref()} if no specific armor code is available.
   */
  public String getArmorSpecificResref() { return getAttribute(KEY_RESREF_ARMOR_SPECIFIC); }
  protected void setArmorSpecificResref(String s)
  {
    if (s.isEmpty()) {
      s = getAnimationResref();
    } else {
      s = getAnimationResref().substring(0, 3) + s.substring(0, 1);
    }
    setAttribute(KEY_RESREF_ARMOR_SPECIFIC, s);
  }

  /**
   * Sets the maximum armor code value uses as suffix in animation filenames.
   * Specify -1 to detect value automatically.
   */
  @Override
  protected void setMaxArmorCode(int v)
  {
    if (v < 0) {
      // autodetection: requires fully initialized resref definitions
      final String[] resrefs = { getArmorBaseResref(), getArmorSpecificResref() };
      if (getArmorBaseResref().equalsIgnoreCase(getArmorSpecificResref())) {
        resrefs[1] = null;
      }
      for (final String resref : resrefs) {
        if (resref != null && !resref.isEmpty()) {
          for (int i = 1; i < 10; i++) {
            if (ResourceFactory.resourceExists(resref + i + "G1.BAM")) {
              v = Math.max(v, i);
            }
          }
        }
      }
    }
    super.setMaxArmorCode(v);
  }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = null;
    String resref1 = getAnimationResref();
    String resref2 = getArmorSpecificResref();

    if (essential) {
      HashSet<String> files = new HashSet<>();
      for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : getSuffixMap().entrySet()) {
        String suffix = SegmentDef.fixBehaviorSuffix(entry.getValue().getValue0());
        if (suffix.startsWith("G")) {
          for (int i = 1; i <= getMaxArmorCode(); i++) {
            String resref = resref2 + i + suffix + ".BAM";
            if (!ResourceFactory.resourceExists(resref)) {
              resref = resref1 + i + suffix + ".BAM";
            }
            files.add(resref);
          }
        }
      }
      retVal = new ArrayList<>(Arrays.asList(files.toArray(new String[files.size()])));
    } else {
      // collecting suffixes
      HashSet<String> actionSet = new HashSet<>();
      for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : getSuffixMap().entrySet()) {
        String suffix = SegmentDef.fixBehaviorSuffix(entry.getValue().getValue0());
        actionSet.add(suffix);
      }

      // generating file list
      retVal = new ArrayList<String>() {{
        for (int i = 1; i <= getMaxArmorCode(); i++) {
          for (final String a : actionSet) {
            String resref = resref2 + i + a + ".BAM";
            if (!ResourceFactory.resourceExists(resref)) {
              resref = resref1 + i + a + ".BAM";
            }
            add(resref);
          }
        }
      }};
    }

    return retVal;
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected void init() throws Exception
  {
    super.init();
    IniMapSection section = getSpecificIniSection();
    setSplittedBams(section.getAsInteger(KEY_SPLIT_BAMS.getName(), 0) != 0);
    setShieldHeightCode(section.getAsString(KEY_HEIGHT_CODE_SHIELD.getName(), ""));
    setPaperdollResref(section.getAsString(KEY_RESREF_PAPERDOLL.getName(), ""));
    setArmorBaseResref(section.getAsString(KEY_RESREF_ARMOR_BASE.getName(), ""));
    setArmorSpecificResref(section.getAsString(KEY_RESREF_ARMOR_SPECIFIC.getName(), ""));
    if (getMaxArmorCode() == 0) {
      setMaxArmorCode(-1);
    }
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;

    if (!getSuffixMap().containsKey(seq)) {
      return retVal;
    }

    // getting armor level
    int armorCode = getArmorCode();
    if (armorCode > getMaxArmorCode()) {
      return retVal;
    }

    // preparing shield slot data
    boolean isLefthandedWeapon = false;
    String prefixShield = getShieldHeightCode();
    String codeShield = "";
    if (!prefixShield.isEmpty()) {
      ItemInfo itmShield = getCreatureInfo().getEquippedShield();
      if (itmShield != null) {
        codeShield = itmShield.getAppearance().trim();
        isLefthandedWeapon = !codeShield.isEmpty() && ItemInfo.testAll(itmShield, ItemInfo.FILTER_WEAPON_MELEE_LEFT_HANDED);
      }
    }

    // getting attack type
    ItemInfo itmWeapon = getCreatureInfo().getEquippedWeapon();
    int itmAbility = getCreatureInfo().getSelectedWeaponAbility();
    AttackType attackType = getAttackType(itmWeapon, itmAbility, isLefthandedWeapon);

    EnumSet<Sequence> sequences = forbiddenSequences.get(attackType);
    if (sequences != null && sequences.contains(seq)) {
      // sequence not allowed for selected weapon
      return retVal;
    }

    ArrayList<Couple<String, SegmentDef.SpriteType>> resrefList = new ArrayList<>();

    // adding creature resref
    String creSuffix = getSuffixMap().get(seq).getValue0();
    SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(creSuffix);
    creSuffix = SegmentDef.fixBehaviorSuffix(creSuffix);
    String creResref = (armorCode > 1) ? getArmorSpecificResref() : getArmorBaseResref();
    if (!ResourceFactory.resourceExists(creResref + armorCode + creSuffix + ".BAM")) {
      creResref = getArmorBaseResref();
      if (!ResourceFactory.resourceExists(creResref + armorCode + creSuffix + ".BAM")) {
        creResref = getAnimationResref();
      }
    }
    creResref += armorCode;
    resrefList.add(Couple.with(creResref, SegmentDef.SpriteType.AVATAR));

    String prefix;
    // adding helmet overlay
    if (isHelmetEquipped()) {
      prefix = getHelmetHeightCode();
      if (!prefix.isEmpty()) {
        ItemInfo itmHelmet = getCreatureInfo().getEquippedHelmet();
        if (itmHelmet != null) {
          String code = itmHelmet.getAppearance().trim();
          if (code.length() == 2) {
            resrefList.add(Couple.with(prefix + code, SegmentDef.SpriteType.HELMET));
          }
        }
      }
    }

    // adding shield overlay
    if (!prefixShield.isEmpty() && !codeShield.isEmpty()) {
      resrefList.add(Couple.with(prefixShield + codeShield, SegmentDef.SpriteType.SHIELD));
    }

    // adding weapon overlay
    prefix = getHeightCode();
    if (itmWeapon != null && !prefix.isEmpty()) {
      String code = itmWeapon.getAppearance().trim();
      if (code.length() == 2) {
        resrefList.add(Couple.with(prefix + code, SegmentDef.SpriteType.WEAPON));
      }
    }

    retVal = new SeqDef(seq);
    for (final Couple<String, SegmentDef.SpriteType> data: resrefList) {
      // getting BAM suffix and cycle index
      prefix = data.getValue0();
      SegmentDef.SpriteType spriteType = data.getValue1();
      HashMap<Sequence, Couple<String, Integer>> curSuffixMap = (spriteType == SegmentDef.SpriteType.AVATAR) ? getSuffixMap() : suffixMapUnsplit;
      String suffix = SegmentDef.fixBehaviorSuffix(curSuffixMap.get(seq).getValue0());
      int cycleIdx = curSuffixMap.get(seq).getValue1().intValue();

      // enabling left-handed weapon overlay if available
      if (spriteType == SegmentDef.SpriteType.SHIELD && isLefthandedWeapon) {
        if (ResourceFactory.resourceExists(prefix + "O" + suffix + ".BAM")) {
          prefix += "O";
        }
      }

      // defining sequences
      ResourceEntry entry = ResourceFactory.getResourceEntry(prefix + suffix + ".BAM");
      if (SpriteUtils.bamCyclesExist(entry, cycleIdx, SeqDef.DIR_FULL_W.length)) {
        SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_FULL_W, false, entry, cycleIdx, spriteType, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
        tmp = SeqDef.createSequence(seq, SeqDef.DIR_FULL_E, true, entry, cycleIdx + 1, spriteType, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
      }
    }

    if (retVal.isEmpty()) {
      retVal = null;
    }

    return retVal;
  }
}
