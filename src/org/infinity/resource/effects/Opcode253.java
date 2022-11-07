// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.StringRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AutomapNote;

/**
 * Implemention of opcode 253.
 */
public class Opcode253 extends BaseOpcode {
  private static final String EFFECT_AC_VALUE = "AC value";
  private static final String EFFECT_BONUS_TO = "Bonus to";

  private static final String[] WEAPON_TYPES_IWD = { "All weapons", "Blunt weapons", "Missile weapons",
      "Piercing weapons", "Slashing weapons", "Set base AC to value" };

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case BG1:
      case IWD2:
      case PST:
        return null;
      case IWD:
        return "Bonus AC vs. weapons";
      default:
        return "Set automap note";
    }
  }

  public Opcode253() {
    super(253, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsGeneric(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new StringRef(buffer, offset, EFFECT_STRING));
    list.add(new DecNumber(buffer, offset + 4, 4, AbstractStruct.COMMON_UNUSED));
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
    list.add(new StringRef(buffer, offset, EFFECT_STRING));
    list.add(new Bitmap(buffer, offset + 4, 4, EFFECT_COLOR, AutomapNote.FLAGS_ARRAY));
    return null;
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    if (ResourceFactory.resourceExists("MAPNOTES.IDS")) {
      list.add(new StringRef(buffer, offset, EFFECT_STRING));
      list.add(new IdsBitmap(buffer, offset + 4, 4, EFFECT_COLOR, "MAPNOTES.IDS", false));
      return null;
    } else {
      return makeEffectParamsBG2(parent, buffer, offset, list, isVersion1);
    }
  }

  @Override
  protected String makeEffectParamsIWD(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    list.add(new DecNumber(buffer, offset, 4, EFFECT_AC_VALUE));
    list.add(new Flag(buffer, offset + 4, 4, EFFECT_BONUS_TO, WEAPON_TYPES_IWD));
    return null;
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
