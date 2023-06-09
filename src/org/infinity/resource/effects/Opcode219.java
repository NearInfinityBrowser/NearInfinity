// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.infinity.datatype.Datatype;
import org.infinity.datatype.IdsTargetType;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 219.
 */
public class Opcode219 extends BaseOpcode {
  private final String[] idsList   = { "", "", "EA.IDS", "GENERAL.IDS", "RACE.IDS", "CLASS.IDS", "", "GENDER.IDS",
      Profile.getProperty(Profile.Key.GET_IDS_ALIGNMENT) };
  private final String[] idsListEE = Arrays.copyOf(idsList, idsList.length + 1);

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD:
      case IWD2:
      case PST:
        return null;
      default:
        return "Attack and Saving Throw roll penalty";
    }
  }

  public Opcode219() {
    super(219, getOpcodeName());
    idsListEE[idsListEE.length - 1] = "KIT.IDS";
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final IdsTargetType param2 = new IdsTargetType(buffer, offset + 4, IdsTargetType.DEFAULT_NAME_TYPE, idsList);
    list.add(param2.createIdsValueFromType(buffer));
    list.add(param2);
    return null;
  }

  @Override
  protected String makeEffectParamsBG1(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final IdsTargetType param2 = new IdsTargetType(buffer, offset + 4, IdsTargetType.DEFAULT_NAME_TYPE, idsListEE);
    list.add(param2.createIdsValueFromType(buffer));
    list.add(param2);
    return null;
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsIWD2(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }

  @Override
  protected String makeEffectParamsPST(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    return super.makeEffectParamsGeneric(parent, buffer, offset, list, isVersion1);
  }
}
