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
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 1.
 */
public class Opcode001 extends BaseOpcode {
  private static final String[] ATTACKS_IWD = Arrays.copyOf(ATTACKS, 6);
  private static final String[] INC_TYPES_EE = { INC_TYPES[0], INC_TYPES[1], INC_TYPES[2], "Set final" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Modify attacks per round";
  }

  public Opcode001() {
    super(1, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_VALUE, ATTACKS));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    int type = buffer.getInt(offset + 4);
    switch (type) {
      case 0: // Increment
      {
        final HashBitmap bmp = new HashBitmap(buffer, offset, 4, EFFECT_VALUE, ATTACKS_EE_MAP, false, true);
        bmp.setFormatter(bmp.formatterBitmap);
        list.add(bmp);
        break;
      }
      case 2: // Set % of
        list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
        break;
      default:
        list.add(new Bitmap(buffer, offset, 4, EFFECT_VALUE, ATTACKS));
    }

    Bitmap item = new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES_EE);
    if (parent != null && parent instanceof UpdateListener) {
      item.addUpdateListener((UpdateListener)parent);
    }
    list.add(item);

    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new Bitmap(buffer, offset, 4, EFFECT_VALUE, ATTACKS_IWD));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_MODIFIER_TYPE, INC_TYPES));
    return null;
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      int param2 = ((IsNumeric) getEntry(struct, EffectEntry.IDX_PARAM2)).getValue();
      switch (param2) {
        case 0: // Increment
        {
          final HashBitmap bmp = new HashBitmap(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_VALUE,
              ATTACKS_EE_MAP, false, true);
          bmp.setFormatter(bmp.formatterBitmap);
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1, bmp);
          break;
        }
        case 2: // Set % of
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
              new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_VALUE));
          break;
        default:
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
              new Bitmap(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_VALUE, ATTACKS));
      }
      return true;
    }
    return false;
  }
}
