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
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 241.
 */
public class Opcode241 extends BaseOpcode {
  private static final String EFFECT_CRE_TYPE   = "Creature type";
  private static final String EFFECT_CHARM_TYPE = "Charm type";
  private static final String EFFECT_DIRECTION  = "Direction";

  private static final String[] DIRECTIONS_IWD = { "Target to source", "Source to target" };

  private static final TreeMap<Long, String> CHARM_MAP = new TreeMap<>();

  static {
    CHARM_MAP.put(0L, "Charmed (neutral)");
    CHARM_MAP.put(1L, "Charmed (hostile)");
    CHARM_MAP.put(2L, "Dire charmed (neutral)");
    CHARM_MAP.put(3L, "Dire charmed (hostile)");
    CHARM_MAP.put(4L, "Controlled");
    CHARM_MAP.put(5L, "Hostile");
    CHARM_MAP.put(1000L, "Charmed (neutral, no text)");
    CHARM_MAP.put(1001L, "Charmed (hostile, no text)");
    CHARM_MAP.put(1002L, "Dire charmed (neutral, no text)");
    CHARM_MAP.put(1003L, "Dire charmed (hostile, no text)");
    CHARM_MAP.put(1004L, "Controlled (no text)");
    CHARM_MAP.put(1005L, "Hostile (no text)");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      case IWD:
      case IWD2:
        return "Vampiric touch";
      default:
        return "Control creature";
    }
  }

  public Opcode241() {
    super(241, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_CRE_TYPE, "GENERAL.IDS"));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_CHARM_TYPE, CHARM_MAP));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DIRECTION, DIRECTIONS_IWD));
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
