// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 18.
 */
public class Opcode018 extends BaseOpcode {
  private static final String[] TYPES = { INC_TYPES[0], INC_TYPES[1], INC_TYPES[2],
      "Increment, don't update current HP", "Set, don't update current HP", "Set % of, don't update current HP" };
  private static final String[] TYPES_EE;

  private static final String[] MODES_EE = { "Normal", "Max. HP only" };

  static {
    TYPES_EE = Arrays.copyOf(TYPES, TYPES.length + 1);
    TYPES_EE[TYPES.length] = "Increment, non-cumulative";
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Maximum HP bonus";
  }

  public Opcode018() {
    super(18, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, TYPES_EE));
    return null;
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
