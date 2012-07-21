// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;

final class Song extends AbstractStruct // implements AddRemovable
{
  private static final String[] s_reverb = {"None", "Small room", "Medium room",
                                            "Large room", "Outside", "Dungeon"};

  Song(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Songs", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    if (ResourceFactory.getInstance().resourceExists("SONGLIST.2DA")) { // BG2
      list.add(new Song2daBitmap(buffer, offset, 4, "Day song"));
      list.add(new Song2daBitmap(buffer, offset + 4, 4, "Night song"));
      list.add(new Song2daBitmap(buffer, offset + 8, 4, "Victory song"));
      list.add(new Song2daBitmap(buffer, offset + 12, 4, "Battle song"));
      list.add(new Song2daBitmap(buffer, offset + 16, 4, "Defeat song"));
    }
    else if (ResourceFactory.getInstance().resourceExists("MUSICLIS.IDS")) { // IWD
      list.add(new IdsBitmap(buffer, offset, 4, "Day song", "MUSICLIS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 4, 4, "Night song", "MUSICLIS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 8, 4, "Victory song", "MUSICLIS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 12, 4, "Battle song", "MUSICLIS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 16, 4, "Defeat song", "MUSICLIS.IDS"));
    }
    else if (ResourceFactory.getInstance().resourceExists("MUSIC.IDS")) { // IWD2
      list.add(new IdsBitmap(buffer, offset, 4, "Day song", "MUSIC.IDS"));
      list.add(new IdsBitmap(buffer, offset + 4, 4, "Night song", "MUSIC.IDS"));
      list.add(new IdsBitmap(buffer, offset + 8, 4, "Victory song", "MUSIC.IDS"));
      list.add(new IdsBitmap(buffer, offset + 12, 4, "Battle song", "MUSIC.IDS"));
      list.add(new IdsBitmap(buffer, offset + 16, 4, "Defeat song", "MUSIC.IDS"));
    }
    else if (ResourceFactory.getInstance().resourceExists("SONGS.IDS")) { // PST
      list.add(new IdsBitmap(buffer, offset, 4, "Day song", "SONGS.IDS"));
      list.add(new IdsBitmap(buffer, offset + 4, 4, "Night song", "SONGS.IDS"));
      list.add(new Unknown(buffer, offset + 8, 4));
      list.add(new IdsBitmap(buffer, offset + 12, 4, "Battle song", "SONGS.IDS"));
      list.add(new Unknown(buffer, offset + 16, 4));
    }
    else { // BG
      list.add(new DecNumber(buffer, offset, 4, "Day song"));
      list.add(new DecNumber(buffer, offset + 4, 4, "Night song"));
      list.add(new DecNumber(buffer, offset + 8, 4, "Victory song"));
      list.add(new DecNumber(buffer, offset + 12, 4, "Battle song"));
      list.add(new DecNumber(buffer, offset + 16, 4, "Defeat song"));
    }
    list.add(new Unknown(buffer, offset + 20, 4));
    list.add(new Unknown(buffer, offset + 24, 4));
    list.add(new Unknown(buffer, offset + 28, 4));
    list.add(new Unknown(buffer, offset + 32, 4));
    list.add(new Unknown(buffer, offset + 36, 4));
    if (getSuperStruct() != null) {
      list.add(new AreResourceRef(buffer, offset + 40, "Main ambient (day) 1",
                                  (AreResource)getSuperStruct()));
      list.add(new AreResourceRef(buffer, offset + 48, "Main ambient (day) 2",
                                  (AreResource)getSuperStruct()));
      list.add(new DecNumber(buffer, offset + 56, 4, "Main ambient volume (day)"));
      list.add(new AreResourceRef(buffer, offset + 60, "Main ambient (night) 1",
                                  (AreResource)getSuperStruct()));
      list.add(new AreResourceRef(buffer, offset + 68, "Main ambient (night) 2",
                                  (AreResource)getSuperStruct()));
      list.add(new DecNumber(buffer, offset + 76, 4, "Main ambient volume (night)"));
    }
    else {
      list.add(new ResourceRef(buffer, offset + 40, "Main ambient (day) 1", "WAV"));
      list.add(new ResourceRef(buffer, offset + 48, "Main ambient (day) 2", "WAV"));
      list.add(new DecNumber(buffer, offset + 56, 4, "Main ambient volume (day)"));
      list.add(new ResourceRef(buffer, offset + 60, "Main ambient (night) 1", "WAV"));
      list.add(new ResourceRef(buffer, offset + 68, "Main ambient (night) 2", "WAV"));
      list.add(new DecNumber(buffer, offset + 76, 4, "Main ambient volume (night)"));
    }
    if (ResourceFactory.getInstance().resourceExists("REVERB.IDS"))
      list.add(new IdsBitmap(buffer, offset + 80, 4, "Reverb", "REVERB.IDS"));
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      list.add(new Bitmap(buffer, offset + 80, 4, "Reverb", s_reverb));
    else
      list.add(new Unknown(buffer, offset + 80, 4));
    list.add(new Unknown(buffer, offset + 84, 60));
    return offset + 144;
  }
}

