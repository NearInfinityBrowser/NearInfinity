// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
 * Implementation of opcode 330.
 */
public class Opcode330 extends BaseOpcode {
  private static final String EFFECT_DISPLAY_TYPE = "Display type";

  private static final String[] DISPLAY_TYPES = { "String reference", "Cynicism" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Float text";
      default:
        return null;
    }
  }

  public Opcode330() {
    super(330, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new StringRef(buffer, offset, EFFECT_STRING));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DISPLAY_TYPE, DISPLAY_TYPES));
    return null;
  }
}
