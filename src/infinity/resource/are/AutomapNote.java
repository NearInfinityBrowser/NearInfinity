// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class AutomapNote extends AbstractStruct implements AddRemovable
{
  private static final String s_flag[] = {"Gray", "Violet", "Green", "Orange", "Red", "Blue",
                                          "Dark blue", "Light gray"};
  private static final String s_source[] = {"Talk override", "Dialog.tlk"};

  AutomapNote() throws Exception
  {
    super(null, "Automap note", new byte[52], 0);
  }

  AutomapNote(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Automap note", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new DecNumber(buffer, offset, 2, "Coordinate: X"));
    list.add(new DecNumber(buffer, offset + 2, 2, "Coordinate: Y"));
    list.add(new StringRef(buffer, offset + 4, "Text"));
    list.add(new Bitmap(buffer, offset + 8, 2, "Text location", s_source));
    list.add(new Bitmap(buffer, offset + 10, 2, "Marker color", s_flag));
    list.add(new DecNumber(buffer, offset + 12, 4, "Control ID"));
    list.add(new Unknown(buffer, offset + 16, 36));
    return offset + 52;
  }
}

