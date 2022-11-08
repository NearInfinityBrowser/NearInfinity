// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 0.
 */
public class Opcode000 extends BaseOpcode {
  private static final String EFFECT_AC_VALUE = "AC value";
  private static final String EFFECT_BONUS_TO = "Bonus to";

  private static final String[] TYPE_IWD2 = { "Generic", "Armor", "Deflection", "Shield", "Crushing", "Piercing",
      "Slashing", "Missile" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "AC bonus";
  }

  public Opcode000() {
    super(0, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AC_VALUE));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_BONUS_TO, AC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AC_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_BONUS_TO, TYPE_IWD2));
    return null;
  }
}
