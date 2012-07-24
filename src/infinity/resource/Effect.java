// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.*;

public final class Effect extends AbstractStruct implements AddRemovable
{
  static final String s_savetype[] = {"No save", "Spell", "Breath weapon",
                                      "Paralyze/Poison/Death", "Rod/Staff/Wand",
                                      "Petrify/Polymorph", "", "", "",
                                      "", "", "", "", "", "", "", "",
                                      "", "", "", "", "", "", "", "",
                                      "Ex: bypass mirror image"};
  static final String s_savetype2[] = {"No save", "", "", "Fortitude", "Reflex",
                                       "Will"};

  public Effect() throws Exception
  {
    super(null, "Effect", new byte[48], 0);
  }

  public Effect(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Effect", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    EffectType type = new EffectType(buffer, offset, 2);
    list.add(type);
    offset = type.readAttributes(buffer, offset + 2, list);
    list.add(new DecNumber(buffer, offset, 4, "# dice thrown/maximum level"));
    list.add(new DecNumber(buffer, offset + 4, 4, "Dice size/minimum level"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      list.add(new Flag(buffer, offset + 8, 4, "Save type", s_savetype2));
      list.add(new DecNumber(buffer, offset + 12, 4, "Save penalty"));
    }
    else {
      list.add(new Flag(buffer, offset + 8, 4, "Save type", s_savetype));
      list.add(new DecNumber(buffer, offset + 12, 4, "Save bonus"));
    }
    list.add(new Unknown(buffer, offset + 16, 4));
    return offset + 20;
  }
}

