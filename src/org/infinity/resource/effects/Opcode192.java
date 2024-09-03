// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 192.
 */
public class Opcode192 extends BaseOpcode {
  private static final String EFFECT_DIRECTION    = "Direction";
  private static final String EFFECT_DAMAGE_TYPE  = "Damage type";

  private static final String[] DIRECTIONS = { "Source to target", "Target to source", "Swap HP",
      "Source to target even over max. HP" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
        return null;
      case PST:
        return "Hit point transfer";
      default:
        return "Find familiar";
    }
  }

  public Opcode192() {
    super(192, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_DIRECTION, DIRECTIONS));
    list.add(new IdsBitmap(buffer, offset + 6, 2, EFFECT_DAMAGE_TYPE, "DAMAGES.IDS"));
    return null;
  }
}
