// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 66.
 */
public class Opcode066 extends BaseOpcode {
  private static final String EFFECT_FADE_AMOUNT    = "Fade amount";
  private static final String EFFECT_VISUAL_EFFECT  = "Visual effect";

  private static final String[] VISUAL_TYPES = { "Draw instantly", "Fade in", "Fade out" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Translucency";
  }

  public Opcode066() {
    super(66, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_FADE_AMOUNT));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_FADE_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_VISUAL_EFFECT, VISUAL_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
  }
}
