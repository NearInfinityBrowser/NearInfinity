// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.DirDef;
import org.infinity.resource.cre.decoder.util.Direction;
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
 * Creature animation decoder for processing type 7000 (monster) animations.
 * Available ranges: (using notation slot/range where slot can be a formula with range definitions as [x,y])
 * (0x7002 | ([0x00,0x1f] << 4))/0xd
 * (0x7004 | ([0x20,0x2f] << 4))/0xb
 * (0x7000 | ([0x30,0x3f] << 4))/0xf
 * (0x7003 | ([0x40,0x4f] << 4))/0xc
 * (0x7002 | ([0x50,0x5f] << 4))/0xd
 * (0x7003 | ([0x70,0x7f] << 4))/0xc
 * (0x7005 | ([0x90,0xaf] << 4))/0xa
 * (0x7007 | ([0xb0,0xbf] << 4))/0x8
 * (0x7002 | ([0xc0,0xcf] << 4))/0xd
 * (0x7002 | ([0xe0,0xef] << 4))/0xd
 * (0x7000 | ([0xf0,0xff] << 4))/0xf
 */
public class MonsterDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER;

  public static final DecoderAttribute KEY_CAN_LIE_DOWN   = DecoderAttribute.with("can_lie_down", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_PATH_SMOOTH    = DecoderAttribute.with("path_smooth", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_SPLIT_BAMS     = DecoderAttribute.with("split_bams", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_GLOW_LAYER     = DecoderAttribute.with("glow_layer", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALETTE1       = DecoderAttribute.with("palette1", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALETTE2       = DecoderAttribute.with("palette2", DecoderAttribute.DataType.STRING);

  /** Assigns BAM suffix and cycle index to a specific animation sequence (unsplit version). */
  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapUnsplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    put(Sequence.WALK, Couple.with("G1", 0));
    put(Sequence.STANCE, Couple.with("G1", 9));
    put(Sequence.STAND, Couple.with("G1", 18));
    put(Sequence.GET_HIT, Couple.with("G1", 27));
    put(Sequence.DIE, Couple.with("G1", 36));
    put(Sequence.TWITCH, Couple.with("G1", 45));
    put(Sequence.SLEEP, Couple.with("G1", 54));
    put(Sequence.GET_UP, Couple.with("G1", 63));
    put(Sequence.ATTACK, Couple.with("G2", 0));
    put(Sequence.ATTACK_2, Couple.with("G2", 9));
    put(Sequence.ATTACK_3, Couple.with("G2", 18));
    put(Sequence.ATTACK_4, Couple.with("G2", 27));
    put(Sequence.ATTACK_5, Couple.with("G2", 36));
    put(Sequence.SPELL, Couple.with("G2", 45));
    put(Sequence.CAST, Couple.with("G2", 54));
  }};

  /** Assigns BAM suffix and cycle index to a specific animation sequence (split version). */
  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapSplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    put(Sequence.WALK, Couple.with("G11", 0));
    put(Sequence.STANCE, Couple.with("G1", 9));
    put(Sequence.STAND, Couple.with("G12", 18));
    put(Sequence.GET_HIT, Couple.with("G13", 27));
    put(Sequence.DIE, Couple.with("G14", 36));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!G14", 36));
    put(Sequence.TWITCH, Couple.with("G15", 45));
    put(Sequence.ATTACK, Couple.with("G2", 0));
    put(Sequence.ATTACK_2, Couple.with("G21", 9));
    put(Sequence.ATTACK_3, Couple.with("G22", 18));
    put(Sequence.ATTACK_4, Couple.with("G23", 27));
    put(Sequence.ATTACK_5, Couple.with("G24", 36));
    put(Sequence.SPELL, Couple.with("G25", 45));
    put(Sequence.CAST, Couple.with("G26", 54));
  }};

  /** Replacement sequences if original sequence definition is missing (unsplit version). */
  private static final HashMap<Sequence, Couple<String, Integer>> replacementMapUnsplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    put(Sequence.DIE, suffixMapUnsplit.get(Sequence.SLEEP));
    put(Sequence.SLEEP, suffixMapUnsplit.get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!" + suffixMapUnsplit.get(Sequence.DIE).getValue0(), suffixMapUnsplit.get(Sequence.DIE).getValue1()));
  }};

  /** Replacement sequences if original sequence definition is missing (split version). */
  private static final HashMap<Sequence, Couple<String, Integer>> replacementMapSplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    // not needed
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
    int falseColor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_CLOWN, 0);
    int splitBams = SpriteTables.valueToInt(data, SpriteTables.COLUMN_SPLIT, 0);
    int translucent = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TRANSLUCENT, 0);
    String palette1 = SpriteTables.valueToString(data, SpriteTables.COLUMN_PALETTE, "");
    String palette2 = SpriteTables.valueToString(data, SpriteTables.COLUMN_PALETTE2, "");

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster]");
    lines.add("false_color=" + falseColor);
    lines.add("split_bams=" + splitBams);
    lines.add("translucent=" + translucent);
    lines.add("resref=" + resref);
    if (!palette1.isEmpty()) {
      lines.add("palette1=" + palette1);
    }
    if (!palette2.isEmpty()) {
      lines.add("palette2=" + palette2);
    }

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns the correct sequence map for the current settings. */
  private HashMap<Sequence, Couple<String, Integer>> getSuffixMap()
  {
    return isSplittedBams() ? suffixMapSplit : suffixMapUnsplit;
  }

  /** Returns whether the creature falls down when dead/unconscious. */
  public boolean canLieDown() { return getAttribute(KEY_CAN_LIE_DOWN); }
  protected void setCanLieDown(boolean b) { setAttribute(KEY_CAN_LIE_DOWN, b); }

  /** ??? */
  public boolean isSmoothPath() { return getAttribute(KEY_PATH_SMOOTH); }
  protected void setSmoothPath(boolean b) { setAttribute(KEY_PATH_SMOOTH, b); }

  /** Returns whether animations are spread over various subfiles. */
  public boolean isSplittedBams() { return getAttribute(KEY_SPLIT_BAMS); }
  protected void setSplittedBams(boolean b) { setAttribute(KEY_SPLIT_BAMS, b); }

  /**
   * Returns the solid background layer of blended/glowing creature animations.
   * (Note: currently not supported by the engine.)
   */
  public String getGlowLayer() { return getAttribute(KEY_GLOW_LAYER); }
  protected void setGlowLayer(String s) { setAttribute(KEY_GLOW_LAYER, s); }

  /**
   * Returns the first replacement palette (BMP) for the creature animation.
   * Falls back to new palette from general attributes.
   */
  public String getPalette1()
  {
    String retVal = getAttribute(KEY_PALETTE1);
    if (retVal.isEmpty()) {
      retVal = getNewPalette();
    }
    return retVal;
  }

  protected void setPalette1(String s) { setAttribute(KEY_PALETTE1, s); }

  /**
   * Returns the second replacement palette (BMP) for the creature animation.
   * Falls back to palette1 or new palette from general attributes.
   */
  public String getPalette2()
  {
    String retVal = getAttribute(KEY_PALETTE2);
    if (retVal.isEmpty()) {
      retVal = getPalette1();
    }
    return retVal;
  }

  protected void setPalette2(String s) { setAttribute(KEY_PALETTE2, s); }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    // collecting suffixes
    String resref = getAnimationResref();
    HashSet<String> files = new HashSet<>();
    for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : getSuffixMap().entrySet()) {
      String suffix = SegmentDef.fixBehaviorSuffix(entry.getValue().getValue0());
      files.add(resref + suffix + ".BAM");
    }

    // generating file list
    ArrayList<String> retVal = new ArrayList<>(Arrays.asList(files.toArray(new String[files.size()])));

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
    // setting properties
    initDefaults(getAnimationInfo());
    IniMapSection section = getSpecificIniSection();
    setCanLieDown(section.getAsInteger(KEY_CAN_LIE_DOWN.getName(), 0) != 0);
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setDetectedByInfravision(section.getAsInteger(KEY_DETECTED_BY_INFRAVISION.getName(), 0) != 0);
    setSmoothPath(section.getAsInteger(KEY_PATH_SMOOTH.getName(), 0) != 0);
    setSplittedBams(section.getAsInteger(KEY_SPLIT_BAMS.getName(), 0) != 0);
    setTranslucent(section.getAsInteger(KEY_TRANSLUCENT.getName(), 0) != 0);
    String s = section.getAsString(KEY_GLOW_LAYER.getName(), "");
    if (s.isEmpty()) {
      // KEY_GLOW_LAYER maybe incorrectly assigned to "general" section
      s = getGeneralIniSection(getAnimationInfo()).getAsString(KEY_GLOW_LAYER.getName(), "");
    }
    setGlowLayer(s);
    setPalette1(section.getAsString(KEY_PALETTE1.getName(), ""));
    setPalette2(section.getAsString(KEY_PALETTE2.getName(), ""));
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;

    Couple<String, Integer> data = getSuffixMap().get(seq);
    if (data == null) {
      return retVal;
    }

    ArrayList<Couple<String, SegmentDef.SpriteType>> creResList = new ArrayList<>();

    // processing creature sprite
    String resref = getAnimationResref();
    String suffix = data.getValue0();
    int ofs = data.getValue1().intValue();
    ResourceEntry bamEntry = ResourceFactory.getResourceEntry(resref + SegmentDef.fixBehaviorSuffix(suffix) + ".BAM");
    if (!SpriteUtils.bamCyclesExist(bamEntry, ofs, 1)) {
      data = (isSplittedBams() ? replacementMapSplit: replacementMapUnsplit).get(seq);
      if (data == null) {
        return retVal;
      }
      suffix = data.getValue0();
      bamEntry = ResourceFactory.getResourceEntry(resref + SegmentDef.fixBehaviorSuffix(suffix) + ".BAM");
      if (!SpriteUtils.bamCyclesExist(bamEntry, ofs, 1)) {
        return retVal;
      }
    }
    SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(suffix);
    suffix = SegmentDef.fixBehaviorSuffix(suffix);
    creResList.add(Couple.with(resref + suffix + ".BAM", SegmentDef.SpriteType.AVATAR));

    // processing weapon overlay
    ItemInfo itmWeapon = getCreatureInfo().getEquippedWeapon();
    if (itmWeapon != null) {
      String weapon = itmWeapon.getAppearance().trim();
      if (!weapon.isEmpty()) {
        Couple<String, Integer> wdata = suffixMapUnsplit.get(seq);
        if (wdata != null) {
          creResList.add(Couple.with(resref + wdata.getValue0() + weapon + ".BAM", SegmentDef.SpriteType.WEAPON));
        }
      }
    }

    retVal = new SeqDef(seq);
    for (final Couple<String, SegmentDef.SpriteType> creInfo : creResList) {
      ResourceEntry entry = ResourceFactory.getResourceEntry(creInfo.getValue0());
      SegmentDef.SpriteType type = creInfo.getValue1();
      if (SpriteUtils.bamCyclesExist(entry, ofs, SeqDef.DIR_FULL_W.length)) {
        SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_FULL_W, false, entry, ofs, type, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
        tmp = SeqDef.createSequence(seq, SeqDef.DIR_FULL_E, true, entry, ofs + 1, type, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
      } else if (entry != null && SpriteUtils.getBamCycles(entry) == 1) {
        // fallback solution: just use first bam cycle (required by a few animations)
        for (final Direction dir : SeqDef.DIR_FULL_W) {
          SeqDef tmp = SeqDef.createSequence(seq, new Direction[] {dir}, false, entry, 0, type, behavior);
          retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
        }
        for (final Direction dir : SeqDef.DIR_FULL_E) {
          SeqDef tmp = SeqDef.createSequence(seq, new Direction[] {dir}, true, entry, 0, type, behavior);
          retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
        }
      }
    }

    if (retVal.isEmpty()) {
      retVal = null;
    }

    return retVal;
  }

  @Override
  protected int[] getNewPaletteData(ResourceEntry bamRes)
  {
    if (bamRes != null) {
      String resref = bamRes.getResourceRef();
      if (resref.length() >= 6) {
        switch (resref.charAt(5)) {
          case '1':
            return SpriteUtils.loadReplacementPalette(getPalette1());
          case '2':
            return SpriteUtils.loadReplacementPalette(getPalette2());
        }
      }
    }
    return super.getNewPaletteData(bamRes);
  }
}
