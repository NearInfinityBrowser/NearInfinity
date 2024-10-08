// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 62.
 */
public class Opcode062 extends BaseOpcode {
  private static final String EFFECT_NUM_SPELLS_TO_ADD  = "# spells to add";
  private static final String EFFECT_SPELL_LEVELS       = "Spell levels";
  private static final String EFFECT_BYPASS_SLOT_REQ    = "EEex: Bypass slot requirement?";

  private static final String[] SPELL_LEVELS_IWD2  = { "Double spells", "Level 1", "Level 2", "Level 3", "Level 4",
      "Level 5", "Level 6", "Level 7", "Level 8", "Level 9" };
  private static final String[] SPELL_LEVELS_BG2 = Arrays.copyOf(SPELL_LEVELS_IWD2, SPELL_LEVELS_IWD2.length + 1);
  private static final String[] SPELL_LEVELS = Arrays.copyOf(SPELL_LEVELS_IWD2, SPELL_LEVELS_IWD2.length - 2);

  static {
    SPELL_LEVELS_BG2[8] = SPELL_LEVELS_BG2[9] = null;
    SPELL_LEVELS_BG2[10] = "Ex: Double spells";
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Bonus priest spells";
  }

  public Opcode062() {
    super(62, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SPELLS_TO_ADD));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_SPELL_LEVELS, SPELL_LEVELS));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SPELLS_TO_ADD));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_SPELL_LEVELS, SPELL_LEVELS_BG2));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SPELLS_TO_ADD));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_SPELL_LEVELS, SPELL_LEVELS_IWD2));
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition() && isEEEx()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_BYPASS_SLOT_REQ, AbstractStruct.OPTION_NOYES));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
