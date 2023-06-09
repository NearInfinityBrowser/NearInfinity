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
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 17.
 */
public class Opcode017 extends BaseOpcode {
  private static final String EFFECTS_HEAL_FLAGS = "Heal flags";

  private static final String[] TYPE_IWD2 = { INC_TYPES[0], INC_TYPES[1], INC_TYPES[2], "Lay on hands",
      "Wholeness of body", "Lathander's renewal" };

  private static final String[] FLAGS     = { "Heal normally", "Raise dead", "Remove limited effects" };
  private static final String[] FLAGS_IWD = { "No flags set", "Raise dead" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Current HP bonus";
  }

  public Opcode017() {
    super(17, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_MODIFIER_TYPE, INC_TYPES));
    list.add(new Flag(buffer, offset + 6, 2, EFFECTS_HEAL_FLAGS, FLAGS));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_MODIFIER_TYPE, INC_TYPES));
    list.add(new Flag(buffer, offset + 6, 2, EFFECTS_HEAL_FLAGS, FLAGS_IWD));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, TYPE_IWD2));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }
}
