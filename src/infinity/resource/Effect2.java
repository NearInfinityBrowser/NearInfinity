// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.EffectType;
import infinity.datatype.Flag;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.spl.SplResource;

import java.util.ArrayList;
import java.util.List;

public final class Effect2 extends AbstractStruct implements AddRemovable
{
  public static final String s_itmflag[] = {"No flags set", "Add strength bonus", "Breakable",
                                             "", "", "", "", "", "", "", "", "Hostile",
                                             "Recharge after resting", "", "", "", "",
                                             "Bypass armor", "Keen edge"};
  public static final String s_splflag[] = {"No flags set", "", "", "", "", "", "", "", "", "",
                                            "", "Hostile", "No LOS required", "Allow spotting", "Outdoors only",
                                            "Non-magical ability", "Trigger/Contingency", "Non-combat ability"};

  public static int readCommon(List<StructEntry> list, byte[] buffer, int offset)
  {
    if (ResourceFactory.getInstance().resourceExists("SCHOOL.IDS"))
      list.add(new IdsBitmap(buffer, offset, 4, "Primary type (school)", "SCHOOL.IDS"));
    else
      list.add(new Bitmap(buffer, offset, 4, "Primary type (school)", SplResource.s_school));
    list.add(new Unknown(buffer, offset + 4, 4));
    list.add(new DecNumber(buffer, offset + 8, 4, "Minimum level"));
    list.add(new DecNumber(buffer, offset + 12, 4, "Maximum level"));
    list.add(new Bitmap(buffer, offset + 16, 4, "Dispel/Resistance", EffectType.s_dispel));
    list.add(new DecNumber(buffer, offset + 20, 4, "Parameter 3"));
    list.add(new DecNumber(buffer, offset + 24, 4, "Parameter 4"));
    list.add(new Unknown(buffer, offset + 28, 8));
    list.add(new TextString(buffer, offset + 36, 8, "Resource 2"));
    list.add(new TextString(buffer, offset + 44, 8, "Resource 3"));
    list.add(new DecNumber(buffer, offset + 52, 4, "Caster location: X"));
    list.add(new DecNumber(buffer, offset + 56, 4, "Caster location: Y"));
    list.add(new DecNumber(buffer, offset + 60, 4, "Target location: X"));
    list.add(new DecNumber(buffer, offset + 64, 4, "Target location: Y"));
    Bitmap res_type = new Bitmap(buffer, offset + 68, 4, "Resource type",
                                 new String[]{"None", "Spell", "Item"});
    list.add(res_type);
    if (res_type.getValue() == 2) {
      list.add(new ResourceRef(buffer, offset + 72, "Parent resource", "ITM"));
      list.add(new Flag(buffer, offset + 80, 4, "Resource flags", s_itmflag));
    }
    else {
      list.add(new ResourceRef(buffer, offset + 72, "Parent resource", "SPL"));
      list.add(new Flag(buffer, offset + 80, 4, "Resource flags", s_splflag));
    }
    if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
      list.add(new IdsBitmap(buffer, offset + 84, 4, "Impact projectile", "PROJECTL.IDS"));
    else
      list.add(new DecNumber(buffer, offset + 84, 4, "Impact projectile"));
    list.add(new IdsBitmap(buffer, offset + 88, 4, "Source item slot", "SLOTS.IDS"));
    list.add(new TextString(buffer, offset + 92, 32, "Variable name"));
    list.add(new DecNumber(buffer, offset + 124, 4, "Caster level"));
    list.add(new Unknown(buffer, offset + 128, 4));
    list.add(new Bitmap(buffer, offset + 132, 4, "Secondary type", SplResource.s_category));
    list.add(new Unknown(buffer, offset + 136, 4));
    list.add(new Unknown(buffer, offset + 140, 56));
    return offset + 196;
  }

  public Effect2() throws Exception
  {
    super(null, "Effect", new byte[264], 0);
  }

  public Effect2(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Effect " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 4, "Signature"));
    addField(new TextString(buffer, offset + 4, 4, "Version"));
    EffectType type = new EffectType(buffer, offset + 8, 4);
    addField(type);
    List<StructEntry> list = new ArrayList<StructEntry>();
    offset = type.readAttributes(buffer, offset + 12, list);
    addToList(getList().size() - 1, list);

    offset = readCommon(getList(), buffer, offset);

    return offset;
  }
}

