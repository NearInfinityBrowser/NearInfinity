// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.TextString;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 402.
 */
public class Opcode402 extends BaseOpcode {
  private static final String EFFECT_LUA_FUNCTION = "Lua function";
  private static final String EFFECT_CRE_TYPE = "Creature type";

  private static final String RES_TYPE_IWD2 = "SPL";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Apply effects list";
      case EE:
        if (isEEEx()) {
          return "EEex: Invoke Lua";
        }
      default:
        return null;
    }
  }

  public Opcode402() {
    super(402, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isEEEx()) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
      list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
      return null;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_CRE_TYPE, CRE_TYPES_IWD2));
    return RES_TYPE_IWD2;
  }

  @Override
  protected int makeEffectResource(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition() && isEEEx()) {
      list.add(new TextString(buffer, offset, 8, EFFECT_LUA_FUNCTION));
      return offset + 8;
    } else {
      return super.makeEffectResource(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
