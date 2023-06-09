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
import org.infinity.datatype.ProRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 140.
 */
public class Opcode140 extends BaseOpcode {
  private static final String EFFECT_GLOW = "Glow";
  private static final String EFFECT_PROJECTILE = "Projectile";

  private static final TreeMap<Long, String> GLOW_MAP = new TreeMap<>();
  private static final TreeMap<Long, String> GLOW_MAP_EE;

  static {
    GLOW_MAP.put(9L, "Necromancy");
    GLOW_MAP.put(10L, "Alteration");
    GLOW_MAP.put(11L, "Enchantment");
    GLOW_MAP.put(12L, "Abjuration");
    GLOW_MAP.put(13L, "Illusion");
    GLOW_MAP.put(14L, "Conjuration");
    GLOW_MAP.put(15L, "Invocation");
    GLOW_MAP.put(16L, "Divination");

    GLOW_MAP_EE = new TreeMap<>(GLOW_MAP);
    GLOW_MAP_EE.put(0L, "Use projectile");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Casting glow";
  }

  public Opcode140() {
    super(140, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_GLOW, GLOW_MAP));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ProRef(buffer, offset, EFFECT_PROJECTILE, false));
    list.add(new DecNumber(buffer, offset + 2, 2, AbstractStruct.COMMON_UNUSED));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_GLOW, GLOW_MAP_EE));
    return null;
  }
}
