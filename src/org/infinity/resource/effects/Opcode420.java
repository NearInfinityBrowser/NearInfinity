// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 420.
 */
public class Opcode420 extends BaseOpcode {
  private static final String EFFECT_DEATH_TYPE = "Death type";

  private static final String[] DEATH_TYPES_IWD2 = { "Acid", "Burning", "Crushing", "Normal", "Exploding", "Stoned",
      "Freezing", null, null, null, "Permanent", "Destruction" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Death magic";
      default:
        return null;
    }
  }

  public Opcode420() {
    super(420, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, DEATH_TYPES_IWD2));
    return null;
  }
}
