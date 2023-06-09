// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 138.
 */
public class Opcode138 extends BaseOpcode {
  private static final String EFFECT_SEQUENCE = "Sequence";

  private static final String[] SEQ_TYPES = { "", "Lay down (short)", "Move hands (short)", "Move hands (long)",
      "Move shoulder (short)", "Move shoulder (long)", "Lay down (long)", "Breathe rapidly (short)",
      "Breath rapidly (long)" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Set animation sequence";
  }

  public Opcode138() {
    super(138, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_SEQUENCE, "SEQ.IDS"));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SEQUENCE, SEQ_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_SEQUENCE, "SEQUENCE.IDS"));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_SEQUENCE, "ANIMSTAT.IDS"));
    return null;
  }
}
