// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
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
 * Implemention of opcode 368.
 */
public class Opcode368 extends BaseOpcode {
  private static final String EFFECT_FLAGS = "Flags";

  private static final String RES_TYPE = "BAM";

  private static final String[] FLAGS = { "Default",
      null, null, null, null, null, null, "Blended", null,
      null, null, null, null, null, null, null, null,
      null, null, null, "Grayscale", null, "Bright", null, null,
      "Hide background on movement", "Dream palette", null, "Garbled frames?", null, null, null, null };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Play BAM with expiration effect";
        }
      default:
        return null;
    }
  }

  public Opcode368() {
    super(368, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNKNOWN));
      list.add(new Flag(buffer, offset + 4, 4, EFFECT_FLAGS, FLAGS));
      return RES_TYPE;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
