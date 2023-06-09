// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsTargetType;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 55.
 */
public class Opcode055 extends BaseOpcode {
  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Slay";
  }

  public Opcode055() {
    super(55, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final IdsTargetType param2 = new IdsTargetType(buffer, offset + 4);
    list.add(param2.createIdsValueFromType(buffer));
    list.add(param2);
    return null;
  }
}
