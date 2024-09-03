// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 327.
 */
public class Opcode327 extends BaseOpcode {
  private static final String EFFECT_TARGET = "Target";
  private static final String EFFECT_FX     = "Effect";

  private static final String[] TARGETS = { "Spell target", "Target point" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Show visual effect";
      default:
        return null;
    }
  }

  public Opcode327() {
    super(327, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_TARGET, TARGETS));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FX, VISUALS));
    return null;
  }
}
