// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.ItemTypeBitmap;
import org.infinity.datatype.StringRef;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 181.
 */
public class Opcode181 extends BaseOpcode {
  private static final String EFFECT_DESC_NOTE    = "Description note";
  private static final String EFFECT_ITEM_TYPE    = "Item type";
  private static final String EFFECT_RESTRICTION  = "Restriction";

  private static final String[] RESTRICTION_TYPES_EE = { "Equip", "Use" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    if (Profile.getEngine() == Profile.Engine.PST) {
      return null;
    } else {
      return "Disallow item type";
    }
  }

  public Opcode181() {
    super(181, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new StringRef(buffer, offset, EFFECT_DESC_NOTE));
    list.add(new ItemTypeBitmap(buffer, offset + 4, 4, EFFECT_ITEM_TYPE));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new ItemTypeBitmap(buffer, offset, 4, EFFECT_ITEM_TYPE));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_RESTRICTION, RESTRICTION_TYPES_EE));
    return null;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
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
