// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class AutomapNotePST extends AbstractStruct implements AddRemovable
{
  public static final String[] s_noyes = { "No", "Yes" };

  AutomapNotePST() throws Exception
  {
    super(null, "Automap note", new byte[532], 0);
  }

  AutomapNotePST(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
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
    addField(new DecNumber(buffer, offset, 4, "Coordinate: X"));
    addField(new DecNumber(buffer, offset + 4, 4, "Coordinate: Y"));
    addField(new TextString(buffer, offset + 8, 500, "Text"));
    addField(new Bitmap(buffer, offset + 508, 4, "Is read only?", s_noyes));
    addField(new Unknown(buffer, offset + 512, 20));
    return offset + 532;
  }
}

