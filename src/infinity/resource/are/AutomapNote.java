// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class AutomapNote extends AbstractStruct implements AddRemovable
{
  // ARE/Automap Notes-specific field labels
  public static final String ARE_AUTOMAP                = "Automap note";
  public static final String ARE_AUTOMAP_LOCATION_X     = "Coordinate: X";
  public static final String ARE_AUTOMAP_LOCATION_Y     = "Coordinate: Y";
  public static final String ARE_AUTOMAP_TEXT           = "Text";
  public static final String ARE_AUTOMAP_TEXT_LOCATION  = "Text location";
  public static final String ARE_AUTOMAP_MARKER_COLOR   = "Marker color";
  public static final String ARE_AUTOMAP_CONTROL_ID     = "Control ID";

  public static final String[] s_flag = {"Gray", "Violet", "Green", "Orange", "Red", "Blue",
                                          "Dark blue", "Light gray"};
  public static final String[] s_source = {"Talk override", "Dialog.tlk"};

  AutomapNote() throws Exception
  {
    super(null, ARE_AUTOMAP, new byte[52], 0);
  }

  AutomapNote(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, ARE_AUTOMAP + " " + number, buffer, offset);
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
    addField(new DecNumber(buffer, offset, 2, ARE_AUTOMAP_LOCATION_X));
    addField(new DecNumber(buffer, offset + 2, 2, ARE_AUTOMAP_LOCATION_Y));
    addField(new StringRef(buffer, offset + 4, ARE_AUTOMAP_TEXT));
    addField(new Bitmap(buffer, offset + 8, 2, ARE_AUTOMAP_TEXT_LOCATION, s_source));
    addField(new Bitmap(buffer, offset + 10, 2, ARE_AUTOMAP_MARKER_COLOR, s_flag));
    addField(new DecNumber(buffer, offset + 12, 4, ARE_AUTOMAP_CONTROL_ID));
    addField(new Unknown(buffer, offset + 16, 36));
    return offset + 52;
  }
}

