// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.DirDef;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.Misc;
import org.infinity.util.tuples.Couple;

/**
 * Creature animation decoder for processing type 1000 (multi_new) animations consisting of 4 segments.
 * Available ranges: [1300,13ff]
 */
public class MonsterMultiNewDecoder extends QuadrantsBaseDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_MULTI_NEW;

  public static final DecoderAttribute KEY_CAN_LIE_DOWN   = DecoderAttribute.with("can_lie_down", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_PATH_SMOOTH    = DecoderAttribute.with("path_smooth", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_SPLIT_BAMS     = DecoderAttribute.with("split_bams", DecoderAttribute.DataType.BOOLEAN);

  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapSplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    // Note: Replace underscore in suffix by one-based quadrant index
    put(Sequence.WALK, Couple.with("G1_1", 0));
    put(Sequence.STANCE, Couple.with("G1_", 9));
    put(Sequence.STAND, Couple.with("G1_2", 18));
    put(Sequence.GET_HIT, Couple.with("G1_3", 27));
    put(Sequence.DIE, Couple.with("G1_4", 36));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!G1_4", 36));
    put(Sequence.TWITCH, Couple.with("G1_5", 45));
    put(Sequence.ATTACK, Couple.with("G2_", 0));
    put(Sequence.ATTACK_2, Couple.with("G2_1", 9));
    put(Sequence.ATTACK_3, Couple.with("G2_2", 18));
    put(Sequence.ATTACK_4, Couple.with("G2_3", 27));
    put(Sequence.ATTACK_5, Couple.with("G2_4", 36));
    put(Sequence.SPELL, Couple.with("G2_5", 45));
    put(Sequence.CAST, Couple.with("G2_6", 54));
  }};
  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapUnsplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    // Note: Replace underscore in suffix by one-based quadrant index
    put(Sequence.WALK, Couple.with("G1_", 0));
    put(Sequence.STANCE, Couple.with("G1_", 9));
    put(Sequence.STAND, Couple.with("G1_", 18));
    put(Sequence.GET_HIT, Couple.with("G1_", 27));
    put(Sequence.DIE, Couple.with("G1_", 36));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!G1_", 36));
    put(Sequence.TWITCH, Couple.with("G1_", 45));
    put(Sequence.ATTACK, Couple.with("G2_", 0));
    put(Sequence.ATTACK_2, Couple.with("G2_", 9));
    put(Sequence.ATTACK_3, Couple.with("G2_", 18));
    put(Sequence.ATTACK_4, Couple.with("G2_", 27));
    put(Sequence.ATTACK_5, Couple.with("G2_", 36));
    put(Sequence.SPELL, Couple.with("G2_", 45));
    put(Sequence.CAST, Couple.with("G2_", 54));
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

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[multi_new]");
    lines.add("false_color=" + falseColor);
    lines.add("quadrants=4");
    lines.add("split_bams=" + splitBams);
    lines.add("translucent=" + translucent);
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterMultiNewDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterMultiNewDecoder(CreResource cre) throws Exception
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

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    String resref = getAnimationResref();
    ArrayList<String> retVal = new ArrayList<String>() {{
    for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : getSuffixMap().entrySet()) {
      String suffixBase = SegmentDef.fixBehaviorSuffix(entry.getValue().getValue0());
      for (int i = 0; i < getQuadrants(); i++) {
        String suffix = suffixBase.replace("_", Integer.toString(i+1));
        add(resref + suffix + ".BAM");
      }
    }
    }};
    return retVal;
  }

  @Override
  protected void init() throws Exception
  {
    super.init();
    IniMapSection section = getSpecificIniSection();
    setCanLieDown(section.getAsInteger(KEY_CAN_LIE_DOWN.getName(), 0) != 0);
    setDetectedByInfravision(section.getAsInteger(KEY_DETECTED_BY_INFRAVISION.getName(), 0) != 0);
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setSmoothPath(section.getAsInteger(KEY_PATH_SMOOTH.getName(), 0) != 0);
    setSplittedBams(section.getAsInteger(KEY_SPLIT_BAMS.getName(), 0) != 0);
    setTranslucent(section.getAsInteger(KEY_TRANSLUCENT.getName(), 0) != 0);
    setQuadrants(section.getAsInteger(KEY_QUADRANTS.getName(), 4));
    Misc.requireCondition(getQuadrants() > 0 && getQuadrants() < 10,
                          "Invalid number of quadrants: " + getQuadrants(), IllegalArgumentException.class);
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    String resref = getAnimationResref();

    List<SegmentDef> cycleList = new ArrayList<>();
    List<SegmentDef> cycleListE = new ArrayList<>();
    boolean valid = true;
    if (getSuffixMap().containsKey(seq)) {
      String suffixBase = getSuffixMap().get(seq).getValue0();
      SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(suffixBase);
      suffixBase = SegmentDef.fixBehaviorSuffix(suffixBase);
      int cycleOfs = getSuffixMap().get(seq).getValue1().intValue();
      for (int i = 0; valid && i < getQuadrants(); i++) {
        String suffix = suffixBase.replace("_", Integer.toString(i+1));
        ResourceEntry entry = ResourceFactory.getResourceEntry(resref + suffix + ".BAM");
        cycleList.add(new SegmentDef(entry, cycleOfs, null, behavior));
        cycleListE.add(new SegmentDef(entry, cycleOfs + 1, null, behavior));
        valid &= SpriteUtils.bamCyclesExist(entry, cycleOfs, SeqDef.DIR_FULL_W.length);
      }
    }

    if (!cycleList.isEmpty() && valid) {
      retVal = SeqDef.createSequence(seq, SeqDef.DIR_FULL_W, false, cycleList);
      SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_FULL_E, true, cycleListE);
      retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
    }

    return retVal;
  }
}
