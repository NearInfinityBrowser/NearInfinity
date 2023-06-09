// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 120.
 */
public class Opcode120 extends BaseOpcode {
  private static final String EFFECT_MAX_ENCHANTMENT  = "Maximum enchantment";
  private static final String EFFECT_WEAPON_TYPE      = "Weapon type";

  private static final String[] WEAPON_TYPES = { "Enchanted", "Magical", "Non-magical", "Silver", "Non-silver",
      "Non-silver, non-magical", "Two-handed", "One-handed", "Cursed", "Non-cursed", "Cold iron", "Non-cold-iron" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Immunity to weapons";
  }

  public Opcode120() {
    super(120, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_MAX_ENCHANTMENT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_WEAPON_TYPE, WEAPON_TYPES));
    return null;
  }
}
