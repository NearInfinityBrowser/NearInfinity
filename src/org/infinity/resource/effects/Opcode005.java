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

/**
 * Implemention of opcode 5.
 */
public class Opcode005 extends BaseOpcode {
  private static final String EFFECT_CREATURE_TYPE = "Creature type";
  private static final String EFFECT_CHARM_TYPE = "Charm type";

  private static final TreeMap<Long, String> TYPE_CHARM = new TreeMap<>();
  private static final TreeMap<Long, String> TYPE_CHARM_IWD = new TreeMap<>();
  private static final TreeMap<Long, String> TYPE_CHARM_BG = new TreeMap<>();

  static {
    TYPE_CHARM.put(0L, "Charmed (neutral)");
    TYPE_CHARM.put(1L, "Charmed (hostile)");
    TYPE_CHARM.put(2L, "Dire charmed (neutral)");
    TYPE_CHARM.put(3L, "Dire charmed (hostile)");

    TYPE_CHARM_IWD.putAll(TYPE_CHARM);
    TYPE_CHARM_IWD.put(4L, "Controlled");
    TYPE_CHARM_IWD.put(5L, "Thrull (hostile)");

    TYPE_CHARM_BG.putAll(TYPE_CHARM_IWD);
    TYPE_CHARM_BG.put(1000L, "Charmed (neutral, no text)");
    TYPE_CHARM_BG.put(1001L, "Charmed (hostile, no text)");
    TYPE_CHARM_BG.put(1002L, "Dire charmed (neutral, no text)");
    TYPE_CHARM_BG.put(1003L, "Dire charmed (hostile, no text)");
    TYPE_CHARM_BG.put(1004L, "Controlled (no text)");
    TYPE_CHARM_BG.put(1005L, "Thrull (hostile, no text)");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Charm creature";
  }

  public Opcode005() {
    super(5, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_CREATURE_TYPE, "GENERAL.IDS"));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_CHARM_TYPE, TYPE_CHARM, false));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_CREATURE_TYPE, "GENERAL.IDS"));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_CHARM_TYPE, TYPE_CHARM_BG, false));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_CREATURE_TYPE, "GENERAL.IDS"));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_CHARM_TYPE, TYPE_CHARM_IWD, false));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_CREATURE_TYPE, "GENERAL.IDS"));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }
}
