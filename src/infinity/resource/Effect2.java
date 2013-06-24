// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.*;
import infinity.resource.spl.SplResource;

import java.util.List;

public final class Effect2 extends AbstractStruct implements AddRemovable
{
  private static final String s_itmflag[] = {"No flags set", "Add strength bonus", "Breakable",
                                              "", "", "", "", "", "", "", "", "Hostile",
                                              "Recharge after resting", "", "", "", "",
                                              "Bypass armor", "Keen edge"};
  private static final String s_splflag[] = {"No flags set", "", "", "", "", "", "", "", "", "",
                                             "", "Hostile", "No LOS required", "Allow spotting", "Outdoors only",
                                             "Non-magical ability", "Trigger/Contingency", "Non-combat ability"};

  public static void readCommon(List<StructEntry> list, byte[] buffer, int offset)
  {
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      list.add(new Flag(buffer, offset, 4, "Save type", Effect.s_savetype2));
      list.add(new DecNumber(buffer, offset + 4, 4, "Save penalty"));
      list.add(new DecNumber(buffer, offset + 8, 4, "Parameter?"));
      list.add(new DecNumber(buffer, offset + 12, 4, "Parameter?"));
    }
    else {
      list.add(new DecNumber(buffer, offset, 4, "# dice thrown"));
      list.add(new DecNumber(buffer, offset + 4, 4, "Dice size"));
      list.add(new Flag(buffer, offset + 8 , 4, "Save type", Effect.s_savetype));
      list.add(new DecNumber(buffer, offset + 12, 4, "Save bonus"));
    }
    list.add(new Unknown(buffer, offset + 16, 4));
    if (ResourceFactory.getInstance().resourceExists("SCHOOL.IDS"))
      list.add(new IdsBitmap(buffer, offset + 20, 4, "Primary type (school)", "SCHOOL.IDS"));
    else
      list.add(new Bitmap(buffer, offset + 20, 4, "Primary type (school)", SplResource.s_school));
    list.add(new Unknown(buffer, offset + 24, 4));
    list.add(new DecNumber(buffer, offset + 28, 4, "Minimum level"));
    list.add(new DecNumber(buffer, offset + 32, 4, "Maximum level"));
    list.add(new Bitmap(buffer, offset + 36, 4, "Dispel/Resistance", EffectType.s_dispel));
    list.add(new DecNumber(buffer, offset + 40, 4, "Parameter 3"));
    list.add(new DecNumber(buffer, offset + 44, 4, "Parameter 4"));
    list.add(new Unknown(buffer, offset + 48, 8));
    list.add(new TextString(buffer, offset + 56, 8, "Resource 2"));
    list.add(new TextString(buffer, offset + 64, 8, "Resource 3"));
    list.add(new DecNumber(buffer, offset + 72, 4, "Caster location: X"));
    list.add(new DecNumber(buffer, offset + 76, 4, "Caster location: Y"));
    list.add(new DecNumber(buffer, offset + 80, 4, "Target location: X"));
    list.add(new DecNumber(buffer, offset + 84, 4, "Target location: Y"));
    Bitmap res_type = new Bitmap(buffer, offset + 88, 4, "Resource type",
                                 new String[]{"None", "Spell", "Item"});
    list.add(res_type);
    if (res_type.getValue() == 2) {
      list.add(new ResourceRef(buffer, offset + 92, "Parent resource", "ITM"));
      list.add(new Flag(buffer, offset + 100, 4, "Resource flags", s_itmflag));
    }
    else {
      list.add(new ResourceRef(buffer, offset + 92, "Parent resource", "SPL"));
      list.add(new Flag(buffer, offset + 100, 4, "Resource flags", s_splflag));
    }
    if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
      list.add(new IdsBitmap(buffer, offset + 104, 4, "Impact projectile", "PROJECTL.IDS"));
    else
      list.add(new DecNumber(buffer, offset + 104, 4, "Impact projectile"));
    list.add(new IdsBitmap(buffer, offset + 108, 4, "Source item slot", "SLOTS.IDS"));
    list.add(new TextString(buffer, offset + 112, 32, "Variable name"));
    list.add(new DecNumber(buffer, offset + 144, 4, "Caster level"));
    list.add(new Unknown(buffer, offset + 148, 4));
    list.add(new Bitmap(buffer, offset + 152, 4, "Secondary type", SplResource.s_category));
    list.add(new Unknown(buffer, offset + 156, 4));
    list.add(new Unknown(buffer, offset + 160, 56));
  }

  public Effect2() throws Exception
  {
    super(null, "Effect", new byte[264], 0);
  }

  public Effect2(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Effect", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    EffectType type = new EffectType(buffer, offset + 8, 4);
    list.add(type);
    offset = type.readAttributes(buffer, offset + 12, list);

    readCommon(list, buffer, offset);

    return offset + 216;
  }
}

