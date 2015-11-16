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
  public static final String[] s_flag = {"Gray", "Violet", "Green", "Orange", "Red", "Blue",
                                          "Dark blue", "Light gray"};
  public static final String[] s_source = {"Talk override", "Dialog.tlk"};

  AutomapNote() throws Exception
  {
    super(null, "Automap note", new byte[52], 0);
  }

  AutomapNote(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Automap note " + number, buffer, offset);
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
    addField(new DecNumber(buffer, offset, 2, "Coordinate: X"));
    addField(new DecNumber(buffer, offset + 2, 2, "Coordinate: Y"));
    addField(new StringRef(buffer, offset + 4, "Text"));
    addField(new Bitmap(buffer, offset + 8, 2, "Text location", s_source));
    addField(new Bitmap(buffer, offset + 10, 2, "Marker color", s_flag));
    addField(new DecNumber(buffer, offset + 12, 4, "Control ID"));
    addField(new Unknown(buffer, offset + 16, 36));
    return offset + 52;
  }
}

