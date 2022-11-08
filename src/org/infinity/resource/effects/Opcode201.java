// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 201.
 */
public class Opcode201 extends BaseOpcode {
  private static final String EFFECT_NUM_LEVELS   = "# levels";
  private static final String EFFECT_SPELL_LEVEL  = "Spell level";
  private static final String EFFECT_FX           = "Effect";

  private static final String RES_TYPE = "SPL";
  private static final String RES_TYPE_PST = "BAM";

  private static final String[] EFFECTS_PST = { "Cloak of warding", "Shield", "Black-barbed shield", "Pain mirror",
      "Guardian mantle", "", "Enoll eva's duplication", "Armor", "Antimagic shell", "", "", "Flame walk",
      "Protection from evil", "Conflagration", "Infernal shield", "Submerge the will", "Balance in all things" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD:
      case IWD2:
        return null;
      case PST:
        return "Play BAM with effects";
      default:
        return "Spell deflection";
    }
  }

  public Opcode201() {
    super(201, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_LEVELS));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_SPELL_LEVEL));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_LEVELS));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_SPELL_LEVEL));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FX, EFFECTS_PST));
    return RES_TYPE_PST;
  }
}
