// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HashBitmap;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implementation of opcode 237.
 */
public class Opcode237 extends BaseOpcode {
  private static final String EFFECT_PUPPET_MASTER  = "Puppet master";
  private static final String EFFECT_PUPPET_TYPE    = "Puppet type";

  private static final TreeMap<Long, String> PUPPET_MASTERS = new TreeMap<>();
  private static final String[] PUPPET_TYPES = { "Talkative, uncontrollable", "Mislead", "Project image",
      "Simulacrum" };

  static {
    PUPPET_MASTERS.put(0L, "Player1");
    PUPPET_MASTERS.put(1L, "Player2");
    PUPPET_MASTERS.put(2L, "Player3");
    PUPPET_MASTERS.put(3L, "Player4");
    PUPPET_MASTERS.put(4L, "Player5");
    PUPPET_MASTERS.put(5L, "Player6");
    PUPPET_MASTERS.put(0xffffffffL, "Not party member");
  }

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD2:
      case PST:
        return null;
      case IWD:
        return "Magical stone";
      default:
        return "Set image type";
    }
  }

  public Opcode237() {
    super(237, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new HashBitmap(buffer, offset, 4, EFFECT_PUPPET_MASTER, PUPPET_MASTERS));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_PUPPET_TYPE, PUPPET_TYPES));
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AMOUNT));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
    return null;
  }
}
