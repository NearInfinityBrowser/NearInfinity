// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 170.
 */
public class Opcode170 extends BaseOpcode {
  private static final String EFFECT_ANIMATION = "Animation";

  private static final String[] ANIMATION_TYPES = { "Blood (behind)", "Blood (front)", "Blood (left)", "Blood (right)",
      "Fire 1", "Fire 2", "Fire 3", "Electricity 1", "Electricity 2", "Electricity 3" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Play damage animation";
  }

  public Opcode170() {
    super(170, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_ANIMATION, ANIMATION_TYPES));
    return null;
  }
}
