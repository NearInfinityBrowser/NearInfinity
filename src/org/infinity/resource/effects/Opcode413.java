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
 * Implementation of opcode 413.
 */
public class Opcode413 extends BaseOpcode {
  private static final String EFFECT_ANIMATION = "Animation";

  private static final String[] ANIMATIONS_IWD2 = { "Sanctuary", "Entangle", "Wisp", "Shield", "Grease", "Web",
      "Minor globe of invulnerability", "Globe of invulnerability", "Shroud of flame", "Antimagic shell",
      "Otiluke's resilient sphere", "Protection from normal missiles", "Cloak of fear", "Entrophy shield", "Fire aura",
      "Frost aura", "Insect plague", "Storm shell", "Shield of lathander", "", "Greater shield of lathander", "",
      "Seven eyes", "", "Blur", "Invisibility", "Fire shield (red)", "Fire shield (blue)", "", "", "Tortoise shell",
      "Death armor" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case IWD2:
        return "Run visual effect";
      default:
        return null;
    }
  }

  public Opcode413() {
    super(413, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, AbstractStruct.COMMON_UNUSED));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_ANIMATION, ANIMATIONS_IWD2));
    return null;
  }
}
