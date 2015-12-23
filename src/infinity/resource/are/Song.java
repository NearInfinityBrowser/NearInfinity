// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.AreResourceRef;
import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.Song2daBitmap;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.Profile;
import infinity.resource.ResourceFactory;

public final class Song extends AbstractStruct // implements AddRemovable
{
  public static final String[] s_reverb = {"None", "Small room", "Medium room",
                                           "Large room", "Outside", "Dungeon"};

  Song(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Songs", buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new Song2daBitmap(buffer, offset, 4, "Day song"));
    addField(new Song2daBitmap(buffer, offset + 4, 4, "Night song"));
    addField(new Song2daBitmap(buffer, offset + 8, 4, "Victory song"));
    addField(new Song2daBitmap(buffer, offset + 12, 4, "Battle song"));
    addField(new Song2daBitmap(buffer, offset + 16, 4, "Defeat song"));
    addField(new Song2daBitmap(buffer, offset + 20, 4, "Alternative day song"));
    addField(new Song2daBitmap(buffer, offset + 24, 4, "Alternative night song"));
    addField(new Song2daBitmap(buffer, offset + 28, 4, "Alternative victory song"));
    addField(new Song2daBitmap(buffer, offset + 32, 4, "Alternative battle song"));
    addField(new Song2daBitmap(buffer, offset + 36, 4, "Alternative defeat song"));
    if (getSuperStruct() != null) {
      addField(new AreResourceRef(buffer, offset + 40, "Main ambient (day) 1",
                                  (AreResource)getSuperStruct()));
      addField(new AreResourceRef(buffer, offset + 48, "Main ambient (day) 2",
                                  (AreResource)getSuperStruct()));
      addField(new DecNumber(buffer, offset + 56, 4, "Main ambient volume (day)"));
      addField(new AreResourceRef(buffer, offset + 60, "Main ambient (night) 1",
                                  (AreResource)getSuperStruct()));
      addField(new AreResourceRef(buffer, offset + 68, "Main ambient (night) 2",
                                  (AreResource)getSuperStruct()));
      addField(new DecNumber(buffer, offset + 76, 4, "Main ambient volume (night)"));
    }
    else {
      addField(new ResourceRef(buffer, offset + 40, "Main ambient (day) 1", "WAV"));
      addField(new ResourceRef(buffer, offset + 48, "Main ambient (day) 2", "WAV"));
      addField(new DecNumber(buffer, offset + 56, 4, "Main ambient volume (day)"));
      addField(new ResourceRef(buffer, offset + 60, "Main ambient (night) 1", "WAV"));
      addField(new ResourceRef(buffer, offset + 68, "Main ambient (night) 2", "WAV"));
      addField(new DecNumber(buffer, offset + 76, 4, "Main ambient volume (night)"));
    }
    if (ResourceFactory.resourceExists("REVERB.IDS")) {
      addField(new IdsBitmap(buffer, offset + 80, 4, "Reverb", "REVERB.IDS"));
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 80, 4, "Reverb", s_reverb));
    } else {
      addField(new Unknown(buffer, offset + 80, 4));
    }
    addField(new Unknown(buffer, offset + 84, 60));
    return offset + 144;
  }
}

