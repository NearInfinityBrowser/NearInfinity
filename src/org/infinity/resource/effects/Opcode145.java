// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 145.
 */
public class Opcode145 extends BaseOpcode {
  private static final String EFFECT_SPELL_CLASS      = "Spell class";
  private static final String EFFECT_DISPLAY_MESSAGE  = "Display message?";

  private static final String[] SPELL_CLASSES       = { "Wizard", "Priest", "Innate" };
  private static final String[] SPELL_CLASSES_EE    = { "Wizard", "Priest", "Innate", "All, magical only" };
  private static final String[] SPELL_CLASSES_IWD2  = { "All spells", "Non-innate", "Arcane", "Divine", "Innate" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Disable spellcasting";
  }

  public Opcode145() {
    super(145, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SPELL_CLASS, SPELL_CLASSES));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SPELL_CLASS, SPELL_CLASSES_EE));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_SPELL_CLASS, SPELL_CLASSES_IWD2));
    return null;
  }

  @Override
  protected int makeEffectSpecial(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      String resType, int param1, int param2) {
    if (Profile.isEnhancedEdition()) {
      list.add(new Bitmap(buffer, offset, 4, EFFECT_DISPLAY_MESSAGE, AbstractStruct.OPTION_YESNO));
      return offset + 4;
    } else {
      return super.makeEffectSpecial(parent, buffer, offset, list, resType, param1, param2);
    }
  }
}
