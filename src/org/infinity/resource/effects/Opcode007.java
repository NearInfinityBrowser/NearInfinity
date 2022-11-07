// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.ColorValue;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 7.
 */
public class Opcode007 extends BaseOpcode {
  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Set color";
  }

  public Opcode007() {
    super(7, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorValue(buffer, offset, 4, EFFECT_COLOR, false));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_LOCATION, COLOR_LOCATIONS_MAP, false));
    return null;
  }
}
