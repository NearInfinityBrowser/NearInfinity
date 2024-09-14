// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 188.
 */
public class Opcode188 extends BaseOpcode {
  private static final String EFFECT_SPELLS_PER_ROUND = "Spells per round";
  private static final String EFFECT_CLEANSE_AURA     = "Cleanse aura?";
  private static final String EFFECT_FLAGS            = "Flags";

  private static final String RES_TYPE_PST = "BAM";

  private static final String[] FLAGS_PST = { null,
      "Random placement", null, null, null, null, null, null, null,
      null, null, null, null, "Sticky", null, null, null,
      "Repeat;Only effective if bit 17 is set", "Foreground", null, null,
      "Fade out;Duration defined by Dice Size, initial transparency defined by red color value. Effect changes if bit 21 is set.",
      "Blended;Only if bit 20 is also set.", null, null };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case PST:
        return "Play BAM file 1";
      default:
        return "Increase spells cast per round";
    }
  }

  public Opcode188() {
    super(188, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_CLEANSE_AURA, AbstractStruct.OPTION_NOYES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_SPELLS_PER_ROUND));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR, ColorPicker.Format.RGBX));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_FLAGS, FLAGS_PST));
    return RES_TYPE_PST;
  }
}
