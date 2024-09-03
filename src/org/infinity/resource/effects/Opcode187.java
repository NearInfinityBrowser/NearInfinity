// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 187.
 */
public class Opcode187 extends BaseOpcode {
  private static final String EFFECT_METHOD = "Method";

  private static final String RES_TYPE = "BAM";

  private static final String[] METHOD_TYPES_PST = { "Default", "Repeat animation", "Remove stickiness" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    if (Profile.getEngine() == Profile.Engine.PST) {
      return "Play BAM file (single/dual)";
    } else {
      return "Set local variable";
    }
  }

  public Opcode187() {
    super(187, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR, ColorPicker.Format.RGBX));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_METHOD, METHOD_TYPES_PST));
    return RES_TYPE;
  }
}
