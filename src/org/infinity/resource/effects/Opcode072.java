// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsTargetType;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 72.
 */
public class Opcode072 extends BaseOpcode {
  private static final String[] IDS_TYPES = { "EA.IDS", "GENERAL.IDS", "RACE.IDS", "CLASS.IDS", "SPECIFIC.IDS",
      "GENDER.IDS", null };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    return "Change AI type";
  }

  public Opcode072() {
    super(72, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    IDS_TYPES[6] = Profile.getProperty(Profile.Key.GET_IDS_ALIGNMENT);
    final IdsTargetType param2 = new IdsTargetType(buffer, offset + 4, IdsTargetType.DEFAULT_NAME_TYPE, IDS_TYPES);
    list.add(param2.createIdsValueFromType(buffer));
    list.add(param2);
    return null;
  }
}
