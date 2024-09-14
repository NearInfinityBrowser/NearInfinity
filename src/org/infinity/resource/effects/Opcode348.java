// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 348.
 */
public class Opcode348 extends BaseOpcode {
  private static final String EFFECT_BASE_AMOUNT      = "Base amount";
  private static final String EFFECT_AMOUNT_PER_LEVEL = "Amount per level";

  private static final String RES_TYPE = "BAM:VVC";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Cloak of Warding";
        }
      default:
        return null;
    }
  }

  public Opcode348() {
    super(348, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_BASE_AMOUNT));
      list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_AMOUNT_PER_LEVEL));
      return RES_TYPE;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
