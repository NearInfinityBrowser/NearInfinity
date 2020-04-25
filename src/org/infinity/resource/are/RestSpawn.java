// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.SpawnResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;

public final class RestSpawn extends AbstractStruct
{
  // ARE/Rest Encounters-specific field labels
  public static final String ARE_RESTSPAWN                      = "Rest encounters";
  public static final String ARE_RESTSPAWN_NAME                 = "Name";
  public static final String ARE_RESTSPAWN_CREATURE_STRING_FMT  = "Creature %d string";
  public static final String ARE_RESTSPAWN_CREATURE_FMT         = "Creature %d";
  public static final String ARE_RESTSPAWN_NUM_CREATURES        = "# creatures";
  public static final String ARE_RESTSPAWN_ENCOUNTER_DIFFICULTY = "Encounter difficulty";
  public static final String ARE_RESTSPAWN_DURATION             = "Creature duration";
  public static final String ARE_RESTSPAWN_WANDER_DISTANCE      = "Creature wander distance";
  public static final String ARE_RESTSPAWN_FOLLOW_DISTANCE      = "Creature follow distance";
  public static final String ARE_RESTSPAWN_MAX_CREATURES        = "Maximum spawned creatures";
  public static final String ARE_RESTSPAWN_ACTIVE               = "Is active?";
  public static final String ARE_RESTSPAWN_PROBABILITY_DAY      = "Probability (day)";
  public static final String ARE_RESTSPAWN_PROBABILITY_NIGHT    = "Probability (night)";

  RestSpawn(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception
  {
    super(superStruct, ARE_RESTSPAWN, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, ARE_RESTSPAWN_NAME));
    for (int i = 0; i < 10; i++) {
      addField(new StringRef(buffer, offset + 32 + (i * 4),
                             String.format(ARE_RESTSPAWN_CREATURE_STRING_FMT, i+1)));
    }
    for (int i = 0; i < 10; i++) {
      addField(new SpawnResourceRef(buffer, offset + 72 + i * 8,
                                    String.format(ARE_RESTSPAWN_CREATURE_FMT, i+1)));
    }
    addField(new DecNumber(buffer, offset + 152, 2, ARE_RESTSPAWN_NUM_CREATURES));
    addField(new DecNumber(buffer, offset + 154, 2, ARE_RESTSPAWN_ENCOUNTER_DIFFICULTY));
    addField(new DecNumber(buffer, offset + 156, 4, ARE_RESTSPAWN_DURATION));
    addField(new DecNumber(buffer, offset + 160, 2, ARE_RESTSPAWN_WANDER_DISTANCE));
    addField(new DecNumber(buffer, offset + 162, 2, ARE_RESTSPAWN_FOLLOW_DISTANCE));
    addField(new DecNumber(buffer, offset + 164, 2, ARE_RESTSPAWN_MAX_CREATURES));
    addField(new Bitmap(buffer, offset + 166, 2, ARE_RESTSPAWN_ACTIVE, OPTION_NOYES));
    addField(new DecNumber(buffer, offset + 168, 2, ARE_RESTSPAWN_PROBABILITY_DAY));
    addField(new DecNumber(buffer, offset + 170, 2, ARE_RESTSPAWN_PROBABILITY_NIGHT));
    addField(new Unknown(buffer, offset + 172, 56));
    return offset + 228;
  }
}

