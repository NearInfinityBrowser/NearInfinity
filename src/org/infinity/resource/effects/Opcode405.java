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
 * Implemention of opcode 405.
 */
public class Opcode405 extends BaseOpcode {
  private static final String EFFECT_INDEX_TO_OVERRIDE  = "Index to override";
  private static final String EFFECT_OVERRIDE_WITH_TYPE = "Override with type";
  private static final String EFFECT_TARGET_CONFIG      = "Target config";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Enfeeblement";
      case EE:
        if (isEEEx()) {
          return "EEex: Override Button Index";
        }
      default:
        return null;
    }
  }

  public Opcode405() {
    super(405, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isEEEx()) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_INDEX_TO_OVERRIDE));
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_OVERRIDE_WITH_TYPE, BUTTON_TYPES));
      return null;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition() && isEEEx()) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_TARGET_CONFIG));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
