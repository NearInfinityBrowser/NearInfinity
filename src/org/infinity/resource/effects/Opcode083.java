// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.util.IdsMapEntry;

/**
 * Implemention of opcode 83.
 */
public class Opcode083 extends BaseOpcode {
  private static final String EFFECT_PROJECTILE = "Projectile";

  private static final TreeMap<Long, String> PRO_MAP = new TreeMap<>();
  private static final TreeMap<Long, String> PRO_MAP_BG1;

  static {
    PRO_MAP.put(0L, "None");
    PRO_MAP.put(4L, "Arrow");
    PRO_MAP.put(9L, "Axe");
    PRO_MAP.put(14L, "Bolt");
    PRO_MAP.put(19L, "Bullet");
    PRO_MAP.put(26L, "Throwing Dagger");
    PRO_MAP.put(34L, "Dart");

    PRO_MAP_BG1 = new TreeMap<>(PRO_MAP);
    PRO_MAP_BG1.put(64L, "Gaze");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Immunity to projectile";
  }

  public Opcode083() {
    super(83, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    final IdsBitmap ids = new IdsBitmap(buffer, offset + 4, 4, "Projectile", "PROJECTL.IDS");
    ids.addIdsMapEntry(new IdsMapEntry(0L, "None"));
    list.add(ids);
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_PROJECTILE, PRO_MAP_BG1));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_PROJECTILE, PROJECTILES_IWD_MAP));
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
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_PROJECTILE, PRO_MAP));
    return null;
  }
}
