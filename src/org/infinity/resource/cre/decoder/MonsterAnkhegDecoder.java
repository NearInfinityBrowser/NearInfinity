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
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.tuples.Couple;

/**
 * Creature animation decoder for processing type 3000 (monster_ankheg) animations.
 * Available ranges: [3000,3fff]
 */
public class MonsterAnkhegDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_ANKHEG;

  public static final DecoderAttribute KEY_MIRROR             = DecoderAttribute.with("mirror", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_EXTEND_DIRECTION   = DecoderAttribute.with("extend_direction", DecoderAttribute.DataType.BOOLEAN);

  private static final HashMap<Sequence, Couple<String, Integer>> suffixMap = new HashMap<Sequence, Couple<String, Integer>>() {{
    // Note: int value indicates direction segment multiplier
    put(Sequence.DIE, Couple.with("G1", 1));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!G1", 1));
    put(Sequence.TWITCH, Couple.with("G1", 2));
    put(Sequence.STAND_EMERGED, Couple.with("G1", 3));
    put(Sequence.STAND_HIDDEN, Couple.with("G2", 0));
    put(Sequence.EMERGE, Couple.with("G2", 1));
    put(Sequence.HIDE, Couple.with("G2", 2));
    put(Sequence.ATTACK, Couple.with("G3", 0));
    put(Sequence.SPELL, Couple.with("G3", 1));
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

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster_ankheg]");
    lines.add("false_color=" + falseColor);
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterAnkhegDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterAnkhegDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns whether eastern directions are calculated. */
  public boolean hasMirroredDirections() { return getAttribute(KEY_MIRROR); }
  protected void setMirroredDirections(boolean b) { setAttribute(KEY_MIRROR, b); }

  /** Returns whether the animation provides the full set of directions. */
  public boolean hasExtendedDirections() { return getAttribute(KEY_EXTEND_DIRECTION); }
  protected void setExtendedDirections(boolean b) { setAttribute(KEY_EXTEND_DIRECTION, b); }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    String resref = getAnimationResref();
    ArrayList<String> retVal = new ArrayList<String>() {{
      add(resref + "DG1.BAM");
      if (!hasMirroredDirections()) { add(resref + "DG1E.BAM"); }
      add(resref + "DG2.BAM");
      if (!hasMirroredDirections()) { add(resref + "DG2E.BAM"); }
      add(resref + "DG3.BAM");
      if (!hasMirroredDirections()) { add(resref + "DG3E.BAM"); }
      add(resref + "G1.BAM");
      if (!hasMirroredDirections()) { add(resref + "G1E.BAM"); }
      add(resref + "G2.BAM");
      if (!hasMirroredDirections()) { add(resref + "G2E.BAM"); }
      add(resref + "G3.BAM");
      if (!hasMirroredDirections()) { add(resref + "G3E.BAM"); }
    }};
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
    setMirroredDirections(section.getAsInteger(KEY_MIRROR.getName(), 0) != 0);
    setExtendedDirections(section.getAsInteger(KEY_EXTEND_DIRECTION.getName(), 0) != 0);
    setDetectedByInfravision(section.getAsInteger(KEY_DETECTED_BY_INFRAVISION.getName(), 0) != 0);
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    String resref = getAnimationResref();

    Direction[] dirWest = hasExtendedDirections() ? SeqDef.DIR_FULL_W : SeqDef.DIR_REDUCED_W;
    Direction[] dirEast = hasExtendedDirections() ? SeqDef.DIR_FULL_E : SeqDef.DIR_REDUCED_E;
    int seg = dirWest.length;
    if (!hasMirroredDirections()) {
      seg += dirEast.length;
    }

    String suffixE = hasMirroredDirections() ? "" : "E";
    int eastOfs = hasMirroredDirections() ? 1 : dirWest.length;
    Couple<String, Integer> data = suffixMap.get(seq);
    if (data == null) {
      return retVal;
    }

    retVal = new SeqDef(seq);
    SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(data.getValue0());
    String suffix = SegmentDef.fixBehaviorSuffix(data.getValue0());
    for (final String type : new String[] {"D", ""}) {
      ResourceEntry entry = ResourceFactory.getResourceEntry(resref + type + suffix + ".BAM");
      int cycle = data.getValue1().intValue() * seg;
      ResourceEntry entryE = ResourceFactory.getResourceEntry(resref + type + suffix + suffixE + ".BAM");
      int cycleE = cycle + eastOfs;

      if (SpriteUtils.bamCyclesExist(entry, cycle, dirWest.length) &&
          SpriteUtils.bamCyclesExist(entryE, cycleE, dirEast.length)) {
        SeqDef tmp = SeqDef.createSequence(seq, dirWest, false, entry, cycle, null, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
        tmp = SeqDef.createSequence(seq, dirEast, hasMirroredDirections(), entryE, cycleE, null, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
      }
    }

    if (retVal.isEmpty()) {
      retVal = null;
    }
    return retVal;
  }
}
