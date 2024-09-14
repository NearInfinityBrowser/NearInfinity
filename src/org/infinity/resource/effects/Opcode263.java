// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 263.
 */
public class Opcode263 extends BaseOpcode {
  private static final String EFFECT_CREATURE_TYPE  = "Creature type";
  private static final String EFFECT_TURN_TYPE      = "Turn type";

  private static final TreeMap<Long, String> TURN_MAP_IWD = new TreeMap<>();

  static {
    TURN_MAP_IWD.put(0L, "Charmed (neutral)");
    TURN_MAP_IWD.put(1L, "Charmed (hostile)");
    TURN_MAP_IWD.put(2L, "Dire charmed (neutral)");
    TURN_MAP_IWD.put(3L, "Dire charmed (hostile)");
    TURN_MAP_IWD.put(4L, "Controlled");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      case IWD:
      case IWD2:
        return "Evil turn undead";
      default:
        return "Backstab bonus";
    }
  }

  public Opcode263() {
    super(263, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_CREATURE_TYPE, "GENERAL.IDS"));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_TURN_TYPE, TURN_MAP_IWD));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsIWD(parent, buffer, offset, list, isVersion1);
  }
}
