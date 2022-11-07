// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 383.
 */
public class Opcode383 extends BaseOpcode {
  private static final String EFFECT_DIRECTION    = "Direction";
  private static final String EFFECT_DAMAGE_TYPE  = "Damage type";

  private static final String[] DIRECTIONS = { "Source to target", "Target to source", "Swap HP",
      "Source to target even over max. HP" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Hit point transfer";
        }
      default:
        return null;
    }
  }

  public Opcode383() {
    super(383, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
      list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_DIRECTION, DIRECTIONS));
      list.add(new IdsBitmap(buffer, offset + 6, 2, EFFECT_DAMAGE_TYPE, "DAMAGES.IDS"));
      return null;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
