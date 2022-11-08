// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.MultiNumber;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 233.
 */
public class Opcode233 extends BaseOpcode {
  private static final String EFFECT_NUM_STARS    = "# stars";
  private static final String EFFECT_PROFICIENCY  = "Proficiency";
  private static final String EFFECT_BEHAVIOR     = "Behavior";
  private static final String EFFECT_FX           = "Effect";

  private static final String[] CLASS_TYPES = { "Active class", "Original class" };
  private static final String[] BEHAVIORS = { "Set if higher", "Increment" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case PST:
        return null;
      case IWD:
      case IWD2:
        return "Show visual effect";
      default:
        return "Modify proficiencies";
    }
  }

  public Opcode233() {
    super(233, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new MultiNumber(buffer, offset, 4, EFFECT_NUM_STARS, 3, 2, CLASS_TYPES, false));
    list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_PROFICIENCY, "STATS.IDS"));
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsBG2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (isTobEx()) {
      list.add(new MultiNumber(buffer, offset, 4, EFFECT_NUM_STARS, 3, 2, CLASS_TYPES, false));
      list.add(new IdsBitmap(buffer, offset + 4, 2, EFFECT_PROFICIENCY, "STATS.IDS"));
      list.add(new Bitmap(buffer, offset + 6, 2, EFFECT_BEHAVIOR, BEHAVIORS));
      return null;
    } else {
      return makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    int mode = buffer.getShort(offset + 6);
    list.add(new MultiNumber(buffer, offset, 4, EFFECT_NUM_STARS, 3, 2, CLASS_TYPES, mode == 1));
    final String idsFile = (Profile.getGame() == Profile.Game.PSTEE) ? "WPROF.IDS" : "STATS.IDS";
    list.add(new IdsBitmap(buffer, offset + 4, 2, EFFECT_PROFICIENCY, idsFile));
    final Bitmap param2b = new Bitmap(buffer, offset + 6, 2, EFFECT_BEHAVIOR, BEHAVIORS);
    if (parent != null) {
      param2b.addUpdateListener((UpdateListener)parent);
    }
    list.add(param2b);
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_FX, VISUALS));
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

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      boolean signed = ((MultiNumber)getEntry(struct, EffectEntry.IDX_PARAM1)).isSigned();
      boolean increment = ((IsNumeric)getEntry(struct, EffectEntry.IDX_PARAM2B)).getValue() == 1;
      if (signed ^ increment) {
        replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1,
            new MultiNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_NUM_STARS,
                3, 2, CLASS_TYPES, increment));
        return true;
      }
    }
    return false;
  }
}
