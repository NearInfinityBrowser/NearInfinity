// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.StringRef;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 338.
 */
public class Opcode338 extends BaseOpcode {
  private static final String EFFECT_MESSAGE = "Message";

  private static final String[] MODES = { "Cannot rest", "Cannot save", "Cannot rest or save" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Disable rest or save";
      default:
        return null;
    }
  }

  public Opcode338() {
    super(338, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new StringRef(buffer, offset, EFFECT_MESSAGE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODE, MODES));
    return null;
  }
}
