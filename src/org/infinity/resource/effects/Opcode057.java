// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 57.
 */
public class Opcode057 extends BaseOpcode {
  private static final String EFFECT_ALIGNMENT = "Alignment";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Change alignment";
  }

  public Opcode057() {
    super(57, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_ALIGNMENT, Profile.getProperty(Profile.Key.GET_IDS_ALIGNMENT)));
    return null;
  }
}
