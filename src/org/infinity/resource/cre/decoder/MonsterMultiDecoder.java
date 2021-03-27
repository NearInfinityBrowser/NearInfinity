// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
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
 * Creature animation decoder for processing type 1000 (multi_monster) animations consisting of 9 quadrants.
 * Note: This type can be incorrectly labeled as "multi_new" in INI files.
 * Available ranges: [1200,12ff], [1400,1fff]
 */
public class MonsterMultiDecoder extends QuadrantsBaseDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_MULTI;

  public static final DecoderAttribute KEY_SPLIT_BAMS     = DecoderAttribute.with("split_bams", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_DOUBLE_BLIT    = DecoderAttribute.with("double_blit", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_PALETTE_1      = DecoderAttribute.with("palette1", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALETTE_2      = DecoderAttribute.with("palette2", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALETTE_3      = DecoderAttribute.with("palette3", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALETTE_4      = DecoderAttribute.with("palette4", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALETTE_5      = DecoderAttribute.with("palette5", DecoderAttribute.DataType.STRING);

  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapUnsplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    // Note: Replace 'underscore' in suffix by one-based quadrant index
    // Note 2: 'exclamation mark' in suffix indicates reversed playback of frames (e.g. get up = reversed sleep); remove
    put(Sequence.WALK, Couple.with("G1_", 0));
    put(Sequence.STANCE, Couple.with("G2_", 0));
    put(Sequence.SPELL, get(Sequence.STANCE));
    put(Sequence.STAND, Couple.with("G2_", 9));   // engine appears to use STANCE sequence instead
//    put(Sequence.STAND, get(Sequence.STANCE));
    put(Sequence.ATTACK, Couple.with("G3_", 0));
//    put(Sequence.ATTACK_2, get(Sequence.ATTACK)); // apparently unused by the engine
//    put(Sequence.ATTACK_3, get(Sequence.ATTACK)); // apparently unused by the engine
    put(Sequence.GET_HIT, Couple.with("G4_", 0));
    put(Sequence.DIE, Couple.with("G4_", 9));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!G4_", 9));
    put(Sequence.TWITCH, Couple.with("G4_", 18));
    put(Sequence.CAST, Couple.with("G5_", 9));
    put(Sequence.SHOOT, get(Sequence.CAST));
  }};

  private static final HashMap<Sequence, Couple<String, Integer>> suffixMapSplit = new HashMap<Sequence, Couple<String, Integer>>() {{
    // Note: Replace 'underscore' in suffix by one-based quadrant index
    // Note 2: Replace 'plus' in suffix by zero-based direction index (0=south, 4=west, ...)
    // Note 3: 'exclamation mark' in suffix indicates reversed playback of frames (e.g. get up = reversed sleep); remove
    put(Sequence.WALK, Couple.with("1_0+", 0));
    put(Sequence.STANCE, Couple.with("2_0+", 0));
    put(Sequence.SPELL, get(Sequence.STANCE));
    put(Sequence.STAND, Couple.with("2_0+", 9));  // engine appears to use STANCE sequence instead
//    put(Sequence.STAND, get(Sequence.STANCE));
    put(Sequence.ATTACK, Couple.with("3_0+", 0));
//    put(Sequence.ATTACK_2, get(Sequence.ATTACK)); // apparently unused by the engine
//    put(Sequence.ATTACK_3, get(Sequence.ATTACK)); // apparently unused by the engine
    put(Sequence.GET_HIT, Couple.with("4_0+", 0));
    put(Sequence.DIE, Couple.with("4_1+", 9));
    put(Sequence.SLEEP, get(Sequence.DIE));
    put(Sequence.GET_UP, Couple.with("!4_1+", 9));
    put(Sequence.TWITCH, Couple.with("4_2+", 18));
    put(Sequence.CAST, Couple.with("5_1+", 9));
    put(Sequence.SHOOT, get(Sequence.CAST));
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
    int splitBams = SpriteTables.valueToInt(data, SpriteTables.COLUMN_SPLIT, 0);
    String paletteBase = SpriteTables.valueToString(data, SpriteTables.COLUMN_PALETTE, "");

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster_multi]");
    lines.add("quadrants=9");
    lines.add("split_bams=" + splitBams);
    lines.add("resref=" + resref);
    if (!paletteBase.isEmpty()) {
      lines.add("palette1=" + paletteBase + "1");
      lines.add("palette2=" + paletteBase + "2");
      lines.add("palette3=" + paletteBase + "3");
      lines.add("palette4=" + paletteBase + "4");
      lines.add("palette5=" + paletteBase + "5");
    }

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterMultiDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterMultiDecoder(CreResource cre) throws Exception
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

  /** unused */
  public boolean isDoubleBlit() { return getAttribute(KEY_DOUBLE_BLIT); }
  protected void setDoubleBlit(boolean b) { setAttribute(KEY_DOUBLE_BLIT, b); }

  /** Returns the palette resref for the specified BAM prefix. Falls back to "new_palette" if needed. */
  public String getPalette(int idx)
  {
    String retVal = null;
    switch (idx) {
      case 1:
        retVal = getAttribute(KEY_PALETTE_1);
        break;
      case 2:
        retVal = getAttribute(KEY_PALETTE_2);
        break;
      case 3:
        retVal = getAttribute(KEY_PALETTE_3);
        break;
      case 4:
        retVal = getAttribute(KEY_PALETTE_4);
        break;
      case 5:
        retVal = getAttribute(KEY_PALETTE_5);
        break;
      default:
        return getNewPalette();
    }

    if (retVal == null || retVal.isEmpty()) {
      String s = getNewPalette();
      if (!s.isEmpty()) {
        retVal = s + idx;
      }
    }

    return retVal;
  }

  protected void setPalette(int idx, String s)
  {
    switch (idx) {
      case 1:
        setAttribute(KEY_PALETTE_1, s);
        break;
      case 2:
        setAttribute(KEY_PALETTE_2, s);
        break;
      case 3:
        setAttribute(KEY_PALETTE_3, s);
        break;
      case 4:
        setAttribute(KEY_PALETTE_4, s);
        break;
      case 5:
        setAttribute(KEY_PALETTE_5, s);
        break;
    }
  }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    String resref = getAnimationResref();
    final HashSet<String> fileSet = new HashSet<>();
    for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : getSuffixMap().entrySet()) {
      for (int i = 0; i < getQuadrants(); i++) {
        for (int j = 0; j < SeqDef.DIR_FULL_W.length; j++) {
          String suffix = entry.getValue().getValue0()
              .replace("_", Integer.toString(i+1))
              .replace("+", Integer.toString(j));
          suffix = SegmentDef.fixBehaviorSuffix(suffix);
          fileSet.add(resref + suffix + ".BAM");
        }
      }
    }
    ArrayList<String> retVal = new ArrayList<String>() {{
      for (final String s : fileSet) {
        add(s);
      }
    }};
    return retVal;
  }

  @Override
  protected void init() throws Exception
  {
    super.init();
    IniMapSection section = getSpecificIniSection();
    if (section.getEntryCount() == 0) {
      // EE: defined as "multi_new" type
      section = getAnimationInfo().getSection(AnimationInfo.Type.MONSTER_MULTI_NEW.getSectionName());
    }
    setSplittedBams(section.getAsInteger(KEY_SPLIT_BAMS.getName(), 0) != 0);
    setQuadrants(section.getAsInteger(KEY_QUADRANTS.getName(), 9));
    setPalette(1, section.getAsString(KEY_PALETTE_1.getName()));
    setPalette(2, section.getAsString(KEY_PALETTE_2.getName()));
    setPalette(3, section.getAsString(KEY_PALETTE_3.getName()));
    setPalette(4, section.getAsString(KEY_PALETTE_4.getName()));
    setPalette(5, section.getAsString(KEY_PALETTE_5.getName()));
    Misc.requireCondition(getQuadrants() < 10, "Too many quadrants defined: " + getQuadrants(), IllegalArgumentException.class);
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected int[] getNewPaletteData(ResourceEntry bamRes)
  {
    int[] retVal = null;

    String bamResref = bamRes.getResourceRef();
    String resref = getAnimationResref();

    int idx = Misc.toNumber(Character.toString(bamResref.charAt(resref.length())), -1);
    retVal = SpriteUtils.loadReplacementPalette(getPalette(idx));

    return retVal;
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    String resref = getAnimationResref();

    List<SegmentDef> cycleList = new ArrayList<>();
    if (getSuffixMap().containsKey(seq)) {
      String suffixBase = getSuffixMap().get(seq).getValue0();
      SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(suffixBase);
      suffixBase = SegmentDef.fixBehaviorSuffix(suffixBase);
      int cycleBase = getSuffixMap().get(seq).getValue1().intValue();
      retVal = new SeqDef(seq);

      // defining western directions
      for (final Direction dir : SeqDef.DIR_FULL_W) {
        cycleList.clear();
        for (int seg = 0; seg < getQuadrants(); seg++) {
          String suffix = suffixBase.replace("_", Integer.toString(seg+1)).replace("+", Integer.toString(dir.getValue()));
          ResourceEntry entry = ResourceFactory.getResourceEntry(resref + suffix + ".BAM");
          int cycleIdx = cycleBase + dir.getValue();
          if (!SpriteUtils.bamCyclesExist(entry, cycleIdx, 1)) {
            return null;
          }
          cycleList.add(new SegmentDef(entry, cycleIdx, SegmentDef.SpriteType.AVATAR, behavior));
        }
        SeqDef tmp = SeqDef.createSequence(seq, new Direction[] {dir}, false, cycleList);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
      }

      // calculating eastern directions
      for (final Direction dir : SeqDef.DIR_FULL_E) {
        cycleList.clear();
        int dir2 = SeqDef.DIR_FULL_W.length - (dir.getValue() - Direction.N.getValue() + 1);  // direction to mirror
        for (int seg = 0; seg < getQuadrants(); seg++) {
          String suffix = suffixBase.replace("_", Integer.toString(seg+1)).replace("+", Integer.toString(dir2));
          ResourceEntry entry = ResourceFactory.getResourceEntry(resref + suffix + ".BAM");
          int cycleIdx = cycleBase + dir2;
          cycleList.add(new SegmentDef(entry, cycleIdx, SegmentDef.SpriteType.AVATAR, behavior));
        }
        SeqDef tmp = SeqDef.createSequence(seq, new Direction[] {dir}, true, cycleList);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
      }

// Structure:
// xxxx1[1-9]0[0-8]: [0-8]
// xxxx1100: 0, xxxx1200: 0, xxxx1300: 0, xxxx1400: 0, xxxx1500: 0, xxxx1600: 0, xxxx1700: 0, xxxx1800: 0, xxxx1900: 0
// xxxx1101: 1, xxxx1201: 1, xxxx1301: 1, xxxx1401: 1, xxxx1501: 1, xxxx1601: 1, xxxx1701: 1, xxxx1801: 1, xxxx1901: 1
// xxxx1102: 2, xxxx1202: 2, xxxx1302: 2, xxxx1402: 2, xxxx1502: 2, xxxx1602: 2, xxxx1702: 2, xxxx1802: 2, xxxx1902: 2
// xxxx1103: 3, xxxx1203: 3, xxxx1303: 3, xxxx1403: 3, xxxx1503: 3, xxxx1603: 3, xxxx1703: 3, xxxx1803: 3, xxxx1903: 3
// xxxx1104: 4, xxxx1204: 4, xxxx1304: 4, xxxx1404: 4, xxxx1504: 4, xxxx1604: 4, xxxx1704: 4, xxxx1804: 4, xxxx1904: 4
// xxxx1105: 5, xxxx1205: 5, xxxx1305: 5, xxxx1405: 5, xxxx1505: 5, xxxx1605: 5, xxxx1705: 5, xxxx1805: 5, xxxx1905: 5
// xxxx1106: 6, xxxx1206: 6, xxxx1306: 6, xxxx1406: 6, xxxx1506: 6, xxxx1606: 6, xxxx1706: 6, xxxx1806: 6, xxxx1906: 6
// xxxx1107: 7, xxxx1207: 7, xxxx1307: 7, xxxx1407: 7, xxxx1507: 7, xxxx1607: 7, xxxx1707: 7, xxxx1807: 7, xxxx1907: 7
// xxxx1108: 8, xxxx1208: 8, xxxx1308: 8, xxxx1408: 8, xxxx1508: 8, xxxx1608: 8, xxxx1708: 8, xxxx1808: 8, xxxx1908: 8
    }

    return retVal;
  }
}
