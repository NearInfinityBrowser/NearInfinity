// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 303.
 */
public class Opcode303 extends BaseOpcode {
  private static final String EFFECT_CONDITION = "Condition";

  private static final String[] CONDITIONS = { "Disabled", "Always" };
  private static final String[] CONDITIONS_EX = { "Disabled", "Always", "Ignore invisibility", "Ignore positioning" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD:
      case IWD2:
      case PST:
        return null;
      default:
        return "Backstab every hit";
    }
  }

  public Opcode303() {
    super(303, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_STAT_VALUE));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_CONDITION, CONDITIONS_EX));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    if (isTobEx()) {
      list.add(new Flag(buffer, offset + 4, 4, EFFECT_CONDITION, CONDITIONS_EX));
    } else {
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_CONDITION, CONDITIONS));
    }
    return null;
  }
}
