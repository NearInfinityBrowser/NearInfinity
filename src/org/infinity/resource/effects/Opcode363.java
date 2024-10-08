// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 363.
 */
public class Opcode363 extends BaseOpcode {
  private static final String EFFECT_MODAL_TO_CHECK = "Modal state to check";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Movement check";
      default:
        return null;
    }
  }

  public Opcode363() {
    super(363, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new IdsBitmap(buffer, offset, 4, EFFECT_MODAL_TO_CHECK, "MODAL.IDS"));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
