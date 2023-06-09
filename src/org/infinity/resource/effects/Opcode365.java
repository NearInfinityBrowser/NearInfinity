// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 365.
 */
public class Opcode365 extends BaseOpcode {
  private static final String EFFECT_DISABLE_DIALOG       = "Disable dialogue?";
  private static final String EFFECT_DISABLE_AI           = "Disable AI?";
  private static final String EFFECT_USE_PURPLE_SELECTION = "Use purple selection color?";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Make unselectable";
      default:
        return null;
    }
  }

  public Opcode365() {
    super(365, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_DISABLE_DIALOG, AbstractStruct.OPTION_YESNO));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DISABLE_AI, AbstractStruct.OPTION_YESNO));
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_USE_PURPLE_SELECTION, AbstractStruct.OPTION_YESNO));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
