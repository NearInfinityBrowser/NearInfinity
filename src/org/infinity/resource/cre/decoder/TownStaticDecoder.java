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
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;

/**
 * Creature animation decoder for processing type 4000 (town_static) animations.
 * Available ranges: [4000,4fff]
 */
public class TownStaticDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.TOWN_STATIC;

  public static final DecoderAttribute KEY_CAN_LIE_DOWN   = DecoderAttribute.with("can_lie_down", DecoderAttribute.DataType.BOOLEAN);

  /** Mapping between sequence and cycle index. */
  private static final HashMap<Sequence, Integer> cycleMap = new HashMap<Sequence, Integer>() {{
    put(Sequence.STANCE, 0);
    put(Sequence.STAND, 16);
    put(Sequence.GET_HIT, 32);
    put(Sequence.DIE, 48);
    put(Sequence.SLEEP, 48);
    put(Sequence.GET_UP, 48);
    put(Sequence.TWITCH, 64);
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
    if (data == null) {
      return retVal;
    }

    String resref = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF, "");
    if (resref.isEmpty()) {
      return retVal;
    }
    int falseColor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_CLOWN, 0);

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[town_static]");
    lines.add("false_color=" + falseColor);
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public TownStaticDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public TownStaticDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns whether the creature falls down when dead/unconscious. */
  public boolean canLieDown() { return getAttribute(KEY_CAN_LIE_DOWN); }
  protected void setCanLieDown(boolean b) { setAttribute(KEY_CAN_LIE_DOWN, b); }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = new ArrayList<>();
    retVal.add(getAnimationResref() + ".BAM");
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
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setCanLieDown(section.getAsInteger(KEY_CAN_LIE_DOWN.getName(), 0) != 0);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    if (!cycleMap.containsKey(seq)) {
      return retVal;
    }

    SegmentDef.Behavior behavior = (seq == Sequence.GET_UP) ? SegmentDef.getBehaviorOf("!") : SegmentDef.getBehaviorOf("");
    int cycle = cycleMap.getOrDefault(seq, 0);
    ResourceEntry entry = ResourceFactory.getResourceEntry(getAnimationResref() + ".BAM");
    if (SpriteUtils.bamCyclesExist(entry, cycle, SeqDef.DIR_FULL.length)) {
      retVal = SeqDef.createSequence(seq, SeqDef.DIR_FULL, false, entry, cycle, null, behavior);
    }
    return retVal;
  }
}
