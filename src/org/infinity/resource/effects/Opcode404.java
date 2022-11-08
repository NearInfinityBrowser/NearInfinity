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
 * Implemention of opcode 404.
 */
public class Opcode404 extends BaseOpcode {
  private static final String EFFECT_TYPE_TO_OVERRIDE   = "Type to override";
  private static final String EFFECT_OVERRIDE_WITH_TYPE = "Override with type";
  private static final String EFFECT_NAUSEA_TYPE        = "Nausea type";

  private static final String[] NAUSEA_TYPES_IWD2 = { "Stinking cloud", "Ghoul touch" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Nausea";
      case EE:
        if (isEEEx()) {
          return "EEex: Override Button Type";
        }
      default:
        return null;
    }
  }

  public Opcode404() {
    super(404, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isEEEx()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_TYPE_TO_OVERRIDE, BUTTON_TYPES));
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
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_NAUSEA_TYPE, NAUSEA_TYPES_IWD2));
    return null;
  }
}
