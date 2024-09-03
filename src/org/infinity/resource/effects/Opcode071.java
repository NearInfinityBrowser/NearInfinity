// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 71.
 */
public class Opcode071 extends BaseOpcode {
  private static final String EFFECT_GENDER = "Gender";
  private static final String EFFECT_HOW    = "How?";

  private static final String[] CHANGE_TYPES = { "Reverse gender", "Set gender" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Change gender";
  }

  public Opcode071() {
    super(71, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new IdsBitmap(buffer, offset, 4, EFFECT_GENDER, "GENDER.IDS"));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_HOW, CHANGE_TYPES));
    return null;
  }
}
