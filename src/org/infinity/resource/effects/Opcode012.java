// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IdsBitmap;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 12.
 */
public class Opcode012 extends BaseOpcode {
  private static final String EFFECT_DAMAGE_TYPE          = "Damage type";
  private static final String EFFECT_FLAGS                = "Flags";
  private static final String EFFECT_SPECIFIC_VISUAL_FOR  = "Specific visual for";

  private static final String[] MODE      = new String[]{"Normal", "Set to value", "Set to %", "Percentage"};
  private static final String[] MODE_BG1  = {"Normal", "Set to value", "Set to %"};
  private static final String[] MODE_IWD2 = {"Normal", "Set to value", "Set to %", "Save for half"};

  private static final String[] SPECIAL_FLAGS_EE = { "Default",
      "Transfer HP to caster (cumulative)*;Bits 0, 1, 3 and 4 are mutually exclusive. Cumulative temporary extra HP.",
      "Transfer HP to target (cumulative)*;Bits 0, 1, 3 and 4 are mutually exclusive. Cumulative temporary extra HP.",
      "Fist damage only",
      "Transfer HP to caster (non-cumulative)*;Bits 0, 1, 3 and 4 are mutually exclusive. Non-cumulative temporary extra HP.",
      "Transfer HP to target (non-cumulative)*;Bits 0, 1, 3 and 4 are mutually exclusive. Non-cumulative temporary extra HP.",
      "Suppress damage feedback",
      "Limit to cur. HP of target minus MINHP*;Bits 1 and 4 switch target -> caster.",
      "Limit to cur./max. HP difference of caster*;Bits 1 and 4 switch caster -> target.",
      "Save for half",
      "Fail for half;A failed (or lack of) saving throw results in half damage",
      "Does not wake sleepers" };

  private static final String[] NPCS_PST = { "None", "The Nameless One", "Annah", "Grace", "Nordom", "Vhailor", "Morte",
      "Dakkon", "Ignus" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Damage";
  }

  public Opcode012() {
    super(12, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_MODE, MODE));
    list.add(new IdsBitmap(buffer, offset + 6, 2, EFFECT_DAMAGE_TYPE, "DAMAGES.IDS"));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_MODE, MODE_BG1));
    list.add(new IdsBitmap(buffer, offset + 6, 2, EFFECT_DAMAGE_TYPE, "DAMAGES.IDS"));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_MODE, MODE_IWD2));
    list.add(new IdsBitmap(buffer, offset + 6, 2, EFFECT_DAMAGE_TYPE, "DAMAGES.IDS"));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    switch (Profile.getEngine()) {
      case EE:
        list.add(new Flag(buffer, offset, 4, EFFECT_FLAGS, SPECIAL_FLAGS_EE));
        break;
      case PST:
        list.add(new Flag(buffer, offset, 4, EFFECT_SPECIFIC_VISUAL_FOR, NPCS_PST));
        break;
      default:
        return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
    return offset + 4;
  }
}
