// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.EffectBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 337.
 */
public class Opcode337 extends BaseOpcode {
  private static final String EFFECT_MATCH_P2_VALUE = "Match 'Parameter 2' value";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Remove effects by opcode";
      default:
        return null;
    }
  }

  public Opcode337() {
    super(337, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_MATCH_P2_VALUE));
    list.add(new EffectBitmap(buffer, offset + 4, 4));
    return null;
  }
}
