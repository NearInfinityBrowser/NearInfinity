// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.EffectType;
import infinity.datatype.Flag;
import infinity.datatype.IdsBitmap;
import infinity.datatype.PriTypeBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.SecTypeBitmap;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;

import java.util.ArrayList;
import java.util.List;

public final class Effect2 extends AbstractStruct implements AddRemovable
{
  // Effect-specific field labels
  public static final String EFFECT                   = Effect.EFFECT;
  public static final String EFFECT_PRIMARY_TYPE      = "Primary type (school)";
  public static final String EFFECT_MIN_LEVEL         = "Minimum level";
  public static final String EFFECT_MAX_LEVEL         = "Maximum level";
  public static final String EFFECT_DISPEL_TYPE       = "Dispel/Resistance";
  public static final String EFFECT_PARAMETER_3       = "Parameter 3";
  public static final String EFFECT_PARAMETER_4       = "Parameter 4";
  public static final String EFFECT_RESOURCE_2        = "Resource 2";
  public static final String EFFECT_RESOURCE_3        = "Resource 3";
  public static final String EFFECT_CASTER_LOCATION_X = "Caster location: X";
  public static final String EFFECT_CASTER_LOCATION_Y = "Caster location: Y";
  public static final String EFFECT_TARGET_LOCATION_X = "Target location: X";
  public static final String EFFECT_TARGET_LOCATION_Y = "Target location: Y";
  public static final String EFFECT_RESOURCE_TYPE     = "Resource type";
  public static final String EFFECT_PARENT_RESOURCE   = "Parent resource";
  public static final String EFFECT_RESOURCE_FLAGS    = "Resource flags";
  public static final String EFFECT_IMPACT_PROJECTILE = "Impact projectile";
  public static final String EFFECT_SOURCE_ITEM_SLOT  = "Source item slot";
  public static final String EFFECT_VARIABLE_NAME     = "Variable name";
  public static final String EFFECT_CASTER_LEVEL      = "Caster level";
  public static final String EFFECT_INTERNAL_FLAGS    = "Internal flags";
  public static final String EFFECT_SECONDARY_TYPE    = "Secondary type";

  public static final String[] s_itmflag = {"No flags set", "Add strength bonus", "Breakable",
                                             "", "", "", "", "", "", "", "", "Hostile",
                                             "Recharge after resting", "", "", "", "",
                                             "Bypass armor", "Keen edge"};
  public static final String[] s_splflag = {"No flags set", "", "", "", "", "", "", "", "", "",
                                            "", "Hostile", "No LOS required", "Allow spotting", "Outdoors only",
                                            "Non-magical ability", "Trigger/Contingency", "Non-combat ability"};
  public static final String[] s_restype = {"None", "Spell", "Item"};

  public static int readCommon(List<StructEntry> list, byte[] buffer, int offset)
  {
    list.add(new PriTypeBitmap(buffer, offset, 4, EFFECT_PRIMARY_TYPE));
    list.add(new Unknown(buffer, offset + 4, 4));
    list.add(new DecNumber(buffer, offset + 8, 4, EFFECT_MIN_LEVEL));
    list.add(new DecNumber(buffer, offset + 12, 4, EFFECT_MAX_LEVEL));
    list.add(new Bitmap(buffer, offset + 16, 4, EFFECT_DISPEL_TYPE, EffectType.s_dispel));
    list.add(new DecNumber(buffer, offset + 20, 4, EFFECT_PARAMETER_3));
    list.add(new DecNumber(buffer, offset + 24, 4, EFFECT_PARAMETER_4));
    list.add(new Unknown(buffer, offset + 28, 8));
    list.add(new TextString(buffer, offset + 36, 8, EFFECT_RESOURCE_2));
    list.add(new TextString(buffer, offset + 44, 8, EFFECT_RESOURCE_3));
    list.add(new DecNumber(buffer, offset + 52, 4, EFFECT_CASTER_LOCATION_X));
    list.add(new DecNumber(buffer, offset + 56, 4, EFFECT_CASTER_LOCATION_Y));
    list.add(new DecNumber(buffer, offset + 60, 4, EFFECT_TARGET_LOCATION_X));
    list.add(new DecNumber(buffer, offset + 64, 4, EFFECT_TARGET_LOCATION_Y));
    Bitmap res_type = new Bitmap(buffer, offset + 68, 4, EFFECT_RESOURCE_TYPE, s_restype);
    list.add(res_type);
    if (res_type.getValue() == 2) {
      list.add(new ResourceRef(buffer, offset + 72, EFFECT_PARENT_RESOURCE, "ITM"));
      list.add(new Flag(buffer, offset + 80, 4, EFFECT_RESOURCE_FLAGS, s_itmflag));
    } else {
      list.add(new ResourceRef(buffer, offset + 72, EFFECT_PARENT_RESOURCE, "SPL"));
      list.add(new Flag(buffer, offset + 80, 4, EFFECT_RESOURCE_FLAGS, s_splflag));
    }
    if (ResourceFactory.resourceExists("PROJECTL.IDS")) {
      list.add(new IdsBitmap(buffer, offset + 84, 4, EFFECT_IMPACT_PROJECTILE, "PROJECTL.IDS"));
    } else {
      list.add(new DecNumber(buffer, offset + 84, 4, EFFECT_IMPACT_PROJECTILE));
    }
    list.add(new IdsBitmap(buffer, offset + 88, 4, EFFECT_SOURCE_ITEM_SLOT, "SLOTS.IDS"));
    list.add(new TextString(buffer, offset + 92, 32, EFFECT_VARIABLE_NAME));
    list.add(new DecNumber(buffer, offset + 124, 4, EFFECT_CASTER_LEVEL));
    list.add(new Flag(buffer, offset + 128, 4, EFFECT_INTERNAL_FLAGS, null));
    list.add(new SecTypeBitmap(buffer, offset + 132, 4, EFFECT_SECONDARY_TYPE));
    list.add(new Unknown(buffer, offset + 136, 4));
    list.add(new Unknown(buffer, offset + 140, 56));
    return offset + 196;
  }

  public Effect2() throws Exception
  {
    super(null, EFFECT, new byte[264], 0);
  }

  public Effect2(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, EFFECT + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    EffectType type = new EffectType(buffer, offset + 8, 4);
    addField(type);
    List<StructEntry> list = new ArrayList<StructEntry>();
    offset = type.readAttributes(buffer, offset + 12, list);
    addToList(getList().size() - 1, list);

    list.clear();
    offset = readCommon(list, buffer, offset);
    addToList(getList().size() - 1, list);

    return offset;
  }
}

