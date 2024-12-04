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
 * Implementation of opcode 15.
 */
public class Opcode015 extends BaseOpcode {
  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Dexterity bonus";
  }

  public Opcode015() {
    super(15, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    int type = buffer.getInt(offset + 4);
    list.add(new DecNumber(buffer, offset, 4, (isVersion1 && type == 3) ? AbstractStruct.COMMON_UNUSED : EFFECT_VALUE));
    Bitmap item = new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE,
        new String[] { INC_TYPES[0], INC_TYPES[1], INC_TYPES[2], "Cat's grace" });
    list.add(item);
    if (parent instanceof UpdateListener) {
      item.addUpdateListener((UpdateListener)parent);
    }
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsEE(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    boolean retVal = false;
    if (struct != null) {
      if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2 ||
          Profile.isEnhancedEdition() || isTobEx()) {
        boolean isVersion1 = (getEntry(struct, EffectEntry.IDX_OPCODE).getSize() == 2);
        int param2 = ((IsNumeric) getEntry(struct, EffectEntry.IDX_PARAM2)).getValue();
        if ((isVersion1 || isTobEx()) && param2 == 3) {
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
              new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, AbstractStruct.COMMON_UNUSED));
        } else {
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
              new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_VALUE));
        }
        retVal = true;
      }
    }
    return retVal;
  }
}
