// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 195.
 */
public class Opcode195 extends BaseOpcode {
  private static final String EFFECT_HP_AMOUNT  = "HP amount";
  private static final String EFFECT_METHOD     = "Method";

  private static final TreeMap<Long, String> METHODS_MAP_PST = new TreeMap<>();

  static {
    METHODS_MAP_PST.put(0L, "Quick fade light->dark->light");
    METHODS_MAP_PST.put(1L, "Quick fade light->dark->light");
    METHODS_MAP_PST.put(2L, "Quick fade light->dark, instant fade light");
    METHODS_MAP_PST.put(3L, "Quick fade light->dark, instant fade light");
    METHODS_MAP_PST.put(4L, "Fade light->dark->light (duration)");
    METHODS_MAP_PST.put(5L, "Fade light->dark->light (duration)");
    METHODS_MAP_PST.put(6L, "Fade light->dark->light (duration)");
    METHODS_MAP_PST.put(7L, "Fade light->dark->light (duration)");
    METHODS_MAP_PST.put(8L, "No effect");
    METHODS_MAP_PST.put(9L, "Very fast light->black->light");
    METHODS_MAP_PST.put(10L, "Instant black for duration, instant light");
    METHODS_MAP_PST.put(100L, "Unknown");
    METHODS_MAP_PST.put(200L, "Unknown");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD:
      case IWD2:
        return null;
      case PST:
        return "Tint screen";
      default:
        return "Drain CON and HP on death";
    }
  }

  public Opcode195() {
    super(195, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_HP_AMOUNT));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR, ColorPicker.Format.RGBX));
    list.add(new HashBitmap(buffer, offset + 4, 4, EFFECT_METHOD, METHODS_MAP_PST, false));
    return null;
  }
}
