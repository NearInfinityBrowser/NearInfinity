// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 416.
 */
public class Opcode416 extends BaseOpcode {
  private static final String EFFECT_DAMAGE_TYPE = "Damage type";

  private static final String[] DAMAGE_TYPES_IWD2 = { "Amount HP per round", "Amount HP per second",
      "1 HP per amount seconds" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Bleeding wounds";
      default:
        return null;
    }
  }

  public Opcode416() {
    super(416, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DAMAGE_TYPE, DAMAGE_TYPES_IWD2));
    return null;
  }
}
