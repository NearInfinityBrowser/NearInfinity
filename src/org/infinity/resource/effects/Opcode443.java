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
 * Implemention of opcode 443.
 */
public class Opcode443 extends BaseOpcode {
  private static final String EFFECT_DAMAGE_REDUCTION = "Damage reduction";

  private static final String[] DAMAGE_REDUCTIONS_IWD2 = { "None", "10/+1", "10/+2", "10/+3", "10/+4", "10/+5" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Protection from arrows";
      default:
        return null;
    }
  }

  public Opcode443() {
    super(443, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DAMAGE_REDUCTION, DAMAGE_REDUCTIONS_IWD2));
    return null;
  }
}
