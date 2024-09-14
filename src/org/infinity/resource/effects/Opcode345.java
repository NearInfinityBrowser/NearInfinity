// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 345.
 */
public class Opcode345 extends BaseOpcode {
  private static final String EFFECT_ENCHANTMENT = "Enchantment";
  private static final String EFFECT_ITEM_TYPE   = "Item type";
  private static final String EFFECT_WEAPON_SLOT = "Weapon slot";


  private static final String[] ITEM_TYPES = { "Magical weapons (<= 'Enchantment')", "Magical weapons (all)",
      "Non-magical weapons", "Silver", "Non-silver", "Non-silver/non-magical", "Two-Handed", "Non-two-handed", "Cursed",
      "Non-cursed", "Cold iron", "Non-cold iron" };

  private static final String[] WEAPON_SLOTS_EE = { "Current weapon", "Main hand weapon", "Off-hand weapon",
      "Both weapons" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Enchantment bonus";
      default:
        return null;
    }
  }

  public Opcode345() {
    super(345, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_ENCHANTMENT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_ITEM_TYPE, ITEM_TYPES));
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_WEAPON_SLOT, WEAPON_SLOTS_EE));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
