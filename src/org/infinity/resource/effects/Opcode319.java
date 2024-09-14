// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsTargetType;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.SpellProtType;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 319.
 */
public class Opcode319 extends BaseOpcode {
  private static final String EFFECT_MODE_EEEX  = "EEex: Mode";
  private static final String EFFECT_DESC_NOTE  = "Description note";

  private static final String[] MODES_EEEX = { "Not usable by", "Usable by", "EEex: Usable by (splprot)",
      "EEex: Not usable by (splprot)" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Restrict item";
      default:
        return null;
    }
  }

  public Opcode319() {
    super(319, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isEEEx()) {
      final Bitmap power = new Bitmap(buffer, offset - 1, 1, EFFECT_MODE_EEEX, MODES_EEEX);
      power.addUpdateListener((UpdateListener)parent);
      list.set(1, power);
    }

    String resType = null;
    byte power = buffer.get(offset - 1);
    if (isEEEx() && (power == 2 || power == 3)) {
      final SpellProtType param2 = new SpellProtType(buffer, offset + 4, 4);
      param2.setName("EEex: " + param2.getName());
      final StructEntry param1 = param2.createCreatureValueFromType(buffer);
      param1.setName("EEex: " + param1.getName());
      list.add(param1);
      list.add(param2);
    } else {
      final IdsTargetType param2 = new IdsTargetType(buffer, offset + 4, 4,
          IdsTargetType.DEFAULT_NAME_TYPE, -1, IdsTargetType.DEFAULT_SECOND_IDS, true);
      param2.addUpdateListener((UpdateListener)parent);
      list.add(param2.createIdsValueFromType(buffer));
      list.add(param2);
      if (param2.getValue() == 11) {
        resType = EFFECT_STRING;
      }
    }

    return resType;
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    boolean retVal = false;
    if (struct != null && Profile.isEnhancedEdition()) {
      if (isEEEx()) {
        int power = ((IsNumeric)getEntry(struct, EffectEntry.IDX_POWER)).getValue();
        if (power == 2 || power == 3) {
          final SpellProtType param2 = new SpellProtType(getEntryData(struct, EffectEntry.IDX_PARAM2), 0, 4);
          param2.setName("EEex: " + param2.getName());
          final StructEntry param1 = param2.createCreatureValueFromType(getEntryData(struct, EffectEntry.IDX_PARAM1), 0);
          param1.setName("EEex: " + param1.getName());
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1, param1);
          replaceEntry(struct, EffectEntry.IDX_PARAM2, EffectEntry.OFS_PARAM2, param2);
          retVal = true;
        }
        else {
          final IdsTargetType param2 = new IdsTargetType(getEntryData(struct, EffectEntry.IDX_PARAM2), 0, 4,
              IdsTargetType.DEFAULT_NAME_TYPE, -1, IdsTargetType.DEFAULT_SECOND_IDS, true);
          param2.addUpdateListener((UpdateListener)struct.getField(0));
          replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
              param2.createIdsValueFromType(getEntryData(struct, EffectEntry.IDX_PARAM1), 0));
          replaceEntry(struct, EffectEntry.IDX_PARAM2, EffectEntry.OFS_PARAM2, param2);
          retVal = true;
        }
      }
      // updating resource field
      final StructEntry entry = getEntry(struct, EffectEntry.IDX_PARAM2);
      if (entry instanceof IdsTargetType) {
        StructEntry resourceEntry =
            ((IdsTargetType)entry).createResourceFromType(getEntryData(struct, EffectEntry.IDX_RESOURCE), 0);
        replaceEntry(struct, EffectEntry.IDX_RESOURCE, EffectEntry.OFS_RESOURCE, resourceEntry);
        retVal = true;
      }
    }
    return retVal;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new StringRef(buffer, offset, EFFECT_DESC_NOTE));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
