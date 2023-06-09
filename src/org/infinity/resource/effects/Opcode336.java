// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 336.
 */
public class Opcode336 extends BaseOpcode {
  private static final String EFFECT_LAST_LETTER = "Last VVC letter";

  private static final String RES_TYPE = "VVC";

  private static final String[] LETTERS = new String['Z' - 'A' + 2];

  static {
    LETTERS[0] = "None";
    for (char ch = 'A'; ch <= 'Z'; ch++) {
      LETTERS[ch - 'A' + 1] = Character.toString(ch);
    }
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Seven eyes overlay";
      default:
        return null;
    }
  }

  public Opcode336() {
    super(336, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_LAST_LETTER, LETTERS));
    list.add(new DecNumber(buffer, offset + 4, 4, EFFECT_TYPE));
    return RES_TYPE;
  }
}
