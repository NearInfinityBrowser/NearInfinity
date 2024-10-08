// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 407.
 */
public class Opcode407 extends BaseOpcode {
  private static final String EFFECT_ON_REMOVE = "EEex: On remove";

  private static final String RES_TYPE = "SPL";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Death ward";
      case EE:
        if (isEEEx()) {
          return "EEex: On Remove";
        }
      default:
        return null;
    }
  }

  public Opcode407() {
    super(407, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isEEEx()) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
      list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
      return RES_TYPE;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected int makeEffectResource(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition() && isEEEx()) {
      list.add(new ResourceRef(buffer, offset, EFFECT_ON_REMOVE, resType.split(":")));
      return offset + 8;
    } else {
      return super.makeEffectResource(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
