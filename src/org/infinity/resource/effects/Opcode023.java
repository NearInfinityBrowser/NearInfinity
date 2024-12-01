// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 23.
 */
public class Opcode023 extends BaseOpcode {
  private static final String[] MODES_EE = { "BG2 mode", "BG1 mode" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case EE:
      case IWD:
      case IWD2:
      case PST:
        return "Morale bonus";
      default:
        return "Reset morale";
    }
  }

  public Opcode023() {
    super(23, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    int ofsSpecial = offset + (isVersion1 ? 0x28 : 0x2c);
    boolean isBG1Mode = buffer.getInt(ofsSpecial) != 0;
    if (isBG1Mode) {
      return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      int special = ((IsNumeric)getEntry(struct, EffectEntry.IDX_SPECIAL)).getValue();
      if (special == 0 ) {
        // Activate BG2 mode
        replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
            new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, AbstractStruct.COMMON_UNUSED));
        replaceEntry(struct, EffectEntry.IDX_PARAM2, EffectEntry.OFS_PARAM2,
            new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM2), 0, 4, AbstractStruct.COMMON_UNUSED));
      } else {
        // Activate BG1 mode
        replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
            new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_VALUE));
        replaceEntry(struct, EffectEntry.IDX_PARAM2, EffectEntry.OFS_PARAM2,
            new Bitmap(getEntryData(struct, EffectEntry.IDX_PARAM2), 0, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
      }
      return true;
    }
    return false;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      final Bitmap bmp = new Bitmap(buffer, offset, 4, EFFECT_MODE, MODES_EE);
      list.add(bmp);
      if (parent instanceof UpdateListener) {
        bmp.addUpdateListener((UpdateListener)parent);
      }
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
