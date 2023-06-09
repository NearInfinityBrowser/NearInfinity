// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 353.
 */
public class Opcode353 extends BaseOpcode {
  private static final String EFFECT_METHOD = "Method";

  private static final TreeMap<Long, String> METHODS_MAP = new TreeMap<>();

  // TODO: verify list of methods
  static {
    METHODS_MAP.put(0L, "No effect");
    METHODS_MAP.put(1L, "No effect");
    METHODS_MAP.put(2L, "No effect");
    METHODS_MAP.put(3L, "No effect");
    METHODS_MAP.put(4L, "Instant color (duration)");
    METHODS_MAP.put(5L, "Instant color (duration)");
    METHODS_MAP.put(6L, "Instant color (duration)");
    METHODS_MAP.put(7L, "Instant color (duration)");
    METHODS_MAP.put(8L, "No effect");
    METHODS_MAP.put(9L, "Instant inverted color (duration)");
    METHODS_MAP.put(10L, "Instant black (duration)");
    METHODS_MAP.put(101L, "Instant color (duration)");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        if (Profile.getGame() == Profile.Game.PSTEE) {
          return "Tint screen";
        }
      default:
        return null;
    }
  }

  public Opcode353() {
    super(353, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (Profile.getGame() == Profile.Game.PSTEE) {
      list.add(new ColorPicker(buffer, offset, EFFECT_COLOR, ColorPicker.Format.RGBX));
      list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_METHOD, METHODS_MAP, false));
      return null;
    } else {
      return super.makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    }
  }
}
