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
 * Implementation of opcode 298.
 */
public class Opcode298 extends BaseOpcode {
  private static final String EFFECT_STORE_LOCATION = "Store party location";
  private static final String EFFECT_USE_SCRIPT     = "Use custom script?";

  private static final String RES_TYPE = "BCS";

  private static final String[] LOCATIONS_TOBEX = { "Use pocket plane field", "Use party location field",
      "Do not store" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD:
      case PST:
        return null;
      case IWD2:
        return "Use magic device bonus";
      default:
        return "Pocket plane";
    }
  }

  public Opcode298() {
    super(298, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_STAT_VALUE));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_STORE_LOCATION, LOCATIONS_TOBEX));
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_USE_SCRIPT, AbstractStruct.OPTION_NOYES));
      return RES_TYPE;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }
}
