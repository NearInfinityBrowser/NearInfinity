// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.SpellProtType;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 326.
 */
public class Opcode326 extends BaseOpcode {
  private static final String RES_TYPE = "SPL";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Apply effects list";
      default:
        return null;
    }
  }

  public Opcode326() {
    super(326, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final SpellProtType param2 = new SpellProtType(buffer, offset + 4, 4);
    list.add(param2.createCreatureValueFromType(buffer, offset));
    list.add(param2);
    return RES_TYPE;
  }
}
