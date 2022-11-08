// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 417.
 */
public class Opcode417 extends BaseOpcode {
  private static final String EFFECT_RADIUS           = "Radius";
  private static final String EFFECT_AREA_EFFECT_TYPE = "Area effect type";

  private static final String RES_TYPE_IWD2 = "SPL";

  private static final String[] EFFECT_TYPES_IWD2 = { "Instant", "Once per round", "Ignore target" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Area effect using effects list";
      default:
        return null;
    }
  }

  public Opcode417() {
    super(417, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_RADIUS));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_AREA_EFFECT_TYPE, EFFECT_TYPES_IWD2));
    return RES_TYPE_IWD2;
  }
}
