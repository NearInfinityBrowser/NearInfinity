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
import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;

/**
 * Creature animation decoder for processing type E000 (monster_icewind) animations.
 * Available ranges: [e000,efff]
 */
public class MonsterIcewindDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_ICEWIND;

  public static final DecoderAttribute KEY_WEAPON_LEFT_HAND = DecoderAttribute.with("weapon_left_hand", DecoderAttribute.DataType.BOOLEAN);

  private static final HashMap<Sequence, String> seqMap = new HashMap<Sequence, String>() {{
    put(Sequence.ATTACK, "A1");
    put(Sequence.ATTACK_2, "A2");
    put(Sequence.ATTACK_3, "A3");
    put(Sequence.ATTACK_4, "A4");
    put(Sequence.CAST, "CA");
    put(Sequence.DIE, "DE");
    put(Sequence.GET_HIT, "GH");
    put(Sequence.GET_UP, "GU");
    put(Sequence.STANCE, "SC");
    put(Sequence.STAND, "SD");
    put(Sequence.SLEEP, "SL");
    put(Sequence.SPELL, "SP");
    put(Sequence.TWITCH, "TW");
    put(Sequence.WALK, "WK");
  }};

  private static final HashMap<Sequence, String> replacementMap = new HashMap<Sequence, String>() {{
    put(Sequence.DIE, seqMap.get(Sequence.SLEEP));
    put(Sequence.SLEEP, seqMap.get(Sequence.DIE));
    put(Sequence.GET_UP, "!" + seqMap.get(Sequence.DIE));
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
    int translucent = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TRANSLUCENT, 0);
    int leftHanded = SpriteTables.valueToInt(data, SpriteTables.COLUMN_WEAPON, 0);

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster_icewind]");
    lines.add("weapon_left_hand=" + leftHanded);
    lines.add("translucent=" + translucent);
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterIcewindDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterIcewindDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** ??? */
  public boolean isWeaponInLeftHand() { return getAttribute(KEY_WEAPON_LEFT_HAND); }
  protected void setWeaponInLeftHand(boolean b) { setAttribute(KEY_WEAPON_LEFT_HAND, b); }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = new ArrayList<>();
    String resref = getAnimationResref();

    final String[] defOvls = essential ? new String[] { "" }
                                       : new String[] { "", "A", "B", "C", "D", "F", "H", "M", "Q", "S", "W" };
    final String[] defSeqs = essential ? new String[] { "DE", "GH", "SD", "WK" }
                                       : new String[] { "A1", "A2", "A3", "A4", "CA", "DE", "GH", "GU", "SC", "SD", "SL", "SP", "TW", "WK" };
    for (final String ovl : defOvls) {
      for (final String seq : defSeqs) {
        String bamFile = resref + ovl + seq + ".BAM";
        if (ResourceFactory.resourceExists(bamFile)) {
          retVal.add(bamFile);
        }
        bamFile = resref + ovl + seq + "E.BAM";
        if (ResourceFactory.resourceExists(bamFile)) {
          retVal.add(bamFile);
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
    initDefaults(getAnimationInfo());
    IniMapSection section = getSpecificIniSection();
    setWeaponInLeftHand(section.getAsInteger(KEY_WEAPON_LEFT_HAND.getName(), 0) != 0);
    setTranslucent(section.getAsInteger(KEY_TRANSLUCENT.getName(), 0) != 0);
    setDetectedByInfravision(section.getAsInteger(KEY_DETECTED_BY_INFRAVISION.getName(), 0) != 0);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    if (!seqMap.containsKey(seq)) {
      return retVal;
    }

    String resref = getAnimationResref();

    // getting weapon code from CRE resource
    String weapon = "";
    ItemInfo itmWeapon = getCreatureInfo().getEquippedWeapon();
    if (itmWeapon != null) {
      weapon = itmWeapon.getAppearance();
      if (!weapon.isEmpty()) {
        weapon = weapon.substring(0, 1).trim();
      }
      weapon = weapon.trim();
    }

    // checking availability of sequence
    String suffix = seqMap.get(seq);
    if (!ResourceFactory.resourceExists(resref + SegmentDef.fixBehaviorSuffix(suffix) + ".BAM")) {
      suffix = replacementMap.get(seq);
      if (!ResourceFactory.resourceExists(resref + SegmentDef.fixBehaviorSuffix(suffix) + ".BAM")) {
        return retVal;
      }
    }

    SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(suffix);
    suffix = SegmentDef.fixBehaviorSuffix(suffix);

    retVal = new SeqDef(seq);
    String[] ovls = weapon.isEmpty() ? new String[] {""} : new String[] {"", weapon};
    for (final String ovl : ovls) {
      SegmentDef.SpriteType spriteType = (!weapon.isEmpty() && ovl.equals(weapon)) ? SegmentDef.SpriteType.WEAPON : SegmentDef.SpriteType.AVATAR;
      ResourceEntry entry = ResourceFactory.getResourceEntry(resref + ovl + suffix + ".BAM");
      if (entry != null) {
        ResourceEntry entryE = ResourceFactory.getResourceEntry(resref + ovl + suffix + "E.BAM");
        if (SpriteUtils.bamCyclesExist(entry, 0, SeqDef.DIR_REDUCED_W.length)) {
          SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_W, false, entry, 0, spriteType, behavior);
          retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
          if (SpriteUtils.bamCyclesExist(entryE, 0, SeqDef.DIR_REDUCED_E.length)) {
            tmp = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_E, false, entryE, SeqDef.DIR_REDUCED_W.length, spriteType, behavior);
          } else {
            // fallback: mirror eastern directions
            tmp = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_E, true, entry, 1, spriteType, behavior);
          }
          retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
        }
      }
    }

    if (retVal.isEmpty()) {
      retVal = null;
    }

    return retVal;
  }
}
