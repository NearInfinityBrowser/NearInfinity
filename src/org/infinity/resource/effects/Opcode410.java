// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
 * Implementation of opcode 410.
 */
public class Opcode410 extends BaseOpcode {
  private static final String EFFECT_NUM_CREATURES    = "# creatures";
  private static final String EFFECT_SUMMON_ANIMATION = "Summon animation";

  private static final String RES_TYPE_IWD2 = "CRE";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Summon friendly creature";
      default:
        return null;
    }
  }

  public Opcode410() {
    super(410, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_CREATURES));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SUMMON_ANIMATION, SUMMON_ANIMS));
    return RES_TYPE_IWD2;
  }
}
