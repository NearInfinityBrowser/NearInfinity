// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 282.
 */
public class Opcode282 extends BaseOpcode {
  private static final String EFFECT_STATE = "State";

  private static final String[] STATES = { "Scripting State 1", "Scripting State 2", "Scripting State 3",
      "Scripting State 4", "Scripting State 5", "Scripting State 6", "Scripting State 7", "Scripting State 8",
      "Scripting State 9", "Scripting State 10", "Melee THAC0 Bonus", "Melee Damage Bonus", "Missile Damage Bonus",
      "Disable Circle", "Fist THAC0 Bonus", "Fist Damage Bonus", "Class String Override Mixed",
      "Class String Override Lower", "Prevent Spell Protection Effects", "Immunity to Backstab", "Lockpicking Bonus",
      "Move Silently Bonus", "Find Traps Bonus", "Pickpocket Bonus", "Hide in Shadows Bonus", "Detect Illusions Bonus",
      "Set Traps Bonus", "Prevent AI Slowdown", "Existance Delay Override", "Animation-only Haste",
      "No Permanent Death", "Immune to Turn Undead", "Chaos Shield", "NPC Bump", "Use Any Item", "Assassinate",
      "Sex Changed", "Spell Failure Innate", "Immune to Tracking", "Dead Magic", "Immune to Timestop",
      "Immune to Sequester", "Stoneskins Golem", "Level Drain", "Do Not Draw" };
  private static final String[] STATES_EE = Arrays.copyOf(STATES, STATES.length);

  static {
    STATES_EE[0] += " / Wing Buffet";
    STATES_EE[1] += " / Death Ward";
    STATES_EE[2] += " / Level Drain Immunity";
    STATES_EE[3] += " / Offensive Modifier";
    STATES_EE[4] += " / Defensive Modifier";
    STATES_EE[5] += " / Defensive Modifier";
    STATES_EE[6] += " / Wizard Spell Immunity";
    STATES_EE[7] += " / Wizard Protection from Energy";
    STATES_EE[8] += " / Wizard Spell Trap";
    STATES_EE[9] += " / Wizard Improved Alacrity";

    for (int i = 10; i < STATES.length; i++) {
      STATES[i] += " [undocumented]";
      STATES_EE[i] += " [undocumented]";
    }
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      case IWD:
      case IWD2:
        return "Hide hit points";
      default:
        return "Modify script state";
    }
  }

  public Opcode282() {
    super(282, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_STATE, STATES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_STATE, STATES_EE));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsIWD(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }
}
