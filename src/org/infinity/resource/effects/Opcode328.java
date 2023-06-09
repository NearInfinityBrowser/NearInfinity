// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 328.
 */
public class Opcode328 extends BaseOpcode {
  private static final String EFFECT_STATE = "State";

  private static final String[] MODES_EE = { "IWD mode", "IWD2 mode" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Set spell state";
      default:
        return null;
    }
  }

  public Opcode328() {
    super(328, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    int ofsSpecial = offset + (isVersion1 ? 0x28 : 0x2c);
    int special = buffer.getInt(ofsSpecial);
    if (special == 1 && ResourceFactory.resourceExists("SPLSTATE.IDS")) {
      // Initialize IWD2 mode
      list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_STATE, "SPLSTATE.IDS"));
    } else {
      // Initialize IWD1 mode
      list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_STATE, SPELL_STATES));
    }
    return null;
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      int special = ((IsNumeric)getEntry(struct, EffectEntry.IDX_SPECIAL)).getValue();
      if (special == 1 && ResourceFactory.resourceExists("SPLSTATE.IDS")) {
        // Activate IWD2 mode
        replaceEntry(struct, EffectEntry.IDX_PARAM2, EffectEntry.OFS_PARAM2,
            new IdsBitmap(getEntryData(struct, EffectEntry.IDX_PARAM2), 0, 4, EFFECT_STATE, "SPLSTATE.IDS"));
      } else {
        // Activate IWD1 mode
        replaceEntry(struct, EffectEntry.IDX_PARAM2, EffectEntry.OFS_PARAM2,
            new Bitmap(getEntryData(struct, EffectEntry.IDX_PARAM2), 0, 4, EFFECT_STATE, SPELL_STATES));
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
      if (parent != null && parent instanceof UpdateListener) {
        bmp.addUpdateListener((UpdateListener)parent);
      }
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
