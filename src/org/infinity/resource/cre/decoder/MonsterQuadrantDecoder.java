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
 * Creature animation decoder for processing type 1000 (monster_quadrant) animations.
 * Available ranges: [1000,1fff]
 */
public class MonsterQuadrantDecoder extends QuadrantsBaseDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_QUADRANT;

  public static final DecoderAttribute KEY_CASTER                 = DecoderAttribute.with("caster", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_PATH_SMOOTH            = DecoderAttribute.with("path_smooth", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_EXTEND_DIRECTION       = DecoderAttribute.with("extend_direction", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_EXTEND_DIRECTION_TEST  = DecoderAttribute.with("extend_direction_test", DecoderAttribute.DataType.INT);

  private static final HashMap<Sequence, Couple<String, Integer>> suffixMap = new HashMap<Sequence, Couple<String, Integer>>() {{
    put(Sequence.WALK, Couple.with("G1", 0));
    put(Sequence.STAND, Couple.with("G2", 0));
    put(Sequence.STANCE, Couple.with("G2", 16));
    put(Sequence.GET_HIT, Couple.with("G2", 32));
    put(Sequence.DIE, Couple.with("G2", 48));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!G2", 48));
    put(Sequence.TWITCH, Couple.with("G2", 64));
    put(Sequence.ATTACK, Couple.with("G3", 0));
    put(Sequence.ATTACK_2, Couple.with("G3", 16));
    put(Sequence.ATTACK_3, Couple.with("G3", 32));
    put(Sequence.CAST, get(Sequence.ATTACK_3));
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
    int caster = SpriteTables.valueToInt(data, SpriteTables.COLUMN_HELMET, 0);
    int extendDirection = ResourceFactory.resourceExists(resref + "G11E.BAM") ? 1 : 0;
    int extendDirectionTest = 9;
    ResourceEntry bamEntry = ResourceFactory.getResourceEntry(resref + "G11.BAM");
    int numCycles = SpriteUtils.getBamCycles(bamEntry);
    if (numCycles == 8) {
      extendDirectionTest = 5;  // TBC
    }

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster_quadrant]");
    lines.add("false_color=" + falseColor);
    lines.add("caster=" + caster);
    lines.add("extend_direction=" + extendDirection);
    lines.add("extend_direction_test=" + extendDirectionTest);
    lines.add("quadrants=4");
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterQuadrantDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterQuadrantDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns whether attack animations {@code Attack2} and {@code Attack3} are used as casting animations. */
  public boolean isCaster() { return getAttribute(KEY_CASTER); }
  protected void setCaster(boolean b) { setAttribute(KEY_CASTER, b); }

  /** ??? */
  public boolean isSmoothPath() { return getAttribute(KEY_PATH_SMOOTH); }
  protected void setSmoothPath(boolean b) { setAttribute(KEY_PATH_SMOOTH, b); }

  /** Returns whether eastern directions are available. */
  public boolean isExtendedDirection() { return getAttribute(KEY_EXTEND_DIRECTION); }
  protected void setExtendedDirection(boolean b) { setAttribute(KEY_EXTEND_DIRECTION, b); }

  /** ??? */
  public int getExtendedDirectionSize() { return getAttribute(KEY_EXTEND_DIRECTION_TEST); }
  protected void setExtendedDirectionSize(int v) { setAttribute(KEY_EXTEND_DIRECTION_TEST, v); }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = new ArrayList<>();
    String resref = getAnimationResref();
    for (final String suffix : new String[] {"G1", "G2", "G3"}) {
      for (int i = 0; i < getQuadrants(); i++) {
        retVal.add(resref + suffix + (i+1) + ".BAM");
        if (isExtendedDirection()) {
          retVal.add(resref + suffix + (i+1) + "E.BAM");
        }
      }
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
    // setting properties
    super.init();
    IniMapSection section = getSpecificIniSection();
    setCaster(section.getAsInteger(KEY_CASTER.getName(), 0) != 0);
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setSmoothPath(section.getAsInteger(KEY_PATH_SMOOTH.getName(), 0) != 0);
    setExtendedDirection(section.getAsInteger(KEY_EXTEND_DIRECTION.getName(), 0) != 0);
    setExtendedDirectionSize(section.getAsInteger(KEY_EXTEND_DIRECTION_TEST.getName(), 9));
    setQuadrants(section.getAsInteger(KEY_QUADRANTS.getName(), 4));
    Misc.requireCondition(getQuadrants() < 10, "Too many quadrants defined: " + getQuadrants(), IllegalArgumentException.class);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    String resref = getAnimationResref();

    if (isCaster() && (seq == Sequence.ATTACK_3)) {
      return retVal;
    }

    if (!isCaster() && (seq == Sequence.CAST)) {
      return retVal;
    }

    List<SegmentDef> cycleList = new ArrayList<>();
    List<SegmentDef> cycleListE = new ArrayList<>();
    boolean valid = true;
    if (suffixMap.containsKey(seq) && (isCaster() || seq != Sequence.SPELL && seq != Sequence.CAST)) {
      String suffix = suffixMap.get(seq).getValue0();
      SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(suffix);
      suffix = SegmentDef.fixBehaviorSuffix(suffix);
      int cycleOfs = suffixMap.get(seq).getValue1().intValue();
      for (int i = 1; valid && i <= getQuadrants(); i++) {
        ResourceEntry entry = ResourceFactory.getResourceEntry(resref + suffix + i + ".BAM");
        ResourceEntry entryE = entry;
        int cycle = cycleOfs;
        int cycleE = cycleOfs + 1;
        valid &= SpriteUtils.bamCyclesExist(entry, cycle, SeqDef.DIR_FULL_W.length);
        if (isExtendedDirection()) {
          entryE = ResourceFactory.getResourceEntry(resref + suffix + i + "E.BAM");
          cycleE = cycle + SeqDef.DIR_FULL_W.length;
          valid &= SpriteUtils.bamCyclesExist(entryE, cycleE, SeqDef.DIR_FULL_E.length);
        }
        cycleList.add(new SegmentDef(entry, cycle, null, behavior));
        cycleListE.add(new SegmentDef(entryE, cycleE, null, behavior));
      }
    }

    if (!cycleList.isEmpty() && valid) {
      retVal = SeqDef.createSequence(seq, SeqDef.DIR_FULL_W, false, cycleList);
      SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_FULL_E, !isExtendedDirection(), cycleListE);
      retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
    }

    return retVal;
  }
}
