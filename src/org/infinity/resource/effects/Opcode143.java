// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 143.
 */
public class Opcode143 extends BaseOpcode {
  private static final String EFFECT_SLOT = "Slot";

  private static final String RES_TYPE = "ITM";

  private static final String[] SLOT_TYPES_PST = { "Hand", "Eyeball/Earring (left)", "Tattoo", "Bracelet",
      "Ring (right)", "Tattoo (top left)", "Ring (left)", "Earring (right)/Lens", "Armor", "Tattoo (bottom right)",
      "Temporary weapon", "Ammo 1", "Ammo 2", "Ammo 3", "Ammo 4", "Ammo 5", "Ammo 6", "Quick item 1", "Quick item 2",
      "Quick item 3", "Quick item 4", "Quick item 5", "Inventory 1", "Inventory 2", "Inventory 3", "Inventory 4",
      "Inventory 5", "Inventory 6", "Inventory 7", "Inventory 8", "Inventory 9", "Inventory 10", "Inventory 11",
      "Inventory 12", "Inventory 13", "Inventory 14", "Inventory 15", "Inventory 16", "Inventory 17", "Inventory 18",
      "Inventory 19", "Inventory 20", "Magic weapon", "Weapon 1", "Weapon 2", "Weapon 3", "Weapon 4" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Create item in slot";
  }

  public Opcode143() {
    super(143, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_SLOT, "SLOTS.IDS", true, false, true));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_SLOT, SLOT_TYPES_PST));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return RES_TYPE;
  }
}
