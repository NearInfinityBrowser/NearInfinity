// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 60.
 */
public class Opcode060 extends BaseOpcode {
  private static final String EFFECT_FAILURE_TYPE = "Failure type";
  private static final String EFFECT_SPELL_CLASS  = "Spell class";

  private static final String[] SPELL_TYPES       = { "Wizard", "Priest", "Innate" };
  private static final String[] SPELL_TYPES_BG2   = { "Wizard", "Priest", "Innate", "Wizard (dead magic)",
      "Priest (dead magic)", "Innate (dead magic)" };
  private static final String[] SPELL_TYPES_IWD2  = { "Arcane", "Divine", "All spells" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Casting failure";
  }

  public Opcode060() {
    super(60, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FAILURE_TYPE, SPELL_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FAILURE_TYPE, SPELL_TYPES_BG2));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FAILURE_TYPE, SPELL_TYPES_BG2));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SPELL_CLASS, SPELL_TYPES_IWD2));
    return null;
  }
}
