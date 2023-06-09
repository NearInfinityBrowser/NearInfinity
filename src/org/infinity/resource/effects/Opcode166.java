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
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 166.
 */
public class Opcode166 extends BaseOpcode {
  private static final String[] INC_TYPES_GENERIC = Arrays.copyOf(INC_TYPES, 2);
  private static final String[] INC_TYPES_TOBEX   = { "Instantaneous", INC_TYPES[1], INC_TYPES[2], INC_TYPES[0] };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Magic resistance bonus";
  }

  public Opcode166() {
    super(166, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES_GENERIC));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES_TOBEX));
      return null;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }
}
