// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 381.
 */
public class Opcode381 extends BaseOpcode {
  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Induce hiccups";
        }
      default:
        return null;
    }
  }

  public Opcode381() {
    super(381, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
      list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
      return null;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
