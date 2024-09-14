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
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 144.
 */
public class Opcode144 extends BaseOpcode {
  private static final String EFFECT_BUTTON = "Button";

  private static final String[] BUTTONS_TOBEX;
  private static final String[] BUTTONS_EE;

  static {
    BUTTONS_TOBEX = Arrays.copyOf(BUTTONS, 15);
    BUTTONS_TOBEX[10] = "Bard song";
    BUTTONS_TOBEX[14] = "Find traps";

    BUTTONS_EE = Arrays.copyOf(BUTTONS_TOBEX, 16);
    BUTTONS_EE[15] = "Inventory screen";
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Disable button";
  }

  public Opcode144() {
    super(144, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_BUTTON, BUTTONS));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_BUTTON, BUTTONS_TOBEX));
      return null;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_BUTTON, BUTTONS_EE));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_BUTTON, BUTTONS_IWD2));
    return null;
  }
}
