// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 127.
 */
public class Opcode127 extends BaseOpcode {
  private static final String EFFECT_TOTAL_XP = "Total XP";
  private static final String EFFECT_FROM_2DA = "From 2DA file";

  private static final String RES_TYPE = "2DA";

  private static final String[] FILE_TYPES = { "Monsum01 (ally)", "Monsum02 (ally)", "Monsum03 (ally)",
      "Anisum01 (ally)", "Anisum02 (ally)", "Monsum01 (enemy)", "Monsum02 (enemy)", "Monsum03 (enemy)",
      "Anisum01 (enemy)", "Anisum02 (enemy)" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      return null;
    } else {
      return "Summon monsters";
    }
  }

  public Opcode127() {
    super(127, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_TOTAL_XP));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FROM_2DA, FILE_TYPES));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }
}
