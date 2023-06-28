// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.AnimateBitmap;
import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 53.
 */
public class Opcode053 extends BaseOpcode {
  private static final String EFFECT_MORPH_INTO = "Morph into";
  private static final String EFFECT_MORPH_TYPE = "Morph type";

  private static final String[] TYPES = { "Temporary change", "Remove temporary change", "Permanent change" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    if (Profile.getEngine() == Profile.Engine.IWD) {
      return AbstractStruct.COMMON_UNUSED;
    } else {
      return "Animation change";
    }
  }

  public Opcode053() {
    super(53, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new AnimateBitmap(buffer, offset, 4, EFFECT_MORPH_INTO));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MORPH_TYPE, TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }
}
