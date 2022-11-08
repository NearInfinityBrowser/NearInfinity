// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
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
 * Implemention of opcode 37.
 */
public class Opcode037 extends BaseOpcode {
  private static final String[] TYPES_EE = { INC_TYPES[0], INC_TYPES[1], INC_TYPES[2], "Increment instantaneously" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      return null;
    } else {
      return "Save vs. spell bonus";
    }
  }

  public Opcode037() {
    super(37, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, TYPES_EE));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }
}
