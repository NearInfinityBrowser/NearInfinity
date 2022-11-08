// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 13.
 */
public class Opcode013 extends BaseOpcode {
  private static final String EFFECT_DISPLAY_TEXT = "Display text?";
  private static final String EFFECT_DEATH_TYPE   = "Death type";

  private static final String[] TYPE_BG1 = { "Acid", "Burning", "Crushed", "Normal", "Exploding", "Stoned", "Freezing",
      "Exploding stoned", "Exploding freezing", "Electrified" };
  private static final String[] TYPE_BG2 = { "Acid", "Burning", "Crushed", "Normal", "Exploding", "Stoned", "Freezing",
      "Exploding stoned", "Exploding freezing", "Electrified", "Disintegration", null };
  private static final String[] TYPE_EE = { "Acid", "Burning", "Crushed", "Normal", "Exploding", "Stoned", "Freezing",
      "Exploding stoned", "Exploding freezing", "Electrified", "Disintegration",
      "Exploding (no drop);Exploding death, inventory is not dropped" };
  private static final String[] TYPE_IWD = { "Acid", "Burning", "Crushed", "Normal", "Exploding", "Stoned", "Freezing",
      null, null, null, "Disintegration", "Destruction" };
  private static final String[] TYPE_IWD2 = { "Acid", "Burning", "Crushed", "Normal", "Exploding", "Stoned", "Freezing",
      "Exploding stoned", "Exploding freezing", "Electrified", "Disintegration", "Destruction" };
  private static final String[] TYPE_PST = { "Normal", null, null, null, "Exploding", null, "Freezing",
      "Exploding stoned" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Kill target";
  }

  public Opcode013() {
    super(13, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_DISPLAY_TEXT, AbstractStruct.OPTION_YESNO));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, TYPE_BG2));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_DISPLAY_TEXT, AbstractStruct.OPTION_YESNO));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, TYPE_BG1));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_DISPLAY_TEXT, AbstractStruct.OPTION_YESNO));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, TYPE_EE));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_DISPLAY_TEXT, AbstractStruct.OPTION_YESNO));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, TYPE_IWD));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_DISPLAY_TEXT, AbstractStruct.OPTION_YESNO));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, TYPE_IWD2));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_DEATH_TYPE, TYPE_PST));
    return null;
  }
}
