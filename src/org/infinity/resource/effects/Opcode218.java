// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 218.
 */
public class Opcode218 extends BaseOpcode {
  private static final String EFFECT_NUM_SKINS          = "# skins";
  private static final String EFFECT_USE_DICE           = "Use dice?";
  private static final String EFFECT_ON_SKINS_DESTROYED = "EEex: On skins destroyed";
  private static final String EFFECT_SKIN_TYPE          = "Skin type";

  private static final String RES_TYPE = "SPL";

  private static final String[] SKIN_TYPES_IWD2 = { "Stoneskin", "Iron skins" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      default:
        return "Stoneskin effect";
    }
  }

  public Opcode218() {
    super(218, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SKINS));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SKINS));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_USE_DICE, AbstractStruct.OPTION_NOYES));
    return isEEEx() ? RES_TYPE : null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_SKINS));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SKIN_TYPE, SKIN_TYPES_IWD2));
    return null;
  }

  @Override
  protected int makeEffectResource(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition() && isEEEx()) {
      list.add(new ResourceRef(buffer, offset, EFFECT_ON_SKINS_DESTROYED, resType.split(":")));
      return offset + 8;
    } else {
      return super.makeEffectResource(parent, buffer, offset, list, resType, param1, param2);
    }
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
