// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 147.
 */
public class Opcode147 extends BaseOpcode {
  private static final String EFFECT_BEHAVIOR   = "Behavior";
  private static final String EFFECT_SPELL_TYPE = "Spell type";

  private static final String RES_TYPE = "SPL";

  private static final String[] BEHAVIOR_TYPES    = { "Default", "No XP", null, "Always successful", null,
      "No XP if already learned", "Exclude spell schools", "Exclude sorcerer", "Fail if max. spells learned" };
  private static final String[] SPELL_TYPES_IWD2  = { "Arcane", "Divine", "Innate" };
  private static final String[] SPELL_TYPES_PST   = { "Wizard", "Priest", "Innate" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Learn spell";
  }

  public Opcode147() {
    super(147, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new DecNumber(buffer, offset, 2, AbstractStruct.COMMON_UNUSED));
      list.add(new Flag(buffer, offset + 2, 2, EFFECT_BEHAVIOR, BEHAVIOR_TYPES));
      list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
      return RES_TYPE;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SPELL_TYPE, SPELL_TYPES_IWD2));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SPELL_TYPE, SPELL_TYPES_PST));
    return RES_TYPE;
  }
}
