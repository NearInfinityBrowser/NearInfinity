// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 9.
 */
public class Opcode009 extends BaseOpcode {
  private static final String EFFECT_CYCLE_SPEED = "Cycle speed";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Set color glow pulse";
  }

  public Opcode009() {
    super(9, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new HashBitmap(buffer, offset + 4, 2, EFFECT_LOCATION, COLOR_LOCATIONS_MAP, false));
    list.add(new DecNumber(buffer, offset + 6, 2, EFFECT_CYCLE_SPEED));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new DecNumber(buffer, offset + 4, 2, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 6, 2, EFFECT_CYCLE_SPEED));
    return null;
  }
}
