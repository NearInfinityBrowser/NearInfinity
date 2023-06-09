// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 61.
 */
public class Opcode061 extends BaseOpcode {
  private static final String EFFECT_FADE_SPEED = "Fade speed";

  private static final String[] ALCHEMY_TYPES_IWD2 = { "Increment", "Set", "Mastery" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Alchemy";
      case EE:
        return "Creature RGB color fade";
      default:
        return null;
    }
  }

  public Opcode061() {
    super(61, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new DecNumber(buffer, offset + 4, 2, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 6, 2, EFFECT_FADE_SPEED));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, ALCHEMY_TYPES_IWD2));
    return null;
  }
}
