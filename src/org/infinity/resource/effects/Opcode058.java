// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 58.
 */
public class Opcode058 extends BaseOpcode {
  private static final String EFFECT_LEVEL              = "Level";
  private static final String EFFECT_DISPEL_TYPE        = "Dispel type";
  private static final String EFFECT_WEAPON_DISPEL_TYPE = "Magic weapon dispel type";

  private static final String[] DISPEL_TYPES        = { "Always dispel", "Use caster level", "Use specific level" };
  private static final String[] WEAPON_DISPEL_TYPES = { "Always dispel", "Do not dispel", "Chance of dispel" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Dispel effects";
  }

  public Opcode058() {
    super(58, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_LEVEL));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DISPEL_TYPE, DISPEL_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_LEVEL));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_DISPEL_TYPE, DISPEL_TYPES));
    list.add(new Bitmap(buffer, offset + 6, 2, EFFECT_WEAPON_DISPEL_TYPE, WEAPON_DISPEL_TYPES));
    return null;
  }
}
