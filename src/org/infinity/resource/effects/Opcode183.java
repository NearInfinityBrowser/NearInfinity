// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ItemTypeBitmap;
import org.infinity.datatype.StringRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 183.
 */
public class Opcode183 extends BaseOpcode {
  private static final String EFFECT_ITEM_TYPE = "Item type";

  private static final String RES_TYPE = "EFF";

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    if (Profile.getEngine() == Profile.Engine.PST) {
      return null;
    } else {
      return "Use EFF when item type equipped";
    }
  }

  public Opcode183() {
    super(183, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new ItemTypeBitmap(buffer, offset + 4, 4, EFFECT_ITEM_TYPE));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new StringRef(buffer, offset, EFFECT_STRING));
    list.add(new ItemTypeBitmap(buffer, offset + 4, 4, EFFECT_ITEM_TYPE));
    return RES_TYPE;
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }
}
