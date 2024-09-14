// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 259.
 */
public class Opcode259 extends BaseOpcode {
  private static final String EFFECT_NUM_SPELLS     = "# spells";
  private static final String EFFECT_SPELL_LEVEL    = "Spell level";
  private static final String EFFECT_NUM_CREATURES  = "# creatures";
  private static final String EFFECT_SUMMON_TYPE    = "Summon type";

  private static final String RES_TYPE      = "SPL";
  private static final String RES_TYPE_IWD  = "CRE";

  private static final String[] SUMMON_TYPES_IWD = { "Default", "Ally", "Hostile", "Forced", "Genie" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD2:
      case PST:
        return null;
      case IWD:
        return "Summon creatures with cloud";
      default:
        return "Spell trap";
    }
  }

  public Opcode259() {
    super(259, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SPELLS));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_SPELL_LEVEL));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SPELLS));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_SPELL_LEVEL));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_CREATURES));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SUMMON_TYPE, SUMMON_TYPES_IWD));
    return RES_TYPE_IWD;
  }

}
