// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
 * Implementation of opcode 50.
 */
public class Opcode050 extends BaseOpcode {
  private static final String EFFECT_CYCLE_SPEED = "Cycle speed";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Character color pulse";
  }

  public Opcode050() {
    super(50, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new HashBitmap(buffer, offset + 4, 1, EFFECT_LOCATION, COLOR_LOCATIONS_MAP, false));
    list.add(new DecNumber(buffer, offset + 5, 1, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 6, 1, EFFECT_CYCLE_SPEED));
    list.add(new DecNumber(buffer, offset + 7, 1, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }
}
