// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.gui.TextListPanel;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 169.
 */
public class Opcode169 extends BaseOpcode {
  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Prevent portrait icon";
  }

  public Opcode169() {
    super(169, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    final Bitmap bitmap = new Bitmap(buffer, offset + 4, 4, EFFECT_ICON, getPortraitIconNames());
    bitmap.setIconType(TextListPanel.IconType.PORTRAIT);
    list.add(bitmap);
    return null;
  }
}
