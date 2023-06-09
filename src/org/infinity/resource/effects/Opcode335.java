// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 335.
 */
public class Opcode335 extends BaseOpcode {
  private static final String EFFECT_STATE      = "State";
  private static final String EFFECT_IDENTIFIER = "Identifier";
  private static final String EFFECT_EYE_GROUP  = "Eye group";

  private static final String RES_TYPE = "SPL";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Seven eyes";
      default:
        return null;
    }
  }

  public Opcode335() {
    super(335, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_STATE, "SPLSTATE.IDS"));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_IDENTIFIER));
    return RES_TYPE;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_EYE_GROUP));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
