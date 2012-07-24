// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.*;

final class Animation extends AbstractStruct implements AddRemovable
{
  private static final String[] s_flag = {"Not shown", "Is shown", "No shadow", "Not light source",
                                          "Partial animation", "Synchronized draw", "Random start",
                                          "Not covered by wall", "Static animation",
                                          "Draw as background", "Play all frames", "Recolored by palette",
                                          "Mirror Y axis", "Don't remove in combat"};

  Animation() throws Exception
  {
    super(null, "Animation", new byte[76], 0);
  }

  Animation(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Animation", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    list.add(new Flag(buffer, offset + 36, 4, "Active at", Actor.s_schedule));
    list.add(new ResourceRef(buffer, offset + 40, "Animation", "BAM"));
    list.add(new DecNumber(buffer, offset + 48, 2, "Animation number"));
    list.add(new DecNumber(buffer, offset + 50, 2, "Frame number"));
    list.add(new Flag(buffer, offset + 52, 4, "Appearance", s_flag));
    list.add(new DecNumber(buffer, offset + 56, 2, "Location: Z"));
    list.add(new DecNumber(buffer, offset + 58, 2, "Translucency"));
    list.add(new DecNumber(buffer, offset + 60, 2, "Start range"));
    list.add(new DecNumber(buffer, offset + 62, 1, "Loop probability"));
    list.add(new DecNumber(buffer, offset + 63, 1, "Start delay (frames)"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_TUTU)
      list.add(new ResourceRef(buffer, offset + 64, "Palette", "BMP"));
    else
      list.add(new Unknown(buffer, offset + 64, 8));
    list.add(new Unknown(buffer, offset + 72, 4));
    return offset + 76;
  }
}

