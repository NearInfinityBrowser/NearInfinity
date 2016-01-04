// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

abstract class AreaLink extends AbstractStruct
{
  // WMP/AreaLink-specific field labels
  public static final String WMP_LINK_TARGET_AREA                   = "Target area";
  public static final String WMP_LINK_TARGET_ENTRANCE               = "Target entrance";
  public static final String WMP_LINK_DISTANCE_SCALE                = "Distance scale";
  public static final String WMP_LINK_DEFAULT_ENTRANCE              = "Default entrance";
  public static final String WMP_LINK_RANDOM_ENCOUNTER_AREA_FMT     = "Random encounter area %d";
  public static final String WMP_LINK_RANDOM_ENCOUNTER_PROBABILITY  = "Random encounter probability";

  public static final String[] s_entrance = {"No default set", "North", "East", "South", "West"};

  AreaLink(String name) throws Exception
  {
    super(null, name, new byte[216], 0);
  }

  AreaLink(AbstractStruct superStruct, byte buffer[], int offset, String name) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 4, WMP_LINK_TARGET_AREA));
    addField(new TextString(buffer, offset + 4, 32, WMP_LINK_TARGET_ENTRANCE));
    addField(new DecNumber(buffer, offset + 36, 4, WMP_LINK_DISTANCE_SCALE));
    addField(new Flag(buffer, offset + 40, 4, WMP_LINK_DEFAULT_ENTRANCE, s_entrance));
    for (int i = 0; i < 5; i++) {
      addField(new ResourceRef(buffer, offset + 44 + (i * 8),
                               String.format(WMP_LINK_RANDOM_ENCOUNTER_AREA_FMT, i+1), "ARE"));
    }
    addField(new DecNumber(buffer, offset + 84, 4, WMP_LINK_RANDOM_ENCOUNTER_PROBABILITY));
    addField(new Unknown(buffer, offset + 88, 128));
    return offset + 216;
  }
}

