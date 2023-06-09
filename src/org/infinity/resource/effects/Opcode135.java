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
 * Implemention of opcode 135.
 */
public class Opcode135 extends BaseOpcode {
  private static final String EFFECT_POLYMORPH_TYPE = "Polymorph type";

  private static final String RES_TYPE = "CRE";

  private static final String[] POLYMORPH_TYPES = { "Change into", "Appearance only", "Appearance only",
      "Appearance only" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Polymorph";
  }

  public Opcode135() {
    super(135, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_POLYMORPH_TYPE, POLYMORPH_TYPES));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return RES_TYPE;
  }
}
