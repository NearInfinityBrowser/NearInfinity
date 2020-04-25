// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.SpawnResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.util.io.StreamUtils;

public final class SpawnPoint extends AbstractStruct implements AddRemovable
{
  // ARE/Spawn Point-specific field labels
  public static final String ARE_SPAWN                      = "Spawn point";
  public static final String ARE_SPAWN_NAME                 = "Name";
  public static final String ARE_SPAWN_LOCATION_X           = "Location: X";
  public static final String ARE_SPAWN_LOCATION_Y           = "Location: Y";
  public static final String ARE_SPAWN_CREATURE_FMT         = "Creature %d";
  public static final String ARE_SPAWN_NUM_CREATURES        = "# creatures";
  public static final String ARE_SPAWN_ENCOUNTER_DIFFICULTY = "Encounter difficulty";
  public static final String ARE_SPAWN_RATE                 = "Spawn rate";
  public static final String ARE_SPAWN_METHOD               = "Spawn method";
  public static final String ARE_SPAWN_DURATION             = "Creature duration";
  public static final String ARE_SPAWN_WANDER_DISTANCE      = "Creature wander distance";
  public static final String ARE_SPAWN_FOLLOW_DISTANCE      = "Creature follow distance";
  public static final String ARE_SPAWN_MAX_CREATURES        = "Maximum spawned creatures";
  public static final String ARE_SPAWN_ACTIVE               = "Is active?";
  public static final String ARE_SPAWN_ACTIVE_AT            = "Active at";
  public static final String ARE_SPAWN_PROBABILITY_DAY      = "Probability (day)";
  public static final String ARE_SPAWN_PROBABILITY_NIGHT    = "Probability (night)";
  public static final String ARE_SPAWN_FREQUENCY            = "Spawn frequency";
  public static final String ARE_SPAWN_COUNTDOWN            = "Countdown";
  public static final String ARE_SPAWN_WEIGHT_FMT           = "Spawn weight %d";

  public static final String[] s_method = {"No flags set", "Spawn until paused",
                                           "Disable after spawn", "Spawn paused"};

  SpawnPoint() throws Exception
  {
    super(null, ARE_SPAWN, StreamUtils.getByteBuffer(200), 0);
  }

  SpawnPoint(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, ARE_SPAWN + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, ARE_SPAWN_NAME));
    addField(new DecNumber(buffer, offset + 32, 2, ARE_SPAWN_LOCATION_X));
    addField(new DecNumber(buffer, offset + 34, 2, ARE_SPAWN_LOCATION_Y));
    for (int i = 0; i < 10; i++) {
      addField(new SpawnResourceRef(buffer, offset + 36 + (i * 8),
                                    String.format(ARE_SPAWN_CREATURE_FMT, i+1)));
    }
    addField(new DecNumber(buffer, offset + 116, 2, ARE_SPAWN_NUM_CREATURES));
    addField(new DecNumber(buffer, offset + 118, 2, ARE_SPAWN_ENCOUNTER_DIFFICULTY));
    addField(new DecNumber(buffer, offset + 120, 2, ARE_SPAWN_RATE));
    addField(new Flag(buffer, offset + 122, 2, ARE_SPAWN_METHOD, s_method));
    addField(new DecNumber(buffer, offset + 124, 4, ARE_SPAWN_DURATION));
    addField(new DecNumber(buffer, offset + 128, 2, ARE_SPAWN_WANDER_DISTANCE));
    addField(new DecNumber(buffer, offset + 130, 2, ARE_SPAWN_FOLLOW_DISTANCE));
    addField(new DecNumber(buffer, offset + 132, 2, ARE_SPAWN_MAX_CREATURES));
    addField(new Bitmap(buffer, offset + 134, 2, ARE_SPAWN_ACTIVE, OPTION_NOYES));
    addField(new Flag(buffer, offset + 136, 4, ARE_SPAWN_ACTIVE_AT, OPTION_SCHEDULE));
    addField(new DecNumber(buffer, offset + 140, 2, ARE_SPAWN_PROBABILITY_DAY));
    addField(new DecNumber(buffer, offset + 142, 2, ARE_SPAWN_PROBABILITY_NIGHT));
    if (Profile.isEnhancedEdition()) {
      addField(new DecNumber(buffer, offset + 144, 4, ARE_SPAWN_FREQUENCY));
      addField(new DecNumber(buffer, offset + 148, 4, ARE_SPAWN_COUNTDOWN));
      for (int i = 0; i < 10; i++) {
        addField(new DecNumber(buffer, offset + 152 + i, 1,
                               String.format(ARE_SPAWN_WEIGHT_FMT, i+1)));
      }
      addField(new Unknown(buffer, offset + 162, 38));
    } else {
      addField(new Unknown(buffer, offset + 144, 56));
    }
    return offset + 200;
  }
}

