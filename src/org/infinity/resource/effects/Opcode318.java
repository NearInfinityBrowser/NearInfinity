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
import org.infinity.datatype.SpellProtType;
import org.infinity.datatype.StringRef;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 318.
 */
public class Opcode318 extends BaseOpcode {
  private static final String EFFECT_STAT_OPCODE  = "Stat opcode";
  private static final String EFFECT_DESC_NOTE    = "Description note";

  private static final String RES_TYPE = "EFF:ITM:SPL";

  private static final String[] OPERATIONS_TOBEX = { "Increment", "Set", "Set % of", "Multiply", "Divide", "Modulus",
      "Logical AND", "Logical OR", "Bitwise AND", "Bitwise OR", "Invert" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Protection from resource";
      case BG2:
        if (isTobEx()) {
          return "Ex: Set stat";
        }
      default:
        return null;
    }
  }

  public Opcode318() {
    super(318, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
      list.add(new IdsBitmap(buffer, offset + 4, 2, EFFECT_STAT_OPCODE, "STATS.IDS"));
      list.add(new Bitmap(buffer, offset + 6, 2, EFFECT_MODIFIER_TYPE, OPERATIONS_TOBEX));
      return null;
    } else {
      return super.makeEffectParamsBG2(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final SpellProtType param2 = new SpellProtType(buffer, offset + 4, 4);
    list.add(param2.createCreatureValueFromType(buffer, offset));
    list.add(param2);
    return RES_TYPE;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new StringRef(buffer, offset, EFFECT_DESC_NOTE));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
