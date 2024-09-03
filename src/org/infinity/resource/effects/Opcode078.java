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
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 78.
 */
public class Opcode078 extends BaseOpcode {
  private static final String EFFECT_AMOUNT_PER_SECOND  = "Amount per second";
  private static final String EFFECT_DISEASE_TYPE       = "Disease type";

  private static final String RES_TYPE = "SPL";

  private static final String[] DISEASE_TYPES = { "1 damage per second", "Amount damage per round",
      "Amount damage per second", "1 damage per amount seconds", "Strength", "Dexterity", "Constitution",
      "Intelligence", "Wisdom", "Charisma", "Slow target" };
  private static final String[] DISEASE_TYPES_EE;
  private static final String[] DISEASE_TYPES_IWD;
  private static final String[] DISEASE_TYPES_IWD2;

  static {
    DISEASE_TYPES_EE = Arrays.copyOf(DISEASE_TYPES, DISEASE_TYPES.length + 3);
    DISEASE_TYPES_EE[11] = "Mold touch/Single";
    DISEASE_TYPES_EE[12] = "Mold touch/Decrement";
    DISEASE_TYPES_EE[13] = "Contagion";

    DISEASE_TYPES_IWD = Arrays.copyOf(DISEASE_TYPES, DISEASE_TYPES.length + 1);
    DISEASE_TYPES_IWD[11] = "Mold touch";

    DISEASE_TYPES_IWD2 = Arrays.copyOf(DISEASE_TYPES, DISEASE_TYPES.length + 5);
    DISEASE_TYPES_IWD2[11] = "Mold touch";
    DISEASE_TYPES_IWD2[12] = "";
    DISEASE_TYPES_IWD2[13] = "Contagion";
    DISEASE_TYPES_IWD2[14] = "Cloud of pestilence";
    DISEASE_TYPES_IWD2[15] = "Dolorous decay";
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Disease";
  }

  public Opcode078() {
    super(78, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DISEASE_TYPE, DISEASE_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT_PER_SECOND));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    final Bitmap bmp = new Bitmap(buffer, offset + 4, 4, EFFECT_DISEASE_TYPE, DISEASE_TYPES_EE);
    list.add(bmp);
    if (Profile.isEnhancedEdition() && parent instanceof UpdateListener) {
      bmp.addUpdateListener((UpdateListener)parent);
      if (bmp.getValue() == 11 || bmp.getValue() == 12) {
        return RES_TYPE;
      }
    }
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DISEASE_TYPE, DISEASE_TYPES_IWD));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_DISEASE_TYPE, DISEASE_TYPES_IWD2));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return makeEffectParamsBG1(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      int param2 = ((IsNumeric)getEntry(struct, EffectEntry.IDX_PARAM2)).getValue();
      switch (param2) {
        case 11:  // Mold Touch/Single
        case 12:  // Mold Touch/Decrement
          replaceEntry(struct, EffectEntry.IDX_RESOURCE, EffectEntry.OFS_RESOURCE,
              new ResourceRef(getEntryData(struct, EffectEntry.IDX_RESOURCE), 0,
                  EFFECT_RESOURCE, RES_TYPE));
          break;
        default:
          replaceEntry(struct, EffectEntry.IDX_RESOURCE, EffectEntry.OFS_RESOURCE,
              new Unknown(getEntryData(struct, EffectEntry.IDX_RESOURCE), 0, 8,
                  AbstractStruct.COMMON_UNUSED));
          break;
      }
      return true;
    }
    return false;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_ICON, getPortraitIconNames(STRING_DEFAULT)));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
