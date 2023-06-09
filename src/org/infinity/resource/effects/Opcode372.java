// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.Flag;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 372.
 */
public class Opcode372 extends BaseOpcode {
  private static final String EFFECT_FLAGS = "Flags";

  private static final String RES_TYPE = "BAM";

  private static final String[] FLAGS = { null,
      "Random placement", null, null, null, null, null, null, null,
      null, null, null, null, "Sticky", null, null, null,
      "Repeat;Only effective if bit 17 is set", "Foreground", null, null,
      "Fade out;Duration defined by Dice Size, initial transparency defined by red color value. Effect changes if bit 21 is set.",
      "Blended;Only if bit 20 is also set.", null, null };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Play BAM file 3";
        }
      default:
        return null;
    }
  }

  public Opcode372() {
    super(372, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new ColorPicker(buffer, offset, EFFECT_COLOR, ColorPicker.Format.RGBX));
      list.add(new Flag(buffer, offset + 4, 4, EFFECT_FLAGS, FLAGS));
      return RES_TYPE;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
