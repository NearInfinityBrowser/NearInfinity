// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import org.infinity.datatype.AreResourceRef;
import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.Song2daBitmap;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;

public final class Song extends AbstractStruct // implements AddRemovable
{
  // ARE/Songs-specific field labels
  public static final String ARE_SONGS                      = "Songs";
  public static final String ARE_SONGS_DAY                  = "Day song";
  public static final String ARE_SONGS_NIGHT                = "Night song";
  public static final String ARE_SONGS_VICTORY              = "Victory song";
  public static final String ARE_SONGS_BATTLE               = "Battle song";
  public static final String ARE_SONGS_DEFEAT               = "Defeat song";
  public static final String ARE_SONGS_DAY_ALTERNATE        = "Alternative day song";
  public static final String ARE_SONGS_NIGHT_ALTERNATE      = "Alternative night song";
  public static final String ARE_SONGS_VICTORY_ALTERNATE    = "Alternative victory song";
  public static final String ARE_SONGS_BATTLE_ALTERNATE     = "Alternative battle song";
  public static final String ARE_SONGS_DEFEAT_ALTERNATE     = "Alternative defeat song";
  public static final String ARE_SONGS_AMBIENT_DAY_1        = "Main ambient (day) 1";
  public static final String ARE_SONGS_AMBIENT_DAY_2        = "Main ambient (day) 2";
  public static final String ARE_SONGS_AMBIENT_VOLUME_DAY   = "Main ambient volume (day)";
  public static final String ARE_SONGS_AMBIENT_NIGHT_1      = "Main ambient (night) 1";
  public static final String ARE_SONGS_AMBIENT_NIGHT_2      = "Main ambient (night) 2";
  public static final String ARE_SONGS_AMBIENT_VOLUME_NIGHT = "Main ambient volume (night)";
  public static final String ARE_SONGS_REVERB               = "Reverb";

  public static final String[] s_reverb = {"None", "Small room", "Medium room",
                                           "Large room", "Outside", "Dungeon"};

  Song(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, ARE_SONGS, buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new Song2daBitmap(buffer, offset, 4, ARE_SONGS_DAY));
    addField(new Song2daBitmap(buffer, offset + 4, 4, ARE_SONGS_NIGHT));
    addField(new Song2daBitmap(buffer, offset + 8, 4, ARE_SONGS_VICTORY));
    addField(new Song2daBitmap(buffer, offset + 12, 4, ARE_SONGS_BATTLE));
    addField(new Song2daBitmap(buffer, offset + 16, 4, ARE_SONGS_DEFEAT));
    addField(new Song2daBitmap(buffer, offset + 20, 4, ARE_SONGS_DAY_ALTERNATE));
    addField(new Song2daBitmap(buffer, offset + 24, 4, ARE_SONGS_NIGHT_ALTERNATE));
    addField(new Song2daBitmap(buffer, offset + 28, 4, ARE_SONGS_VICTORY_ALTERNATE));
    addField(new Song2daBitmap(buffer, offset + 32, 4, ARE_SONGS_BATTLE_ALTERNATE));
    addField(new Song2daBitmap(buffer, offset + 36, 4, ARE_SONGS_DEFEAT_ALTERNATE));
    if (getSuperStruct() != null) {
      addField(new AreResourceRef(buffer, offset + 40, ARE_SONGS_AMBIENT_DAY_1,
                                  (AreResource)getSuperStruct()));
      addField(new AreResourceRef(buffer, offset + 48, ARE_SONGS_AMBIENT_DAY_2,
                                  (AreResource)getSuperStruct()));
      addField(new DecNumber(buffer, offset + 56, 4, ARE_SONGS_AMBIENT_VOLUME_DAY));
      addField(new AreResourceRef(buffer, offset + 60, ARE_SONGS_AMBIENT_NIGHT_1,
                                  (AreResource)getSuperStruct()));
      addField(new AreResourceRef(buffer, offset + 68, ARE_SONGS_AMBIENT_NIGHT_2,
                                  (AreResource)getSuperStruct()));
      addField(new DecNumber(buffer, offset + 76, 4, ARE_SONGS_AMBIENT_VOLUME_NIGHT));
    }
    else {
      addField(new ResourceRef(buffer, offset + 40, ARE_SONGS_AMBIENT_DAY_1, "WAV"));
      addField(new ResourceRef(buffer, offset + 48, ARE_SONGS_AMBIENT_DAY_2, "WAV"));
      addField(new DecNumber(buffer, offset + 56, 4, ARE_SONGS_AMBIENT_VOLUME_DAY));
      addField(new ResourceRef(buffer, offset + 60, ARE_SONGS_AMBIENT_NIGHT_1, "WAV"));
      addField(new ResourceRef(buffer, offset + 68, ARE_SONGS_AMBIENT_NIGHT_2, "WAV"));
      addField(new DecNumber(buffer, offset + 76, 4, ARE_SONGS_AMBIENT_VOLUME_NIGHT));
    }
    if (ResourceFactory.resourceExists("REVERB.IDS")) {
      addField(new IdsBitmap(buffer, offset + 80, 4, ARE_SONGS_REVERB, "REVERB.IDS"));
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 80, 4, ARE_SONGS_REVERB, s_reverb));
    } else {
      addField(new Unknown(buffer, offset + 80, 4));
    }
    addField(new Unknown(buffer, offset + 84, 60));
    return offset + 144;
  }
}

