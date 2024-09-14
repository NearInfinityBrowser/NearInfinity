// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 303.
 */
public class Opcode303 extends BaseOpcode {
  private static final TreeMap<Long, String> TYPE_MAP_TOBEX = new TreeMap<>();

  static {
    TYPE_MAP_TOBEX.put(0L, "Normal conditions");
    TYPE_MAP_TOBEX.put(1L, "Ignore visual state and position");
    TYPE_MAP_TOBEX.put(2L, "Ignore visual state only");
    TYPE_MAP_TOBEX.put(4L, "Ignore position only");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD:
      case IWD2:
      case PST:
        return null;
      default:
        return "Backstab every hit";
    }
  }

  public Opcode303() {
    super(303, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_STAT_VALUE));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
      list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_TYPE, TYPE_MAP_TOBEX));
      return null;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }
}
