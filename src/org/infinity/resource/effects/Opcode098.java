// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 98.
 */
public class Opcode098 extends BaseOpcode {
  private static final String EFFECT_REGENERATION_TYPE = "Regeneration type";

  private static final String[] REGEN_TYPES_PST = { "Regen all HP", "Regenerate amount percentage",
      "Amount HP per second", "1 HP per amount seconds" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Regeneration";
  }

  public Opcode098() {
    super(98, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, REGEN_TYPES);
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, REGEN_TYPES_IWD);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, REGEN_TYPES_IWD);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, REGEN_TYPES_PST);
  }

  protected String makeEffectParamsInternal(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1, String[] regenTypes) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    final Bitmap bmp = new Bitmap(buffer, offset + 4, 4, EFFECT_REGENERATION_TYPE, regenTypes);
    list.add(bmp);
    if (parent != null && parent instanceof UpdateListener) {
      bmp.addUpdateListener((UpdateListener)parent);
    }
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_ICON, getPortraitIconNames(STRING_DEFAULT)));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
