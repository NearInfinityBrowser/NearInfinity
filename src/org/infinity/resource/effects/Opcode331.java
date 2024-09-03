// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Summon2daBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 331.
 */
public class Opcode331 extends BaseOpcode {
  private static final String EFFECT_NUM_CREATURES = "# creatures";
  private static final String EFFECT_2DA_REFERENCE = "2DA reference";

  private static final String RES_TYPE = "2DA";

  private static final String[] MODES_EE = { "Use dice", "Use dice", "Use caster level" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Summon creatures 2";
      default:
        return null;
    }
  }

  public Opcode331() {
    super(331, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_CREATURES));
    list.add(new Summon2daBitmap(buffer, offset + 4, 4, EFFECT_2DA_REFERENCE));
    return RES_TYPE;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_MODE, MODES_EE));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
