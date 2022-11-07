// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsFlag;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 357.
 */
public class Opcode357 extends BaseOpcode {
  private static final String EFFECT_ACTION = "Action";
  private static final String EFFECT_STATE  = "State";

  private static final String[] ACTIONS = { "Clear", "Set" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Set state";
        }
      default:
        return null;
    }
  }

  public Opcode357() {
    super(357, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_ACTION, ACTIONS));
      list.add(new IdsFlag(buffer, offset + 4, 4, EFFECT_STATE, "STATE.IDS"));
      return null;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
