// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
 * Implemention of opcode 141.
 */
public class Opcode141 extends BaseOpcode {
  private static final String EFFECT_TARGET = "Target";
  private static final String EFFECT_FX = "Effect";

  private static final String[] TARGET_TYPES = { "Spell target", "Target point" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Lighting effects";
  }

  public Opcode141() {
    super(141, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FX, LIGHTING));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_TARGET, TARGET_TYPES));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FX, LIGHTING));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG2(parent, buffer, offset, list, isVersion1);
  }
}
