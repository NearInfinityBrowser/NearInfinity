// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Profile;

public final class Animation extends AbstractStruct implements AddRemovable
{
  public static final String[] s_flag =
    {"Not shown", "Is shown", "No shadow", "Not light source", "Partial animation",
     "Synchronized draw", "Random start","Not covered by wall", "Static animation",
     "Draw as background", "Play all frames", "Recolored by palette", "Mirror Y axis",
     "Don't remove in combat"};
  public static final String[] s_flag_ee =
    {"Not shown", "Is shown", "No shadow", "Not light source", "Partial animation",
     "Synchronized draw", "Random start", "Not covered by wall", "Static animation",
     "Draw as background", "Play all frames", "Recolored by palette", "Mirror Y axis",
     "Don't remove in combat", "Use WBM", "Under ground", "Use PVRZ"};

  Animation() throws Exception
  {
    super(null, "Animation", new byte[76], 0);
  }

  Animation(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Animation " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    addField(new Flag(buffer, offset + 36, 4, "Active at", Actor.s_schedule));
    if (Profile.isEnhancedEdition()) {
      addField(new ResourceRef(buffer, offset + 40, "Animation", new String[]{"BAM", "WBM", "PVRZ"}));
    } else {
      addField(new ResourceRef(buffer, offset + 40, "Animation", "BAM"));
    }
    addField(new DecNumber(buffer, offset + 48, 2, "Animation number"));
    addField(new DecNumber(buffer, offset + 50, 2, "Frame number"));
    if (Profile.isEnhancedEdition()) {
      addField(new Flag(buffer, offset + 52, 4, "Appearance", s_flag_ee));
    } else {
      addField(new Flag(buffer, offset + 52, 4, "Appearance", s_flag));
    }
    addField(new DecNumber(buffer, offset + 56, 2, "Location: Z"));
    addField(new DecNumber(buffer, offset + 58, 2, "Translucency"));
    addField(new DecNumber(buffer, offset + 60, 2, "Start range"));
    addField(new DecNumber(buffer, offset + 62, 1, "Loop probability"));
    addField(new DecNumber(buffer, offset + 63, 1, "Start delay (frames)"));
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new ResourceRef(buffer, offset + 64, "Palette", "BMP"));
    } else {
      addField(new Unknown(buffer, offset + 64, 8));
    }
    if (Profile.isEnhancedEdition()) {
      addField(new DecNumber(buffer, offset + 72, 2, "Movie width"));
      addField(new DecNumber(buffer, offset + 74, 2, "Movie height"));
    } else {
      addField(new Unknown(buffer, offset + 72, 4));
    }
    return offset + 76;
  }
}

