// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 122.
 */
public class Opcode122 extends BaseOpcode {
  private static final String EFFECT_NUM_TO_CREATE = "# to create";

  private static final String RES_TYPE = "ITM";

  private static final String[] TYPES = { "Group", "Slot" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Create inventory item";
  }

  public Opcode122() {
    super(122, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_NUM_TO_CREATE));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_LOCATION));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_TYPE, TYPES));
    return RES_TYPE;
  }
}
