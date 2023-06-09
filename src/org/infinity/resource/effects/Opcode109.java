// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsTargetType;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 109.
 */
public class Opcode109 extends BaseOpcode {
  private static final String EFFECT_FX = "Effect";

  private static final String[] PARALYZE_EFFECTS_EE = { "Normal", "Fake petrification" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Paralyze";
  }

  public Opcode109() {
    super(109, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final IdsTargetType param2 = new IdsTargetType(buffer, offset + 4);
    list.add(param2.createIdsValueFromType(buffer));
    list.add(param2);
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_FX, PARALYZE_EFFECTS_EE));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
