// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 248.
 */
public class Opcode248 extends BaseOpcode {
  private static final String EFFECT_NUM_CREATURES  = "# creatures";
  private static final String EFFECT_SHADOW_TYPE    = "Shadow type";

  private static final String RES_TYPE = "EFF";

  private static final String[] SHADOW_TYPES_IWD = { "Shadow", "Demishadow", "Shade" };

  private static final TreeMap<Long, String> TYPE_MAP = new TreeMap<>();

  static {
    TYPE_MAP.put(0L, "Default");
    TYPE_MAP.put(4L, "Fists only");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      case IWD:
      case IWD2:
        return "Summon shadow";
      default:
        return "Melee hit effect";
    }
  }

  public Opcode248() {
    super(248, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_TYPE, TYPE_MAP, false));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_CREATURES));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SHADOW_TYPE, SHADOW_TYPES_IWD));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsIWD(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }
}
