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
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 351.
 */
public class Opcode351 extends BaseOpcode {
  private static final String EFFECT_AMOUNT = "Amount";
  private static final String EFFECT_ADD_CASTER_LEVEL_BONUS = "Add caster level bonus";

  private static final String RES_TYPE = "BAM:VVC";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Armor";
        }
      default:
        return null;
    }
  }

  public Opcode351() {
    super(351, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_ADD_CASTER_LEVEL_BONUS, AbstractStruct.OPTION_NOYES));
      return RES_TYPE;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
