// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.ColorPicker;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.IdsFlag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 232.
 */
public class Opcode232 extends BaseOpcode {
  private static final String EFFECT_TARGET     = "Target";
  private static final String EFFECT_CONDITION  = "Condition";
  private static final String EFFECT_PERIOD     = "Trigger check period";
  private static final String EFFECT_SPEED      = "Speed";

  private static final String RES_TYPE = "SPL";

  private static final String[] TARGETS = { "Myself", "LastHitter", "[EVILCUTOFF]", "[ANYONE]" };

  private static final String[] CONDITIONS = {
      "HitBy([ANYONE]) / instant",
      "See([EVILCUTOFF]) / per round",
      "HPPercentLT(Myself,50) / per round",
      "HPPercentLT(Myself,25) / per round",
      "HPPercentLT(Myself,10) / per round",
      "StateCheck(Myself,STATE_HELPLESS) / per round",
      "StateCheck(Myself,STATE_POISONED) / per round",
      "AttackedBy([ANYONE]) / instant",
      "PersonalSpaceDistance([ANYONE],4) / per round",
      "PersonalSpaceDistance([ANYONE],10) / per round",
      "-Unknown- / per round",
      "TookDamage() / instant" };

  // temporary object to help creating EE string array
  private static final String[] NEW_CONDITIONS_EE = {
      "Killed([ANYONE]) / instant",
      "TimeOfDay('Special') / per round",
      "PersonalSpaceDistance([ANYONE],'Special') / per round",
      "StateCheck(Myself,'Special') / per round",
      "Die() / instant",
      "Died([ANYONE]) / instant",
      "TurnedBy([ANYONE]) / instant",
      "HPLT(Myself,'Special') / per round",
      "HPPercentLT(Myself,'Special') / per round",
      "CheckSpellState(Myself,'Special') / per round" };
  private static final String[] CONDITIONS_EE = Arrays.copyOf(CONDITIONS, CONDITIONS.length + NEW_CONDITIONS_EE.length);

  static {
    CONDITIONS_EE[10] = "Delay('Special') / per round";
    System.arraycopy(NEW_CONDITIONS_EE, 0, CONDITIONS_EE, CONDITIONS.length, NEW_CONDITIONS_EE.length);
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      case IWD:
      case IWD2:
        return "Creature RGB color fade";
      default:
        return "Cast spell on condition";
    }
  }

  public Opcode232() {
    super(232, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_TARGET, TARGETS));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_CONDITION, CONDITIONS));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_TARGET, TARGETS));
      list.add(new Bitmap(buffer, offset + 4, 2, EFFECT_CONDITION, CONDITIONS));
      list.add(new DecNumber(buffer, offset + 6, 2, EFFECT_PERIOD));
      return RES_TYPE;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_TARGET, TARGETS));
    Bitmap item = new Bitmap(buffer, offset + 4, 4, EFFECT_CONDITION, CONDITIONS_EE);
    list.add(item);
    if (parent instanceof UpdateListener) {
      item.addUpdateListener((UpdateListener)parent);
    }
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new HashBitmap(buffer, offset + 4, 2, EFFECT_LOCATION, COLOR_LOCATIONS_MAP, false));
    list.add(new DecNumber(buffer, offset + 6, 2, EFFECT_SPEED));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ColorPicker(buffer, offset, EFFECT_COLOR));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      int param2 = ((IsNumeric)getEntry(struct, EffectEntry.IDX_PARAM2)).getValue();
      switch (param2) {
        case 13: // Time of day
          replaceEntry(struct, EffectEntry.IDX_SPECIAL, EffectEntry.OFS_SPECIAL,
              new IdsBitmap(getEntryData(struct, EffectEntry.IDX_SPECIAL), 0, 4,
                  EFFECT_SPECIAL, "TIMEODAY.IDS"));
          break;
        case 15: // State
          replaceEntry(struct, EffectEntry.IDX_SPECIAL, EffectEntry.OFS_SPECIAL,
              new IdsFlag(getEntryData(struct, EffectEntry.IDX_SPECIAL), 0, 4,
                  EFFECT_SPECIAL, "STATE.IDS"));
          break;
        case 21:
          replaceEntry(struct, EffectEntry.IDX_SPECIAL, EffectEntry.OFS_SPECIAL,
              new IdsBitmap(getEntryData(struct, EffectEntry.IDX_SPECIAL), 0, 4,
                  EFFECT_SPECIAL, "SPLSTATE.IDS"));
          break;
        default:
          replaceEntry(struct, EffectEntry.IDX_SPECIAL, EffectEntry.OFS_SPECIAL,
              new DecNumber(getEntryData(struct, EffectEntry.IDX_SPECIAL), 0, 4, EFFECT_SPECIAL));
      }
      return true;
    }
    return false;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      switch (param2) {
        case 13: // Time of day
          list.add(new IdsBitmap(buffer, offset, 4, EFFECT_SPECIAL, "TIMEODAY.IDS"));
          break;
        case 15: // State check
          list.add(new IdsFlag(buffer, offset, 4, EFFECT_SPECIAL, "STATE.IDS"));
          break;
        case 21:  // Spell State check
          list.add(new IdsBitmap(buffer, offset, 4, EFFECT_SPECIAL, "SPLSTATE.IDS"));
          break;
        default:
          list.add(new DecNumber(buffer, offset, 4, EFFECT_SPECIAL));
      }
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
