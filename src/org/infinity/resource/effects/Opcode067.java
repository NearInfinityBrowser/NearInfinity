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
 * Implemention of opcode 67.
 */
public class Opcode067 extends BaseOpcode {
  private static final String EFFECT_NUM_CREATURES    = "# creatures";
  private static final String EFFECT_ALLEGIANCE       = "Allegiance";
  private static final String EFFECT_SUMMON_ANIMATION = "Summon animation";

  private static final String RES_TYPE = "CRE";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Summon creature";
  }

  public Opcode067() {
    super(67, getOpcodeName());
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
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_ALLEGIANCE, SUMMON_CONTROLS));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG2(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG2(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_CREATURES));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SUMMON_ANIMATION, SUMMON_ANIMS));
    return RES_TYPE;
  }
}
