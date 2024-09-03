// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.SpellProtType;
import org.infinity.datatype.StringRef;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 324.
 */
public class Opcode324 extends BaseOpcode {
  private static final String EFFECT_OVERRIDE_STRREF = "EEex: Override strref";

  private static final String RES_TYPE = "EFF:ITM:SPL";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Immunity to resource and message";
      default:
        return null;
    }
  }

  public Opcode324() {
    super(324, getOpcodeName());
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
    if (Profile.isEnhancedEdition() && isEEEx()) {
      list.add(new StringRef(buffer, offset, EFFECT_OVERRIDE_STRREF));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
