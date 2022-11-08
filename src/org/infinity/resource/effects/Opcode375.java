// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
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
 * Implemention of opcode 375.
 */
public class Opcode375 extends BaseOpcode {
  private static final String EFFECT_FX = "Effect";

  private static final String RES_TYPE = "BAM";

  private static final String[] EFFECTS = { "Cloak of warding", "Shield", "Black-barbed shield", "Pain mirror",
      "Guardian mantle", "", "Enoll eva's duplication", "Armor", "Antimagic shell", "", "", "Flame walk",
      "Protection from evil", "Conflagration", "Infernal shield", "Submerge the will", "Balance in all things" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Play BAM with effects";
        }
      default:
        return null;
    }
  }

  public Opcode375() {
    super(375, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FX, EFFECTS));
      return RES_TYPE;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
