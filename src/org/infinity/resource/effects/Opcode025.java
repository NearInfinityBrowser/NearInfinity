// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.UpdateListener;
import org.infinity.gui.TextListPanel;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 25.
 */
public class Opcode025 extends BaseOpcode {
  private static final String EFFECT_POISON_TYPE = "Poison type";

  private static final String[] TYPES     = { "1 damage per second", "1 damage per second", "Amount damage per second",
      "1 damage per amount seconds", "Param3 damage per amount seconds" };
  private static final String[] TYPES_IWD = { "1 damage per second", "Amount damage per second",
      "Amount damage per second", "1 damage per amount seconds", "Amount damage per round", "(Crash)", "Snakebite",
      "Unused", "Envenomed weapon" };
  private static final String[] TYPES_BG1;
  private static final String[] TYPES_IWD2;

  static {
    TYPES_BG1 = Arrays.copyOf(TYPES, TYPES.length);
    TYPES_BG1[3] = "1 damage per amount+1 seconds";

    TYPES_IWD2 = Arrays.copyOf(TYPES_IWD, TYPES_IWD.length);
    TYPES_IWD2[5] = "Unused";
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Poison";
  }

  public Opcode025() {
    super(25, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, TYPES);
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, TYPES_BG1);
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, TYPES_IWD);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsInternal(parent, buffer, offset, list, isVersion1, TYPES_IWD2);
  }

  private String makeEffectParamsInternal(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1, String[] poisonTypes) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    final Bitmap bmp = new Bitmap(buffer, offset + 4, 4, EFFECT_POISON_TYPE, poisonTypes);
    list.add(bmp);
    if (parent instanceof UpdateListener) {
      bmp.addUpdateListener((UpdateListener)parent);
    }
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      final Bitmap bitmap = new Bitmap(buffer, offset, 4, EFFECT_ICON, getPortraitIconNames(STRING_DEFAULT));
      bitmap.setIconType(TextListPanel.IconType.PORTRAIT);
      list.add(bitmap);
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
